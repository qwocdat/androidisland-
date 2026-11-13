package com.xiao.androidisland.liquidisland.controlcenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiao.androidisland.core.model.MediaTrackState
import com.xiao.androidisland.core.model.SystemStatusSnapshot
import com.xiao.androidisland.liquidisland.renderer.LiquidBackdrop
import com.xiao.androidisland.liquidisland.renderer.LiquidEffectController
import com.xiao.androidisland.liquidisland.renderer.LiquidMaterial
import com.xiao.androidisland.liquidisland.renderer.liquidGlassSurface
import com.xiao.androidisland.liquidisland.statusbar.IosConcentricWifiIcon
import kotlin.math.roundToInt

/**
 * Reconstructs the iOS Control Center from `00:49`–`01:06` of `96942.mp4` (`Dynamic Motion 1.2 For Android`):
 * - Header Row: Large `7:01` clock + `Tue, Oct 13` on left; Edit (`✎`), Power (`⏻`), Settings (`⚙`) on right.
 * - 2x2 Liquid Glass Connectivity Pills:
 *   1. `Wi-Fi` (`WE 5G`)
 *   2. `Airplane mode`
 *   3. `Mobile data`
 *   4. `Mobile Hotspot`
 * - Bottom Section:
 *   - Left: Vertical Brightness & Volume capsules.
 *   - Right: Large `Media output` card (`Restless / untitled`) with artwork backdrop.
 */
@Composable
fun IosControlCenterSheet(
    status: SystemStatusSnapshot,
    track: MediaTrackState,
    backdrop: LiquidBackdrop,
    effectController: LiquidEffectController,
    onToggleWifi: () -> Unit,
    onToggleAirplane: () -> Unit,
    onToggleMobileData: () -> Unit,
    onToggleHotspot: () -> Unit,
    onCycleRingerMode: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseControlCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var dySum = 0f
                detectVerticalDragGestures(
                    onDragStart = { dySum = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dySum += dragAmount
                        if (dySum < -90f) {
                            onCloseControlCenter()
                        }
                    },
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Header Row: "7:01  Tue, Oct 13" on left | Edit, Power (Cycle Ringer preview), Settings on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = status.formattedClock,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.formattedDateHeader,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "✎",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = onCycleRingerMode),
                )
                Text(
                    text = "⏻",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = onCloseControlCenter),
                )
                Text(
                    text = "⚙",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = onOpenSettings),
                )
            }
        }

        // 2. 2x2 Connectivity Pills Grid (00:49 in 96942.mp4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ControlCenterConnectivityPill(
                title = "Wi-Fi",
                subtitle = if (status.isWifiEnabled) status.wifiSsid else "Off",
                isActive = status.isWifiEnabled,
                backdrop = backdrop,
                effectController = effectController,
                onClick = onToggleWifi,
                modifier = Modifier.weight(1f),
            ) {
                IosConcentricWifiIcon(
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            ControlCenterConnectivityPill(
                title = "Airplane mode",
                subtitle = if (status.isAirplaneModeEnabled) "On" else null,
                isActive = status.isAirplaneModeEnabled,
                backdrop = backdrop,
                effectController = effectController,
                onClick = onToggleAirplane,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "✈", color = Color.White, fontSize = 18.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ControlCenterConnectivityPill(
                title = "Mobile data",
                subtitle = if (status.isMobileDataEnabled) "On" else null,
                isActive = status.isMobileDataEnabled,
                backdrop = backdrop,
                effectController = effectController,
                onClick = onToggleMobileData,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "⇅", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            ControlCenterConnectivityPill(
                title = "Mobile Hotspot",
                subtitle = if (status.isHotspotEnabled) "On" else null,
                isActive = status.isHotspotEnabled,
                backdrop = backdrop,
                effectController = effectController,
                onClick = onToggleHotspot,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "📡", color = Color.White, fontSize = 16.sp)
            }
        }

        // 3. Bottom Row: Vertical Brightness & Volume Sliders (Left) + Media Output Card (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Left 2 Vertical Pill Sliders (Brightness & Volume)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ControlCenterVerticalSlider(
                    percent = status.brightnessPercent,
                    iconText = "☀",
                    backdrop = backdrop,
                    effectController = effectController,
                    onPercentChange = onBrightnessChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                ControlCenterVerticalSlider(
                    percent = status.volumePercent,
                    iconText = "🔊",
                    backdrop = backdrop,
                    effectController = effectController,
                    onPercentChange = onVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }

            // Right: Media Output Card ("Media output / Restless / untitled" at 00:49 in 96942.mp4)
            val mediaCardShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .liquidGlassSurface(
                        backdrop = backdrop,
                        shape = mediaCardShape,
                        material = LiquidMaterial.ControlCenterModuleGlass,
                        controller = effectController,
                    )
                    .clip(mediaCardShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x664A554E),
                                Color(0xCC1E2220),
                            )
                        )
                    )
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "✓", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Media output",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                ) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlCenterConnectivityPill(
    title: String,
    subtitle: String?,
    isActive: Boolean,
    backdrop: LiquidBackdrop,
    effectController: LiquidEffectController,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit,
) {
    val pillShape = RoundedCornerShape(34.dp)
    val material = if (isActive) {
        LiquidMaterial.ControlCenterActiveTileGlass
    } else {
        LiquidMaterial.ControlCenterModuleGlass
    }

    Row(
        modifier = modifier
            .height(68.dp)
            .liquidGlassSurface(
                backdrop = backdrop,
                shape = pillShape,
                material = material,
                controller = effectController,
            )
            .clip(pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            iconContent()
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ControlCenterVerticalSlider(
    percent: Int,
    iconText: String,
    backdrop: LiquidBackdrop,
    effectController: LiquidEffectController,
    onPercentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    var heightPx by remember { mutableFloatStateOf(1f) }
    val fillFraction = (percent.coerceIn(0, 100) / 100f).coerceIn(0.12f, 1f)

    Box(
        modifier = modifier
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val pct = ((1f - (change.position.y / heightPx)) * 100f)
                        .roundToInt()
                        .coerceIn(0, 100)
                    onPercentChange(pct)
                }
            }
            .liquidGlassSurface(
                backdrop = backdrop,
                shape = shape,
                material = LiquidMaterial.ControlCenterModuleGlass,
                controller = effectController,
            )
            .clip(shape),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(fillFraction)
                .background(Color.White.copy(alpha = 0.32f)),
        )
        Text(
            text = iconText,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )
    }
}
