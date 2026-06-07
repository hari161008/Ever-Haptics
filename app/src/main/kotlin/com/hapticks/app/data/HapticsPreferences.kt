package com.hapticks.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticBeat
import com.hapticks.app.haptics.HapticPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.hapticsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hapticks")

class HapticsPreferences(context: Context) {
    private val dataStore = context.applicationContext.hapticsDataStore

    val settings: Flow<HapticsSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) { Log.w(TAG, "DataStore read failed", throwable); emit(emptyPreferences()) }
            else throw throwable
        }
        .map { prefs ->
            HapticsSettings(
                globalEnabled = prefs[Keys.GLOBAL_ENABLED] ?: HapticsSettings.Default.globalEnabled,
                tapEnabled = prefs[Keys.TAP_ENABLED] ?: HapticsSettings.Default.tapEnabled,
                intensity = (prefs[Keys.INTENSITY] ?: HapticsSettings.Default.intensity).coerceIn(0f, 1f),
                pattern = HapticPattern.fromStorageKey(prefs[Keys.PATTERN]),
                tapHapticCustomSequence = parseCustomSequence(prefs[Keys.TAP_HAPTIC_CUSTOM_SEQUENCE]),
                scrollEnabled = prefs[Keys.SCROLL_ENABLED] ?: HapticsSettings.Default.scrollEnabled,
                scrollHapticEventsPerCm = (prefs[Keys.SCROLL_HAPTIC_EVENTS_PER_CM] ?: HapticsSettings.Default.scrollHapticEventsPerCm).coerceIn(HapticsSettings.MIN_SCROLL_EVENTS_PER_CM, HapticsSettings.MAX_SCROLL_EVENTS_PER_CM),
                scrollIntensity = (prefs[Keys.SCROLL_INTENSITY] ?: HapticsSettings.Default.scrollIntensity).coerceIn(0f, 1f),
                scrollIntensityEnabled = prefs[Keys.SCROLL_INTENSITY_ENABLED] ?: HapticsSettings.Default.scrollIntensityEnabled,
                scrollPattern = HapticPattern.fromStorageKey(prefs[Keys.SCROLL_PATTERN]).takeIf { prefs.contains(Keys.SCROLL_PATTERN) } ?: HapticsSettings.Default.scrollPattern,
                scrollVibrationsPerEvent = (prefs[Keys.SCROLL_VIBS_PER_EVENT] ?: HapticsSettings.Default.scrollVibrationsPerEvent).coerceIn(HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT, HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT),
                scrollVibrationsPerEventEnabled = prefs[Keys.SCROLL_VIBS_PER_EVENT_ENABLED] ?: HapticsSettings.Default.scrollVibrationsPerEventEnabled,
                scrollSpeedVibrationScale = (prefs[Keys.SCROLL_SPEED_VIB_SCALE] ?: HapticsSettings.Default.scrollSpeedVibrationScale).coerceIn(HapticsSettings.MIN_SCROLL_SPEED_VIB_SCALE, HapticsSettings.MAX_SCROLL_SPEED_VIB_SCALE),
                scrollSpeedVibrationEnabled = prefs[Keys.SCROLL_SPEED_VIB_ENABLED] ?: HapticsSettings.Default.scrollSpeedVibrationEnabled,
                scrollTailCutoffMs = (prefs[Keys.SCROLL_TAIL_CUTOFF_MS] ?: HapticsSettings.Default.scrollTailCutoffMs).coerceIn(HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS, HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS),
                scrollTailCutoffEnabled = prefs[Keys.SCROLL_TAIL_CUTOFF_ENABLED] ?: HapticsSettings.Default.scrollTailCutoffEnabled,
                scrollHorizontalEnabled = prefs[Keys.SCROLL_HORIZONTAL_ENABLED] ?: HapticsSettings.Default.scrollHorizontalEnabled,
                edgePattern = HapticPattern.fromStorageKey(prefs[Keys.EDGE_PATTERN]).takeIf { prefs.contains(Keys.EDGE_PATTERN) } ?: HapticsSettings.Default.edgePattern,
                edgeIntensity = (prefs[Keys.EDGE_INTENSITY] ?: HapticsSettings.Default.edgeIntensity).coerceIn(0f, 1f),
                a11yScrollBoundEdge = prefs[Keys.A11Y_SCROLL_BOUND_EDGE] ?: HapticsSettings.Default.a11yScrollBoundEdge,
                edgeLsposedLibxposedPath = prefs[Keys.EDGE_LSPOSED_LIBXPOSED_PATH] ?: HapticsSettings.Default.edgeLsposedLibxposedPath,
                tapExcludedPackages = prefs[Keys.TAP_EXCLUDED_PACKAGES] ?: emptySet(),
                scrollExcludedPackages = prefs[Keys.SCROLL_EXCLUDED_PACKAGES] ?: emptySet(),
                edgeExcludedPackages = prefs[Keys.EDGE_EXCLUDED_PACKAGES] ?: emptySet(),
                chargingVibEnabled = prefs[Keys.CHARGING_VIB_ENABLED] ?: HapticsSettings.Default.chargingVibEnabled,
                chargingVibOnConnect = prefs[Keys.CHARGING_VIB_ON_CONNECT] ?: HapticsSettings.Default.chargingVibOnConnect,
                chargingVibOnDisconnect = prefs[Keys.CHARGING_VIB_ON_DISCONNECT] ?: HapticsSettings.Default.chargingVibOnDisconnect,
                chargingVibPattern = HapticPattern.fromStorageKey(prefs[Keys.CHARGING_VIB_PATTERN]).takeIf { prefs.contains(Keys.CHARGING_VIB_PATTERN) } ?: HapticsSettings.Default.chargingVibPattern,
                chargingVibIntensity = (prefs[Keys.CHARGING_VIB_INTENSITY] ?: HapticsSettings.Default.chargingVibIntensity).coerceIn(0f, 1f),
                chargingVibCustomSequence = parseCustomSequence(prefs[Keys.CHARGING_VIB_CUSTOM_SEQUENCE]),
                volumeHapticEnabled = prefs[Keys.VOLUME_HAPTIC_ENABLED] ?: HapticsSettings.Default.volumeHapticEnabled,
                volumeHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.VOLUME_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.VOLUME_HAPTIC_PATTERN) } ?: HapticsSettings.Default.volumeHapticPattern,
                volumeHapticIntensity = (prefs[Keys.VOLUME_HAPTIC_INTENSITY] ?: HapticsSettings.Default.volumeHapticIntensity).coerceIn(0f, 1f),
                volumeHapticCustomSequence = parseCustomSequence(prefs[Keys.VOLUME_HAPTIC_CUSTOM_SEQUENCE]),
                powerHapticEnabled = prefs[Keys.POWER_HAPTIC_ENABLED] ?: HapticsSettings.Default.powerHapticEnabled,
                powerHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.POWER_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.POWER_HAPTIC_PATTERN) } ?: HapticsSettings.Default.powerHapticPattern,
                powerHapticIntensity = (prefs[Keys.POWER_HAPTIC_INTENSITY] ?: HapticsSettings.Default.powerHapticIntensity).coerceIn(0f, 1f),
                powerHapticCustomSequence = parseCustomSequence(prefs[Keys.POWER_HAPTIC_CUSTOM_SEQUENCE]),
                brightnessHapticEnabled = prefs[Keys.BRIGHTNESS_HAPTIC_ENABLED] ?: HapticsSettings.Default.brightnessHapticEnabled,
                brightnessHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.BRIGHTNESS_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.BRIGHTNESS_HAPTIC_PATTERN) } ?: HapticsSettings.Default.brightnessHapticPattern,
                brightnessHapticIntensity = (prefs[Keys.BRIGHTNESS_HAPTIC_INTENSITY] ?: HapticsSettings.Default.brightnessHapticIntensity).coerceIn(0f, 1f),
                brightnessHapticCustomSequence = parseCustomSequence(prefs[Keys.BRIGHTNESS_HAPTIC_CUSTOM_SEQUENCE]),
                navBarHapticEnabled = prefs[Keys.NAVBAR_HAPTIC_ENABLED] ?: HapticsSettings.Default.navBarHapticEnabled,
                navBarHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.NAVBAR_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.NAVBAR_HAPTIC_PATTERN) } ?: HapticsSettings.Default.navBarHapticPattern,
                navBarHapticIntensity = (prefs[Keys.NAVBAR_HAPTIC_INTENSITY] ?: HapticsSettings.Default.navBarHapticIntensity).coerceIn(0f, 1f),
                navBarHapticCustomSequence = parseCustomSequence(prefs[Keys.NAVBAR_HAPTIC_CUSTOM_SEQUENCE]),
                unlockHapticEnabled = prefs[Keys.UNLOCK_HAPTIC_ENABLED] ?: HapticsSettings.Default.unlockHapticEnabled,
                unlockHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.UNLOCK_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.UNLOCK_HAPTIC_PATTERN) } ?: HapticsSettings.Default.unlockHapticPattern,
                unlockHapticIntensity = (prefs[Keys.UNLOCK_HAPTIC_INTENSITY] ?: HapticsSettings.Default.unlockHapticIntensity).coerceIn(0f, 1f),
                unlockHapticCustomSequence = parseCustomSequence(prefs[Keys.UNLOCK_HAPTIC_CUSTOM_SEQUENCE]),
                useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: HapticsSettings.Default.useDynamicColors,
                themeMode = try { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: HapticsSettings.Default.themeMode.name) } catch (_: Exception) { ThemeMode.SYSTEM },
                amoledBlack = prefs[Keys.AMOLED_BLACK] ?: HapticsSettings.Default.amoledBlack,
                seedColor = prefs[Keys.SEED_COLOR] ?: HapticsSettings.Default.seedColor,
                callHapticEnabled = prefs[Keys.CALL_HAPTIC_ENABLED] ?: HapticsSettings.Default.callHapticEnabled,
                callHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.CALL_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.CALL_HAPTIC_PATTERN) } ?: HapticsSettings.Default.callHapticPattern,
                callHapticIntensity = (prefs[Keys.CALL_HAPTIC_INTENSITY] ?: HapticsSettings.Default.callHapticIntensity).coerceIn(0f, 1f),
                callHapticCustomSequence = parseCustomSequence(prefs[Keys.CALL_HAPTIC_CUSTOM_SEQUENCE]),
                notifHapticEnabled = prefs[Keys.NOTIF_HAPTIC_ENABLED] ?: HapticsSettings.Default.notifHapticEnabled,
                notifHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.NOTIF_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.NOTIF_HAPTIC_PATTERN) } ?: HapticsSettings.Default.notifHapticPattern,
                notifHapticIntensity = (prefs[Keys.NOTIF_HAPTIC_INTENSITY] ?: HapticsSettings.Default.notifHapticIntensity).coerceIn(0f, 1f),
                notifHapticCustomSequence = parseCustomSequence(prefs[Keys.NOTIF_HAPTIC_CUSTOM_SEQUENCE]),
                alarmHapticEnabled = prefs[Keys.ALARM_HAPTIC_ENABLED] ?: HapticsSettings.Default.alarmHapticEnabled,
                alarmHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.ALARM_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.ALARM_HAPTIC_PATTERN) } ?: HapticsSettings.Default.alarmHapticPattern,
                alarmHapticIntensity = (prefs[Keys.ALARM_HAPTIC_INTENSITY] ?: HapticsSettings.Default.alarmHapticIntensity).coerceIn(0f, 1f),
                alarmHapticCustomSequence = parseCustomSequence(prefs[Keys.ALARM_HAPTIC_CUSTOM_SEQUENCE]),
                keyboardHapticEnabled = prefs[Keys.KEYBOARD_HAPTIC_ENABLED] ?: HapticsSettings.Default.keyboardHapticEnabled,
                keyboardHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.KEYBOARD_HAPTIC_PATTERN]).takeIf { prefs.contains(Keys.KEYBOARD_HAPTIC_PATTERN) } ?: HapticsSettings.Default.keyboardHapticPattern,
                keyboardHapticIntensity = (prefs[Keys.KEYBOARD_HAPTIC_INTENSITY] ?: HapticsSettings.Default.keyboardHapticIntensity).coerceIn(0f, 1f),
                keyboardHapticCustomSequence = parseCustomSequence(prefs[Keys.KEYBOARD_HAPTIC_CUSTOM_SEQUENCE]),
                batterySaverDetectionEnabled = prefs[Keys.BATTERY_SAVER_DETECTION_ENABLED] ?: HapticsSettings.Default.batterySaverDetectionEnabled,
                musicHapticsEnabled = prefs[Keys.MUSIC_HAPTICS_ENABLED] ?: HapticsSettings.Default.musicHapticsEnabled,
                musicHapticsSource = prefs[Keys.MUSIC_HAPTICS_SOURCE]?.let { runCatching { MusicHapticsSource.valueOf(it) }.getOrNull() } ?: HapticsSettings.Default.musicHapticsSource,
                musicHapticsSensitivity = (prefs[Keys.MUSIC_HAPTICS_SENSITIVITY] ?: HapticsSettings.Default.musicHapticsSensitivity).coerceIn(0f, 1f),
                musicHapticsStrength = (prefs[Keys.MUSIC_HAPTICS_STRENGTH] ?: HapticsSettings.Default.musicHapticsStrength).coerceIn(0f, 1f),
            )
        }

    suspend fun setGlobalEnabled(enabled: Boolean) = edit { it[Keys.GLOBAL_ENABLED] = enabled }
    suspend fun setTapEnabled(enabled: Boolean) = edit { it[Keys.TAP_ENABLED] = enabled }
    suspend fun setIntensity(intensity: Float) = edit { it[Keys.INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setPattern(pattern: HapticPattern) = edit { it[Keys.PATTERN] = pattern.name }
    suspend fun setTapHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.TAP_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setScrollEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_ENABLED] = enabled }
    suspend fun setScrollPattern(pattern: HapticPattern) = edit { it[Keys.SCROLL_PATTERN] = pattern.name }
    suspend fun setScrollHapticEventsPerCm(value: Float) = edit { it[Keys.SCROLL_HAPTIC_EVENTS_PER_CM] = value.coerceIn(HapticsSettings.MIN_SCROLL_EVENTS_PER_CM, HapticsSettings.MAX_SCROLL_EVENTS_PER_CM) }
    suspend fun setScrollIntensity(intensity: Float) = edit { it[Keys.SCROLL_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setScrollIntensityEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_INTENSITY_ENABLED] = enabled }
    suspend fun setScrollVibrationsPerEvent(value: Float) = edit { it[Keys.SCROLL_VIBS_PER_EVENT] = value.coerceIn(HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT, HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT) }
    suspend fun setScrollVibrationsPerEventEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_VIBS_PER_EVENT_ENABLED] = enabled }
    suspend fun setScrollSpeedVibrationScale(value: Float) = edit { it[Keys.SCROLL_SPEED_VIB_SCALE] = value.coerceIn(HapticsSettings.MIN_SCROLL_SPEED_VIB_SCALE, HapticsSettings.MAX_SCROLL_SPEED_VIB_SCALE) }
    suspend fun setScrollSpeedVibrationEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_SPEED_VIB_ENABLED] = enabled }
    suspend fun setScrollTailCutoffMs(value: Int) = edit { it[Keys.SCROLL_TAIL_CUTOFF_MS] = value.coerceIn(HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS, HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS) }
    suspend fun setScrollTailCutoffEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_TAIL_CUTOFF_ENABLED] = enabled }
    suspend fun setScrollHorizontalEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_HORIZONTAL_ENABLED] = enabled }
    suspend fun setEdgePattern(pattern: HapticPattern) = edit { it[Keys.EDGE_PATTERN] = pattern.name }
    suspend fun setEdgeIntensity(intensity: Float) = edit { it[Keys.EDGE_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setA11yScrollBoundEdge(enabled: Boolean) = edit { it[Keys.A11Y_SCROLL_BOUND_EDGE] = enabled }
    suspend fun setEdgeLsposedLibxposedPath(enabled: Boolean) = edit { it[Keys.EDGE_LSPOSED_LIBXPOSED_PATH] = enabled }
    suspend fun setTapExcludedPackages(packages: Set<String>) = edit { it[Keys.TAP_EXCLUDED_PACKAGES] = packages }
    suspend fun setScrollExcludedPackages(packages: Set<String>) = edit { it[Keys.SCROLL_EXCLUDED_PACKAGES] = packages }
    suspend fun setEdgeExcludedPackages(packages: Set<String>) = edit { it[Keys.EDGE_EXCLUDED_PACKAGES] = packages }
    suspend fun setUseDynamicColors(enabled: Boolean) = edit { it[Keys.USE_DYNAMIC_COLORS] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAmoledBlack(enabled: Boolean) = edit { it[Keys.AMOLED_BLACK] = enabled }
    suspend fun setSeedColor(color: Int) = edit { it[Keys.SEED_COLOR] = color }
    suspend fun setChargingVibEnabled(enabled: Boolean) = edit { it[Keys.CHARGING_VIB_ENABLED] = enabled }
    suspend fun setChargingVibOnConnect(enabled: Boolean) = edit { it[Keys.CHARGING_VIB_ON_CONNECT] = enabled }
    suspend fun setChargingVibOnDisconnect(enabled: Boolean) = edit { it[Keys.CHARGING_VIB_ON_DISCONNECT] = enabled }
    suspend fun setChargingVibPattern(pattern: HapticPattern) = edit { it[Keys.CHARGING_VIB_PATTERN] = pattern.name }
    suspend fun setChargingVibIntensity(intensity: Float) = edit { it[Keys.CHARGING_VIB_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setChargingVibCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.CHARGING_VIB_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setVolumeHapticEnabled(enabled: Boolean) = edit { it[Keys.VOLUME_HAPTIC_ENABLED] = enabled }
    suspend fun setVolumeHapticPattern(pattern: HapticPattern) = edit { it[Keys.VOLUME_HAPTIC_PATTERN] = pattern.name }
    suspend fun setVolumeHapticIntensity(intensity: Float) = edit { it[Keys.VOLUME_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setVolumeHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.VOLUME_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setPowerHapticEnabled(enabled: Boolean) = edit { it[Keys.POWER_HAPTIC_ENABLED] = enabled }
    suspend fun setPowerHapticPattern(pattern: HapticPattern) = edit { it[Keys.POWER_HAPTIC_PATTERN] = pattern.name }
    suspend fun setPowerHapticIntensity(intensity: Float) = edit { it[Keys.POWER_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setPowerHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.POWER_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setBrightnessHapticEnabled(enabled: Boolean) = edit { it[Keys.BRIGHTNESS_HAPTIC_ENABLED] = enabled }
    suspend fun setBrightnessHapticPattern(pattern: HapticPattern) = edit { it[Keys.BRIGHTNESS_HAPTIC_PATTERN] = pattern.name }
    suspend fun setBrightnessHapticIntensity(intensity: Float) = edit { it[Keys.BRIGHTNESS_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setBrightnessHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.BRIGHTNESS_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setNavBarHapticEnabled(enabled: Boolean) = edit { it[Keys.NAVBAR_HAPTIC_ENABLED] = enabled }
    suspend fun setNavBarHapticPattern(pattern: HapticPattern) = edit { it[Keys.NAVBAR_HAPTIC_PATTERN] = pattern.name }
    suspend fun setNavBarHapticIntensity(intensity: Float) = edit { it[Keys.NAVBAR_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setNavBarHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.NAVBAR_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setUnlockHapticEnabled(enabled: Boolean) = edit { it[Keys.UNLOCK_HAPTIC_ENABLED] = enabled }
    suspend fun setUnlockHapticPattern(pattern: HapticPattern) = edit { it[Keys.UNLOCK_HAPTIC_PATTERN] = pattern.name }
    suspend fun setUnlockHapticIntensity(intensity: Float) = edit { it[Keys.UNLOCK_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setUnlockHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.UNLOCK_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setCallHapticEnabled(enabled: Boolean) = edit { it[Keys.CALL_HAPTIC_ENABLED] = enabled }
    suspend fun setCallHapticPattern(pattern: HapticPattern) = edit { it[Keys.CALL_HAPTIC_PATTERN] = pattern.name }
    suspend fun setCallHapticIntensity(intensity: Float) = edit { it[Keys.CALL_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setCallHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.CALL_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setNotifHapticEnabled(enabled: Boolean) = edit { it[Keys.NOTIF_HAPTIC_ENABLED] = enabled }
    suspend fun setNotifHapticPattern(pattern: HapticPattern) = edit { it[Keys.NOTIF_HAPTIC_PATTERN] = pattern.name }
    suspend fun setNotifHapticIntensity(intensity: Float) = edit { it[Keys.NOTIF_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setNotifHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.NOTIF_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }
    suspend fun setAlarmHapticEnabled(enabled: Boolean) = edit { it[Keys.ALARM_HAPTIC_ENABLED] = enabled }
    suspend fun setAlarmHapticPattern(pattern: HapticPattern) = edit { it[Keys.ALARM_HAPTIC_PATTERN] = pattern.name }
    suspend fun setAlarmHapticIntensity(intensity: Float) = edit { it[Keys.ALARM_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setAlarmHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.ALARM_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }

    suspend fun setKeyboardHapticEnabled(enabled: Boolean) = edit { it[Keys.KEYBOARD_HAPTIC_ENABLED] = enabled }
    suspend fun setKeyboardHapticPattern(pattern: HapticPattern) = edit { it[Keys.KEYBOARD_HAPTIC_PATTERN] = pattern.name }
    suspend fun setKeyboardHapticIntensity(intensity: Float) = edit { it[Keys.KEYBOARD_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setKeyboardHapticCustomSequence(seq: CustomHapticSequence) = edit { it[Keys.KEYBOARD_HAPTIC_CUSTOM_SEQUENCE] = serializeCustomSequence(seq) }

    suspend fun setBatterySaverDetectionEnabled(enabled: Boolean) = edit { it[Keys.BATTERY_SAVER_DETECTION_ENABLED] = enabled }
    suspend fun setMusicHapticsEnabled(enabled: Boolean) = edit { it[Keys.MUSIC_HAPTICS_ENABLED] = enabled }
    suspend fun setMusicHapticsSource(source: MusicHapticsSource) = edit { it[Keys.MUSIC_HAPTICS_SOURCE] = source.name }
    suspend fun setMusicHapticsSensitivity(value: Float) = edit { it[Keys.MUSIC_HAPTICS_SENSITIVITY] = value.coerceIn(0f, 1f) }
    suspend fun setMusicHapticsStrength(value: Float) = edit { it[Keys.MUSIC_HAPTICS_STRENGTH] = value.coerceIn(0f, 1f) }

    private suspend inline fun edit(crossinline block: (MutablePreferences) -> Unit) {
        try { dataStore.edit { block(it) } } catch (e: IOException) { Log.w(TAG, "DataStore write failed", e) }
    }

    private object Keys {
        val GLOBAL_ENABLED = booleanPreferencesKey("global_enabled")
        val TAP_ENABLED = booleanPreferencesKey("tap_enabled")
        val INTENSITY = floatPreferencesKey("intensity")
        val PATTERN = stringPreferencesKey("pattern")
        val TAP_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("tap_haptic_custom_sequence")
        val SCROLL_ENABLED = booleanPreferencesKey("scroll_enabled")
        val SCROLL_PATTERN = stringPreferencesKey("scroll_pattern")
        val SCROLL_HAPTIC_EVENTS_PER_CM = floatPreferencesKey("scroll_haptic_events_per_cm")
        val SCROLL_INTENSITY = floatPreferencesKey("scroll_intensity")
        val SCROLL_INTENSITY_ENABLED = booleanPreferencesKey("scroll_intensity_enabled")
        val SCROLL_VIBS_PER_EVENT = floatPreferencesKey("scroll_vibs_per_event")
        val SCROLL_VIBS_PER_EVENT_ENABLED = booleanPreferencesKey("scroll_vibs_per_event_enabled")
        val SCROLL_SPEED_VIB_SCALE = floatPreferencesKey("scroll_speed_vib_scale")
        val SCROLL_SPEED_VIB_ENABLED = booleanPreferencesKey("scroll_speed_vib_enabled")
        val SCROLL_TAIL_CUTOFF_MS = intPreferencesKey("scroll_tail_cutoff_ms")
        val SCROLL_TAIL_CUTOFF_ENABLED = booleanPreferencesKey("scroll_tail_cutoff_enabled")
        val SCROLL_HORIZONTAL_ENABLED = booleanPreferencesKey("scroll_horizontal_enabled")
        val EDGE_PATTERN = stringPreferencesKey("edge_pattern")
        val EDGE_INTENSITY = floatPreferencesKey("edge_intensity")
        val A11Y_SCROLL_BOUND_EDGE = booleanPreferencesKey("a11y_scroll_bound_edge")
        val EDGE_LSPOSED_LIBXPOSED_PATH = booleanPreferencesKey("edge_lsposed_libxposed_path")
        val TAP_EXCLUDED_PACKAGES = stringSetPreferencesKey("tap_excluded_packages")
        val SCROLL_EXCLUDED_PACKAGES = stringSetPreferencesKey("scroll_excluded_packages")
        val EDGE_EXCLUDED_PACKAGES = stringSetPreferencesKey("edge_excluded_packages")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val SEED_COLOR = intPreferencesKey("seed_color")
        val CHARGING_VIB_ENABLED = booleanPreferencesKey("charging_vib_enabled")
        val CHARGING_VIB_ON_CONNECT = booleanPreferencesKey("charging_vib_on_connect")
        val CHARGING_VIB_ON_DISCONNECT = booleanPreferencesKey("charging_vib_on_disconnect")
        val CHARGING_VIB_PATTERN = stringPreferencesKey("charging_vib_pattern")
        val CHARGING_VIB_INTENSITY = floatPreferencesKey("charging_vib_intensity")
        val CHARGING_VIB_CUSTOM_SEQUENCE = stringPreferencesKey("charging_vib_custom_sequence")
        val VOLUME_HAPTIC_ENABLED = booleanPreferencesKey("volume_haptic_enabled")
        val VOLUME_HAPTIC_PATTERN = stringPreferencesKey("volume_haptic_pattern")
        val VOLUME_HAPTIC_INTENSITY = floatPreferencesKey("volume_haptic_intensity")
        val VOLUME_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("volume_haptic_custom_sequence")
        val POWER_HAPTIC_ENABLED = booleanPreferencesKey("power_haptic_enabled")
        val POWER_HAPTIC_PATTERN = stringPreferencesKey("power_haptic_pattern")
        val POWER_HAPTIC_INTENSITY = floatPreferencesKey("power_haptic_intensity")
        val POWER_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("power_haptic_custom_sequence")
        val BRIGHTNESS_HAPTIC_ENABLED = booleanPreferencesKey("brightness_haptic_enabled")
        val BRIGHTNESS_HAPTIC_PATTERN = stringPreferencesKey("brightness_haptic_pattern")
        val BRIGHTNESS_HAPTIC_INTENSITY = floatPreferencesKey("brightness_haptic_intensity")
        val BRIGHTNESS_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("brightness_haptic_custom_sequence")
        val NAVBAR_HAPTIC_ENABLED = booleanPreferencesKey("navbar_haptic_enabled")
        val NAVBAR_HAPTIC_PATTERN = stringPreferencesKey("navbar_haptic_pattern")
        val NAVBAR_HAPTIC_INTENSITY = floatPreferencesKey("navbar_haptic_intensity")
        val NAVBAR_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("navbar_haptic_custom_sequence")
        val UNLOCK_HAPTIC_ENABLED = booleanPreferencesKey("unlock_haptic_enabled")
        val UNLOCK_HAPTIC_PATTERN = stringPreferencesKey("unlock_haptic_pattern")
        val UNLOCK_HAPTIC_INTENSITY = floatPreferencesKey("unlock_haptic_intensity")
        val UNLOCK_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("unlock_haptic_custom_sequence")
        val CALL_HAPTIC_ENABLED = booleanPreferencesKey("call_haptic_enabled")
        val CALL_HAPTIC_PATTERN = stringPreferencesKey("call_haptic_pattern")
        val CALL_HAPTIC_INTENSITY = floatPreferencesKey("call_haptic_intensity")
        val CALL_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("call_haptic_custom_sequence")
        val NOTIF_HAPTIC_ENABLED = booleanPreferencesKey("notif_haptic_enabled")
        val NOTIF_HAPTIC_PATTERN = stringPreferencesKey("notif_haptic_pattern")
        val NOTIF_HAPTIC_INTENSITY = floatPreferencesKey("notif_haptic_intensity")
        val NOTIF_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("notif_haptic_custom_sequence")
        val ALARM_HAPTIC_ENABLED = booleanPreferencesKey("alarm_haptic_enabled")
        val ALARM_HAPTIC_PATTERN = stringPreferencesKey("alarm_haptic_pattern")
        val ALARM_HAPTIC_INTENSITY = floatPreferencesKey("alarm_haptic_intensity")
        val ALARM_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("alarm_haptic_custom_sequence")
        val KEYBOARD_HAPTIC_ENABLED = booleanPreferencesKey("keyboard_haptic_enabled")
        val KEYBOARD_HAPTIC_PATTERN = stringPreferencesKey("keyboard_haptic_pattern")
        val KEYBOARD_HAPTIC_INTENSITY = floatPreferencesKey("keyboard_haptic_intensity")
        val KEYBOARD_HAPTIC_CUSTOM_SEQUENCE = stringPreferencesKey("keyboard_haptic_custom_sequence")
        val BATTERY_SAVER_DETECTION_ENABLED = booleanPreferencesKey("battery_saver_detection_enabled")
        val MUSIC_HAPTICS_ENABLED = booleanPreferencesKey("music_haptics_enabled")
        val MUSIC_HAPTICS_SOURCE = stringPreferencesKey("music_haptics_source")
        val MUSIC_HAPTICS_SENSITIVITY = floatPreferencesKey("music_haptics_sensitivity")
        val MUSIC_HAPTICS_STRENGTH = floatPreferencesKey("music_haptics_strength")
    }

    private companion object {
        const val TAG = "HapticsPrefs"
        fun serializeCustomSequence(seq: CustomHapticSequence): String =
            seq.beats.joinToString(";") { "${it.offsetMs}:${it.amplitude}" }
        fun parseCustomSequence(raw: String?): CustomHapticSequence {
            if (raw.isNullOrBlank()) return CustomHapticSequence()
            return try {
                val beats = raw.split(";").mapNotNull { token ->
                    val parts = token.split(":")
                    if (parts.size == 2) HapticBeat(parts[0].toLong(), parts[1].toInt()) else null
                }
                CustomHapticSequence(beats)
            } catch (_: Exception) { CustomHapticSequence() }
        }
    }
}
