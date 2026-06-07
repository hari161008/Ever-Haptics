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
        scope.launch {
            val prefs = (applicationContext as HapticksApp).preferences
            val settings = prefs.settings.first()
            val newEnabled = !settings.musicHapticsEnabled
            prefs.setMusicHapticsEnabled(newEnabled)
            if (newEnabled) {
                MusicHapticsService.start(applicationContext, settings.musicHapticsSource)
            } else {
                MusicHapticsService.stop(applicationContext)
            }
            withContext(Dispatchers.Main) { refreshTileState(newEnabled) }
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
