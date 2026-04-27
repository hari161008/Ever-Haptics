package com.hapticks.app.xposed

import com.hapticks.app.data.HapticsSettings
import io.github.libxposed.service.XposedService

object XposedEdgeRemotePrefs {

    const val GROUP: String = "edge"

    const val KEY_ENABLED: String = "edge_lsposed_libxposed_path"
    const val KEY_PATTERN: String = "edge_pattern"
    const val KEY_INTENSITY: String = "edge_intensity"

    fun push(service: XposedService, settings: HapticsSettings) {
        val prefs = service.getRemotePreferences(GROUP) ?: return
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.edgeLsposedLibxposedPath)
            putString(KEY_PATTERN, settings.edgePattern.name)
            putFloat(KEY_INTENSITY, settings.edgeIntensity)
            apply()
        }
    }
}
