package com.droidbubble.app.ui.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Authentic iOS 26 Liquid Glass Switch (matching 112206.mp4 100%).
 * - Rest state: Pill-shaped track with solid white horizontal oval thumb.
 * - Pressed/Dragging state: Thumb expands above & below the track and morphs into a
 *   crystal-clear refractive Liquid Glass capsule with dual-peak specular rim highlights,
 *   chromatic track-color rim reflection, and velocity-driven elastic stretch.
 */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeTrackColor: Color = Color(0xFF34C759),
    inactiveTrackColor: Color = if (isSystemInDarkTheme()) Color(0xFF39393D) else Color(0xFFD1D1D6),
) {
    val scope = rememberCoroutineScope()
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentEnabled by rememberUpdatedState(enabled)

    val thumbFractionAnim = remember { Animatable(if (checked) 1f else 0f) }
    val pressProgressAnim = remember { Animatable(0f) }
    val velocityAnim = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(checked) {
        if (!isDragging) {
            thumbFractionAnim.animateTo(
                targetValue = if (checked) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f)
            )
        }
    }

    val trackWidthDp = 58.dp
    val trackHeightDp = 30.dp

    Box(
        modifier = modifier
            .size(width = trackWidthDp, height = trackHeightDp)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!currentEnabled || currentOnCheckedChange == null) return@awaitEachGesture

                    isDragging = true
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    val startFraction = thumbFractionAnim.value
                    var totalDragX = 0f

                    scope.launch {
                        pressProgressAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = 460f)
                        )
                    }

                    val travelPx = (size.width - size.height * 1.12f).coerceAtLeast(1f)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) break
                        val dx = change.positionChange().x
                        if (dx != 0f) {
                            totalDragX += dx
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val vx = (velocityTracker.calculateVelocity().x / 1200f).coerceIn(-1.5f, 1.5f)
                            val nextFraction = (startFraction + totalDragX / travelPx).coerceIn(0f, 1f)
                            scope.launch {
                                thumbFractionAnim.snapTo(nextFraction)
                                velocityAnim.snapTo(vx)
                            }
                            change.consume()
                        }
                    }

                    isDragging = false
                    val isTap = abs(totalDragX) < 8.dp.toPx()
                    val newChecked = if (isTap) !currentChecked else thumbFractionAnim.value >= 0.5f

                    scope.launch {
                        launch {
                            thumbFractionAnim.animateTo(
                                targetValue = if (newChecked) 1f else 0f,
                                animationSpec = spring(dampingRatio = 0.66f, stiffness = 400f)
                            )
                        }
                        launch {
                            pressProgressAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f)
                            )
                        }
                        launch {
                            velocityAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            )
                        }
                    }

                    if (newChecked != currentChecked) {
                        currentOnCheckedChange?.invoke(newChecked)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fraction = thumbFractionAnim.value.coerceIn(0f, 1f)
            val press = pressProgressAnim.value.coerceIn(0f, 1f)
            val vel = velocityAnim.value
            val alphaMult = if (enabled) 1f else 0.45f

            val w = size.width
            val h = size.height
            val trackRadius = h * 0.5f

            // 1. Track Capsule (smoothly interpolates between dark gray OFF and vibrant green ON)
            val trackColor = lerp(inactiveTrackColor, activeTrackColor, fraction).copy(alpha = alphaMult)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = CornerRadius(trackRadius, trackRadius)
            )
            // Subtle inner track depth
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.14f * alphaMult),
                    0.35f to Color.Transparent,
                    1f to Color.White.copy(alpha = 0.08f * alphaMult)
                ),
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = CornerRadius(trackRadius, trackRadius)
            )

            // 2. Thumb geometry: horizontal pill at rest, expands into protruding Liquid Glass capsule on press
            val padPx = 3.dp.toPx()
            val restThumbW = 33.dp.toPx().coerceAtMost(w * 0.58f)
            val restThumbH = (h - padPx * 2f).coerceAtLeast(18.dp.toPx())

            val velStretchX = 1f + (abs(vel) * 0.12f).coerceAtMost(0.16f)
            val velSquishY = 1f - (abs(vel) * 0.06f).coerceAtMost(0.08f)

            val scaleX = lerp(1f, 1.30f, press) * velStretchX
            val scaleY = lerp(1f, 1.34f, press) * velSquishY

            val curThumbW = restThumbW * scaleX
            val curThumbH = restThumbH * scaleY
            val thumbRadius = curThumbH * 0.5f

            val minCenterX = padPx + restThumbW * 0.5f
            val maxCenterX = w - padPx - restThumbW * 0.5f
            val centerX = lerp(minCenterX, maxCenterX, fraction)
            val centerY = h * 0.5f

            val thumbLeft = centerX - curThumbW * 0.5f
            val thumbTop = centerY - curThumbH * 0.5f

            // 3. Soft drop shadow beneath the thumb
            drawRoundRect(
                color = Color.Black.copy(alpha = lerp(0.18f, 0.28f, press) * alphaMult),
                topLeft = Offset(thumbLeft, thumbTop + 1.5.dp.toPx()),
                size = Size(curThumbW, curThumbH),
                cornerRadius = CornerRadius(thumbRadius, thumbRadius)
            )

            // 4. Solid White Pill Fill (fades out to crystal-clear glass when pressed!)
            val solidWhiteAlpha = lerp(1f, 0.04f, press) * alphaMult
            if (solidWhiteAlpha > 0.01f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = solidWhiteAlpha),
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(curThumbW, curThumbH),
                    cornerRadius = CornerRadius(thumbRadius, thumbRadius)
                )
            }

            // 5. Liquid Glass Lens Optics (active when pressed / dragging, matching 112206.mp4 00:03-00:09)
            if (press > 0.01f) {
                val glassTint = lerp(Color(0xFF8E8E93), activeTrackColor, fraction)

                // Subtle translucent glass volume + caustic lens sheen
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color.White.copy(alpha = 0.22f * press * alphaMult),
                        0.28f to glassTint.copy(alpha = 0.06f * press * alphaMult),
                        0.72f to Color.Black.copy(alpha = 0.08f * press * alphaMult),
                        1.00f to Color.White.copy(alpha = 0.16f * press * alphaMult),
                        startY = thumbTop,
                        endY = thumbTop + curThumbH
                    ),
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(curThumbW, curThumbH),
                    cornerRadius = CornerRadius(thumbRadius, thumbRadius)
                )

                // Dark inner edge refraction band (creates convex optical glass depth like 112206.mp4)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color.Black.copy(alpha = 0.22f * press * alphaMult),
                        0.22f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.20f * press * alphaMult),
                        startY = thumbTop,
                        endY = thumbTop + curThumbH
                    ),
                    topLeft = Offset(thumbLeft + 1.5.dp.toPx(), thumbTop + 1.5.dp.toPx()),
                    size = Size((curThumbW - 3.dp.toPx()).coerceAtLeast(1f), (curThumbH - 3.dp.toPx()).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius((thumbRadius - 1.5.dp.toPx()).coerceAtLeast(1f), (thumbRadius - 1.5.dp.toPx()).coerceAtLeast(1f)),
                    style = Stroke(width = 2.0.dp.toPx())
                )

                // Crisp dual-peak specular glass rim border (tinted with track green when ON, bright silver when OFF)
                val rimAccent = lerp(Color.White, activeTrackColor, fraction * 0.65f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color.White.copy(alpha = 0.88f * press * alphaMult),
                        0.25f to rimAccent.copy(alpha = 0.45f * press * alphaMult),
                        0.50f to rimAccent.copy(alpha = 0.20f * press * alphaMult),
                        0.75f to rimAccent.copy(alpha = 0.45f * press * alphaMult),
                        1.00f to Color.White.copy(alpha = 0.78f * press * alphaMult),
                        startY = thumbTop,
                        endY = thumbTop + curThumbH
                    ),
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(curThumbW, curThumbH),
                    cornerRadius = CornerRadius(thumbRadius, thumbRadius),
                    style = Stroke(width = 1.35.dp.toPx())
                )
            }
        }
    }
}

