package com.xiao.androidisland.liquidisland.engine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiao.androidisland.core.model.BiometricAuthType
import com.xiao.androidisland.core.model.IslandActivityState
import com.xiao.androidisland.core.model.IslandNotificationItem
import com.xiao.androidisland.core.model.LiquidIslandSettings
import com.xiao.androidisland.core.model.MediaTrackState
import com.xiao.androidisland.core.model.NavigationActivityState
import com.xiao.androidisland.core.model.RingerModeType
import com.xiao.androidisland.core.model.SecondaryBubbleType
import com.xiao.androidisland.core.model.TimerActivityState
import com.xiao.androidisland.liquidisland.renderer.LiquidBackdrop
import com.xiao.androidisland.liquidisland.renderer.LiquidEffectController
import com.xiao.androidisland.liquidisland.renderer.LiquidMaterial
import com.xiao.androidisland.liquidisland.renderer.liquidGlassSurface
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Coupled Dynamic Island Kinematics State shared between [DynamicIslandComposable] and
 * [com.xiao.androidisland.liquidisland.statusbar.DynamicIosStatusBar] on the exact same frame timeline.
 */
@Stable
class CoupledIslandKinematicsState(
    val screenWidthDp: Float = 360f,
) {
    val widthAnim = Animatable(120f)
    val heightAnim = Animatable(32f)
    val cornerRadiusAnim = Animatable(16f)
    val splitBubbleSizeAnim = Animatable(0f)
    val splitBubbleGapAnim = Animatable(0f)

    var leftEdgeVelocityDpPerSec by mutableFloatStateOf(0f)
        private set
    var rightEdgeVelocityDpPerSec by mutableFloatStateOf(0f)
        private set

    val centerXDp: Float
        get() = screenWidthDp * 0.5f

    val totalOccupiedWidthDp: Float
        get() {
            val mainW = widthAnim.value
            val bubbleW = splitBubbleSizeAnim.value
            val gapW = splitBubbleGapAnim.value
            return if (bubbleW > 1f) mainW + gapW + bubbleW else mainW
        }

    val leftEdgeDp: Float
        get() = centerXDp - (totalOccupiedWidthDp * 0.5f)

    val rightEdgeDp: Float
        get() = centerXDp + (totalOccupiedWidthDp * 0.5f)

    suspend fun animateToState(
        state: IslandActivityState,
        settings: LiquidIslandSettings,
    ) {
        val speed = settings.animationSpeedMultiplier.coerceIn(0.5f, 2.0f)
        val stiffness = 380f * speed
        val damping = 0.72f

        val targetW = state.widthDp * settings.islandWidthScale
        val targetH = state.heightDp * settings.islandHeightScale
        val targetR = state.cornerRadiusDp * settings.islandHeightScale
        val targetBubbleSize = if (state.hasDetachedBubble) state.detachedBubbleSizeDp else 0f
        val targetBubbleGap = if (state.hasDetachedBubble) state.detachedBubbleGapDp else 0f

        kotlinx.coroutines.coroutineScope {
            launch {
                widthAnim.animateTo(
                    targetValue = targetW,
                    animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
                ) {
                    // Instantaneous edge velocity in dp/s: left edge moves opposite to +dW/2
                    leftEdgeVelocityDpPerSec = -velocity * 0.5f
                    rightEdgeVelocityDpPerSec = velocity * 0.5f
                }
                leftEdgeVelocityDpPerSec = 0f
                rightEdgeVelocityDpPerSec = 0f
            }
            launch {
                heightAnim.animateTo(
                    targetValue = targetH,
                    animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
                )
            }
            launch {
                cornerRadiusAnim.animateTo(
                    targetValue = targetR,
                    animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
                )
            }
            launch {
                splitBubbleSizeAnim.animateTo(
                    targetValue = targetBubbleSize,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = stiffness),
                )
            }
            launch {
                splitBubbleGapAnim.animateTo(
                    targetValue = targetBubbleGap,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = stiffness),
                )
            }
        }
    }
}

