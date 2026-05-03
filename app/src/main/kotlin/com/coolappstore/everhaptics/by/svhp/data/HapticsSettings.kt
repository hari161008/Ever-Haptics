package com.coolappstore.everhaptics.by.svhp.data

import androidx.compose.runtime.Immutable
import com.coolappstore.everhaptics.by.svhp.haptics.HapticPattern

@Immutable
data class HapticsSettings(
    // Global master toggle
    val globalEnabled: Boolean = true,

    // Tap Haptics Default Settings
    val tapEnabled: Boolean = true,
    val intensity: Float = 1.0f,
    val pattern: HapticPattern = HapticPattern.Default,

    // Scroll Haptics Default Settings
    val scrollEnabled: Boolean = false,
    val scrollHapticEventsPerCm: Float = 1.0f, // haptic events per 1 cm scrolled
    val scrollIntensity: Float = 0.45f,
    val scrollIntensityEnabled: Boolean = false,
    val scrollPattern: HapticPattern = HapticPattern.TICK,
    val scrollVibrationsPerEvent: Float = 1f,
    val scrollVibrationsPerEventEnabled: Boolean = false,
    val scrollSpeedVibrationScale: Float = 0f,
    val scrollSpeedVibrationEnabled: Boolean = false,
    val scrollTailCutoffMs: Int = 0,
    val scrollTailCutoffEnabled: Boolean = false,
    val scrollHorizontalEnabled: Boolean = false,

    // Charging Vibration Settings
    val chargingVibEnabled: Boolean = false,
    val chargingVibOnConnect: Boolean = true,
    val chargingVibOnDisconnect: Boolean = false,
    val chargingVibDurationIndex: Int = 0, // 0=Short(2s), 1=Medium(2.5s), 2=Long(3s)
    val chargingVibIntensity: Float = 1.0f,

    // Button Haptics Settings
    val volumeHapticEnabled: Boolean = false,
    val volumeHapticPattern: HapticPattern = HapticPattern.TICK,
    val volumeHapticIntensity: Float = 0.7f,
    val powerHapticEnabled: Boolean = false,
    val powerHapticPattern: HapticPattern = HapticPattern.HEAVY_CLICK,
    val powerHapticIntensity: Float = 1.0f,
    val brightnessHapticEnabled: Boolean = false,
    val brightnessHapticPattern: HapticPattern = HapticPattern.TICK,
    val brightnessHapticIntensity: Float = 0.5f,

    // Navigation Bar Haptics (Home button)
    val navBarHapticEnabled: Boolean = false,
    val navBarHapticPattern: HapticPattern = HapticPattern.CLICK,
    val navBarHapticIntensity: Float = 0.8f,

    // Unlock Haptics
    val unlockHapticEnabled: Boolean = false,
    val unlockHapticPattern: HapticPattern = HapticPattern.DOUBLE_CLICK,
    val unlockHapticIntensity: Float = 0.8f,

    // Theme Default Settings
    val useDynamicColors: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val seedColor: Int = 0xFF6750A4.toInt(),

    // App Exclusion Settings
    val tapExcludedPackages: Set<String> = emptySet(),
    val scrollExcludedPackages: Set<String> = emptySet(),
) {
    companion object {
        const val MIN_SCROLL_EVENTS_PER_CM = 0.2f
        const val MAX_SCROLL_EVENTS_PER_CM = 5.0f
        const val MIN_SCROLL_VIBS_PER_EVENT = 1f
        const val MAX_SCROLL_VIBS_PER_EVENT = 3f
        const val MIN_SCROLL_SPEED_VIB_SCALE = 0f
        const val MAX_SCROLL_SPEED_VIB_SCALE = 1f
        const val MIN_SCROLL_TAIL_CUTOFF_MS = 0
        const val MAX_SCROLL_TAIL_CUTOFF_MS = 30
        val Default: HapticsSettings = HapticsSettings()
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
