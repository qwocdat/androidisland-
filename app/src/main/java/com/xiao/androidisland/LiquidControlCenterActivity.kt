package com.xiao.androidisland

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xiao.androidisland.liquidisland.controlcenter.IosControlCenterSheet
import com.xiao.androidisland.liquidisland.renderer.rememberLayerBackdrop
import com.xiao.androidisland.liquidisland.renderer.rememberLiquidEffectController

class LiquidControlCenterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val systemRepo = AndroidIslandApp.system(this)

        setContent {
            val status by systemRepo.statusSnapshot.collectAsState()
            val track by systemRepo.currentTrack.collectAsState()
            val backdrop = rememberLayerBackdrop()
            val effectController = rememberLiquidEffectController()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xCC0A0F1D)
            ) {
                IosControlCenterSheet(
                    status = status,
                    track = track,
                    backdrop = backdrop,
                    effectController = effectController,
                    onToggleWifi = { systemRepo.toggleWifi(this@LiquidControlCenterActivity) },
                    onToggleAirplane = { systemRepo.toggleAirplaneMode(this@LiquidControlCenterActivity) },
                    onToggleMobileData = { systemRepo.toggleMobileData(this@LiquidControlCenterActivity) },
                    onToggleHotspot = { systemRepo.toggleHotspot(this@LiquidControlCenterActivity) },
                    onCycleRingerMode = { systemRepo.cycleRingerMode() },
                    onBrightnessChange = { systemRepo.setBrightnessPercent(it) },
                    onVolumeChange = { systemRepo.setVolumePercent(it) },
                    onOpenSettings = {
                        startActivity(Intent(this@LiquidControlCenterActivity, MainActivity::class.java))
                        finish()
                    },
                    onCloseControlCenter = { finish() },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        }
    }
}
