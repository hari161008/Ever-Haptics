package com.hapticks.app.data

import androidx.compose.runtime.Immutable
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticPattern

@Immutable
data class HapticsSettings(
    val globalEnabled: Boolean = true,
    val tapEnabled: Boolean = true,
    val intensity: Float = 1.0f,
    val pattern: HapticPattern = HapticPattern.Default,
    val tapHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    val scrollEnabled: Boolean = false,
    val scrollHapticEventsPerCm: Float = 1.0f,
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
    val edgePattern: HapticPattern = HapticPattern.SOFT_BUMP,
    val edgeIntensity: Float = 1.0f,
    val a11yScrollBoundEdge: Boolean = false,
    val edgeLsposedLibxposedPath: Boolean = false,
    val tapExcludedPackages: Set<String> = emptySet(),
    val scrollExcludedPackages: Set<String> = emptySet(),
    val edgeExcludedPackages: Set<String> = emptySet(),
    // Charging
    val chargingVibEnabled: Boolean = false,
    val chargingVibOnConnect: Boolean = true,
    val chargingVibOnDisconnect: Boolean = false,
    val chargingVibPattern: HapticPattern = HapticPattern.HEAVY_CLICK,
    val chargingVibIntensity: Float = 1.0f,
    val chargingVibCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    // Volume
    val volumeHapticEnabled: Boolean = false,
    val volumeHapticPattern: HapticPattern = HapticPattern.TICK,
    val volumeHapticIntensity: Float = 0.7f,
    val volumeHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    // Power
    val powerHapticEnabled: Boolean = false,
    val powerHapticPattern: HapticPattern = HapticPattern.HEAVY_CLICK,
    val powerHapticIntensity: Float = 1.0f,
    val powerHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    // Brightness
    val brightnessHapticEnabled: Boolean = false,
    val brightnessHapticPattern: HapticPattern = HapticPattern.TICK,
    val brightnessHapticIntensity: Float = 0.5f,
    val brightnessHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    // NavBar
    val navBarHapticEnabled: Boolean = false,
    val navBarHapticPattern: HapticPattern = HapticPattern.CLICK,
    val navBarHapticIntensity: Float = 0.8f,
    val navBarHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    // Unlock
    val unlockHapticEnabled: Boolean = false,
    val unlockHapticPattern: HapticPattern = HapticPattern.DOUBLE_CLICK,
    val unlockHapticIntensity: Float = 0.8f,
    val unlockHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    // Keyboard
    val keyboardHapticEnabled: Boolean = false,
    val keyboardHapticPattern: HapticPattern = HapticPattern.TICK,
    val keyboardHapticIntensity: Float = 0.5f,
    val keyboardHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    // Theme
    val useDynamicColors: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val seedColor: Int = 0xFF6750A4.toInt(),
    // Notification / call / alarm haptics
    val callHapticEnabled: Boolean = false,
    val callHapticPattern: HapticPattern = HapticPattern.HEAVY_CLICK,
    val callHapticIntensity: Float = 1.0f,
    val callHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    val notifHapticEnabled: Boolean = false,
    val notifHapticPattern: HapticPattern = HapticPattern.CLICK,
    val notifHapticIntensity: Float = 0.7f,
    val notifHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
    val alarmHapticEnabled: Boolean = false,
    val alarmHapticPattern: HapticPattern = HapticPattern.DOUBLE_CLICK,
    val alarmHapticIntensity: Float = 1.0f,
    val alarmHapticCustomSequence: CustomHapticSequence = CustomHapticSequence(),
) {
    companion object {
        const val MIN_SCROLL_EVENTS_PER_CM = 0.1f   // 1 haptic every 10 cm
        const val MAX_SCROLL_EVENTS_PER_CM = 5.0f   // 1 haptic every 0.2 cm
        const val MIN_SCROLL_EVENTS_PER_HUNDRED_PX = 0.1f
        const val MAX_SCROLL_EVENTS_PER_HUNDRED_PX = 20f
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
