package com.xiao.androidisland.core.model

import android.app.PendingIntent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

enum class IslandMode {
    DROID_BUBBLE,
    LIQUID_ISLAND,
}

enum class RingerModeType {
    RING,
    VIBRATE,
    SILENT,
}

enum class BiometricAuthType {
    FACE_ID,
    FINGERPRINT,
}

enum class AbsorptionStage {
    UNTOUCHED,
    APPROACH,
    CONTACT,
    ABSORBED,
    RELEASING,
}

@Immutable
data class LiquidIslandSettings(
    val activeMode: IslandMode = IslandMode.LIQUID_ISLAND,
    val isLiquidIslandEnabled: Boolean = true,
    val isIosStatusBarEnabled: Boolean = true,
    val isControlCenterEnabled: Boolean = true,
    val isDarkMode: Boolean = false,
    val islandWidthScale: Float = 1.0f,
    val islandHeightScale: Float = 1.0f,
    val islandHorizontalOffsetDp: Float = 0f,
    val islandVerticalOffsetDp: Float = 0f,
    val animationSpeedMultiplier: Float = 1.0f,
    val liquidGlassIntensity: Float = 0.88f,
    val refractionIntensity: Float = 0.85f,
    val motionBlurIntensity: Float = 0.90f,
    val absorptionIntensity: Float = 0.92f,
    val isAlbumGradientEnabled: Boolean = true,
    val lockScreenAuthType: BiometricAuthType = BiometricAuthType.FACE_ID,
    val use24HourClock: Boolean = false,
    val showCalendarWidget: Boolean = true,
    val showClockWidget: Boolean = true,
)

@Immutable
data class MediaTrackState(
    val title: String = "Restless",
    val artist: String = "untitled",
    val isPlaying: Boolean = true,
    val positionMs: Long = 33_000L,
    val durationMs: Long = 173_000L,
    val primaryColor: Color = Color(0xFF4B5563),
    val secondaryColor: Color = Color(0xFF1F2937),
    val appName: String = "Spotify",
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0.25f

    val formattedElapsed: String
        get() = formatSeconds(positionMs / 1000L)

    val formattedRemaining: String
        get() {
            val remSec = ((durationMs - positionMs).coerceAtLeast(0L)) / 1000L
            return "-${formatSeconds(remSec)}"
        }

    companion object {
        fun formatSeconds(totalSeconds: Long): String {
            val mins = (totalSeconds / 60L).coerceAtLeast(0L)
            val secs = (totalSeconds % 60L).coerceAtLeast(0L)
            return "%02d:%02d".format(mins, secs)
        }
    }
}

@Immutable
data class IslandNotificationItem(
    val key: String = "tg_1",
    val appName: String = "Telegram",
    val timeLabel: String = "Now",
    val title: String = "Video Downloader | Instagram TikTok",
    val body: String = "🎉 +12 hours of Premium for every friend who joins with your link and downloads their first video 👑",
    val isCompactBanner: Boolean = false,
    val accentColor: Color = Color(0xFF2AABEE),
    val contentIntent: PendingIntent? = null,
    val actionLabels: List<String> = listOf("Reply", "Mark as read"),
)

@Immutable
data class TimerActivityState(
    val isRunning: Boolean = true,
    val elapsedSeconds: Long = 14L,
    val lapCount: Int = 1,
) {
    val formattedTime: String
        get() = MediaTrackState.formatSeconds(elapsedSeconds)
}

@Immutable
data class NavigationActivityState(
    val isActive: Boolean = false,
    val distanceText: String = "3.6 km",
    val instructionText: String = "Continue straight ahead",
    val arrivalTimeText: String = "Arrive 7:21 PM",
)

/**
 * Represents the 10 distinct Dynamic Island states analyzed from `96942.mp4` (`Dynamic Motion 1.2 For Android`).
 */
sealed interface IslandActivityState {
    val widthDp: Float
    val heightDp: Float
    val cornerRadiusDp: Float
    val hasDetachedBubble: Boolean
        get() = false
    val detachedBubbleSizeDp: Float
        get() = 0f
    val detachedBubbleGapDp: Float
        get() = 0f

    /** 00:00 — Compact Idle Pill (`120.0 x 32.0 dp`) */
    data object CompactIdle : IslandActivityState {
        override val widthDp: Float = 120f
        override val heightDp: Float = 32f
        override val cornerRadiusDp: Float = 16f
    }

    /** 00:01 — Compact Incoming Notification Pill (`192.0 x 34.0 dp`) */
    data class CompactNotification(val item: IslandNotificationItem) : IslandActivityState {
        override val widthDp: Float = 192f
        override val heightDp: Float = 34f
        override val cornerRadiusDp: Float = 17f
    }

    /** 00:04 & 00:10 — Expanded Notification Card (`347.0 x 178.0 dp` or `347.0 x 86.0 dp`) */
    data class ExpandedNotification(val item: IslandNotificationItem) : IslandActivityState {
        override val widthDp: Float = 347f
        override val heightDp: Float = if (item.isCompactBanner) 86f else 178f
        override val cornerRadiusDp: Float = if (item.isCompactBanner) 32f else 38f
    }

