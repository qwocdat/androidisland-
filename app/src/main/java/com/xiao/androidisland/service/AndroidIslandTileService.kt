package com.xiao.androidisland.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xiao.androidisland.AndroidIslandApp
import com.xiao.androidisland.LiquidControlCenterActivity

class AndroidIslandTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        AndroidIslandApp.systemOrNull(this)?.setControlCenterVisible(true)
        val intent = Intent(this, LiquidControlCenterActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }
}
