package com.hapticks.app.haptics

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class HapticEngine(context: Context) {

    private val vibrator: Vibrator = run {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    }

    val hasVibrator: Boolean = vibrator.hasVibrator()
    private val hasAmplitudeControl: Boolean = vibrator.hasAmplitudeControl()

    private val touchAttrs: VibrationAttributes =
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)

    private val primitiveSupport: Map<HapticPattern, Boolean> =
        HapticPattern.entries.associateWith { pattern ->
            vibrator.areAllPrimitivesSupported(*primitivesRequired(pattern))
        }

    private val effectCache: ConcurrentHashMap<Int, VibrationEffect> =
        ConcurrentHashMap(HapticPattern.entries.size * INTENSITY_BUCKETS)
    private val lastPlayMs: ConcurrentHashMap<HapticPattern, AtomicLong> =
        ConcurrentHashMap(HapticPattern.entries.size)

    fun play(pattern: HapticPattern, intensity: Float, throttleMs: Long = 0L): Boolean {
        if (!hasVibrator) return false
        if (intensity <= MIN_AUDIBLE_INTENSITY) return false
        if (throttleMs > 0L) {
            val now = System.currentTimeMillis()
            val lastMs = lastPlayMs.getOrPut(pattern) { AtomicLong(0L) }
            val prev = lastMs.get()
            if (now - prev < throttleMs) return false
            lastMs.compareAndSet(prev, now)
        }
        val clamped = intensity.coerceIn(0f, 1f)
        val effect = effectFor(pattern, clamped)
        vibrator.vibrate(effect, touchAttrs)
        return true
    }

    fun playOneShot(durationMs: Long, amplitude: Int) {
        if (!hasVibrator) return
        val effect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
        vibrator.vibrate(effect, touchAttrs)
    }

    fun playStrong(pattern: HapticPattern) {
        if (!hasVibrator) return
        vibrator.vibrate(effectFor(pattern, 1.0f), touchAttrs)
    }

    private fun effectFor(pattern: HapticPattern, intensity: Float): VibrationEffect {
        val bucket = ((intensity * (INTENSITY_BUCKETS - 1)) + 0.5f).toInt().coerceIn(0, INTENSITY_BUCKETS - 1)
        val key = pattern.ordinal * INTENSITY_BUCKETS + bucket
        effectCache[key]?.let { return it }
        val bucketIntensity = bucket.toFloat() / (INTENSITY_BUCKETS - 1)
        val built = buildEffect(pattern, bucketIntensity)
        return effectCache.putIfAbsent(key, built) ?: built
    }

    private fun buildEffect(pattern: HapticPattern, intensity: Float): VibrationEffect {
        if (primitiveSupport[pattern] == true) {
            val composition = VibrationEffect.startComposition()
            when (pattern) {
                HapticPattern.CLICK -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                HapticPattern.TICK -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                HapticPattern.HEAVY_CLICK -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (intensity * 0.6f).coerceIn(0f, 1f), 40)
                }
                HapticPattern.DOUBLE_CLICK -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity, 80)
                }
                HapticPattern.SOFT_BUMP -> composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, intensity)
                HapticPattern.DOUBLE_TICK -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity, 60)
                }
            }
            return composition.compose()
        }
        val effectId = when (pattern) {
            HapticPattern.CLICK -> VibrationEffect.EFFECT_CLICK
            HapticPattern.TICK -> VibrationEffect.EFFECT_TICK
            HapticPattern.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
            HapticPattern.DOUBLE_CLICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
            HapticPattern.SOFT_BUMP -> VibrationEffect.EFFECT_TICK
            HapticPattern.DOUBLE_TICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
        }
        return if (hasAmplitudeControl && intensity < 0.9f) {
            VibrationEffect.createOneShot(20L, (intensity * 255f).toInt().coerceIn(1, 255))
        } else {
            VibrationEffect.createPredefined(effectId)
        }
    }

    private companion object {
        const val MIN_AUDIBLE_INTENSITY = 0.01f
        const val INTENSITY_BUCKETS = 21
        fun primitivesRequired(pattern: HapticPattern): IntArray = when (pattern) {
            HapticPattern.CLICK, HapticPattern.DOUBLE_CLICK, HapticPattern.HEAVY_CLICK ->
                intArrayOf(VibrationEffect.Composition.PRIMITIVE_CLICK)
            HapticPattern.TICK, HapticPattern.DOUBLE_TICK ->
                intArrayOf(VibrationEffect.Composition.PRIMITIVE_TICK)
            HapticPattern.SOFT_BUMP ->
                intArrayOf(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
        }
    }
}
