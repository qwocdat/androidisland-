package com.xiao.androidisland.liquidisland.absorption

import androidx.compose.runtime.Immutable
import com.xiao.androidisland.core.model.AbsorptionStage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Edge-Based Liquid Absorption Engine (Section VIII).
 *
 * Computes per-frame signed distance from each individual Status Bar icon's bounding box
 * to the moving left or right boundary of the Dynamic Island (including any detached split bubble),
 * and produces continuous kinematics for:
 * - **Phase A (Approach)**: Magnetic liquid attraction toward the approaching Island rim,
 *   horizontal stretch (`scaleX > 1`), and vertical pinch (`scaleY < 1`).
 * - **Phase B (Contact)**: Directional Motion Blur vector `(velocityBlurPx, 0)` aligned with the
 *   instantaneous Island edge velocity `d(EdgeX)/dt` + liquid necking deformation.
 * - **Phase C (Absorption)**: Full compression and coalescence into the Island's SDF surface.
 * - **Reverse Release (When Island contracts)**: Smoothly reverses from `ABSORBED -> RELEASING -> UNTOUCHED`
 *   with outward directional Motion Blur and exact coordinate recovery.
 */
object AbsorptionEngine {

    // Distances in dp relative to the Island's outer edge
    const val APPROACH_OUTER_DISTANCE_DP = 18.0f
    const val CONTACT_DISTANCE_DP = 5.0f
    const val FULL_ABSORPTION_DEPTH_DP = -7.0f

    @Immutable
    data class ElementAbsorptionKinematics(
        val elementId: String,
        val stage: AbsorptionStage,
        /** 0.0 = completely untouched, 1.0 = completely absorbed inside the Island */
        val absorptionProgress: Float,
        /** Horizontal displacement in dp pulling the icon into (or releasing it from) the Island rim */
        val translationXDp: Float,
        /** Horizontal stretch factor (e.g. 1.0 -> 1.42 during liquid suction/release) */
        val scaleX: Float,
        /** Vertical pinch factor (e.g. 1.0 -> 0.45 as the icon necks into the liquid rim) */
        val scaleY: Float,
        /** Signed directional motion blur length in pixels (+ towards right, - towards left) */
        val directionalMotionBlurPx: Float,
        /** Surface coalescence opacity (remains 1.0 during Approach/Contact, merges only inside Phase C) */
        val coalescenceAlpha: Float,
    )

    fun evaluateElementAbsorption(
        elementId: String,
        elementCenterXDp: Float,
        elementWidthDp: Float,
        islandCenterXDp: Float,
        islandLeftEdgeDp: Float,
        islandRightEdgeDp: Float,
        islandEdgeVelocityDpPerSec: Float,
        absorptionIntensity: Float = 1.0f,
        motionBlurIntensity: Float = 1.0f,
        density: Float = 2.75f,
    ): ElementAbsorptionKinematics {
        val isLeftOfIsland = elementCenterXDp < islandCenterXDp
        val halfElementW = elementWidthDp * 0.5f

        // Signed distance from the element's inner edge facing the Island to the Island's outer rim:
        // Positive = outside the Island, 0 = touching rim, Negative = overlapped by the Island rim.
        val signedEdgeDistDp = if (isLeftOfIsland) {
            islandLeftEdgeDp - (elementCenterXDp + halfElementW)
        } else {
            (elementCenterXDp - halfElementW) - islandRightEdgeDp
        }

        if (signedEdgeDistDp >= APPROACH_OUTER_DISTANCE_DP) {
            return ElementAbsorptionKinematics(
                elementId = elementId,
                stage = AbsorptionStage.UNTOUCHED,
                absorptionProgress = 0f,
                translationXDp = 0f,
                scaleX = 1f,
                scaleY = 1f,
                directionalMotionBlurPx = 0f,
                coalescenceAlpha = 1f,
            )
        }

        val totalSpan = APPROACH_OUTER_DISTANCE_DP - FULL_ABSORPTION_DEPTH_DP
        val rawProgress = ((APPROACH_OUTER_DISTANCE_DP - signedEdgeDistDp) / totalSpan).coerceIn(0f, 1f)
        val smoothProgress = smoothstep(0f, 1f, rawProgress) * absorptionIntensity.coerceIn(0.2f, 1.5f)
        val clampedProgress = smoothProgress.coerceIn(0f, 1f)

        val isContracting = if (isLeftOfIsland) {
            islandEdgeVelocityDpPerSec > 8f // Left edge moving right (Island shrinking)
        } else {
            islandEdgeVelocityDpPerSec < -8f // Right edge moving left (Island shrinking)
        }

        val stage = when {
            clampedProgress >= 0.92f -> AbsorptionStage.ABSORBED
            isContracting && clampedProgress > 0.05f -> AbsorptionStage.RELEASING
            signedEdgeDistDp <= CONTACT_DISTANCE_DP -> AbsorptionStage.CONTACT
            else -> AbsorptionStage.APPROACH
        }

        // Direction pointing from the element toward the Island's rim (+1 for left icons, -1 for right icons)
        val towardIslandSign = if (isLeftOfIsland) 1f else -1f

        // Pull element into the liquid meniscus as the rim touches it
        val pullDistanceDp = towardIslandSign * (clampedProgress * clampedProgress) * (elementWidthDp * 0.68f)

        // Bell-curve stretch peaking during Phase B (Contact & Release)
        val contactBell = 4f * clampedProgress * (1f - clampedProgress * 0.65f)
        val speedBoost = (abs(islandEdgeVelocityDpPerSec) / 420f).coerceIn(0f, 0.45f)
        val stretchX = 1f + (0.36f * contactBell + speedBoost * 0.35f) * absorptionIntensity
        val pinchY = (1f - (0.58f * clampedProgress) * absorptionIntensity).coerceIn(0.22f, 1f)

        // Directional Motion Blur strictly proportional to instantaneous movement + contact phase
        // Blur points toward the Island during expansion and away from the Island during release
        val velocityDirection = if (abs(islandEdgeVelocityDpPerSec) > 5f) {
            sign(islandEdgeVelocityDpPerSec)
        } else {
            towardIslandSign
        }
        val baseBlurDp = (abs(islandEdgeVelocityDpPerSec) * 0.022f + contactBell * 6.5f) *
            motionBlurIntensity *
            clampedProgress
        val directionalBlurPx = (baseBlurDp * density).coerceIn(0f, 24f) * velocityDirection

        // Coalescence: element stays visible while stretching in Phase A & B, and only merges
        // inside the dark liquid body at the tail end of Phase C (progress 0.76..1.0)
        val coalescenceAlpha = 1f - smoothstep(0.76f, 0.98f, clampedProgress)

        return ElementAbsorptionKinematics(
            elementId = elementId,
            stage = stage,
            absorptionProgress = clampedProgress,
            translationXDp = pullDistanceDp,
            scaleX = stretchX,
            scaleY = pinchY,
            directionalMotionBlurPx = directionalBlurPx,
            coalescenceAlpha = coalescenceAlpha,
        )
    }

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0).coerceAtLeast(1e-5f)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
