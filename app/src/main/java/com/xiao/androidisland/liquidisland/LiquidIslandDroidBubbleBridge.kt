package com.xiao.androidisland.liquidisland

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Rect as AndroidRect
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.StatusBarNotification
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xiao.androidisland.core.model.IslandActivityState
import com.xiao.androidisland.core.model.IslandNotificationItem
import com.xiao.androidisland.core.model.NavigationActivityState
import com.xiao.androidisland.core.model.RingerModeType
import com.xiao.androidisland.core.model.TimerActivityState
import com.xiao.androidisland.core.permission.ShizukuBridge
import com.xiao.androidisland.core.system.SystemStateManager
import com.xiao.androidisland.liquidisland.controlcenter.IosControlCenterSheet
import com.xiao.androidisland.liquidisland.engine.CoupledIslandKinematicsState
import com.xiao.androidisland.liquidisland.engine.DynamicIslandComposable
import com.xiao.androidisland.liquidisland.renderer.LiquidGlassNativeBridge
import com.xiao.androidisland.liquidisland.renderer.rememberLayerBackdrop
import com.xiao.androidisland.liquidisland.renderer.rememberLiquidEffectController
import com.xiao.androidisland.liquidisland.settings.LiquidIslandSettingsSheet
import com.xiao.androidisland.liquidisland.statusbar.DynamicIosStatusBar
import com.xiao.androidisland.liquidisland.widgets.DualMonthCalendarWidget
import com.xiao.androidisland.liquidisland.widgets.HomeAppIconsAndAnalogClockRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Seamless bridge connecting the existing `com.droidbubble.app` ("Androidisland - LIQUID EDITION • by digitalxiao")
 * with the `LiquidIsland` (`96942.mp4` - Dynamic Motion 1.2 For Android) engine whenever the user selects
 * **Liquidisland ("Thấu kính quang học" / `DesignLanguage.GLASS`)** in "Chế độ giao diện Đảo".
 *
 * Preserves 100% of `Droidbubble` ("Đảo đen nguyên bản") and the 5-tab main UI untouched.
 */
object LiquidIslandDroidBubbleBridge {

    @Volatile
    private var systemStateManagerInstance: SystemStateManager? = null

    private val _isGlassModeFlow = MutableStateFlow(false)
    val isGlassModeFlow: StateFlow<Boolean> = _isGlassModeFlow.asStateFlow()

    fun getSystemManager(context: Context): SystemStateManager {
        val existing = systemStateManagerInstance
        if (existing != null) return existing
        return synchronized(this) {
            systemStateManagerInstance ?: SystemStateManager(
                context = context.applicationContext,
                shizukuBridge = ShizukuBridge()
            ).also {
                it.startObserving()
                systemStateManagerInstance = it
            }
        }
    }

    fun updateDesignLanguage(designLanguageAny: Any?) {
        val mode = extractDesignLanguageName(designLanguageAny) ?: return
        _isGlassModeFlow.value = mode.equals("GLASS", ignoreCase = true) ||
            mode.contains("LIQUID", ignoreCase = true)
    }

    fun shouldRenderLiquidIsland(context: Context, vararg args: Any?): Boolean {
        getSystemManager(context)
        for (arg in args) {
            val mode = extractDesignLanguageName(arg)
            if (mode != null) {
                val isGlass = mode.equals("GLASS", ignoreCase = true) ||
                    mode.contains("LIQUID", ignoreCase = true)
                _isGlassModeFlow.value = isGlass
                return isGlass
            }
        }
        return _isGlassModeFlow.value
    }

    private fun extractDesignLanguageName(obj: Any?, depth: Int = 0): String? {
        if (obj == null || depth > 2) return null
        if (obj is Enum<*>) {
            val clsName = obj.javaClass.simpleName
            if (clsName.contains("DesignLanguage", ignoreCase = true) ||
                obj.name == "GLASS" || obj.name == "DEFAULT" || obj.name == "DROIDBUBBLE"
            ) {
                return obj.name
            }
        }
        val cls = obj.javaClass
        if (cls.name.startsWith("java.") || cls.name.startsWith("kotlin.") || cls.name.startsWith("android.")) {
            return null
        }
        runCatching {
            val method = cls.methods.firstOrNull {
                it.parameterCount == 0 &&
                    (it.name == "getDesignLanguage" || it.returnType.simpleName.contains("DesignLanguage", ignoreCase = true))
            }
            if (method != null) {
                val res = method.invoke(obj)
                if (res is Enum<*>) return res.name
            }
        }
        if (depth == 0) {
            runCatching {
                for (m in cls.methods) {
                    if (m.parameterCount == 0 && m.name.startsWith("get") &&
                        (m.name.contains("Settings", ignoreCase = true) || m.name.contains("State", ignoreCase = true))
                    ) {
                        val sub = m.invoke(obj)
                        val nested = extractDesignLanguageName(sub, depth + 1)
                        if (nested != null) return nested
                    }
                }
            }
        }
        return null
    }

