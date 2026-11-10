package com.droidbubble.app.ui.liquid

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
)

typealias LiquidNavItem = NavigationItem

enum class Platform {
    Android,
    IOS,
}

fun platform(): Platform = Platform.Android

@Immutable
data class Shadow(
    val radius: Dp = 10.dp,
    val color: Color = Color.Black,
    val alpha: Float = 0.2f,
)

fun Modifier.dropShadow(
    shape: Shape,
    shadow: Shadow,
): Modifier = this.drawWithContent {
    val shadowAlpha = (shadow.color.alpha * shadow.alpha).coerceIn(0f, 1f)
    if (shadowAlpha > 0.01f) {
        val spreadPx = shadow.radius.toPx()
        val outline = shape.createOutline(size, layoutDirection, this)
        if (outline is Outline.Rounded) {
            val r = outline.roundRect.topLeftCornerRadius.x
            drawRoundRect(
                color = shadow.color.copy(alpha = shadowAlpha * 0.45f),
                topLeft = Offset(0f, spreadPx * 0.22f),
                size = size,
                cornerRadius = CornerRadius(r, r),
            )
            drawRoundRect(
                color = shadow.color.copy(alpha = shadowAlpha * 0.25f),
                topLeft = Offset(-spreadPx * 0.1f, spreadPx * 0.35f),
                size = Size(size.width + spreadPx * 0.2f, size.height + spreadPx * 0.15f),
                cornerRadius = CornerRadius(r + spreadPx * 0.1f, r + spreadPx * 0.1f),
            )
        }
    }
    drawContent()
}

@Immutable
data class LightPosition(
    val x: Float,
    val y: Float,
    val z: Float,
)

@Immutable
data class LightSource(
    val position: LightPosition,
    val color: Color,
    val intensity: Float,
)

interface HighlightStyle

@Immutable
data class BloomStroke(
    val color: Color,
    val innerBlurRadius: Dp,
    val primaryLight: LightSource,
    val secondaryLight: LightSource,
    val dualPeak: Boolean = true,
) : HighlightStyle

@Immutable
data class Highlight(
    val width: Dp = 1.dp,
    val alpha: Float = 1f,
    val style: HighlightStyle,
)

@Immutable
data class DeviceTilt(
    val gravityX: Float = 0f,
    val gravityY: Float = -1f,
    val gravityZ: Float = 0f,
)

@Composable
fun rememberDeviceTilt(): State<DeviceTilt> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(DeviceTilt()) }
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val v = event?.values ?: return
                if (v.size >= 2) {
                    val gx = (-v[0] / 9.81f).coerceIn(-1f, 1f)
                    val gy = (-v[1] / 9.81f).coerceIn(-1f, 1f)
                    val gz = if (v.size >= 3) (v[2] / 9.81f).coerceIn(-1f, 1f) else 0f
                    tiltState.value = DeviceTilt(gx, gy, gz)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }
    return tiltState
}

@Stable
interface Backdrop {
    val isCoordinatesDependent: Boolean
    val offsetResidualX: Float
    val offsetResidualY: Float

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    )
}

@Stable
class LayerBackdrop : Backdrop {
    internal var graphicsLayer: GraphicsLayer? = null
    internal var layerCoordinates: LayoutCoordinates? = null

    override val isCoordinatesDependent: Boolean = true
    override val offsetResidualX: Float = 0f
    override val offsetResidualY: Float = 0f

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        val layer = graphicsLayer ?: return
        val srcCoords = layerCoordinates
        val dstCoords = coordinates
        var dx = 0f
        var dy = 0f
        if (srcCoords != null && dstCoords != null && srcCoords.isAttached && dstCoords.isAttached) {
            val srcPos = srcCoords.positionInWindow()
            val dstPos = dstCoords.positionInWindow()
            dx = srcPos.x - dstPos.x
            dy = srcPos.y - dstPos.y
        }
        drawContext.canvas.save()
        drawContext.canvas.translate(dx, dy)
        drawLayer(layer)
        drawContext.canvas.restore()
    }
}

@Composable
fun rememberLayerBackdrop(): LayerBackdrop = remember { LayerBackdrop() }

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier =
    this then LayerBackdropElement(backdrop)

