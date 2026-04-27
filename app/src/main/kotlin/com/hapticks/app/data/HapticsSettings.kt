package com.hapticks.app.data

import androidx.compose.runtime.Immutable
import com.hapticks.app.haptics.HapticPattern

@Immutable
data class HapticsSettings(
    // Tap Haptics Default Settings
    val tapEnabled: Boolean = true,
    val intensity: Float = 1.0f,
    val pattern: HapticPattern = HapticPattern.Default,

    // Scroll Haptics Default Settings
    val scrollEnabled: Boolean = false,
    val scrollHapticEventsPerHundredPx: Float = 2.2f,
    val scrollIntensity: Float = 0.45f,
    val scrollPattern: HapticPattern = HapticPattern.TICK,
    val scrollVibrationsPerEvent: Float = 1f,
    val scrollSpeedVibrationScale: Float = 0f,
    val scrollTailCutoffMs: Int = 0,

    // Edge Haptics Default Settings
    val edgePattern: HapticPattern = HapticPattern.SOFT_BUMP,
    val edgeIntensity: Float = 1.0f,
    val a11yScrollBoundEdge: Boolean = false,
    val edgeLsposedLibxposedPath: Boolean = false,

    // Theme Default Settings
    val useDynamicColors: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val seedColor: Int = 0xFF6750A4.toInt(),
) {
    companion object {
        const val MIN_SCROLL_EVENTS_PER_HUNDRED_PX = 0.1f
        const val MAX_SCROLL_EVENTS_PER_HUNDRED_PX = 20f
        const val MIN_SCROLL_VIBS_PER_EVENT = 1f
        const val MAX_SCROLL_VIBS_PER_EVENT = 5f
        const val MIN_SCROLL_SPEED_VIB_SCALE = 0f
        const val MAX_SCROLL_SPEED_VIB_SCALE = 1f
        const val MIN_SCROLL_TAIL_CUTOFF_MS = 0
        const val MAX_SCROLL_TAIL_CUTOFF_MS = 500
        val Default: HapticsSettings = HapticsSettings()
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
