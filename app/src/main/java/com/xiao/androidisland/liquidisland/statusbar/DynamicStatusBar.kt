package com.xiao.androidisland.liquidisland.statusbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiao.androidisland.core.model.LiquidIslandSettings
import com.xiao.androidisland.core.model.SystemStatusSnapshot
import com.xiao.androidisland.liquidisland.absorption.AbsorptionEngine
import com.xiao.androidisland.liquidisland.absorption.MotionBlurEngine.applyLiquidAbsorption

/**
 * Dynamic iOS Status Bar (Section V & Section VIII).
 *
 * Shares the exact per-frame animation state (`islandCenterXDp`, `islandLeftEdgeDp`,
 * `islandRightEdgeDp`, `leftEdgeVelocityDpPerSec`, `rightEdgeVelocityDpPerSec`) with
 * the Dynamic Island Engine so that as the Island expands or splits, each individual
 * Status Bar element (`Clock`, `NotificationBadge`, `CellularBars`, `WifiIcon`, `BatteryIcon`)
 * undergoes Phase A (Approach stretch), Phase B (Contact + Directional Motion Blur),
 * and Phase C (Liquid Absorption), and reverses smoothly when the Island shrinks.
 */
@Composable
fun DynamicIosStatusBar(
    status: SystemStatusSnapshot,
    settings: LiquidIslandSettings,
    screenWidthDp: Float,
    islandCenterXDp: Float,
    islandLeftEdgeDp: Float,
    islandRightEdgeDp: Float,
    leftEdgeVelocityDpPerSec: Float,
    rightEdgeVelocityDpPerSec: Float,
    isOnDarkBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val contentColor = if (isOnDarkBackground) Color.White else Color(0xFF141619)

    // Evaluate per-element edge-based absorption kinematics (Section VIII.4)
    val clockKinematics = AbsorptionEngine.evaluateElementAbsorption(
        elementId = "status_clock",
        elementCenterXDp = 36f,
        elementWidthDp = 42f,
        islandCenterXDp = islandCenterXDp,
        islandLeftEdgeDp = islandLeftEdgeDp,
        islandRightEdgeDp = islandRightEdgeDp,
        islandEdgeVelocityDpPerSec = leftEdgeVelocityDpPerSec,
        absorptionIntensity = settings.absorptionIntensity,
        motionBlurIntensity = settings.motionBlurIntensity,
        density = density,
    )

    val badgeKinematics = AbsorptionEngine.evaluateElementAbsorption(
        elementId = "status_notif_badge",
        elementCenterXDp = 74f,
        elementWidthDp = 24f,
        islandCenterXDp = islandCenterXDp,
        islandLeftEdgeDp = islandLeftEdgeDp,
        islandRightEdgeDp = islandRightEdgeDp,
        islandEdgeVelocityDpPerSec = leftEdgeVelocityDpPerSec,
        absorptionIntensity = settings.absorptionIntensity,
        motionBlurIntensity = settings.motionBlurIntensity,
        density = density,
    )

    val cellularKinematics = AbsorptionEngine.evaluateElementAbsorption(
        elementId = "status_cellular",
        elementCenterXDp = screenWidthDp - 84f,
        elementWidthDp = 22f,
        islandCenterXDp = islandCenterXDp,
        islandLeftEdgeDp = islandLeftEdgeDp,
        islandRightEdgeDp = islandRightEdgeDp,
        islandEdgeVelocityDpPerSec = rightEdgeVelocityDpPerSec,
        absorptionIntensity = settings.absorptionIntensity,
        motionBlurIntensity = settings.motionBlurIntensity,
        density = density,
    )

    val wifiKinematics = AbsorptionEngine.evaluateElementAbsorption(
        elementId = "status_wifi",
        elementCenterXDp = screenWidthDp - 58f,
        elementWidthDp = 20f,
        islandCenterXDp = islandCenterXDp,
        islandLeftEdgeDp = islandLeftEdgeDp,
        islandRightEdgeDp = islandRightEdgeDp,
        islandEdgeVelocityDpPerSec = rightEdgeVelocityDpPerSec,
        absorptionIntensity = settings.absorptionIntensity,
        motionBlurIntensity = settings.motionBlurIntensity,
        density = density,
    )

    val batteryKinematics = AbsorptionEngine.evaluateElementAbsorption(
        elementId = "status_battery",
        elementCenterXDp = screenWidthDp - 28f,
        elementWidthDp = 32f,
        islandCenterXDp = islandCenterXDp,
        islandLeftEdgeDp = islandLeftEdgeDp,
        islandRightEdgeDp = islandRightEdgeDp,
        islandEdgeVelocityDpPerSec = rightEdgeVelocityDpPerSec,
        absorptionIntensity = settings.absorptionIntensity,
        motionBlurIntensity = settings.motionBlurIntensity,
        density = density,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp),
    ) {
        // Left Group: Real-Time Clock ("7:05") + Red Notification Pill Badge (matching 96942.mp4 00:00..00:38)
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = status.formattedClock,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.applyLiquidAbsorption(
                    kinematics = clockKinematics,
                    towardIslandSign = 1f,
                ),
            )

            if (status.activeNotificationCount > 0) {
                Box(
                    modifier = Modifier
                        .applyLiquidAbsorption(
                            kinematics = badgeKinematics,
                            towardIslandSign = 1f,
                        )
                        .size(width = 24.dp, height = 16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(0f, size.height * 0.15f),
                            size = Size(size.width, size.height * 0.7f),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            style = Stroke(width = 1.3.dp.toPx()),
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.6.dp.toPx(),
                            center = Offset(size.width * 0.5f, size.height * 0.5f),
                        )
                    }
                }
            }
        }

        // Right Group: Dual-SIM Cellular Signal Bars + Wi-Fi + iOS Battery Capsule with Percentage Inside ("79" / "81")
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Dual-SIM / Cellular Signal Bars
            IosDualSimSignalBars(
                primaryBars = status.signalBarsPrimary,
                secondaryBars = status.signalBarsSecondarySim,
                isDualSim = status.isDualSimActive,
                tint = contentColor,
                modifier = Modifier
                    .size(width = 18.dp, height = 12.dp)
                    .applyLiquidAbsorption(
                        kinematics = cellularKinematics,
                        towardIslandSign = -1f,
                    ),
            )

            // Concentric iOS Wi-Fi Icon
            if (status.isWifiEnabled) {
                IosConcentricWifiIcon(
                    tint = contentColor,
                    modifier = Modifier
                        .size(16.dp)
                        .applyLiquidAbsorption(
                            kinematics = wifiKinematics,
                            towardIslandSign = -1f,
                        ),
                )
            }

            // iOS Battery Pill with Percentage Inside ("79" / "80" / "81" in 96942.mp4)
            IosBatteryPillWithPercent(
                percent = status.batteryPercent,
                isCharging = status.isCharging,
                isOnDark = isOnDarkBackground,
                modifier = Modifier.applyLiquidAbsorption(
                    kinematics = batteryKinematics,
                    towardIslandSign = -1f,
                ),
            )
        }
    }
}