private class LayerBackdropElement(
    val backdrop: LayerBackdrop,
) : ModifierNodeElement<LayerBackdropNode>() {
    override fun create(): LayerBackdropNode = LayerBackdropNode(backdrop)

    override fun update(node: LayerBackdropNode) {
        node.backdrop = backdrop
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
    }

    override fun equals(other: Any?): Boolean =
        other is LayerBackdropElement && backdrop === other.backdrop

    override fun hashCode(): Int = backdrop.hashCode()
}

private class LayerBackdropNode(
    var backdrop: LayerBackdrop,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var internalLayer: GraphicsLayer? = null

    override fun onAttach() {
        internalLayer = requireGraphicsContext().createGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        backdrop.graphicsLayer = internalLayer
    }

    override fun onDetach() {
        internalLayer?.let {
            requireGraphicsContext().releaseGraphicsLayer(it)
        }
        if (backdrop.graphicsLayer === internalLayer) {
            backdrop.graphicsLayer = null
        }
        internalLayer = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.layerCoordinates = coordinates
    }

    override fun ContentDrawScope.draw() {
        val layer = internalLayer
        if (layer != null) {
            layer.record {
                this@draw.drawContent()
            }
            drawLayer(layer)
        } else {
            drawContent()
        }
    }
}

class RuntimeShaderScope @RequiresApi(Build.VERSION_CODES.TIRAMISU) constructor(
    val shader: RuntimeShader,
) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setFloatUniform(name: String, value: Float) {
        shader.setFloatUniform(name, value)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setFloatUniform(name: String, v1: Float, v2: Float) {
        shader.setFloatUniform(name, v1, v2)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setFloatUniform(name: String, values: FloatArray) {
        shader.setFloatUniform(name, values)
    }
}

class BackdropEffectScope(
    override val density: Float,
    override val fontScale: Float,
    val size: Size,
    val shape: Shape,
    val layoutDirection: LayoutDirection,
    val downscaleFactor: Int = 1,
    private val shaderCache: MutableMap<String, Any>,
) : Density {
    var padding: Float = 0f
    private val preEffects = mutableListOf<AndroidRenderEffect>()
    private var shaderEffect: AndroidRenderEffect? = null

    fun isRuntimeShaderSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun colorControls(
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
            val scale = contrast
            val translate = (-0.5f * scale + 0.5f + brightness) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
            satMatrix.postConcat(contrastMatrix)
            preEffects.add(
                AndroidRenderEffect.createColorFilterEffect(ColorMatrixColorFilter(satMatrix))
            )
        }
    }

    fun blur(radiusX: Float, radiusY: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (radiusX > 0.1f || radiusY > 0.1f)) {
            // In miuix, blur(4.dp) runs on a 4x downscaled buffer (= 16.dp full-res Gaussian blur).
            val fullResBlurX = (radiusX * 4.5f).coerceAtLeast(1f)
            val fullResBlurY = (radiusY * 4.5f).coerceAtLeast(1f)
            preEffects.add(
                AndroidRenderEffect.createBlurEffect(
                    fullResBlurX,
                    fullResBlurY,
                    Shader.TileMode.CLAMP,
                )
            )
        }
    }

    fun runtimeShaderEffect(
        key: String,
        shaderString: String,
        uniformShaderName: String,
        block: RuntimeShaderScope.() -> Unit,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeShader = (shaderCache.getOrPut(key) {
                RuntimeShader(shaderString)
            } as RuntimeShader)
            RuntimeShaderScope(runtimeShader).apply(block)
            shaderEffect = AndroidRenderEffect.createRuntimeShaderEffect(runtimeShader, uniformShaderName)
        }
    }

    internal fun buildPreRenderEffect(): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || preEffects.isEmpty()) return null
        var current = preEffects.first()
        for (i in 1 until preEffects.size) {
            current = AndroidRenderEffect.createChainEffect(preEffects[i], current)
        }
        return current.asComposeRenderEffect()
    }

    internal fun buildShaderRenderEffect(): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return shaderEffect?.asComposeRenderEffect()
    }
}

fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: (BackdropEffectScope.() -> Unit)? = null,
    highlight: (() -> Highlight?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
): Modifier = this then DrawBackdropElement(
    backdrop = backdrop,
    shape = shape,
    effects = effects,
    highlight = highlight,
    layerBlock = layerBlock,
    onDrawSurface = onDrawSurface,
)

