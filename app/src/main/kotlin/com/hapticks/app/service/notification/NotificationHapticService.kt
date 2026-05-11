package com.hapticks.app.service.notification

import android.app.Notification
import android.os.Build
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

/**
 * NotificationListenerService that fires custom haptic patterns for:
 *  - Incoming calls (category CALL)
 *  - Notifications (category MSG, EMAIL, EVENT, etc.)
 *  - Alarms (category ALARM)
 *
 * The user must enable this service in Settings > Apps > Special app access > Notification access.
 */
class NotificationHapticService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val callJob = mutableMapOf<String, Job>()

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val category = notification.category ?: return
        val extras = notification.extras

        val app = applicationContext as? HapticksApp ?: return

        scope.launch {
            val settings: HapticsSettings = try {
                app.preferences.settings.first()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read settings", e)
                return@launch
            }

            if (!settings.globalEnabled) return@launch

            when (category) {
                Notification.CATEGORY_CALL -> {
                    if (!settings.callHapticEnabled) return@launch
                    // Cancel previous call loop for this key
                    callJob[sbn.key]?.cancel()
                    callJob[sbn.key] = launch {
                        // Repeat haptic every 1.5 s for incoming call
                        while (isActive) {
                            fireHaptic(
                                pattern = settings.callHapticPattern,
                                intensity = settings.callHapticIntensity,
                                customSequence = settings.callHapticCustomSequence,
                            )
                            delay(1500)
                        }
                    }
                }

                Notification.CATEGORY_ALARM -> {
                    if (!settings.alarmHapticEnabled) return@launch
                    callJob[sbn.key]?.cancel()
                    callJob[sbn.key] = launch {
                        while (isActive) {
                            fireHaptic(
                                pattern = settings.alarmHapticPattern,
                                intensity = settings.alarmHapticIntensity,
                                customSequence = settings.alarmHapticCustomSequence,
                            )
                            delay(2000)
                        }
                    }
                }

                Notification.CATEGORY_MESSAGE,
                Notification.CATEGORY_EMAIL,
                Notification.CATEGORY_EVENT,
                Notification.CATEGORY_REMINDER,
                Notification.CATEGORY_SOCIAL,
                Notification.CATEGORY_PROMO -> {
                    if (!settings.notifHapticEnabled) return@launch
                    fireHaptic(
                        pattern = settings.notifHapticPattern,
                        intensity = settings.notifHapticIntensity,
                        customSequence = settings.notifHapticCustomSequence,
                    )
                }

                else -> {
                    // Generic notification
                    if (!settings.notifHapticEnabled) return@launch
                    // Only fire for user-visible, non-ongoing notifications
                    if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return@launch
                    fireHaptic(
                        pattern = settings.notifHapticPattern,
                        intensity = settings.notifHapticIntensity,
                        customSequence = settings.notifHapticCustomSequence,
                    )
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Cancel repeating call/alarm haptic when the notification is dismissed
        callJob.remove(sbn.key)?.cancel()
    }

    private suspend fun fireHaptic(
        pattern: HapticPattern,
        intensity: Float,
        customSequence: CustomHapticSequence,
    ) {
        if (!customSequence.isEmpty) {
            playCustomSequence(customSequence)
            return
        }
        val amp = (intensity * 255f).toInt().coerceIn(1, 255)
        val effect = buildEffect(pattern, intensity)
        withContext(Dispatchers.Main) {
            vibrator.vibrate(effect)
        }
    }

    private fun buildEffect(pattern: HapticPattern, intensity: Float): VibrationEffect {
        val hasAmplitudeControl = vibrator.hasAmplitudeControl()
        return when (pattern) {
            HapticPattern.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticPattern.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticPattern.HEAVY_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            HapticPattern.DOUBLE_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            HapticPattern.SOFT_BUMP -> if (hasAmplitudeControl)
                VibrationEffect.createOneShot(30L, (intensity * 120f).toInt().coerceIn(1, 255))
            else VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticPattern.DOUBLE_TICK -> {
                val timings = longArrayOf(0L, 30L, 60L, 30L)
                val amps = intArrayOf(0, (intensity * 255f).toInt().coerceIn(1, 255), 0, (intensity * 200f).toInt().coerceIn(1, 255))
                if (hasAmplitudeControl) VibrationEffect.createWaveform(timings, amps, -1)
                else VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            }
        }
    }

    private suspend fun playCustomSequence(seq: CustomHapticSequence) {
        val sorted = seq.beats.sortedBy { it.offsetMs }
        val startMs = System.currentTimeMillis()
        for (beat in sorted) {
            val now = System.currentTimeMillis() - startMs
            val waitMs = beat.offsetMs - now
            if (waitMs > 0) delay(waitMs)
            withContext(Dispatchers.Main) {
                vibrator.vibrate(VibrationEffect.createOneShot(60L, beat.amplitude.coerceIn(1, 255)))
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
