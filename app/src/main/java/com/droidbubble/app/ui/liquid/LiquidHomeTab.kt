package com.droidbubble.app.ui.liquid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.LiquidIosLockAuthPill
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.ActiveCallState
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.ScreenRecordingState
import com.droidbubble.app.service.NativeIslandSuppressor
import com.droidbubble.app.service.VoiceMemoManager
import com.droidbubble.app.ui.main.MainScreenViewModel
import com.droidbubble.app.ui.main.PermissionHelper
import com.droidbubble.app.ui.main.UpdateCardItem
import com.droidbubble.app.util.LicenseManager
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LiquidHomeTab(
    settings: IslandSettings,
    strings: AppStrings,
    viewModel: MainScreenViewModel,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit = {},
    onOpenDroidbar: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenVideoAnnouncement: () -> Unit,
    onDeactivateLicense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsState()
    val pendingPerms by viewModel.pendingPermissions.collectAsState()

    val batteryPct = remember {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 88) ?: 88
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt().coerceIn(1, 100) else 88
    }

    var permRefreshTick by remember { mutableIntStateOf(0) }
    val runtimePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permRefreshTick++
        viewModel.checkPermissions(context)
    }

    val a11yGranted = remember(pendingPerms, permRefreshTick) { PermissionHelper.isAccessibilityGranted(context) }
    val notifGranted = remember(pendingPerms, permRefreshTick) { PermissionHelper.isNotificationListenerGranted(context) }
    val overlayGranted = remember(pendingPerms, permRefreshTick) { PermissionHelper.isOverlayGranted(context) }
    val batteryUnrestricted = remember(pendingPerms, permRefreshTick) { PermissionHelper.isBatteryOptimizationIgnored(context) }
    val micAndPhoneGranted = remember(permRefreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val btGranted = remember(permRefreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Charging / Island Preview Card (matching 00:00 - 00:04)
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clickable {
                        IslandStateManager.triggerChargingEvent(batteryPct, 33.0)
                    }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left blue vertical indicator bar
                    Box(
                        modifier = Modifier
                            .padding(vertical = 22.dp)
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        // Black Dynamic Island Charging Pill
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black)
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.chargingText,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$batteryPct %",
                                        color = Color(0xFF30D158),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    // Battery pill icon
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(3.5.dp))
                                            .background(Color(0xFF30D158).copy(alpha = 0.3f))
                                            .padding(1.5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth((batteryPct / 100f).coerceIn(0.2f, 1f))
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF30D158))
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = strings.batteryChargingNotification,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }

        // Mode Selector: Droidbubble & LiquidIsland (2 chế độ giao diện Đảo động)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Chế độ giao diện Đảo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Chọn kiểu dáng hiển thị của viên thuốc và thẻ mở rộng",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Chế độ 1: Droidbubble
                        val isDroidbubble = settings.designLanguage == com.droidbubble.app.model.DesignLanguage.DROIDBUBBLE ||
                                (!settings.glassEnabled && settings.designLanguage != com.droidbubble.app.model.DesignLanguage.GLASS)
                        Surface(
                            onClick = {
                                onUpdateSettings {
                                    it.copy(
                                        designLanguage = com.droidbubble.app.model.DesignLanguage.DROIDBUBBLE,
                                        glassEnabled = false
                                    )
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDroidbubble) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = if (isDroidbubble) BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.Black)
                                        .border(1.dp, if (isDroidbubble) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF), RoundedCornerShape(7.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Droidbubble",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isDroidbubble) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Đảo đen nguyên bản",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Chế độ 2: LiquidIsland (Dynamic Island iOS - Dynamic Motion 1.2)
                        val isLiquid = settings.designLanguage == com.droidbubble.app.model.DesignLanguage.GLASS || settings.glassEnabled
                        Surface(
                            onClick = {
                                onUpdateSettings {
                                    it.copy(
                                        designLanguage = com.droidbubble.app.model.DesignLanguage.GLASS,
                                        glassEnabled = true,
                                        showNotifications = false
                                    )
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isLiquid) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = if (isLiquid) BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.Black)
                                        .border(1.dp, if (isLiquid) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF), RoundedCornerShape(7.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Liquidisland",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isLiquid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Dynamic Island iOS",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Công tắc Ẩn thanh trạng thái ngoài màn hình & Tùy biến sâu Thanh trạng thái mới
        item {
            val activeSliderId by IslandStateManager.activeSliderId.collectAsState()
            val isDraggingAnySlider = activeSliderId != null
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDraggingAnySlider) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = if (isDraggingAnySlider) 0.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = if (isDraggingAnySlider) 0f else 1f },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CropFree,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Ẩn thanh trạng thái & Vẽ thanh mới",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ẩn ngoài màn hình toàn hệ thống và tự ẩn biểu tượng khi Đảo động che lên",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.hideStatusBar,
                            onCheckedChange = { hide ->
                                onUpdateSettings { it.copy(hideStatusBar = hide) }
                                NativeIslandSuppressor.applyStatusBarVisibility(context, hide)
                            }
                        )
                    }

                    AnimatedVisibility(visible = settings.hideStatusBar) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = if (isDraggingAnySlider) 0f else 1f },
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Nền thanh trạng thái ngoài màn hình",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val bgModes = listOf("BLACK" to "Vẽ thanh mới", "HIDE_ALL" to "Ẩn trống", "TRANSPARENT" to "Trong suốt")
                                    bgModes.forEach { (modeKey, modeLabel) ->
                                        val selected = settings.statusBarBgMode == modeKey
                                        Surface(
                                            onClick = { onUpdateSettings { it.copy(statusBarBgMode = modeKey) } },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                            border = if (selected) BorderStroke(1.4.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = modeLabel,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Kiểu biểu tượng Pin",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val batModes = listOf("CAPSULE" to "Viên thuốc xanh", "IOS_OUTLINE" to "Viền iOS", "TEXT_ONLY" to "Chỉ số %")
                                    batModes.forEach { (bKey, bLabel) ->
                                        val selected = settings.statusBarBatteryStyle == bKey
                                        Surface(
                                            onClick = { onUpdateSettings { it.copy(statusBarBatteryStyle = bKey) } },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                            border = if (selected) BorderStroke(1.4.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = bLabel,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hiện Đồng hồ (24h)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Switch(
                                        checked = settings.statusBarShowClock,
                                        onCheckedChange = { v -> onUpdateSettings { it.copy(statusBarShowClock = v) } }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hiện Chuông / Rung bên cạnh giờ", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Switch(
                                        checked = settings.statusBarShowRinger,
                                        onCheckedChange = { v -> onUpdateSettings { it.copy(statusBarShowRinger = v) } }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hiện Sóng di động kép (SIM 1 & 2)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Switch(
                                        checked = settings.statusBarShowSignal,
                                        onCheckedChange = { v -> onUpdateSettings { it.copy(statusBarShowSignal = v) } }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hiện Wi-Fi / 5G & Pin", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Switch(
                                        checked = settings.statusBarShowWifi && settings.statusBarShowBattery,
                                        onCheckedChange = { v -> onUpdateSettings { it.copy(statusBarShowWifi = v, statusBarShowBattery = v) } }
                                    )
                                }
                            }

                            LiquidSliderRow(
                                label = "Cỡ chữ & Biểu tượng thanh trạng thái",
                                value = settings.statusBarScale,
                                valueRange = 0.80f..1.25f,
                                valueText = String.format(java.util.Locale.US, "%.2fx", settings.statusBarScale),
                                showValue = settings.showSliderValues,
                                onValueChange = { v -> onUpdateSettings { it.copy(statusBarScale = v) } }
                            )
                            LiquidSliderRow(
                                label = "Căn lề ngang thanh trạng thái",
                                value = settings.statusBarHorizontalPaddingDp.toFloat(),
                                valueRange = 8f..36f,
                                valueText = "${settings.statusBarHorizontalPaddingDp} dp",
                                showValue = settings.showSliderValues,
                                onValueChange = { v -> onUpdateSettings { it.copy(statusBarHorizontalPaddingDp = v.roundToInt()) } }
                            )
                            LiquidSliderRow(
                                label = "Chiều cao thanh trạng thái",
                                value = settings.statusBarHeightDp.toFloat(),
                                valueRange = 28f..54f,
                                valueText = "${settings.statusBarHeightDp} dp",
                                showValue = settings.showSliderValues,
                                onValueChange = { v -> onUpdateSettings { it.copy(statusBarHeightDp = v.roundToInt()) } }
                            )
                            LiquidSliderRow(
                                label = "Khoảng cách tự ẩn vật thể khi Đảo chạm tới",
                                value = settings.statusBarOcclusionMarginDp.toFloat(),
                                valueRange = 0f..24f,
                                valueText = "${settings.statusBarOcclusionMarginDp} dp",
                                showValue = settings.showSliderValues,
                                onValueChange = { v -> onUpdateSettings { it.copy(statusBarOcclusionMarginDp = v.roundToInt()) } }
                            )
                        }
                    }
                }
            }
        }

        // Cài đặt riêng của chế độ Liquidisland (Dynamic Motion 1.2 For Android - giống y hệt video 96942.mp4)
        if (settings.designLanguage == com.droidbubble.app.model.DesignLanguage.GLASS || settings.glassEnabled) {
            item {
                LiquidIosDynamicMotionSettingsSection(
                    settings = settings,
                    batteryPct = batteryPct,
                    onUpdateSettings = onUpdateSettings
                )
            }
        }

        // 2. CORE FEATURES Header
        item {
            Text(
                text = strings.coreFeatures,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }

        // 3. 2-Column Permission Tiles Grid (matching 00:00 - 00:04)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PermissionTileCard(
                        title = strings.permAccessibility,
                        statusText = if (a11yGranted) strings.statusGranted else strings.statusStopped,
                        isGranted = a11yGranted,
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PermissionTileCard(
                        title = strings.permNotifications,
                        statusText = if (notifGranted) strings.statusGranted else strings.statusStopped,
                        isGranted = notifGranted,
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PermissionTileCard(
                        title = strings.permCallScreening,
                        statusText = if (overlayGranted) strings.statusGranted else strings.statusInactive,
                        isGranted = overlayGranted,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PermissionTileCard(
                        title = strings.permPhoneAccess,
                        statusText = if (micAndPhoneGranted) strings.statusGranted else strings.statusStopped,
                        isGranted = micAndPhoneGranted,
                        onClick = {
                            runtimePermLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PermissionTileCard(
                        title = strings.permAutostart,
                        statusText = if (settings.restartOnBoot) strings.statusGranted else strings.statusDisabled,
                        isGranted = settings.restartOnBoot,
                        onClick = {
                            viewModel.updateSettings { it.copy(restartOnBoot = !it.restartOnBoot) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PermissionTileCard(
                        title = strings.permBatteryLimit,
                        statusText = if (batteryUnrestricted) strings.statusDisabled else strings.statusStopped,
                        isGranted = batteryUnrestricted,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Full-width Bluetooth Tile
                PermissionTileCard(
                    title = strings.permBluetooth,
                    statusText = if (btGranted) strings.statusGranted else strings.statusStopped,
                    isGranted = btGranted,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !btGranted) {
                            runtimePermLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                        } else {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 4. Footnote & Info Hint Card
        item {
            Text(
                text = strings.permFootnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = strings.permBannerHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Preserved Extra Utilities (Droidbar, Tasks, Video Announcement, Update, License)
        item {
            Text(
                text = strings.extraUtilitiesTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 6.dp, top = 8.dp)
            )
        }



        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDroidbar() }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Thanh cạnh Droidbar", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Thanh công cụ trượt nhanh ở cạnh màn hình", style = MaterialTheme.typography.bodySmall) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerticalSplit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTasks() }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(strings.chainedTasksTitle, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(strings.chainedTasksSub, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                val detectedOsIsland = remember { NativeIslandSuppressor.getDetectedOsIslandName() }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text("Ẩn HyperIsland / Đảo mặc định", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(detectedOsIsland, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = settings.hideNativeOsIsland,
                                onCheckedChange = { enabled ->
                                    viewModel.updateSettings { it.copy(hideNativeOsIsland = enabled) }
                                    NativeIslandSuppressor.applyNativeIslandSuppression(context, enabled)
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lớp phủ đen che kín Đảo hệ thống",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = settings.nativeIslandMaskBoost,
                                onCheckedChange = { boost ->
                                    viewModel.updateSettings { it.copy(nativeIslandMaskBoost = boost) }
                                }
                            )
                        }
                        OutlinedButton(
                            onClick = { NativeIslandSuppressor.openNativeIslandSystemSettings(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mở cài đặt Đảo hệ thống trên máy", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                val savedUid = remember { LicenseManager.getUserUid(context) }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Thiết bị đã cấp phép (UID)", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("UID: $savedUid • Tác giả: digitalxiao", style = MaterialTheme.typography.bodySmall) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF30D158).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF30D158))
                            }
                        },
                        trailingContent = {
                            TextButton(onClick = onDeactivateLicense) {
                                Text("Kiểm tra lại", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionTileCard(
    title: String,
    statusText: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isGranted) Color(0xFF30D158) else Color(0xFFFF453A)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = modifier.height(82.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                Text(
                    text = statusText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class ActivityItemSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun LiquidActivitiesTab(
    settings: IslandSettings,
    strings: AppStrings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onOpenMediaPlayerCustomization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVi = settings.appLanguage.lowercase() == "vi"

    val activities = remember(strings, settings.isGlass) {
        if (settings.isGlass) {
            listOf(
                ActivityItemSpec("PLAYER", strings.actPlayer, Icons.Default.MusicNote, Color(0xFF0A84FF)),
                ActivityItemSpec("ALARM", strings.actAlarm, Icons.Default.Alarm, Color(0xFFFF9F0A)),
                ActivityItemSpec("CALL", strings.actCall, Icons.Default.Call, Color(0xFF30D158)),
                ActivityItemSpec("VOICE_MEMO", strings.actVoiceMemo, Icons.Default.Mic, Color(0xFFFF453A)),
                ActivityItemSpec("SCREEN_RECORDER", strings.actScreenRecorder, Icons.Default.FiberManualRecord, Color(0xFFFF453A)),
                ActivityItemSpec("CHARGING", strings.actCharging, Icons.Default.Bolt, Color(0xFF30D158)),
                ActivityItemSpec("FLASHLIGHT", strings.actFlashlight, Icons.Default.FlashlightOn, Color(0xFFFF9F0A))
            )
        } else {
            listOf(
                ActivityItemSpec("PLAYER", strings.actPlayer, Icons.Default.MusicNote, Color(0xFF0A84FF)),
                ActivityItemSpec("CALL", strings.actCall, Icons.Default.Call, Color(0xFF30D158)),
                ActivityItemSpec("VOICE_MEMO", strings.actVoiceMemo, Icons.Default.Mic, Color(0xFFFF453A)),
                ActivityItemSpec("SCREEN_RECORDER", strings.actScreenRecorder, Icons.Default.FiberManualRecord, Color(0xFFFF453A)),
                ActivityItemSpec("FLASHLIGHT", strings.actFlashlight, Icons.Default.FlashlightOn, Color(0xFFFF9F0A)),
                ActivityItemSpec("CHARGING", strings.actCharging, Icons.Default.Bolt, Color(0xFF30D158)),
                ActivityItemSpec("ALARM", strings.actAlarm, Icons.Default.Alarm, Color(0xFFFF9F0A)),
                ActivityItemSpec("WEATHER", strings.actWeather, Icons.Default.WbSunny, Color(0xFF64D2FF)),
                ActivityItemSpec("FPS", strings.actFps, Icons.Default.Speed, Color(0xFF30D158)),
                ActivityItemSpec("NETWORK", strings.actNetwork, Icons.Default.Wifi, Color(0xFF64D2FF))
            )
        }
    }

    var selectedActivityKey by remember { mutableStateOf("PLAYER") }
    var isPreviewExpanded by remember { mutableStateOf(false) }

    var expandEnableCard by remember { mutableStateOf(true) }
    var expandGeometryCard by remember { mutableStateOf(true) }
    var expandFeaturesCard by remember { mutableStateOf(false) }

    val currentSpec = activities.firstOrNull { it.key == selectedActivityKey } ?: activities.first()
    val accentColor by animateColorAsState(targetValue = currentSpec.color, label = "actAccent")

    val isActivityEnabled = when (selectedActivityKey) {
        "PLAYER" -> settings.showMedia
        "CALL" -> settings.showCalls
        "VOICE_MEMO" -> settings.showVoiceMemo
        "SCREEN_RECORDER" -> settings.showScreenRecording
        "NOTIFICATIONS" -> settings.showNotifications
        "FLASHLIGHT" -> settings.showFlashlight
        "CHARGING" -> settings.showCharging
        "ALARM" -> settings.showAlarm
        "WEATHER" -> settings.showWeather
        "FPS" -> settings.showFps
        "NETWORK" -> settings.showNetworkSpeed
        else -> true
    }

    fun setActivityEnabled(enabled: Boolean) {
        onUpdateSettings { s ->
            when (selectedActivityKey) {
                "PLAYER" -> s.copy(showMedia = enabled)
                "CALL" -> s.copy(showCalls = enabled)
                "VOICE_MEMO" -> s.copy(showVoiceMemo = enabled)
                "SCREEN_RECORDER" -> s.copy(showScreenRecording = enabled)
                "NOTIFICATIONS" -> s.copy(showNotifications = enabled)
                "FLASHLIGHT" -> s.copy(showFlashlight = enabled)
                "CHARGING" -> s.copy(showCharging = enabled)
                "ALARM" -> s.copy(showAlarm = enabled)
                "WEATHER" -> s.copy(showWeather = enabled)
                "FPS" -> s.copy(showFps = enabled)
                "NETWORK" -> s.copy(showNetworkSpeed = enabled)
                else -> s
            }
        }
    }

    val voiceMemoState by IslandStateManager.voiceMemoState.collectAsState()
    val activeSliderId by IslandStateManager.activeSliderId.collectAsState()
    val isDraggingAnySlider = activeSliderId != null

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Dark Live Preview Container
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF14151A),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .graphicsLayer { alpha = if (isDraggingAnySlider) 0f else 1f }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF30D158))
                            )
                            Text(
                                text = strings.livePreview,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = if (isPreviewExpanded) strings.expanded else strings.compact,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Inner black phone forehead preview box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(138.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF050507))
                            .clickable { isPreviewExpanded = !isPreviewExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        ActivityLivePreviewPill(
                            activityKey = selectedActivityKey,
                            isExpanded = isPreviewExpanded,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }

        // 2. Compact / Expanded Segmented Toggle Bar
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF14151A),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .graphicsLayer { alpha = if (isDraggingAnySlider) 0f else 1f }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (!isPreviewExpanded) accentColor else Color.Transparent)
                            .clickable { isPreviewExpanded = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropFree,
                                contentDescription = null,
                                tint = if (!isPreviewExpanded) Color.White else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.compact,
                                color = if (!isPreviewExpanded) Color.White else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isPreviewExpanded) accentColor else Color.Transparent)
                            .clickable { isPreviewExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = null,
                                tint = if (isPreviewExpanded) Color.White else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.expanded,
                                color = if (isPreviewExpanded) Color.White else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Horizontal Row of Activity Selector Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (isDraggingAnySlider) 0f else 1f }
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activities.forEach { spec ->
                    val isSelected = spec.key == selectedActivityKey
                    Surface(
                        onClick = { selectedActivityKey = spec.key },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) spec.color.copy(alpha = 0.16f) else Color(0xFF14151A),
                        modifier = Modifier
                            .width(86.dp)
                            .height(94.dp)
                            .then(
                                if (isSelected) Modifier.border(1.8.dp, spec.color, RoundedCornerShape(20.dp))
                                else Modifier
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) spec.color.copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.06f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = spec.label,
                                    tint = if (isSelected) spec.color else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = spec.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) spec.color else Color.White.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Enable Activity Accordion
        item {
            ExpandableLiquidCard(
                title = strings.enableActivity,
                icon = Icons.Default.PowerSettingsNew,
                iconTint = accentColor,
                expanded = expandEnableCard,
                onToggle = { expandEnableCard = !expandEnableCard }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentSpec.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Switch(
                        checked = isActivityEnabled,
                        onCheckedChange = { setActivityEnabled(it) }
                    )
                }

                // Real trigger button so user can test/launch the selected activity right on the real Dynamic Island
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedActivityKey == "VOICE_MEMO") {
                        Button(
                            onClick = { VoiceMemoManager.toggleRecording(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (voiceMemoState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (voiceMemoState.isRecording) {
                                    if (isVi) "Dừng Ghi Âm Trên Đảo Động" else "Stop Voice Memo on Island"
                                } else {
                                    if (isVi) "Bắt Đầu Ghi Âm Thật Trên Đảo" else "Start Real Voice Memo on Island"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                when (selectedActivityKey) {
                                    "PLAYER" -> IslandStateManager.updateMedia(
                                        MediaInfo(
                                            packageName = "com.spotify.music",
                                            title = "Starboy",
                                            artist = "The Weeknd",
                                            isPlaying = true,
                                            durationMs = 230000L,
                                            positionMs = 84000L
                                        )
                                    )
                                    "CALL" -> IslandStateManager.updateCallState(
                                        ActiveCallState(isActive = !IslandStateManager.isCallActive, callerName = "Alex Johnson", durationSeconds = 76)
                                    )
                                    "SCREEN_RECORDER" -> IslandStateManager.updateRecordingState(
                                        ScreenRecordingState(isRecording = !IslandStateManager.isRecordingActive, durationSeconds = 48)
                                    )
                                    "CHARGING" -> IslandStateManager.triggerChargingEvent(88, 33.0)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isVi) "Xem thử hoạt động này trên Đảo thật" else "Test Activity on Live Island",
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 5. Geometry & Sizes Accordion (matching 00:22 - 00:28)
        item {
            ExpandableLiquidCard(
                title = strings.geometryAndSizes,
                icon = Icons.Default.CropFree,
                iconTint = accentColor,
                expanded = expandGeometryCard,
                onToggle = { expandGeometryCard = !expandGeometryCard }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.quickPresets,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.clickable {
                            onUpdateSettings {
                                it.copy(
                                    activityExpandedWidthDp = 300,
                                    activityExpandedHeightDp = 80,
                                    activityCompactWidthDp = 170,
                                    activityCompactHeightDp = 38,
                                    widthDp = 170,
                                    heightDp = 38
                                )
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                        Text(strings.reset, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // 4 Preset Chips (Default, 300 dp, 340 dp, 360 dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetWidths = listOf(0 to strings.presetDefault, 300 to "300 dp", 340 to "340 dp", 360 to "360 dp")
                    presetWidths.forEach { (wVal, label) ->
                        val selected = (wVal == 0 && settings.activityExpandedWidthDp == 0) || (wVal == settings.activityExpandedWidthDp)
                        Surface(
                            onClick = {
                                onUpdateSettings {
                                    it.copy(
                                        activityExpandedWidthDp = if (wVal == 0) 300 else wVal,
                                        expandedScale = if (wVal <= 300) 1.0f else if (wVal == 340) 1.1f else 1.2f
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .then(
                                    if (selected) Modifier.border(1.2.dp, accentColor, RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                LiquidSliderRow(
                    label = strings.expandedWidth,
                    value = settings.activityExpandedWidthDp.toFloat(),
                    valueRange = 220f..380f,
                    valueText = "${settings.activityExpandedWidthDp} dp",
                    showValue = settings.showSliderValues,
                    accentColor = accentColor,
                    onValueChange = { v ->
                        onUpdateSettings {
                            it.copy(
                                activityExpandedWidthDp = v.roundToInt(),
                                expandedScale = (v / 310f).coerceIn(0.8f, 1.3f)
                            )
                        }
                    }
                )

                LiquidSliderRow(
                    label = strings.expandedHeight,
                    value = settings.activityExpandedHeightDp.toFloat(),
                    valueRange = 60f..210f,
                    valueText = "${settings.activityExpandedHeightDp} dp",
                    showValue = settings.showSliderValues,
                    accentColor = accentColor,
                    onValueChange = { v ->
                        onUpdateSettings { it.copy(activityExpandedHeightDp = v.roundToInt()) }
                    }
                )

                LiquidSliderRow(
                    label = strings.compactWidth,
                    value = settings.activityCompactWidthDp.toFloat(),
                    valueRange = 80f..240f,
                    valueText = "${settings.activityCompactWidthDp} dp",
                    showValue = settings.showSliderValues,
                    accentColor = accentColor,
                    onValueChange = { v ->
                        val w = v.roundToInt()
                        onUpdateSettings { it.copy(activityCompactWidthDp = w, widthDp = w) }
                    }
                )

                LiquidSliderRow(
                    label = strings.compactHeight,
                    value = settings.activityCompactHeightDp.toFloat(),
                    valueRange = 24f..56f,
                    valueText = "${settings.activityCompactHeightDp} dp",
                    showValue = settings.showSliderValues,
                    accentColor = accentColor,
                    onValueChange = { v ->
                        val h = v.roundToInt()
                        onUpdateSettings { it.copy(activityCompactHeightDp = h, heightDp = h) }
                    }
                )
            }
        }

        // 6. Activity Features Accordion
        item {
            ExpandableLiquidCard(
                title = strings.activityFeatures,
                icon = Icons.Default.Tune,
                iconTint = accentColor,
                expanded = expandFeaturesCard,
                onToggle = { expandFeaturesCard = !expandFeaturesCard }
            ) {
                Text(
                    text = if (isVi) "Các tùy chỉnh chuyên sâu cho ${currentSpec.label}." else "Specific settings applied for ${currentSpec.label}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedActivityKey == "PLAYER") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isVi) "Sóng nhạc theo màu ảnh bìa" else "Ambient Album Wave", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = settings.mediaWaveAnimation,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(mediaWaveAnimation = v) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isVi) "Làm mờ ảnh bìa làm nền" else "Blurred Album Art Background", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = settings.blurredAlbumArtBackground,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(blurredAlbumArtBackground = v) } }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isVi) "Ẩn nhạc khi tạm dừng" else "Dismiss Media on Pause", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = settings.dismissMediaOnPause,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(dismissMediaOnPause = v) } }
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenMediaPlayerCustomization,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isVi) "Mở Trình tùy chỉnh Bố cục Nhạc đầy đủ" else "Open Full Media Layout Customizer",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (selectedActivityKey == "NOTIFICATIONS") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isVi) "Tự động ẩn thông báo" else "Auto-Dismiss Notifications", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = settings.autoDismissNotifications,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(autoDismissNotifications = v) } }
                        )
                    }
                    LiquidSliderRow(
                        label = if (isVi) "Số dòng tối đa" else "Notification Max Lines",
                        value = settings.notificationMaxLines.toFloat(),
                        valueRange = 1f..6f,
                        valueText = "${settings.notificationMaxLines}",
                        showValue = settings.showSliderValues,
                        accentColor = accentColor,
                        onValueChange = { v -> onUpdateSettings { it.copy(notificationMaxLines = v.roundToInt()) } }
                    )
                }
            }
        }

        // 7. Bottom Reset Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        setActivityEnabled(true)
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset ${currentSpec.label}",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = {
                        onUpdateSettings {
                            it.copy(
                                activityExpandedWidthDp = 300,
                                activityExpandedHeightDp = 80,
                                activityCompactWidthDp = 170,
                                activityCompactHeightDp = 38,
                                widthDp = 124,
                                heightDp = 38,
                                expandedScale = 1.1f
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.resetAllSizes,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLivePreviewPill(
    activityKey: String,
    isExpanded: Boolean,
    accentColor: Color
) {
    val pillW by animateDpAsState(
        targetValue = if (isExpanded) 288.dp else 168.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "prevPillW"
    )
    val pillH by animateDpAsState(
        targetValue = if (isExpanded) {
            if (activityKey == "PLAYER") 116.dp else 72.dp
        } else 38.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "prevPillH"
    )

    Box(
        modifier = Modifier
            .width(pillW)
            .height(pillH)
            .clip(RoundedCornerShape(if (isExpanded) 28.dp else 22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF14151B),
                        accentColor.copy(alpha = if (isExpanded) 0.22f else 0.08f)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(if (isExpanded) 28.dp else 22.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded) {
            // Compact Preview (matching 00:06, 00:10, 00:13, 00:17)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (activityKey) {
                    "PLAYER" -> {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF2D55)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        MiniEqualizerBars(color = Color(0xFFFF2D55))
                    }
                    "CALL" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(15.dp))
                            Text("1:15", color = Color(0xFF30D158), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        MiniEqualizerBars(color = Color(0xFF30D158))
                    }
                    "VOICE_MEMO" -> {
                        MiniEqualizerBars(color = Color(0xFFFF453A), barCount = 9)
                        Text("00:42", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    "SCREEN_RECORDER" -> {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF453A))
                        )
                        Text("00:48", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    else -> {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        Text(activityKey, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        } else {
            // Expanded Preview (matching 00:08, 00:11, 00:14, 00:15)
            when (activityKey) {
                "PLAYER" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFF2D55)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text("Starboy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    Text("The Weeknd", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                                }
                            }
                            MiniEqualizerBars(color = Color(0xFFFF2D55))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("1:24", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.45f)
                                        .background(Color.White)
                                )
                            }
                            Text("-2:26", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Icon(Icons.Default.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                "CALL" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("1:16", color = Color(0xFF30D158), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Alex Johnson", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF453A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                "VOICE_MEMO" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniEqualizerBars(color = Color(0xFFFF453A), barCount = 12)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("00:42", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF453A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF453A).copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("Screen Recorder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text("00:48", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        MiniEqualizerBars(color = Color(0xFFFF453A))
                    }
                }
            }
        }
    }
}

@Composable
fun MiniEqualizerBars(
    color: Color,
    barCount: Int = 5
) {
    val transition = rememberInfiniteTransition(label = "mini_eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_p"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        for (i in 0 until barCount) {
            val factor = 0.35f + 0.65f * abs(sin(phase + i * 0.7f))
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((5.dp + 11.dp * factor))
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun LiquidShortcutsTab(
    settings: IslandSettings,
    strings: AppStrings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onOpenChainedTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVi = settings.appLanguage.lowercase() == "vi"
    var editingGestureTarget by remember { mutableStateOf<String?>(null) }

    val gestureActionLabels = remember(isVi) {
        mapOf(
            "VOLUME_DOWN" to (if (isVi) "Giảm âm lượng (Volume Down)" else "Volume Down"),
            "VOLUME_UP" to (if (isVi) "Tăng âm lượng (Volume Up)" else "Volume Up"),
            "DISMISS" to (if (isVi) "Ẩn thông báo / Thu gọn" else "Dismiss Notification"),
            "NOTIFICATION_SHADE" to (if (isVi) "Mở thanh thông báo" else "Notification Shade"),
            "QUICK_SETTINGS" to (if (isVi) "Mở Cài đặt nhanh" else "Quick Settings"),
            "RECENT_APPS" to (if (isVi) "Đa nhiệm (Recent Apps)" else "Recent Apps"),
            "FLASHLIGHT" to (if (isVi) "Bật / Tắt Đèn pin" else "Flashlight"),
            "SCREENSHOT" to (if (isVi) "Chụp màn hình" else "Screenshot"),
            "PLAY_PAUSE" to (if (isVi) "Phát / Tạm dừng nhạc" else "Play / Pause Media"),
            "VOICE_MEMO" to (if (isVi) "Ghi âm giọng nói" else "Voice Memo"),
            "EXPAND" to (if (isVi) "Mở rộng Đảo động" else "Expand Island"),
            "NONE" to (if (isVi) "Không làm gì" else "None")
        )
    }

    val transition = rememberInfiniteTransition(label = "swipe_preview")
    val swipeProgress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipe_p"
    )
    val isSwipingRight = swipeProgress > 0f

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Animated Swipe Gesture Preview Card (matching 00:29 - 00:34)
        item {
            val stripColor = if (isSwipingRight) Color(0xFF30D158) else Color(0xFFFF9F0A)
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 18.dp)
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(stripColor)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        // Black Pill with animated glowing touch dot
                        Box(
                            modifier = Modifier
                                .width(190.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width * (0.5f + swipeProgress * 0.32f)
                                val cy = size.height / 2f
                                val trailStartX = size.width * 0.5f
                                drawLine(
                                    color = stripColor.copy(alpha = 0.55f),
                                    start = Offset(trailStartX, cy),
                                    end = Offset(cx, cy),
                                    strokeWidth = 6f
                                )
                                drawCircle(
                                    color = stripColor.copy(alpha = 0.35f),
                                    radius = 34f,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 10f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }

                        Text(
                            text = if (isSwipingRight) strings.swipeRightGesture else strings.swipeLeftGesture,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Gesture Control Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF9F0A).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(18.dp))
                        }
                        Text(strings.gestureControl, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    Column {
                        Text(strings.islandGestures, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Switch(
                            checked = settings.islandGesturesEnabled,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(islandGesturesEnabled = v) } }
                        )
                    }

                    Column {
                        Text(strings.onlyWhenEmpty, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Switch(
                            checked = settings.gesturesOnlyWhenEmpty,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(gesturesOnlyWhenEmpty = v) } }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.adaptiveGestures, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                            Switch(
                                checked = settings.adaptiveGesturesEnabled,
                                onCheckedChange = { v -> onUpdateSettings { it.copy(adaptiveGesturesEnabled = v) } }
                            )
                        }
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFFF9F0A))
                    }
                }
            }
        }

        // 3. Horizontal Swipes Card
        item {
            GestureSectionCard(
                title = strings.horizontalSwipes,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                iconTint = Color(0xFFBF5AF2)
            ) {
                GestureActionRow(
                    title = strings.swipeLeft,
                    subtitle = null,
                    actionLabel = gestureActionLabels[settings.swipeLeftAction] ?: settings.swipeLeftAction,
                    leadingIcon = Icons.Default.Menu,
                    onClick = { editingGestureTarget = "SWIPE_LEFT" }
                )
                GestureActionRow(
                    title = strings.swipeRight,
                    subtitle = null,
                    actionLabel = gestureActionLabels[settings.swipeRightAction] ?: settings.swipeRightAction,
                    leadingIcon = Icons.Default.Add,
                    onClick = { editingGestureTarget = "SWIPE_RIGHT" }
                )
            }
        }

        // 4. Vertical Swipes Card
        item {
            GestureSectionCard(
                title = strings.verticalSwipes,
                icon = Icons.Default.KeyboardArrowUp,
                iconTint = Color(0xFFBF5AF2)
            ) {
                GestureActionRow(
                    title = strings.swipeUp,
                    subtitle = strings.swipeUpSub,
                    actionLabel = gestureActionLabels[settings.swipeUpAction] ?: settings.swipeUpAction,
                    leadingIcon = Icons.Default.Notifications,
                    onClick = { editingGestureTarget = "SWIPE_UP" }
                )
                GestureActionRow(
                    title = strings.swipeDown,
                    subtitle = strings.swipeDownSub,
                    actionLabel = gestureActionLabels[settings.swipeDownAction] ?: settings.swipeDownAction,
                    leadingIcon = Icons.Default.Notifications,
                    onClick = { editingGestureTarget = "SWIPE_DOWN" }
                )
            }
        }

        // 5. Tap Actions Card
        item {
            GestureSectionCard(
                title = strings.tapActions,
                icon = Icons.Default.Star,
                iconTint = Color(0xFF0A84FF)
            ) {
                GestureActionRow(
                    title = strings.doubleTap,
                    subtitle = null,
                    actionLabel = gestureActionLabels[settings.doubleTapAction] ?: settings.doubleTapAction,
                    leadingIcon = Icons.Default.Menu,
                    onClick = { editingGestureTarget = "DOUBLE_TAP" }
                )
                GestureActionRow(
                    title = strings.longPress,
                    subtitle = null,
                    actionLabel = gestureActionLabels[settings.longPressAction] ?: settings.longPressAction,
                    leadingIcon = Icons.Default.Star,
                    onClick = { editingGestureTarget = "LONG_PRESS" }
                )
            }
        }

        // 6. Chained Action Shortcuts (Preserved Tasks Feature)
        item {
            Surface(
                onClick = onOpenChainedTasks,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(strings.chainedTasksTitle, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(strings.chainedTasksSub, style = MaterialTheme.typography.bodySmall) },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // 7. Bottom Info Banner
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = strings.gestureHintBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (editingGestureTarget != null) {
        val target = editingGestureTarget!!
        AlertDialog(
            onDismissRequest = { editingGestureTarget = null },
            title = { Text(if (isVi) "Chọn hành động cử chỉ" else "Select Gesture Action", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    gestureActionLabels.forEach { (actionKey, label) ->
                        Surface(
                            onClick = {
                                onUpdateSettings { s ->
                                    when (target) {
                                        "SWIPE_LEFT" -> s.copy(swipeLeftAction = actionKey)
                                        "SWIPE_RIGHT" -> s.copy(swipeRightAction = actionKey)
                                        "SWIPE_UP" -> s.copy(swipeUpAction = actionKey)
                                        "SWIPE_DOWN" -> s.copy(swipeDownAction = actionKey)
                                        "DOUBLE_TAP" -> s.copy(doubleTapAction = actionKey)
                                        "LONG_PRESS" -> s.copy(longPressAction = actionKey)
                                        else -> s
                                    }
                                }
                                editingGestureTarget = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingGestureTarget = null }) {
                    Text(if (isVi) "Đóng" else "Close")
                }
            }
        )
    }
}

@Composable
private fun GestureSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            content()
        }
    }
}

@Composable
private fun GestureActionRow(
    title: String,
    subtitle: String?,
    actionLabel: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = actionLabel,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(86.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}



@Composable
private fun LiquidIosDynamicMotionSettingsSection(
    settings: IslandSettings,
    batteryPct: Int,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit
) {
    var previewAuthTick by remember { mutableIntStateOf(0) }
    val activeSliderId by IslandStateManager.activeSliderId.collectAsState()
    val isDraggingAnySlider = activeSliderId != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isDraggingAnySlider) 0f else 1f },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. LOCK SCREEN AUTH (Face ID | Fingerprint + Black Pill Preview)
        Text(
            text = "— LOCK SCREEN AUTH",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B1120),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lock Screen Auth",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF060913))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isFaceId = settings.lockScreenAuthMode.uppercase() != "FINGERPRINT"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isFaceId) Color(0xFF2563EB) else Color.Transparent)
                            .clickable {
                                onUpdateSettings { it.copy(lockScreenAuthMode = "FACE_ID") }
                                previewAuthTick++
                                IslandStateManager.triggerUnlockEvent()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Face ID",
                            color = if (isFaceId) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isFaceId) Color(0xFF2563EB) else Color.Transparent)
                            .clickable {
                                onUpdateSettings { it.copy(lockScreenAuthMode = "FINGERPRINT") }
                                previewAuthTick++
                                IslandStateManager.triggerUnlockEvent()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Fingerprint",
                            color = if (!isFaceId) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.runtime.key(settings.lockScreenAuthMode, previewAuthTick) {
                        Box(
                            modifier = Modifier
                                .width(188.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(Color.Black)
                                .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(19.dp))
                                .clickable {
                                    previewAuthTick++
                                    IslandStateManager.triggerUnlockEvent()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LiquidIosLockAuthPill(
                                authMode = settings.lockScreenAuthMode,
                                contentAlpha = 1f
                            )
                        }
                    }
                }
            }
        }

        // 2. MEDIA STYLE — Album Gradient
        Text(
            text = "— MEDIA STYLE",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B1120),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Album Gradient",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp
                    )
                    Text(
                        text = "Đổ màu nền theo ảnh bìa bài nhạc khi mở rộng thẻ phát nhạc",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = settings.blurredAlbumArtBackground,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(blurredAlbumArtBackground = enabled) }
                    }
                )
            }
        }

        // 3. REAL-TIME SYSTEM DETECTION MODULES (No video timestamps or fake demo triggers)
        Text(
            text = "— ĐỒNG BỘ HOẠT ĐỘNG HỆ THỐNG THỜI GIAN THỰC",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động nhận diện Bấm giờ & Hẹn giờ", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Text("Tự động hiển thị bộ đếm trực tiếp khi bật Bấm giờ hoặc Hẹn giờ trong ứng dụng Đồng hồ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.showAlarm,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(showAlarm = v) } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động nhận diện Điều hướng & Tiến trình", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Text("Hiển thị khoảng cách chỉ đường và tiến trình tải xuống trực tiếp trên Đảo động", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.showLiveProgress,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(showLiveProgress = v) } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động nhận diện Chuông / Rung / Im lặng & Sạc pin", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Text("Phản hồi tức thì khi gạt phím âm lượng chế độ chuông hoặc cắm sạc nhanh", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.showRingerMode && settings.showCharging,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(showRingerMode = v, showCharging = v) } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Làm mờ chuyển động quang học", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Text("Hiệu ứng làm mờ theo vận tốc khi Đảo động phóng to lên thanh trạng thái hoặc thu gọn", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.motionBlur,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(motionBlur = v) } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tách đôi viên thuốc đa nhiệm", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Text("Tách bong bóng tròn bên phải với lực hút chất lỏng khi có 2 hoạt động chạy song song", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.showDualActivityInCompact,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(showDualActivityInCompact = v) } }
                    )
                }
            }
        }
    }
}
