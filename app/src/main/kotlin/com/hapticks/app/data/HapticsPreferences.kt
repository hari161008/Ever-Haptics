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
            if (throwable is IOException) {
                Log.w(TAG, "DataStore read failed; falling back to defaults", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { prefs ->
            HapticsSettings(
                tapEnabled = prefs[Keys.TAP_ENABLED] ?: HapticsSettings.Default.tapEnabled,
                intensity = (prefs[Keys.INTENSITY] ?: HapticsSettings.Default.intensity)
                    .coerceIn(0f, 1f),
                pattern = HapticPattern.fromStorageKey(prefs[Keys.PATTERN]),
                scrollEnabled = prefs[Keys.SCROLL_ENABLED] ?: HapticsSettings.Default.scrollEnabled,
                scrollHapticEventsPerHundredPx = (prefs[Keys.SCROLL_HAPTIC_EVENTS_PER_HUNDRED_PX]
                    ?: HapticsSettings.Default.scrollHapticEventsPerHundredPx).coerceIn(
                    HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX,
                    HapticsSettings.MAX_SCROLL_EVENTS_PER_HUNDRED_PX,
                ),
                scrollIntensity = (prefs[Keys.SCROLL_INTENSITY] ?: HapticsSettings.Default.scrollIntensity)
                    .coerceIn(0f, 1f),
                scrollPattern = HapticPattern.fromStorageKey(prefs[Keys.SCROLL_PATTERN])
                    .takeIf { prefs.contains(Keys.SCROLL_PATTERN) } ?: HapticsSettings.Default.scrollPattern,
                scrollVibrationsPerEvent = (prefs[Keys.SCROLL_VIBS_PER_EVENT]
                    ?: HapticsSettings.Default.scrollVibrationsPerEvent).coerceIn(
                    HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT,
                    HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT,
                ),
                scrollSpeedVibrationScale = (prefs[Keys.SCROLL_SPEED_VIB_SCALE]
                    ?: HapticsSettings.Default.scrollSpeedVibrationScale).coerceIn(
                    HapticsSettings.MIN_SCROLL_SPEED_VIB_SCALE,
                    HapticsSettings.MAX_SCROLL_SPEED_VIB_SCALE,
                ),
                scrollTailCutoffMs = (prefs[Keys.SCROLL_TAIL_CUTOFF_MS]
                    ?: HapticsSettings.Default.scrollTailCutoffMs).coerceIn(
                    HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS,
                    HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS,
                ),
                edgePattern = HapticPattern.fromStorageKey(prefs[Keys.EDGE_PATTERN])
                    .takeIf { prefs.contains(Keys.EDGE_PATTERN) } ?: HapticsSettings.Default.edgePattern,
                edgeIntensity = (prefs[Keys.EDGE_INTENSITY] ?: HapticsSettings.Default.edgeIntensity)
                    .coerceIn(0f, 1f),
                a11yScrollBoundEdge = prefs[Keys.A11Y_SCROLL_BOUND_EDGE] ?: HapticsSettings.Default.a11yScrollBoundEdge,
                edgeLsposedLibxposedPath = prefs[Keys.EDGE_LSPOSED_LIBXPOSED_PATH]
                    ?: HapticsSettings.Default.edgeLsposedLibxposedPath,
                useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: HapticsSettings.Default.useDynamicColors,
                themeMode = try {
                    ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: HapticsSettings.Default.themeMode.name)
                } catch (_: Exception) {
                    ThemeMode.SYSTEM
                },
                amoledBlack = prefs[Keys.AMOLED_BLACK] ?: HapticsSettings.Default.amoledBlack,
                seedColor = prefs[Keys.SEED_COLOR] ?: HapticsSettings.Default.seedColor,
                tapExcludedPackages = prefs[Keys.TAP_EXCLUDED_PACKAGES] ?: emptySet(),
                scrollExcludedPackages = prefs[Keys.SCROLL_EXCLUDED_PACKAGES] ?: emptySet(),
                edgeExcludedPackages = prefs[Keys.EDGE_EXCLUDED_PACKAGES] ?: emptySet(),
            )
        }

    suspend fun setTapEnabled(enabled: Boolean) = edit { it[Keys.TAP_ENABLED] = enabled }
    suspend fun setIntensity(intensity: Float) = edit {
        it[Keys.INTENSITY] = intensity.coerceIn(0f, 1f)
    }
    suspend fun setPattern(pattern: HapticPattern) = edit { it[Keys.PATTERN] = pattern.name }
    suspend fun setScrollEnabled(enabled: Boolean) = edit { it[Keys.SCROLL_ENABLED] = enabled }
    suspend fun setScrollPattern(pattern: HapticPattern) = edit { it[Keys.SCROLL_PATTERN] = pattern.name }
    suspend fun setScrollIntensity(intensity: Float) = edit {
        it[Keys.SCROLL_INTENSITY] = intensity.coerceIn(0f, 1f)
    }
    suspend fun setScrollHapticEventsPerHundredPx(value: Float) = edit {
        it[Keys.SCROLL_HAPTIC_EVENTS_PER_HUNDRED_PX] = value.coerceIn(
            HapticsSettings.MIN_SCROLL_EVENTS_PER_HUNDRED_PX,
            HapticsSettings.MAX_SCROLL_EVENTS_PER_HUNDRED_PX,
        )
    }
    suspend fun setScrollVibrationsPerEvent(value: Float) = edit {
        it[Keys.SCROLL_VIBS_PER_EVENT] = value.coerceIn(
            HapticsSettings.MIN_SCROLL_VIBS_PER_EVENT,
            HapticsSettings.MAX_SCROLL_VIBS_PER_EVENT,
        )
    }
    suspend fun setScrollSpeedVibrationScale(value: Float) = edit {
        it[Keys.SCROLL_SPEED_VIB_SCALE] = value.coerceIn(
            HapticsSettings.MIN_SCROLL_SPEED_VIB_SCALE,
            HapticsSettings.MAX_SCROLL_SPEED_VIB_SCALE,
        )
    }
    suspend fun setScrollTailCutoffMs(value: Int) = edit {
        it[Keys.SCROLL_TAIL_CUTOFF_MS] = value.coerceIn(
            HapticsSettings.MIN_SCROLL_TAIL_CUTOFF_MS,
            HapticsSettings.MAX_SCROLL_TAIL_CUTOFF_MS,
        )
    }
    suspend fun setEdgePattern(pattern: HapticPattern) = edit { it[Keys.EDGE_PATTERN] = pattern.name }
    suspend fun setEdgeIntensity(intensity: Float) = edit {
        it[Keys.EDGE_INTENSITY] = intensity.coerceIn(0f, 1f)
    }
    suspend fun setA11yScrollBoundEdge(enabled: Boolean) = edit { it[Keys.A11Y_SCROLL_BOUND_EDGE] = enabled }

    suspend fun setEdgeLsposedLibxposedPath(enabled: Boolean) = edit { it[Keys.EDGE_LSPOSED_LIBXPOSED_PATH] = enabled }

    suspend fun setTapExcludedPackages(packages: Set<String>) = edit { it[Keys.TAP_EXCLUDED_PACKAGES] = packages }
    suspend fun setScrollExcludedPackages(packages: Set<String>) = edit { it[Keys.SCROLL_EXCLUDED_PACKAGES] = packages }
    suspend fun setEdgeExcludedPackages(packages: Set<String>) = edit { it[Keys.EDGE_EXCLUDED_PACKAGES] = packages }

    suspend fun setUseDynamicColors(enabled: Boolean) = edit { it[Keys.USE_DYNAMIC_COLORS] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAmoledBlack(enabled: Boolean) = edit { it[Keys.AMOLED_BLACK] = enabled }
    suspend fun setSeedColor(color: Int) = edit { it[Keys.SEED_COLOR] = color }

    private suspend inline fun edit(crossinline block: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit { block(it) }
        } catch (e: IOException) {
            Log.w(TAG, "DataStore write failed; change will not persist", e)
        }
    }

    private object Keys {
        val TAP_ENABLED = booleanPreferencesKey("tap_enabled")
        val INTENSITY = floatPreferencesKey("intensity")
        val PATTERN = stringPreferencesKey("pattern")
        val SCROLL_ENABLED = booleanPreferencesKey("scroll_enabled")
        val SCROLL_PATTERN = stringPreferencesKey("scroll_pattern")
        val SCROLL_HAPTIC_EVENTS_PER_HUNDRED_PX = floatPreferencesKey("scroll_haptic_events_per_hundred_px")
        val SCROLL_INTENSITY = floatPreferencesKey("scroll_intensity")
        val SCROLL_VIBS_PER_EVENT = floatPreferencesKey("scroll_vibs_per_event")
        val SCROLL_SPEED_VIB_SCALE = floatPreferencesKey("scroll_speed_vib_scale")
        val SCROLL_TAIL_CUTOFF_MS = intPreferencesKey("scroll_tail_cutoff_ms")
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
    }

    private companion object {
        const val TAG = "HapticsPrefs"
    }
}
