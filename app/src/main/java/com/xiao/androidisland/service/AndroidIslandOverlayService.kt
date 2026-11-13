package com.xiao.androidisland.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.droidbubble.app.R
import com.xiao.androidisland.AndroidIslandApp
import com.xiao.androidisland.MainActivity
import com.xiao.androidisland.core.model.IslandMode
import com.xiao.androidisland.droidbubble.DroidBubbleOverlayView
import com.xiao.androidisland.droidbubble.DroidBubbleState
import com.xiao.androidisland.liquidisland.controlcenter.IosControlCenterSheet
import com.xiao.androidisland.liquidisland.engine.CoupledIslandKinematicsState
import com.xiao.androidisland.liquidisland.engine.DynamicIslandComposable
import com.xiao.androidisland.liquidisland.renderer.rememberLayerBackdrop
import com.xiao.androidisland.liquidisland.renderer.rememberLiquidEffectController
import com.xiao.androidisland.liquidisland.statusbar.DynamicIosStatusBar

class AndroidIslandOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var topBarComposeView: ComposeView? = null
    private var controlCenterComposeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        startForegroundWithChannel()
        if (Settings.canDrawOverlays(this)) {
            attachTopOverlayWindow()
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_OPEN_CONTROL_CENTER -> {
                AndroidIslandApp.system(this).setControlCenterVisible(true)
                ensureControlCenterWindow()
            }
            ACTION_CLOSE_CONTROL_CENTER -> {
                AndroidIslandApp.system(this).setControlCenterVisible(false)
            }
            ACTION_STOP_OVERLAY -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (topBarComposeView == null && Settings.canDrawOverlays(this)) {
                    attachTopOverlayWindow()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithChannel() {
        val channelId = "android_island_overlay_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                description = getString(R.string.overlay_notification_text)
            }
            nm.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_island_tile)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun attachTopOverlayWindow() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            title = "AndroidIslandLiquidOverlay"
        }

        val systemRepo = AndroidIslandApp.system(this)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AndroidIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@AndroidIslandOverlayService)
            setContent {
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
                        .wrapContentHeight()
                        .pointerInput(Unit) {
                            var dySum = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dySum = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    dySum += dragAmount
                                    if (dySum > 70f) {
                                        systemRepo.setControlCenterVisible(true)
                                        ensureControlCenterWindow()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (settings.activeMode == IslandMode.DROID_BUBBLE) {
                        DroidBubbleOverlayView(
                            state = DroidBubbleState(),
                            onToggleExpand = { systemRepo.onIslandTapped() },
                            modifier = Modifier.padding(top = 8.dp)
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
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        runCatching {
            wm.addView(view, lp)
            topBarComposeView = view
        }
    }

    private fun ensureControlCenterWindow() {
        if (controlCenterComposeView != null || !Settings.canDrawOverlays(this)) return
        val wm = windowManager ?: (getSystemService(WINDOW_SERVICE) as WindowManager).also { windowManager = it }
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            title = "AndroidIslandControlCenter"
        }

        val systemRepo = AndroidIslandApp.system(this)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AndroidIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@AndroidIslandOverlayService)
            setContent {
                val visible by systemRepo.isControlCenterVisible.collectAsState()
                val status by systemRepo.statusSnapshot.collectAsState()
                val track by systemRepo.currentTrack.collectAsState()
                val backdrop = rememberLayerBackdrop()
                val effectController = rememberLiquidEffectController()

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        IosControlCenterSheet(
                            status = status,
                            track = track,
                            backdrop = backdrop,
                            effectController = effectController,
                            onToggleWifi = { systemRepo.toggleWifi(this@AndroidIslandOverlayService) },
                            onToggleAirplane = { systemRepo.toggleAirplaneMode(this@AndroidIslandOverlayService) },
                            onToggleMobileData = { systemRepo.toggleMobileData(this@AndroidIslandOverlayService) },
                            onToggleHotspot = { systemRepo.toggleHotspot(this@AndroidIslandOverlayService) },
                            onCycleRingerMode = { systemRepo.cycleRingerMode() },
                            onBrightnessChange = { systemRepo.setBrightnessPercent(it) },
                            onVolumeChange = { systemRepo.setVolumePercent(it) },
                            onOpenSettings = {
                                systemRepo.setControlCenterVisible(false)
                                removeControlCenterWindow()
                                startActivity(
                                    Intent(this@AndroidIslandOverlayService, MainActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            onCloseControlCenter = {
                                systemRepo.setControlCenterVisible(false)
                                removeControlCenterWindow()
                            }
                        )
                    }
                }
            }
        }

        runCatching {
            wm.addView(view, lp)
            controlCenterComposeView = view
        }
    }

    private fun removeControlCenterWindow() {
        val view = controlCenterComposeView ?: return
        runCatching { windowManager?.removeView(view) }
        controlCenterComposeView = null
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        topBarComposeView?.let { runCatching { windowManager?.removeView(it) } }
        topBarComposeView = null
        removeControlCenterWindow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 96942
        const val ACTION_OPEN_CONTROL_CENTER = "com.xiao.androidisland.action.OPEN_CONTROL_CENTER"
        const val ACTION_CLOSE_CONTROL_CENTER = "com.xiao.androidisland.action.CLOSE_CONTROL_CENTER"
        const val ACTION_STOP_OVERLAY = "com.xiao.androidisland.action.STOP_OVERLAY"

        fun startOrUpdate(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, AndroidIslandOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