private class DrawBackdropElement(
    val backdrop: Backdrop,
    val shape: () -> Shape,
    val effects: (BackdropEffectScope.() -> Unit)?,
    val highlight: (() -> Highlight?)?,
    val layerBlock: (GraphicsLayerScope.() -> Unit)?,
    val onDrawSurface: (DrawScope.() -> Unit)?,
) : ModifierNodeElement<DrawBackdropNode>() {
    override fun create(): DrawBackdropNode = DrawBackdropNode(
        backdrop = backdrop,
        shape = shape,
        effects = effects,
        highlight = highlight,
        layerBlock = layerBlock,
        onDrawSurface = onDrawSurface,
    )

    override fun update(node: DrawBackdropNode) {
        node.backdrop = backdrop
        node.shape = shape
        node.effects = effects
        node.highlight = highlight
        node.layerBlock = layerBlock
        node.onDrawSurface = onDrawSurface
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBackdrop"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackdropElement) return false
        return backdrop === other.backdrop &&
            shape === other.shape &&
            effects === other.effects &&
            highlight === other.highlight &&
            layerBlock === other.layerBlock &&
            onDrawSurface === other.onDrawSurface
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + (effects?.hashCode() ?: 0)
        result = 31 * result + (highlight?.hashCode() ?: 0)
        result = 31 * result + (layerBlock?.hashCode() ?: 0)
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        return result
    }
}

private class SimpleGraphicsLayerScope : GraphicsLayerScope {
    override var alpha: Float = 1f
    override var scaleX: Float = 1f
    override var scaleY: Float = 1f
    override var translationX: Float = 0f
    override var translationY: Float = 0f
    override var shadowElevation: Float = 0f
    override var rotationX: Float = 0f
    override var rotationY: Float = 0f
    override var rotationZ: Float = 0f
    override var cameraDistance: Float = 8f
    override var transformOrigin: androidx.compose.ui.graphics.TransformOrigin =
        androidx.compose.ui.graphics.TransformOrigin.Center
    override var shape: Shape = androidx.compose.ui.graphics.RectangleShape
    override var clip: Boolean = false
    override var renderEffect: RenderEffect? = null
    override var ambientShadowColor: Color = Color.Black
    override var spotShadowColor: Color = Color.Black
    override var compositingStrategy: androidx.compose.ui.graphics.CompositingStrategy =
        androidx.compose.ui.graphics.CompositingStrategy.Auto
    override var size: Size = Size.Zero
    override var density: Float = 1f
    override var fontScale: Float = 1f

    fun reset(drawSize: Size, d: Float, fs: Float) {
        alpha = 1f
        scaleX = 1f
        scaleY = 1f
        translationX = 0f
        translationY = 0f
        rotationZ = 0f
        size = drawSize
        density = d
        fontScale = fs
    }
}

/**
 * Two-stage GraphicsLayer backdrop pipeline:
 *  Stage 1 (`preLayer`): records the backdrop and applies `colorControls` (vibrancy) + Gaussian `blur`.
 *  Stage 2 (`shaderLayer`): records `preLayer` and applies the AGSL `RuntimeShader` (`lens` refraction & chromatic aberration).
 * Using two separate GraphicsLayers ensures Android's Skia pipeline blurs the background *before* the RuntimeShader samples `"content"`,
 * preventing unblurred background text from bleeding into the glass pill!
 */
