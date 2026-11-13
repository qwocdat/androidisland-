package com.xiao.androidisland.liquidisland.absorption

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Directional Motion Blur & Liquid Deformation Modifier Engine (Section VIII.3 & VIII.5).
 *
 * Uses a 9-tap directional velocity AGSL `RuntimeShader` on Android 13–16 (API 33+)
 * and horizontal directional blur + affine liquid stretch on API 28–32 so that each
 * Status Bar icon blurs strictly along its instantaneous suction/release motion vector
 * rather than using a static isotropic blur or simple fade-out.
 */
object MotionBlurEngine {

    private const val DIRECTIONAL_ABSORPTION_AGSL = """
uniform shader content;
uniform float2 size;
uniform float2 motionVectorPx;
uniform float absorptionProgress;
uniform float towardIslandSign;

half4 main(float2 coord) {
    float2 center = size * 0.5;
    float2 rel = coord - center;

    // Liquid necking deformation toward the Island edge
    float normX = (coord.x / max(size.x, 1.0)) - 0.5;
    float edgeWeight = clamp(0.5 + towardIslandSign * normX, 0.0, 1.0);
    float pinchY = max(0.32, 1.0 - 0.42 * absorptionProgress * edgeWeight);
    float2 warpedCoord = float2(coord.x, center.y + rel.y / pinchY);

    // 9-tap Gaussian-weighted directional velocity blur along motionVectorPx
    half4 color = half4(0.0);
    color += content.eval(warpedCoord - motionVectorPx * 1.00) * 0.05;
    color += content.eval(warpedCoord - motionVectorPx * 0.75) * 0.09;
    color += content.eval(warpedCoord - motionVectorPx * 0.50) * 0.12;
    color += content.eval(warpedCoord - motionVectorPx * 0.25) * 0.15;
    color += content.eval(warpedCoord)                        * 0.18;
    color += content.eval(warpedCoord + motionVectorPx * 0.25) * 0.15;
    color += content.eval(warpedCoord + motionVectorPx * 0.50) * 0.12;
    color += content.eval(warpedCoord + motionVectorPx * 0.75) * 0.09;
    color += content.eval(warpedCoord + motionVectorPx * 1.00) * 0.05;

    return color;
}
"""

    fun Modifier.applyLiquidAbsorption(
        kinematics: AbsorptionEngine.ElementAbsorptionKinematics,
        towardIslandSign: Float,
    ): Modifier {
        if (kinematics.absorptionProgress <= 0.001f && abs(kinematics.directionalMotionBlurPx) <= 0.1f) {
            return this
        }
        return this.graphicsLayer {
            translationX = kinematics.translationXDp.dp.toPx()
            scaleX = kinematics.scaleX
            scaleY = kinematics.scaleY
            alpha = kinematics.coalescenceAlpha

            val blurPx = kinematics.directionalMotionBlurPx
            if (abs(blurPx) > 0.35f && size.width > 1f && size.height > 1f) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    renderEffect = createApi33DirectionalShaderEffect(
                        width = size.width,
                        height = size.height,
                        motionBlurX = blurPx,
                        progress = kinematics.absorptionProgress,
                        towardSign = towardIslandSign,
                    ).asComposeRenderEffect()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Directional X-axis blur (blurY = 0.5px, blurX = |blurPx|)
                    renderEffect = AndroidRenderEffect.createBlurEffect(
                        abs(blurPx).coerceAtLeast(0.5f),
                        0.5f,
                        Shader.TileMode.DECAL,
                    ).asComposeRenderEffect()
                }
            } else {
                renderEffect = null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun createApi33DirectionalShaderEffect(
        width: Float,
        height: Float,
        motionBlurX: Float,
        progress: Float,
        towardSign: Float,
    ): AndroidRenderEffect {
        val shader = RuntimeShader(DIRECTIONAL_ABSORPTION_AGSL).apply {
            setFloatUniform("size", width, height)
            setFloatUniform("motionVectorPx", motionBlurX, 0f)
            setFloatUniform("absorptionProgress", progress)
            setFloatUniform("towardIslandSign", towardSign)
        }
        return AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
    }
}
