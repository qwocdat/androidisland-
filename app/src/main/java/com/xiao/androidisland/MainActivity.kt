package com.xiao.androidisland

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiao.androidisland.core.model.IslandMode
import com.xiao.androidisland.droidbubble.DroidBubbleOverlayView
import com.xiao.androidisland.droidbubble.DroidBubbleState
import com.xiao.androidisland.liquidisland.controlcenter.IosControlCenterSheet
import com.xiao.androidisland.liquidisland.engine.CoupledIslandKinematicsState
import com.xiao.androidisland.liquidisland.engine.DynamicIslandComposable
import com.xiao.androidisland.liquidisland.renderer.LiquidBackdrop
import com.xiao.androidisland.liquidisland.renderer.LiquidEffectController
import com.xiao.androidisland.liquidisland.renderer.rememberLayerBackdrop
import com.xiao.androidisland.liquidisland.renderer.rememberLiquidEffectController
import com.xiao.androidisland.liquidisland.renderer.LiquidGlassNativeBridge
import com.xiao.androidisland.liquidisland.settings.LiquidIslandSettingsSheet
import com.xiao.androidisland.liquidisland.statusbar.DynamicIosStatusBar
import com.xiao.androidisland.liquidisland.widgets.DualMonthCalendarWidget
import com.xiao.androidisland.liquidisland.widgets.HomeAppIconsAndAnalogClockRow
import com.xiao.androidisland.service.AndroidIslandOverlayService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val systemRepo = AndroidIslandApp.system(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val settings by systemRepo.settings.collectAsState()
                val status by systemRepo.statusSnapshot.collectAsState()
                val islandState by systemRepo.islandState.collectAsState()
                val track by systemRepo.currentTrack.collectAsState()
                val isControlCenterOpen by systemRepo.isControlCenterVisible.collectAsState()

                var selectedBottomTab by remember { mutableIntStateOf(0) }
                var overlayRunning by remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }

                val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat().coerceAtLeast(360f)
                val kinematics = remember { CoupledIslandKinematicsState() }
                val backdrop = rememberLayerBackdrop()
                val effectController = rememberLiquidEffectController()

                val currentW = kinematics.widthAnim.value
                val islandCenterX = screenWidthDp * 0.5f + settings.islandHorizontalOffsetDp
                val islandLeftEdge = islandCenterX - currentW * 0.5f
                val islandRightEdge = islandCenterX + currentW * 0.5f
                val velocity = kinematics.widthAnim.velocity

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0F1D)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        WallpaperBackdrop()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            Spacer(modifier = Modifier.height(54.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (selectedBottomTab) {
                                    0 -> HomeReferenceWorkspace(
                                        backdrop = backdrop,
                                        effectController = effectController,
                                        nativeShaderActive = LiquidGlassNativeBridge.isNativeLibraryLoaded,
                                        onTriggerState = { key -> systemRepo.triggerStateByKey(key) },
                                        onStartOverlayService = {
                                            if (Settings.canDrawOverlays(this@MainActivity)) {
                                                AndroidIslandOverlayService.startOrUpdate(this@MainActivity)
                                                overlayRunning = true
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

                                    1 -> IosControlCenterSheet(
                                        status = status,
                                        track = track,
                                        backdrop = backdrop,
                                        effectController = effectController,
                                        onToggleWifi = { systemRepo.toggleWifi(this@MainActivity) },
                                        onToggleAirplane = { systemRepo.toggleAirplaneMode(this@MainActivity) },
                                        onToggleMobileData = { systemRepo.toggleMobileData(this@MainActivity) },
                                        onToggleHotspot = { systemRepo.toggleHotspot(this@MainActivity) },
                                        onCycleRingerMode = { systemRepo.cycleRingerMode() },
                                        onBrightnessChange = { systemRepo.setBrightnessPercent(it) },
                                        onVolumeChange = { systemRepo.setVolumePercent(it) },
                                        onOpenSettings = { selectedBottomTab = 2 },
                                        onCloseControlCenter = { selectedBottomTab = 0 }
                                    )

                                    else -> Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        LiquidIslandSettingsSheet(
                                            settings = settings,
                                            isSystemOverlayRunning = overlayRunning,
                                            onUpdateSettings = { newSettings ->
                                                systemRepo.updateSettings(newSettings)
                                            },
                                            onSelectDemoState = { key ->
                                                systemRepo.triggerStateByKey(key)
                                            },
                                            onToggleOverlayService = { enable ->
                                                if (enable) {
                                                    if (Settings.canDrawOverlays(this@MainActivity)) {
                                                        AndroidIslandOverlayService.startOrUpdate(this@MainActivity)
                                                        overlayRunning = true
                                                    } else {
                                                        startActivity(
                                                            Intent(
                                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                                Uri.parse("package:$packageName")
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    stopService(Intent(this@MainActivity, AndroidIslandOverlayService::class.java))
                                                    overlayRunning = false
                                                }
                                            },
                                            onOpenControlCenter = { selectedBottomTab = 1 }
                                        )
                                    }
                                }
                            }

                            LiquidBottomNavigationDock(
                                selectedTab = selectedBottomTab,
                                currentMode = settings.activeMode,
                                onSelectTab = { selectedBottomTab = it },
                                onToggleMode = {
                                    systemRepo.updateSettings { s ->
                                        s.copy(
                                            activeMode = if (s.activeMode == IslandMode.LIQUID_ISLAND) {
                                                IslandMode.DROID_BUBBLE
                                            } else {
                                                IslandMode.LIQUID_ISLAND
                                            }
                                        )
                                    }
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .align(Alignment.TopCenter),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (settings.activeMode == IslandMode.DROID_BUBBLE) {
                                DroidBubbleOverlayView(
                                    state = DroidBubbleState(),
                                    onToggleExpand = { systemRepo.onIslandTapped() },
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            } else {
                                if (settings.isIosStatusBarEnabled) {
                                    DynamicIosStatusBar(
                                        status = status,
                                        settings = settings,
                                        screenWidthDp = screenWidthDp,
                                        islandCenterXDp = islandCenterX,
                                        islandLeftEdgeDp = islandLeftEdge,
                                        islandRightEdgeDp = islandRightEdge,
                                        leftEdgeVelocityDpPerSec = -velocity * 0.5f,
                                        rightEdgeVelocityDpPerSec = velocity * 0.5f,
                                        isOnDarkBackground = true
                                    )
                                }

                                DynamicIslandComposable(
                                    activityState = islandState,
                                    settings = settings,
                                    kinematics = kinematics,
                                    backdrop = backdrop,
                                    effectController = effectController,
                                    onTapIsland = { systemRepo.onIslandTapped() },
                                    onLongPressIsland = { systemRepo.onIslandSecondaryTapped() },
                                    onMediaPrev = { systemRepo.onMediaPrevious() },
                                    onMediaPlayPause = { systemRepo.onMediaPlayPause() },
                                    onMediaNext = { systemRepo.onMediaNext() },
                                    onTimerLap = { systemRepo.onTimerLap() },
                                    onTimerCancel = { systemRepo.onTimerCancel() },
                                    onEndNavigationTrip = { systemRepo.endNavigation() },
                                    onDismissNotification = { systemRepo.dismissNotification() },
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isControlCenterOpen,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IosControlCenterSheet(
                                status = status,
                                track = track,
                                backdrop = backdrop,
                                effectController = effectController,
                                onToggleWifi = { systemRepo.toggleWifi(this@MainActivity) },
                                onToggleAirplane = { systemRepo.toggleAirplaneMode(this@MainActivity) },
                                onToggleMobileData = { systemRepo.toggleMobileData(this@MainActivity) },
                                onToggleHotspot = { systemRepo.toggleHotspot(this@MainActivity) },
                                onCycleRingerMode = { systemRepo.cycleRingerMode() },
                                onBrightnessChange = { systemRepo.setBrightnessPercent(it) },
                                onVolumeChange = { systemRepo.setVolumePercent(it) },
                                onOpenSettings = {
                                    systemRepo.setControlCenterVisible(false)
                                    selectedBottomTab = 2
                                },
                                onCloseControlCenter = {
                                    systemRepo.setControlCenterVisible(false)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xD80A0F1D))
                                    .statusBarsPadding()
                            )
                        }
                    }
                }
            }
        }
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
    }
}

@Composable
private fun HomeReferenceWorkspace(
    backdrop: LiquidBackdrop,
    effectController: LiquidEffectController,
    nativeShaderActive: Boolean,
    onTriggerState: (String) -> Unit,
    onStartOverlayService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DualMonthCalendarWidget(
            backdrop = backdrop,
            effectController = effectController
        )

        HomeAppIconsAndAnalogClockRow(
            backdrop = backdrop,
            effectController = effectController,
            onTriggerMusicDemo = { onTriggerState("MusicExpanded") },
            onTriggerNotificationDemo = { onTriggerState("NotifExpanded") }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        text = "Shader=${if (nativeShaderActive) "GLES 3.0 NDK" else "AGSL RuntimeShader"}",
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

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateChip("00:00 Idle", Color(0xFF334155)) { onTriggerState("Idle") }
                StateChip("00:01 Telegram", Color(0xFF0284C7)) { onTriggerState("NotifCompact") }
                StateChip("00:04 Telegram Card", Color(0xFF0369A1)) { onTriggerState("NotifExpanded") }
                StateChip("00:07 Music Pill", Color(0xFF7C3AED)) { onTriggerState("MusicCompact") }
                StateChip("00:09 Split Metaball", Color(0xFF9333EA)) { onTriggerState("SplitDual") }
                StateChip("00:10 Metrolist", Color(0xFF4F46E5)) { onTriggerState("NotifBanner") }
                StateChip("00:13 Music Card", Color(0xFF6D28D9)) { onTriggerState("MusicExpanded") }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateChip("00:22 Timer", Color(0xFFD97706)) { onTriggerState("TimerCompact") }
                StateChip("00:28 Timer Card", Color(0xFFB45309)) { onTriggerState("TimerExpanded") }
                StateChip("00:39 Nav 3.6km", Color(0xFF059669)) { onTriggerState("NavCompact") }
                StateChip("00:44 Nav Card", Color(0xFF047857)) { onTriggerState("NavExpanded") }
                StateChip("00:50 Ring", Color(0xFF475569)) { onTriggerState("Ring") }
                StateChip("00:51 Vibrate", Color(0xFFEA580C)) { onTriggerState("Vibrate") }
                StateChip("00:54 Silent", Color(0xFFDC2626)) { onTriggerState("Silent") }
                StateChip("01:03 Charging 80%", Color(0xFF16A34A)) { onTriggerState("Charging") }
                StateChip("01:21 Face ID", Color(0xFF0D9488)) { onTriggerState("Auth") }
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
    currentMode: IslandMode,
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
        DockTabItem("Home", selectedTab == 0) { onSelectTab(0) }
        DockTabItem("Control Center", selectedTab == 1) { onSelectTab(1) }
        DockTabItem("Settings", selectedTab == 2) { onSelectTab(2) }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (currentMode == IslandMode.LIQUID_ISLAND) Color(0xFF2563EB)
                    else Color(0xFF475569)
                )
                .clickable { onToggleMode() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = if (currentMode == IslandMode.LIQUID_ISLAND) "LiquidIsland" else "DroidBubble",
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
