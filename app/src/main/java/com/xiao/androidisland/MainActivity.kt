package com.xiao.androidisland

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiao.androidisland.core.model.IslandAppMode
import com.xiao.androidisland.core.model.RingerModeState
import com.xiao.androidisland.droidbubble.DroidBubbleOverlayBar
import com.xiao.androidisland.liquidisland.absorption.AbsorptionEngine
import com.xiao.androidisland.liquidisland.controlcenter.IosControlCenterScreen
import com.xiao.androidisland.liquidisland.engine.LiquidIslandOverlayHost
import com.xiao.androidisland.liquidisland.renderer.LiquidGlassNativeBridge
import com.xiao.androidisland.liquidisland.settings.LiquidIslandSettingsScreen
import com.xiao.androidisland.liquidisland.statusbar.DynamicStatusBar
import com.xiao.androidisland.liquidisland.widgets.HomeScreenWidgetsSection
import com.xiao.androidisland.service.AndroidIslandOverlayService
import com.xiao.androidisland.service.IslandNotificationListenerService

/**
 * Main interactive workspace and settings host for AndroidIsland – LiquidIsland.
 *
 * Mirrors the exact visual layout of `96942.mp4` ("Dynamic Motion 1.2 For Android"):
 * - Top iOS Dynamic Status Bar with real-time Edge-Based Liquid Absorption & Directional Motion Blur
 * - Coupled Spring-Physics Dynamic Island (`LiquidIslandOverlayHost` or preserved `DroidBubbleOverlayBar`)
 * - Home Screen Preview tab featuring the Dual-Month Calendar Widget, App Tiles (`Spotify`, `Threads`),
 *   and Live Analog Clock Widget (`00:00`–`00:48` in `96942.mp4`) + interactive state triggers for testing all 10 video timelines
 * - Full-screen iOS Control Center (`00:49`–`01:06` in `96942.mp4`)
 * - LiquidIsland Settings tab (`01:19`–`01:25` in `96942.mp4`: `LOCK SCREEN AUTH`, `MEDIA STYLE`, `LIQUID GLASS`, permissions)
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val systemRepo = AndroidIslandApp.system(this)
        val shizukuBridge = (application as AndroidIslandApp).shizukuBridge

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val settings by systemRepo.settings.collectAsState()
                val statusBarState by systemRepo.statusBarState.collectAsState()
                val islandState by systemRepo.islandState.collectAsState()
                val mediaData by systemRepo.activeMedia.collectAsState()
                val shizukuState by shizukuBridge.state.collectAsState()
                val isControlCenterOpen by systemRepo.isControlCenterVisible.collectAsState()

                var selectedBottomTab by remember { mutableIntStateOf(0) } // 0 = Home Preview, 1 = Control Center, 2 = Settings
                var islandWidthDp by remember { mutableFloatStateOf(120f) }
                var islandVelocity by remember { mutableFloatStateOf(0f) }
                var splitOffsetDp by remember { mutableFloatStateOf(0f) }

                var brightness by remember { mutableFloatStateOf(systemRepo.getBrightnessFraction()) }
                var volume by remember { mutableFloatStateOf(systemRepo.getVolumeFraction()) }
                var flashlightOn by remember { mutableStateOf(false) }

                val absorptionFrame = remember(islandWidthDp, islandVelocity, splitOffsetDp, settings) {
                    AbsorptionEngine.evaluateFrame(
                        screenWidthDp = 360f,
                        islandWidthDp = islandWidthDp,
                        islandVelocityDpPerSec = islandVelocity,
                        splitRightBubbleOffsetDp = splitOffsetDp,
                        absorptionStrength = settings.absorptionStrength,
                        motionBlurIntensity = settings.motionBlurIntensity
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0F1D)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // iOS Wallpaper Backdrop matching reference video 96942.mp4
                        WallpaperBackdrop()

                        // Main screen content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            // Top reserve space for Dynamic Status Bar + Dynamic Island
                            Spacer(modifier = Modifier.height(54.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (selectedBottomTab) {
                                    0 -> HomeReferenceWorkspace(
                                        absorptionLeftProgress = absorptionFrame.leftCluster.absorptionProgress,
                                        absorptionRightProgress = absorptionFrame.rightCluster.absorptionProgress,
                                        nativeShaderActive = LiquidGlassNativeBridge.isNativeAvailable,
                                        onTriggerTelegramCompact = {
                                            systemRepo.triggerTelegramNotificationDemo(expanded = false)
                                        },
                                        onTriggerTelegramExpanded = {
                                            systemRepo.triggerTelegramNotificationDemo(expanded = true)
                                        },
                                        onTriggerMetrolistBanner = {
                                            systemRepo.triggerMetrolistUpdateBanner()
                                        },
                                        onTriggerCompactMusic = {
                                            systemRepo.triggerMusicIsland(expanded = false)
                                        },
                                        onTriggerSplitActivity = {
                                            systemRepo.triggerSplitDualActivity()
                                        },
                                        onTriggerExpandedMusic = {
                                            systemRepo.triggerMusicIsland(expanded = true)
                                        },
                                        onTriggerTimerCompact = {
                                            systemRepo.startOrToggleTimer(expanded = false)
                                        },
                                        onTriggerTimerExpanded = {
                                            systemRepo.startOrToggleTimer(expanded = true)
                                        },
                                        onTriggerNavCompact = {
                                            systemRepo.triggerNavigationIsland(expanded = false)
                                        },
                                        onTriggerNavExpanded = {
                                            systemRepo.triggerNavigationIsland(expanded = true)
                                        },
                                        onTriggerRinger = { mode ->
                                            systemRepo.triggerRingerAlert(mode)
                                        },
                                        onTriggerCharging = {
                                            systemRepo.triggerChargingAlert(80)
                                        },
                                        onTriggerBiometric = {
                                            systemRepo.triggerBiometricAuthSequence()
                                        },
                                        onResetIdle = {
                                            systemRepo.resetToIdle()
                                        },
                                        onStartOverlayService = {
                                            if (Settings.canDrawOverlays(this@MainActivity)) {
                                                AndroidIslandOverlayService.startOrUpdate(this@MainActivity)
                                            } else {
                                                startActivity(
                                                    Intent(
                                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        Uri.parse("package:$packageName")
                                                    )
                                                )
                                            }
                                        }
                                    )

                                    1 -> IosControlCenterScreen(
                                        statusBarState = statusBarState,
                                        islandState = islandState,
                                        mediaData = mediaData,
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
                                        onMediaPlayPause = {
                                            systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                        },
                                        onMediaNext = {
                                            systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                                        },
                                        onMediaPrevious = {
                                            systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                        },
                                        onDismiss = { selectedBottomTab = 0 }
                                    )

                                    else -> LiquidIslandSettingsScreen(
                                        settings = settings,
                                        islandState = islandState,
                                        shizukuState = shizukuState,
                                        hasOverlayPermission = Settings.canDrawOverlays(this@MainActivity),
                                        hasNotificationAccess = isNotificationServiceEnabled(),
                                        hasWriteSettings = Settings.System.canWrite(this@MainActivity),
                                        onUpdateSettings = { transform ->
                                            systemRepo.updateSettings(transform)
                                        },
                                        onTriggerBiometricDemo = {
                                            systemRepo.triggerBiometricAuthSequence()
                                        },
                                        onRequestOverlayPermission = {
                                            startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:$packageName")
                                                )
                                            )
                                        },
                                        onRequestNotificationAccess = {
                                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        },
                                        onRequestAccessibility = {
                                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        },
                                        onRequestWriteSettings = {
                                            startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                    Uri.parse("package:$packageName")
                                                )
                                            )
                                        },
                                        onRequestShizuku = {
                                            shizukuBridge.requestPermissionIfNeeded()
                                        }
                                    )
                                }
                            }

                            // Bottom Liquid Glass Dock Bar
                            LiquidBottomNavigationDock(
                                selectedTab = selectedBottomTab,
                                currentMode = settings.mode,
                                onSelectTab = { selectedBottomTab = it },
                                onToggleMode = {
                                    systemRepo.updateSettings { s ->
                                        s.copy(
                                            mode = if (s.mode == IslandAppMode.LIQUID_ISLAND) {
                                                IslandAppMode.DROID_BUBBLE
                                            } else {
                                                IslandAppMode.LIQUID_ISLAND
                                            }
                                        )
                                    }
                                }
                            )
                        }

                        // Persistent Top Layer: Dynamic iOS Status Bar + Dynamic Island with Edge-Based Liquid Absorption
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .align(Alignment.TopCenter),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (settings.mode == IslandAppMode.DROID_BUBBLE) {
                                DroidBubbleOverlayBar(
                                    islandState = islandState,
                                    onExpandToggle = { systemRepo.onIslandTapped() }
                                )
                            } else {
                                if (settings.replaceSystemStatusBar) {
                                    DynamicStatusBar(
                                        state = statusBarState,
                                        absorptionFrame = absorptionFrame,
                                        onLeftSwipeDown = {
                                            systemRepo.setControlCenterVisible(false)
                                        },
                                        onRightSwipeDown = {
                                            systemRepo.setControlCenterVisible(true)
                                        }
                                    )
                                }

                                LiquidIslandOverlayHost(
                                    islandState = islandState,
                                    settings = settings,
                                    onGeometryFrame = { wDp, velDpPerSec, splitOffDp ->
                                        islandWidthDp = wDp
                                        islandVelocity = velDpPerSec
                                        splitOffsetDp = splitOffDp
                                    },
                                    onPrimaryTap = { systemRepo.onIslandTapped() },
                                    onDismissIsland = { systemRepo.collapseIslandToCompactOrIdle() },
                                    onMediaPlayPause = {
                                        systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                    },
                                    onMediaNext = {
                                        systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                                    },
                                    onMediaPrevious = {
                                        systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                    },
                                    onMediaSeek = { posMs ->
                                        IslandNotificationListenerService.instance?.seekActiveSessionTo(posMs)
                                        systemRepo.seekMediaTo(posMs)
                                    },
                                    onTimerCancel = { systemRepo.cancelTimer() },
                                    onNavigationEnd = { systemRepo.endNavigation() }
                                )
                            }
                        }

                        // Pull-down full-screen iOS Control Center overlay when triggered from top-right swipe
                        AnimatedVisibility(
                            visible = isControlCenterOpen,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IosControlCenterScreen(
                                statusBarState = statusBarState,
                                islandState = islandState,
                                mediaData = mediaData,
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
                                onMediaPlayPause = {
                                    systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                                },
                                onMediaNext = {
                                    systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                                },
                                onMediaPrevious = {
                                    systemRepo.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                },
                                onDismiss = {
                                    systemRepo.setControlCenterVisible(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        return enabledListeners.contains(packageName)
    }
}

@Composable
private fun WallpaperBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF4F74A8),
                    Color(0xFF8AA7CE),
                    Color(0xFF3E5E8E),
                    Color(0xFF192844)
                )
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x55FFFFFF), Color.Transparent),
                center = Offset(size.width * 0.25f, size.height * 0.22f),
                radius = size.width * 0.65f
            ),
            center = Offset(size.width * 0.25f, size.height * 0.22f),
            radius = size.width * 0.65f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66224070), Color.Transparent),
                center = Offset(size.width * 0.78f, size.height * 0.55f),
                radius = size.width * 0.75f
            ),
            center = Offset(size.width * 0.78f, size.height * 0.55f),
            radius = size.width * 0.75f
        )
    }
}

@Composable
private fun HomeReferenceWorkspace(
    absorptionLeftProgress: Float,
    absorptionRightProgress: Float,
    nativeShaderActive: Boolean,
    onTriggerTelegramCompact: () -> Unit,
    onTriggerTelegramExpanded: () -> Unit,
    onTriggerMetrolistBanner: () -> Unit,
    onTriggerCompactMusic: () -> Unit,
    onTriggerSplitActivity: () -> Unit,
    onTriggerExpandedMusic: () -> Unit,
    onTriggerTimerCompact: () -> Unit,
    onTriggerTimerExpanded: () -> Unit,
    onTriggerNavCompact: () -> Unit,
    onTriggerNavExpanded: () -> Unit,
    onTriggerRinger: (RingerModeState) -> Unit,
    onTriggerCharging: () -> Unit,
    onTriggerBiometric: () -> Unit,
    onResetIdle: () -> Unit,
    onStartOverlayService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Exact Home Screen Widgets from 00:00 - 00:48 of 96942.mp4
        HomeScreenWidgetsSection(
            onSpotifyClick = onTriggerExpandedMusic,
            onThreadsClick = onTriggerTelegramExpanded,
            onClockClick = onTriggerTimerExpanded
        )

        // 2. Interactive Dynamic Motion 1.2 Timeline State Controller
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xB8101828))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DYNAMIC MOTION 1.2 TIMELINE STATES",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Absorption L=${(absorptionLeftProgress * 100).toInt()}% • R=${(absorptionRightProgress * 100).toInt()}% • NDK=${if (nativeShaderActive) "GLES3" else "AGSL"}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { onStartOverlayService() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Enable Overlay",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Row 1: Notifications & Music (00:00 - 00:18)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateChip("00:00 Idle Pill", Color(0xFF334155), onResetIdle)
                StateChip("00:01 Telegram Pill", Color(0xFF0284C7), onTriggerTelegramCompact)
                StateChip("00:04 Telegram Card", Color(0xFF0369A1), onTriggerTelegramExpanded)
                StateChip("00:07 Compact Music", Color(0xFF7C3AED), onTriggerCompactMusic)
                StateChip("00:09 Split Metaball", Color(0xFF9333EA), onTriggerSplitActivity)
                StateChip("00:10 Metrolist 13.7.0", Color(0xFF4F46E5), onTriggerMetrolistBanner)
                StateChip("00:13 Jane!! Music Card", Color(0xFF6D28D9), onTriggerExpandedMusic)
            }

            // Row 2: Timer, Navigation, Ringer, Charging, Face ID (00:22 - 01:25)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateChip("00:22 Timer Pill", Color(0xFFD97706), onTriggerTimerCompact)
                StateChip("00:28 Timer Card", Color(0xFFB45309), onTriggerTimerExpanded)
                StateChip("00:39 Nav 3.6 km", Color(0xFF059669), onTriggerNavCompact)
                StateChip("00:44 Nav Expanded", Color(0xFF047857), onTriggerNavExpanded)
                StateChip("00:50 Ring", Color(0xFF475569)) { onTriggerRinger(RingerModeState.NORMAL) }
                StateChip("00:51 Vibrate", Color(0xFFEA580C)) { onTriggerRinger(RingerModeState.VIBRATE) }
                StateChip("00:54 Silent", Color(0xFFDC2626)) { onTriggerRinger(RingerModeState.SILENT) }
                StateChip("01:03 Charging 80%", Color(0xFF16A34A), onTriggerCharging)
                StateChip("01:21 Face ID Auth", Color(0xFF0D9488), onTriggerBiometric)
            }
        }
    }
}

@Composable
private fun StateChip(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.88f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LiquidBottomNavigationDock(
    selectedTab: Int,
    currentMode: IslandAppMode,
    onSelectTab: (Int) -> Unit,
    onToggleMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xCC111827))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockTabItem(
            title = "Home & Widgets",
            selected = selectedTab == 0,
            onClick = { onSelectTab(0) }
        )
        DockTabItem(
            title = "Control Center",
            selected = selectedTab == 1,
            onClick = { onSelectTab(1) }
        )
        DockTabItem(
            title = "Settings",
            selected = selectedTab == 2,
            onClick = { onSelectTab(2) }
        )

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (currentMode == IslandAppMode.LIQUID_ISLAND) Color(0xFF2563EB)
                    else Color(0xFF475569)
                )
                .clickable { onToggleMode() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = if (currentMode == IslandAppMode.LIQUID_ISLAND) "LiquidIsland" else "DroidBubble",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DockTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
