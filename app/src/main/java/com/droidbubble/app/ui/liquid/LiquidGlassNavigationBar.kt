// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.droidbubble.app.ui.liquid

// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

private val LocalIosTabScale = staticCompositionLocalOf { { 1f } }

private val iosIndicatorSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

// Mirrors HighlightStyle.kt's LIGHT_REF — keep in sync.
private const val LIGHT_REF_X = 0.5f
private const val LIGHT_REF_Y = 0.7f
private const val GRAVITY_DIR_THRESHOLD_SQ = 0.01f // |g_xy| > 0.1, ≈ 6° tilt

// 3° quantization step for the gravity direction: finer changes are imperceptible.
private const val GRAVITY_ANGLE_STEP_RAD = (3.0 * PI / 180.0).toFloat()

/**
 * In-screen-plane gravity direction angle (radians, quantized to 3° steps).
 */
@Composable
private fun rememberQuantizedGravityAngle(): State<Float> {
    val tiltState = rememberDeviceTilt()
    return remember(tiltState) {
        derivedStateOf {
            val tilt = tiltState.value
            val gx = tilt.gravityX
            val gy = tilt.gravityY
            val gMagSq = gx * gx + gy * gy
            if (gMagSq > GRAVITY_DIR_THRESHOLD_SQ) {
                (atan2(gy, gx) / GRAVITY_ANGLE_STEP_RAD).roundToInt() * GRAVITY_ANGLE_STEP_RAD
            } else {
                // Near-flat: the in-plane gravity direction is unstable, pin to (0, -1).
                (-PI / 2).toFloat()
            }
        }
    }
}

/**
 * [base] with its `dualPeak` primary light rotated to the gravity angle plus [extraDegrees].
 */
@Composable
private fun rememberGravityRotatedHighlight(
    base: Highlight,
    extraDegrees: Float,
): State<Highlight> {
    val gravityAngle = rememberQuantizedGravityAngle()
    return remember(gravityAngle, base, extraDegrees) {
        derivedStateOf {
            val baseStyle = base.style as BloomStroke
            val basePrimary = baseStyle.primaryLight
            val rad = gravityAngle.value + (extraDegrees * PI / 180.0).toFloat()
            base.copy(
                style = baseStyle.copy(
                    primaryLight = basePrimary.copy(
                        position = LightPosition(
                            x = LIGHT_REF_X + cos(rad),
                            y = LIGHT_REF_Y + sin(rad),
                            z = basePrimary.position.z,
                        ),
                    ),
                ),
            )
        }
    }
}

