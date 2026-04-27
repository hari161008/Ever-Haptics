package com.hapticks.app.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hapticks.app.HapticksApp
import com.hapticks.app.data.HapticsPreferences
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.data.ThemeMode
import com.hapticks.app.haptics.HapticEngine
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.service.HapticsAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the Feel Every Tap flow: tap toggle, pattern, and intensity shared with
 * [HapticsAccessibilityService] via [HapticsPreferences].
 */
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

    init {
        refreshServiceState()
    }

    fun refreshServiceState() {
        _isServiceEnabled.value = isAccessibilityServiceEnabled(getApplication())
    }

    fun setTapEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setTapEnabled(enabled) }
    }

    fun commitIntensity(intensity: Float) {
        viewModelScope.launch { preferences.setIntensity(intensity) }
    }

    fun setPattern(pattern: HapticPattern) {
        viewModelScope.launch { preferences.setPattern(pattern) }
    }

    fun setScrollEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setScrollEnabled(enabled) }
    }

    fun commitScrollIntensity(intensity: Float) {
        viewModelScope.launch { preferences.setScrollIntensity(intensity) }
    }

    fun commitScrollHapticDensity(eventsPerHundredPx: Float) {
        viewModelScope.launch { preferences.setScrollHapticEventsPerHundredPx(eventsPerHundredPx) }
    }

    fun setScrollPattern(pattern: HapticPattern) {
        viewModelScope.launch { preferences.setScrollPattern(pattern) }
    }

    fun commitScrollVibrationsPerEvent(value: Float) {
        viewModelScope.launch { preferences.setScrollVibrationsPerEvent(value) }
    }

    fun commitScrollSpeedVibScale(value: Float) {
        viewModelScope.launch { preferences.setScrollSpeedVibrationScale(value) }
    }

    fun commitScrollTailCutoffMs(value: Int) {
        viewModelScope.launch { preferences.setScrollTailCutoffMs(value) }
    }

    fun setTapExcludedPackages(packages: Set<String>) {
        viewModelScope.launch { preferences.setTapExcludedPackages(packages) }
    }

    fun setScrollExcludedPackages(packages: Set<String>) {
        viewModelScope.launch { preferences.setScrollExcludedPackages(packages) }
    }

    fun resetTapDefaults() {
        viewModelScope.launch {
            preferences.setIntensity(HapticsSettings.Default.intensity)
            preferences.setPattern(HapticsSettings.Default.pattern)
        }
    }

    fun resetScrollDefaults() {
        viewModelScope.launch {
            preferences.setScrollHapticEventsPerHundredPx(HapticsSettings.Default.scrollHapticEventsPerHundredPx)
            preferences.setScrollIntensity(HapticsSettings.Default.scrollIntensity)
            preferences.setScrollVibrationsPerEvent(HapticsSettings.Default.scrollVibrationsPerEvent)
            preferences.setScrollSpeedVibrationScale(HapticsSettings.Default.scrollSpeedVibrationScale)
            preferences.setScrollTailCutoffMs(HapticsSettings.Default.scrollTailCutoffMs)
            preferences.setScrollPattern(HapticsSettings.Default.scrollPattern)
        }
    }

    /** Plays the configured tap pattern (for the dedicated test control only). */
    fun testHaptic() {
        val s = settings.value
        engine.play(s.pattern, s.intensity)
    }

    /** Plays a short burst of scroll haptics (for the dedicated test control only). */
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

    fun setUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch { preferences.setUseDynamicColors(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { preferences.setAmoledBlack(enabled) }
    }

    fun setSeedColor(color: Int) {
        viewModelScope.launch { preferences.setSeedColor(color) }
    }

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
                context,
                HapticsAccessibilityService::class.java,
            ).flattenToString()

            return enabledServices
                .split(':')
                .any { serviceId ->
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
