package com.xiao.androidisland.service

import android.app.Notification
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.xiao.androidisland.AndroidIslandApp
import com.xiao.androidisland.core.model.IslandNotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val repo = AndroidIslandApp.systemOrNull(this) ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        val appLabel = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
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
                isDownloadProgress = extras.containsKey(Notification.EXTRA_PROGRESS),
                isCompactBannerStyle = isUpdateStyle
            )
        )
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
        val controller = activeController ?: return
        val metadata = controller.metadata ?: return
        val playbackState = controller.playbackState
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: return
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unknown Artist"
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        repo.updateRealMediaMetadata(title = title, artist = artist, isPlaying = isPlaying)
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
