package com.coolappstore.everhaptics.by.svhp.xposed

import com.coolappstore.everhaptics.by.svhp.data.HapticsSettings
import io.github.libxposed.service.XposedService

object XposedEdgeRemotePrefs {

    const val GROUP: String = "edge"

    const val KEY_ENABLED: String = "edge_lsposed_libxposed_path"
    const val KEY_PATTERN: String = "edge_pattern"
    const val KEY_INTENSITY: String = "edge_intensity"

    fun push(service: XposedService, settings: HapticsSettings) {
        val prefs = service.getRemotePreferences(GROUP) ?: return
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, false)
            putString(KEY_PATTERN, "TICK")
            putFloat(KEY_INTENSITY, 0.5f)
            apply()
        }
    }
}