    /**
     * Synchronizes any active event/state from `com.droidbubble.app` (`IslandState`, `IslandEvent`, etc.)
     * or real Android `MediaSessionManager` into [SystemStateManager].
     */
    private fun syncLegacyStateIfPresent(systemRepo: SystemStateManager, vararg args: Any?) {
        for (arg in args) {
            if (arg == null) continue
            val clsName = arg.javaClass.simpleName
            val str = arg.toString()
            if (clsName.contains("Event", ignoreCase = true) || clsName.contains("State", ignoreCase = true)) {
                when {
                    str.contains("Charging", ignoreCase = true) -> {
                        val level = extractIntProperty(arg, "level", "batteryLevel", "percent")
                            ?: systemRepo.statusSnapshot.value.batteryPercent
                        systemRepo.updateChargingStateFromBridge(level)
                    }
                    str.contains("Media", ignoreCase = true) || str.contains("Music", ignoreCase = true) -> {
                        val title = extractStringProperty(arg, "title", "trackTitle", "songTitle")
                        val artist = extractStringProperty(arg, "artist", "subTitle", "subtitle")
                        if (!title.isNullOrBlank()) {
                            systemRepo.updateRealMediaMetadata(
                                title = title,
                                artist = artist ?: "Unknown Artist",
                                isPlaying = true
                            )
                        }
                    }
                    str.contains("Timer", ignoreCase = true) || str.contains("Stopwatch", ignoreCase = true) -> {
                        if (systemRepo.islandState.value !is IslandActivityState.CompactTimer &&
                            systemRepo.islandState.value !is IslandActivityState.ExpandedTimer
                        ) {
                            systemRepo.triggerStateByKey("TimerCompact")
                        }
                    }
                }
            }
        }
    }

    private fun extractStringProperty(obj: Any, vararg names: String): String? {
        for (n in names) {
            val getter = "get" + n.replaceFirstChar { it.uppercase() }
            val value = runCatching {
                obj.javaClass.methods.firstOrNull { it.parameterCount == 0 && it.name.equals(getter, ignoreCase = true) }
                    ?.invoke(obj) as? String
            }.getOrNull()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun extractIntProperty(obj: Any, vararg names: String): Int? {
        for (n in names) {
            val getter = "get" + n.replaceFirstChar { it.uppercase() }
            val value = runCatching {
                (obj.javaClass.methods.firstOrNull { it.parameterCount == 0 && it.name.equals(getter, ignoreCase = true) }
                    ?.invoke(obj) as? Number)?.toInt()
            }.getOrNull()
            if (value != null && value in 1..100) return value
        }
        return null
    }

    private fun notifyBoundsToLegacyCallbacks(bounds: ComposeRect, vararg args: Any?) {
        val androidRect = AndroidRect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt()
        )
        val intRect = IntRect(
            androidRect.left,
            androidRect.top,
            androidRect.right,
            androidRect.bottom
        )
        for (arg in args) {
            if (arg == null) continue
            if (arg is Function1<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val fn = arg as Function1<Any?, Any?>
                runCatching { fn.invoke(bounds) }
                    .recoverCatching { fn.invoke(androidRect) }
                    .recoverCatching { fn.invoke(intRect) }
            } else if (arg.javaClass.simpleName.contains("TouchManager", ignoreCase = true)) {
                runCatching {
                    arg.javaClass.methods.firstOrNull {
                        it.parameterCount == 1 &&
                            (it.parameterTypes[0] == AndroidRect::class.java ||
                                it.name.contains("Region", ignoreCase = true) ||
                                it.name.contains("Bounds", ignoreCase = true))
                    }?.invoke(arg, androidRect)
                }
            }
        }
    }

    fun onNotificationPosted(context: Context, sbn: StatusBarNotification?) {
        if (sbn == null) return
        val repo = getSystemManager(context)
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        val appLabel = runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        }.getOrDefault(sbn.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })

        val isUpdateStyle = title.contains("update", ignoreCase = true) ||
            text.contains("update available", ignoreCase = true)

