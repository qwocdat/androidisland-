package com.droidbubble.app.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.widget.Toast
import rikka.shizuku.Shizuku

/**
 * Suppresses / hides built-in native OS Dynamic Islands across modern Android skins:
 *  - Xiaomi HyperOS: HyperIsland / Focus Notifications (Thông báo tiêu điểm)
 *  - OPPO / OnePlus / Realme ColorOS & OxygenOS: Fluid Cloud (Thiết bị & Dịch vụ nổi)
 *  - Vivo / iQOO OriginOS: Atomic Island (Đảo nguyên tử)
 *  - Honor MagicOS: Magic Capsule
 *  - Huawei HarmonyOS / EMUI: Live View
 *  - Samsung One UI: Now Bar / Live Notifications
 */
object NativeIslandSuppressor {

    private val SYSTEM_ISLAND_KEYS = listOf(
        // Xiaomi HyperOS (HyperIsland & Focus Notifications)
        "miui_focus_notification_enable",
        "hyper_island_enable",
        "focus_notification_status",
        "status_bar_show_focus_notification",
        "miui_bubble_notification",
        // OPPO / OnePlus / Realme (Fluid Cloud)
        "oplus_customize_fluid_cloud_enable",
        "coloros_fluid_cloud_switch",
        "fluid_cloud_enable",
        // Vivo / iQOO OriginOS (Atomic Island)
        "vivo_atomic_island_switch",
        "atomic_island_enable",
        // Honor MagicOS (Magic Capsule)
        "magic_capsule_enable",
        "honor_magic_capsule_switch",
        // Samsung One UI (Now Bar / Ongoing Chip)
        "now_bar_enabled",
        "ongoing_activity_chip_enabled"
    )

    private val NATIVE_ISLAND_PACKAGES = setOf(
        "miui.systemui.plugin",
        "com.miui.systemui.plugin",
        "com.oplus.fluidcloud",
        "com.oplus.smartengine",
        "com.vivo.smartisland",
        "com.vivo.originord",
        "com.hihonor.magiccapsule"
    )

    fun getDetectedOsIslandName(): String {
        val brand = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        return when {
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") ->
                "Xiaomi HyperOS (HyperIsland / Thông báo tiêu điểm)"
            brand.contains("oppo") || brand.contains("oneplus") || brand.contains("realme") ->
                "ColorOS / OxygenOS (Fluid Cloud)"
            brand.contains("vivo") || brand.contains("iqoo") ->
                "OriginOS (Atomic Island / Đảo nguyên tử)"
            brand.contains("honor") ->
                "MagicOS (Magic Capsule)"
            brand.contains("huawei") ->
                "HarmonyOS (Live View)"
            brand.contains("samsung") ->
                "Samsung One UI (Now Bar / Live Chip)"
            else ->
                "HyperIsland / Đảo mặc định hệ điều hành"
        }
    }

    /**
     * Applies or restores native OS island suppression via Settings tables and Shizuku (if active).
     */
    fun applyNativeIslandSuppression(context: Context, suppress: Boolean): Boolean {
        val targetValue = if (suppress) 0 else 1
        var appliedCount = 0

        // 1. Try direct ContentResolver Settings write (works if WRITE_SECURE_SETTINGS or WRITE_SETTINGS is granted)
        val cr = context.contentResolver
        for (key in SYSTEM_ISLAND_KEYS) {
            runCatching {
                if (Settings.System.putInt(cr, key, targetValue)) appliedCount++
            }
            runCatching {
                if (Settings.Secure.putInt(cr, key, targetValue)) appliedCount++
            }
            runCatching {
                if (Settings.Global.putInt(cr, key, targetValue)) appliedCount++
            }
        }

        // 2. Try Shizuku privileged shell commands if Shizuku is running & granted
        runCatching {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                for (key in SYSTEM_ISLAND_KEYS) {
                    execShizukuCommand("settings put system $key $targetValue")
                    execShizukuCommand("settings put secure $key $targetValue")
                    execShizukuCommand("settings put global $key $targetValue")
                }
                appliedCount++
            }
        }

        return appliedCount > 0
    }

    private fun execShizukuCommand(cmd: String) {
        runCatching {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? Process
            process?.waitFor()
        }
    }

    /**
     * Checks whether a StatusBarNotification is a native system island / HyperIsland overlay notification
     * that should be suppressed so it doesn't overlap with Androidisland.
     */
    fun isNativeIslandPluginNotification(sbn: StatusBarNotification): Boolean {
        val pkg = sbn.packageName
        if (pkg in NATIVE_ISLAND_PACKAGES) return true
        val extras = sbn.notification.extras ?: return false
        return extras.containsKey("miui.focus.param") ||
            extras.containsKey("miui.focus.rv") ||
            extras.containsKey("miui.focus.pics") ||
            extras.containsKey("oplus.fluid.cloud") ||
            extras.containsKey("vivo.atomic.island") ||
            extras.containsKey("honor.magic.capsule")
    }

    /**
     * Opens the native OS settings page for HyperIsland / Focus Notifications / Fluid Cloud / Magic Capsule.
     */
    fun openNativeIslandSystemSettings(context: Context) {
        val candidateIntents = listOf(
            // Xiaomi HyperOS Focus Notification Settings
            Intent().apply {
                component = ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$MiuiFocusNotificationSettingsActivity"
                )
            },
            Intent("miui.intent.action.FOCUS_NOTIFICATION_SETTINGS"),
            Intent().apply {
                component = ComponentName(
                    "com.miui.notification",
                    "miui.notification.management.activity.FocusNotificationSettingsActivity"
                )
            },
            // OPPO / OnePlus / Realme Fluid Cloud Settings
            Intent("oplus.intent.action.FLUID_CLOUD_SETTINGS"),
            Intent().apply {
                component = ComponentName(
                    "com.oplus.notificationmanager",
                    "com.oplus.notificationmanager.FluidCloudSettingsActivity"
                )
            },
            // Vivo OriginOS Atomic Island
            Intent("vivo.intent.action.ATOMIC_ISLAND_SETTINGS"),
            // Honor Magic Capsule
            Intent("com.hihonor.intent.action.MAGIC_CAPSULE_SETTINGS"),
            // Fallback to Notification & Status Bar Settings
            Intent("com.android.settings.STATUS_BAR_SETTINGS"),
            Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in candidateIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {
            }
        }
        Toast.makeText(context, "Đang mở cài đặt thông báo hệ thống...", Toast.LENGTH_SHORT).show()
    }
}
