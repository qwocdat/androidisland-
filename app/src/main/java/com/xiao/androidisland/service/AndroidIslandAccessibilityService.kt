package com.xiao.androidisland.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.xiao.androidisland.AndroidIslandApp

/**
 * Accessibility interceptor that coordinates top-edge swipe gestures, collapsed status bar
 * enforcement, and biometric prompt detection for LiquidIsland.
 */
class AndroidIslandAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AndroidIslandOverlayService.startOrUpdate(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString().orEmpty()
        val cls = event.className?.toString().orEmpty()

        // Detect system biometric prompt window to trigger LiquidIsland Face ID / Fingerprint morph
        if (pkg.contains("biometric", ignoreCase = true) ||
            cls.contains("BiometricPrompt", ignoreCase = true) ||
            cls.contains("AuthContainerView", ignoreCase = true)
        ) {
            AndroidIslandApp.systemOrNull(this)?.triggerBiometricAuthSequence()
        }
    }

    override fun onInterrupt() = Unit

    fun collapseSystemShade() {
        runCatching {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile
        var instance: AndroidIslandAccessibilityService? = null
            private set
    }
}
