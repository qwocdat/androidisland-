package com.droidbubble.app.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val createIntent: (Context) -> Intent
)

object PermissionHelper {

    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isNotificationListenerGranted(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    fun isNotificationPostingGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun isAccessibilityGranted(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        val myService = "${context.packageName}/${com.droidbubble.app.service.IslandAccessibilityService::class.java.name}"
        val myShortService = "${context.packageName}/.service.IslandAccessibilityService"
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(myService, ignoreCase = true) || componentName.equals(myShortService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun getPendingPermissions(context: Context): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        if (!isOverlayGranted(context)) {
            list.add(
                PermissionItem(
                    id = "overlay",
                    title = "Hiển thị trên các ứng dụng khác",
                    description = "Cần thiết để hiển thị Đảo động trên mọi ứng dụng",
                    isGranted = false,
                    createIntent = { ctx ->
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${ctx.packageName}")
                        )
                    }
                )
            )
        }

        if (!isNotificationListenerGranted(context)) {
            list.add(
                PermissionItem(
                    id = "notification_listener",
                    title = "Truy cập Thông báo & Đa phương tiện",
                    description = "Cần thiết để hiển thị trình phát nhạc và thông báo đến",
                    isGranted = false,
                    createIntent = {
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    }
                )
            )
        }

        if (!isNotificationPostingGranted(context)) {
            list.add(
                PermissionItem(
                    id = "post_notifications",
                    title = "Gửi thông báo",
                    description = "Cần thiết để duy trì dịch vụ chạy nền trên Android 13+",
                    isGranted = false,
                    createIntent = { ctx ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                            }
                        } else {
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${ctx.packageName}")
                            )
                        }
                    }
                )
            )
        }

        if (!isBatteryOptimizationIgnored(context)) {
            list.add(
                PermissionItem(
                    id = "battery_optimization",
                    title = "Không hạn chế pin",
                    description = "Ngăn hệ thống tự động tắt dịch vụ Đảo động khi chạy nền",
                    isGranted = false,
                    createIntent = { ctx ->
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${ctx.packageName}")
                        )
                    }
                )
            )
        }

        if (!isAccessibilityGranted(context)) {
            list.add(
                PermissionItem(
                    id = "accessibility",
                    title = "Chạm trên thanh trạng thái (Trợ năng)",
                    description = "Cần thiết để hiển thị Đảo động đè lên thanh trạng thái và nhận thao tác chạm mượt mà",
                    isGranted = false,
                    createIntent = {
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    }
                )
            )
        }

        return list
    }
}
