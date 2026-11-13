package com.xiao.androidisland.core.system

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import com.xiao.androidisland.core.model.BiometricAuthType
import com.xiao.androidisland.core.model.IslandActivityState
import com.xiao.androidisland.core.model.IslandNotificationItem
import com.xiao.androidisland.core.model.LiquidIslandSettings
import com.xiao.androidisland.core.model.MediaTrackState
import com.xiao.androidisland.core.model.NavigationActivityState
import com.xiao.androidisland.core.model.RingerModeType
import com.xiao.androidisland.core.model.SecondaryBubbleType
import com.xiao.androidisland.core.model.SystemStatusSnapshot
import com.xiao.androidisland.core.model.TimerActivityState
import com.xiao.androidisland.core.permission.ShizukuBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Central System State, MediaSession, Notification, and Dynamic Island Activity Manager.
 * Synchronizes real Android system broadcasts (Clock, Timezone, Battery, Charging, RingerMode,
 * Wi-Fi, Mobile Data, Brightness, Volume, MediaSession) with the LiquidIsland state machine.
 */
class SystemStateManager(
    context: Context,
    val shizukuBridge: ShizukuBridge = ShizukuBridge(),
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val _settings = MutableStateFlow(LiquidIslandSettings())
    val settings: StateFlow<LiquidIslandSettings> = _settings.asStateFlow()

    private val _statusSnapshot = MutableStateFlow(SystemStatusSnapshot())
    val statusSnapshot: StateFlow<SystemStatusSnapshot> = _statusSnapshot.asStateFlow()

    private val _islandState = MutableStateFlow<IslandActivityState>(
        IslandActivityState.CompactMusic(MediaTrackState())
    )
    val islandState: StateFlow<IslandActivityState> = _islandState.asStateFlow()

    private val _currentTrack = MutableStateFlow(
        MediaTrackState(
            title = "Jane!!",
            artist = "AjayRoy",
            isPlaying = true,
            positionMs = 53_000L,
            durationMs = 201_000L,
            primaryColor = Color(0xFF8B2635),
            secondaryColor = Color(0xFF1F2937),
        )
    )
    val currentTrack: StateFlow<MediaTrackState> = _currentTrack.asStateFlow()

    private val _timerState = MutableStateFlow(TimerActivityState(isRunning = false, elapsedSeconds = 1L))
    private var timerJob: Job? = null
    private var transientAlertJob: Job? = null
    private var isRegistered = false

    private val demoTracks = listOf(
        MediaTrackState("Jane!!", "AjayRoy", true, 53_000L, 201_000L, Color(0xFF8B2635), Color(0xFF1E293B)),
        MediaTrackState("Restless", "untitled", true, 33_000L, 173_000L, Color(0xFF475569), Color(0xFF0F172A)),
        MediaTrackState("mirrors demo", "overtonight throwaways", true, 2_000L, 105_000L, Color(0xFF312E81), Color(0xFF111827)),
        MediaTrackState("Stephanie (Slowed)", "Nafeesisboujee, 10X", true, 1_000L, 142_000L, Color(0xFF78350F), Color(0xFF1C1917)),
        MediaTrackState("VIDEOCLUB - ROI | INSTR", "harisgaming", true, 12_000L, 154_000L, Color(0xFF9D174D), Color(0xFF1E1B4B)),
    )
    private var currentTrackIndex = 0

    private val settingsObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            refreshSystemStatus()
        }
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val act = intent?.action.orEmpty()
            refreshSystemStatus()
            when (act) {
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    showTransientIslandAlert(
                        IslandActivityState.RingerAlert(_statusSnapshot.value.ringerMode)
                    )
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    showTransientIslandAlert(
                        IslandActivityState.ChargingAlert(_statusSnapshot.value.batteryPercent)
                    )
                }
            }
        }
    }

    fun startObserving() {
        if (isRegistered) return
        isRegistered = true
        shizukuBridge.register()

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(systemReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(systemReceiver, filter)
            }
        } catch (_: Throwable) {}

        try {
            appContext.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false,
                settingsObserver,
            )
        } catch (_: Throwable) {}

        refreshSystemStatus()
    }

    fun stopObserving() {
        if (!isRegistered) return
        isRegistered = false
        shizukuBridge.unregister()
        try {
            appContext.unregisterReceiver(systemReceiver)
        } catch (_: Throwable) {}
        try {
            appContext.contentResolver.unregisterContentObserver(settingsObserver)
        } catch (_: Throwable) {}
    }

    fun updateSettings(newSettings: LiquidIslandSettings) {
        _settings.value = newSettings
        refreshSystemStatus()
    }

    @Suppress("DEPRECATION", "MissingPermission")
    fun refreshSystemStatus() {
        val now = LocalDateTime.now()
        val timePattern = if (_settings.value.use24HourClock) "HH:mm" else "h:mm"
        val clockStr = now.format(DateTimeFormatter.ofPattern(timePattern, Locale.ENGLISH))
        val dateStr = now.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))

        val batteryPct = try {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 1..100 } ?: 80
        } catch (_: Throwable) {
            80
        }
        val charging = try {
            batteryManager?.isCharging == true
        } catch (_: Throwable) {
            false
        }

        val ringer = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerModeType.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> RingerModeType.VIBRATE
            else -> RingerModeType.RING
        }

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, maxVol)
        val volPct = ((curVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

        val brightRaw = try {
            Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 155)
        } catch (_: Throwable) {
            155
        }
        val brightPct = ((brightRaw.coerceIn(0, 255) / 255f) * 100f).roundToInt().coerceIn(0, 100)

        val airplane = try {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        } catch (_: Throwable) {
            false
        }

        val wifiEnabled = try {
            wifiManager?.isWifiEnabled ?: true
        } catch (_: Throwable) {
            true
        }

        val activeNet = connectivityManager?.activeNetwork
        val caps = activeNet?.let { connectivityManager.getNetworkCapabilities(it) }
        val mobileDataOn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        val ssid = try {
            val info = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) caps?.transportInfo as? WifiInfo else null)
                ?: wifiManager?.connectionInfo
            info?.ssid?.removeSurrounding("\"")?.takeIf {
                it.isNotBlank() && !it.equals("<unknown ssid>", ignoreCase = true)
            } ?: "WE 5G"
        } catch (_: Throwable) {
            "WE 5G"
        }

        _statusSnapshot.update { prev ->
            prev.copy(
                formattedClock = clockStr,
                formattedDateHeader = dateStr,
                batteryPercent = batteryPct,
                isCharging = charging,
                ringerMode = ringer,
                volumePercent = volPct,
                brightnessPercent = brightPct,
                isAirplaneModeEnabled = airplane,
                isWifiEnabled = wifiEnabled,
                wifiSsid = ssid,
                isMobileDataEnabled = mobileDataOn,
            )
        }
    }

    fun onIslandTapped() {
        val current = _islandState.value
        _islandState.value = when (current) {
            IslandActivityState.CompactIdle -> IslandActivityState.ExpandedMusic(_currentTrack.value)
            is IslandActivityState.CompactMusic -> IslandActivityState.ExpandedMusic(current.track)
            is IslandActivityState.ExpandedMusic -> IslandActivityState.CompactMusic(current.track)
            is IslandActivityState.SplitDualActivity -> IslandActivityState.ExpandedTimer(current.timer)
            is IslandActivityState.CompactNotification -> IslandActivityState.ExpandedNotification(current.item)
            is IslandActivityState.ExpandedNotification -> IslandActivityState.CompactIdle
            is IslandActivityState.CompactTimer -> IslandActivityState.ExpandedTimer(current.timer)
            is IslandActivityState.ExpandedTimer -> IslandActivityState.CompactTimer(current.timer)
            is IslandActivityState.CompactNavigation -> IslandActivityState.ExpandedNavigation(current.nav)
            is IslandActivityState.ExpandedNavigation -> IslandActivityState.CompactNavigation(current.nav)
            is IslandActivityState.RingerAlert -> IslandActivityState.CompactMusic(_currentTrack.value)
            is IslandActivityState.ChargingAlert -> IslandActivityState.CompactMusic(_currentTrack.value)
            is IslandActivityState.BiometricAuthAlert -> IslandActivityState.CompactMusic(_currentTrack.value)
        }
    }

    fun onIslandSecondaryTapped() {
        val current = _islandState.value
        if (current is IslandActivityState.SplitDualActivity) {
            _islandState.value = IslandActivityState.ExpandedMusic(current.track)
        } else {
            _islandState.value = IslandActivityState.SplitDualActivity(
                track = _currentTrack.value,
                secondaryType = SecondaryBubbleType.TIMER,
                timer = _timerState.value,
            )
        }
    }

    fun triggerStateByKey(key: String) {
        transientAlertJob?.cancel()
        when (key) {
            "Idle" -> _islandState.value = IslandActivityState.CompactIdle
            "NotifCompact" -> _islandState.value = IslandActivityState.CompactNotification(IslandNotificationItem())
            "NotifExpanded" -> _islandState.value = IslandActivityState.ExpandedNotification(IslandNotificationItem())
            "NotifBanner" -> _islandState.value = IslandActivityState.ExpandedNotification(
                IslandNotificationItem(
                    key = "metrolist_1",
                    appName = "Metrolist",
                    timeLabel = "Now",
                    title = "Update available",
                    body = "13.7.0",
                    isCompactBanner = true,
                )
            )
            "MusicCompact" -> _islandState.value = IslandActivityState.CompactMusic(_currentTrack.value)
            "SplitDual" -> {
                ensureTimerRunning()
                _islandState.value = IslandActivityState.SplitDualActivity(
                    track = _currentTrack.value,
                    secondaryType = SecondaryBubbleType.TIMER,
                    timer = _timerState.value,
                )
            }
            "MusicExpanded" -> _islandState.value = IslandActivityState.ExpandedMusic(_currentTrack.value)
            "TimerCompact" -> {
                ensureTimerRunning()
                _islandState.value = IslandActivityState.CompactTimer(_timerState.value)
            }
            "TimerExpanded" -> {
                ensureTimerRunning()
                _islandState.value = IslandActivityState.ExpandedTimer(_timerState.value)
            }
            "NavCompact" -> _islandState.value = IslandActivityState.CompactNavigation(NavigationActivityState(isActive = true))
            "NavExpanded" -> _islandState.value = IslandActivityState.ExpandedNavigation(NavigationActivityState(isActive = true))
            "Ring" -> showTransientIslandAlert(IslandActivityState.RingerAlert(RingerModeType.RING))
            "Vibrate" -> showTransientIslandAlert(IslandActivityState.RingerAlert(RingerModeType.VIBRATE))
            "Silent" -> showTransientIslandAlert(IslandActivityState.RingerAlert(RingerModeType.SILENT))
            "Charging" -> showTransientIslandAlert(IslandActivityState.ChargingAlert(_statusSnapshot.value.batteryPercent))
            "Auth" -> showTransientIslandAlert(
                IslandActivityState.BiometricAuthAlert(
                    authType = _settings.value.lockScreenAuthType,
                    isAuthenticated = true,
                )
            )
        }
    }

    private fun showTransientIslandAlert(alertState: IslandActivityState) {
        transientAlertJob?.cancel()
        val previous = _islandState.value
        _islandState.value = alertState
        transientAlertJob = scope.launch {
            delay(2_400L)
            if (_islandState.value == alertState) {
                _islandState.value = if (
                    previous is IslandActivityState.RingerAlert ||
                    previous is IslandActivityState.ChargingAlert ||
                    previous is IslandActivityState.BiometricAuthAlert
                ) {
                    IslandActivityState.CompactMusic(_currentTrack.value)
                } else {
                    previous
                }
            }
        }
    }

    private fun ensureTimerRunning() {
        if (timerJob?.isActive == true) return
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                _timerState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1L) }
                when (val cur = _islandState.value) {
                    is IslandActivityState.CompactTimer -> {
                        _islandState.value = cur.copy(timer = _timerState.value)
                    }
                    is IslandActivityState.ExpandedTimer -> {
                        _islandState.value = cur.copy(timer = _timerState.value)
                    }
                    is IslandActivityState.SplitDualActivity -> {
                        _islandState.value = cur.copy(timer = _timerState.value)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onTimerLap() {
        _timerState.update { it.copy(lapCount = it.lapCount + 1) }
    }

    fun onTimerCancel() {
        timerJob?.cancel()
        _timerState.value = TimerActivityState(isRunning = false, elapsedSeconds = 0L)
        _islandState.value = IslandActivityState.CompactMusic(_currentTrack.value)
    }

    fun onMediaPrevious() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        currentTrackIndex = (currentTrackIndex - 1 + demoTracks.size) % demoTracks.size
        val nextTrack = demoTracks[currentTrackIndex]
        _currentTrack.value = nextTrack
        syncTrackIntoCurrentIslandState(nextTrack)
    }

    fun onMediaPlayPause() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val updated = _currentTrack.value.copy(isPlaying = !_currentTrack.value.isPlaying)
        _currentTrack.value = updated
        syncTrackIntoCurrentIslandState(updated)
    }

    fun onMediaNext() {
        dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
        currentTrackIndex = (currentTrackIndex + 1) % demoTracks.size
        val nextTrack = demoTracks[currentTrackIndex]
        _currentTrack.value = nextTrack
        syncTrackIntoCurrentIslandState(nextTrack)
    }

    private fun syncTrackIntoCurrentIslandState(track: MediaTrackState) {
        when (val cur = _islandState.value) {
            is IslandActivityState.CompactMusic -> _islandState.value = cur.copy(track = track)
            is IslandActivityState.ExpandedMusic -> _islandState.value = cur.copy(track = track)
            is IslandActivityState.SplitDualActivity -> _islandState.value = cur.copy(track = track)
            else -> Unit
        }
    }

    fun onIncomingRealNotification(item: IslandNotificationItem) {
        _statusSnapshot.update { it.copy(activeNotificationCount = (it.activeNotificationCount + 1).coerceAtMost(9)) }
        _islandState.value = IslandActivityState.ExpandedNotification(item)
    }

    fun updateRealMediaMetadata(title: String, artist: String, isPlaying: Boolean) {
        val updated = _currentTrack.value.copy(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
        )
        _currentTrack.value = updated
        syncTrackIntoCurrentIslandState(updated)
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val step = ((clamped / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0)
        } catch (_: Throwable) {}
        _statusSnapshot.update { it.copy(volumePercent = clamped) }
    }

    fun setBrightnessPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        if (Settings.System.canWrite(appContext)) {
            val raw = ((clamped / 100f) * 255f).roundToInt().coerceIn(1, 255)
            try {
                Settings.System.putInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, raw)
            } catch (_: Throwable) {}
        }
        _statusSnapshot.update { it.copy(brightnessPercent = clamped) }
    }

    fun cycleRingerMode() {
        val next = when (_statusSnapshot.value.ringerMode) {
            RingerModeType.RING -> RingerModeType.VIBRATE
            RingerModeType.VIBRATE -> RingerModeType.SILENT
            RingerModeType.SILENT -> RingerModeType.RING
        }
        if (notificationManager?.isNotificationPolicyAccessGranted == true || next != RingerModeType.SILENT) {
            try {
                audioManager.ringerMode = when (next) {
                    RingerModeType.RING -> AudioManager.RINGER_MODE_NORMAL
                    RingerModeType.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                    RingerModeType.SILENT -> AudioManager.RINGER_MODE_SILENT
                }
            } catch (_: Throwable) {}
        }
        _statusSnapshot.update { it.copy(ringerMode = next) }
        showTransientIslandAlert(IslandActivityState.RingerAlert(next))
    }

    fun toggleWifi(context: Context) {
        val target = !_statusSnapshot.value.isWifiEnabled
        scope.launch {
            val ok = shizukuBridge.executeCommand(if (target) "svc wifi enable" else "svc wifi disable")
            if (!ok) {
                try {
                    context.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Throwable) {}
            }
            _statusSnapshot.update { it.copy(isWifiEnabled = target) }
        }
    }

    fun toggleMobileData(context: Context) {
        val target = !_statusSnapshot.value.isMobileDataEnabled
        scope.launch {
            val ok = shizukuBridge.executeCommand(if (target) "svc data enable" else "svc data disable")
            if (!ok) {
                try {
                    context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Throwable) {}
            }
            _statusSnapshot.update { it.copy(isMobileDataEnabled = target) }
        }
    }

    fun toggleAirplaneMode(context: Context) {
        val target = !_statusSnapshot.value.isAirplaneModeEnabled
        scope.launch {
            val flag = if (target) 1 else 0
            val ok = shizukuBridge.executeCommand("settings put global airplane_mode_on $flag")
            if (!ok) {
                try {
                    context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Throwable) {}
            }
            _statusSnapshot.update { it.copy(isAirplaneModeEnabled = target) }
        }
    }

    fun toggleHotspot(context: Context) {
        val target = !_statusSnapshot.value.isHotspotEnabled
        _statusSnapshot.update { it.copy(isHotspotEnabled = target) }
        try {
            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {}
    }

    private fun dispatchMediaKey(keyCode: Int) {
        try {
            val now = SystemClock.uptimeMillis()
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        } catch (_: Throwable) {}
    }

    private val _isControlCenterVisible = MutableStateFlow(false)
    val isControlCenterVisible: StateFlow<Boolean> = _isControlCenterVisible.asStateFlow()

    fun setControlCenterVisible(visible: Boolean) {
        _isControlCenterVisible.value = visible
    }

    fun triggerBiometricAuthSequence() {
        triggerStateByKey("Auth")
    }

    fun updateSettings(transform: (LiquidIslandSettings) -> LiquidIslandSettings) {
        _settings.value = transform(_settings.value)
    }

    fun endNavigation() {
        _islandState.value = IslandActivityState.CompactIdle
    }

    fun dismissNotification() {
        _islandState.value = IslandActivityState.CompactIdle
    }

    fun seekMediaTo(positionMs: Long) {
        _currentTrack.update { it.copy(positionMs = positionMs.coerceIn(0L, it.durationMs.coerceAtLeast(1L))) }
        syncTrackIntoCurrentIslandState(_currentTrack.value)
    }
}
