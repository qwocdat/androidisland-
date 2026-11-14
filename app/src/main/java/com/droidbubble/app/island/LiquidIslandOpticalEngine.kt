package com.droidbubble.app.island

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

// =========================================================================================
// 1. TRANSITION & VELOCITY MOTION BLUR ENGINE (96942.mp4 00:05, 00:12, 00:36, 00:45)
// =========================================================================================

private const val AGSL_DIRECTIONAL_VELOCITY_BLUR_SHADER = """
uniform shader content;
uniform float2 resolution;
uniform float2 velocityPx;

half4 main(float2 fragCoord) {
    float speed = length(velocityPx);
    if (speed < 0.35) {
        return content.eval(fragCoord);
    }
    float2 dir = velocityPx / max(speed, 0.0001);
    float blurSpan = clamp(speed * 0.45, 0.0, 28.0);
    float2 stepVec = dir * (blurSpan / 4.0);

    half4 sum = half4(0.0);
    sum += content.eval(clamp(fragCoord - stepVec * 4.0, float2(0.0), resolution)) * 0.028;
    sum += content.eval(clamp(fragCoord - stepVec * 3.0, float2(0.0), resolution)) * 0.066;
    sum += content.eval(clamp(fragCoord - stepVec * 2.0, float2(0.0), resolution)) * 0.124;
    sum += content.eval(clamp(fragCoord - stepVec * 1.0, float2(0.0), resolution)) * 0.179;
    sum += content.eval(fragCoord) * 0.206;
    sum += content.eval(clamp(fragCoord + stepVec * 1.0, float2(0.0), resolution)) * 0.179;
    sum += content.eval(clamp(fragCoord + stepVec * 2.0, float2(0.0), resolution)) * 0.124;
    sum += content.eval(clamp(fragCoord + stepVec * 3.0, float2(0.0), resolution)) * 0.066;
    sum += content.eval(clamp(fragCoord + stepVec * 4.0, float2(0.0), resolution)) * 0.028;
    return sum;
}
"""

@Composable
fun rememberIslandVelocityVector(
    currentWidthDp: Dp,
    currentHeightDp: Dp,
    enabled: Boolean
): Offset {
    val density = LocalDensity.current
    var velocity by remember { mutableStateOf(Offset.Zero) }
    var prevW by remember { mutableFloatStateOf(with(density) { currentWidthDp.toPx() }) }
    var prevH by remember { mutableFloatStateOf(with(density) { currentHeightDp.toPx() }) }

    LaunchedEffect(currentWidthDp, currentHeightDp, enabled) {
        if (!enabled) {
            velocity = Offset.Zero
            return@LaunchedEffect
        }
        val targetW = with(density) { currentWidthDp.toPx() }
        val targetH = with(density) { currentHeightDp.toPx() }
        val dw = targetW - prevW
        val dh = targetH - prevH
        prevW = targetW
        prevH = targetH
        if (abs(dw) > 0.4f || abs(dh) > 0.4f) {
            velocity = Offset(dw * 1.6f, dh * 1.6f)
            withFrameNanos { }
            delay(32)
            velocity = Offset(velocity.x * 0.35f, velocity.y * 0.35f)
            delay(32)
            velocity = Offset.Zero
        } else {
            velocity = Offset.Zero
        }
    }
    return velocity
}

