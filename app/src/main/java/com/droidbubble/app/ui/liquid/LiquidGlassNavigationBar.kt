package com.droidbubble.app.ui.liquid

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

data class LiquidNavItem(
    val label: String,
    val icon: ImageVector
)

@Immutable
data class InnerShadow(
    val radius: Dp = 24.dp,
    val offset: DpOffset = DpOffset(0.dp, radius),
    val color: Color = Color.Black.copy(alpha = 0.15f),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    companion object {
        @Stable
        val Default: InnerShadow = InnerShadow()
    }
}

fun Modifier.innerShadow(
    shape: Shape,
    shadow: () -> InnerShadow?,
): Modifier = this.drawWithContent {
    drawContent()
    val s = shadow() ?: return@drawWithContent
    if (s.alpha <= 0.01f) return@drawWithContent
    val strokePx = s.radius.toPx().coerceAtLeast(1f)
    val outline = shape.createOutline(size, layoutDirection, this)
    if (outline is Outline.Rounded) {
        val r = outline.roundRect.topLeftCornerRadius.x
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    s.color.copy(alpha = (s.color.alpha * s.alpha).coerceIn(0f, 1f)),
                    Color.Transparent,
                    s.color.copy(alpha = (s.color.alpha * s.alpha * 0.5f).coerceIn(0f, 1f))
                )
            ),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = strokePx * 0.45f)
        )
    }
}

/**
 * Physics-driven damped drag & press controller adapted from liquid.zip's DampedDragAnimation.
 * Provides smooth continuous drag across tabs, press expansion (78f / 56f), velocity-based
 * liquid squash & stretch deformation, and spring snap to the target tab.
 */
@Stable
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float,
    private val valueRange: ClosedFloatingPointRange<Float>,
    private val initialScale: Float = 1f,
    private val pressedScale: Float = 78f / 56f,
    private val canDrag: (Offset) -> Boolean,
    private val onDragStarted: DampedDragAnimation.(Offset) -> Unit,
    private val onDragStopped: DampedDragAnimation.() -> Unit,
    private val onDragCancelled: DampedDragAnimation.() -> Unit,
    private val onDrag: DampedDragAnimation.(change: Offset, dragAmount: Offset) -> Unit,
) {
    private val valueAnim = Animatable(initialValue)
    private val pressAnim = Animatable(0f)
    private val velocityAnim = Animatable(0f)

    var targetValue by mutableFloatStateOf(initialValue)
        private set

    val value: Float get() = valueAnim.value
    val pressProgress: Float get() = pressAnim.value
    val velocity: Float get() = velocityAnim.value

    val scaleX: Float
        get() = lerp(initialScale, pressedScale * 1.06f, pressProgress)

    val scaleY: Float
        get() = lerp(initialScale, pressedScale * 0.96f, pressProgress)

    fun updateValue(newValue: Float) {
        val clamped = newValue.coerceIn(valueRange.start, valueRange.endInclusive)
        targetValue = clamped
        animationScope.launch {
            valueAnim.animateTo(
                targetValue = clamped,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 420f
                )
            )
        }
    }

    fun animateToValue(newValue: Float) {
        val clamped = newValue.coerceIn(valueRange.start, valueRange.endInclusive)
        targetValue = clamped
        animationScope.launch {
            launch {
                pressAnim.animateTo(0.45f, tween(90))
                pressAnim.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 320f))
            }
            valueAnim.animateTo(
                targetValue = clamped,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 380f
                )
            )
        }
    }

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!canDrag(down.position)) return@awaitEachGesture

            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            animationScope.launch {
                pressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f)
                )
            }
            onDragStarted(down.position)

            var pointerId = down.id
            var dragged = false
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId }
                        ?: event.changes.firstOrNull()
                        ?: break
                    pointerId = change.id
                    if (!change.pressed) {
                        change.consume()
                        break
                    }
                    val delta = change.positionChange()
                    if (delta != Offset.Zero) {
                        dragged = true
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val vx = velocityTracker.calculateVelocity().x / 1000f
                        animationScope.launch {
                            velocityAnim.snapTo(vx.coerceIn(-4f, 4f))
                        }
                        onDrag(change.position, delta)
                        change.consume()
                    }
                }
                animationScope.launch {
                    pressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
                    )
                    velocityAnim.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 300f))
                }
                onDragStopped()
            } catch (_: Exception) {
                animationScope.launch {
                    pressAnim.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 300f))
                    velocityAnim.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 300f))
                }
                onDragCancelled()
            }
        }
    }
}

