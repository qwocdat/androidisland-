package com.droidbubble.app.island

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CustomStatusBarOverlay(
    settings: IslandSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val islandBoundsPx by IslandStateManager.islandHorizontalBoundsPx.collectAsState()
    val isExpanded by IslandStateManager.isExpanded.collectAsState()

    var currentTimeText by remember(settings.statusBarClock24h) {
        val pattern = if (settings.statusBarClock24h) "HH:mm" else "hh:mm"
        mutableStateOf(SimpleDateFormat(pattern, Locale.getDefault()).format(Date()))
    }
    LaunchedEffect(settings.statusBarClock24h) {
        val pattern = if (settings.statusBarClock24h) "HH:mm" else "hh:mm"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        while (true) {
            currentTimeText = sdf.format(Date())
            delay(1000L)
        }
    }

    var batteryPct by remember { mutableIntStateOf(86) }
    var isCharging by remember { mutableStateOf(false) }
    var ringerMode by remember { mutableIntStateOf(AudioManager.RINGER_MODE_NORMAL) }
    var isWifiConnected by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        fun refreshTelemetry(batteryIntent: Intent? = null) {
            val bIntent = batteryIntent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (bIntent != null) {
                val level = bIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 86)
                val scale = bIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                batteryPct = ((level * 100f) / scale).roundToInt().coerceIn(1, 100)
                val status = bIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = bIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL ||
                    plugged != 0
            }
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                ringerMode = am.ringerMode
            }
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != false
        }

        refreshTelemetry()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> refreshTelemetry(intent)
                    else -> refreshTelemetry()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
        }
        runCatching {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val marginPx = with(density) { settings.statusBarOcclusionMarginDp.dp.roundToPx() }
    val scale = settings.statusBarScale.coerceIn(0.80f, 1.25f)

    val modeUpper = settings.statusBarBgMode.uppercase()
    val bgBrush = when (modeUpper) {
        "TRANSPARENT" -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        "GLASS" -> Brush.verticalGradient(
            listOf(
                Color(0xF2050507),
                Color(0xCC050507),
                Color.Transparent
            )
        )
        else -> Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.Black,
                0.82f to Color.Black,
                0.94f to Color.Black.copy(alpha = 0.92f),
                1.00f to Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(settings.statusBarHeightDp.coerceIn(28, 56).dp)
            .background(bgBrush)
            .offset(y = settings.statusBarTopOffsetDp.dp)
            .padding(horizontal = settings.statusBarHorizontalPaddingDp.coerceIn(8, 36).dp),
        contentAlignment = Alignment.Center
    ) {
        if (modeUpper == "HIDE_ALL") {
            return@Box
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT CLUSTER: Clock ("16:07"), Custom Carrier/Label, Silent/Vibrate Bell
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((5f * scale).dp)
            ) {
                if (settings.statusBarShowClock) {
                    StatusBarAutoOccludingItem(
                        islandBoundsPx = islandBoundsPx,
                        isIslandFullyExpanded = isExpanded,
                        occlusionMarginPx = marginPx
                    ) {
                        Text(
                            text = if (settings.statusBarCustomLabel.isNotBlank()) {
                                "$currentTimeText ${settings.statusBarCustomLabel}"
                            } else {
                                currentTimeText
                            },
                            color = Color.White,
                            fontSize = (14.5f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }

                if (settings.statusBarShowRinger && ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                    StatusBarAutoOccludingItem(
                        islandBoundsPx = islandBoundsPx,
                        isIslandFullyExpanded = isExpanded,
                        occlusionMarginPx = marginPx
                    ) {
                        Icon(
                            imageVector = if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                                Icons.Default.Vibration
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size((14.5f * scale).dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // RIGHT CLUSTER: Dual-SIM Cellular Bars + Wi-Fi + Battery Capsule (Each item auto-hides individually when overlapped by Island!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((5.5f * scale).dp)
            ) {
                if (settings.statusBarShowSignal) {
                    // SIM 1 Signal Bars
                    StatusBarAutoOccludingItem(
                        islandBoundsPx = islandBoundsPx,
                        isIslandFullyExpanded = isExpanded,
                        occlusionMarginPx = marginPx
                    ) {
                        StatusBarSignalBarsIcon(
                            activeBars = 4,
                            scale = scale
                        )
                    }
                    // SIM 2 Signal Bars (matching dual-SIM status bar in 96995.mp4)
                    StatusBarAutoOccludingItem(
                        islandBoundsPx = islandBoundsPx,
                        isIslandFullyExpanded = isExpanded,
                        occlusionMarginPx = marginPx
                    ) {
                        StatusBarSignalBarsIcon(
                            activeBars = 4,
                            scale = scale
                        )
                    }
                }

                if (settings.statusBarShowWifi) {
                    StatusBarAutoOccludingItem(
                        islandBoundsPx = islandBoundsPx,
                        isIslandFullyExpanded = isExpanded,
                        occlusionMarginPx = marginPx
                    ) {
                        if (isWifiConnected) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size((15.5f * scale).dp)
                            )
                        } else {
                            Text(
                                text = "5G",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = (11.5f * scale).sp
                            )
                        }
                    }
                }

                if (settings.statusBarShowBattery) {
                    StatusBarAutoOccludingItem(
                        islandBoundsPx = islandBoundsPx,
                        isIslandFullyExpanded = isExpanded,
                        occlusionMarginPx = marginPx
                    ) {
                        StatusBarBatteryIndicator(
                            batteryPct = batteryPct,
                            isCharging = isCharging,
                            style = settings.statusBarBatteryStyle,
                            scale = scale
                        )
                    }
                }
            }
        }
    }
}

/**
 * Automatically hides this specific status bar object when the Dynamic Island widens, splits, or expands
 * over its horizontal screen coordinates, and smoothly restores it as soon as the Island shrinks back.
 */
@Composable
private fun StatusBarAutoOccludingItem(
    islandBoundsPx: IntRange,
    isIslandFullyExpanded: Boolean,
    occlusionMarginPx: Int,
    content: @Composable () -> Unit
) {
    var itemLeftPx by remember { mutableIntStateOf(0) }
    var itemRightPx by remember { mutableIntStateOf(0) }

    val isOverlapped = remember(islandBoundsPx, itemLeftPx, itemRightPx, isIslandFullyExpanded, occlusionMarginPx) {
        if (isIslandFullyExpanded) {
            true
        } else if (islandBoundsPx.isEmpty() || itemRightPx <= itemLeftPx) {
            false
        } else {
            val expandedLeft = islandBoundsPx.first - occlusionMarginPx
            val expandedRight = islandBoundsPx.last + occlusionMarginPx
            itemRightPx > expandedLeft && itemLeftPx < expandedRight
        }
    }

    val visibilityProgress by animateFloatAsState(
        targetValue = if (isOverlapped) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "statusBarItemOcclusion"
    )

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                itemLeftPx = bounds.left.roundToInt()
                itemRightPx = bounds.right.roundToInt()
            }
            .graphicsLayer {
                alpha = visibilityProgress
                val s = 0.72f + 0.28f * visibilityProgress
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun StatusBarSignalBarsIcon(
    activeBars: Int,
    scale: Float
) {
    Canvas(
        modifier = Modifier
            .width((15f * scale).dp)
            .height((11f * scale).dp)
    ) {
        val barCount = 4
        val spacing = size.width * 0.10f
        val barW = (size.width - spacing * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val frac = 0.34f + (i / (barCount - 1f)) * 0.66f
            val barH = size.height * frac
            val left = i * (barW + spacing)
            val top = size.height - barH
            drawRoundRect(
                color = if (i < activeBars) Color.White else Color.White.copy(alpha = 0.32f),
                topLeft = Offset(left, top),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

@Composable
private fun StatusBarBatteryIndicator(
    batteryPct: Int,
    isCharging: Boolean,
    style: String,
    scale: Float
) {
    val greenColor = Color(0xFF30D158)
    when (style.uppercase()) {
        "TEXT_ONLY" -> {
            Text(
                text = if (isCharging) "$batteryPct%⚡" else "$batteryPct%",
                color = if (isCharging) greenColor else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (13f * scale).sp
            )
        }
        "IOS_OUTLINE" -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width((25f * scale).dp)
                        .height((12.5f * scale).dp)
                        .clip(RoundedCornerShape((4f * scale).dp))
                        .background(Color.White.copy(alpha = 0.32f))
                        .padding(1.2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((batteryPct / 100f).coerceIn(0.15f, 1f))
                            .clip(RoundedCornerShape((2.8f * scale).dp))
                            .background(if (isCharging) greenColor else Color.White)
                    )
                    Text(
                        text = "$batteryPct",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (8.5f * scale).sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Box(
                    modifier = Modifier
                        .width((1.6f * scale).dp)
                        .height((4.5f * scale).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(alpha = 0.55f))
                )
            }
        }
        else -> {
            // Default HyperOS / iOS Green Battery Capsule ("86 ⚡" in 96995.mp4)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height((15f * scale).dp)
                        .clip(RoundedCornerShape((5f * scale).dp))
                        .background(
                            if (isCharging || batteryPct >= 20) greenColor else Color(0xFFFF453A)
                        )
                        .padding(horizontal = (4.5f * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "$batteryPct",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (9.5f * scale).sp
                        )
                        if (isCharging) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size((9.5f * scale).dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .width((1.8f * scale).dp)
                        .height((5f * scale).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            (if (isCharging || batteryPct >= 20) greenColor else Color(0xFFFF453A)).copy(alpha = 0.85f)
                        )
                )
            }
        }
    }
}