@Composable
fun Modifier.liquidVelocityMotionBlur(
    velocityPx: Offset,
    enabled: Boolean,
    expandProgress: Float = 0f
): Modifier {
    // In Dynamic Motion 1.2 (96942.mp4 at 00:05, 00:12, 00:45), cards blur smoothly mid-transition
    val midTransitionBlur = if (expandProgress in 0.04f..0.88f) {
        sin(((expandProgress - 0.04f) / 0.84f) * PI.toFloat()).coerceIn(0f, 1f) * 14f
    } else 0f

    val speed = hypot(velocityPx.x, velocityPx.y)
    if (!enabled && midTransitionBlur < 0.5f) return this
    if (speed < 0.5f && midTransitionBlur < 0.5f) return this

    val shaderHolder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(AGSL_DIRECTIONAL_VELOCITY_BLUR_SHADER) }.getOrNull()
        } else null
    }

    return this.graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && midTransitionBlur >= 1.0f) {
            runCatching {
                renderEffect = AndroidRenderEffect.createBlurEffect(
                    midTransitionBlur,
                    midTransitionBlur * 1.15f,
                    Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderHolder != null && size.width > 1f && size.height > 1f && speed >= 0.5f) {
            runCatching {
                shaderHolder.setFloatUniform("resolution", size.width, size.height)
                shaderHolder.setFloatUniform("velocityPx", velocityPx.x, velocityPx.y)
                renderEffect = AndroidRenderEffect
                    .createRuntimeShaderEffect(shaderHolder, "content")
                    .asComposeRenderEffect()
            }
        }
    }
}

// =========================================================================================
// 2. TRUE iOS DYNAMIC ISLAND CONTAINER (Pure #000000 Black + Album Gradient for Media)
// =========================================================================================

@Composable
fun LiquidIslandOpticalBox(
    settings: IslandSettings,
    shape: Shape,
    accentColor: Color = Color.Transparent,
    isExpandedCard: Boolean = false,
    albumArtBitmap: Bitmap? = null,
    priority: IslandPriority? = null,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val showAlbumGradient = isExpandedCard &&
        priority == IslandPriority.MEDIA &&
        settings.blurredAlbumArtBackground &&
        albumArtBitmap != null &&
        !albumArtBitmap.isRecycled

    val borderColor = when {
        isExpandedCard && priority == IslandPriority.ALARM -> Color(0x44FF9F0A)
        showAlbumGradient -> Color.White.copy(alpha = 0.18f)
        else -> Color.White.copy(alpha = 0.13f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .border(width = 0.85.dp, color = borderColor, shape = shape),
        contentAlignment = contentAlignment
    ) {
        if (showAlbumGradient && albumArtBitmap != null) {
            // Exact Album Gradient mode from 01:07-01:17 & 01:19-01:25 of 96942.mp4
            val extracted = if (accentColor != Color.Transparent) accentColor else extractMediaAccentColor(albumArtBitmap)
            val topTone = if (extracted != Color.Transparent) extracted else Color(0xFF475569)
            Image(
                bitmap = albumArtBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 36.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .graphicsLayer {
                        scaleX = 1.45f
                        scaleY = 1.45f
                        alpha = 0.62f
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                topTone.copy(alpha = 0.48f),
                                Color(0xFF141418).copy(alpha = 0.72f),
                                Color(0xFF08080A).copy(alpha = 0.92f)
                            )
                        )
                    )
            )
        }
        content()
    }
}

// =========================================================================================
// 3. PURE BLACK SMOOTH-MINIMUM (smin) METABALL BRIDGE (96942.mp4 00:09, 00:23-00:27)
// =========================================================================================

