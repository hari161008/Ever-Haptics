package com.hapticks.app.service.notification

import android.app.Notification
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.hapticks.app.HapticksApp
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

class NotificationHapticService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repeatingJobs = mutableMapOf<String, Job>()
    private val autoVisualizers = mutableMapOf<String, Visualizer>()
    private val lastHapticMs = AtomicLong(0L)

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }
    private val ringtoneAttrs: VibrationAttributes by lazy { VibrationAttributes.createForUsage(VibrationAttributes.USAGE_RINGTONE) }
    private val notifAttrs: VibrationAttributes by lazy { VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION) }
    private val alarmAttrs: VibrationAttributes by lazy { VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        if (sbn.packageName == packageName) return
        if (notification.priority < Notification.PRIORITY_DEFAULT) return
        val app = applicationContext as? HapticksApp ?: return
        scope.launch {
            val settings = try { app.preferences.settings.first() } catch (e: Exception) { Log.w(TAG, "settings read failed", e); return@launch }
            if (!settings.globalEnabled) return@launch
            val category = notification.category
            when {
                category == Notification.CATEGORY_CALL || isIncomingCallNotification(sbn) -> {
                    if (!settings.callHapticEnabled) return@launch
                    repeatingJobs[sbn.key]?.cancel()
                    if (settings.callHapticAuto) {
                        startAutoVisualizer(sbn.key, settings.callAutoSensitivity, settings.callAutoStrength, ringtoneAttrs)
                    } else {
                        stopAutoVisualizer(sbn.key)
                        repeatingJobs[sbn.key] = launch {
                            while (isActive) {
                                fireHaptic(settings.callHapticPattern, settings.callHapticIntensity, settings.callHapticCustomSequence, ringtoneAttrs)
                                val d = if (!settings.callHapticCustomSequence.isEmpty) settings.callHapticCustomSequence.durationMs + 800L else 1500L
                                delay(d.coerceAtLeast(1200L))
                            }
                        }
                    }
                }
                category == Notification.CATEGORY_ALARM -> {
                    if (!settings.alarmHapticEnabled) return@launch
                    repeatingJobs[sbn.key]?.cancel()
                    if (settings.alarmHapticAuto) {
                        startAutoVisualizer(sbn.key, settings.alarmAutoSensitivity, settings.alarmAutoStrength, alarmAttrs)
                    } else {
                        stopAutoVisualizer(sbn.key)
                        repeatingJobs[sbn.key] = launch {
                            while (isActive) {
                                fireHaptic(settings.alarmHapticPattern, settings.alarmHapticIntensity, settings.alarmHapticCustomSequence, alarmAttrs)
                                val d = if (!settings.alarmHapticCustomSequence.isEmpty) settings.alarmHapticCustomSequence.durationMs + 600L else 2000L
                                delay(d.coerceAtLeast(1500L))
                            }
                        }
                    }
                }
                else -> {
                    if (!settings.notifHapticEnabled) return@launch
                    if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return@launch
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val isGroupChild = (notification.flags and Notification.FLAG_GROUP_SUMMARY == 0) && notification.group != null
                        if (isGroupChild) return@launch
                    }
                    fireHaptic(settings.notifHapticPattern, settings.notifHapticIntensity, settings.notifHapticCustomSequence, notifAttrs)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        repeatingJobs.remove(sbn.key)?.cancel()
        stopAutoVisualizer(sbn.key)
    }

    private fun startAutoVisualizer(key: String, sensitivity: Float, strength: Float, attrs: VibrationAttributes) {
        stopAutoVisualizer(key)
        try {
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtLeast(1024)
            val vis = Visualizer(0)
            vis.captureSize = captureSize
            val dynamicAvg = floatArrayOf(0.04f)
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer, w: ByteArray, sr: Int) {}
                override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {
                    val rateHz = (samplingRate / 1000).coerceAtLeast(8000)
                    val freqPerBin = rateHz.toFloat() / captureSize
                    val bassStart = (40f / freqPerBin).toInt().coerceIn(1, fft.size / 2 - 4)
                    val bassEnd = (300f / freqPerBin).toInt().coerceIn(bassStart + 2, fft.size / 2 - 2)
                    var sumSq = 0.0; var count = 0
                    for (k in bassStart..bassEnd) {
                        val re = fft[k * 2].toFloat(); val im = fft[k * 2 + 1].toFloat()
                        sumSq += re * re + im * im; count++
                    }
                    if (count == 0) return
                    val energy = sqrt(sumSq / count).toFloat() / 128f
                    dynamicAvg[0] = dynamicAvg[0] * 0.9f + energy * 0.1f
                    val threshold = dynamicAvg[0] * (1.8f - sensitivity * 1.0f)
                    val now = System.currentTimeMillis(); val last = lastHapticMs.get()
                    if (energy > threshold && energy > 0.008f && (now - last) >= 80L) {
                        if (lastHapticMs.compareAndSet(last, now)) {
                            val bs = ((energy - threshold) / threshold).coerceIn(0f, 1f)
                            val amp = (strength * (0.35f + 0.65f * bs) * 255f).toInt().coerceIn(30, 255)
                            val dur = (35L + (bs * 55f).toLong()).coerceIn(30L, 90L)
                            vibrator.vibrate(VibrationEffect.createOneShot(dur, amp), attrs)
                        }
                    }
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true)
            vis.enabled = true
            autoVisualizers[key] = vis
        } catch (_: Exception) {}
    }

    private fun stopAutoVisualizer(key: String) {
        autoVisualizers.remove(key)?.apply { runCatching { enabled = false }; runCatching { release() } }
    }

    private fun isIncomingCallNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false
        val extras = notification.extras
        val title = extras.getCharSequence("android.title")?.toString()?.lowercase() ?: ""
        val text = extras.getCharSequence("android.text")?.toString()?.lowercase() ?: ""
        val pkg = sbn.packageName.lowercase()
        val isDialer = listOf("dialer", "phone", "telecom", "incallui", "truecaller").any { pkg.contains(it) }
        val hasCallText = text.contains("incoming call") || title.contains("incoming call")
        return isDialer && hasCallText && notification.category != Notification.CATEGORY_ALARM
    }

    private suspend fun fireHaptic(pattern: HapticPattern, intensity: Float, customSequence: CustomHapticSequence, attrs: VibrationAttributes) {
        if (!customSequence.isEmpty) { playCustomSequence(customSequence, attrs); return }
        val effect = buildEffect(pattern, intensity)
        withContext(Dispatchers.Main) { vibrator.vibrate(effect, attrs) }
    }

    private fun buildEffect(pattern: HapticPattern, intensity: Float): VibrationEffect {
        val hasComp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
        val hasAmp = vibrator.hasAmplitudeControl()
        val amp = (intensity * 255f).toInt().coerceIn(1, 255)
        if (hasComp) {
            return VibrationEffect.startComposition().apply {
                when (pattern) {
                    HapticPattern.CLICK -> addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    HapticPattern.TICK -> addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                    HapticPattern.HEAVY_CLICK -> { addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity); addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (intensity * 0.6f).coerceIn(0f, 1f), 40) }
                    HapticPattern.DOUBLE_CLICK -> { addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity); addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity, 80) }
                    HapticPattern.SOFT_BUMP -> { val p = if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)) VibrationEffect.Composition.PRIMITIVE_LOW_TICK else VibrationEffect.Composition.PRIMITIVE_TICK; addPrimitive(p, (intensity * 0.5f).coerceIn(0f, 1f)) }
                    HapticPattern.DOUBLE_TICK -> { addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity); addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity, 60) }
                }
            }.compose()
        }
        return when (pattern) {
            HapticPattern.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticPattern.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticPattern.HEAVY_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            HapticPattern.DOUBLE_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            HapticPattern.SOFT_BUMP -> if (hasAmp) VibrationEffect.createOneShot(30L, (amp * 0.5f).toInt().coerceIn(1, 255)) else VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticPattern.DOUBLE_TICK -> if (hasAmp) VibrationEffect.createWaveform(longArrayOf(0L, 30L, 60L, 30L), intArrayOf(0, amp, 0, amp), -1) else VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        }
    }

    private suspend fun playCustomSequence(seq: CustomHapticSequence, attrs: VibrationAttributes) {
        val sorted = seq.beats.sortedBy { it.offsetMs }
        val startMs = System.currentTimeMillis()
        for (beat in sorted) {
            val wait = beat.offsetMs - (System.currentTimeMillis() - startMs)
            if (wait > 0) delay(wait)
            withContext(Dispatchers.Main) { vibrator.vibrate(VibrationEffect.createOneShot(80L, beat.amplitude.coerceIn(1, 255)), attrs) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoVisualizers.values.forEach { runCatching { it.enabled = false }; runCatching { it.release() } }
        autoVisualizers.clear()
        scope.cancel()
    }

    companion object { private const val TAG = "NotifHapticService" }
}