    /** 00:07 — Compact Music Pill (`150.0 x 32.0 dp`) */
    data class CompactMusic(val track: MediaTrackState) : IslandActivityState {
        override val widthDp: Float = 150f
        override val heightDp: Float = 32f
        override val cornerRadiusDp: Float = 16f
    }

    /** 00:09, 00:25, 00:49 — Split Dual Island (`132.0 x 32.5 dp` main pill + `32.5 dp` detached circle) */
    data class SplitDualActivity(
        val track: MediaTrackState,
        val secondaryType: SecondaryBubbleType = SecondaryBubbleType.TIMER,
        val timer: TimerActivityState = TimerActivityState(),
    ) : IslandActivityState {
        override val widthDp: Float = 132f
        override val heightDp: Float = 32.5f
        override val cornerRadiusDp: Float = 16.25f
        override val hasDetachedBubble: Boolean = true
        override val detachedBubbleSizeDp: Float = 32.5f
        override val detachedBubbleGapDp: Float = 8f
    }

    /** 00:13 & 01:08 — Expanded Music Player Card (`347.0 x 180.0 dp`) */
    data class ExpandedMusic(val track: MediaTrackState) : IslandActivityState {
        override val widthDp: Float = 347f
        override val heightDp: Float = 180f
        override val cornerRadiusDp: Float = 40f
    }

    /** 00:22 — Compact Timer Pill (`142.0 x 32.5 dp`) */
    data class CompactTimer(val timer: TimerActivityState) : IslandActivityState {
        override val widthDp: Float = 142f
        override val heightDp: Float = 32.5f
        override val cornerRadiusDp: Float = 16.25f
    }

    /** 00:28 — Expanded Timer Card (`347.0 x 91.0 dp`) */
    data class ExpandedTimer(val timer: TimerActivityState) : IslandActivityState {
        override val widthDp: Float = 347f
        override val heightDp: Float = 91f
        override val cornerRadiusDp: Float = 36f
    }

    /** 00:39 & 00:47 — Compact Navigation Pill (`148.0 x 32.5 dp`) */
    data class CompactNavigation(val nav: NavigationActivityState) : IslandActivityState {
        override val widthDp: Float = 148f
        override val heightDp: Float = 32.5f
        override val cornerRadiusDp: Float = 16.25f
    }

    /** 00:44 — Expanded Navigation Card (`340.0 x 136.0 dp`) */
    data class ExpandedNavigation(val nav: NavigationActivityState) : IslandActivityState {
        override val widthDp: Float = 340f
        override val heightDp: Float = 136f
        override val cornerRadiusDp: Float = 36f
    }

    /** 00:50, 00:51, 00:54, 00:58 — Wide Ringer Alert Pill (`295.0 x 36.0 dp`) */
    data class RingerAlert(val ringerMode: RingerModeType) : IslandActivityState {
        override val widthDp: Float = 295f
        override val heightDp: Float = 36f
        override val cornerRadiusDp: Float = 18f
    }

    /** 01:03 — Wide Charging Alert Pill (`295.0 x 36.0 dp`) */
    data class ChargingAlert(val batteryPercent: Int) : IslandActivityState {
        override val widthDp: Float = 295f
        override val heightDp: Float = 36f
        override val cornerRadiusDp: Float = 18f
    }

    /** 01:19–01:25 — Biometric Lock Screen Auth Pill (`188.0 x 38.0 dp`) */
    data class BiometricAuthAlert(
        val authType: BiometricAuthType,
        val isAuthenticated: Boolean = true,
    ) : IslandActivityState {
        override val widthDp: Float = 188f
        override val heightDp: Float = 38f
        override val cornerRadiusDp: Float = 19f
    }
}

enum class SecondaryBubbleType {
    MUSIC_WAVEFORM,
    TIMER,
    DOWNLOAD,
}

@Immutable
data class SystemStatusSnapshot(
    val formattedClock: String = "7:02",
    val formattedDateHeader: String = "Tue, Oct 13",
    val signalBarsPrimary: Int = 4,
    val signalBarsSecondarySim: Int = 3,
    val isDualSimActive: Boolean = true,
    val isWifiEnabled: Boolean = true,
    val wifiSsid: String = "WE 5G",
    val isMobileDataEnabled: Boolean = false,
    val isAirplaneModeEnabled: Boolean = false,
    val isHotspotEnabled: Boolean = false,
    val isBluetoothEnabled: Boolean = true,
    val isFlashlightEnabled: Boolean = false,
    val isRotationLocked: Boolean = true,
    val batteryPercent: Int = 80,
    val isCharging: Boolean = false,
    val ringerMode: RingerModeType = RingerModeType.RING,
    val brightnessPercent: Int = 62,
    val volumePercent: Int = 54,
    val activeNotificationCount: Int = 1,
)