/**
 * Authentic iOS 26 Liquid Glass Slider (matching 112205.mp4 100%).
 * - Rest state: Thin rounded track with active blue/accent left fill and solid white horizontal pill thumb.
 * - Pressed/Dragging state: Pill thumb expands ~1.38x and morphs into a crystal-clear Liquid Glass lens
 *   capsule so the blue/gray track passes transparently through it, with glowing active-color rim refraction
 *   on the left edge and crisp white specular highlights top & bottom.
 */
@Composable
fun LiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = if (isSystemInDarkTheme()) Color(0xFF39393D) else Color(0xFFD1D1D6),
) {
    val scope = rememberCoroutineScope()
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentRange by rememberUpdatedState(valueRange)
    val currentSteps by rememberUpdatedState(steps)

    val pressProgressAnim = remember { Animatable(0f) }
    val velocityAnim = remember { Animatable(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    val rangeSpan = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val normalizedFraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    fun quantize(rawFraction: Float): Float {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val stepped = if (currentSteps > 0) {
            val segments = currentSteps + 1
            (clamped * segments).roundToInt().toFloat() / segments
        } else {
            clamped
        }
        return currentRange.start + stepped * (currentRange.endInclusive - currentRange.start)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!currentEnabled) return@awaitEachGesture

                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    scope.launch {
                        pressProgressAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = 460f)
                        )
                    }

                    val thumbHalfW = 17.dp.toPx()
                    val usableW = (size.width - thumbHalfW * 2f).coerceAtLeast(1f)

                    fun emitForX(x: Float) {
                        val frac = ((x - thumbHalfW) / usableW).coerceIn(0f, 1f)
                        val mapped = quantize(frac)
                        if (mapped != currentValue) {
                            currentOnValueChange(mapped)
                        }
                    }

                    emitForX(down.position.x)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) break
                        val dx = change.positionChange().x
                        if (dx != 0f) {
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val vx = (velocityTracker.calculateVelocity().x / 1800f).coerceIn(-1.5f, 1.5f)
                            scope.launch { velocityAnim.snapTo(vx) }
                            emitForX(change.position.x)
                            change.consume()
                        }
                    }

                    scope.launch {
                        launch {
                            pressProgressAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f)
                            )
                        }
                        launch {
                            velocityAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            )
                        }
                    }
                    currentOnFinished?.invoke()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val press = pressProgressAnim.value.coerceIn(0f, 1f)
            val vel = velocityAnim.value
            val alphaMult = if (enabled) 1f else 0.45f

            val w = size.width
            val h = size.height
            val centerY = h * 0.5f

            val restThumbW = 34.dp.toPx()
            val restThumbH = 20.dp.toPx()
            val thumbHalfW = restThumbW * 0.5f
            val usableW = (w - restThumbW).coerceAtLeast(1f)
            val thumbCenterX = thumbHalfW + normalizedFraction * usableW

            // 1. Track (thin 6.dp capsule, active left fill + inactive right fill)
            // Drawn BEFORE the glass thumb so when pressed, the track passes transparently THROUGH the glass capsule!
            val trackH = lerp(6.dp.toPx(), 7.dp.toPx(), press)
            val trackTop = centerY - trackH * 0.5f
            val trackRadius = trackH * 0.5f
            val trackStartX = 4.dp.toPx()
            val trackEndX = (w - 4.dp.toPx()).coerceAtLeast(trackStartX + 1f)
            val fullTrackW = (trackEndX - trackStartX).coerceAtLeast(1f)

            // Inactive background track
            drawRoundRect(
                color = inactiveTrackColor.copy(alpha = alphaMult),
                topLeft = Offset(trackStartX, trackTop),
                size = Size(fullTrackW, trackH),
                cornerRadius = CornerRadius(trackRadius, trackRadius)
            )

            // Active filled track up to thumb center
            val activeW = (thumbCenterX - trackStartX).coerceIn(0f, fullTrackW)
            if (activeW > 0.5f) {
                drawRoundRect(
                    color = activeTrackColor.copy(alpha = alphaMult),
                    topLeft = Offset(trackStartX, trackTop),
                    size = Size(activeW, trackH),
                    cornerRadius = CornerRadius(trackRadius, trackRadius)
                )
            }

            // 2. Thumb geometry: horizontal pill at rest, expands ~1.38x into refractive Liquid Glass capsule on press
            val velStretchX = 1f + (abs(vel) * 0.14f).coerceAtMost(0.18f)
            val velSquishY = 1f - (abs(vel) * 0.06f).coerceAtMost(0.08f)

            val scaleX = lerp(1f, 1.38f, press) * velStretchX
            val scaleY = lerp(1f, 1.42f, press) * velSquishY

            val curThumbW = restThumbW * scaleX
            val curThumbH = restThumbH * scaleY
            val thumbRadius = curThumbH * 0.5f
            val thumbLeft = thumbCenterX - curThumbW * 0.5f
            val thumbTop = centerY - curThumbH * 0.5f

            // 3. Soft shadow under pill
            drawRoundRect(
                color = Color.Black.copy(alpha = lerp(0.16f, 0.22f, press) * alphaMult),
                topLeft = Offset(thumbLeft, thumbTop + 1.5.dp.toPx()),
                size = Size(curThumbW, curThumbH),
                cornerRadius = CornerRadius(thumbRadius, thumbRadius)
            )

            // 4. Solid White Pill Fill (fades out cleanly on press to reveal transparent glass over track!)
            val solidWhiteAlpha = lerp(1f, 0.03f, press) * alphaMult
            if (solidWhiteAlpha > 0.01f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = solidWhiteAlpha),
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(curThumbW, curThumbH),
                    cornerRadius = CornerRadius(thumbRadius, thumbRadius)
                )
            }

            // 5. Liquid Glass Lens Optics (112205.mp4 00:00-00:08)
            if (press > 0.01f) {
                // Translucent glass volume with subtle vertical convexity
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color.White.copy(alpha = 0.20f * press * alphaMult),
                        0.25f to Color.White.copy(alpha = 0.04f * press * alphaMult),
                        0.75f to Color.Black.copy(alpha = 0.06f * press * alphaMult),
                        1.00f to Color.White.copy(alpha = 0.14f * press * alphaMult),
                        startY = thumbTop,
                        endY = thumbTop + curThumbH
                    ),
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(curThumbW, curThumbH),
                    cornerRadius = CornerRadius(thumbRadius, thumbRadius)
                )

                // Horizontal chromatic edge glow: vibrant activeTrackColor on left edge where blue bar enters glass!
                val leftRimColor = lerp(Color.White, activeTrackColor, (normalizedFraction * 3f).coerceIn(0.25f, 1f))
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        0.00f to leftRimColor.copy(alpha = 0.90f * press * alphaMult),
                        0.45f to Color.White.copy(alpha = 0.42f * press * alphaMult),
                        1.00f to Color.White.copy(alpha = 0.58f * press * alphaMult),
                        startX = thumbLeft,
                        endX = thumbLeft + curThumbW
                    ),
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(curThumbW, curThumbH),
                    cornerRadius = CornerRadius(thumbRadius, thumbRadius),
                    style = Stroke(width = 1.4.dp.toPx())
                )

                // Top & bottom dual-peak specular white glass arcs
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color.White.copy(alpha = 0.85f * press * alphaMult),
                        0.22f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1.00f to Color.White.copy(alpha = 0.68f * press * alphaMult),
                        startY = thumbTop,
                        endY = thumbTop + curThumbH
                    ),
                    topLeft = Offset(thumbLeft + 0.5.dp.toPx(), thumbTop + 0.5.dp.toPx()),
                    size = Size((curThumbW - 1.dp.toPx()).coerceAtLeast(1f), (curThumbH - 1.dp.toPx()).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius((thumbRadius - 0.5.dp.toPx()).coerceAtLeast(1f), (thumbRadius - 0.5.dp.toPx()).coerceAtLeast(1f)),
                    style = Stroke(width = 1.0.dp.toPx())
                )
            }
        }
    }
}
