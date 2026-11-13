package com.xiao.androidisland

import com.xiao.androidisland.core.model.AbsorptionPhase
import com.xiao.androidisland.liquidisland.absorption.AbsorptionEngine
import com.xiao.androidisland.liquidisland.absorption.MotionBlurEngine
import com.xiao.androidisland.liquidisland.renderer.LiquidShader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidAbsorptionAndShaderTest {

    @Test
    fun `idle 120dp pill keeps status bar clusters in Rest phase`() {
        val frame = AbsorptionEngine.evaluateFrame(
            screenWidthDp = 360f,
            islandWidthDp = 120f,
            islandVelocityDpPerSec = 0f,
            splitRightBubbleOffsetDp = 0f,
            absorptionStrength = 1.0f,
            motionBlurIntensity = 1.0f
        )

        assertEquals(AbsorptionPhase.REST, frame.leftCluster.phase)
        assertEquals(AbsorptionPhase.REST, frame.rightCluster.phase)
        assertEquals(1.0f, frame.leftCluster.alpha, 0.01f)
        assertEquals(1.0f, frame.rightCluster.alpha, 0.01f)
    }

    @Test
    fun `expanded 295dp ringer or charging pill fully absorbs status bar clusters`() {
        val frame = AbsorptionEngine.evaluateFrame(
            screenWidthDp = 360f,
            islandWidthDp = 295f,
            islandVelocityDpPerSec = 420f,
            splitRightBubbleOffsetDp = 0f,
            absorptionStrength = 1.0f,
            motionBlurIntensity = 1.0f
        )

        assertEquals(AbsorptionPhase.ABSORBED, frame.leftCluster.phase)
        assertEquals(AbsorptionPhase.ABSORBED, frame.rightCluster.phase)
        assertTrue(frame.leftCluster.translationXDp > 0f)
        assertTrue(frame.rightCluster.translationXDp < 0f)
        assertTrue(frame.leftCluster.alpha < 0.05f)
        assertTrue(frame.rightCluster.alpha < 0.05f)
    }

    @Test
    fun `contact phase applies horizontal directional motion blur and scale deformation`() {
        val frame = AbsorptionEngine.evaluateFrame(
            screenWidthDp = 360f,
            islandWidthDp = 210f,
            islandVelocityDpPerSec = 520f,
            splitRightBubbleOffsetDp = 0f,
            absorptionStrength = 1.0f,
            motionBlurIntensity = 1.0f
        )

        assertTrue(frame.leftCluster.scaleX > 1.0f)
        assertTrue(frame.leftCluster.scaleY < 1.0f)
        assertTrue(frame.leftCluster.blurRadiusPx > 0f)

        val taps = MotionBlurEngine.computeTapOffsets(frame.leftCluster)
        assertEquals(9, taps.size)
    }

    @Test
    fun `agsl and glsl liquid shaders contain lens refraction and metaball smooth union`() {
        assertTrue(LiquidShader.LIQUID_LENS_AGSL.contains("sdRoundedBox"))
        assertTrue(LiquidShader.LIQUID_LENS_AGSL.contains("smin"))
        assertTrue(LiquidShader.DIRECTIONAL_MOTION_BLUR_AGSL.contains("blurVectorPx"))
    }
}
