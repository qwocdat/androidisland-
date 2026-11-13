package com.xiao.androidisland.liquidisland.widgets

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiao.androidisland.liquidisland.renderer.LiquidBackdrop
import com.xiao.androidisland.liquidisland.renderer.LiquidEffectController
import com.xiao.androidisland.liquidisland.renderer.LiquidMaterial
import com.xiao.androidisland.liquidisland.renderer.liquidGlassSurface
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dual-Month Calendar Widget & Analog Clock Widget from `00:00`–`00:48` of `96942.mp4` (Section IX).
 * Updates automatically from real system date/time (`LocalDate.now()`, `LocalTime.now()`) and
 * supports interactive month switching by tapping the left/right month headers.
 */
@Composable
fun DualMonthCalendarWidget(
    backdrop: LiquidBackdrop,
    effectController: LiquidEffectController,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var monthOffset by remember { mutableIntStateOf(0) }
    val firstMonth = remember(today, monthOffset) { YearMonth.from(today).plusMonths(monthOffset.toLong()) }
    val secondMonth = remember(firstMonth) { firstMonth.plusMonths(1) }

    val cardShape = RoundedCornerShape(32.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                backdrop = backdrop,
                shape = cardShape,
                material = LiquidMaterial.ControlCenterModuleGlass,
                controller = effectController,
            )
            .clip(cardShape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MonthCalendarGrid(
            yearMonth = firstMonth,
            today = today,
            onHeaderClick = { monthOffset -= 1 },
            modifier = Modifier.weight(1f),
        )

        MonthCalendarGrid(
            yearMonth = secondMonth,
            today = today,
            onHeaderClick = { monthOffset += 1 },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MonthCalendarGrid(
    yearMonth: YearMonth,
    today: LocalDate,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthTitle = yearMonth.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).uppercase()
    val daysInMonth = yearMonth.lengthOfMonth()
    // Sunday = 0 .. Saturday = 6
    val firstDayOfWeekIndex = yearMonth.atDay(1).dayOfWeek.value % 7
    val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = monthTitle,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clickable(onClick = onHeaderClick)
                .padding(bottom = 8.dp),
        )

        // Day-of-week header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dayHeaders.forEach { d ->
                Text(
                    text = d,
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(19.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val totalCells = firstDayOfWeekIndex + daysInMonth
        val rowCount = (totalCells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until rowCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - firstDayOfWeekIndex + 1
                        if (dayNum in 1..daysInMonth) {
                            val isToday = yearMonth.year == today.year &&
                                yearMonth.month == today.month &&
                                dayNum == today.dayOfMonth
                            Box(
                                modifier = Modifier
                                    .size(19.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) Color.White else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    color = if (isToday) Color(0xFF1F2421) else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom row from `00:00`–`00:48` of `96942.mp4`:
 * - Spotify shortcut tile
 * - Threads shortcut tile
 * - Analog Clock + Date Pill Widget (`Tue, 13 Oct`)
 */
@Composable
fun HomeAppIconsAndAnalogClockRow(
    backdrop: LiquidBackdrop,
    effectController: LiquidEffectController,
    onTriggerMusicDemo: () -> Unit,
    onTriggerNotificationDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentTickMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTickMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val nowDate = remember(currentTickMs / 60_000L) { LocalDate.now() }
    val nowTime = remember(currentTickMs) { LocalTime.now() }
    val dayOfWeekShort = nowDate.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
    val monthShort = nowDate.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Spotify App Icon (Tap triggers Music Island)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onTriggerMusicDemo),
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF121417)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(38.dp)) {
                    drawCircle(color = Color(0xFF1ED760))
                    drawArc(
                        color = Color.Black,
                        startAngle = -125f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.2f, size.height * 0.28f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.5f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Spotify", color = Color(0xFF1F2421), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }

        // Threads App Icon (Tap triggers Notification Island)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onTriggerNotificationDemo),
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF121417)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "@",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Threads", color = Color(0xFF1F2421), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }

        // Analog Clock + Date Capsule Widget ("Tue, 13 Oct")
        val pillShape = RoundedCornerShape(36.dp)
        Row(
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
                .liquidGlassSurface(
                    backdrop = backdrop,
                    shape = pillShape,
                    material = LiquidMaterial.ControlCenterModuleGlass,
                    controller = effectController,
                )
                .clip(pillShape)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnalogClockFace(
                time = nowTime,
                modifier = Modifier.size(62.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "$dayOfWeekShort,",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${nowDate.dayOfMonth} $monthShort",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AnalogClockFace(
    time: LocalTime,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.48f
        drawCircle(color = Color(0xFF232625), radius = r, center = center)

        // 12 tick marks
        for (i in 0 until 12) {
            val angle = (i * 30.0 - 90.0) * PI / 180.0
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            val inner = if (i % 3 == 0) r * 0.74f else r * 0.82f
            drawLine(
                color = Color.White.copy(alpha = if (i % 3 == 0) 0.9f else 0.45f),
                start = Offset(center.x + c * inner, center.y + s * inner),
                end = Offset(center.x + c * (r * 0.92f), center.y + s * (r * 0.92f)),
                strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Hour hand
        val hourAngle = (((time.hour % 12) + time.minute / 60.0) * 30.0 - 90.0) * PI / 180.0
        drawLine(
            color = Color.White,
            start = center,
            end = Offset(
                center.x + cos(hourAngle).toFloat() * (r * 0.48f),
                center.y + sin(hourAngle).toFloat() * (r * 0.48f),
            ),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Minute hand
        val minAngle = ((time.minute + time.second / 60.0) * 6.0 - 90.0) * PI / 180.0
        drawLine(
            color = Color.White,
            start = center,
            end = Offset(
                center.x + cos(minAngle).toFloat() * (r * 0.68f),
                center.y + sin(minAngle).toFloat() * (r * 0.68f),
            ),
            strokeWidth = 2.0.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Red second hand
        val secAngle = (time.second * 6.0 - 90.0) * PI / 180.0
        drawLine(
            color = Color(0xFFFF453A),
            start = center,
            end = Offset(
                center.x + cos(secAngle).toFloat() * (r * 0.78f),
                center.y + sin(secAngle).toFloat() * (r * 0.78f),
            ),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(color = Color(0xFFFF453A), radius = 2.5.dp.toPx(), center = center)
    }
}
