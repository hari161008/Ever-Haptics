package com.hapticks.app.service

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
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.hapticks.app.HapticksApp
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticEngine
import com.hapticks.app.service.accessibility.isAccessibilityEventFromOwnApplication
import com.hapticks.app.service.accessibility.interacted.InteractableViewHaptics
import com.hapticks.app.service.accessibility.scrolled.ScrollContentVibration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HapticsAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var settingsJob: Job? = null

    @Volatile private var current: HapticsSettings = HapticsSettings.Default

    private lateinit var engine: HapticEngine
    private lateinit var audioManager: AudioManager
    private val vibrator: Vibrator by lazy {
        (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    }

    private var chargingReceiver: BroadcastReceiver? = null
    private var brightnessObserver: ContentObserver? = null
    private var unlockReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null

    // Debounce for navbar haptics — prevents multiple fires per gesture
    @Volatile private var lastNavBarHapticMs: Long = 0L
    private val navBarDebounceMs = 500L

    // Debounce for keyboard haptics
    @Volatile private var lastKeyboardHapticMs: Long = 0L
    private val keyboardDebounceMs = 30L

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

    private fun playCustomOrPattern(seq: CustomHapticSequence, playPattern: () -> Unit) {
        if (!seq.isEmpty) scope.launch { playCustomSequence(seq) }
        else playPattern()
    }

    private suspend fun playCustomSequence(seq: CustomHapticSequence) {
        val sorted = seq.beats.sortedBy { it.offsetMs }
        val touchAttrs = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
        val startMs = System.currentTimeMillis()
        for (beat in sorted) {
            val now = System.currentTimeMillis() - startMs
            val waitMs = beat.offsetMs - now
            if (waitMs > 0) delay(waitMs)
            withContext(Dispatchers.Main) {
                try { vibrator.vibrate(VibrationEffect.createOneShot(60L, beat.amplitude.coerceIn(1, 255)), touchAttrs) }
                catch (_: Exception) { engine.playOneShot(60L, beat.amplitude) }
            }
        }
    }

    private fun updateChargingReceiver(settings: HapticsSettings) {
        if (settings.globalEnabled && settings.chargingVibEnabled) {
            if (chargingReceiver == null) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val s = current
                        if (!s.globalEnabled || !s.chargingVibEnabled) return
                        if (s.batterySaverDetectionEnabled && isBatterySaverActive()) return
                        val shouldPlay = when (intent?.action) {
                            Intent.ACTION_POWER_CONNECTED -> s.chargingVibOnConnect
                            Intent.ACTION_POWER_DISCONNECTED -> s.chargingVibOnDisconnect
                            else -> false
                        }
                        if (shouldPlay) playCustomOrPattern(s.chargingVibCustomSequence) { engine.play(s.chargingVibPattern, s.chargingVibIntensity) }
                    }
                }
                val filter = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) }
                registerReceiver(receiver, filter)
                chargingReceiver = receiver
            }
        } else {
            chargingReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {}; chargingReceiver = null }
        }
    }

    private fun updateBrightnessObserver(settings: HapticsSettings) {
        if (settings.globalEnabled && settings.brightnessHapticEnabled) {
            if (brightnessObserver == null) {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        val s = current
                        if (!s.globalEnabled || !s.brightnessHapticEnabled) return
                        if (s.batterySaverDetectionEnabled && isBatterySaverActive()) return
                        playCustomOrPattern(s.brightnessHapticCustomSequence) { engine.play(s.brightnessHapticPattern, s.brightnessHapticIntensity, 0L) }
                    }
                }
                contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, observer)
                brightnessObserver = observer
            }
        } else {
            brightnessObserver?.let { try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}; brightnessObserver = null }
        }
    }

    private fun updateUnlockReceiver(settings: HapticsSettings) {
        if (settings.globalEnabled && settings.unlockHapticEnabled) {
            if (unlockReceiver == null) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val s = current
                        if (!s.globalEnabled || !s.unlockHapticEnabled || intent?.action != Intent.ACTION_USER_PRESENT) return
                        if (s.batterySaverDetectionEnabled && isBatterySaverActive()) return
                        playCustomOrPattern(s.unlockHapticCustomSequence) { engine.play(s.unlockHapticPattern, s.unlockHapticIntensity) }
                    }
                }
                registerReceiver(receiver, IntentFilter(Intent.ACTION_USER_PRESENT))
                unlockReceiver = receiver
            }
        } else {
            unlockReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {}; unlockReceiver = null }
        }
    }

    private fun updateScreenReceiver(settings: HapticsSettings) {
        if (settings.globalEnabled && settings.powerHapticEnabled) {
            if (screenReceiver == null) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val s = current
                        if (!s.globalEnabled || !s.powerHapticEnabled) return
                        if (s.batterySaverDetectionEnabled && isBatterySaverActive()) return
                        when (intent?.action) {
                            Intent.ACTION_SCREEN_OFF -> playCustomOrPattern(s.powerHapticCustomSequence) { engine.play(s.powerHapticPattern, s.powerHapticIntensity) }
                            Intent.ACTION_SCREEN_ON -> playCustomOrPattern(s.powerHapticCustomSequence) { engine.play(s.powerHapticPattern, s.powerHapticIntensity, 200L) }
                        }
                    }
                }
                val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
                registerReceiver(receiver, filter)
                screenReceiver = receiver
            }
        } else {
            screenReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {}; screenReceiver = null }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val s = current
        if (!s.globalEnabled) return false
        if (s.batterySaverDetectionEnabled && isBatterySaverActive()) return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (s.volumeHapticEnabled && event.action == KeyEvent.ACTION_DOWN) {
                    val atLimit = isVolumeAtLimit(event.keyCode)
                    val intensity = if (atLimit) 1.0f else s.volumeHapticIntensity
                    playCustomOrPattern(s.volumeHapticCustomSequence) { engine.play(s.volumeHapticPattern, intensity, 0L) }
                }
            }
            KeyEvent.KEYCODE_POWER -> {
                if (s.powerHapticEnabled && event.action == KeyEvent.ACTION_DOWN)
                    playCustomOrPattern(s.powerHapticCustomSequence) { engine.play(s.powerHapticPattern, s.powerHapticIntensity, 0L) }
            }
            KeyEvent.KEYCODE_HOME -> {
                if (s.navBarHapticEnabled && event.action == KeyEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    if (now - lastNavBarHapticMs >= navBarDebounceMs) {
                        lastNavBarHapticMs = now
                        playCustomOrPattern(s.navBarHapticCustomSequence) { engine.play(s.navBarHapticPattern, s.navBarHapticIntensity, 0L) }
                    }
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                if (s.navBarHapticEnabled && event.action == KeyEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    if (now - lastNavBarHapticMs >= navBarDebounceMs) {
                        lastNavBarHapticMs = now
                        playCustomOrPattern(s.navBarHapticCustomSequence) { engine.play(s.navBarHapticPattern, s.navBarHapticIntensity, 0L) }
                    }
                }
            }
        }
        return false
    }

    private fun isVolumeAtLimit(keyCode: Int): Boolean = try {
        val stream = AudioManager.STREAM_MUSIC
        val cur = audioManager.getStreamVolume(stream)
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> cur >= audioManager.getStreamMaxVolume(stream)
            KeyEvent.KEYCODE_VOLUME_DOWN -> cur <= audioManager.getStreamMinVolume(stream)
            else -> false
        }
    } catch (_: Exception) { false }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val s = current
        if (!s.globalEnabled) return
        if (s.batterySaverDetectionEnabled && isBatterySaverActive()) return
        val type = ev.eventType
        val pkg = ev.packageName?.toString()
        val fromOwnApp = isAccessibilityEventFromOwnApplication(ev)
        if (fromOwnApp && type != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        when (type) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (s.tapEnabled && !s.tapExcludedPackages.contains(pkg) && InteractableViewHaptics.hasToggleLikeContentChange(ev)) {
                    if (!s.tapHapticCustomSequence.isEmpty) scope.launch { playCustomSequence(s.tapHapticCustomSequence) }
                    else InteractableViewHaptics.handle(engine, s, ev)
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Keyboard haptics: detect clicks on IME / keyboard views
                if (s.keyboardHapticEnabled) {
                    val now = System.currentTimeMillis()
                    val sourceClass = ev.className?.toString() ?: ""
                    val isKeyboardEvent = isKeyboardPackage(pkg) ||
                            sourceClass.contains("Key", ignoreCase = true) ||
                            sourceClass.contains("Button", ignoreCase = true)
                    if (isKeyboardEvent && now - lastKeyboardHapticMs >= keyboardDebounceMs) {
                        lastKeyboardHapticMs = now
                        playCustomOrPattern(s.keyboardHapticCustomSequence) { engine.play(s.keyboardHapticPattern, s.keyboardHapticIntensity, 0L) }
                        return
                    }
                }
                if (s.tapEnabled && !s.tapExcludedPackages.contains(pkg)) {
                    if (!s.tapHapticCustomSequence.isEmpty) scope.launch { playCustomSequence(s.tapHapticCustomSequence) }
                    else InteractableViewHaptics.handle(engine, s, ev)
                }
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // Keyboard haptics via text change (catches software keyboard typing)
                if (s.keyboardHapticEnabled) {
                    val now = System.currentTimeMillis()
                    if (now - lastKeyboardHapticMs >= keyboardDebounceMs) {
                        lastKeyboardHapticMs = now
                        playCustomOrPattern(s.keyboardHapticCustomSequence) { engine.play(s.keyboardHapticPattern, s.keyboardHapticIntensity, 0L) }
                    }
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // NavBar gesture navigation: fire only when a new window/activity is shown
                // and debounce to prevent multiple fires per gesture
                if (s.navBarHapticEnabled) {
                    val now = System.currentTimeMillis()
                    // Only fire for actual window transitions (non-null class name = real activity/fragment)
                    val className = ev.className?.toString()
                    val isRealWindow = !className.isNullOrBlank() &&
                            !className.contains("PopupWindow", ignoreCase = true) &&
                            !className.contains("Toast", ignoreCase = true) &&
                            !className.contains("DropDown", ignoreCase = true) &&
                            !className.contains("AutoComplete", ignoreCase = true) &&
                            ev.packageName != "android"
                    if (isRealWindow && now - lastNavBarHapticMs >= navBarDebounceMs) {
                        lastNavBarHapticMs = now
                        playCustomOrPattern(s.navBarHapticCustomSequence) { engine.play(s.navBarHapticPattern, s.navBarHapticIntensity, 0L) }
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (s.scrollEnabled && !s.scrollExcludedPackages.contains(pkg)) {
                    when (val scroll = ScrollContentVibration.onViewScrolled(ev, s)) {
                        is ScrollContentVibration.Decision.Play -> {
                            if (scroll.count <= 1) engine.play(s.scrollPattern, scroll.intensity, 0L)
                            else scope.launch { repeat(scroll.count) { i -> if (i > 0) delay(42L); engine.play(s.scrollPattern, scroll.intensity, 0L) } }
                        }
                        ScrollContentVibration.Decision.None -> Unit
                    }
                }
            }
        }
    }

    private fun isKeyboardPackage(pkg: String?): Boolean {
        if (pkg == null) return false
        return pkg.contains("keyboard", ignoreCase = true) ||
                pkg.contains("inputmethod", ignoreCase = true) ||
                pkg.contains("gboard", ignoreCase = true) ||
                pkg.contains("honeyboard", ignoreCase = true) ||
                pkg.contains("swiftkey", ignoreCase = true) ||
                pkg.contains("grammarly", ignoreCase = true) ||
                pkg.contains("fleksy", ignoreCase = true) ||
                pkg.contains("chrooma", ignoreCase = true) ||
                pkg.contains("touchpal", ignoreCase = true) ||
                pkg.contains("kika", ignoreCase = true) ||
                pkg == "com.google.android.inputmethod.latin"
    }

    override fun onInterrupt() = Unit

    private fun isBatterySaverActive(): Boolean =
        (getSystemService(POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true

    override fun onDestroy() {
        settingsJob?.cancel(); scope.cancel()
        chargingReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        brightnessObserver?.let { try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {} }
        unlockReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        screenReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    private fun applyEventMask(settings: HapticsSettings) {
        val info = serviceInfo ?: return
        var mask = InteractableViewHaptics.eventTypeMask(settings)
        if (settings.scrollEnabled) mask = mask or AccessibilityEvent.TYPE_VIEW_SCROLLED
        if (settings.navBarHapticEnabled) mask = mask or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (settings.keyboardHapticEnabled) mask = mask or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED
        if (mask == 0) mask = AccessibilityEvent.TYPE_VIEW_CLICKED
        if (info.eventTypes == mask) return
        info.eventTypes = mask; serviceInfo = info
    }
}
