package com.xiao.androidisland.liquidisland.renderer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class LiquidMaterial(
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val blurRadius: Dp,
    val depthEffect: Boolean,
    val chromaticAberration: Float,
    val vibrancySaturation: Float,
    val surfaceTint: Color,
    val rimLightPrimaryColor: Color,
    val rimLightSecondaryColor: Color,
    val rimStrokeWidth: Dp,
    val rimAlpha: Float,
    val innerShadow: InnerShadow?,
    val extraLightDegrees: Float = -45f,
) {
    companion object {
        /**
         * Deep Obsidian Liquid Glass for the Dynamic Island capsule & expanded cards (`96942.mp4`).
         * Combines strong SDF rim refraction (`refractionHeight = 20.dp, refractionAmount = 24.dp`),
         * subtle chromatic dispersion (`0.18f`), and crisp Fresnel specular rim lighting.
         */
        val DynamicIslandObsidianGlass = LiquidMaterial(
            refractionHeight = 20.dp,
            refractionAmount = 24.dp,
            blurRadius = 6.dp,
            depthEffect = true,
            chromaticAberration = 0.18f,
            vibrancySaturation = 1.45f,
            surfaceTint = Color(0xEC08090C),
            rimLightPrimaryColor = Color(0xB8FFFFFF),
            rimLightSecondaryColor = Color(0x6670A5FF),
            rimStrokeWidth = 0.9.dp,
            rimAlpha = 0.72f,
            innerShadow = InnerShadow(
                radius = 12.dp,
                color = Color.White.copy(alpha = 0.08f),
            ),
            extraLightDegrees = -35f,
        )

        /**
         * Translucent Smoked Liquid Glass for iOS Control Center modules (`00:49` in `96942.mp4`)
         * and Home Screen Calendar/Clock widgets (`00:00`).
         */
        val ControlCenterModuleGlass = LiquidMaterial(
            refractionHeight = 22.dp,
            refractionAmount = 26.dp,
            blurRadius = 8.dp,
            depthEffect = true,
            chromaticAberration = 0.16f,
            vibrancySaturation = 1.40f,
            surfaceTint = Color(0x88383C3A),
            rimLightPrimaryColor = Color(0xD9FFFFFF),
            rimLightSecondaryColor = Color(0x80C8D4CE),
            rimStrokeWidth = 1.0.dp,
            rimAlpha = 0.75f,
            innerShadow = InnerShadow(
                radius = 14.dp,
                color = Color.Black.copy(alpha = 0.16f),
            ),
            extraLightDegrees = -45f,
        )

        val ControlCenterActiveTileGlass = LiquidMaterial(
            refractionHeight = 16.dp,
            refractionAmount = 18.dp,
            blurRadius = 6.dp,
            depthEffect = false,
            chromaticAberration = 0.08f,
            vibrancySaturation = 1.25f,
            surfaceTint = Color(0xCC4B514E),
            rimLightPrimaryColor = Color(0xFFFFFFFF),
            rimLightSecondaryColor = Color(0xAA9CE8B5),
            rimStrokeWidth = 1.0.dp,
            rimAlpha = 0.85f,
            innerShadow = InnerShadow(
                radius = 10.dp,
                color = Color.Black.copy(alpha = 0.10f),
            ),
        )
    }
}
