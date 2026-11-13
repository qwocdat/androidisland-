package com.xiao.androidisland.liquidisland.renderer

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost

fun Modifier.liquidGlassSurface(
    backdrop: LiquidBackdrop,
    shape: Shape,
    material: LiquidMaterial,
    controller: LiquidEffectController,
): Modifier = this
    .then(
        LiquidGlassSurfaceElement(
            backdrop = backdrop,
            shape = shape,
            material = material,
            controller = controller,
        )
    )
    .then(
        if (material.innerShadow != null) {
            Modifier.innerShadow(shape) { material.innerShadow }
        } else {
            Modifier
        }
    )

private data class LiquidGlassSurfaceElement(
    val backdrop: LiquidBackdrop,
    val shape: Shape,
    val material: LiquidMaterial,
    val controller: LiquidEffectController,
) : ModifierNodeElement<LiquidGlassSurfaceNode>() {
    override fun create(): LiquidGlassSurfaceNode =
        LiquidGlassSurfaceNode(backdrop, shape, material, controller)

    override fun update(node: LiquidGlassSurfaceNode) {
        node.backdrop = backdrop
        node.shape = shape
        node.material = material
        node.controller = controller
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlassSurface"
    }
}

private class LiquidGlassSurfaceNode(
    var backdrop: LiquidBackdrop,
    var shape: Shape,
    var material: LiquidMaterial,
    var controller: LiquidEffectController,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var samplingLayer: GraphicsLayer? = null
    private var coordinates: LayoutCoordinates? = null
    private val clipPath = Path()

    override fun onAttach() {
        samplingLayer = requireGraphicsContext().createGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }

    override fun onDetach() {
        samplingLayer?.let { layer ->
            requireGraphicsContext().releaseGraphicsLayer(layer)
            samplingLayer = null
        }
        coordinates = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val layer = samplingLayer
        val density = requireDensity()
        val outline = shape.createOutline(size, layoutDirection, this)

        clipPath.reset()
        when (outline) {
            is Outline.Rectangle -> clipPath.addRect(outline.rect)
            is Outline.Rounded -> clipPath.addRoundRect(outline.roundRect)
            is Outline.Generic -> clipPath.addPath(outline.path)
        }

        val radii = computeCornerRadii(size)
        val refractionHeightPx = material.refractionHeight.toPx()
        val refractionAmountPx = material.refractionAmount.toPx()
        val blurPx = material.blurRadius.toPx()

        if (layer != null && size.width > 1f && size.height > 1f) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                layer.renderEffect = buildApi33LiquidRenderEffect(
                    width = size.width,
                    height = size.height,
                    radii = radii,
                    refractionHeightPx = refractionHeightPx,
                    refractionAmountPx = refractionAmountPx,
                    blurPx = blurPx,
                    depthEffect = material.depthEffect,
                    chromaticAberration = material.chromaticAberration,
                    saturation = material.vibrancySaturation,
                )?.asComposeRenderEffect()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0f) {
                layer.renderEffect = AndroidRenderEffect.createBlurEffect(
                    blurPx,
                    blurPx,
                    Shader.TileMode.CLAMP,
                ).asComposeRenderEffect()
            }

            layer.record(
                size = androidx.compose.ui.unit.IntSize(
                    size.width.toInt().coerceAtLeast(1),
                    size.height.toInt().coerceAtLeast(1),
                )
            ) {
                with(backdrop) {
                    drawBackdrop(
                        density = density,
                        coordinates = coordinates,
                        layerBlock = null,
                        downscaleFactor = 1,
                    )
                }
            }
        }

        clipPath(clipPath) {
            if (layer != null) {
                drawLayer(layer)
            }
            drawRect(color = material.surfaceTint)
        }

        val primaryLight = controller.computePrimaryLightPosition(material.extraLightDegrees)
        val strokeWidthPx = material.rimStrokeWidth.toPx().coerceAtLeast(1f)
        val rimBrush = Brush.linearGradient(
            colors = listOf(
                material.rimLightPrimaryColor.copy(alpha = material.rimAlpha),
                material.rimLightPrimaryColor.copy(alpha = material.rimAlpha * 0.28f),
                material.rimLightSecondaryColor.copy(alpha = material.rimAlpha * 0.72f),
            ),
            start = Offset(
                x = (primaryLight.x * size.width * 0.35f).coerceIn(0f, size.width),
                y = 0f,
            ),
            end = Offset(
                x = size.width,
                y = size.height,
            ),
        )

        val halfStroke = strokeWidthPx * 0.5f
        val avgRadius = radii[0].coerceAtMost(size.minDimension * 0.5f)
        drawRoundRect(
            brush = rimBrush,
            topLeft = Offset(halfStroke, halfStroke),
            size = Size(
                (size.width - strokeWidthPx).coerceAtLeast(1f),
                (size.height - strokeWidthPx).coerceAtLeast(1f),
            ),
            cornerRadius = CornerRadius(avgRadius, avgRadius),
            style = Stroke(width = strokeWidthPx),
        )

        drawContent()
    }

    private fun ContentDrawScope.computeCornerRadii(sizePx: Size): FloatArray {
        val cornerShape = shape as? CornerBasedShape
        val maxRadius = sizePx.minDimension / 2f
        if (cornerShape == null) {
            return floatArrayOf(maxRadius, maxRadius, maxRadius, maxRadius)
        }
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val topLeft = if (isLtr) cornerShape.topStart.toPx(sizePx, this) else cornerShape.topEnd.toPx(sizePx, this)
        val topRight = if (isLtr) cornerShape.topEnd.toPx(sizePx, this) else cornerShape.topStart.toPx(sizePx, this)
        val bottomRight = if (isLtr) cornerShape.bottomEnd.toPx(sizePx, this) else cornerShape.bottomStart.toPx(sizePx, this)
        val bottomLeft = if (isLtr) cornerShape.bottomStart.toPx(sizePx, this) else cornerShape.bottomEnd.toPx(sizePx, this)
        return floatArrayOf(
            topLeft.fastCoerceAtMost(maxRadius),
            topRight.fastCoerceAtMost(maxRadius),
            bottomRight.fastCoerceAtMost(maxRadius),
            bottomLeft.fastCoerceAtMost(maxRadius),
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun buildApi33LiquidRenderEffect(
        width: Float,
        height: Float,
        radii: FloatArray,
        refractionHeightPx: Float,
        refractionAmountPx: Float,
        blurPx: Float,
        depthEffect: Boolean,
        chromaticAberration: Float,
        saturation: Float,
    ): AndroidRenderEffect? {
        if (refractionHeightPx <= 0f || refractionAmountPx <= 0f) return null

        val colorMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }
        val vibrancyEffect = AndroidRenderEffect.createColorFilterEffect(
            ColorMatrixColorFilter(colorMatrix)
        )

        val preBlurEffect = if (blurPx > 0f) {
            AndroidRenderEffect.createBlurEffect(
                blurPx,
                blurPx,
                vibrancyEffect,
                Shader.TileMode.CLAMP,
            )
        } else {
            vibrancyEffect
        }

        val dispersionEnabled = chromaticAberration > 0f
        val shaderSource = if (dispersionEnabled) {
            LiquidShader.ROUNDED_RECT_REFRACTION_WITH_DISPERSION_SHADER
        } else {
            LiquidShader.ROUNDED_RECT_REFRACTION_SHADER
        }

        val runtimeShader = RuntimeShader(shaderSource).apply {
            setFloatUniform("size", width, height)
            setFloatUniform("offset", 0f, 0f)
            setFloatUniform("cornerRadii", radii[0], radii[1], radii[2], radii[3])
            setFloatUniform("refractionHeight", refractionHeightPx)
            setFloatUniform("refractionAmount", -refractionAmountPx)
            setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
            if (dispersionEnabled) {
                setFloatUniform("chromaticAberration", chromaticAberration)
            }
        }

        val refractionEffect = AndroidRenderEffect.createRuntimeShaderEffect(
            runtimeShader,
            "content",
        )

        return AndroidRenderEffect.createChainEffect(refractionEffect, preBlurEffect)
    }
}
