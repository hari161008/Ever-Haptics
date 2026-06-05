package com.hapticks.app.service.notification

import android.app.Notification
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.hapticks.app.HapticksApp
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NotificationHapticService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Holds repeating jobs for calls/alarms keyed by sbn.key
    private val repeatingJobs = mutableMapOf<String, Job>()

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    // VibrationAttributes for each category — ensures proper interaction with DnD/silent mode
    private val ringtoneAttrs: VibrationAttributes by lazy {
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_RINGTONE)
    }
    private val notifAttrs: VibrationAttributes by lazy {
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION)
    }
    private val alarmAttrs: VibrationAttributes by lazy {
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return

        // Skip our own notifications
        if (sbn.packageName == packageName) return

        // Skip silent/min-priority notifications
        if (notification.priority < Notification.PRIORITY_DEFAULT) return

        val app = applicationContext as? HapticksApp ?: return

        scope.launch {
            val settings = try {
                app.preferences.settings.first()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read settings", e)
                return@launch
            }

            if (!settings.globalEnabled) return@launch

            val category = notification.category

            when {
                // ── Incoming call ──────────────────────────────────────────
                category == Notification.CATEGORY_CALL ||
                isIncomingCallNotification(sbn) -> {
                    if (!settings.callHapticEnabled) return@launch
                    repeatingJobs[sbn.key]?.cancel()
                    repeatingJobs[sbn.key] = launch {
                        while (isActive) {
                            fireHaptic(
                                pattern = settings.callHapticPattern,
                                intensity = settings.callHapticIntensity,
                                customSequence = settings.callHapticCustomSequence,
                                attrs = ringtoneAttrs,
                            )
                            // Wait pattern duration + gap before repeating
                            val patternDuration = if (!settings.callHapticCustomSequence.isEmpty)
                                settings.callHapticCustomSequence.durationMs + 800L
                            else 1500L
                            delay(patternDuration.coerceAtLeast(1200L))
                        }
                    }
                }

                // ── Alarm ──────────────────────────────────────────────────
                category == Notification.CATEGORY_ALARM -> {
                    if (!settings.alarmHapticEnabled) return@launch
                    repeatingJobs[sbn.key]?.cancel()
                    repeatingJobs[sbn.key] = launch {
                        while (isActive) {
                            fireHaptic(
                                pattern = settings.alarmHapticPattern,
                                intensity = settings.alarmHapticIntensity,
                                customSequence = settings.alarmHapticCustomSequence,
                                attrs = alarmAttrs,
                            )
                            val patternDuration = if (!settings.alarmHapticCustomSequence.isEmpty)
                                settings.alarmHapticCustomSequence.durationMs + 600L
                            else 2000L
                            delay(patternDuration.coerceAtLeast(1500L))
                        }
                    }
                }

                // ── Notification (all other categories) ───────────────────
                else -> {
                    if (!settings.notifHapticEnabled) return@launch
                    // Skip ongoing/foreground-service notifications
                    if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return@launch
                    // Skip grouped children — only fire for summary or standalone
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val isGroupChild = (notification.flags and Notification.FLAG_GROUP_SUMMARY == 0) &&
                                notification.group != null
                        if (isGroupChild) return@launch
                    }
                    fireHaptic(
                        pattern = settings.notifHapticPattern,
                        intensity = settings.notifHapticIntensity,
                        customSequence = settings.notifHapticCustomSequence,
                        attrs = notifAttrs,
                    )
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        repeatingJobs.remove(sbn.key)?.cancel()
    }

    /** Heuristic to catch call notifications that don't use CATEGORY_CALL */
    private fun isIncomingCallNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        // Apps that properly set CATEGORY_CALL (WhatsApp, Telegram, modern dialers) are already
        // handled by the CATEGORY_CALL branch above. This heuristic is only for legacy apps that
        // use a phone/dialer package but omit the category.
        val extras = notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.lowercase() ?: ""
        val text = extras.getCharSequence("android.text")?.toString()?.lowercase() ?: ""
        val callPkg = sbn.packageName.lowercase()

        // Only match packages that are clearly telephony/dialer apps (not messaging apps like
        // WhatsApp or Telegram which would cause false positives on regular messages).
        val isLikelyDialerPackage = listOf("dialer", "phone", "telecom", "incallui", "truecaller")
            .any { callPkg.contains(it) }

        // Require explicit "incoming call" phrasing — avoid matching generic uses of "calling".
        val hasExplicitCallText = text.contains("incoming call") || title.contains("incoming call")

        return isLikelyDialerPackage && hasExplicitCallText &&
                notification.category != Notification.CATEGORY_ALARM
    }

    private suspend fun fireHaptic(
        pattern: HapticPattern,
        intensity: Float,
        customSequence: CustomHapticSequence,
        attrs: VibrationAttributes,
    ) {
        if (!customSequence.isEmpty) {
            playCustomSequence(customSequence, attrs)
            return
        }
        val effect = buildEffect(pattern, intensity)
        withContext(Dispatchers.Main) {
            vibrator.vibrate(effect, attrs)
        }
    }

    private fun buildEffect(pattern: HapticPattern, intensity: Float): VibrationEffect {
        val hasComposition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
        val hasAmp = vibrator.hasAmplitudeControl()
        val amp = (intensity * 255f).toInt().coerceIn(1, 255)

        if (hasComposition) {
            return VibrationEffect.startComposition().apply {
                when (pattern) {
                    HapticPattern.CLICK -> addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    HapticPattern.TICK -> addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                    HapticPattern.HEAVY_CLICK -> {
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (intensity * 0.6f).coerceIn(0f, 1f), 40)
                    }
                    HapticPattern.DOUBLE_CLICK -> {
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity, 80)
                    }
                    HapticPattern.SOFT_BUMP -> {
                        val primitive = if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK))
                            VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                        else VibrationEffect.Composition.PRIMITIVE_TICK
                        addPrimitive(primitive, (intensity * 0.5f).coerceIn(0f, 1f))
                    }
                    HapticPattern.DOUBLE_TICK -> {
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity, 60)
                    }
                }
            }.compose()
        }

        // Fallback waveform / predefined
        return when (pattern) {
            HapticPattern.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticPattern.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticPattern.HEAVY_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            HapticPattern.DOUBLE_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            HapticPattern.SOFT_BUMP -> if (hasAmp)
                VibrationEffect.createOneShot(30L, (amp * 0.5f).toInt().coerceIn(1, 255))
            else VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticPattern.DOUBLE_TICK -> if (hasAmp)
                VibrationEffect.createWaveform(longArrayOf(0L, 30L, 60L, 30L), intArrayOf(0, amp, 0, amp), -1)
            else VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        }
    }

    private suspend fun playCustomSequence(seq: CustomHapticSequence, attrs: VibrationAttributes) {
        val sorted = seq.beats.sortedBy { it.offsetMs }
        val startMs = System.currentTimeMillis()
        for (beat in sorted) {
            val elapsed = System.currentTimeMillis() - startMs
            val wait = beat.offsetMs - elapsed
            if (wait > 0) delay(wait)
            withContext(Dispatchers.Main) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(80L, beat.amplitude.coerceIn(1, 255)),
                    attrs,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "NotifHapticService"
    }
}
