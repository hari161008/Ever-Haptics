package com.coolappstore.everhaptics.by.svhp.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.coolappstore.everhaptics.by.svhp.HapticksApp
import com.coolappstore.everhaptics.by.svhp.data.HapticsSettings
import com.coolappstore.everhaptics.by.svhp.haptics.HapticEngine
import com.coolappstore.everhaptics.by.svhp.service.accessibility.isAccessibilityEventFromOwnApplication
import com.coolappstore.everhaptics.by.svhp.service.accessibility.interacted.InteractableViewHaptics
import com.coolappstore.everhaptics.by.svhp.service.accessibility.scrolled.ScrollAbsoluteEdgeVibration
import com.coolappstore.everhaptics.by.svhp.service.accessibility.scrolled.ScrollContentVibration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HapticsAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var settingsJob: Job? = null

    @Volatile
    private var current: HapticsSettings = HapticsSettings.Default

    private lateinit var engine: HapticEngine
    private lateinit var audioManager: AudioManager

    private var chargingReceiver: BroadcastReceiver? = null
    private var brightnessObserver: ContentObserver? = null
    private var unlockReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as HapticksApp
        engine = app.hapticEngine
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        applyEventMask(HapticsSettings.Default)

        settingsJob = app.preferences.settings
            .distinctUntilChanged()
            .onEach { snapshot ->
                current = snapshot
                applyEventMask(snapshot)
                updateChargingReceiver(snapshot)
                updateBrightnessObserver(snapshot)
                updateUnlockReceiver(snapshot)
                updateScreenReceiver(snapshot)
            }
            .launchIn(scope)
    }

    private fun updateChargingReceiver(settings: HapticsSettings) {
        if (settings.globalEnabled && settings.chargingVibEnabled) {
            if (chargingReceiver == null) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val s = current
                        if (!s.globalEnabled || !s.chargingVibEnabled) return
                        val durationMs = when (s.chargingVibDurationIndex) {
                            0 -> 2000L
                            1 -> 2500L
                            else -> 3000L
                        }
                        val amplitude = (s.chargingVibIntensity * 255).toInt().coerceIn(1, 255)
                        when (intent?.action) {
                            Intent.ACTION_POWER_CONNECTED -> {
                                if (s.chargingVibOnConnect) engine.playOneShot(durationMs, amplitude)
                            }
                            Intent.ACTION_POWER_DISCONNECTED -> {
                                if (s.chargingVibOnDisconnect) engine.playOneShot(durationMs, amplitude)
                            }
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                }
                registerReceiver(receiver, filter)
                chargingReceiver = receiver
            }
        } else {
            chargingReceiver?.let {
                try { unregisterReceiver(it) } catch (_: Exception) {}
                chargingReceiver = null
            }
        }
    }

    private fun updateBrightnessObserver(settings: HapticsSettings) {
        if (settings.globalEnabled && settings.brightnessHapticEnabled) {
            if (brightnessObserver == null) {
                val handler = Handler(Looper.getMainLooper())
                val observer = object : ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        val s = current
                        if (s.globalEnabled && s.brightnessHapticEnabled) {
                            engine.play(s.brightnessHapticPattern, s.brightnessHapticIntensity, throttleMs = 0L)
                        }
                    }
                }
                contentResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                    false,
                    observer,
                )
                brightnessObserver = observer
            }
        } else {
            brightnessObserver?.let {
                try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}
                brightnessObserver = null
            }
        }
    }

    private fun updateUnlockReceiver(settings: HapticsSettings) {
        if (settings.globalEnabled && settings.unlockHapticEnabled) {
            if (unlockReceiver == null) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val s = current
                        if (s.globalEnabled && s.unlockHapticEnabled && intent?.action == Intent.ACTION_USER_PRESENT) {
                            engine.play(s.unlockHapticPattern, s.unlockHapticIntensity)
                        }
                    }
                }
                val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
                registerReceiver(receiver, filter)
                unlockReceiver = receiver
            }
        } else {
            unlockReceiver?.let {
                try { unregisterReceiver(it) } catch (_: Exception) {}
                unlockReceiver = null
            }
        }
    }

    private fun updateScreenReceiver(settings: HapticsSettings) {
        if (settings.globalEnabled && settings.powerHapticEnabled) {
            if (screenReceiver == null) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val s = current
                        if (!s.globalEnabled || !s.powerHapticEnabled) return
                        when (intent?.action) {
                            Intent.ACTION_SCREEN_OFF -> {
                                engine.play(s.powerHapticPattern, s.powerHapticIntensity)
                            }
                            Intent.ACTION_SCREEN_ON -> {
                                engine.play(s.powerHapticPattern, s.powerHapticIntensity, throttleMs = 200L)
                            }
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                }
                registerReceiver(receiver, filter)
                screenReceiver = receiver
            }
        } else {
            screenReceiver?.let {
                try { unregisterReceiver(it) } catch (_: Exception) {}
                screenReceiver = null
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val s = current
        if (!s.globalEnabled) return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (s.volumeHapticEnabled && event.action == KeyEvent.ACTION_DOWN) {
                    // Fire immediately for every key event including repeats
                    val isAtLimit = isVolumeAtLimit(event.keyCode)
                    if (isAtLimit) {
                        engine.play(s.volumeHapticPattern, 1.0f, throttleMs = 0L)
                    } else {
                        engine.play(s.volumeHapticPattern, s.volumeHapticIntensity, throttleMs = 0L)
                    }
                }
            }
            KeyEvent.KEYCODE_POWER -> {
                if (s.powerHapticEnabled && event.action == KeyEvent.ACTION_DOWN) {
                    engine.play(s.powerHapticPattern, s.powerHapticIntensity, throttleMs = 0L)
                }
            }
            KeyEvent.KEYCODE_HOME -> {
                if (s.navBarHapticEnabled && event.action == KeyEvent.ACTION_DOWN) {
                    engine.play(s.navBarHapticPattern, s.navBarHapticIntensity, throttleMs = 100L)
                }
            }
        }
        return false
    }

    private fun isVolumeAtLimit(keyCode: Int): Boolean {
        return try {
            val stream = AudioManager.STREAM_MUSIC
            val currentVolume = audioManager.getStreamVolume(stream)
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            val minVolume = audioManager.getStreamMinVolume(stream)
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> currentVolume >= maxVolume
                KeyEvent.KEYCODE_VOLUME_DOWN -> currentVolume <= minVolume
                else -> false
            }
        } catch (_: Exception) { false }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val s = current
        if (!s.globalEnabled) return
        val type = ev.eventType
        val pkg = ev.packageName?.toString()

        val fromOwnApp = isAccessibilityEventFromOwnApplication(ev)
        if (fromOwnApp && type != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        when (type) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (s.tapEnabled &&
                    !s.tapExcludedPackages.contains(pkg) &&
                    InteractableViewHaptics.hasToggleLikeContentChange(ev)
                ) {
                    InteractableViewHaptics.handle(engine, s, ev)
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (!s.tapExcludedPackages.contains(pkg)) {
                    InteractableViewHaptics.handle(engine, s, ev)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Detect home/launcher navigation for navBar haptic
                if (s.navBarHapticEnabled) {
                    val pkgName = pkg ?: ""
                    if (pkgName == "com.android.launcher3" || pkgName.contains("launcher", ignoreCase = true)) {
                        engine.play(s.navBarHapticPattern, s.navBarHapticIntensity, throttleMs = 300L)
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                var consumedByEdge = false

                if (!s.scrollExcludedPackages.contains(pkg)) {
                    if (ScrollAbsoluteEdgeVibration.onViewScrolled(ev) ==
                        ScrollAbsoluteEdgeVibration.Result.PlayEdgeHaptic
                    ) {
                        consumedByEdge = true
                    }
                }

                if (s.scrollEnabled && !consumedByEdge && !s.scrollExcludedPackages.contains(pkg)) {
                    when (val scroll = ScrollContentVibration.onViewScrolled(ev, s)) {
                        is ScrollContentVibration.Decision.Play -> {
                            val count = scroll.count
                            if (count <= 1) {
                                engine.play(s.scrollPattern, scroll.intensity, throttleMs = 0L)
                            } else {
                                scope.launch {
                                    repeat(count) { i ->
                                        if (i > 0) delay(42L)
                                        engine.play(s.scrollPattern, scroll.intensity, throttleMs = 0L)
                                    }
                                }
                            }
                        }
                        ScrollContentVibration.Decision.None -> Unit
                    }
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        settingsJob?.cancel()
        scope.cancel()
        chargingReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        brightnessObserver?.let { try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {} }
        unlockReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        screenReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    private fun applyEventMask(settings: HapticsSettings) {
        val info = serviceInfo ?: return
        var mask = InteractableViewHaptics.eventTypeMask(settings)
        if (settings.scrollEnabled) {
            mask = mask or AccessibilityEvent.TYPE_VIEW_SCROLLED
        }
        if (settings.navBarHapticEnabled) {
            mask = mask or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        }
        if (mask == 0) mask = AccessibilityEvent.TYPE_VIEW_CLICKED
        if (info.eventTypes == mask) return
        info.eventTypes = mask
        serviceInfo = info
    }
}