        repo.onIncomingRealNotification(
            IslandNotificationItem(
                key = sbn.key,
                appName = appLabel,
                timeLabel = "Now",
                title = title.ifEmpty { appLabel },
                body = text,
                isCompactBanner = isUpdateStyle
            )
        )
    }

    /**
     * Rendered inside `IslandView.kt` (hosted by `IslandOverlayService`) whenever
     * **Liquidisland ("Thấu kính quang học" / `DesignLanguage.GLASS`)** is active.
     */
    @Composable
    fun RenderLiquidIslandOverlay(vararg args: Any?) {
        val context = LocalContext.current
        val systemRepo = remember(context) { getSystemManager(context) }

        LaunchedEffect(args.contentHashCode()) {
            syncLegacyStateIfPresent(systemRepo, *args)
        }

        // Poll active MediaSession using com.droidbubble.app's granted NotificationListenerService
        LaunchedEffect(context) {
            while (true) {
                pollActiveMediaSessions(context, systemRepo)
                delay(1500L)
            }
        }

        val settings by systemRepo.settings.collectAsState()
        val status by systemRepo.statusSnapshot.collectAsState()
        val islandState by systemRepo.islandState.collectAsState()
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat().coerceAtLeast(360f)

        val kinematics = remember { CoupledIslandKinematicsState() }
        val backdrop = rememberLayerBackdrop()
        val effectController = rememberLiquidEffectController()

        val currentW = kinematics.widthAnim.value
        val islandCenterX = screenWidthDp * 0.5f + settings.islandHorizontalOffsetDp
        val islandLeftEdge = islandCenterX - currentW * 0.5f
        val islandRightEdge = islandCenterX + currentW * 0.5f
        val velocity = kinematics.widthAnim.velocity

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
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
                modifier = Modifier
                    .padding(top = 4.dp)
                    .onGloballyPositioned { coords ->
                        notifyBoundsToLegacyCallbacks(coords.boundsInWindow(), *args)
                    }
            )
        }
    }

    /**
     * Rendered in the top Preview Card of `LiquidHomeTab.kt` when **Liquidisland ("Thấu kính quang học")**
     * is selected in "Chế độ giao diện Đảo".
     */
    @Composable
    fun RenderLiquidIslandHomePreview(vararg args: Any?) {
        val context = LocalContext.current
        val systemRepo = remember(context) { getSystemManager(context) }

        val settings by systemRepo.settings.collectAsState()
        val status by systemRepo.statusSnapshot.collectAsState()
        val islandState by systemRepo.islandState.collectAsState()
        val track by systemRepo.currentTrack.collectAsState()

        var showControlCenterDialog by remember { mutableStateOf(false) }
        var showLiquidSettingsDialog by remember { mutableStateOf(false) }

        val kinematics = remember { CoupledIslandKinematicsState() }
        val backdrop = rememberLayerBackdrop()
        val effectController = rememberLiquidEffectController()

        val screenWidthDp = 340f
        val currentW = kinematics.widthAnim.value
        val islandCenterX = screenWidthDp * 0.5f
        val islandLeftEdge = islandCenterX - currentW * 0.5f
        val islandRightEdge = islandCenterX + currentW * 0.5f
        val velocity = kinematics.widthAnim.velocity

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF172542),
                            Color(0xFF0E172A),
                            Color(0xFF090E1A)
                        )
                    )
                )
                .border(1.dp, Color(0x3860A5FA), RoundedCornerShape(28.dp))
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live iOS Status Bar + Dynamic Island with Edge-Based Liquid Absorption & Motion Blur
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
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
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Interactive Timeline State Chips from 96942.mp4 (Dynamic Motion 1.2 For Android)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PreviewChip("Idle", Color(0xFF334155)) { systemRepo.triggerStateByKey("Idle") }
                PreviewChip("Telegram", Color(0xFF0284C7)) { systemRepo.triggerStateByKey("NotifCompact") }
                PreviewChip("Telegram Mở rộng", Color(0xFF0369A1)) { systemRepo.triggerStateByKey("NotifExpanded") }
                PreviewChip("Nhạc", Color(0xFF7C3AED)) { systemRepo.triggerStateByKey("MusicCompact") }
                PreviewChip("Tách 2 Đảo", Color(0xFF9333EA)) { systemRepo.triggerStateByKey("SplitDual") }
                PreviewChip("Metrolist 13.7.0", Color(0xFF4F46E5)) { systemRepo.triggerStateByKey("NotifBanner") }
                PreviewChip("Trình phát nhạc", Color(0xFF6D28D9)) { systemRepo.triggerStateByKey("MusicExpanded") }
                PreviewChip("Bấm giờ", Color(0xFFD97706)) { systemRepo.triggerStateByKey("TimerCompact") }
                PreviewChip("Thẻ Bấm giờ", Color(0xFFB45309)) { systemRepo.triggerStateByKey("TimerExpanded") }
                PreviewChip("Điều hướng", Color(0xFF059669)) { systemRepo.triggerStateByKey("NavCompact") }
                PreviewChip("Bản đồ Mở rộng", Color(0xFF047857)) { systemRepo.triggerStateByKey("NavExpanded") }
                PreviewChip("Chuông", Color(0xFF475569)) { systemRepo.triggerStateByKey("Ring") }
                PreviewChip("Rung", Color(0xFFEA580C)) { systemRepo.triggerStateByKey("Vibrate") }
                PreviewChip("Im lặng", Color(0xFFDC2626)) { systemRepo.triggerStateByKey("Silent") }
                PreviewChip("Sạc 80%", Color(0xFF16A34A)) { systemRepo.triggerStateByKey("Charging") }
                PreviewChip("Face ID", Color(0xFF0D9488)) { systemRepo.triggerStateByKey("Auth") }
            }

            // Action row to open iOS Control Center & LiquidIsland Auth/Media Style Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Liquidisland • Thấu kính quang học & Hấp thụ Status Bar",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewChip("Control Center", Color(0xFF2563EB)) {
                        showControlCenterDialog = true
                    }
                    PreviewChip("Tùy chỉnh Liquid", Color(0xFF0F766E)) {
                        showLiquidSettingsDialog = true
                    }
                }
            }
        }

        if (showControlCenterDialog) {
            Dialog(
                onDismissRequest = { showControlCenterDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xD90A0F1D)
                ) {
                    IosControlCenterSheet(
                        status = status,
                        track = track,
                        backdrop = backdrop,
                        effectController = effectController,
                        onToggleWifi = { systemRepo.toggleWifi(context) },
                        onToggleAirplane = { systemRepo.toggleAirplaneMode(context) },
                        onToggleMobileData = { systemRepo.toggleMobileData(context) },
                        onToggleHotspot = { systemRepo.toggleHotspot(context) },
                        onCycleRingerMode = { systemRepo.cycleRingerMode() },
                        onBrightnessChange = { systemRepo.setBrightnessPercent(it) },
                        onVolumeChange = { systemRepo.setVolumePercent(it) },
                        onOpenSettings = {
                            showControlCenterDialog = false
                            showLiquidSettingsDialog = true
                        },
                        onCloseControlCenter = { showControlCenterDialog = false },
                        modifier = Modifier.padding(top = 28.dp)
                    )
                }
            }
        }

        if (showLiquidSettingsDialog) {
            Dialog(
                onDismissRequest = { showLiquidSettingsDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xEE090D18)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIQUIDISLAND STUDIO & WIDGETS",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            PreviewChip("Đóng", Color(0xFFDC2626)) {
                                showLiquidSettingsDialog = false
                            }
                        }

                        DualMonthCalendarWidget(
                            backdrop = backdrop,
                            effectController = effectController
                        )

                        HomeAppIconsAndAnalogClockRow(
                            backdrop = backdrop,
                            effectController = effectController,
                            onTriggerMusicDemo = { systemRepo.triggerStateByKey("MusicExpanded") },
                            onTriggerNotificationDemo = { systemRepo.triggerStateByKey("NotifExpanded") }
                        )

                        LiquidIslandSettingsSheet(
                            settings = settings,
                            isSystemOverlayRunning = true,
                            onUpdateSettings = { systemRepo.updateSettings(it) },
                            onSelectDemoState = { systemRepo.triggerStateByKey(it) },
                            onToggleOverlayService = {},
                            onOpenControlCenter = {
                                showLiquidSettingsDialog = false
                                showControlCenterDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PreviewChip(
        label: String,
        color: Color,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.90f))
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    private fun pollActiveMediaSessions(context: Context, systemRepo: SystemStateManager) {
        runCatching {
            val manager = context.getSystemService(MediaSessionManager::class.java) ?: return
            val services = context.packageManager.queryIntentServices(
                android.content.Intent("android.service.notification.NotificationListenerService")
                    .setPackage(context.packageName),
                0
            )
            val serviceInfo = services.firstOrNull()?.serviceInfo ?: return
            val component = ComponentName(serviceInfo.packageName, serviceInfo.name)
            val controllers: List<MediaController> = manager.getActiveSessions(component)
            val chosen = controllers.firstOrNull {
                val st = it.playbackState?.state
                st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
            } ?: controllers.firstOrNull() ?: return
            val meta = chosen.metadata ?: return
            val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: return
            val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: "Unknown Artist"
            val isPlaying = chosen.playbackState?.state == PlaybackState.STATE_PLAYING
            systemRepo.updateRealMediaMetadata(title = title, artist = artist, isPlaying = isPlaying)
        }
    }
}