@Composable
private fun rememberQuantizedGravityAngle(): State<Float> {
    val context = LocalContext.current
    val angleState = remember { mutableFloatStateOf((-PI / 2).toFloat()) }
    DisposableEffect(context) {
        val sm = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val stepRad = (3.0 * PI / 180.0).toFloat()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size >= 2) {
                    val gx = -values[0]
                    val gy = values[1]
                    if (gx * gx + gy * gy > 0.04f) {
                        val q = (atan2(gy, gx) / stepRad).roundToInt() * stepRad
                        if (angleState.floatValue != q) {
                            angleState.floatValue = q
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sm?.unregisterListener(listener)
        }
    }
    return angleState
}

private val LocalIosTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun LiquidGlassNavigationBar(
    items: List<LiquidNavItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    refractionStrength: Float = 1.14f,
    surfaceTension: Float = 4.0f,
    modifier: Modifier = Modifier
) {
    val pillShape = CircleShape
    val tabContentColor = if (isDark) Color(0xFFE5E5EA) else Color(0xFF1C1C1E)
    val containerColor = if (isDark) {
        Color(0xFF1E1E24).copy(alpha = 0.88f)
    } else {
        Color(0xFFF4F6FA).copy(alpha = 0.90f)
    }

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size.coerceAtLeast(1)

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { (surfaceTension * 1.2f).dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).coerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onItemClickUpdated by rememberUpdatedState(onItemClick)

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDrag = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            initialScale = 1f,
            pressedScale = 72f / 56f,
            canDrag = { position ->
                position.x in 0f..totalWidthPx
            },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onItemClickUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }

    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDrag.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (currentIndex != index) {
            currentIndex = index
            onItemClickUpdated(index)
        }
        dampedDrag.animateToValue(index.toFloat())
    }

    val gravityAngle by rememberQuantizedGravityAngle()
    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = if (navBarBottomPadding != 0.dp) 6.dp + navBarBottomPadding else 14.dp

    val tabsContent: @Composable RowScope.(Boolean) -> Unit = { isOverlayPass ->
        val tabScale = LocalIosTabScale.current
        items.forEachIndexed { index, item ->
            val isSelected = index == currentIndex
            val itemColor = when {
                isOverlayPass -> accentColor
                isSelected -> accentColor
                else -> tabContentColor.copy(alpha = 0.72f)
            }
            Column(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        selected = isSelected
                        role = Role.Tab
                        onClick {
                            activateTab(index)
                            true
                        }
                    }
                    .onKeyEvent { event ->
                        val isActivationKey = event.key == Key.Enter ||
                            event.key == Key.NumPadEnter ||
                            event.key == Key.Spacebar
                        if (isActivationKey) {
                            if (event.type == KeyEventType.KeyUp) activateTab(index)
                            true
                        } else {
                            false
                        }
                    }
                    .focusable()
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val s = tabScale()
                        scaleX = s
                        scaleY = s
                    },
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                horizontalAlignment = CenterHorizontally,
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = itemColor
                )
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected || isOverlayPass) FontWeight.SemiBold else FontWeight.Medium,
                    color = itemColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(bottom = bottomPaddingValue, start = 14.dp, end = 14.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Base frosted glass pill bar with gravity-rotated specular rim & damped press scaling
            CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                Row(
                    modifier = Modifier
                        .selectableGroup()
                        .onSizeChanged { coords ->
                            totalWidthPx = coords.width.toFloat()
                            val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                            tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                        }
                        .graphicsLayer {
                            translationX = panelOffset
                            val width = size.width.coerceAtLeast(1f)
                            val s = lerp(1f, 1f + (12.dp.toPx() * refractionStrength) / width, dampedDrag.pressProgress)
                            scaleX = s
                            scaleY = s
                        }
                        .shadow(
                            elevation = if (isDark) 18.dp else 12.dp,
                            shape = pillShape,
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.18f),
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.22f)
                        )
                        .clip(pillShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFF2A2B33).copy(alpha = 0.92f),
                                        Color(0xFF191A20).copy(alpha = 0.95f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        Color(0xFFECEFF5).copy(alpha = 0.92f)
                                    )
                                }
                            ),
                            shape = pillShape
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.25f else 0.85f),
                                    accentColor.copy(alpha = 0.18f * dampedDrag.pressProgress),
                                    Color.White.copy(alpha = if (isDark) 0.08f else 0.45f)
                                ),
                                start = Offset(
                                    x = 200f + 180f * cos(gravityAngle),
                                    y = 40f + 40f * sin(gravityAngle)
                                ),
                                end = Offset(
                                    x = 200f - 180f * cos(gravityAngle),
                                    y = 40f - 40f * sin(gravityAngle)
                                )
                            ),
                            shape = pillShape
                        )
                        .then(dampedDrag.modifier)
                        .height(64.dp)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = { tabsContent(false) },
                )
            }

            // Liquid Lens Indicator Pill that morphs, squashes & stretches with velocity and magnifies active tab
            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                val press = dampedDrag.pressProgress
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDrag.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                            val v = dampedDrag.velocity / 10f
                            val stretchX = 1f / (1f - (v * 0.65f).coerceIn(-0.22f, 0.22f))
                            val squashY = 1f - (abs(v) * 0.18f).coerceIn(0f, 0.18f)
                            scaleX = lerp(1f, 1.08f * refractionStrength.coerceIn(0.8f, 1.3f), press) * stretchX
                            scaleY = lerp(1f, 1.04f, press) * squashY
                        }
                        .shadow(
                            elevation = (4.dp + 8.dp * press),
                            shape = pillShape,
                            ambientColor = accentColor.copy(alpha = 0.25f),
                            spotColor = accentColor.copy(alpha = 0.30f)
                        )
                        .clip(pillShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color.White.copy(alpha = 0.16f + 0.08f * press),
                                        accentColor.copy(alpha = 0.18f + 0.10f * press),
                                        Color.White.copy(alpha = 0.08f + 0.06f * press)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color(0xFFDCE4F2).copy(alpha = 0.72f + 0.15f * press),
                                        Color.White.copy(alpha = 0.78f)
                                    )
                                }
                            ),
                            shape = pillShape
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.45f else 0.95f),
                                    accentColor.copy(alpha = 0.25f + 0.35f * press),
                                    Color.Black.copy(alpha = if (isDark) 0.25f else 0.08f)
                                )
                            ),
                            shape = pillShape
                        )
                        .innerShadow(shape = pillShape) {
                            InnerShadow(
                                radius = 8.dp * (0.35f + 0.65f * press),
                                color = Color.Black.copy(alpha = 0.14f),
                                alpha = 0.4f + 0.6f * press,
                            )
                        }
                        .height(56.dp)
                        .width(tabWidthDp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(
                        LocalIosTabScale provides { lerp(1f, 1.14f, press) },
                        LocalContentColor provides accentColor
                    ) {
                        Row(
                            modifier = Modifier
                                .clearAndSetSemantics {}
                                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).coerceAtLeast(1f).toDp() })
                                .height(56.dp)
                                .graphicsLayer {
                                    val progressOffset = dampedDrag.value * tabWidthPx
                                    translationX = if (isLtr) -progressOffset else progressOffset
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            content = { tabsContent(true) },
                        )
                    }
                }
            }
        }
    }
}
