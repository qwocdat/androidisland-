package com.xiao.androidisland

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xiao.androidisland.liquidisland.controlcenter.IosControlCenterScreen

/**
 * Translucent full-screen launcher activity for the iOS Control Center (`00:49`–`01:06` of `96942.mp4`).
 */
class LiquidControlCenterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val systemRepo = AndroidIslandApp.system(this)

        setContent {
            val statusBar by systemRepo.statusBarState.collectAsState()
            val islandState by systemRepo.islandState.collectAsState()
            val media by systemRepo.activeMedia.collectAsState()
            val settings by systemRepo.settings.collectAsState()
            var brightness by remember { mutableFloatStateOf(systemRepo.getBrightnessFraction()) }
            var volume by remember { mutableFloatStateOf(systemRepo.getVolumeFraction()) }
            var flashlightOn by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent
            ) {
                IosControlCenterScreen(
                    statusBarState = statusBar,
                    islandState = islandState,
                    mediaData = media,
                    settings = settings,
                    brightnessFraction = brightness,
                    volumeFraction = volume,
                    isFlashlightOn = flashlightOn,
                    onBrightnessChange = {
                        brightness = it
                        systemRepo.setBrightnessFraction(it)
                    },
                    onVolumeChange = {
                        volume = it
                        systemRepo.setVolumeFraction(it)
                    },
                    onToggleWifi = { systemRepo.toggleWifi() },
                    onToggleCellular = { systemRepo.toggleMobileData() },
                    onToggleBluetooth = { systemRepo.toggleBluetooth() },
                    onToggleAirplane = { systemRepo.toggleAirplaneMode() },
                    onToggleHotspot = { systemRepo.toggleHotspot() },
                    onToggleFlashlight = {
                        flashlightOn = !flashlightOn
                        systemRepo.toggleFlashlight(flashlightOn)
                    },
                    onTriggerRingerPreview = { mode ->
                        systemRepo.triggerRingerAlert(mode)
                    },
                    onMediaPlayPause = { systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                    onMediaNext = { systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) },
                    onMediaPrevious = { systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                    onDismiss = { finish() }
                )
            }
        }
    }
}
