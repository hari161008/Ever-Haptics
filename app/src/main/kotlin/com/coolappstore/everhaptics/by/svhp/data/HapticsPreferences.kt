package com.coolappstore.everhaptics.by.svhp.data

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
import com.coolappstore.everhaptics.by.svhp.haptics.HapticPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.hapticsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ever_haptics")

class HapticsPreferences(context: Context) {

    private val dataStore = context.applicationContext.hapticsDataStore

    val settings: Flow<HapticsSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "DataStore read failed; falling back to defaults", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { prefs ->
            HapticsSettings(
                globalEnabled = prefs[Keys.GLOBAL_ENABLED] ?: HapticsSettings.Default.globalEnabled,
                tapEnabled = prefs[Keys.TAP_ENABLED] ?: HapticsSettings.Default.tapEnabled,
                intensity = (prefs[Keys.INTENSITY] ?: HapticsSettings.Default.intensity).coerceIn(0f, 1f),
                pattern = HapticPattern.fromStorageKey(prefs[Keys.PATTERN]),
                scrollEnabled = prefs[Keys.SCROLL_ENABLED] ?: HapticsSettings.Default.scrollEnabled,
                scrollHapticEventsPerCm = (prefs[Keys.SCROLL_HAPTIC_EVENTS_PER_CM]
                    ?: HapticsSettings.Default.scrollHapticEventsPerCm).coerceIn(
                    HapticsSettings.MIN_SCROLL_EVENTS_PER_CM,
                    HapticsSettings.MAX_SCROLL_EVENTS_PER_CM,
                ),
                scrollIntensity = (prefs[Keys.SCROLL_INTENSITY] ?: HapticsSettings.Default.scrollIntensity).coerceIn(0f, 1f),
                scrollIntensityEnabled = prefs[Keys.SCROLL_INTENSITY_ENABLED] ?: HapticsSettings.Default.scrollIntensityEnabled,
                scrollPattern = HapticPattern.fromStorageKey(prefs[Keys.SCROLL_PATTERN])
                    .takeIf { prefs.contains(Keys.SCROLL_PATTERN) } ?: HapticsSettings.Default.scrollPattern,
                scrollVibrationsPerEvent = (prefs[Keys.SCROLL_VIBS_PER_EVENT]
                    ?: HapticsSettings.Default.scrollVibrationsPerEvent).coerceIn(
                    HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT,
                    HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT,
                ),
                scrollVibrationsPerEventEnabled = prefs[Keys.SCROLL_VIBS_PER_EVENT_ENABLED] ?: HapticsSettings.Default.scrollVibrationsPerEventEnabled,
                scrollSpeedVibrationScale = (prefs[Keys.SCROLL_SPEED_VIB_SCALE]
                    ?: HapticsSettings.Default.scrollSpeedVibrationScale).coerceIn(
                    HapticsSettings.MIN_SCROLL_SPEED_VIB_SCALE,
                    HapticsSettings.MAX_SCROLL_SPEED_VIB_SCALE,
                ),
                scrollSpeedVibrationEnabled = prefs[Keys.SCROLL_SPEED_VIB_ENABLED] ?: HapticsSettings.Default.scrollSpeedVibrationEnabled,
                scrollTailCutoffMs = (prefs[Keys.SCROLL_TAIL_CUTOFF_MS]
                    ?: HapticsSettings.Default.scrollTailCutoffMs).coerceIn(
                    HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS,
                    HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS,
                ),
                scrollTailCutoffEnabled = prefs[Keys.SCROLL_TAIL_CUTOFF_ENABLED] ?: HapticsSettings.Default.scrollTailCutoffEnabled,
                scrollHorizontalEnabled = prefs[Keys.SCROLL_HORIZONTAL_ENABLED] ?: HapticsSettings.Default.scrollHorizontalEnabled,
                chargingVibEnabled = prefs[Keys.CHARGING_VIB_ENABLED] ?: HapticsSettings.Default.chargingVibEnabled,
                chargingVibOnConnect = prefs[Keys.CHARGING_VIB_ON_CONNECT] ?: HapticsSettings.Default.chargingVibOnConnect,
                chargingVibOnDisconnect = prefs[Keys.CHARGING_VIB_ON_DISCONNECT] ?: HapticsSettings.Default.chargingVibOnDisconnect,
                chargingVibDurationIndex = (prefs[Keys.CHARGING_VIB_DURATION_INDEX] ?: HapticsSettings.Default.chargingVibDurationIndex).coerceIn(0, 2),
                chargingVibIntensity = (prefs[Keys.CHARGING_VIB_INTENSITY] ?: HapticsSettings.Default.chargingVibIntensity).coerceIn(0f, 1f),
                volumeHapticEnabled = prefs[Keys.VOLUME_HAPTIC_ENABLED] ?: HapticsSettings.Default.volumeHapticEnabled,
                volumeHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.VOLUME_HAPTIC_PATTERN])
                    .takeIf { prefs.contains(Keys.VOLUME_HAPTIC_PATTERN) } ?: HapticsSettings.Default.volumeHapticPattern,
                volumeHapticIntensity = (prefs[Keys.VOLUME_HAPTIC_INTENSITY] ?: HapticsSettings.Default.volumeHapticIntensity).coerceIn(0f, 1f),
                powerHapticEnabled = prefs[Keys.POWER_HAPTIC_ENABLED] ?: HapticsSettings.Default.powerHapticEnabled,
                powerHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.POWER_HAPTIC_PATTERN])
                    .takeIf { prefs.contains(Keys.POWER_HAPTIC_PATTERN) } ?: HapticsSettings.Default.powerHapticPattern,
                powerHapticIntensity = (prefs[Keys.POWER_HAPTIC_INTENSITY] ?: HapticsSettings.Default.powerHapticIntensity).coerceIn(0f, 1f),
                brightnessHapticEnabled = prefs[Keys.BRIGHTNESS_HAPTIC_ENABLED] ?: HapticsSettings.Default.brightnessHapticEnabled,
                brightnessHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.BRIGHTNESS_HAPTIC_PATTERN])
                    .takeIf { prefs.contains(Keys.BRIGHTNESS_HAPTIC_PATTERN) } ?: HapticsSettings.Default.brightnessHapticPattern,
                brightnessHapticIntensity = (prefs[Keys.BRIGHTNESS_HAPTIC_INTENSITY] ?: HapticsSettings.Default.brightnessHapticIntensity).coerceIn(0f, 1f),
                navBarHapticEnabled = prefs[Keys.NAVBAR_HAPTIC_ENABLED] ?: HapticsSettings.Default.navBarHapticEnabled,
                navBarHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.NAVBAR_HAPTIC_PATTERN])
                    .takeIf { prefs.contains(Keys.NAVBAR_HAPTIC_PATTERN) } ?: HapticsSettings.Default.navBarHapticPattern,
                navBarHapticIntensity = (prefs[Keys.NAVBAR_HAPTIC_INTENSITY] ?: HapticsSettings.Default.navBarHapticIntensity).coerceIn(0f, 1f),
                unlockHapticEnabled = prefs[Keys.UNLOCK_HAPTIC_ENABLED] ?: HapticsSettings.Default.unlockHapticEnabled,
                unlockHapticPattern = HapticPattern.fromStorageKey(prefs[Keys.UNLOCK_HAPTIC_PATTERN])
                    .takeIf { prefs.contains(Keys.UNLOCK_HAPTIC_PATTERN) } ?: HapticsSettings.Default.unlockHapticPattern,
                unlockHapticIntensity = (prefs[Keys.UNLOCK_HAPTIC_INTENSITY] ?: HapticsSettings.Default.unlockHapticIntensity).coerceIn(0f, 1f),
                useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: HapticsSettings.Default.useDynamicColors,
                themeMode = try {
                    ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: HapticsSettings.Default.themeMode.name)
                } catch (_: Exception) { ThemeMode.SYSTEM },
                amoledBlack = prefs[Keys.AMOLED_BLACK] ?: HapticsSettings.Default.amoledBlack,
                seedColor = prefs[Keys.SEED_COLOR] ?: HapticsSettings.Default.seedColor,
                tapExcludedPackages = prefs[Keys.TAP_EXCLUDED_PACKAGES] ?: emptySet(),
                scrollExcludedPackages = prefs[Keys.SCROLL_EXCLUDED_PACKAGES] ?: emptySet(),
            )
        }

    suspend fun setGlobalEnabled(enabled: Boolean) = edit { it[Keys.GLOBAL_ENABLED] = enabled }
    suspend fun setTapEnabled(enabled: Boolean) = edit { it[Keys.TAP_ENABLED] = enabled }
    suspend fun setIntensity(intensity: Float) = edit { it[Keys.INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setPattern(pattern: HapticPattern) = edit { it[Keys.PATTERN] = pattern.name }
    suspend fun setScrollEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_ENABLED] = enabled }
    suspend fun setScrollPattern(pattern: HapticPattern) = edit { it[Keys.SCROLL_PATTERN] = pattern.name }
    suspend fun setScrollIntensity(intensity: Float) = edit { it[Keys.SCROLL_INTENSITY] = intensity.coerceIn(0f, 1f) }
    suspend fun setScrollIntensityEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_INTENSITY_ENABLED] = enabled }
    suspend fun setScrollHapticEventsPerCm(value: Float) = edit {
        it[Keys.SCROLL_HAPTIC_EVENTS_PER_CM] = value.coerceIn(HapticsSettings.MIN_SCROLL_EVENTS_PER_CM, HapticsSettings.MAX_SCROLL_EVENTS_PER_CM)
    }
    suspend fun setScrollVibrationsPerEvent(value: Float) = edit {
        it[Keys.SCROLL_VIBS_PER_EVENT] = value.coerceIn(HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT, HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT)
    }
    suspend fun setScrollVibrationsPerEventEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_VIBS_PER_EVENT_ENABLED] = enabled }
    suspend fun setScrollSpeedVibrationScale(value: Float) = edit {
        it[Keys.SCROLL_SPEED_VIB_SCALE] = value.coerceIn(HapticsSettings.MIN_SCROLL_SPEED_VIB_SCALE, HapticsSettings.MAX_SCROLL_SPEED_VIB_SCALE)
    }
    suspend fun setScrollSpeedVibrationEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_SPEED_VIB_ENABLED] = enabled }
    suspend fun setScrollTailCutoffMs(value: Int) = edit {
        it[Keys.SCROLL_TAIL_CUTOFF_MS] = value.coerceIn(HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS, HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS)
    }
    suspend fun setScrollTailCutoffEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_TAIL_CUTOFF_ENABLED] = enabled }
    suspend fun setScrollHorizontalEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_HORIZONTAL_ENABLED] = enabled }
    suspend fun setTapExcludedPackages(packages: Set<String>) = edit { it[Keys.TAP_EXCLUDED_PACKAGES] = packages }
    suspend fun setScrollExcludedPackages(packages: Set<String>) = edit { it[Keys.SCROLL_EXCLUDED_PACKAGES] = packages }
    suspend fun setUseDynamicColors(enabled: Boolean) = edit { it[Keys.USE_DYNAMIC_COLORS] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAmoledBlack(enabled: Boolean) = edit { it[Keys.AMOLED_BLACK] = enabled }
    suspend fun setSeedColor(color: Int) = edit { it[Keys.SEED_COLOR] = color }

    // Charging
    suspend fun setChargingVibEnabled(enabled: Boolean) = edit { it[Keys.CHARGING_VIB_ENABLED] = enabled }
    suspend fun setChargingVibOnConnect(enabled: Boolean) = edit { it[Keys.CHARGING_VIB_ON_CONNECT] = enabled }
    suspend fun setChargingVibOnDisconnect(enabled: Boolean) = edit { it[Keys.CHARGING_VIB_ON_DISCONNECT] = enabled }
    suspend fun setChargingVibDurationIndex(index: Int) = edit { it[Keys.CHARGING_VIB_DURATION_INDEX] = index.coerceIn(0, 2) }
    suspend fun setChargingVibIntensity(intensity: Float) = edit { it[Keys.CHARGING_VIB_INTENSITY] = intensity.coerceIn(0f, 1f) }

    // Volume haptics
    suspend fun setVolumeHapticEnabled(enabled: Boolean) = edit { it[Keys.VOLUME_HAPTIC_ENABLED] = enabled }
    suspend fun setVolumeHapticPattern(pattern: HapticPattern) = edit { it[Keys.VOLUME_HAPTIC_PATTERN] = pattern.name }
    suspend fun setVolumeHapticIntensity(intensity: Float) = edit { it[Keys.VOLUME_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }

    // Power haptics
    suspend fun setPowerHapticEnabled(enabled: Boolean) = edit { it[Keys.POWER_HAPTIC_ENABLED] = enabled }
    suspend fun setPowerHapticPattern(pattern: HapticPattern) = edit { it[Keys.POWER_HAPTIC_PATTERN] = pattern.name }
    suspend fun setPowerHapticIntensity(intensity: Float) = edit { it[Keys.POWER_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }

    // Brightness haptics
    suspend fun setBrightnessHapticEnabled(enabled: Boolean) = edit { it[Keys.BRIGHTNESS_HAPTIC_ENABLED] = enabled }
    suspend fun setBrightnessHapticPattern(pattern: HapticPattern) = edit { it[Keys.BRIGHTNESS_HAPTIC_PATTERN] = pattern.name }
    suspend fun setBrightnessHapticIntensity(intensity: Float) = edit { it[Keys.BRIGHTNESS_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }

    // NavBar haptics
    suspend fun setNavBarHapticEnabled(enabled: Boolean) = edit { it[Keys.NAVBAR_HAPTIC_ENABLED] = enabled }
    suspend fun setNavBarHapticPattern(pattern: HapticPattern) = edit { it[Keys.NAVBAR_HAPTIC_PATTERN] = pattern.name }
    suspend fun setNavBarHapticIntensity(intensity: Float) = edit { it[Keys.NAVBAR_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }

    // Unlock haptics
    suspend fun setUnlockHapticEnabled(enabled: Boolean) = edit { it[Keys.UNLOCK_HAPTIC_ENABLED] = enabled }
    suspend fun setUnlockHapticPattern(pattern: HapticPattern) = edit { it[Keys.UNLOCK_HAPTIC_PATTERN] = pattern.name }
    suspend fun setUnlockHapticIntensity(intensity: Float) = edit { it[Keys.UNLOCK_HAPTIC_INTENSITY] = intensity.coerceIn(0f, 1f) }

    private suspend inline fun edit(crossinline block: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit { block(it) }
        } catch (e: IOException) {
            Log.w(TAG, "DataStore write failed; change will not persist", e)
        }
    }

    private object Keys {
        val GLOBAL_ENABLED = booleanPreferencesKey("global_enabled")
        val TAP_ENABLED = booleanPreferencesKey("tap_enabled")
        val INTENSITY = floatPreferencesKey("intensity")
        val PATTERN = stringPreferencesKey("pattern")
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
        val TAP_EXCLUDED_PACKAGES = stringSetPreferencesKey("tap_excluded_packages")
        val SCROLL_EXCLUDED_PACKAGES = stringSetPreferencesKey("scroll_excluded_packages")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val SEED_COLOR = intPreferencesKey("seed_color")
        // Charging
        val CHARGING_VIB_ENABLED = booleanPreferencesKey("charging_vib_enabled")
        val CHARGING_VIB_ON_CONNECT = booleanPreferencesKey("charging_vib_on_connect")
        val CHARGING_VIB_ON_DISCONNECT = booleanPreferencesKey("charging_vib_on_disconnect")
        val CHARGING_VIB_DURATION_INDEX = intPreferencesKey("charging_vib_duration_index")
        val CHARGING_VIB_INTENSITY = floatPreferencesKey("charging_vib_intensity")
        // Volume
        val VOLUME_HAPTIC_ENABLED = booleanPreferencesKey("volume_haptic_enabled")
        val VOLUME_HAPTIC_PATTERN = stringPreferencesKey("volume_haptic_pattern")
        val VOLUME_HAPTIC_INTENSITY = floatPreferencesKey("volume_haptic_intensity")
        // Power
        val POWER_HAPTIC_ENABLED = booleanPreferencesKey("power_haptic_enabled")
        val POWER_HAPTIC_PATTERN = stringPreferencesKey("power_haptic_pattern")
        val POWER_HAPTIC_INTENSITY = floatPreferencesKey("power_haptic_intensity")
        // Brightness
        val BRIGHTNESS_HAPTIC_ENABLED = booleanPreferencesKey("brightness_haptic_enabled")
        val BRIGHTNESS_HAPTIC_PATTERN = stringPreferencesKey("brightness_haptic_pattern")
        val BRIGHTNESS_HAPTIC_INTENSITY = floatPreferencesKey("brightness_haptic_intensity")
        // NavBar
        val NAVBAR_HAPTIC_ENABLED = booleanPreferencesKey("navbar_haptic_enabled")
        val NAVBAR_HAPTIC_PATTERN = stringPreferencesKey("navbar_haptic_pattern")
        val NAVBAR_HAPTIC_INTENSITY = floatPreferencesKey("navbar_haptic_intensity")
        // Unlock
        val UNLOCK_HAPTIC_ENABLED = booleanPreferencesKey("unlock_haptic_enabled")
        val UNLOCK_HAPTIC_PATTERN = stringPreferencesKey("unlock_haptic_pattern")
        val UNLOCK_HAPTIC_INTENSITY = floatPreferencesKey("unlock_haptic_intensity")
    }

    private companion object {
        const val TAG = "EverHapticsPrefs"
    }
}