@Composable
fun LiquidMetaballBridgeCanvas(
    mainPillWidth: Dp,
    mainPillHeight: Dp,
    mainPillCenterX: Dp,
    bubbleSize: Dp,
    bubbleCenterX: Dp,
    surfaceTension: Float,
    isLiquidGlass: Boolean,
    accentColor: Color,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.01f || mainPillWidth <= 4.dp) return
    val mainRightEdge = mainPillCenterX + (mainPillWidth / 2f)
    val mainLeftEdge = mainPillCenterX - (mainPillWidth / 2f)
    val bubbleLeftEdge = bubbleCenterX - (bubbleSize / 2f)
    val bubbleRightEdge = bubbleCenterX + (bubbleSize / 2f)

    val gapDp = if (bubbleCenterX >= mainPillCenterX) {
        bubbleLeftEdge - mainRightEdge
    } else {
        mainLeftEdge - bubbleRightEdge
    }

    val maxReachDp = (9f + surfaceTension * 2.2f).dp
    if (gapDp > maxReachDp || gapDp < (-bubbleSize * 0.65f)) return

    val bridgeStretch = (1f - (gapDp.value / maxReachDp.value)).coerceIn(0f, 1f)
    if (bridgeStretch <= 0.01f) return

    val canvasWidthDp = mainPillWidth + bubbleSize * 2f + 48.dp
    val canvasHeightDp = maxOf(mainPillHeight, bubbleSize)

    Canvas(
        modifier = modifier
            .width(canvasWidthDp)
            .height(canvasHeightDp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val mainCenterPx = Offset(cx + mainPillCenterX.toPx(), cy)
        val bubbleCenterPx = Offset(cx + bubbleCenterX.toPx(), cy)
        val mainHalfW = mainPillWidth.toPx() / 2f
        val mainHalfH = mainPillHeight.toPx() / 2f
        val bubbleR = bubbleSize.toPx() / 2f

        val isRight = bubbleCenterPx.x >= mainCenterPx.x
        val anchorMainX = if (isRight) mainCenterPx.x + mainHalfW - 4.dp.toPx() else mainCenterPx.x - mainHalfW + 4.dp.toPx()
        val anchorBubX = if (isRight) bubbleCenterPx.x - bubbleR + 4.dp.toPx() else bubbleCenterPx.x + bubbleR - 4.dp.toPx()
        val midX = (anchorMainX + anchorBubX) * 0.5f

        val anchorSpreadY = minOf(mainHalfH, bubbleR) * (0.50f + 0.36f * bridgeStretch)
        val waistHalfH = minOf(mainHalfH, bubbleR) * (0.12f + 0.62f * bridgeStretch * bridgeStretch)

        val neckPath = Path().apply {
            moveTo(anchorMainX, cy - anchorSpreadY)
            cubicTo(
                x1 = (anchorMainX + midX) * 0.5f,
                y1 = cy - waistHalfH,
                x2 = (midX + anchorBubX) * 0.5f,
                y2 = cy - waistHalfH,
                x3 = anchorBubX,
                y3 = cy - anchorSpreadY
            )
            lineTo(anchorBubX, cy + anchorSpreadY)
            cubicTo(
                x1 = (midX + anchorBubX) * 0.5f,
                y1 = cy + waistHalfH,
                x2 = (anchorMainX + midX) * 0.5f,
                y2 = cy + waistHalfH,
                x3 = anchorMainX,
                y3 = cy + anchorSpreadY
            )
            close()
        }

        // Pure #000000 black iOS metaball bridge
        drawPath(
            path = neckPath,
            color = Color.Black.copy(alpha = bridgeStretch)
        )
    }
}

// =========================================================================================
// 4. 3-PHASE EDGE-BASED STATUS BAR LIQUID ABSORPTION (96942.mp4 00:01, 00:50, 01:03)
// =========================================================================================

@Composable
fun LiquidStatusBarAbsorptionOverlay(
    compactWidth: Dp,
    compactHeight: Dp,
    restingWidth: Dp,
    surfaceTension: Float,
    accentColor: Color,
    isLiquidGlass: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLiquidGlass || compactWidth <= restingWidth + 6.dp) return
    val expansionDelta = (compactWidth - restingWidth).value.coerceAtLeast(0f)
    val phaseProgress = (expansionDelta / 68f).coerceIn(0f, 1f)
    if (phaseProgress <= 0.02f || phaseProgress >= 0.98f) return

    val bellIntensity = sin(phaseProgress * PI.toFloat()).coerceIn(0f, 1f)
    val dropletScaleX = 1.0f + 0.55f * bellIntensity
    val dropletScaleY = (1.0f - 0.28f * bellIntensity).coerceAtLeast(0.55f)

    val totalOverlayWidth = compactWidth + 36.dp
    Canvas(
        modifier = modifier
            .width(totalOverlayWidth)
            .height(compactHeight)
    ) {
        val cy = size.height / 2f
        val halfPillW = compactWidth.toPx() / 2f
        val centerX = size.width / 2f
        val leftEdgeX = centerX - halfPillW
        val rightEdgeX = centerX + halfPillW

        val pullDistPx = (1f - phaseProgress) * 12.dp.toPx()
        val dropW = 7.dp.toPx() * dropletScaleX
        val dropH = 5.dp.toPx() * dropletScaleY

        for (sign in intArrayOf(-1, 1)) {
            val rimX = if (sign < 0) leftEdgeX else rightEdgeX
            val dropCenterX = rimX + sign * pullDistPx
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.85f * bellIntensity),
                topLeft = Offset(dropCenterX - dropW / 2f, cy - dropH / 2f),
                size = Size(dropW, dropH),
                cornerRadius = CornerRadius(dropH / 2f, dropH / 2f)
            )
        }
    }
}

