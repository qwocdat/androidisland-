package com.xiao.androidisland.liquidisland.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize

@Stable
interface LiquidBackdrop {
    val isCoordinatesDependent: Boolean
    val offsetResidualX: Float
    val offsetResidualY: Float

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
        downscaleFactor: Int = 1,
    )
}

@Stable
class LayerBackdrop(
    val graphicsLayer: GraphicsLayer,
) : LiquidBackdrop {
    var rootOffset: Offset by mutableStateOf(Offset.Zero)
    var layerSize: IntSize by mutableStateOf(IntSize.Zero)
    var isRecorded: Boolean by mutableStateOf(false)

    override val isCoordinatesDependent: Boolean = true
    override val offsetResidualX: Float get() = rootOffset.x
    override val offsetResidualY: Float get() = rootOffset.y

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        if (!isRecorded) return
        val targetPos = coordinates?.positionInRoot() ?: Offset.Zero
        val dx = rootOffset.x - targetPos.x
        val dy = rootOffset.y - targetPos.y
        drawContext.canvas.save()
        drawContext.canvas.translate(dx, dy)
        drawLayer(graphicsLayer)
        drawContext.canvas.restore()
    }
}

@Stable
class CombinedBackdrop(
    val first: LiquidBackdrop,
    val second: LiquidBackdrop,
) : LiquidBackdrop {
    override val isCoordinatesDependent: Boolean =
        first.isCoordinatesDependent || second.isCoordinatesDependent
    override val offsetResidualX: Float get() = first.offsetResidualX
    override val offsetResidualY: Float get() = first.offsetResidualY

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        with(first) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        with(second) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
    }
}

@Composable
fun rememberLayerBackdrop(): LayerBackdrop {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { LayerBackdrop(graphicsLayer) }
}

fun Modifier.recordLayerBackdrop(backdrop: LayerBackdrop): Modifier =
    this
        .onGloballyPositioned { coords ->
            backdrop.rootOffset = coords.positionInRoot()
            backdrop.layerSize = coords.size
        }
        .then(RecordBackdropElement(backdrop))

private data class RecordBackdropElement(
    val backdrop: LayerBackdrop,
) : ModifierNodeElement<RecordBackdropNode>() {
    override fun create(): RecordBackdropNode = RecordBackdropNode(backdrop)
    override fun update(node: RecordBackdropNode) {
        node.backdrop = backdrop
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "recordLayerBackdrop"
    }
}

private class RecordBackdropNode(
    var backdrop: LayerBackdrop,
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        backdrop.graphicsLayer.record(
            size = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
        ) {
            this@draw.drawContent()
        }
        backdrop.isRecorded = true
        drawLayer(backdrop.graphicsLayer)
    }
}