@Composable
fun DynamicIslandComposable(
    activityState: IslandActivityState,
    settings: LiquidIslandSettings,
    kinematics: CoupledIslandKinematicsState,
    backdrop: LiquidBackdrop,
    effectController: LiquidEffectController,
    onTapIsland: () -> Unit,
    onLongPressIsland: () -> Unit,
    onMediaPrev: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaNext: () -> Unit,
    onTimerLap: () -> Unit,
    onTimerCancel: () -> Unit,
    onEndNavigationTrip: () -> Unit,
    onDismissNotification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(activityState, settings.islandWidthScale, settings.islandHeightScale, settings.animationSpeedMultiplier) {
        kinematics.animateToState(activityState, settings)
    }

    val currentW = kinematics.widthAnim.value.dp
    val currentH = kinematics.heightAnim.value.dp
    val currentRadius = kinematics.cornerRadiusAnim.value.dp
    val bubbleSize = kinematics.splitBubbleSizeAnim.value.dp
    val bubbleGap = kinematics.splitBubbleGapAnim.value.dp

    val islandShape = RoundedCornerShape(currentRadius)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .offset(
                x = settings.islandHorizontalOffsetDp.dp,
                y = settings.islandVerticalOffsetDp.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Main Dynamic Island Capsule / Expanded Card
        Box(
            modifier = Modifier
                .width(currentW)
                .height(currentH)
                .liquidGlassSurface(
                    backdrop = backdrop,
                    shape = islandShape,
                    material = LiquidMaterial.DynamicIslandObsidianGlass,
                    controller = effectController,
                )
                .clip(islandShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTapIsland,
                )
                .pointerInput(activityState) {
                    var dragDx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragDx = 0f },
                        onHorizontalDrag = { _, delta ->
                            dragDx += delta
                            if (dragDx > 95f) {
                                onLongPressIsland()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = activityState,
                transitionSpec = {
                    fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(130, easing = FastOutSlowInEasing))
                },
                label = "IslandContentTransition",
            ) { state ->
                when (state) {
                    IslandActivityState.CompactIdle -> {
                        CompactIdleIslandContent()
                    }
                    is IslandActivityState.CompactNotification -> {
                        CompactNotificationIslandContent(state.item)
                    }
                    is IslandActivityState.ExpandedNotification -> {
                        ExpandedNotificationIslandContent(
                            item = state.item,
                            onActionClick = onDismissNotification,
                        )
                    }
                    is IslandActivityState.CompactMusic -> {
                        CompactMusicIslandContent(state.track)
                    }
                    is IslandActivityState.SplitDualActivity -> {
                        CompactSplitMainPillContent(state.track, state.secondaryType)
                    }
                    is IslandActivityState.ExpandedMusic -> {
                        ExpandedMusicIslandContent(
                            track = state.track,
                            isAlbumGradientEnabled = settings.isAlbumGradientEnabled,
                            onPrev = onMediaPrev,
                            onPlayPause = onMediaPlayPause,
                            onNext = onMediaNext,
                        )
                    }
                    is IslandActivityState.CompactTimer -> {
                        CompactTimerIslandContent(state.timer)
                    }
                    is IslandActivityState.ExpandedTimer -> {
                        ExpandedTimerIslandContent(
                            timer = state.timer,
                            onLap = onTimerLap,
                            onCancel = onTimerCancel,
                        )
                    }
                    is IslandActivityState.CompactNavigation -> {
                        CompactNavigationIslandContent(state.nav)
                    }
                    is IslandActivityState.ExpandedNavigation -> {
                        ExpandedNavigationIslandContent(
                            nav = state.nav,
                            onEndTrip = onEndNavigationTrip,
                        )
                    }
                    is IslandActivityState.RingerAlert -> {
                        WideRingerAlertIslandContent(state.ringerMode)
                    }
                    is IslandActivityState.ChargingAlert -> {
                        WideChargingAlertIslandContent(state.batteryPercent)
                    }
                    is IslandActivityState.BiometricAuthAlert -> {
                        BiometricAuthIslandContent(state.authType, state.isAuthenticated)
                    }
                }
            }
        }

        // Detached Secondary Circular Activity Bubble + Metaball Liquid Bridge (00:09, 00:25, 00:49 in 96942.mp4)
        if (bubbleSize > 2.dp) {
            // Render organic liquid necking bridge when gap is small (< 10dp)
            Box(
                modifier = Modifier
                    .width(bubbleGap)
                    .height(currentH),
                contentAlignment = Alignment.Center,
            ) {
                if (bubbleGap < 9.dp) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val bridgeH = size.height * (1f - (bubbleGap.toPx() / 28f)).coerceIn(0f, 0.55f)
                        if (bridgeH > 1f) {
                            drawRect(
                                color = Color(0xEC08090C),
                                topLeft = Offset(0f, (size.height - bridgeH) * 0.5f),
                                size = Size(size.width, bridgeH),
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .liquidGlassSurface(
                        backdrop = backdrop,
                        shape = CircleShape,
                        material = LiquidMaterial.DynamicIslandObsidianGlass,
                        controller = effectController,
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onLongPressIsland),
                contentAlignment = Alignment.Center,
            ) {
                val splitState = activityState as? IslandActivityState.SplitDualActivity
                when (splitState?.secondaryType) {
                    SecondaryBubbleType.TIMER -> {
                        OrangeClockGlyph(modifier = Modifier.size(18.dp))
                    }
                    SecondaryBubbleType.DOWNLOAD -> {
                        DownloadCircleGlyph(modifier = Modifier.size(18.dp))
                    }
                    else -> {
                        AnimatedEqualizerWaveform(
                            barColor = Color(0xFF6EE7B7),
                            isPlaying = true,
                            modifier = Modifier.size(width = 16.dp, height = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactIdleIslandContent() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Subtle camera lens reflection dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF141824)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B3A67)),
            )
        }
    }
}

@Composable
private fun CompactNotificationIslandContent(item: IslandNotificationItem) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadCircleGlyph(modifier = Modifier.size(20.dp))
        Text(
            text = item.appName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = "⌄",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ExpandedNotificationIslandContent(
    item: IslandNotificationItem,
    onActionClick: () -> Unit,
) {
    if (item.isCompactBanner) {
        // 00:10 Metrolist "Update available / 13.7.0" Banner
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF263238)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "♪", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.appName} • ${item.timeLabel}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.body,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                )
            }
        }
    } else {
        // 00:04 Telegram Full Notification Card with "Reply" and "Mark as read" buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD97706)),
                    contentAlignment = Alignment.Center,
                ) {
                    DownloadCircleGlyph(modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${item.appName} • ${item.timeLabel}",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.5.sp,
                    )
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.body,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item.actionLabels.forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable(onClick = onActionClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMusicIslandContent(track: MediaTrackState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumCoverThumb(
            primary = track.primaryColor,
            secondary = track.secondaryColor,
            modifier = Modifier.size(20.dp),
        )
        AnimatedEqualizerWaveform(
            barColor = Color(0xFFF9A8D4),
            isPlaying = track.isPlaying,
            modifier = Modifier.size(width = 20.dp, height = 13.dp),
        )
    }
}

@Composable
private fun CompactSplitMainPillContent(
    track: MediaTrackState,
    secondaryType: SecondaryBubbleType,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumCoverThumb(
            primary = track.primaryColor,
            secondary = track.secondaryColor,
            modifier = Modifier.size(20.dp),
        )
        if (secondaryType == SecondaryBubbleType.TIMER) {
            AnimatedEqualizerWaveform(
                barColor = Color.White,
                isPlaying = track.isPlaying,
                modifier = Modifier.size(width = 18.dp, height = 12.dp),
            )
        } else {
            OrangeClockGlyph(modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ExpandedMusicIslandContent(
    track: MediaTrackState,
    isAlbumGradientEnabled: Boolean,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val bgBrush = if (isAlbumGradientEnabled) {
        Brush.radialGradient(
            colors = listOf(
                track.primaryColor.copy(alpha = 0.52f),
                track.secondaryColor.copy(alpha = 0.25f),
                Color.Transparent,
            ),
            center = Offset(120f, 90f),
            radius = 420f,
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top Row: Album Cover (54dp) + Title/Artist + Animated Equalizer Waveform (00:13 & 01:08 in 96942.mp4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumCoverThumb(
                primary = track.primaryColor,
                secondary = track.secondaryColor,
                modifier = Modifier.size(54.dp),
                cornerRadiusDp = 14f,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            AnimatedEqualizerWaveform(
                barColor = Color.White.copy(alpha = 0.85f),
                isPlaying = track.isPlaying,
                modifier = Modifier.size(width = 28.dp, height = 20.dp),
            )
        }

        // Middle Row: Elapsed "00:53" + Progress Track + Remaining "-02:28"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = track.formattedElapsed,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.24f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(track.progressFraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White),
                )
            }
            Text(
                text = track.formattedRemaining,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Bottom Row: Transport Controls (<<   || / |>   >>) + AirPlay Output Icon on Right
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(38.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "⏮",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable(onClick = onPrev),
                )
                Text(
                    text = if (track.isPlaying) "⏸" else "▶",
                    color = Color.White,
                    fontSize = 26.sp,
                    modifier = Modifier.clickable(onClick = onPlayPause),
                )
                Text(
                    text = "⏭",
                    color = Color.White,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable(onClick = onNext),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                AirPlayOutputGlyph(modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CompactTimerIslandContent(timer: TimerActivityState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrangeClockGlyph(modifier = Modifier.size(18.dp))
        Text(
            text = timer.formattedTime,
            color = Color(0xFFFF9F0A),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ExpandedTimerIslandContent(
    timer: TimerActivityState,
    onLap: () -> Unit,
    onCancel: () -> Unit,
) {
    // Matches 00:28 in 96942.mp4: Orange "Lap" circle + Dark "X" circle on left, huge "00:14" orange digits on right
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x4DFF9F0A))
                    .clickable(onClick = onLap),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Lap",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✕",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            text = timer.formattedTime,
            color = Color(0xFFFF9F0A),
            fontSize = 38.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun CompactNavigationIslandContent(nav: NavigationActivityState) {
    // Matches 00:39 & 00:47 in 96942.mp4: Up arrow on left, "3.6 km" on right
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "↑",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = nav.distanceText,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ExpandedNavigationIslandContent(
    nav: NavigationActivityState,
    onEndTrip: () -> Unit,
) {
    // Matches 00:44 in 96942.mp4: Up arrow + "3.6 km" + "Arrive 7:21 PM" + red "End Trip" pill button
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "↑",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = nav.distanceText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = nav.instructionText,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.5.sp,
                )
            }
        }

        Text(
            text = nav.arrivalTimeText,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 12.sp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color(0x38FF3B30))
                .clickable(onClick = onEndTrip),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "End Trip",
                color = Color(0xFFFF453A),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun WideRingerAlertIslandContent(ringerMode: RingerModeType) {
    // Matches 00:50 (Ring), 00:51 (Vibrate orange), 00:54 (Silent red) in 96942.mp4
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (ringerMode) {
            RingerModeType.RING -> {
                Text(text = "🔔", color = Color.White, fontSize = 15.sp)
                Text(
                    text = "Ring",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            RingerModeType.VIBRATE -> {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF9500)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "📳", color = Color.White, fontSize = 13.sp)
                }
                Text(
                    text = "Vibrate",
                    color = Color(0xFFFF9500),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            RingerModeType.SILENT -> {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF3B30)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🔕", color = Color.White, fontSize = 13.sp)
                }
                Text(
                    text = "Silent",
                    color = Color(0xFFFF3B30),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun WideChargingAlertIslandContent(batteryPercent: Int) {
    // Matches 01:03 in 96942.mp4: "Charging" on left, green "80%" + green battery capsule on right
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Charging",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$batteryPercent%",
                color = Color(0xFF34C759),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF34C759)),
            )
        }
    }
}

@Composable
private fun BiometricAuthIslandContent(
    authType: BiometricAuthType,
    isAuthenticated: Boolean,
) {
    // Matches 01:19..01:25 in 96942.mp4: Lock icon on left, Face ID / Fingerprint / green checkmark on right
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isAuthenticated) "🔓" else "🔒",
            color = Color.White,
            fontSize = 15.sp,
        )
        Text(
            text = if (isAuthenticated) {
                "✓"
            } else if (authType == BiometricAuthType.FACE_ID) {
                "Face ID"
            } else {
                "Touch ID"
            },
            color = if (isAuthenticated) Color(0xFF34C759) else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun AlbumCoverThumb(
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Float = 6f,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primary, secondary),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.55f)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = size.minDimension * 0.22f,
            )
        }
    }
}

@Composable
fun AnimatedEqualizerWaveform(
    barColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "WavePhase",
    )

    Canvas(modifier = modifier) {
        val barCount = 6
        val barW = size.width / (barCount * 1.8f)
        val gap = (size.width - barW * barCount) / (barCount - 1).coerceAtLeast(1)
        for (i in 0 until barCount) {
            val frac = if (isPlaying) {
                0.28f + 0.72f * ((sin(phase + i * 0.95f) + 1f) * 0.5f)
            } else {
                0.25f
            }
            val bh = size.height * frac
            val x = i * (barW + gap)
            val y = (size.height - bh) * 0.5f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barW, bh),
                cornerRadius = CornerRadius(barW * 0.5f, barW * 0.5f),
            )
        }
    }
}

@Composable
fun OrangeClockGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.44f
        drawCircle(
            color = Color(0xFFFF9F0A),
            radius = r,
            style = Stroke(width = size.minDimension * 0.12f),
        )
        drawLine(
            color = Color(0xFFFF9F0A),
            start = center,
            end = Offset(center.x + r * 0.45f, center.y - r * 0.45f),
            strokeWidth = size.minDimension * 0.12f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun DownloadCircleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.45f
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = r,
            style = Stroke(width = size.minDimension * 0.10f),
        )
        drawLine(
            color = Color.White,
            start = Offset(center.x, center.y - r * 0.45f),
            end = Offset(center.x, center.y + r * 0.35f),
            strokeWidth = size.minDimension * 0.11f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun AirPlayOutputGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawArc(
            color = Color.White,
            startAngle = -140f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.76f),
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round),
        )
        val triangle = Path().apply {
            moveTo(w * 0.50f, h * 0.48f)
            lineTo(w * 0.24f, h * 0.88f)
            lineTo(w * 0.76f, h * 0.88f)
            close()
        }
        drawPath(triangle, color = Color.White)
    }
}
