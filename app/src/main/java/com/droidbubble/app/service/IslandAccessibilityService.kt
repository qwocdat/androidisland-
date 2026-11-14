package com.droidbubble.app.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.util.LicenseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IslandAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastClockScanMs: Long = 0L

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected = _isConnected.asStateFlow()

        var instance: IslandAccessibilityService? = null
            private set

        private val CLOCK_PACKAGES = setOf(
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.coloros.alarmclock",
            "com.oplus.alarmclock",
            "com.oneplus.deskclock",
            "com.asus.deskclock"
        )

        private val STOPWATCH_TIME_REGEX = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?(?:[.,](\d{1,2}))?$""")

        fun openNotificationShade(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false
        }

        fun openQuickSettings(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) ?: false
        }

        fun toggleSplitScreen(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) ?: false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km != null) {
            IslandStateManager.setDeviceLocked(km.isKeyguardLocked)
        }
        IslandOverlayService.onAccessibilityServiceChanged(this)
        serviceScope.launch {
            LicenseManager.syncDeviceTelemetry(
                context = applicationContext,
                accessibilityOverride = true
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val pkg = ev.packageName?.toString() ?: return

        if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null) {
                IslandStateManager.setDeviceLocked(km.isKeyguardLocked)
            }
            if (pkg == IslandStateManager.dismissedMediaPackage) {
                IslandStateManager.unDismissMedia(pkg)
            }
            NotificationReceiverService.instance?.refreshMedia()
            NotificationReceiverService.instance?.scanActiveClockAndNavigationNotifications()
        }

        val isClockApp = CLOCK_PACKAGES.contains(pkg) || pkg.contains("deskclock") || pkg.contains("clock")
        if (isClockApp && (
                ev.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                ev.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            )
        ) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastClockScanMs >= 220L || ev.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                lastClockScanMs = now
                scanClockAppWindow(pkg)
            }
        }
    }

    private fun scanClockAppWindow(pkg: String) {
        val root = rootInActiveWindow ?: return
        try {
            val texts = mutableListOf<String>()
            collectNodeTexts(root, texts, 0)
            if (texts.isEmpty()) return

            var bestSeconds: Long? = null
            var hasCentiseconds = false
            for (raw in texts) {
                val cleaned = raw.trim()
                val m = STOPWATCH_TIME_REGEX.matchEntire(cleaned) ?: continue
                val g1 = m.groupValues[1].toLongOrNull() ?: 0L
                val g2 = m.groupValues[2].toLongOrNull() ?: 0L
                val g3 = m.groupValues[3].toLongOrNull()
                val g4 = m.groupValues[4]
                val totalSeconds = if (g3 != null) {
                    g1 * 3600L + g2 * 60L + g3
                } else {
                    g1 * 60L + g2
                }
                if (g4.isNotEmpty() || totalSeconds > 0L) {
                    bestSeconds = totalSeconds
                    if (g4.isNotEmpty()) hasCentiseconds = true
                    if (hasCentiseconds && totalSeconds > 0L) break
                }
            }

            val allJoined = texts.joinToString(" ").lowercase()
            val isResetZero = bestSeconds == null || (bestSeconds == 0L && (allJoined.contains("00:00.00") || allJoined.contains("00:00,00")))
            val hasPauseOrLap = allJoined.contains("tạm dừng") ||
                allJoined.contains("pause") ||
                allJoined.contains("vòng") ||
                allJoined.contains("lap") ||
                allJoined.contains("dừng") ||
                allJoined.contains("stop")
            val hasResumeOrStart = allJoined.contains("tiếp tục") ||
                allJoined.contains("resume") ||
                allJoined.contains("bắt đầu") ||
                allJoined.contains("start") ||
                allJoined.contains("đặt lại") ||
                allJoined.contains("reset")

            if (bestSeconds != null && (!isResetZero || hasPauseOrLap)) {
                val isTimer = !hasCentiseconds && (allJoined.contains("hẹn giờ") || allJoined.contains("timer") || allJoined.contains("đếm ngược"))
                val isRunning = hasPauseOrLap || !hasResumeOrStart
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val pendingIntent = if (launchIntent != null) {
                    PendingIntent.getActivity(
                        this,
                        pkg.hashCode(),
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else null

                IslandStateManager.syncExternalClockActivity(
                    seconds = bestSeconds,
                    isCountdown = isTimer,
                    isRunning = isRunning,
                    label = if (isTimer) "Hẹn giờ" else "Bấm giờ",
                    showIntent = pendingIntent
                )
            } else if (isResetZero && !hasPauseOrLap && IslandStateManager.isAlarmActive) {
                IslandStateManager.clearExternalClockActivity()
            }
        } catch (_: Throwable) {
        } finally {
            @Suppress("DEPRECATION")
            runCatching { root.recycle() }
        }
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > 14 || out.size > 48) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            collectNodeTexts(child, out, depth + 1)
            @Suppress("DEPRECATION")
            runCatching { child.recycle() }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isConnected.value = false
        IslandOverlayService.onAccessibilityServiceChanged(null)
        serviceScope.launch {
            LicenseManager.syncDeviceTelemetry(
                context = applicationContext,
                accessibilityOverride = false
            )
        }
    }
}
