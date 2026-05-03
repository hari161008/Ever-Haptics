package com.coolappstore.everhaptics.by.svhp.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.coolappstore.everhaptics.by.svhp.HapticksApp
import com.coolappstore.everhaptics.by.svhp.data.HapticsPreferences
import com.coolappstore.everhaptics.by.svhp.data.HapticsSettings
import com.coolappstore.everhaptics.by.svhp.data.ThemeMode
import com.coolappstore.everhaptics.by.svhp.haptics.HapticEngine
import com.coolappstore.everhaptics.by.svhp.haptics.HapticPattern
import com.coolappstore.everhaptics.by.svhp.service.HapticsAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeelEveryTapViewModel(
    application: Application,
    private val preferences: HapticsPreferences,
    private val engine: HapticEngine,
) : AndroidViewModel(application) {

    val settings: StateFlow<HapticsSettings> = preferences.settings
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HapticsSettings.Default,
        )

    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    init { refreshServiceState() }

    fun refreshServiceState() {
        _isServiceEnabled.value = isAccessibilityServiceEnabled(getApplication())
    }

    // Global
    fun setGlobalEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setGlobalEnabled(enabled) } }

    fun setTapEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setTapEnabled(enabled) } }
    fun commitIntensity(intensity: Float) { viewModelScope.launch { preferences.setIntensity(intensity) } }
    fun setPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setPattern(pattern) } }
    fun setScrollEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setScrollEnabled(enabled) } }
    fun commitScrollIntensity(intensity: Float) { viewModelScope.launch { preferences.setScrollIntensity(intensity) } }
    fun setScrollIntensityEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setScrollIntensityEnabled(enabled) } }
    fun commitScrollHapticEventsPerCm(value: Float) { viewModelScope.launch { preferences.setScrollHapticEventsPerCm(value) } }
    fun setScrollPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setScrollPattern(pattern) } }
    fun commitScrollVibrationsPerEvent(value: Float) { viewModelScope.launch { preferences.setScrollVibrationsPerEvent(value) } }
    fun setScrollVibrationsPerEventEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setScrollVibrationsPerEventEnabled(enabled) } }
    fun commitScrollSpeedVibScale(value: Float) { viewModelScope.launch { preferences.setScrollSpeedVibrationScale(value) } }
    fun setScrollSpeedVibrationEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setScrollSpeedVibrationEnabled(enabled) } }
    fun commitScrollTailCutoffMs(value: Int) { viewModelScope.launch { preferences.setScrollTailCutoffMs(value) } }
    fun setScrollTailCutoffEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setScrollTailCutoffEnabled(enabled) } }
    fun setScrollHorizontalEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setScrollHorizontalEnabled(enabled) } }
    fun setTapExcludedPackages(packages: Set<String>) { viewModelScope.launch { preferences.setTapExcludedPackages(packages) } }
    fun setScrollExcludedPackages(packages: Set<String>) { viewModelScope.launch { preferences.setScrollExcludedPackages(packages) } }

    fun resetTapDefaults() {
        viewModelScope.launch {
            preferences.setIntensity(HapticsSettings.Default.intensity)
            preferences.setPattern(HapticsSettings.Default.pattern)
        }
    }

    fun resetScrollDefaults() {
        viewModelScope.launch {
            preferences.setScrollHapticEventsPerCm(HapticsSettings.Default.scrollHapticEventsPerCm)
            preferences.setScrollIntensity(HapticsSettings.Default.scrollIntensity)
            preferences.setScrollIntensityEnabled(HapticsSettings.Default.scrollIntensityEnabled)
            preferences.setScrollVibrationsPerEvent(HapticsSettings.Default.scrollVibrationsPerEvent)
            preferences.setScrollVibrationsPerEventEnabled(HapticsSettings.Default.scrollVibrationsPerEventEnabled)
            preferences.setScrollSpeedVibrationScale(HapticsSettings.Default.scrollSpeedVibrationScale)
            preferences.setScrollSpeedVibrationEnabled(HapticsSettings.Default.scrollSpeedVibrationEnabled)
            preferences.setScrollTailCutoffMs(HapticsSettings.Default.scrollTailCutoffMs)
            preferences.setScrollTailCutoffEnabled(HapticsSettings.Default.scrollTailCutoffEnabled)
            preferences.setScrollPattern(HapticsSettings.Default.scrollPattern)
            preferences.setScrollHorizontalEnabled(HapticsSettings.Default.scrollHorizontalEnabled)
        }
    }

    // Charging
    fun setChargingVibEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setChargingVibEnabled(enabled) } }
    fun setChargingVibOnConnect(enabled: Boolean) { viewModelScope.launch { preferences.setChargingVibOnConnect(enabled) } }
    fun setChargingVibOnDisconnect(enabled: Boolean) { viewModelScope.launch { preferences.setChargingVibOnDisconnect(enabled) } }
    fun setChargingVibDurationIndex(index: Int) { viewModelScope.launch { preferences.setChargingVibDurationIndex(index) } }
    fun commitChargingVibIntensity(intensity: Float) { viewModelScope.launch { preferences.setChargingVibIntensity(intensity) } }
    fun resetChargingDefaults() {
        viewModelScope.launch {
            preferences.setChargingVibDurationIndex(HapticsSettings.Default.chargingVibDurationIndex)
            preferences.setChargingVibIntensity(HapticsSettings.Default.chargingVibIntensity)
            preferences.setChargingVibOnConnect(HapticsSettings.Default.chargingVibOnConnect)
            preferences.setChargingVibOnDisconnect(HapticsSettings.Default.chargingVibOnDisconnect)
        }
    }

    // Volume haptics
    fun setVolumeHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setVolumeHapticEnabled(enabled) } }
    fun setVolumeHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setVolumeHapticPattern(pattern) } }
    fun commitVolumeHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setVolumeHapticIntensity(intensity) } }

    // Power haptics
    fun setPowerHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setPowerHapticEnabled(enabled) } }
    fun setPowerHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setPowerHapticPattern(pattern) } }
    fun commitPowerHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setPowerHapticIntensity(intensity) } }

    // Brightness haptics
    fun setBrightnessHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setBrightnessHapticEnabled(enabled) } }
    fun setBrightnessHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setBrightnessHapticPattern(pattern) } }
    fun commitBrightnessHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setBrightnessHapticIntensity(intensity) } }

    // NavBar haptics
    fun setNavBarHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setNavBarHapticEnabled(enabled) } }
    fun setNavBarHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setNavBarHapticPattern(pattern) } }
    fun commitNavBarHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setNavBarHapticIntensity(intensity) } }

    // Unlock haptics
    fun setUnlockHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setUnlockHapticEnabled(enabled) } }
    fun setUnlockHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setUnlockHapticPattern(pattern) } }
    fun commitUnlockHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setUnlockHapticIntensity(intensity) } }

    fun resetButtonHapticsDefaults() {
        viewModelScope.launch {
            preferences.setVolumeHapticPattern(HapticsSettings.Default.volumeHapticPattern)
            preferences.setVolumeHapticIntensity(HapticsSettings.Default.volumeHapticIntensity)
            preferences.setPowerHapticPattern(HapticsSettings.Default.powerHapticPattern)
            preferences.setPowerHapticIntensity(HapticsSettings.Default.powerHapticIntensity)
            preferences.setBrightnessHapticPattern(HapticsSettings.Default.brightnessHapticPattern)
            preferences.setBrightnessHapticIntensity(HapticsSettings.Default.brightnessHapticIntensity)
            preferences.setNavBarHapticPattern(HapticsSettings.Default.navBarHapticPattern)
            preferences.setNavBarHapticIntensity(HapticsSettings.Default.navBarHapticIntensity)
            preferences.setUnlockHapticPattern(HapticsSettings.Default.unlockHapticPattern)
            preferences.setUnlockHapticIntensity(HapticsSettings.Default.unlockHapticIntensity)
        }
    }

    fun testHaptic() {
        val s = settings.value
        engine.play(s.pattern, s.intensity)
    }

    fun testScrollHaptic() {
        val s = settings.value
        viewModelScope.launch {
            val i = s.scrollIntensity
            engine.play(s.scrollPattern, i, 0L)
            delay(52)
            engine.play(s.scrollPattern, i, 0L)
            delay(52)
            engine.play(s.scrollPattern, i, 0L)
        }
    }

    fun testChargingHaptic() {
        val s = settings.value
        val amplitude = (s.chargingVibIntensity * 255).toInt().coerceIn(1, 255)
        val durationMs = when (s.chargingVibDurationIndex) {
            0 -> 2000L
            1 -> 2500L
            else -> 3000L
        }
        engine.playOneShot(durationMs, amplitude)
    }

    fun testVolumeHaptic() {
        val s = settings.value
        engine.play(s.volumeHapticPattern, s.volumeHapticIntensity)
    }

    fun testPowerHaptic() {
        val s = settings.value
        engine.play(s.powerHapticPattern, s.powerHapticIntensity)
    }

    fun testBrightnessHaptic() {
        val s = settings.value
        engine.play(s.brightnessHapticPattern, s.brightnessHapticIntensity)
    }

    fun testNavBarHaptic() {
        val s = settings.value
        engine.play(s.navBarHapticPattern, s.navBarHapticIntensity)
    }

    fun testUnlockHaptic() {
        val s = settings.value
        engine.play(s.unlockHapticPattern, s.unlockHapticIntensity)
    }

    fun setUseDynamicColors(enabled: Boolean) { viewModelScope.launch { preferences.setUseDynamicColors(enabled) } }
    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { preferences.setThemeMode(mode) } }
    fun setAmoledBlack(enabled: Boolean) { viewModelScope.launch { preferences.setAmoledBlack(enabled) } }
    fun setSeedColor(color: Int) { viewModelScope.launch { preferences.setSeedColor(color) } }

    companion object {
        private fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager ?: return false
            if (!manager.isEnabled) return false
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val expectedComponent = ComponentName(
                context, HapticsAccessibilityService::class.java,
            ).flattenToString()
            return enabledServices.split(':').any { serviceId ->
                val normalized = serviceId.trim()
                normalized.equals(expectedComponent, ignoreCase = true) ||
                    normalized.endsWith(HapticsAccessibilityService::class.java.name, ignoreCase = true)
            }
        }

        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as HapticksApp
                return FeelEveryTapViewModel(app, app.preferences, app.hapticEngine) as T
            }
        }
    }
}
