package com.xiao.androidisland.service

import android.app.Notification
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.xiao.androidisland.AndroidIslandApp
import com.xiao.androidisland.core.model.IslandMediaData
import com.xiao.androidisland.core.model.IslandNavigationData
import com.xiao.androidisland.core.model.IslandNotificationAction
import com.xiao.androidisland.core.model.IslandNotificationData
import com.xiao.androidisland.core.model.IslandTimerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Listens to active Android notifications and MediaSession updates to feed real-time
 * notifications, music playback, timer/stopwatch, and turn-by-turn navigation into
 * both DroidBubble and LiquidIsland modes.
 */
class IslandNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var mediaProgressJob: Job? = null

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindMediaControllers(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishActiveControllerState()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishActiveControllerState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupMediaSessionTracking()
        startProgressTicker()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        setupMediaSessionTracking()
        syncExistingNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        processStatusBarNotification(sbn, isPosted = true)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        syncExistingNotifications()
    }

    private fun setupMediaSessionTracking() {
        runCatching {
            val manager = getSystemService(MediaSessionManager::class.java) ?: return
            mediaSessionManager = manager
            val component = ComponentName(this, IslandNotificationListenerService::class.java)
            val controllers = manager.getActiveSessions(component)
            bindMediaControllers(controllers)
            manager.addOnActiveSessionsChangedListener(sessionListener, component)
        }
    }

    private fun bindMediaControllers(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        val chosen = controllers?.firstOrNull {
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: controllers?.firstOrNull()
        activeController = chosen
        chosen?.registerCallback(controllerCallback)
        publishActiveControllerState()
    }

    private fun startProgressTicker() {
        mediaProgressJob?.cancel()
        mediaProgressJob = scope.launch {
            while (isActive) {
                if (activeController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    publishActiveControllerState()
                }
                delay(1000L)
            }
        }
    }

    private fun publishActiveControllerState() {
        val repo = AndroidIslandApp.systemOrNull(this) ?: return
        val controller = activeController
        if (controller == null) return
        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unknown Artist"
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(1L)

        val basePos = playbackState?.position ?: 0L
        val lastUpdate = playbackState?.lastPositionUpdateTime ?: 0L
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val speed = playbackState?.playbackSpeed ?: 1f
        val elapsed = if (isPlaying && lastUpdate > 0L) {
            ((SystemClock.elapsedRealtime() - lastUpdate) * speed).toLong().coerceAtLeast(0L)
        } else {
            0L
        }
        val positionMs = (basePos + elapsed).coerceIn(0L, durationMs)
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        repo.updateMediaSession(
            IslandMediaData(
                packageName = controller.packageName,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                positionMs = positionMs,
                isPlaying = isPlaying,
                albumArt = art,
                dominantColorArgb = 0xFF7B3FE4.toInt(),
                secondaryColorArgb = 0xFF28194B.toInt(),
                outputDeviceName = "Speaker"
            )
        )
    }

    private fun syncExistingNotifications() {
        val repo = AndroidIslandApp.systemOrNull(this) ?: return
        val list = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        repo.updateNotificationCount(list.size)
        list.firstOrNull { !it.isOngoing }?.let { processStatusBarNotification(it, isPosted = false) }
    }

    private fun processStatusBarNotification(sbn: StatusBarNotification, isPosted: Boolean) {
        val repo = AndroidIslandApp.systemOrNull(this) ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        val pkg = sbn.packageName.lowercase()
        val appLabel = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        }.getOrDefault(sbn.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })

        // Check if timer / chronometer notification
        val usesChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        if (usesChronometer || pkg.contains("clock") || pkg.contains("timer")) {
            val baseWhen = notification.`when`
            val elapsedMs = if (baseWhen > 0L) (System.currentTimeMillis() - baseWhen).coerceAtLeast(1000L) else 14_000L
            repo.updateTimer(
                IslandTimerData(
                    id = sbn.key,
                    label = title.ifEmpty { "Timer" },
                    elapsedMs = elapsedMs,
                    totalDurationMs = 0L,
                    isCountdown = false,
                    isRunning = true
                )
            )
            return
        }

        // Check if Maps / Navigation ongoing notification
        if (notification.category == Notification.CATEGORY_NAVIGATION || pkg.contains("maps") || pkg.contains("navigation")) {
            repo.updateNavigation(
                IslandNavigationData(
                    distanceText = title.ifEmpty { "3.6 km" },
                    streetOrInstruction = text.ifEmpty { "Continue straight" },
                    etaText = "Arrive 7:21 PM",
                    maneuverType = "STRAIGHT",
                    isActive = true
                )
            )
            return
        }

        if (!isPosted) return

        val actions = notification.actions?.take(2)?.mapNotNull { action ->
            val label = action.title?.toString()?.trim() ?: return@mapNotNull null
            IslandNotificationAction(label = label, actionIntent = action.actionIntent)
        }.orEmpty().ifEmpty {
            listOf(
                IslandNotificationAction(label = "Reply"),
                IslandNotificationAction(label = "Mark as read")
            )
        }

        val isUpdateStyle = title.contains("update", ignoreCase = true) ||
            text.contains("update available", ignoreCase = true)

        repo.postIncomingNotification(
            IslandNotificationData(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appLabel,
                title = title.ifEmpty { appLabel },
                subtitle = if (isUpdateStyle) text else "",
                body = text,
                timeLabel = "Now",
                isOngoingDownload = extras.containsKey(Notification.EXTRA_PROGRESS),
                isUpdateBanner = isUpdateStyle,
                actions = actions,
                contentIntent = notification.contentIntent
            )
        )
    }

    fun seekActiveSessionTo(positionMs: Long) {
        runCatching {
            activeController?.transportControls?.seekTo(positionMs)
        }
    }

    override fun onDestroy() {
        runCatching {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
            activeController?.unregisterCallback(controllerCallback)
        }
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: IslandNotificationListenerService? = null
            private set
    }
}
