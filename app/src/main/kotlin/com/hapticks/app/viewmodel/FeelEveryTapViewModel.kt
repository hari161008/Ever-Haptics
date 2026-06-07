package com.hapticks.app.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
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
import com.hapticks.app.haptics.CustomHapticSequence
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

class FeelEveryTapViewModel(
    application: Application,
    private val preferences: HapticsPreferences,
    private val engine: HapticEngine,
) : AndroidViewModel(application) {

    val settings: StateFlow<HapticsSettings> = preferences.settings
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HapticsSettings.Default)

    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    private val _isBatterySaverActive = MutableStateFlow(false)
    val isBatterySaverActive: StateFlow<Boolean> = _isBatterySaverActive.asStateFlow()

    private val batterySaverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                _isBatterySaverActive.value = pm.isPowerSaveMode
            }
        }
    }

    init {
        refreshServiceState()
        val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as PowerManager
        _isBatterySaverActive.value = pm.isPowerSaveMode
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        getApplication<Application>().registerReceiver(batterySaverReceiver, filter)
    }

    override fun onCleared() {
        super.onCleared()
        try { getApplication<Application>().unregisterReceiver(batterySaverReceiver) } catch (_: Exception) {}
    }

    fun refreshServiceState() { _isServiceEnabled.value = isAccessibilityServiceEnabled(getApplication()) }

    fun setGlobalEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setGlobalEnabled(enabled) } }
    fun setTapEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setTapEnabled(enabled) } }
    fun commitIntensity(intensity: Float) { viewModelScope.launch { preferences.setIntensity(intensity) } }
    fun setPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setPattern(pattern) } }
    fun setTapHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setTapHapticCustomSequence(seq) } }
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
    fun setEdgePattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setEdgePattern(pattern) } }
    fun commitEdgeIntensity(intensity: Float) { viewModelScope.launch { preferences.setEdgeIntensity(intensity) } }
    fun setA11yScrollBoundEdge(enabled: Boolean) { viewModelScope.launch { preferences.setA11yScrollBoundEdge(enabled) } }
    fun setEdgeLsposedLibxposedPath(enabled: Boolean) { viewModelScope.launch { preferences.setEdgeLsposedLibxposedPath(enabled) } }
    fun setTapExcludedPackages(packages: Set<String>) { viewModelScope.launch { preferences.setTapExcludedPackages(packages) } }
    fun setScrollExcludedPackages(packages: Set<String>) { viewModelScope.launch { preferences.setScrollExcludedPackages(packages) } }
    fun setEdgeExcludedPackages(packages: Set<String>) { viewModelScope.launch { preferences.setEdgeExcludedPackages(packages) } }
    fun setUseDynamicColors(enabled: Boolean) { viewModelScope.launch { preferences.setUseDynamicColors(enabled) } }
    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { preferences.setThemeMode(mode) } }
    fun setAmoledBlack(enabled: Boolean) { viewModelScope.launch { preferences.setAmoledBlack(enabled) } }
    fun setSeedColor(color: Int) { viewModelScope.launch { preferences.setSeedColor(color) } }

    fun resetTapDefaults() {
        viewModelScope.launch {
            preferences.setIntensity(HapticsSettings.Default.intensity)
            preferences.setPattern(HapticsSettings.Default.pattern)
            preferences.setTapHapticCustomSequence(HapticsSettings.Default.tapHapticCustomSequence)
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
    fun resetEdgeDefaults() {
        viewModelScope.launch {
            preferences.setEdgePattern(HapticsSettings.Default.edgePattern)
            preferences.setEdgeIntensity(HapticsSettings.Default.edgeIntensity)
        }
    }

    // Charging
    fun setChargingVibEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setChargingVibEnabled(enabled) } }
    fun setChargingVibOnConnect(enabled: Boolean) { viewModelScope.launch { preferences.setChargingVibOnConnect(enabled) } }
    fun setChargingVibOnDisconnect(enabled: Boolean) { viewModelScope.launch { preferences.setChargingVibOnDisconnect(enabled) } }
    fun setChargingVibPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setChargingVibPattern(pattern) } }
    fun commitChargingVibIntensity(intensity: Float) { viewModelScope.launch { preferences.setChargingVibIntensity(intensity) } }
    fun setChargingVibCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setChargingVibCustomSequence(seq) } }
    fun resetChargingDefaults() {
        viewModelScope.launch {
            preferences.setChargingVibPattern(HapticsSettings.Default.chargingVibPattern)
            preferences.setChargingVibIntensity(HapticsSettings.Default.chargingVibIntensity)
            preferences.setChargingVibOnConnect(HapticsSettings.Default.chargingVibOnConnect)
            preferences.setChargingVibOnDisconnect(HapticsSettings.Default.chargingVibOnDisconnect)
            preferences.setChargingVibCustomSequence(HapticsSettings.Default.chargingVibCustomSequence)
        }
    }

    // Volume
    fun setVolumeHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setVolumeHapticEnabled(enabled) } }
    fun setVolumeHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setVolumeHapticPattern(pattern) } }
    fun commitVolumeHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setVolumeHapticIntensity(intensity) } }
    fun setVolumeHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setVolumeHapticCustomSequence(seq) } }
    // Power
    fun setPowerHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setPowerHapticEnabled(enabled) } }
    fun setPowerHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setPowerHapticPattern(pattern) } }
    fun commitPowerHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setPowerHapticIntensity(intensity) } }
    fun setPowerHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setPowerHapticCustomSequence(seq) } }
    // Brightness
    fun setBrightnessHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setBrightnessHapticEnabled(enabled) } }
    fun setBrightnessHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setBrightnessHapticPattern(pattern) } }
    fun commitBrightnessHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setBrightnessHapticIntensity(intensity) } }
    fun setBrightnessHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setBrightnessHapticCustomSequence(seq) } }
    // NavBar
    fun setNavBarHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setNavBarHapticEnabled(enabled) } }
    fun setNavBarHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setNavBarHapticPattern(pattern) } }
    fun commitNavBarHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setNavBarHapticIntensity(intensity) } }
    fun setNavBarHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setNavBarHapticCustomSequence(seq) } }
    // Unlock
    fun setUnlockHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setUnlockHapticEnabled(enabled) } }
    fun setUnlockHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setUnlockHapticPattern(pattern) } }
    fun commitUnlockHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setUnlockHapticIntensity(intensity) } }
    fun setUnlockHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setUnlockHapticCustomSequence(seq) } }

    fun setBatterySaverDetectionEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setBatterySaverDetectionEnabled(enabled) } }
    fun setMusicHapticsEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setMusicHapticsEnabled(enabled) } }
    fun setMusicHapticsSource(source: com.hapticks.app.data.MusicHapticsSource) { viewModelScope.launch { preferences.setMusicHapticsSource(source) } }
    fun commitMusicHapticsSensitivity(value: Float) { viewModelScope.launch { preferences.setMusicHapticsSensitivity(value) } }
    fun commitMusicHapticsStrength(value: Float) { viewModelScope.launch { preferences.setMusicHapticsStrength(value) } }

    fun resetButtonHapticsDefaults() {
        viewModelScope.launch {
            preferences.setVolumeHapticEnabled(HapticsSettings.Default.volumeHapticEnabled)
            preferences.setVolumeHapticPattern(HapticsSettings.Default.volumeHapticPattern)
            preferences.setVolumeHapticIntensity(HapticsSettings.Default.volumeHapticIntensity)
            preferences.setVolumeHapticCustomSequence(HapticsSettings.Default.volumeHapticCustomSequence)
            preferences.setPowerHapticEnabled(HapticsSettings.Default.powerHapticEnabled)
            preferences.setPowerHapticPattern(HapticsSettings.Default.powerHapticPattern)
            preferences.setPowerHapticIntensity(HapticsSettings.Default.powerHapticIntensity)
            preferences.setPowerHapticCustomSequence(HapticsSettings.Default.powerHapticCustomSequence)
            preferences.setBrightnessHapticEnabled(HapticsSettings.Default.brightnessHapticEnabled)
            preferences.setBrightnessHapticPattern(HapticsSettings.Default.brightnessHapticPattern)
            preferences.setBrightnessHapticIntensity(HapticsSettings.Default.brightnessHapticIntensity)
            preferences.setBrightnessHapticCustomSequence(HapticsSettings.Default.brightnessHapticCustomSequence)
        }
    }
    fun resetNavBarDefaults() {
        viewModelScope.launch {
            preferences.setNavBarHapticPattern(HapticsSettings.Default.navBarHapticPattern)
            preferences.setNavBarHapticIntensity(HapticsSettings.Default.navBarHapticIntensity)
            preferences.setNavBarHapticCustomSequence(HapticsSettings.Default.navBarHapticCustomSequence)
        }
    }
    fun resetUnlockDefaults() {
        viewModelScope.launch {
            preferences.setUnlockHapticPattern(HapticsSettings.Default.unlockHapticPattern)
            preferences.setUnlockHapticIntensity(HapticsSettings.Default.unlockHapticIntensity)
            preferences.setUnlockHapticCustomSequence(HapticsSettings.Default.unlockHapticCustomSequence)
        }
    }

    // Call haptics
    fun setCallHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setCallHapticEnabled(enabled) } }
    fun setCallHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setCallHapticPattern(pattern) } }
    fun commitCallHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setCallHapticIntensity(intensity) } }
    fun setCallHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setCallHapticCustomSequence(seq) } }
    // Notification haptics
    fun setNotifHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setNotifHapticEnabled(enabled) } }
    fun setNotifHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setNotifHapticPattern(pattern) } }
    fun commitNotifHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setNotifHapticIntensity(intensity) } }
    fun setNotifHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setNotifHapticCustomSequence(seq) } }
    // Alarm haptics
    fun setAlarmHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setAlarmHapticEnabled(enabled) } }
    fun setAlarmHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setAlarmHapticPattern(pattern) } }
    fun commitAlarmHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setAlarmHapticIntensity(intensity) } }
    fun setAlarmHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setAlarmHapticCustomSequence(seq) } }

    // Keyboard
    fun setKeyboardHapticEnabled(enabled: Boolean) { viewModelScope.launch { preferences.setKeyboardHapticEnabled(enabled) } }
    fun setKeyboardHapticPattern(pattern: HapticPattern) { viewModelScope.launch { preferences.setKeyboardHapticPattern(pattern) } }
    fun commitKeyboardHapticIntensity(intensity: Float) { viewModelScope.launch { preferences.setKeyboardHapticIntensity(intensity) } }
    fun setKeyboardHapticCustomSequence(seq: CustomHapticSequence) { viewModelScope.launch { preferences.setKeyboardHapticCustomSequence(seq) } }
    fun testKeyboardHaptic() {
        val s = settings.value
        if (!s.keyboardHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.keyboardHapticCustomSequence) } }
        else { engine.play(s.keyboardHapticPattern, s.keyboardHapticIntensity) }
    }
    fun resetKeyboardDefaults() {
        viewModelScope.launch {
            preferences.setKeyboardHapticPattern(HapticsSettings.Default.keyboardHapticPattern)
            preferences.setKeyboardHapticIntensity(HapticsSettings.Default.keyboardHapticIntensity)
            preferences.setKeyboardHapticCustomSequence(HapticsSettings.Default.keyboardHapticCustomSequence)
        }
    }

    fun resetNotificationHapticsDefaults() {
        viewModelScope.launch {
            preferences.setCallHapticEnabled(HapticsSettings.Default.callHapticEnabled)
            preferences.setCallHapticPattern(HapticsSettings.Default.callHapticPattern)
            preferences.setCallHapticIntensity(HapticsSettings.Default.callHapticIntensity)
            preferences.setCallHapticCustomSequence(HapticsSettings.Default.callHapticCustomSequence)
            preferences.setNotifHapticEnabled(HapticsSettings.Default.notifHapticEnabled)
            preferences.setNotifHapticPattern(HapticsSettings.Default.notifHapticPattern)
            preferences.setNotifHapticIntensity(HapticsSettings.Default.notifHapticIntensity)
            preferences.setNotifHapticCustomSequence(HapticsSettings.Default.notifHapticCustomSequence)
            preferences.setAlarmHapticEnabled(HapticsSettings.Default.alarmHapticEnabled)
            preferences.setAlarmHapticPattern(HapticsSettings.Default.alarmHapticPattern)
            preferences.setAlarmHapticIntensity(HapticsSettings.Default.alarmHapticIntensity)
            preferences.setAlarmHapticCustomSequence(HapticsSettings.Default.alarmHapticCustomSequence)
        }
    }

    fun testHaptic() {
        val s = settings.value
        if (!s.tapHapticCustomSequence.isEmpty) {
            viewModelScope.launch { playCustomSequenceOnEngine(s.tapHapticCustomSequence) }
        } else { engine.play(s.pattern, s.intensity) }
    }
    fun testScrollHaptic() {
        val s = settings.value
        viewModelScope.launch { repeat(3) { i -> if (i > 0) delay(52); engine.play(s.scrollPattern, s.scrollIntensity, 0L) } }
    }
    fun testChargingHaptic() {
        val s = settings.value
        if (!s.chargingVibCustomSequence.isEmpty) {
            viewModelScope.launch { playCustomSequenceOnEngine(s.chargingVibCustomSequence) }
        } else { engine.play(s.chargingVibPattern, s.chargingVibIntensity) }
    }
    fun testEdgeHaptic() { val s = settings.value; engine.play(s.edgePattern, s.edgeIntensity) }
    fun testVolumeHaptic() {
        val s = settings.value
        if (!s.volumeHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.volumeHapticCustomSequence) } }
        else { engine.play(s.volumeHapticPattern, s.volumeHapticIntensity) }
    }
    fun testPowerHaptic() {
        val s = settings.value
        if (!s.powerHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.powerHapticCustomSequence) } }
        else { engine.play(s.powerHapticPattern, s.powerHapticIntensity) }
    }
    fun testBrightnessHaptic() {
        val s = settings.value
        if (!s.brightnessHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.brightnessHapticCustomSequence) } }
        else { engine.play(s.brightnessHapticPattern, s.brightnessHapticIntensity) }
    }
    fun testNavBarHaptic() {
        val s = settings.value
        if (!s.navBarHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.navBarHapticCustomSequence) } }
        else { engine.play(s.navBarHapticPattern, s.navBarHapticIntensity) }
    }
    fun testUnlockHaptic() {
        val s = settings.value
        if (!s.unlockHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.unlockHapticCustomSequence) } }
        else { engine.play(s.unlockHapticPattern, s.unlockHapticIntensity) }
    }
    fun testCallHaptic() {
        val s = settings.value
        if (!s.callHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.callHapticCustomSequence) } }
        else { engine.play(s.callHapticPattern, s.callHapticIntensity) }
    }
    fun testNotifHaptic() {
        val s = settings.value
        if (!s.notifHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.notifHapticCustomSequence) } }
        else { engine.play(s.notifHapticPattern, s.notifHapticIntensity) }
    }
    fun testAlarmHaptic() {
        val s = settings.value
        if (!s.alarmHapticCustomSequence.isEmpty) { viewModelScope.launch { playCustomSequenceOnEngine(s.alarmHapticCustomSequence) } }
        else { engine.play(s.alarmHapticPattern, s.alarmHapticIntensity) }
    }

    private suspend fun playCustomSequenceOnEngine(seq: CustomHapticSequence) {
        val sorted = seq.beats.sortedBy { it.offsetMs }
        val startMs = System.currentTimeMillis()
        for (beat in sorted) {
            val now = System.currentTimeMillis() - startMs
            val waitMs = beat.offsetMs - now
            if (waitMs > 0) delay(waitMs)
            engine.playOneShot(60L, beat.amplitude)
        }
    }

    companion object {
        private fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            if (!manager.isEnabled) return false
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val expectedComponent = ComponentName(context, HapticsAccessibilityService::class.java).flattenToString()
            return enabledServices.split(':').any { it.trim().equals(expectedComponent, ignoreCase = true) || it.trim().endsWith(HapticsAccessibilityService::class.java.name, ignoreCase = true) }
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
