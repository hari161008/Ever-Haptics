package com.hapticks.app.util

import android.content.Context
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.data.MusicHapticsSource
import com.hapticks.app.data.ThemeMode
import com.hapticks.app.haptics.CustomHapticSequence
import com.hapticks.app.haptics.HapticBeat
import com.hapticks.app.haptics.HapticPattern
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupInfo(
    val appVersion: String,
    val timestamp: Long,
    val formattedDate: String,
    val activeFeaturesCount: Int,
    val settings: HapticsSettings,
)

object BackupManager {

    private const val BACKUP_VERSION = 1

    private fun serializeBeats(seq: CustomHapticSequence): JSONArray {
        val array = JSONArray()
        seq.beats.forEach { beat ->
            val obj = JSONObject()
            obj.put("offsetMs", beat.offsetMs)
            obj.put("amplitude", beat.amplitude)
            array.put(obj)
        }
        return array
    }

    private fun parseBeats(array: JSONArray?): CustomHapticSequence {
        if (array == null) return CustomHapticSequence()
        val list = mutableListOf<HapticBeat>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val offset = obj.optLong("offsetMs", 0L)
            val amplitude = obj.optInt("amplitude", 255)
            list.add(HapticBeat(offset, amplitude))
        }
        return CustomHapticSequence(list)
    }

    private fun stringSetToJson(set: Set<String>): JSONArray {
        val array = JSONArray()
        set.forEach { array.put(it) }
        return array
    }

    private fun jsonToStringSet(array: JSONArray?): Set<String> {
        if (array == null) return emptySet()
        val set = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optString(i, "")
            if (item.isNotBlank()) set.add(item)
        }
        return set
    }

    fun createBackupJson(settings: HapticsSettings): String {
        val root = JSONObject()
        root.put("appName", "Ever Haptics")
        root.put("backupVersion", BACKUP_VERSION)
        root.put("appVersion", "7.0.0")
        root.put("timestamp", System.currentTimeMillis())

        val cfg = JSONObject()
        cfg.put("globalEnabled", settings.globalEnabled)

        // Tap feedback
        cfg.put("tapEnabled", settings.tapEnabled)
        cfg.put("intensity", settings.intensity.toDouble())
        cfg.put("pattern", settings.pattern.name)
        cfg.put("tapHapticCustomSequence", serializeBeats(settings.tapHapticCustomSequence))
        cfg.put("tapExcludedPackages", stringSetToJson(settings.tapExcludedPackages))

        // Tactile scrolling
        cfg.put("scrollEnabled", settings.scrollEnabled)
        cfg.put("scrollHapticEventsPerCm", settings.scrollHapticEventsPerCm.toDouble())
        cfg.put("scrollIntensity", settings.scrollIntensity.toDouble())
        cfg.put("scrollIntensityEnabled", settings.scrollIntensityEnabled)
        cfg.put("scrollPattern", settings.scrollPattern.name)
        cfg.put("scrollVibrationsPerEvent", settings.scrollVibrationsPerEvent.toDouble())
        cfg.put("scrollVibrationsPerEventEnabled", settings.scrollVibrationsPerEventEnabled)
        cfg.put("scrollSpeedVibrationScale", settings.scrollSpeedVibrationScale.toDouble())
        cfg.put("scrollSpeedVibrationEnabled", settings.scrollSpeedVibrationEnabled)
        cfg.put("scrollTailCutoffMs", settings.scrollTailCutoffMs)
        cfg.put("scrollTailCutoffEnabled", settings.scrollTailCutoffEnabled)
        cfg.put("scrollHorizontalEnabled", settings.scrollHorizontalEnabled)
        cfg.put("scrollExcludedPackages", stringSetToJson(settings.scrollExcludedPackages))

        // Charging
        cfg.put("chargingVibEnabled", settings.chargingVibEnabled)
        cfg.put("chargingVibOnConnect", settings.chargingVibOnConnect)
        cfg.put("chargingVibOnDisconnect", settings.chargingVibOnDisconnect)
        cfg.put("chargingVibPattern", settings.chargingVibPattern.name)
        cfg.put("chargingVibIntensity", settings.chargingVibIntensity.toDouble())
        cfg.put("chargingVibCustomSequence", serializeBeats(settings.chargingVibCustomSequence))

        // Buttons: Volume
        cfg.put("volumeHapticEnabled", settings.volumeHapticEnabled)
        cfg.put("volumeHapticPattern", settings.volumeHapticPattern.name)
        cfg.put("volumeHapticIntensity", settings.volumeHapticIntensity.toDouble())
        cfg.put("volumeHapticCustomSequence", serializeBeats(settings.volumeHapticCustomSequence))

        // Buttons: Power
        cfg.put("powerHapticEnabled", settings.powerHapticEnabled)
        cfg.put("powerHapticPattern", settings.powerHapticPattern.name)
        cfg.put("powerHapticIntensity", settings.powerHapticIntensity.toDouble())
        cfg.put("powerHapticCustomSequence", serializeBeats(settings.powerHapticCustomSequence))

        // Buttons: Brightness
        cfg.put("brightnessHapticEnabled", settings.brightnessHapticEnabled)
        cfg.put("brightnessHapticPattern", settings.brightnessHapticPattern.name)
        cfg.put("brightnessHapticIntensity", settings.brightnessHapticIntensity.toDouble())
        cfg.put("brightnessHapticCustomSequence", serializeBeats(settings.brightnessHapticCustomSequence))

        // NavBar
        cfg.put("navBarHapticEnabled", settings.navBarHapticEnabled)
        cfg.put("navBarHapticPattern", settings.navBarHapticPattern.name)
        cfg.put("navBarHapticIntensity", settings.navBarHapticIntensity.toDouble())
        cfg.put("navBarHapticCustomSequence", serializeBeats(settings.navBarHapticCustomSequence))

        // Status Bar
        cfg.put("statusBarHapticEnabled", settings.statusBarHapticEnabled)
        cfg.put("statusBarExpandEnabled", settings.statusBarExpandEnabled)
        cfg.put("statusBarExpandPattern", settings.statusBarExpandPattern.name)
        cfg.put("statusBarExpandIntensity", settings.statusBarExpandIntensity.toDouble())
        cfg.put("statusBarExpandCustomSequence", serializeBeats(settings.statusBarExpandCustomSequence))
        cfg.put("statusBarCollapseEnabled", settings.statusBarCollapseEnabled)
        cfg.put("statusBarCollapsePattern", settings.statusBarCollapsePattern.name)
        cfg.put("statusBarCollapseIntensity", settings.statusBarCollapseIntensity.toDouble())
        cfg.put("statusBarCollapseCustomSequence", serializeBeats(settings.statusBarCollapseCustomSequence))

        // Unlock
        cfg.put("unlockHapticEnabled", settings.unlockHapticEnabled)
        cfg.put("unlockHapticPattern", settings.unlockHapticPattern.name)
        cfg.put("unlockHapticIntensity", settings.unlockHapticIntensity.toDouble())
        cfg.put("unlockHapticCustomSequence", serializeBeats(settings.unlockHapticCustomSequence))

        // Keyboard
        cfg.put("keyboardHapticEnabled", settings.keyboardHapticEnabled)
        cfg.put("keyboardHapticPattern", settings.keyboardHapticPattern.name)
        cfg.put("keyboardHapticIntensity", settings.keyboardHapticIntensity.toDouble())
        cfg.put("keyboardHapticCustomSequence", serializeBeats(settings.keyboardHapticCustomSequence))

        // Theme & Appearance
        cfg.put("useDynamicColors", settings.useDynamicColors)
        cfg.put("themeMode", settings.themeMode.name)
        cfg.put("amoledBlack", settings.amoledBlack)
        cfg.put("seedColor", settings.seedColor)

        // System & Motion
        cfg.put("batterySaverDetectionEnabled", settings.batterySaverDetectionEnabled)
        cfg.put("autoCheckUpdatesEnabled", settings.autoCheckUpdatesEnabled)
        cfg.put("predictiveBackEnabled", settings.predictiveBackEnabled)
        cfg.put("depthBlurEnabled", settings.depthBlurEnabled)

        // Music Haptics
        cfg.put("musicHapticsEnabled", settings.musicHapticsEnabled)
        cfg.put("musicHapticsSource", settings.musicHapticsSource.name)
        cfg.put("musicHapticsSensitivity", settings.musicHapticsSensitivity.toDouble())
        cfg.put("musicHapticsStrength", settings.musicHapticsStrength.toDouble())

        // Call, Notification & Alarm
        cfg.put("callHapticEnabled", settings.callHapticEnabled)
        cfg.put("callHapticPattern", settings.callHapticPattern.name)
        cfg.put("callHapticIntensity", settings.callHapticIntensity.toDouble())
        cfg.put("callHapticCustomSequence", serializeBeats(settings.callHapticCustomSequence))
        cfg.put("callHapticAuto", settings.callHapticAuto)
        cfg.put("callAutoSensitivity", settings.callAutoSensitivity.toDouble())
        cfg.put("callAutoStrength", settings.callAutoStrength.toDouble())

        cfg.put("notifHapticEnabled", settings.notifHapticEnabled)
        cfg.put("notifHapticPattern", settings.notifHapticPattern.name)
        cfg.put("notifHapticIntensity", settings.notifHapticIntensity.toDouble())
        cfg.put("notifHapticCustomSequence", serializeBeats(settings.notifHapticCustomSequence))

        cfg.put("alarmHapticEnabled", settings.alarmHapticEnabled)
        cfg.put("alarmHapticPattern", settings.alarmHapticPattern.name)
        cfg.put("alarmHapticIntensity", settings.alarmHapticIntensity.toDouble())
        cfg.put("alarmHapticCustomSequence", serializeBeats(settings.alarmHapticCustomSequence))
        cfg.put("alarmHapticAuto", settings.alarmHapticAuto)
        cfg.put("alarmAutoSensitivity", settings.alarmAutoSensitivity.toDouble())
        cfg.put("alarmAutoStrength", settings.alarmAutoStrength.toDouble())

        root.put("settings", cfg)
        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): HapticsSettings? {
        return try {
            val root = JSONObject(jsonString)
            val cfg = root.optJSONObject("settings") ?: return null

            fun optPattern(key: String, default: HapticPattern): HapticPattern {
                val str = cfg.optString(key, default.name)
                return HapticPattern.fromStorageKey(str)
            }

            fun optFloat(key: String, default: Float): Float {
                return cfg.optDouble(key, default.toDouble()).toFloat()
            }

            HapticsSettings(
                globalEnabled = cfg.optBoolean("globalEnabled", true),
                tapEnabled = cfg.optBoolean("tapEnabled", false),
                intensity = optFloat("intensity", 1.0f).coerceIn(0f, 1f),
                pattern = optPattern("pattern", HapticPattern.Default),
                tapHapticCustomSequence = parseBeats(cfg.optJSONArray("tapHapticCustomSequence")),
                tapExcludedPackages = jsonToStringSet(cfg.optJSONArray("tapExcludedPackages")),

                scrollEnabled = cfg.optBoolean("scrollEnabled", false),
                scrollHapticEventsPerCm = optFloat("scrollHapticEventsPerCm", 1.0f),
                scrollIntensity = optFloat("scrollIntensity", 0.45f).coerceIn(0f, 1f),
                scrollIntensityEnabled = cfg.optBoolean("scrollIntensityEnabled", false),
                scrollPattern = optPattern("scrollPattern", HapticPattern.TICK),
                scrollVibrationsPerEvent = optFloat("scrollVibrationsPerEvent", 1f),
                scrollVibrationsPerEventEnabled = cfg.optBoolean("scrollVibrationsPerEventEnabled", false),
                scrollSpeedVibrationScale = optFloat("scrollSpeedVibrationScale", 0f),
                scrollSpeedVibrationEnabled = cfg.optBoolean("scrollSpeedVibrationEnabled", false),
                scrollTailCutoffMs = cfg.optInt("scrollTailCutoffMs", 0),
                scrollTailCutoffEnabled = cfg.optBoolean("scrollTailCutoffEnabled", false),
                scrollHorizontalEnabled = cfg.optBoolean("scrollHorizontalEnabled", false),
                scrollExcludedPackages = jsonToStringSet(cfg.optJSONArray("scrollExcludedPackages")),

                chargingVibEnabled = cfg.optBoolean("chargingVibEnabled", false),
                chargingVibOnConnect = cfg.optBoolean("chargingVibOnConnect", false),
                chargingVibOnDisconnect = cfg.optBoolean("chargingVibOnDisconnect", false),
                chargingVibPattern = optPattern("chargingVibPattern", HapticPattern.HEAVY_CLICK),
                chargingVibIntensity = optFloat("chargingVibIntensity", 1.0f).coerceIn(0f, 1f),
                chargingVibCustomSequence = parseBeats(cfg.optJSONArray("chargingVibCustomSequence")),

                volumeHapticEnabled = cfg.optBoolean("volumeHapticEnabled", false),
                volumeHapticPattern = optPattern("volumeHapticPattern", HapticPattern.TICK),
                volumeHapticIntensity = optFloat("volumeHapticIntensity", 0.7f).coerceIn(0f, 1f),
                volumeHapticCustomSequence = parseBeats(cfg.optJSONArray("volumeHapticCustomSequence")),

                powerHapticEnabled = cfg.optBoolean("powerHapticEnabled", false),
                powerHapticPattern = optPattern("powerHapticPattern", HapticPattern.HEAVY_CLICK),
                powerHapticIntensity = optFloat("powerHapticIntensity", 1.0f).coerceIn(0f, 1f),
                powerHapticCustomSequence = parseBeats(cfg.optJSONArray("powerHapticCustomSequence")),

                brightnessHapticEnabled = cfg.optBoolean("brightnessHapticEnabled", false),
                brightnessHapticPattern = optPattern("brightnessHapticPattern", HapticPattern.TICK),
                brightnessHapticIntensity = optFloat("brightnessHapticIntensity", 0.5f).coerceIn(0f, 1f),
                brightnessHapticCustomSequence = parseBeats(cfg.optJSONArray("brightnessHapticCustomSequence")),

                navBarHapticEnabled = cfg.optBoolean("navBarHapticEnabled", false),
                navBarHapticPattern = optPattern("navBarHapticPattern", HapticPattern.CLICK),
                navBarHapticIntensity = optFloat("navBarHapticIntensity", 0.8f).coerceIn(0f, 1f),
                navBarHapticCustomSequence = parseBeats(cfg.optJSONArray("navBarHapticCustomSequence")),

                statusBarHapticEnabled = cfg.optBoolean("statusBarHapticEnabled", false),
                statusBarExpandEnabled = cfg.optBoolean("statusBarExpandEnabled", false),
                statusBarExpandPattern = optPattern("statusBarExpandPattern", HapticPattern.TICK),
                statusBarExpandIntensity = optFloat("statusBarExpandIntensity", 0.7f).coerceIn(0f, 1f),
                statusBarExpandCustomSequence = parseBeats(cfg.optJSONArray("statusBarExpandCustomSequence")),
                statusBarCollapseEnabled = cfg.optBoolean("statusBarCollapseEnabled", false),
                statusBarCollapsePattern = optPattern("statusBarCollapsePattern", HapticPattern.TICK),
                statusBarCollapseIntensity = optFloat("statusBarCollapseIntensity", 0.7f).coerceIn(0f, 1f),
                statusBarCollapseCustomSequence = parseBeats(cfg.optJSONArray("statusBarCollapseCustomSequence")),

                unlockHapticEnabled = cfg.optBoolean("unlockHapticEnabled", false),
                unlockHapticPattern = optPattern("unlockHapticPattern", HapticPattern.DOUBLE_CLICK),
                unlockHapticIntensity = optFloat("unlockHapticIntensity", 0.8f).coerceIn(0f, 1f),
                unlockHapticCustomSequence = parseBeats(cfg.optJSONArray("unlockHapticCustomSequence")),

                keyboardHapticEnabled = cfg.optBoolean("keyboardHapticEnabled", false),
                keyboardHapticPattern = optPattern("keyboardHapticPattern", HapticPattern.TICK),
                keyboardHapticIntensity = optFloat("keyboardHapticIntensity", 0.5f).coerceIn(0f, 1f),
                keyboardHapticCustomSequence = parseBeats(cfg.optJSONArray("keyboardHapticCustomSequence")),

                useDynamicColors = cfg.optBoolean("useDynamicColors", true),
                themeMode = try { ThemeMode.valueOf(cfg.optString("themeMode", ThemeMode.SYSTEM.name)) } catch (_: Exception) { ThemeMode.SYSTEM },
                amoledBlack = cfg.optBoolean("amoledBlack", false),
                seedColor = cfg.optInt("seedColor", 0xFF6750A4.toInt()),

                batterySaverDetectionEnabled = cfg.optBoolean("batterySaverDetectionEnabled", false),
                autoCheckUpdatesEnabled = cfg.optBoolean("autoCheckUpdatesEnabled", true),
                predictiveBackEnabled = cfg.optBoolean("predictiveBackEnabled", true),
                depthBlurEnabled = cfg.optBoolean("depthBlurEnabled", true),

                musicHapticsEnabled = cfg.optBoolean("musicHapticsEnabled", false),
                musicHapticsSource = try { MusicHapticsSource.valueOf(cfg.optString("musicHapticsSource", MusicHapticsSource.Default.name)) } catch (_: Exception) { MusicHapticsSource.Default },
                musicHapticsSensitivity = optFloat("musicHapticsSensitivity", 0.65f).coerceIn(0f, 1f),
                musicHapticsStrength = optFloat("musicHapticsStrength", 0.7f).coerceIn(0f, 1f),

                callHapticEnabled = cfg.optBoolean("callHapticEnabled", false),
                callHapticPattern = optPattern("callHapticPattern", HapticPattern.HEAVY_CLICK),
                callHapticIntensity = optFloat("callHapticIntensity", 1.0f).coerceIn(0f, 1f),
                callHapticCustomSequence = parseBeats(cfg.optJSONArray("callHapticCustomSequence")),
                callHapticAuto = cfg.optBoolean("callHapticAuto", false),
                callAutoSensitivity = optFloat("callAutoSensitivity", 0.65f).coerceIn(0f, 1f),
                callAutoStrength = optFloat("callAutoStrength", 0.8f).coerceIn(0f, 1f),

                notifHapticEnabled = cfg.optBoolean("notifHapticEnabled", false),
                notifHapticPattern = optPattern("notifHapticPattern", HapticPattern.CLICK),
                notifHapticIntensity = optFloat("notifHapticIntensity", 0.7f).coerceIn(0f, 1f),
                notifHapticCustomSequence = parseBeats(cfg.optJSONArray("notifHapticCustomSequence")),

                alarmHapticEnabled = cfg.optBoolean("alarmHapticEnabled", false),
                alarmHapticPattern = optPattern("alarmHapticPattern", HapticPattern.DOUBLE_CLICK),
                alarmHapticIntensity = optFloat("alarmHapticIntensity", 1.0f).coerceIn(0f, 1f),
                alarmHapticCustomSequence = parseBeats(cfg.optJSONArray("alarmHapticCustomSequence")),
                alarmHapticAuto = cfg.optBoolean("alarmHapticAuto", false),
                alarmAutoSensitivity = optFloat("alarmAutoSensitivity", 0.65f).coerceIn(0f, 1f),
                alarmAutoStrength = optFloat("alarmAutoStrength", 0.8f).coerceIn(0f, 1f),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun inspectBackup(jsonString: String): BackupInfo? {
        return try {
            val root = JSONObject(jsonString)
            val appVersion = root.optString("appVersion", "7.0.0")
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val formattedDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
            val settings = parseBackupJson(jsonString) ?: return null

            var count = 0
            if (settings.tapEnabled) count++
            if (settings.scrollEnabled) count++
            if (settings.chargingVibEnabled) count++
            if (settings.volumeHapticEnabled) count++
            if (settings.powerHapticEnabled) count++
            if (settings.brightnessHapticEnabled) count++
            if (settings.navBarHapticEnabled) count++
            if (settings.statusBarHapticEnabled) count++
            if (settings.unlockHapticEnabled) count++
            if (settings.keyboardHapticEnabled) count++
            if (settings.musicHapticsEnabled) count++
            if (settings.callHapticEnabled) count++
            if (settings.notifHapticEnabled) count++
            if (settings.alarmHapticEnabled) count++

            BackupInfo(
                appVersion = appVersion,
                timestamp = timestamp,
                formattedDate = formattedDate,
                activeFeaturesCount = count,
                settings = settings,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun writeBackup(output: OutputStream, settings: HapticsSettings): Boolean {
        return try {
            val json = createBackupJson(settings)
            output.bufferedWriter().use { it.write(json) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBackup(input: InputStream): String? {
        return try {
            input.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    fun createBackupFile(context: Context, settings: HapticsSettings): java.io.File? {
        return try {
            val backupDir = java.io.File(context.cacheDir, "backups").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = java.io.File(backupDir, "EverHaptics_backup_$timestamp.json")
            val json = createBackupJson(settings)
            file.writeText(json)
            file
        } catch (_: Exception) {
            null
        }
    }

    fun writeBackupToUri(context: Context, uri: android.net.Uri, settings: HapticsSettings): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                writeBackup(out, settings)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: android.net.Uri): BackupInfo? {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                readBackup(input)
            } ?: return null
            inspectBackup(json)
        } catch (_: Exception) {
            null
        }
    }
}
