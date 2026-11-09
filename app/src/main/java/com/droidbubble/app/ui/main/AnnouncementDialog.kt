package com.droidbubble.app.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.util.DynamicFeatureEngine
import com.droidbubble.app.util.LicenseManager
import com.droidbubble.app.util.OnlineAppConfig
import com.droidbubble.app.util.OnlineConfigManager
import com.droidbubble.app.util.UpdateManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "androidisland_announcement_prefs"
private const val KEY_LAST_DISMISSED_VERSION = "last_dismissed_announcement_version"
const val TIKTOK_VIDEO_URL = "https://vt.tiktok.com/ZSpBjvVcP/"

object AnnouncementManager {
    fun shouldShow(context: Context): Boolean {
        val cached = OnlineConfigManager.getCachedConfig(context)
        if (!cached.showAnnouncement) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_DISMISSED_VERSION, null) != cached.announcementId
    }

    fun markDismissed(context: Context) {
        val cached = OnlineConfigManager.getCachedConfig(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_DISMISSED_VERSION, cached.announcementId)
            .apply()
    }
}

@Composable
fun VideoAnnouncementDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(OnlineConfigManager.getCachedConfig(context)) }
    var showAdminEditor by remember { mutableStateOf(false) }
    var isDownloadingApk by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val remote = OnlineConfigManager.fetchOnlineConfig(context)
        config = remote
        DynamicFeatureEngine.applyDynamicFeatures(context, remote)
    }

    if (showAdminEditor) {
        OnlineAdminConfigDialog(
            initialConfig = config,
            onDismiss = { showAdminEditor = false },
            onSaved = { updated ->
                config = updated
                showAdminEditor = false
            }
        )
        return
    }

    val currentVer = remember { UpdateManager.getCurrentVersion(context) }
    val hasNewerApkOnline = remember(config.latestVersion, currentVer) {
        UpdateManager.isNewerVersion(config.latestVersion, currentVer) && config.apkDownloadUrl.startsWith("http")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { showAdminEditor = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = config.announcementTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = config.announcementSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = config.announcementBannerTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = config.announcementBannerDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Bảng cập nhật trực tiếp trong ứng dụng (cả OTA tính năng lẫn cài đặt nhanh APK)
                if (hasNewerApkOnline && config.apkDownloadUrl.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDownloadingApk) {
                                isDownloadingApk = true
                                downloadProgress = 0f
                                scope.launch {
                                    val apkFile = UpdateManager.downloadApk(context, config.apkDownloadUrl) { p ->
                                        downloadProgress = p
                                    }
                                    isDownloadingApk = false
                                    if (apkFile != null) {
                                        UpdateManager.installApk(context, apkFile)
                                    } else {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.apkDownloadUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        runCatching { context.startActivity(intent) }
                                    }
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isDownloadingApk) {
                                            "Đang tải cập nhật v${config.latestVersion} (${(downloadProgress * 100).toInt()}%)"
                                        } else {
                                            "Cập nhật trực tiếp trong App: v${config.latestVersion}"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = config.updateNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isDownloadingApk) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            if (isDownloadingApk) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Nút đồng bộ bản vá tính năng & sửa lỗi trực tuyến ngay lập tức (không cần tải lại app)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                val remote = OnlineConfigManager.fetchOnlineConfig(context)
                                config = remote
                                DynamicFeatureEngine.applyDynamicFeatures(context, remote, forceApply = true)
                                Toast.makeText(
                                    context,
                                    "Đã đồng bộ tính năng và bản vá lỗi Online (Bản vá #${remote.patchVersion})!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đồng bộ Tính năng & Sửa lỗi Online (OTA)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Nhấn để áp dụng ngay bản vá tính năng và kích thước Đảo mới nhất mà không cần cài lại ứng dụng (Patch #${config.patchVersion})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Chi tiết cập nhật mới:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                config.changelogItems.forEach { itemText ->
                    val parts = itemText.split(":", limit = 2)
                    if (parts.size == 2) {
                        ChangelogItem(title = parts[0].trim(), desc = parts[1].trim())
                    } else {
                        ChangelogItem(title = "Điểm mới", desc = itemText)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val url = config.tiktokVideoUrl.ifBlank { TIKTOK_VIDEO_URL }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(config.tiktokButtonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun OnlineAdminConfigDialog(
    initialConfig: OnlineAppConfig,
    onDismiss: () -> Unit,
    onSaved: (OnlineAppConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 0 = Thiết bị & UID, 1 = Báo lỗi, 2 = OTA Tính năng & Fix lỗi, 3 = Thông báo & APK
    var selectedTab by remember { mutableIntStateOf(0) }

    var announcementId by remember { mutableStateOf(initialConfig.announcementId) }
    var showAnnouncement by remember { mutableStateOf(initialConfig.showAnnouncement) }
    var title by remember { mutableStateOf(initialConfig.announcementTitle) }
    var subtitle by remember { mutableStateOf(initialConfig.announcementSubtitle) }
    var bannerTitle by remember { mutableStateOf(initialConfig.announcementBannerTitle) }
    var bannerDesc by remember { mutableStateOf(initialConfig.announcementBannerDesc) }
    var changelogText by remember { mutableStateOf(initialConfig.changelogItems.joinToString("\n")) }
    var tiktokUrl by remember { mutableStateOf(initialConfig.tiktokVideoUrl) }
    var tiktokBtn by remember { mutableStateOf(initialConfig.tiktokButtonText) }
    var latestVersion by remember { mutableStateOf(initialConfig.latestVersion) }
    var apkDownloadUrl by remember { mutableStateOf(initialConfig.apkDownloadUrl) }
    var updateNotes by remember { mutableStateOf(initialConfig.updateNotes) }
    var allowAllDevices by remember { mutableStateOf(initialConfig.allowAllDevices) }
    var allowedUidsText by remember { mutableStateOf(initialConfig.allowedUids.joinToString("\n")) }
    var customServerUrl by remember { mutableStateOf(OnlineConfigManager.getCustomConfigUrl(context)) }
    var isPublishing by remember { mutableStateOf(false) }

    // OTA Live Feature & BugFix fields
    var patchVersion by remember { mutableStateOf(initialConfig.patchVersion.toString()) }
    var remoteToastMessage by remember { mutableStateOf(initialConfig.remoteToastMessage) }
    val featureSwitches = remember {
        mutableStateMapOf<String, Boolean>().apply {
            put("alwaysShowPill", initialConfig.onlineFeatureSwitches["alwaysShowPill"] ?: true)
            put("hideNativeOsIsland", initialConfig.onlineFeatureSwitches["hideNativeOsIsland"] ?: true)
            put("bubbleEffectEnabled", initialConfig.onlineFeatureSwitches["bubbleEffectEnabled"] ?: true)
            put("showCalls", initialConfig.onlineFeatureSwitches["showCalls"] ?: true)
            put("showMedia", initialConfig.onlineFeatureSwitches["showMedia"] ?: true)
            put("showNotifications", initialConfig.onlineFeatureSwitches["showNotifications"] ?: true)
            put("showCharging", initialConfig.onlineFeatureSwitches["showCharging"] ?: true)
            put("showFlashlight", initialConfig.onlineFeatureSwitches["showFlashlight"] ?: true)
            put("showFps", initialConfig.onlineFeatureSwitches["showFps"] ?: false)
            put("showNetworkSpeed", initialConfig.onlineFeatureSwitches["showNetworkSpeed"] ?: false)
        }
    }
    var otaWidthDp by remember { mutableStateOf(initialConfig.onlineDimensionOverrides["widthDp"]?.toString() ?: "124") }
    var otaHeightDp by remember { mutableStateOf(initialConfig.onlineDimensionOverrides["heightDp"]?.toString() ?: "38") }
    var otaTopOffsetDp by remember { mutableStateOf(initialConfig.onlineDimensionOverrides["topOffsetDp"]?.toString() ?: "12") }
    var otaCornerRadiusDp by remember { mutableStateOf(initialConfig.onlineDimensionOverrides["cornerRadiusDp"]?.toString() ?: "22") }

    // Live Firestore UID & Bug Report state
    var firestoreDevices by remember { mutableStateOf<List<LicenseManager.DeviceUidRecord>>(emptyList()) }
    var bugReports by remember { mutableStateOf<List<LicenseManager.BugReportRecord>>(emptyList()) }
    var isLoadingDevices by remember { mutableStateOf(false) }
    var isLoadingBugs by remember { mutableStateOf(false) }
    var manualUidInput by remember { mutableStateOf("") }

    val approvedCount = remember(firestoreDevices) {
        firestoreDevices.count { it.approved || it.status == "approved" }
    }
    val accessibilityActiveCount = remember(firestoreDevices) {
        firestoreDevices.count { it.accessibilityActive }
    }
    val pendingCount = remember(firestoreDevices) {
        firestoreDevices.count { !it.approved && it.status != "approved" && it.status != "blocked" }
    }

    fun refreshAdminData() {
        isLoadingDevices = true
        isLoadingBugs = true
        scope.launch {
            firestoreDevices = LicenseManager.fetchAllDeviceUidsFromFirestore()
            isLoadingDevices = false
        }
        scope.launch {
            bugReports = LicenseManager.fetchAllBugReportsFromFirestore()
            isLoadingBugs = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAdminData()
    }

    fun formatTimestamp(ms: Long): String {
        if (ms <= 0L) return "Chưa rõ"
        return runCatching {
            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ms))
        }.getOrDefault("")
    }

    fun buildCurrentConfig(): OnlineAppConfig {
        val items = changelogText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val uids = allowedUidsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val dims = mutableMapOf<String, Int>()
        otaWidthDp.toIntOrNull()?.let { dims["widthDp"] = it }
        otaHeightDp.toIntOrNull()?.let { dims["heightDp"] = it }
        otaTopOffsetDp.toIntOrNull()?.let { dims["topOffsetDp"] = it }
        otaCornerRadiusDp.toIntOrNull()?.let { dims["cornerRadiusDp"] = it }

        return OnlineAppConfig(
            announcementId = announcementId.trim().ifEmpty { "v2.5.0" },
            showAnnouncement = showAnnouncement,
            announcementTitle = title.trim(),
            announcementSubtitle = subtitle.trim(),
            announcementBannerTitle = bannerTitle.trim(),
            announcementBannerDesc = bannerDesc.trim(),
            changelogItems = if (items.isNotEmpty()) items else initialConfig.changelogItems,
            tiktokVideoUrl = tiktokUrl.trim(),
            tiktokButtonText = tiktokBtn.trim(),
            latestVersion = latestVersion.trim(),
            latestVersionCode = 25,
            apkDownloadUrl = apkDownloadUrl.trim(),
            updateNotes = updateNotes.trim(),
            forceUpdate = false,
            allowAllDevices = allowAllDevices,
            allowedUids = uids,
            patchVersion = (patchVersion.toIntOrNull() ?: initialConfig.patchVersion),
            remoteToastMessage = remoteToastMessage.trim(),
            onlineFeatureSwitches = featureSwitches.toMap(),
            onlineDimensionOverrides = dims
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Trang Quản Trị Admin", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    IconButton(onClick = { refreshAdminData() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới", modifier = Modifier.size(18.dp))
                    }
                }

                // Live Statistics Summary Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdminStatChip(
                        label = "Đã kích hoạt",
                        value = "$approvedCount",
                        color = Color(0xFF28CD41),
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatChip(
                        label = "Bật Trợ năng",
                        value = "$accessibilityActiveCount",
                        color = Color(0xFF0A84FF),
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatChip(
                        label = "Báo lỗi",
                        value = "${bugReports.size}",
                        color = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4 Admin Tabs (2x2 compact rows)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("UID (${firestoreDevices.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Báo lỗi (${bugReports.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("OTA Tính năng & Fix", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        label = { Text("Thông báo & APK", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: DUYỆT UID THIẾT BỊ & THỐNG KÊ TRỢ NĂNG
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cho phép mọi thiết bị vào thẳng", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                Text("Bỏ qua bước chờ duyệt UID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = allowAllDevices, onCheckedChange = { allowAllDevices = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = manualUidInput,
                                onValueChange = { manualUidInput = it },
                                placeholder = { Text("Dán mã uid_... cần duyệt") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val target = manualUidInput.trim()
                                    if (target.isNotEmpty()) {
                                        scope.launch {
                                            LicenseManager.setDeviceUidApproval(target, "Duyệt thủ công", true)
                                            val currentList = allowedUidsText.lines().map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                                            currentList.add(target)
                                            allowedUidsText = currentList.joinToString("\n")
                                            manualUidInput = ""
                                            Toast.makeText(context, "Đã duyệt UID: $target", Toast.LENGTH_SHORT).show()
                                            refreshAdminData()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Duyệt", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Thiết bị ($approvedCount kích hoạt • $accessibilityActiveCount bật Trợ năng • $pendingCount chờ duyệt)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isLoadingDevices) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            }
                        } else if (firestoreDevices.isEmpty()) {
                            Text(
                                text = "Chưa có thiết bị nào gửi yêu cầu trên Cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            firestoreDevices.forEach { dev ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = dev.deviceName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            val badgeColor = when {
                                                dev.approved || dev.status == "approved" -> Color(0xFF28CD41)
                                                dev.status == "blocked" -> Color(0xFFFF3B30)
                                                else -> Color(0xFFFF9500)
                                            }
                                            val badgeText = when {
                                                dev.approved || dev.status == "approved" -> "ĐÃ KÍCH HOẠT"
                                                dev.status == "blocked" -> "ĐÃ KHÓA"
                                                else -> "CHỜ DUYỆT"
                                            }
                                            Text(
                                                text = badgeText,
                                                color = badgeColor,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val a11yText = if (dev.accessibilityActive) "Trợ năng: BẬT" else "Trợ năng: TẮT"
                                            val a11yColor = if (dev.accessibilityActive) Color(0xFF28CD41) else MaterialTheme.colorScheme.onSurfaceVariant
                                            Text(
                                                text = a11yText,
                                                color = a11yColor,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp
                                            )
                                            Text(
                                                text = "v${dev.appVersion} • ${formatTimestamp(dev.lastActiveAt)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = dev.uid,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        LicenseManager.setDeviceUidApproval(dev.uid, dev.deviceName, true)
                                                        val currentList = allowedUidsText.lines().map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                                                        currentList.add(dev.uid)
                                                        allowedUidsText = currentList.joinToString("\n")
                                                        refreshAdminData()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cho phép", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        LicenseManager.setDeviceUidApproval(dev.uid, dev.deviceName, false)
                                                        val currentList = allowedUidsText.lines().map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
                                                        currentList.remove(dev.uid)
                                                        allowedUidsText = currentList.joinToString("\n")
                                                        refreshAdminData()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Khóa", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = allowedUidsText,
                            onValueChange = { allowedUidsText = it },
                            label = { Text("Danh sách UID được phép (Mỗi dòng 1 UID)") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    1 -> {
                        // TAB 1: DANH SÁCH BÁO CÁO LỖI & ĐỀ XUẤT TỪ NGƯỜI DÙNG
                        Text(
                            text = "Báo cáo lỗi & Góp ý từ người dùng (${bugReports.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isLoadingBugs) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            }
                        } else if (bugReports.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BugReport,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "Chưa có báo cáo lỗi nào.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            bugReports.forEach { report ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val isBug = report.type.uppercase() == "BUG"
                                            Text(
                                                text = if (isBug) "BÁO LỖI" else "ĐỀ XUẤT",
                                                color = if (isBug) Color(0xFFFF3B30) else Color(0xFF0A84FF),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = formatTimestamp(report.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = report.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        Text(
                                            text = report.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "${report.deviceName} • Android ${report.androidVersion} • v${report.appVersion}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${report.uid} (${if (report.accessibilityActive) "Trợ năng: BẬT" else "Trợ năng: TẮT"})",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        LicenseManager.deleteBugReportFromFirestore(report.reportId)
                                                        refreshAdminData()
                                                        Toast.makeText(context, "Đã xóa / xử lý báo cáo", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Đã xử lý", fontSize = 11.5.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: CẬP NHẬT TÍNH NĂNG & FIX LỖI TRỰC TUYẾN (OTA LIVE PATCH)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Đẩy Tính năng & Fix lỗi Trực tuyến (OTA)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Thay đổi các thông số bên dưới và bấm Đẩy lên Web để mọi máy người dùng tự động cập nhật tính năng & sửa lỗi kích thước Đảo mà không cần tải lại APK.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = patchVersion,
                                onValueChange = { patchVersion = it },
                                label = { Text("Mã bản vá (Patch #)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val next = (patchVersion.toIntOrNull() ?: 1) + 1
                                    patchVersion = next.toString()
                                },
                                modifier = Modifier.padding(top = 8.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("+1 Bản vá", fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = remoteToastMessage,
                            onValueChange = { remoteToastMessage = it },
                            label = { Text("Thông báo hiện trực tiếp trên Đảo động (Toast OTA)") },
                            placeholder = { Text("VD: Đã cập nhật bản vá lỗi Đảo động #2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Fix lỗi kích thước & vị trí Đảo động (dp):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = otaWidthDp,
                                onValueChange = { otaWidthDp = it },
                                label = { Text("Rộng (widthDp)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = otaHeightDp,
                                onValueChange = { otaHeightDp = it },
                                label = { Text("Cao (heightDp)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = otaTopOffsetDp,
                                onValueChange = { otaTopOffsetDp = it },
                                label = { Text("Lề trên (topOffsetDp)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = otaCornerRadiusDp,
                                onValueChange = { otaCornerRadiusDp = it },
                                label = { Text("Bo góc (cornerRadius)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Công tắc tính năng trực tuyến (OTA Switches):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        val switchLabels = listOf(
                            "alwaysShowPill" to "Luôn hiển thị viên thuốc đen mặc định",
                            "hideNativeOsIsland" to "Ẩn HyperIsland / Đảo gốc hệ điều hành",
                            "bubbleEffectEnabled" to "Hiệu ứng giọt nước tách đôi (Liquid Bubble)",
                            "showCalls" to "Hiển thị Cuộc gọi trên Đảo động",
                            "showMedia" to "Hiển thị Trình phát nhạc trên Đảo động",
                            "showNotifications" to "Hiển thị Thông báo trên Đảo động",
                            "showCharging" to "Hiển thị Trạng thái Sạc pin",
                            "showFlashlight" to "Hiển thị Đèn pin trên Đảo động",
                            "showFps" to "Widget đo FPS thời gian thực",
                            "showNetworkSpeed" to "Widget đo Tốc độ mạng"
                        )
                        switchLabels.forEach { (key, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = featureSwitches[key] ?: false,
                                    onCheckedChange = { featureSwitches[key] = it }
                                )
                            }
                        }
                    }

                    else -> {
                        // TAB 3: CHỈNH SỬA THÔNG BÁO & ĐẨY BẢN UPDATE APK ONLINE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hiển thị bảng thông báo khi mở App", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                            Switch(checked = showAnnouncement, onCheckedChange = { showAnnouncement = it })
                        }

                        OutlinedTextField(
                            value = announcementId,
                            onValueChange = { announcementId = it },
                            label = { Text("Mã đợt thông báo (Đổi mã để hiện lại trên mọi máy)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Tiêu đề bảng thông báo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            label = { Text("Phụ đề tác giả") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bannerTitle,
                            onValueChange = { bannerTitle = it },
                            label = { Text("Tiêu đề khung nổi bật") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bannerDesc,
                            onValueChange = { bannerDesc = it },
                            label = { Text("Nội dung khung nổi bật") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = changelogText,
                            onValueChange = { changelogText = it },
                            label = { Text("Chi tiết cập nhật (Mỗi dòng: Tiêu đề: Mô tả)") },
                            minLines = 4,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tiktokUrl,
                            onValueChange = { tiktokUrl = it },
                            label = { Text("Link Video TikTok") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tiktokBtn,
                            onValueChange = { tiktokBtn = it },
                            label = { Text("Chữ trên nút mở Video") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = latestVersion,
                            onValueChange = { latestVersion = it },
                            label = { Text("Phiên bản APK mới nhất (VD: 2.1.0)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = apkDownloadUrl,
                            onValueChange = { apkDownloadUrl = it },
                            label = { Text("Link tải trực tiếp APK cập nhật") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = updateNotes,
                            onValueChange = { updateNotes = it },
                            label = { Text("Ghi chú bản cập nhật APK") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customServerUrl,
                            onValueChange = { customServerUrl = it },
                            label = { Text("Link JSON Server riêng (Tùy chọn)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val json = OnlineConfigManager.toJsonString(buildCurrentConfig())
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("online_update.json", json))
                            Toast.makeText(context, "Đã sao chép JSON vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy JSON", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            isPublishing = true
                            val newCfg = buildCurrentConfig()
                            OnlineConfigManager.setCustomConfigUrl(context, customServerUrl)
                            OnlineConfigManager.saveLocalOverrideConfig(context, newCfg)
                            scope.launch {
                                val pushed = OnlineConfigManager.publishOnlineConfig(context, newCfg)
                                DynamicFeatureEngine.applyDynamicFeatures(context, newCfg, forceApply = true)
                                isPublishing = false
                                val msg = if (pushed) {
                                    "Đã đẩy cấu hình, tính năng OTA & danh sách UID lên Server Online!"
                                } else {
                                    "Đã áp dụng trên máy! Hãy bấm Copy JSON để dán lên GitHub."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                onSaved(newCfg)
                            }
                        },
                        enabled = !isPublishing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đẩy lên Web", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
private fun AdminStatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChangelogItem(
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF28CD41),
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