private class DrawBackdropNode(
    var backdrop: Backdrop,
    var shape: () -> Shape,
    var effects: (BackdropEffectScope.() -> Unit)?,
    var highlight: (() -> Highlight?)?,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var onDrawSurface: (DrawScope.() -> Unit)?,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var preLayer: GraphicsLayer? = null
    private var shaderLayer: GraphicsLayer? = null
    private var nodeCoordinates: LayoutCoordinates? = null
    private val clipPath = Path()
    private val shaderCache = mutableMapOf<String, Any>()
    private val layerScope = SimpleGraphicsLayerScope()

    override fun onAttach() {
        val gc = requireGraphicsContext()
        preLayer = gc.createGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        shaderLayer = gc.createGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }

    override fun onDetach() {
        val gc = requireGraphicsContext()
        preLayer?.let { gc.releaseGraphicsLayer(it) }
        shaderLayer?.let { gc.releaseGraphicsLayer(it) }
        preLayer = null
        shaderLayer = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        nodeCoordinates = coordinates
    }

    override fun ContentDrawScope.draw() {
        val currentShape = shape()
        val outline = currentShape.createOutline(size, layoutDirection, this)
        clipPath.reset()
        when (outline) {
            is Outline.Rectangle -> clipPath.addRect(outline.rect)
            is Outline.Rounded -> clipPath.addRoundRect(outline.roundRect)
            is Outline.Generic -> clipPath.addPath(outline.path)
        }

        layerScope.reset(size, density, fontScale)
        layerBlock?.invoke(layerScope)

        val sx = layerScope.scaleX
        val sy = layerScope.scaleY

        val pLayer = preLayer
        val sLayer = shaderLayer

        if (pLayer != null && sLayer != null) {
            val scope = BackdropEffectScope(
                density = density,
                fontScale = fontScale,
                size = size,
                shape = currentShape,
                layoutDirection = layoutDirection,
                downscaleFactor = 1,
                shaderCache = shaderCache,
            )
            effects?.invoke(scope)
            val pad = scope.padding
            val preEffect = scope.buildPreRenderEffect()
            val agslEffect = scope.buildShaderRenderEffect()

            val recordSize = IntSize(
                (size.width + pad * 2f).toInt().coerceAtLeast(1),
                (size.height + pad * 2f).toInt().coerceAtLeast(1),
            )

            // Stage 1: Record backdrop and apply vibrancy + Gaussian blur
            pLayer.renderEffect = preEffect
            pLayer.record(size = recordSize) {
                drawContext.canvas.save()
                drawContext.canvas.translate(pad, pad)
                with(backdrop) {
                    drawBackdrop(
                        density = this@draw,
                        coordinates = nodeCoordinates,
                        layerBlock = layerBlock,
                        downscaleFactor = 1,
                    )
                }
                drawContext.canvas.restore()
            }

            // Stage 2: Apply AGSL lens refraction shader over the blurred preLayer
            sLayer.renderEffect = agslEffect
            sLayer.record(size = recordSize) {
                drawLayer(pLayer)
            }
        }

        drawContext.canvas.save()
        if (sx != 1f || sy != 1f) {
            val pivotX = size.width * 0.5f
            val pivotY = size.height * 0.5f
            drawContext.canvas.translate(pivotX, pivotY)
            drawContext.canvas.scale(sx, sy)
            drawContext.canvas.translate(-pivotX, -pivotY)
        }

        if (sLayer != null) {
            val scope = BackdropEffectScope(
                density = density,
                fontScale = fontScale,
                size = size,
                shape = currentShape,
                layoutDirection = layoutDirection,
                downscaleFactor = 1,
                shaderCache = shaderCache,
            )
            effects?.invoke(scope)
            val pad = scope.padding

            drawContext.canvas.save()
            drawContext.canvas.clipPath(clipPath)
            drawContext.canvas.translate(-pad, -pad)
            drawLayer(sLayer)
            drawContext.canvas.translate(pad, pad)
            onDrawSurface?.invoke(this)
            drawContext.canvas.restore()
        } else {
            drawContext.canvas.save()
            drawContext.canvas.clipPath(clipPath)
            onDrawSurface?.invoke(this)
            drawContext.canvas.restore()
        }

        // Draw BloomStroke dual-peak specular rim highlight
        val h = highlight?.invoke()
        if (h != null && h.alpha > 0.005f) {
            val strokeStyle = h.style as? BloomStroke
            if (strokeStyle != null && outline is Outline.Rounded) {
                val r = outline.roundRect.topLeftCornerRadius.x
                val strokeWidthPx = h.width.toPx().coerceAtLeast(1f)
                val pLight = strokeStyle.primaryLight
                val sLight = strokeStyle.secondaryLight
                val angle = atan2(pLight.position.y - 0.5f, pLight.position.x - 0.5f)
                val cosA = cos(angle)
                val sinA = sin(angle)
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val halfDiag = size.maxDimension * 0.55f
                val startOffset = Offset(cx + cosA * halfDiag, cy + sinA * halfDiag)
                val endOffset = Offset(cx - cosA * halfDiag, cy - sinA * halfDiag)
                val primaryAlpha = (pLight.intensity * 0.55f * h.alpha).coerceIn(0f, 1f)
                val baseAlpha = (strokeStyle.color.alpha * h.alpha).coerceIn(0f, 1f)
                val secondaryAlpha = (sLight.intensity * 0.45f * h.alpha).coerceIn(0f, 1f)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            pLight.color.copy(alpha = primaryAlpha),
                            strokeStyle.color.copy(alpha = baseAlpha),
                            sLight.color.copy(alpha = secondaryAlpha),
                        ),
                        start = startOffset,
                        end = endOffset,
                    ),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = strokeWidthPx),
                )
            }
        }

        drawContent()
        drawContext.canvas.restore()
    }
}