@Composable
fun IosDualSimSignalBars(
    primaryBars: Int,
    secondaryBars: Int,
    isDualSim: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.16f
        val gap = w * 0.08f
        val heights = floatArrayOf(0.35f, 0.54f, 0.76f, 1.0f)
        for (i in 0 until 4) {
            val bh = h * heights[i]
            val x = i * (barW + gap)
            val y = h - bh
            val active = i < primaryBars
            drawRoundRect(
                color = if (active) tint else tint.copy(alpha = 0.28f),
                topLeft = Offset(x, y),
                size = Size(barW, bh),
                cornerRadius = CornerRadius(barW * 0.45f, barW * 0.45f),
            )
        }
        if (isDualSim) {
            // Subtle bottom dual-SIM dots
            for (i in 0 until 4) {
                val x = i * (barW + gap) + barW * 0.5f
                drawCircle(
                    color = if (i < secondaryBars) tint else tint.copy(alpha = 0.25f),
                    radius = barW * 0.32f,
                    center = Offset(x, h + 1.5.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun IosConcentricWifiIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.82f
        val stroke = w * 0.13f

        val rOuter = w * 0.56f
        drawArc(
            color = tint,
            startAngle = -132f,
            sweepAngle = 84f,
            useCenter = false,
            topLeft = Offset(cx - rOuter, cy - rOuter),
            size = Size(rOuter * 2f, rOuter * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        val rMid = w * 0.34f
        drawArc(
            color = tint,
            startAngle = -130f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(cx - rMid, cy - rMid),
            size = Size(rMid * 2f, rMid * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        drawCircle(
            color = tint,
            radius = stroke * 0.65f,
            center = Offset(cx, cy),
        )
    }
}

@Composable
fun IosBatteryPillWithPercent(
    percent: Int,
    isCharging: Boolean,
    isOnDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val pillBg = if (isCharging) {
        Color(0xFF34C759)
    } else if (isOnDark) {
        Color(0xFFECECEC)
    } else {
        Color(0xFF1C1E22)
    }
    val textColor = if (isCharging || !isOnDark) Color.White else Color(0xFF121417)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 27.dp, height = 14.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(pillBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = percent.coerceIn(1, 100).toString(),
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(modifier = Modifier.width(1.5.dp))
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = 5.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(pillBg.copy(alpha = 0.65f)),
        )
    }
}
