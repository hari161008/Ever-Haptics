package com.hapticks.app.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hapticks.app.HapticksApp
import com.hapticks.app.R
import com.hapticks.app.data.MusicHapticsSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicHapticsTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onTileAdded() { refreshTile() }
    override fun onStartListening() { refreshTile() }
    override fun onStopListening() {}

    override fun onClick() {
        // Without the microphone permission the service cannot start, so send the user to the app.
        if (!MusicHapticsService.hasRecordAudioPermission(applicationContext)) {
            openApp()
            return
        }
        scope.launch {
            val prefs = (applicationContext as HapticksApp).preferences
            val settings = prefs.settings.first()
            val newEnabled = !settings.musicHapticsEnabled
            val started = if (newEnabled) MusicHapticsService.start(applicationContext, settings.musicHapticsSource) else true
            val finalEnabled = newEnabled && started
            prefs.setMusicHapticsEnabled(finalEnabled)
            if (!newEnabled) MusicHapticsService.stop(applicationContext)
            withContext(Dispatchers.Main) { refreshTileState(finalEnabled) }
        }
    }

    @Suppress("DEPRECATION")
    private fun openApp() {
        val intent = android.content.Intent(this, com.hapticks.app.MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE),
                )
            } else {
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun refreshTile() {
        scope.launch {
            val prefs = (applicationContext as HapticksApp).preferences
            val isEnabled = prefs.settings.first().musicHapticsEnabled
            withContext(Dispatchers.Main) { refreshTileState(isEnabled) }
        }
    }

    private fun refreshTileState(isEnabled: Boolean) {
        qsTile?.apply {
            state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Music Haptics"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (isEnabled) "On" else "Off"
            }
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_launcher_foreground)
            updateTile()
        }
    }
}
