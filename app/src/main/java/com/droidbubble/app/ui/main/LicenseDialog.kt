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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.droidbubble.app.util.LicenseManager
import com.droidbubble.app.util.OnlineConfigManager

@Composable
fun LicenseActivationScreen(
    onActivated: () -> Unit
) {
    val context = LocalContext.current
    val deviceUid = remember { LicenseManager.getUserUid(context) }
    val deviceModel = remember { LicenseManager.getDeviceModelName() }

    var statusMessage by remember {
        mutableStateOf("UID của bạn đã được gửi lên hệ thống. Vui lòng bấm Xác minh để kiểm tra.")
    }
    var isPendingStatus by remember { mutableStateOf(true) }
    var isChecking by remember { mutableStateOf(false) }
    var showAdminUnlockDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf<String?>(null) }

    // Checkbox không hiển thị lại màn xác minh ở các lần mở app sau
    var dontShowAgain by remember { mutableStateOf(LicenseManager.isSkipActivationPrompt(context)) }

    // Dialog thông báo Follow & Tim Video TikTok khi UID được xác minh thành công
    var showSocialDialog by remember { mutableStateOf(false) }
    var onlineConfig by remember { mutableStateOf(OnlineConfigManager.getCachedConfig(context)) }

    fun triggerUidCheck() {
        if (isChecking) return
        isChecking = true
        LicenseManager.checkAndActivateByUid(context) { res ->
            isChecking = false
            statusMessage = res.message
            isPendingStatus = res.status != "blocked"
            if (res.success) {
                onlineConfig = OnlineConfigManager.getCachedConfig(context)
                showSocialDialog = true
            } else {
                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Chỉ kiểm tra và gửi UID lên hệ thống 1 lần duy nhất khi mở màn hình, KHÔNG chạy vòng lặp tự động bấm nút
    LaunchedEffect(Unit) {
        val remote = OnlineConfigManager.fetchOnlineConfig(context)
        onlineConfig = remote
        LicenseManager.registerDeviceOnCloud(context)
    }

    // Hộp thoại Follow & Tim Video TikTok sau khi xác minh thành công
    if (showSocialDialog) {
        AlertDialog(
            onDismissRequest = {
                showSocialDialog = false
                onActivated()
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = onlineConfig.announcementTitle.ifBlank { "Xác minh thiết bị thành công" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = onlineConfig.announcementSubtitle.ifBlank { "Bản quyền phát triển bởi digitalxiao" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = onlineConfig.announcementBannerTitle.ifBlank { "Ủng hộ tác giả digitalxiao" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = onlineConfig.announcementBannerDesc.ifBlank { "Hãy bấm Follow kênh TikTok và thả tim video hướng dẫn để cập nhật các bản mod mới nhất!" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Tùy chọn không hỏi lại
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dontShowAgain = !dontShowAgain
                                LicenseManager.setSkipActivationPrompt(context, dontShowAgain)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = {
                                dontShowAgain = it
                                LicenseManager.setSkipActivationPrompt(context, it)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Không hiển thị lại màn hình xác minh này",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val videoUrl = onlineConfig.tiktokVideoUrl.ifBlank { "https://vt.tiktok.com/ZSpBjvVcP/" }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                        showSocialDialog = false
                        onActivated()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(onlineConfig.tiktokButtonText.ifBlank { "Mở TikTok & Thả tim" }, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSocialDialog = false
                        onActivated()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Vào ứng dụng")
                }
            }
        )
    }

    if (showAdminUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showAdminUnlockDialog = false },
            title = { Text("Mở khóa dành cho Admin", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nhập mã quản trị viên (digitalxiao) để tự động duyệt UID thiết bị này lên hệ thống:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = {
                            adminPinInput = it
                            adminPinError = null
                        },
                        label = { Text("Mã Admin") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = adminPinError != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (adminPinError != null) {
                        Text(adminPinError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (LicenseManager.verifyAdminPasscodeAndSelfApprove(context, adminPinInput)) {
                            showAdminUnlockDialog = false
                            Toast.makeText(context, "Đã duyệt thiết bị Admin thành công!", Toast.LENGTH_SHORT).show()
                            onActivated()
                        } else {
                            adminPinError = "Mã Admin không chính xác!"
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminUnlockDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AlertDialog(
            onDismissRequest = { /* Bắt buộc UID được duyệt mới vào ứng dụng */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showAdminUnlockDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Xác thực UID",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Xác thực Thiết bị (UID)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Bản quyền phát triển bởi digitalxiao",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ứng dụng sử dụng cơ chế cấp phép trực tiếp qua UID thiết bị. Hãy sao chép mã UID bên dưới gửi cho Admin rồi bấm nút Xác minh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Hộp hiển thị Mã UID Thiết bị & Tên máy
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mã định danh thiết bị (UID)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = deviceUid,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Device UID", deviceUid))
                                        Toast.makeText(context, "Đã sao chép UID: $deviceUid", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Sao chép UID",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = deviceModel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Hộp hiển thị trạng thái duyệt UID trực tiếp
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPendingStatus) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isPendingStatus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusMessage,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isPendingStatus) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Tùy chọn không hỏi lại ở màn xác minh
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dontShowAgain = !dontShowAgain
                                LicenseManager.setSkipActivationPrompt(context, dontShowAgain)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = {
                                dontShowAgain = it
                                LicenseManager.setSkipActivationPrompt(context, it)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Không hiển thị lại ở lần mở sau",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Device UID", deviceUid))
                            Toast.makeText(context, "Đã sao chép UID: $deviceUid", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chép UID")
                    }

                    Button(
                        onClick = { triggerUidCheck() },
                        enabled = !isChecking,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.35f)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đang kiểm tra...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xác minh", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )
    }
}