@Composable
fun IosLiquidGlassNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    backdrop: LayerBackdrop?,
    isBlurActive: Boolean,
    modifier: Modifier = Modifier,
    isDarkOverride: Boolean? = null,
    accentColorOverride: Color? = null,
    badge: (Int) -> (@Composable () -> Unit)? = { null },
) {
    val isDark = isDarkOverride ?: isSystemInDarkTheme()
    val pillShape = remember { CircleShape }
    val accentColor = accentColorOverride ?: MaterialTheme.colorScheme.primary
    val tabContentColor = MaterialTheme.colorScheme.onSurface
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = if (isBlurActive) surfaceContainer.copy(alpha = 0.72f) else surfaceContainer

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
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
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
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

    // Keyed on dampedDrag: the position lambda captures it; a stale capture would freeze the press spot.
    val interactiveHighlight = remember(animationScope, isLtr, dampedDrag) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { layerSize, _ ->
                Offset(
                    x = if (isLtr) {
                        (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        layerSize.width - (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    y = layerSize.height / 2f,
                )
            },
        )
    }

    // Read .value only inside highlight lambdas (draw phase), never in composition.
    val baseHighlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = -45f)
    val pillHighlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = 90f)

    val combinedBackdrop = backdrop?.let { rememberCombinedBackdrop(it, tabsBackdrop) }

    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = when (platform()) {
        Platform.IOS -> 20.dp

        else -> {
            if (navBarBottomPadding != 0.dp) 8.dp + navBarBottomPadding else 36.dp
        }
    }

    val tabsContent: @Composable RowScope.() -> Unit = {
        val tabScale = LocalIosTabScale.current
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        selected = index == currentIndex
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
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                horizontalAlignment = CenterHorizontally,
            ) {
                BadgedBox(badge = { badge(index)?.invoke() }) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = LocalContentColor.current,
                    )
                }
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = LocalContentColor.current,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(bottom = bottomPaddingValue, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            CompositionLocalProvider(
                LocalIosTabScale provides {
                    lerp(1f, 0.92f, dampedDrag.pressProgress)
                },
                LocalContentColor provides tabContentColor
            ) {
                Row(
                    modifier = Modifier
                        .selectableGroup()
                        .onSizeChanged { coords ->
                            totalWidthPx = coords.width.toFloat()
                            val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                            tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                        }
                        .graphicsLayer { translationX = panelOffset }
                        .dropShadow(
                            shape = pillShape,
                            shadow = Shadow(
                                radius = 10.dp,
                                color = Color.Black,
                                alpha = if (isDark) 0.2f else 0.1f,
                            ),
                        )
                        .background(containerColor, pillShape)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                0f to Color.White.copy(alpha = if (isDark) 0.18f else 0.65f),
                                1f to Color.White.copy(alpha = if (isDark) 0.04f else 0.15f),
                            ),
                            shape = pillShape,
                        )
                        .drawWithContent {
                            drawContent()
                            val progress = dampedDrag.pressProgress
                            if (progress > 0.005f) {
                                val touchCenter = dampedDrag.touchOffset
                                val glowRadius = (this.size.maxDimension * 0.70f).coerceAtLeast(140.dp.toPx())
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0.00f to Color.White.copy(alpha = 0.40f * progress),
                                        0.22f to Color.White.copy(alpha = 0.22f * progress),
                                        0.48f to accentColor.copy(alpha = 0.15f * progress),
                                        0.75f to accentColor.copy(alpha = 0.05f * progress),
                                        1.00f to Color.Transparent,
                                        center = touchCenter,
                                        radius = glowRadius,
                                    ),
                                    radius = glowRadius,
                                    center = touchCenter,
                                )
                            }
                        }
                        .then(dampedDrag.modifier)
                        .height(64.dp)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = tabsContent,
                )
            }

            if (tabWidthPx > 0f) {
                val tabWidthDp = with(density) { tabWidthPx.toDp() }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val singleTabWidth = tabWidthPx
                            val progressOffset = dampedDrag.value * singleTabWidth
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                            scaleX = dampedDrag.scaleX
                            scaleY = dampedDrag.scaleY
                            val v = dampedDrag.velocity / 10f
                            scaleX /= 1f - (v * 0.75f).coerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (v * 0.25f).coerceIn(-0.2f, 0.2f)
                        }
                        .clip(pillShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) listOf(
                                    Color.White.copy(alpha = lerp(0.14f, 0.28f, dampedDrag.pressProgress)),
                                    Color.White.copy(alpha = lerp(0.04f, 0.08f, dampedDrag.pressProgress)),
                                ) else listOf(
                                    Color.White.copy(alpha = lerp(0.70f, 0.90f, dampedDrag.pressProgress)),
                                    Color.White.copy(alpha = lerp(0.40f, 0.60f, dampedDrag.pressProgress)),
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                0f to Color.White.copy(alpha = lerp(0.40f, 0.75f, dampedDrag.pressProgress)),
                                0.5f to Color.White.copy(alpha = lerp(0.12f, 0.30f, dampedDrag.pressProgress)),
                                1f to Color.White.copy(alpha = lerp(0.04f, 0.10f, dampedDrag.pressProgress)),
                            ),
                            shape = pillShape,
                        )
                        .drawWithContent {
                            drawContent()
                            val progress = dampedDrag.pressProgress
                            if (progress > 0.005f) {
                                val localTouchX = if (isLtr) {
                                    dampedDrag.touchOffset.x - dampedDrag.value * tabWidthPx
                                } else {
                                    dampedDrag.touchOffset.x - (totalWidthPx - (dampedDrag.value + 1f) * tabWidthPx)
                                }
                                val canvasWidth = this.size.width
                                val canvasHeight = this.size.height
                                val pillTouchCenter = Offset(
                                    x = localTouchX.coerceIn(0f, canvasWidth),
                                    y = dampedDrag.touchOffset.y.coerceIn(0f, canvasHeight),
                                )
                                val pillGlowRadius = canvasWidth * 1.1f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0.00f to Color.White.copy(alpha = 0.50f * progress),
                                        0.25f to Color.White.copy(alpha = 0.28f * progress),
                                        0.55f to accentColor.copy(alpha = 0.16f * progress),
                                        0.80f to accentColor.copy(alpha = 0.04f * progress),
                                        1.00f to Color.Transparent,
                                        center = pillTouchCenter,
                                        radius = pillGlowRadius,
                                    ),
                                    radius = pillGlowRadius,
                                    center = pillTouchCenter,
                                )
                            }
                        }
                        .height(56.dp)
                        .width(tabWidthDp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(
                        LocalIosTabScale provides {
                            lerp(1f, 0.92f, dampedDrag.pressProgress)
                        },
                        LocalContentColor provides accentColor
                    ) {
                        Row(
                            modifier = Modifier
                                .clearAndSetSemantics {}
                                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                                .height(56.dp)
                                .graphicsLayer {
                                    val progressOffset = dampedDrag.value * tabWidthPx
                                    translationX = if (isLtr) -progressOffset else progressOffset
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            content = tabsContent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    backdrop: LayerBackdrop? = null,
    isBlurActive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    IosLiquidGlassNavigationBar(
        items = items,
        selectedIndex = selectedIndex,
        onItemClick = onItemClick,
        backdrop = backdrop,
        isBlurActive = isBlurActive,
        modifier = modifier,
        isDarkOverride = isDark,
        accentColorOverride = accentColor,
    )
}
