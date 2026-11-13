package com.xiao.androidisland.liquidisland.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiao.androidisland.core.model.BiometricAuthType
import com.xiao.androidisland.core.model.IslandMode
import com.xiao.androidisland.core.model.LiquidIslandSettings
import kotlinx.coroutines.delay

/**
 * Reconstructs the `LOCK SCREEN AUTH` (`Face ID` / `Fingerprint`) and `MEDIA STYLE` (`Album Gradient`)
 * cards from `01:19`–`01:25` of `96942.mp4`, alongside all customization controls required in Section XIII
 * and a live state bar for testing every Dynamic Island & Status Bar Absorption state.
 */
@Composable
fun LiquidIslandSettingsSheet(
    settings: LiquidIslandSettings,
    isSystemOverlayRunning: Boolean,
    onUpdateSettings: (LiquidIslandSettings) -> Unit,
    onSelectDemoState: (String) -> Unit,
    onToggleOverlayService: (Boolean) -> Unit,
    onOpenControlCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var authVerified by remember(settings.lockScreenAuthType) { mutableStateOf(false) }
    LaunchedEffect(settings.lockScreenAuthType) {
        authVerified = false
        delay(850L)
        authVerified = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0D18),
                        Color(0xFF0F1629),
                        Color(0xFF090C15),
                    )
                )
            )
            .border(1.dp, Color(0x334D6BFE), RoundedCornerShape(30.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Mode Selector: DroidBubble (Preserved) vs LiquidIsland
        Text(
            text = "— ANDROIDISLAND MODE",
            color = Color(0xFF8B95B0),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF141A2E))
                .padding(4.dp),
        ) {
            val isLiquid = settings.activeMode == IslandMode.LIQUID_ISLAND
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (!isLiquid) Color(0xFF3B66F5) else Color.Transparent)
                    .clickable {
                        onUpdateSettings(settings.copy(activeMode = IslandMode.DROID_BUBBLE))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "DroidBubble (Classic)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isLiquid) Color(0xFF3B66F5) else Color.Transparent)
                    .clickable {
                        onUpdateSettings(settings.copy(activeMode = IslandMode.LIQUID_ISLAND))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "LiquidIsland (iOS 18)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // 2. Interactive Dynamic Island & Absorption State Bar
        Text(
            text = "— TEST STATES & STATUS BAR LIQUID ABSORPTION",
            color = Color(0xFF8B95B0),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )

        val stateChips = listOf(
            "Idle" to "Compact Idle (00:00)",
            "NotifCompact" to "Telegram Pill (00:01)",
            "NotifExpanded" to "Telegram Full (00:04)",
            "MusicCompact" to "Music Pill (00:07)",
            "SplitDual" to "Split Dual (00:09)",
            "NotifBanner" to "Metrolist (00:10)",
            "MusicExpanded" to "Music Card (00:13)",
            "TimerCompact" to "Timer Pill (00:22)",
            "TimerExpanded" to "Timer Card (00:28)",
            "NavCompact" to "Nav Pill (00:39)",
            "NavExpanded" to "Nav Card (00:44)",
            "Ring" to "Ring (00:50)",
            "Vibrate" to "Vibrate (00:51)",
            "Silent" to "Silent (00:54)",
            "Charging" to "Charging 80% (01:03)",
            "Auth" to "Face ID Auth (01:19)",
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            stateChips.forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2946))
                        .border(1.dp, Color(0x446085FF), RoundedCornerShape(16.dp))
                        .clickable { onSelectDemoState(key) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // 3. LOCK SCREEN AUTH Card (Exact match to 01:19 - 01:25 in 96942.mp4)
        Text(
            text = "— LOCK SCREEN AUTH",
            color = Color(0xFF8B95B0),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF141C36), Color(0xFF0E1428))
                    )
                )
                .border(1.dp, Color(0x334A6CF7), RoundedCornerShape(24.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Lock Screen Auth",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0E1C))
                    .padding(4.dp),
            ) {
                val isFaceId = settings.lockScreenAuthType == BiometricAuthType.FACE_ID
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isFaceId) Color(0xFF3B66F5) else Color.Transparent)
                        .clickable {
                            onUpdateSettings(settings.copy(lockScreenAuthType = BiometricAuthType.FACE_ID))
                            onSelectDemoState("Auth")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Face ID",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isFaceId) Color(0xFF3B66F5) else Color.Transparent)
                        .clickable {
                            onUpdateSettings(settings.copy(lockScreenAuthType = BiometricAuthType.FINGERPRINT))
                            onSelectDemoState("Auth")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Fingerprint",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Animated Preview Pill inside Lock Screen Auth card (01:20..01:24 in 96942.mp4)
            Row(
                modifier = Modifier
                    .width(148.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (authVerified) "🔓" else "🔒",
                    color = Color.White,
                    fontSize = 15.sp,
                )
                AnimatedContent(targetState = authVerified, label = "AuthCheckAnim") { verified ->
                    if (verified) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "✓", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = if (settings.lockScreenAuthType == BiometricAuthType.FACE_ID) "😊" else "☝",
                            color = Color(0xFF34C759),
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }

        // 4. MEDIA STYLE Card (Exact match to 01:19 - 01:25 in 96942.mp4)
        Text(
            text = "— MEDIA STYLE",
            color = Color(0xFF8B95B0),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF141C36))
                .border(1.dp, Color(0x334A6CF7), RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Album Gradient",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Switch(
                checked = settings.isAlbumGradientEnabled,
                onCheckedChange = {
                    onUpdateSettings(settings.copy(isAlbumGradientEnabled = it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3B66F5),
                ),
            )
        }

        // 5. System Overlay & Customization Controls (Section XIII)
        Text(
            text = "— LIQUIDISLAND ENGINE & ABSORPTION TUNING",
            color = Color(0xFF8B95B0),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )

        SettingToggleRow(
            label = "Enable System-Wide Overlay Service (24/7)",
            checked = isSystemOverlayRunning,
            onCheckedChange = onToggleOverlayService,
        )

        SettingToggleRow(
            label = "Enable iOS Dynamic Status Bar",
            checked = settings.isIosStatusBarEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(isIosStatusBarEnabled = it)) },
        )

        SettingToggleRow(
            label = "Enable iOS Control Center Gesture",
            checked = settings.isControlCenterEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(isControlCenterEnabled = it)) },
        )

        SettingToggleRow(
            label = "Dark Wallpaper Theme",
            checked = settings.isDarkMode,
            onCheckedChange = { onUpdateSettings(settings.copy(isDarkMode = it)) },
        )

        SettingSliderRow(
            label = "Island Width Scale (${"%.2f".format(settings.islandWidthScale)}x)",
            value = settings.islandWidthScale,
            valueRange = 0.80f..1.25f,
            onValueChange = { onUpdateSettings(settings.copy(islandWidthScale = it)) },
        )

        SettingSliderRow(
            label = "Island Height Scale (${"%.2f".format(settings.islandHeightScale)}x)",
            value = settings.islandHeightScale,
            valueRange = 0.85f..1.25f,
            onValueChange = { onUpdateSettings(settings.copy(islandHeightScale = it)) },
        )

        SettingSliderRow(
            label = "Animation Speed (${"%.2f".format(settings.animationSpeedMultiplier)}x)",
            value = settings.animationSpeedMultiplier,
            valueRange = 0.50f..1.80f,
            onValueChange = { onUpdateSettings(settings.copy(animationSpeedMultiplier = it)) },
        )

        SettingSliderRow(
            label = "Liquid Glass Refraction (${(settings.refractionIntensity * 100).toInt()}%)",
            value = settings.refractionIntensity,
            valueRange = 0.20f..1.40f,
            onValueChange = { onUpdateSettings(settings.copy(refractionIntensity = it)) },
        )

        SettingSliderRow(
            label = "Directional Motion Blur (${(settings.motionBlurIntensity * 100).toInt()}%)",
            value = settings.motionBlurIntensity,
            valueRange = 0.0f..1.50f,
            onValueChange = { onUpdateSettings(settings.copy(motionBlurIntensity = it)) },
        )

        SettingSliderRow(
            label = "Edge Liquid Absorption (${(settings.absorptionIntensity * 100).toInt()}%)",
            value = settings.absorptionIntensity,
            valueRange = 0.20f..1.40f,
            onValueChange = { onUpdateSettings(settings.copy(absorptionIntensity = it)) },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF3B66F5))
                .clickable(onClick = onOpenControlCenter),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Open iOS Control Center Sheet (00:49)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3B66F5),
            ),
        )
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF3B66F5),
            ),
        )
    }
}
