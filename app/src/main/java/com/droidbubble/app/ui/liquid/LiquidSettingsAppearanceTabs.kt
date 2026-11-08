package com.droidbubble.app.ui.liquid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.ActiveCallState
import com.droidbubble.app.model.DesignLanguage
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.service.HardwareEqualizerManager
import com.droidbubble.app.service.NativeIslandSuppressor
import com.droidbubble.app.service.VoiceMemoManager
import com.droidbubble.app.theme.ISLAND_FONTS
import com.droidbubble.app.ui.main.CutoutModeOption
import com.droidbubble.app.ui.main.CutoutPositionSelectorCard
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LiquidSettingsTab(
    settings: IslandSettings,
    strings: AppStrings,
    onToggleEnabled: (Boolean) -> Unit,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onOpenAppFilterSheet: () -> Unit,
    onOpenAnimationSettings: () -> Unit,
    onOpenConfigManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVi = settings.appLanguage.lowercase() == "vi"

    var expandLiquidMaterial by remember { mutableStateOf(false) }
    var expandGeometry by remember { mutableStateOf(false) }
    var expandInteraction by remember { mutableStateOf(false) }
    var expandCustomization by remember { mutableStateOf(false) }
    var expandEqualizer by remember { mutableStateOf(false) }
    var expandPowerSaver by remember { mutableStateOf(false) }

    val batteryPct = remember {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 81) ?: 81
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt().coerceIn(1, 100) else 81
    }
    val systemPowerSave = remember {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isPowerSaveMode == true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Preview Header & Card (matching 00:37 - 00:43)
        item {
            Text(
                text = strings.livePreview,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Black Pill Preview
                    Box(
                        modifier = Modifier
                            .width(settings.widthDp.coerceIn(100, 220).dp)
                            .height(settings.heightDp.coerceIn(28, 52).dp)
                            .clip(RoundedCornerShape(settings.cornerRadiusDp.dp))
                            .background(Color.Black)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF30D158)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            MiniEqualizerBars(color = Color(0xFFFF2D55))
                        }
                    }
                }
            }
        }

        // 2. System Presence Card
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(strings.systemPresence, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.islandEnabled, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Switch(
                            checked = settings.isEnabled,
                            onCheckedChange = { onToggleEnabled(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.blackIdleIsland, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.alwaysShowPill,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(alwaysShowPill = v) } }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isVi) "Ẩn HyperIsland / Đảo mặc định hệ điều hành" else "Hide HyperIsland / Native OS Island",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = remember { NativeIslandSuppressor.getDetectedOsIslandName() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.hideNativeOsIsland,
                            onCheckedChange = { v ->
                                onUpdateSettings { it.copy(hideNativeOsIsland = v) }
                                NativeIslandSuppressor.applyNativeIslandSuppression(context, v)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isVi) "Lớp phủ đen che kín Đảo hệ thống" else "Opaque Black Cutout Mask",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.nativeIslandMaskBoost,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(nativeIslandMaskBoost = v) } }
                        )
                    }
                }
            }
        }

        // 3. Liquid Material Accordion (Refraction & Surface Tension)
        item {
            ExpandableLiquidCard(
                title = strings.liquidMaterial,
                subtitle = "Refraction: ${String.format(Locale.US, "%.2f", settings.liquidRefraction)} · Tension: ${String.format(Locale.US, "%.2f", settings.liquidSurfaceTension)}",
                icon = Icons.Default.Settings,
                expanded = expandLiquidMaterial,
                onToggle = { expandLiquidMaterial = !expandLiquidMaterial }
            ) {
                LiquidSliderRow(
                    label = strings.refraction,
                    value = settings.liquidRefraction,
                    valueRange = 0.50f..2.00f,
                    valueText = String.format(Locale.US, "%.2f", settings.liquidRefraction),
                    showValue = settings.showSliderValues,
                    onValueChange = { v -> onUpdateSettings { it.copy(liquidRefraction = v) } }
                )
                LiquidSliderRow(
                    label = strings.surfaceTension,
                    value = settings.liquidSurfaceTension,
                    valueRange = 1.00f..8.00f,
                    valueText = String.format(Locale.US, "%.2f", settings.liquidSurfaceTension),
                    showValue = settings.showSliderValues,
                    onValueChange = { v -> onUpdateSettings { it.copy(liquidSurfaceTension = v) } }
                )
            }
        }

        // 4. Geometry Accordion (matching 00:43 - 00:44)
        item {
            ExpandableLiquidCard(
                title = strings.geometry,
                subtitle = "${settings.widthDp}x${settings.heightDp}px · Offset: ${settings.topOffsetDp}Y · Rounded: ${settings.expandedCornerRadiusDp}px",
                icon = Icons.Default.Build,
                expanded = expandGeometry,
                onToggle = { expandGeometry = !expandGeometry }
            ) {
                val currentCutoutMode = when {
                    settings.isTeardropCutout -> CutoutModeOption.TEARDROP
                    settings.isLeftCutout -> CutoutModeOption.LEFT
                    else -> CutoutModeOption.CENTER
                }
                CutoutPositionSelectorCard(
                    selectedMode = currentCutoutMode,
                    onSelectMode = { mode ->
                        onUpdateSettings { s ->
                            when (mode) {
                                CutoutModeOption.CENTER -> s.copy(isLeftCutout = false, isTeardropCutout = false)
                                CutoutModeOption.LEFT -> s.copy(isLeftCutout = true, isTeardropCutout = false)
                                CutoutModeOption.TEARDROP -> s.copy(isLeftCutout = false, isTeardropCutout = true)
                            }
                        }
                    }
                )

                LiquidSliderRow(
                    label = strings.verticalOffset,
                    value = settings.topOffsetDp.toFloat(),
                    valueRange = -30f..60f,
                    valueText = "${settings.topOffsetDp}px",
                    showValue = settings.showSliderValues,
                    onValueChange = { v -> onUpdateSettings { it.copy(topOffsetDp = v.roundToInt()) } }
                )
                LiquidSliderRow(
                    label = strings.verticalOffsetOpening,
                    value = settings.verticalOffsetOpeningDp.toFloat(),
                    valueRange = -20f..40f,
                    valueText = "${settings.verticalOffsetOpeningDp}dp",
                    showValue = settings.showSliderValues,
                    onValueChange = { v -> onUpdateSettings { it.copy(verticalOffsetOpeningDp = v.roundToInt()) } }
                )
                LiquidSliderRow(
                    label = strings.horizontalOffset,
                    value = settings.horizontalOffsetDp.toFloat(),
                    valueRange = -50f..50f,
                    valueText = "${settings.horizontalOffsetDp}px",
                    showValue = settings.showSliderValues,
                    onValueChange = { v -> onUpdateSettings { it.copy(horizontalOffsetDp = v.roundToInt()) } }
                )
                LiquidSliderRow(
                    label = strings.widthLabel,
                    value = settings.widthDp.toFloat(),
                    valueRange = 60f..260f,
                    valueText = "${settings.widthDp}px",
                    showValue = settings.showSliderValues,
                    onValueChange = { v -> onUpdateSettings { it.copy(widthDp = v.roundToInt()) } }
                )
                LiquidSliderRow(
                    label = strings.heightLabel,
                    value = settings.heightDp.toFloat(),
                    valueRange = 20f..56f,
                    valueText = "${settings.heightDp}px",
                    showValue = settings.showSliderValues,
                    onValueChange = { v -> onUpdateSettings { it.copy(heightDp = v.roundToInt()) } }
                )
                LiquidSliderRow(
                    label = strings.expandedRounding,
                    value = settings.expandedCornerRadiusDp.toFloat(),
                    valueRange = 14f..48f,
                    valueText = "${settings.expandedCornerRadiusDp}px",
                    showValue = settings.showSliderValues,
                    onValueChange = { v ->
                        val r = v.roundToInt()
                        onUpdateSettings { it.copy(expandedCornerRadiusDp = r, cornerRadiusDp = (r * 0.6f).roundToInt().coerceIn(10, 28)) }
                    }
                )
            }
        }

        // 5. Interaction Accordion (matching 01:02 - 01:06)
        item {
            ExpandableLiquidCard(
                title = strings.interaction,
                subtitle = if (settings.hapticsEnabled) "Haptic feedback · Active" else "Haptic feedback · Off",
                icon = Icons.Default.ThumbUp,
                expanded = expandInteraction,
                onToggle = { expandInteraction = !expandInteraction }
            ) {
                LiquidToggleRow(
                    title = strings.vibration,
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(hapticsEnabled = v) } }
                )
                LiquidToggleRow(
                    title = strings.tapToExpand,
                    checked = settings.tapToExpand,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(tapToExpand = v) } }
                )
                LiquidToggleRow(
                    title = strings.disableExpandLandscape,
                    checked = settings.disableExpandInLandscape,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(disableExpandInLandscape = v) } }
                )
                LiquidToggleRow(
                    title = strings.closeOnBgTap,
                    checked = settings.closeOnBackgroundTap,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(closeOnBackgroundTap = v) } }
                )
                LiquidToggleRow(
                    title = strings.bgDimming,
                    checked = settings.backgroundDimming,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(backgroundDimming = v) } }
                )
                LiquidToggleRow(
                    title = strings.privacyIndicators,
                    checked = settings.privacyIndicators,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(privacyIndicators = v) } }
                )
                LiquidSliderRow(
                    label = strings.dotPosition,
                    value = settings.privacyDotPositionPx.toFloat(),
                    valueRange = 0f..16f,
                    valueText = "${settings.privacyDotPositionPx}px",
                    showValue = settings.showSliderValues,
                    onValueChange = { v -> onUpdateSettings { it.copy(privacyDotPositionPx = v.roundToInt()) } }
                )
                LiquidToggleRow(
                    title = strings.motionBlur,
                    checked = settings.motionBlur,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(motionBlur = v) } }
                )
            }
        }

        // 6. Customization Accordion (matching 01:01 - 01:07)
        item {
            ExpandableLiquidCard(
                title = strings.customization,
                subtitle = "Opacity: ${(settings.islandOpacity * 100).roundToInt()}% · Borders & Colors",
                icon = Icons.Default.Edit,
                expanded = expandCustomization,
                onToggle = { expandCustomization = !expandCustomization }
            ) {
                LiquidSliderRow(
                    label = strings.opacity,
                    value = settings.islandOpacity,
                    valueRange = 0.2f..1.0f,
                    valueText = String.format(Locale.US, "%.1f", settings.islandOpacity),
                    showValue = settings.showSliderValues,
                    onValueChange = { v ->
                        onUpdateSettings {
                            it.copy(
                                islandOpacity = v,
                                glassOpacity = (v * 100).roundToInt().coerceIn(15, 100)
                            )
                        }
                    }
                )
                LiquidToggleRow(
                    title = strings.dynamicLight,
                    subtitle = strings.dynamicLightSub,
                    checked = settings.dynamicLightGlow,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(dynamicLightGlow = v, showOutline = v) } }
                )
                LiquidToggleRow(
                    title = strings.hideInLandscape,
                    checked = !settings.showInLandscape,
                    onCheckedChange = { hide -> onUpdateSettings { it.copy(showInLandscape = !hide) } }
                )
                LiquidToggleRow(
                    title = strings.lockRotation,
                    checked = settings.lockRotation,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(lockRotation = v) } }
                )
                LiquidToggleRow(
                    title = strings.hideWhenIdle,
                    checked = !settings.alwaysShowPill,
                    onCheckedChange = { hideIdle -> onUpdateSettings { it.copy(alwaysShowPill = !hideIdle) } }
                )
                LiquidToggleRow(
                    title = strings.hideWhenShadeOpen,
                    checked = settings.hideWhenShadeOpen,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(hideWhenShadeOpen = v) } }
                )

                Surface(
                    onClick = onOpenAppFilterSheet,
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(strings.hideInApps, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(strings.hideInAppsSub, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    )
                }
            }
        }

        // 7. Audio Equalizer Accordion (matching 01:07 - 01:08)
        item {
            ExpandableLiquidCard(
                title = strings.audioEqualizer,
                subtitle = "Hardware EQ: ${settings.eqPreset}",
                icon = Icons.Default.Equalizer,
                expanded = expandEqualizer,
                onToggle = { expandEqualizer = !expandEqualizer }
            ) {
                LiquidToggleRow(
                    title = strings.showEqualizer,
                    checked = settings.eqShowEqualizer,
                    onCheckedChange = { v ->
                        onUpdateSettings {
                            val next = it.copy(eqShowEqualizer = v)
                            HardwareEqualizerManager.applyFromSettings(next)
                            next
                        }
                    }
                )
                LiquidToggleRow(
                    title = strings.realTimeSoundVis,
                    checked = settings.eqRealTimeVisuals,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(eqRealTimeVisuals = v, mediaWaveAnimation = v) } }
                )

                // Preset Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val eqPresets = listOf("CUSTOM" to strings.eqTuningMode, "BASS_BOOST" to "Bass Boost", "VOCAL" to "Vocal", "POP" to "Pop", "ROCK" to "Rock", "FLAT" to "Flat")
                    eqPresets.forEach { (pKey, pName) ->
                        val isSel = settings.eqPreset == pKey
                        Surface(
                            onClick = {
                                val bands = HardwareEqualizerManager.getPresetBands(pKey)
                                onUpdateSettings { s ->
                                    val next = if (pKey == "CUSTOM") {
                                        s.copy(eqPreset = pKey)
                                    } else {
                                        s.copy(
                                            eqPreset = pKey,
                                            eqBand60Hz = bands[0],
                                            eqBand230Hz = bands[1],
                                            eqBand910Hz = bands[2],
                                            eqBand3kHz = bands[3],
                                            eqBand14kHz = bands[4]
                                        )
                                    }
                                    HardwareEqualizerManager.applyFromSettings(next)
                                    next
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = pName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Text(
                    text = strings.frequencyBands,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                LiquidSliderRow(
                    label = "60 Hz",
                    value = settings.eqBand60Hz,
                    valueRange = -15f..15f,
                    valueText = String.format(Locale.US, "%.1f dB", settings.eqBand60Hz),
                    showValue = true,
                    onValueChange = { v ->
                        onUpdateSettings {
                            val next = it.copy(eqPreset = "CUSTOM", eqBand60Hz = v)
                            HardwareEqualizerManager.applyFromSettings(next)
                            next
                        }
                    }
                )
                LiquidSliderRow(
                    label = "230 Hz",
                    value = settings.eqBand230Hz,
                    valueRange = -15f..15f,
                    valueText = String.format(Locale.US, "%.1f dB", settings.eqBand230Hz),
                    showValue = true,
                    onValueChange = { v ->
                        onUpdateSettings {
                            val next = it.copy(eqPreset = "CUSTOM", eqBand230Hz = v)
                            HardwareEqualizerManager.applyFromSettings(next)
                            next
                        }
                    }
                )
                LiquidSliderRow(
                    label = "910 Hz",
                    value = settings.eqBand910Hz,
                    valueRange = -15f..15f,
                    valueText = String.format(Locale.US, "%.1f dB", settings.eqBand910Hz),
                    showValue = true,
                    onValueChange = { v ->
                        onUpdateSettings {
                            val next = it.copy(eqPreset = "CUSTOM", eqBand910Hz = v)
                            HardwareEqualizerManager.applyFromSettings(next)
                            next
                        }
                    }
                )
                LiquidSliderRow(
                    label = "3 kHz",
                    value = settings.eqBand3kHz,
                    valueRange = -15f..15f,
                    valueText = String.format(Locale.US, "%.1f dB", settings.eqBand3kHz),
                    showValue = true,
                    onValueChange = { v ->
                        onUpdateSettings {
                            val next = it.copy(eqPreset = "CUSTOM", eqBand3kHz = v)
                            HardwareEqualizerManager.applyFromSettings(next)
                            next
                        }
                    }
                )
                LiquidSliderRow(
                    label = "14 kHz",
                    value = settings.eqBand14kHz,
                    valueRange = -15f..15f,
                    valueText = String.format(Locale.US, "%.1f dB", settings.eqBand14kHz),
                    showValue = true,
                    onValueChange = { v ->
                        onUpdateSettings {
                            val next = it.copy(eqPreset = "CUSTOM", eqBand14kHz = v)
                            HardwareEqualizerManager.applyFromSettings(next)
                            next
                        }
                    }
                )
            }
        }

        // 8. Power Saver Accordion (matching 00:59 - 01:10)
        item {
            ExpandableLiquidCard(
                title = strings.powerSaver,
                subtitle = "${settings.powerSavingProfile} (≤ ${settings.ultraTriggerThresholdPct}%)",
                icon = Icons.Default.EnergySavingsLeaf,
                expanded = expandPowerSaver,
                onToggle = { expandPowerSaver = !expandPowerSaver }
            ) {
                val isUltra = settings.ultraPowerSaverEnabled || settings.powerSavingProfile == "ULTRA"
                val redAlert = Color(0xFFD93829)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isUltra) redAlert.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isUltra) Modifier.border(1.dp, redAlert.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isUltra) redAlert.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isUltra) redAlert else MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = if (isUltra) "Ultra Saver Active" else "Standard Mode",
                                fontWeight = FontWeight.Bold,
                                color = if (isUltra) redAlert else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Battery: $batteryPct% · System Saver: ${if (systemPowerSave) "ON" else "OFF"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(strings.energySavingProfile, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val profiles = listOf(
                        "STANDARD" to strings.profileStandard,
                        "ECO" to strings.profileEco,
                        "ULTRA" to strings.profileUltra
                    )
                    profiles.forEach { (key, label) ->
                        val sel = settings.powerSavingProfile == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(3.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (sel) {
                                        if (key == "ULTRA") redAlert else MaterialTheme.colorScheme.primary
                                    } else Color.Transparent
                                )
                                .clickable {
                                    onUpdateSettings {
                                        it.copy(
                                            powerSavingProfile = key,
                                            ultraPowerSaverEnabled = (key == "ULTRA"),
                                            appThemeMode = if (key == "ULTRA" && it.ultraTrueBlackOled) "OLED" else it.appThemeMode
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                LiquidToggleRow(
                    title = strings.ultraPowerSaver,
                    subtitle = strings.ultraPowerSaverSub,
                    checked = settings.ultraPowerSaverEnabled,
                    onCheckedChange = { v ->
                        onUpdateSettings {
                            it.copy(
                                ultraPowerSaverEnabled = v,
                                powerSavingProfile = if (v) "ULTRA" else "STANDARD"
                            )
                        }
                    }
                )

                Text(strings.ultraModeTrigger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val triggers = listOf(
                        "SYSTEM_OR_LOW" to strings.triggerSystemOrLow,
                        "SYSTEM_SAVER" to strings.triggerSystemSaver,
                        "LOW_BATTERY" to strings.triggerLowBattery
                    )
                    triggers.forEach { (tKey, tLabel) ->
                        val sel = settings.ultraModeTrigger == tKey
                        Surface(
                            onClick = { onUpdateSettings { it.copy(ultraModeTrigger = tKey) } },
                            shape = RoundedCornerShape(12.dp),
                            color = if (sel) redAlert else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                        ) {
                            Text(
                                text = tLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                LiquidSliderRow(
                    label = strings.ultraTriggerThreshold,
                    value = settings.ultraTriggerThresholdPct.toFloat(),
                    valueRange = 5f..50f,
                    valueText = "${settings.ultraTriggerThresholdPct}%",
                    showValue = true,
                    accentColor = redAlert,
                    onValueChange = { v -> onUpdateSettings { it.copy(ultraTriggerThresholdPct = v.roundToInt()) } }
                )

                Text(strings.ultraPowerSavingFeatures, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LiquidToggleRow(
                    title = strings.trueBlackOled,
                    checked = settings.ultraTrueBlackOled,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(ultraTrueBlackOled = v) } }
                )
                LiquidToggleRow(
                    title = strings.staticIndicators,
                    checked = settings.ultraStaticIndicators,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(ultraStaticIndicators = v) } }
                )
                LiquidToggleRow(
                    title = strings.limit30Fps,
                    checked = settings.ultraLimit30Fps,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(ultraLimit30Fps = v) } }
                )
                LiquidToggleRow(
                    title = strings.disableVibrationHaptics,
                    checked = settings.ultraDisableHaptics,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(ultraDisableHaptics = v) } }
                )
                LiquidToggleRow(
                    title = strings.essentialEventsOnly,
                    checked = settings.ultraEssentialOnly,
                    onCheckedChange = { v -> onUpdateSettings { it.copy(ultraEssentialOnly = v) } }
                )
            }
        }

        // 9. Sub-System Configurations Header & Cards (Animation Settings + Config Manager)
        item {
            Text(
                text = strings.subSystemConfigs,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }

        item {
            Surface(
                onClick = onOpenAnimationSettings,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(strings.animationSettings, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(strings.animationSettingsSub) },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF9F0A).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFF9F0A))
                        }
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                )
            }
        }

        item {
            Surface(
                onClick = onOpenConfigManager,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(strings.configManager, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(strings.configManagerSub) },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF30D158).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF30D158))
                        }
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun LiquidToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LiquidAppearanceTab(
    settings: IslandSettings,
    strings: AppStrings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVi = settings.appLanguage.lowercase() == "vi"

    var expandLanguageList by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var expandDevPanel by remember { mutableStateOf(false) }
    var devKeyInput by remember { mutableStateOf("") }
    var showDevKeyPassword by remember { mutableStateOf(false) }
    var swappedPreviewMultitask by remember { mutableStateOf(false) }

    val currentLangOption = SUPPORTED_LANGUAGES.firstOrNull { it.code == settings.appLanguage.lowercase() }
        ?: SUPPORTED_LANGUAGES.first()

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text(if (isVi) "Chọn phông chữ" else "Select Font", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ISLAND_FONTS.forEach { fontOption ->
                        val isSel = settings.fontKey == fontOption.key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onUpdateSettings { it.copy(fontKey = fontOption.key) }
                                    showFontDialog = false
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(fontOption.name, fontFamily = fontOption.family, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            RadioButton(selected = isSel, onClick = {
                                onUpdateSettings { it.copy(fontKey = fontOption.key) }
                                showFontDialog = false
                            })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text(if (isVi) "Xong" else "Done")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Language Card (matching 00:46 - 00:49, with Tiếng Việt as Default + 12 languages from video)
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(strings.languageLabel, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    // Inner Language Dropdown Selector Box
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandLanguageList = !expandLanguageList }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    Column {
                                        Text(
                                            text = "${currentLangOption.flag} ${currentLangOption.nativeName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (expandLanguageList) strings.scrollListHint else currentLangOption.code.uppercase(),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (expandLanguageList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            AnimatedVisibility(visible = expandLanguageList) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(SUPPORTED_LANGUAGES, key = { it.code }) { lang ->
                                        val isSelected = lang.code == currentLangOption.code
                                        Surface(
                                            onClick = {
                                                onUpdateSettings { it.copy(appLanguage = lang.code) }
                                                expandLanguageList = false
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                                    else Modifier
                                                )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${lang.flag}  ${lang.nativeName}",
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.5.sp
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Theme 2x2 Visual Card (System, Dark, OLED, Light - matching 00:46 - 00:49)
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(strings.themeLabel, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeVisualCard(
                            label = strings.themeSystem,
                            modeKey = "SYSTEM",
                            isSelected = settings.appThemeMode == "SYSTEM",
                            onClick = { onUpdateSettings { it.copy(appThemeMode = "SYSTEM") } },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeVisualCard(
                            label = strings.themeDark,
                            modeKey = "DARK",
                            isSelected = settings.appThemeMode == "DARK",
                            onClick = { onUpdateSettings { it.copy(appThemeMode = "DARK") } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeVisualCard(
                            label = strings.themeOled,
                            modeKey = "OLED",
                            isSelected = settings.appThemeMode == "OLED",
                            onClick = { onUpdateSettings { it.copy(appThemeMode = "OLED") } },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeVisualCard(
                            label = strings.themeLight,
                            modeKey = "LIGHT",
                            isSelected = settings.appThemeMode == "LIGHT",
                            onClick = { onUpdateSettings { it.copy(appThemeMode = "LIGHT") } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Accent Color Card (matching 00:49 - 00:50)
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(strings.accentColorLabel, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    val colors = listOf(
                        Triple("BLUE", if (isVi) "Xanh" else "Blue", Color(0xFF0A84FF)),
                        Triple("PURPLE", if (isVi) "Tím" else "Purple", Color(0xFFBF5AF2)),
                        Triple("PINK", if (isVi) "Hồng" else "Pink", Color(0xFFFF375F)),
                        Triple("RED", if (isVi) "Đỏ" else "Red", Color(0xFFFF453A)),
                        Triple("ORANGE", if (isVi) "Cam" else "Orange", Color(0xFFFF9F0A)),
                        Triple("GREEN", if (isVi) "Lục" else "Green", Color(0xFF30D158)),
                        Triple("CYAN", if (isVi) "Lam" else "Cyan", Color(0xFF64D2FF))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colors.forEach { (key, label, swatch) ->
                            val isSel = settings.accentColorKey == key
                            Surface(
                                onClick = { onUpdateSettings { it.copy(accentColorKey = key) } },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSel) swatch.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .width(74.dp)
                                    .height(94.dp)
                                    .then(
                                        if (isSel) Modifier.border(1.8.dp, swatch, RoundedCornerShape(18.dp))
                                        else Modifier
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(swatch)
                                            .then(
                                                if (isSel) Modifier.border(2.5.dp, Color.White, CircleShape)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSel) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) swatch else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Island Refresh Rate Card (60, 90, 120, 165 FPS - matching 00:50)
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(strings.islandRefreshRate, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(60, 90, 120, 165).forEach { fps ->
                            val isSel = settings.islandRefreshRateHz == fps
                            Surface(
                                onClick = { onUpdateSettings { it.copy(islandRefreshRateHz = fps) } },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .then(
                                        if (isSel) Modifier.border(1.8.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                        else Modifier
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$fps",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "FPS",
                                        fontSize = 11.sp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Multitasking Card (matching 00:50 - 00:52)
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(strings.multitasking, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    val modes = listOf(
                        Triple("CARDS", strings.multiCards, strings.multiCardsSub),
                        Triple("CIRCLE", strings.multiCircle, strings.multiCircleSub),
                        Triple("FLUID_MERGE", strings.multiFluidMerge, strings.multiFluidMergeSub),
                        Triple("APP_LAUNCH", strings.multiAppLaunch, strings.multiAppLaunchSub)
                    )
                    for (r in 0 until 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (c in 0 until 2) {
                                val (mKey, mTitle, mSub) = modes[r * 2 + c]
                                val isSel = settings.multitaskingMode == mKey
                                Surface(
                                    onClick = {
                                        onUpdateSettings {
                                            it.copy(
                                                multitaskingMode = mKey,
                                                showDualActivityInCompact = (mKey != "CARDS")
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(106.dp)
                                        .then(
                                            if (isSel) Modifier.border(1.8.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                                            else Modifier
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF101116)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(42.dp)
                                                        .height(14.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.Black)
                                                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                )
                                                if (mKey != "CARDS") {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Black)
                                                            .border(0.8.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = mTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = mSub,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Multitasking Preview (Timer 14:59 + Music)
                    Text(strings.multitaskingPreview, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                    Surface(
                        onClick = { swappedPreviewMultitask = !swappedPreviewMultitask },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(settings.animBubbleGapDp.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(128.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (swappedPreviewMultitask) Icons.Default.MusicNote else Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9F0A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (swappedPreviewMultitask) "Starboy" else "14:59",
                                            color = Color(0xFFFF9F0A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(
                                            if (settings.multitaskingBubbleShape == "SQUIRCLE") RoundedCornerShape(12.dp)
                                            else CircleShape
                                        )
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF9F0A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (swappedPreviewMultitask) Icons.Default.Timer else Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Timer + Music · Tap to switch",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Multitasking Bubble Shape
                    Text(strings.multitaskingBubbleShape, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                    val shapes = listOf("CIRCLE" to "Circle", "SQUIRCLE" to "Squircle", "ADAPTIVE" to "Adaptive", "CUSTOM" to "Custom")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shapes.forEach { (sKey, sName) ->
                            val isSel = settings.multitaskingBubbleShape == sKey
                            Surface(
                                onClick = { onUpdateSettings { it.copy(multitaskingBubbleShape = sKey) } },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .then(
                                        if (isSel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                        else Modifier
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = sName,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    LiquidSliderRow(
                        label = strings.bubbleWidth,
                        value = settings.multitaskingBubbleWidthDp.toFloat(),
                        valueRange = 30f..64f,
                        valueText = "${settings.multitaskingBubbleWidthDp} dp",
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(multitaskingBubbleWidthDp = v.roundToInt()) } }
                    )
                    LiquidSliderRow(
                        label = strings.cornerRadius,
                        value = settings.multitaskingCornerRadiusDp.toFloat(),
                        valueRange = 8f..32f,
                        valueText = "${settings.multitaskingCornerRadiusDp} dp",
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(multitaskingCornerRadiusDp = v.roundToInt()) } }
                    )
                    LiquidToggleRow(
                        title = strings.centeringShift,
                        checked = settings.centeringShift,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(centeringShift = v) } }
                    )
                    LiquidToggleRow(
                        title = strings.leftMultitaskingCircle,
                        checked = settings.leftMultitaskingCircle,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(leftMultitaskingCircle = v) } }
                    )
                }
            }
        }

        // 6. Font & Island Style Card (Preserved from original app)
        item {
            Surface(
                onClick = { showFontDialog = true },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentFontName = ISLAND_FONTS.firstOrNull { it.key == settings.fontKey }?.name ?: "Google Sans Flex"
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(if (isVi) "Phông chữ Đảo động (Font)" else "Island Font", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(currentFontName) },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                )
            }
        }

        // 7. UI Helpers Card (Show Slider Values - matching 00:53)
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(strings.uiHelpers, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    LiquidToggleRow(
                        title = strings.showSliderValues,
                        subtitle = strings.showSliderValuesSub,
                        checked = settings.showSliderValues,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(showSliderValues = v) } }
                    )
                }
            }
        }

        // 8. Diagnostic Metrics Card (Crashlytics Data Collection - matching 00:54)
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(strings.diagnosticMetrics, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    LiquidToggleRow(
                        title = strings.crashlyticsCollection,
                        checked = settings.crashlyticsEnabled,
                        onCheckedChange = { v -> onUpdateSettings { it.copy(crashlyticsEnabled = v) } }
                    )
                }
            }
        }

        // 9. Developer Panel Card (Developer Sandbox - matching 00:54 - 00:55)
        item {
            ExpandableLiquidCard(
                title = strings.developerPanel,
                subtitle = if (settings.devPanelUnlocked) "Unlocked · Active" else "Restricted section",
                icon = Icons.Default.Build,
                expanded = expandDevPanel,
                onToggle = { expandDevPanel = !expandDevPanel }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF453A).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(strings.developerSandbox, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(strings.developerSandboxSub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = devKeyInput,
                    onValueChange = { devKeyInput = it },
                    placeholder = { Text(strings.enterSecretDevKey) },
                    singleLine = true,
                    visualTransformation = if (showDevKeyPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showDevKeyPassword = !showDevKeyPassword }) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val clean = devKeyInput.trim().lowercase()
                        if (clean == "digitalxiao" || clean == "admin" || clean == "2026" || clean.isNotEmpty()) {
                            onUpdateSettings { it.copy(devPanelUnlocked = true) }
                            Toast.makeText(context, if (isVi) "Đã mở khóa Developer Sandbox!" else "Developer Sandbox Unlocked!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(strings.unlockSystem, fontWeight = FontWeight.Bold)
                }

                if (settings.devPanelUnlocked) {
                    Text(
                        text = if (isVi) "Công cụ mô phỏng nhanh trên Đảo thật:" else "Live Island Simulation Triggers:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                IslandStateManager.updateMedia(
                                    MediaInfo(
                                        packageName = "com.spotify.music",
                                        title = "Starboy",
                                        artist = "The Weeknd",
                                        isPlaying = true,
                                        durationMs = 230000L,
                                        positionMs = 65000L
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Media", fontSize = 12.sp) }

                        OutlinedButton(
                            onClick = {
                                IslandStateManager.updateCallState(
                                    ActiveCallState(isActive = true, callerName = "Alex Johnson", durationSeconds = 76)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Call", fontSize = 12.sp) }

                        OutlinedButton(
                            onClick = { VoiceMemoManager.toggleRecording(context) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Memo", fontSize = 12.sp) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                IslandStateManager.updateNotification(
                                    NotificationInfo(
                                        key = "dev_notif",
                                        packageName = context.packageName,
                                        appName = "Androidisland",
                                        title = "digitalxiao",
                                        text = "Thử nghiệm thông báo trực tiếp trên Đảo động Liquid Island!"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Notif", fontSize = 12.sp) }

                        OutlinedButton(
                            onClick = { IslandStateManager.triggerChargingEvent(88, 67.0) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Charge", fontSize = 12.sp) }

                        OutlinedButton(
                            onClick = {
                                IslandStateManager.updateMedia(null)
                                IslandStateManager.updateNotification(null)
                                IslandStateManager.updateCallState(ActiveCallState(isActive = false))
                                VoiceMemoManager.stopRecording()
                                IslandStateManager.collapse()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reset", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeVisualCard(
    label: String,
    modeKey: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
            .height(106.dp)
            .then(
                if (isSelected) Modifier.border(1.8.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                when (modeKey) {
                    "SYSTEM" -> {
                        drawRect(color = Color(0xFF14151A), size = Size(size.width / 2f, size.height))
                        drawRect(color = Color(0xFFE6EAF2), topLeft = Offset(size.width / 2f, 0f), size = Size(size.width / 2f, size.height))
                    }
                    "DARK" -> {
                        drawRect(color = Color(0xFF14151A))
                    }
                    "OLED" -> {
                        drawRect(color = Color.Black)
                    }
                    "LIGHT" -> {
                        drawRect(color = Color(0xFFEAEFF7))
                    }
                }
                val pillW = size.width * 0.36f
                val pillH = 10.dp.toPx()
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset((size.width - pillW) / 2f, 8.dp.toPx()),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                )
            }
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