// =========================================================================================
// 5. iOS PRIVACY INDICATOR DOT (Camera Green / Microphone Orange)
// =========================================================================================

@Composable
fun LiquidPrivacyIndicatorDot(
    priority: IslandPriority,
    isCallActive: Boolean,
    isVoiceMemoActive: Boolean,
    isRecordingActive: Boolean,
    offsetPx: Int,
    modifier: Modifier = Modifier
) {
    val isMicInUse = isCallActive || isVoiceMemoActive || priority == IslandPriority.CALL || priority == IslandPriority.VOICE_MEMO
    val isCamOrScreenInUse = isRecordingActive || priority == IslandPriority.SCREEN_RECORDING
    if (!isMicInUse && !isCamOrScreenInUse) return

    val dotColor = if (isCamOrScreenInUse) Color(0xFF30D158) else Color(0xFFFF9500)
    Box(
        modifier = modifier
            .offset { IntOffset(x = -offsetPx, y = 0) }
            .size(6.dp)
            .clip(CircleShape)
            .background(dotColor)
    )
}

// =========================================================================================
// 6. iOS 6-BAR HARMONIC WAVEFORM VISUALIZER (96942.mp4 00:07, 00:13, 01:08)
// =========================================================================================

@Composable
fun LiquidSixBarVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    barCount: Int = 6,
    maxHeightDp: Dp = 18.dp,
    barWidthDp: Dp = 2.4.dp,
    modifier: Modifier = Modifier
) {
    val activeBarColor = if (accentColor != Color.Transparent) {
        lerp(Color.White, accentColor, 0.55f)
    } else {
        Color.White
    }

    if (!isPlaying) {
        // Static bars when paused (exact match to 00:15-00:18 in 96942.mp4)
        Row(
            modifier = modifier.height(maxHeightDp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.2.dp)
        ) {
            for (i in 0 until barCount) {
                Box(
                    modifier = Modifier
                        .width(barWidthDp)
                        .height(maxHeightDp * 0.42f)
                        .clip(RoundedCornerShape(barWidthDp / 2f))
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "ios_six_bar_eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 740, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eqPhase"
    )

    val phaseOffsets = remember { floatArrayOf(0.0f, 1.25f, 2.45f, 0.85f, 2.05f, 3.20f) }
    val ampScale = remember { floatArrayOf(0.55f, 0.92f, 0.78f, 1.00f, 0.82f, 0.58f) }

    Row(
        modifier = modifier.height(maxHeightDp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.2.dp)
    ) {
        for (i in 0 until barCount) {
            val wave = (0.24f + 0.76f * abs(sin(phase + phaseOffsets[i % phaseOffsets.size]))) * ampScale[i % ampScale.size]
            Box(
                modifier = Modifier
                    .width(barWidthDp)
                    .height((maxHeightDp * wave).coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(barWidthDp / 2f))
                    .background(activeBarColor)
            )
        }
    }
}

// =========================================================================================
// 7. iOS EXPANDED MEDIA PLAYER (96942.mp4 00:13-00:19 & 01:07-01:17)
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidExpandedMediaView(
    media: MediaInfo?,
    settings: IslandSettings,
    onCollapse: () -> Unit,
    scale: Float = 1.0f
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSeeking by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var isSeekPending by remember { mutableStateOf(false) }

    val rawDuration = media?.durationMs ?: 0L
    val hasValidDuration = rawDuration > 0L
    val duration = if (hasValidDuration) rawDuration else 0L

    var livePosition by remember(media?.packageName, media?.title) {
        mutableLongStateOf(media?.computeCurrentPosition() ?: 0L)
    }

    LaunchedEffect(media?.isPlaying, media?.positionMs, media?.lastPositionUpdateTimeMs, isSeeking, isSeekPending) {
        if (!isSeeking && !isSeekPending) {
            livePosition = media?.computeCurrentPosition() ?: 0L
            if (media?.isPlaying == true) {
                while (isActive) {
                    delay(200)
                    if (!isSeeking && !isSeekPending) {
                        livePosition = media?.computeCurrentPosition() ?: 0L
                    }
                }
            }
        }
    }

    val currentPosition = when {
        isSeeking -> if (hasValidDuration) (scrubFraction * duration).toLong().coerceIn(0L, duration) else livePosition
        else -> livePosition
    }

    val sliderProgress = when {
        isSeeking -> scrubFraction
        hasValidDuration -> (livePosition.toFloat() / duration).coerceIn(0f, 1f)
        else -> 0f
    }

    val displayArt = media?.albumArt ?: media?.appIcon
    val mediaAccent = remember(displayArt) { extractMediaAccentColor(displayArt) }
    val artSize = (54.dp * scale).coerceIn(46.dp, 62.dp)
    val isPlaying = media?.isPlaying == true

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Row 1: Album Art (54dp) + Title/Artist + 6-Bar Waveform (00:13 in 96942.mp4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable {
                        launchAppOrIntent(context, media?.packageName, media?.sessionActivity)
                        onCollapse()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (displayArt != null && !displayArt.isRecycled) {
                    Image(
                        bitmap = displayArt.asImageBitmap(),
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF98989F),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media?.title?.ifBlank { "Not Playing" } ?: "Not Playing",
                    color = Color.White,
                    fontSize = (17f * scale).coerceIn(15f, 19f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1000,
                        velocity = 28.dp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = media?.artist?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
                    color = Color(0xFF98989F),
                    fontSize = (13.5f * scale).coerceIn(12f, 15f).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1500,
                        velocity = 24.dp
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            LiquidSixBarVisualizer(
                isPlaying = isPlaying,
                accentColor = mediaAccent,
                barCount = 6,
                maxHeightDp = 20.dp,
                barWidthDp = 2.5.dp
            )
        }

        // Row 2: Elapsed Time (00:53) + Smooth iOS Capsule Progress Bar + Remaining Time (-02:28)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatLiquidDuration(currentPosition),
                color = Color(0xFF98989F),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(10.dp))

            Slider(
                value = sliderProgress,
                enabled = hasValidDuration,
                onValueChange = { frac ->
                    isSeeking = true
                    scrubFraction = frac
                },
                onValueChangeFinished = {
                    if (hasValidDuration) {
                        val seekTargetMs = (scrubFraction * duration).toLong().coerceIn(0L, duration)
                        livePosition = seekTargetMs
                        isSeekPending = true
                        isSeeking = false
                        IslandStateManager.onSeekTo?.invoke(seekTargetMs)
                        coroutineScope.launch {
                            delay(650)
                            isSeekPending = false
                        }
                    } else {
                        isSeeking = false
                    }
                },
                thumb = {},
                track = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sliderProgress.coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            val remainingMs = if (hasValidDuration) (duration - currentPosition).coerceAtLeast(0L) else 0L
            Text(
                text = if (hasValidDuration) "-${formatLiquidDuration(remainingMs)}" else "-00:00",
                color = Color(0xFF98989F),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Row 3: Centered iOS Transport Controls (Prev, Play/Pause, Next) + Right AirPlay Button (00:13 & 01:08)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Invisible 36dp balancer on left so Prev / Play / Next sit dead-center
            Spacer(modifier = Modifier.size(36.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { IslandStateManager.onSkipPrevious?.invoke() }
                )

                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { IslandStateManager.onPlayPause?.invoke() }
                )

                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { IslandStateManager.onSkipNext?.invoke() }
                )
            }

            // AirPlay / Audio Output icon on far right (exact match to 00:13 & 01:08 in 96942.mp4)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable {
                        runCatching {
                            val panelIntent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(panelIntent)
                        }.onFailure {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }
                        onCollapse()
                    },
                contentAlignment = Alignment.Center
            ) {
                IosAirPlayIcon(modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun IosAirPlayIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.44f
        val strokeW = 1.6.dp.toPx()

        // Concentric circular wave arcs
        drawArc(
            color = tint,
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.44f, cy - w * 0.44f),
            size = Size(w * 0.88f, w * 0.88f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = 145f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.25f, cy - w * 0.25f),
            size = Size(w * 0.50f, w * 0.50f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Bottom upward-pointing solid triangle
        val triPath = Path().apply {
            moveTo(cx, h * 0.52f)
            lineTo(cx - w * 0.24f, h * 0.92f)
            lineTo(cx + w * 0.24f, h * 0.92f)
            close()
        }
        drawPath(path = triPath, color = tint)
    }
}

// =========================================================================================
// 8. iOS EXPANDED STOPWATCH / TIMER CARD (96942.mp4 00:28-00:29: "Lap", "X", Orange "00:14")
// =========================================================================================

@Composable
fun LiquidExpandedTimerView(
    alarmInfo: IslandStateManager.AlarmInfo?,
    onCollapse: () -> Unit,
    scale: Float = 1.0f
) {
    val context = LocalContext.current
    val timeText = alarmInfo?.formattedTime?.ifBlank { "00:14" } ?: "00:14"
    val btnSize = (50.dp * scale).coerceIn(44.dp, 54.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: "Lap" orange-brown circle + "X" dark-gray circle (exact match to 00:28 in 96942.mp4)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(btnSize)
                    .clip(CircleShape)
                    .background(Color(0xFF3B2307))
                    .clickable {
                        if (IslandStateManager.isStopwatchRunning.value) {
                            IslandStateManager.lapStopwatch()
                        } else {
                            IslandStateManager.startStopwatch()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lap",
                    color = Color(0xFFFF9F0A),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .size(btnSize)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E))
                    .clickable {
                        if (IslandStateManager.isStopwatchRunning.value) {
                            IslandStateManager.stopStopwatch()
                        } else {
                            IslandStateManager.updateAlarm(null)
                        }
                        onCollapse()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop Timer",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Right: Huge orange digital timer readout (00:07 / 00:14 in 96942.mp4)
        Text(
            text = timeText,
            color = Color(0xFFFF9F0A),
            fontSize = (40f * scale).coerceIn(32f, 44f).sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.sp,
            maxLines = 1,
            modifier = Modifier.clickable {
                if (alarmInfo?.showIntent != null) {
                    runCatching { alarmInfo.showIntent.send() }
                } else {
                    runCatching {
                        context.startActivity(Intent(android.provider.AlarmClock.ACTION_SHOW_TIMERS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
                onCollapse()
            }
        )
    }
}

// =========================================================================================
// 9. iOS EXPANDED NAVIGATION CARD (96942.mp4 00:44: "↑ 3.6 km", "Arrive 7:21 PM", "End Trip")
// =========================================================================================

@Composable
fun LiquidExpandedNavigationView(
    notification: NotificationInfo?,
    onCollapse: () -> Unit,
    scale: Float = 1.0f
) {
    val context = LocalContext.current
    val rawTitle = notification?.title?.ifBlank { "3.6 km" } ?: "3.6 km"
    val rawSub = notification?.text?.ifBlank { "Arrive 7:21 PM" } ?: "Arrive 7:21 PM"

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    launchAppOrIntent(context, notification?.packageName, notification?.contentIntent)
                    onCollapse()
                },
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Turn Direction",
                tint = Color.White,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rawTitle,
                    color = Color.White,
                    fontSize = (22f * scale).coerceIn(18f, 25f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rawSub,
                    color = Color(0xFF98989F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Bottom full-width dark-red "End Trip" pill button (exact match to 00:44 in 96942.mp4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(Color(0xFF381316))
                .clickable {
                    notification?.actions?.firstOrNull()?.actionIntent?.let { pi ->
                        runCatching { pi.send() }
                    }
                    IslandStateManager.dismissProgress()
                    onCollapse()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "End Trip",
                color = Color(0xFFFF453A),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =========================================================================================
// 10. iOS WIDE RINGER / VIBRATE / SILENT PILL (96942.mp4 00:50-01:00)
// =========================================================================================

@Composable
fun LiquidIosRingerPill(
    mode: IslandStateManager.RingerMode,
    contentAlpha: Float,
    bellShakeRotation: Float,
    vibrateShakeOffset: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .graphicsLayer { alpha = contentAlpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (mode) {
            IslandStateManager.RingerMode.RING -> {
                // 00:50 & 00:58: White bell on left, white "Ring" on right
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Ring",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = bellShakeRotation }
                )
                Spacer(modifier = Modifier.width(36.dp))
                Text(
                    text = "Ring",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IslandStateManager.RingerMode.VIBRATE -> {
                // 00:51-00:52: Solid orange capsule badge on left, orange "Vibrate" on right
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(24.dp)
                        .graphicsLayer { translationX = vibrateShakeOffset.toPx() }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF9500)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Vibrate",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(36.dp))
                Text(
                    text = "Vibrate",
                    color = Color(0xFFFF9500),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IslandStateManager.RingerMode.SILENT -> {
                // 00:54-00:56: Solid red capsule badge on left, red "Silent" on right
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF3B30)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "Silent",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(36.dp))
                Text(
                    text = "Silent",
                    color = Color(0xFFFF3B30),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// =========================================================================================
// 11. iOS WIDE CHARGING PILL (96942.mp4 01:03-01:04: "Charging" left, "80%" + Green Battery right)
// =========================================================================================

@Composable
fun LiquidIosChargingPill(
    percentage: Int,
    contentAlpha: Float
) {
    val iosGreen = Color(0xFF30D158)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .graphicsLayer { alpha = contentAlpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Charging",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(36.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${percentage.coerceIn(0, 100)}%",
                color = iosGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // iOS Battery Icon with Lightning Bolt (01:03 in 96942.mp4)
            Canvas(modifier = Modifier.size(width = 26.dp, height = 13.dp)) {
                val bodyW = size.width - 2.5.dp.toPx()
                val bodyH = size.height
                val r = 3.5.dp.toPx()
                drawRoundRect(
                    color = iosGreen.copy(alpha = 0.35f),
                    size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(r, r)
                )
                val fillFrac = (percentage.coerceIn(10, 100) / 100f)
                drawRoundRect(
                    color = iosGreen,
                    size = Size(bodyW * fillFrac, bodyH),
                    cornerRadius = CornerRadius(r, r)
                )
                // Battery nub on right
                drawRoundRect(
                    color = iosGreen.copy(alpha = 0.65f),
                    topLeft = Offset(bodyW + 1.dp.toPx(), bodyH * 0.28f),
                    size = Size(1.5.dp.toPx(), bodyH * 0.44f),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
                // White lightning bolt in center of battery
                val cx = bodyW * 0.5f
                val cy = bodyH * 0.5f
                val bolt = Path().apply {
                    moveTo(cx + 1.2.dp.toPx(), cy - 4.2.dp.toPx())
                    lineTo(cx - 2.5.dp.toPx(), cy + 0.4.dp.toPx())
                    lineTo(cx - 0.2.dp.toPx(), cy + 0.4.dp.toPx())
                    lineTo(cx - 1.2.dp.toPx(), cy + 4.2.dp.toPx())
                    lineTo(cx + 2.5.dp.toPx(), cy - 0.4.dp.toPx())
                    lineTo(cx + 0.2.dp.toPx(), cy - 0.4.dp.toPx())
                    close()
                }
                drawPath(path = bolt, color = Color.White)
            }
        }
    }
}

// =========================================================================================
// 12. iOS LOCK SCREEN AUTH PILL (96942.mp4 01:19-01:25: Face ID & Fingerprint Morphing)
// =========================================================================================

@Composable
fun LiquidIosLockAuthPill(
    authMode: String,
    contentAlpha: Float
) {
    // Stage 0 (0..550ms): Face ID / Fingerprint icon
    // Stage 1 (550..900ms): Spinning ring (for Fingerprint) or morphing
    // Stage 2 (900ms+): Green Checkmark
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(authMode) {
        stage = 0
        delay(520)
        stage = 1
        delay(340)
        stage = 2
    }

    val spinTransition = rememberInfiniteTransition(label = "fp_spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (stage == 2) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "authCheckScale"
    )

    val iosGreen = Color(0xFF30D158)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .graphicsLayer { alpha = contentAlpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = if (stage >= 2) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = "Lock Auth",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(36.dp))

        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (authMode.uppercase(Locale.US) == "FINGERPRINT") {
                when (stage) {
                    0 -> {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint",
                            tint = iosGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    1 -> {
                        Canvas(
                            modifier = Modifier
                                .size(19.dp)
                                .graphicsLayer { rotationZ = spinAngle }
                        ) {
                            drawArc(
                                color = iosGreen,
                                startAngle = 0f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    else -> {
                        // Solid green circle with black checkmark (01:24-01:25 in 96942.mp4)
                        Canvas(
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                        ) {
                            drawCircle(color = iosGreen)
                            val p = Path().apply {
                                moveTo(size.width * 0.28f, size.height * 0.52f)
                                lineTo(size.width * 0.44f, size.height * 0.68f)
                                lineTo(size.width * 0.74f, size.height * 0.34f)
                            }
                            drawPath(
                                path = p,
                                color = Color.Black,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            } else {
                // FACE_ID mode (01:19-01:20 in 96942.mp4)
                if (stage < 2) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val s = size.width
                        val sw = 1.7.dp.toPx()
                        val corner = s * 0.26f
                        // 4 corner brackets + smiling face
                        val path = Path().apply {
                            // Top-left
                            moveTo(0f, corner); lineTo(0f, 0f); lineTo(corner, 0f)
                            // Top-right
                            moveTo(s - corner, 0f); lineTo(s, 0f); lineTo(s, corner)
                            // Bottom-left
                            moveTo(0f, s - corner); lineTo(0f, s); lineTo(corner, s)
                            // Bottom-right
                            moveTo(s - corner, s); lineTo(s, s); lineTo(s, s - corner)
                        }
                        drawPath(path = path, color = iosGreen, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        // Eyes & smile
                        drawCircle(color = iosGreen, radius = 1.2.dp.toPx(), center = Offset(s * 0.35f, s * 0.36f))
                        drawCircle(color = iosGreen, radius = 1.2.dp.toPx(), center = Offset(s * 0.65f, s * 0.36f))
                        drawArc(
                            color = iosGreen,
                            startAngle = 25f,
                            sweepAngle = 130f,
                            useCenter = false,
                            topLeft = Offset(s * 0.30f, s * 0.38f),
                            size = Size(s * 0.40f, s * 0.34f),
                            style = Stroke(width = sw, cap = StrokeCap.Round)
                        )
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = checkScale
                                scaleY = checkScale
                            }
                    ) {
                        val p = Path().apply {
                            moveTo(size.width * 0.20f, size.height * 0.52f)
                            lineTo(size.width * 0.42f, size.height * 0.74f)
                            lineTo(size.width * 0.82f, size.height * 0.28f)
                        }
                        drawPath(
                            path = p,
                            color = iosGreen,
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
    }
}

fun resolveLiquidPriorityAccentColor(
    priority: IslandPriority?,
    mediaAccentColor: Color = Color.Transparent
): Color = when (priority) {
    IslandPriority.MEDIA -> if (mediaAccentColor != Color.Transparent) mediaAccentColor else Color.White
    IslandPriority.CALL -> Color(0xFF30D158)
    IslandPriority.CHARGING -> Color(0xFF30D158)
    IslandPriority.VOICE_MEMO, IslandPriority.SCREEN_RECORDING -> Color(0xFFFF453A)
    IslandPriority.ALARM -> Color(0xFFFF9F0A)
    IslandPriority.PROGRESS -> Color(0xFF30D158)
    else -> Color.White
}

private fun formatLiquidDuration(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
