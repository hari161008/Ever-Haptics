package com.hapticks.app.edge

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hapticks.app.HapticksApp
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticPattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Builds [VibrationEffect]s for scroll-edge feedback (accessibility / LSPosed hooks).
 */
object EdgeHapticsBridge {

    enum class EdgeVibrationEvent { EDGE_HIT, RELEASE, ABSORB }

    sealed class TestResult {
        object Fired : TestResult()
        object NoVibrator : TestResult()
    }

    /** In-app edge waveform test (dedicated test button only). */
    fun testEdgeHaptic(context: Context): TestResult {
        val vibrator = resolveVibrator(context) ?: return TestResult.NoVibrator
        if (!vibrator.hasVibrator()) return TestResult.NoVibrator

        val attrs = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
        return try {
            val hapticksApp = context.applicationContext as HapticksApp
            val s = hapticksApp.preferences.settings
                .let { flow ->
                    try {
                        runBlocking { flow.first() }
                    } catch (_: Throwable) {
                        HapticsSettings.Default
                    }
                }
            vibrator.vibrate(buildEdgeEffect(s.edgePattern, s.edgeIntensity), attrs)
            TestResult.Fired
        } catch (_: Throwable) {
            TestResult.NoVibrator
        }
    }

    fun edgeVibrationEffect(pattern: HapticPattern, intensity: Float): VibrationEffect =
        edgeVibrationEffect(pattern, intensity, EdgeVibrationEvent.EDGE_HIT)

    fun edgeVibrationEffect(
        pattern: HapticPattern,
        intensity: Float,
        eventType: EdgeVibrationEvent,
    ): VibrationEffect {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        return when (eventType) {
            EdgeVibrationEvent.EDGE_HIT -> buildEdgeEffect(pattern, clampedIntensity)
            EdgeVibrationEvent.RELEASE -> buildReleaseEffect(clampedIntensity)
            EdgeVibrationEvent.ABSORB -> buildAbsorbEffect(clampedIntensity)
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? = try {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        mgr?.defaultVibrator
    } catch (_: Throwable) {
        null
    }

    private fun buildEdgeEffect(pattern: HapticPattern, intensity: Float): VibrationEffect {
        return try {
            val composition = VibrationEffect.startComposition()
            when (pattern) {
                HapticPattern.CLICK ->
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                HapticPattern.TICK ->
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                HapticPattern.HEAVY_CLICK -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    composition.addPrimitive(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        (intensity * 0.6f).coerceIn(0f, 1f),
                        40,
                    )
                }
                HapticPattern.DOUBLE_CLICK -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity, 80)
                }
                HapticPattern.SOFT_BUMP ->
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, intensity)
                HapticPattern.DOUBLE_TICK -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity, 60)
                }
            }
            composition.compose()
        } catch (_: Throwable) {
            val effectId = when (pattern) {
                HapticPattern.CLICK -> VibrationEffect.EFFECT_CLICK
                HapticPattern.TICK -> VibrationEffect.EFFECT_TICK
                HapticPattern.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
                HapticPattern.DOUBLE_CLICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
                HapticPattern.SOFT_BUMP -> VibrationEffect.EFFECT_TICK
                HapticPattern.DOUBLE_TICK -> VibrationEffect.EFFECT_DOUBLE_CLICK
            }
            VibrationEffect.createPredefined(effectId)
        }
    }

    private fun buildReleaseEffect(intensity: Float): VibrationEffect {
        return try {
            VibrationEffect.startComposition()
                .addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                    (intensity * 0.7f).coerceIn(0.1f, 1f),
                )
                .compose()
        } catch (_: Throwable) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }
    }

    private fun buildAbsorbEffect(intensity: Float): VibrationEffect {
        return try {
            VibrationEffect.startComposition()
                .addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    intensity.coerceIn(0.15f, 1f),
                )
                .addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                    (intensity * 0.5f).coerceIn(0.1f, 1f),
                    30,
                )
                .compose()
        } catch (_: Throwable) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
    }
}