/**
 * Interactive radial press highlight modifier from liquid.zip (`InteractiveHighlight`).
 */
@Stable
class InteractiveHighlight(
    private val animationScope: CoroutineScope,
    private val position: (size: Size, offset: Offset) -> Offset,
) {
    private val pressAlphaAnim = Animatable(0f)
    private var lastTouchOffset by mutableStateOf(Offset.Zero)

    val modifier: Modifier = Modifier.drawWithContent {
        drawContent()
        val a = pressAlphaAnim.value
        if (a > 0.005f) {
            val center = position(size, lastTouchOffset)
            val radius = size.maxDimension * 0.55f
            if (radius > 1f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0.00f to Color.White.copy(alpha = 0.35f * a),
                        0.30f to Color.White.copy(alpha = 0.18f * a),
                        0.65f to Color.White.copy(alpha = 0.06f * a),
                        1.00f to Color.Transparent,
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }
    }

    val gestureModifier: Modifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            lastTouchOffset = down.position
            animationScope.launch {
                pressAlphaAnim.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 400f))
            }
            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull()
                if (change != null) {
                    lastTouchOffset = change.position
                }
            } while (event.changes.any { it.pressed })
            animationScope.launch {
                pressAlphaAnim.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 300f))
            }
        }
    }
}

/**
 * Physics-driven damped drag & press controller from liquid.zip (`DampedDragAnimation`).
 */
@Stable
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float,
    private val valueRange: ClosedFloatingPointRange<Float>,
    private val visibilityThreshold: Float = 0.001f,
    private val initialScale: Float = 1f,
    private val pressedScale: Float = 78f / 56f,
    private val canDrag: (Offset) -> Boolean,
    private val onDragStarted: DampedDragAnimation.(Offset) -> Unit,
    private val onDragStopped: DampedDragAnimation.() -> Unit,
    private val onDragCancelled: DampedDragAnimation.() -> Unit,
    private val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueAnim = Animatable(initialValue, visibilityThreshold)
    private val pressProgressAnim = Animatable(0f, visibilityThreshold)
    private val velocityAnim = Animatable(0f, visibilityThreshold)

    var touchOffset by mutableStateOf(Offset.Zero)

    var targetValue by mutableFloatStateOf(initialValue)
        private set

    val value: Float
        get() = valueAnim.value

    val pressProgress: Float
        get() = pressProgressAnim.value

    val velocity: Float
        get() = velocityAnim.value

    val scaleX: Float
        get() = lerp(initialScale, pressedScale, pressProgressAnim.value)

    val scaleY: Float
        get() = lerp(initialScale, pressedScale, pressProgressAnim.value)

    fun updateValue(newValue: Float) {
        val clamped = newValue.coerceIn(valueRange)
        targetValue = clamped
        animationScope.launch {
            valueAnim.animateTo(
                targetValue = clamped,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 380f,
                    visibilityThreshold = visibilityThreshold,
                ),
            )
        }
    }

    fun animateToValue(newValue: Float) {
        val clamped = newValue.coerceIn(valueRange)
        targetValue = clamped
        animationScope.launch {
            valueAnim.animateTo(
                targetValue = clamped,
                animationSpec = spring(
                    dampingRatio = 0.70f,
                    stiffness = 340f,
                    visibilityThreshold = visibilityThreshold,
                ),
            )
        }
    }

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!canDrag(down.position)) return@awaitEachGesture
            touchOffset = down.position

            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            animationScope.launch {
                pressProgressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 420f),
                )
            }
            onDragStarted(down.position)

            var cancelled = false
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                    if (change == null || !change.pressed) {
                        break
                    }
                    touchOffset = change.position
                    val delta = change.positionChange()
                    if (delta != Offset.Zero) {
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val vx = velocityTracker.calculateVelocity().x
                        val normalizedVel = (vx / 1400f).coerceIn(-2.5f, 2.5f)
                        animationScope.launch {
                            velocityAnim.snapTo(normalizedVel)
                        }
                        onDrag(size, delta)
                        change.consume()
                    }
                }
            } catch (_: Throwable) {
                cancelled = true
            }

            animationScope.launch {
                launch {
                    pressProgressAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
                    )
                }
                launch {
                    velocityAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    )
                }
            }

            if (cancelled) {
                onDragCancelled()
            } else {
                onDragStopped()
            }
        }
    }
}
