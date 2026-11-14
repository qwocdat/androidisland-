package com.droidbubble.app.island

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.model.ActiveCallState
import com.droidbubble.app.model.DEFAULT_MEDIA_LAYERS
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import com.droidbubble.app.model.VoiceMemoState
import com.droidbubble.app.ui.liquid.BloomStroke
import com.droidbubble.app.ui.liquid.Highlight
import com.droidbubble.app.ui.liquid.InnerShadow
import com.droidbubble.app.ui.liquid.InteractiveHighlight
import com.droidbubble.app.ui.liquid.LightPosition
import com.droidbubble.app.ui.liquid.LightSource
import com.droidbubble.app.ui.liquid.drawBackdrop
import com.droidbubble.app.ui.liquid.innerShadow
import com.droidbubble.app.ui.liquid.layerBackdrop
import com.droidbubble.app.ui.liquid.lens
import com.droidbubble.app.ui.liquid.rememberDeviceTilt
import com.droidbubble.app.ui.liquid.rememberLayerBackdrop
import com.droidbubble.app.ui.liquid.vibrancy
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

// =========================================================================================
// 1. GRAVITY-REACTIVE DUAL-PEAK SPECULAR RIM HIGHLIGHT (Liquid.zip Optical Specular Engine)
// =========================================================================================

private val liquidIslandBaseHighlight = Highlight(
    width = 1.25.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.22f),
        innerBlurRadius = 2.4.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.35f, -0.05f),
            color = Color.White,
            intensity = 1.15f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.85f, -0.5f),
            color = Color(0xFF93C5FD),
            intensity = 0.55f,
        ),
        dualPeak = true,
    ),
)

@Composable
fun rememberLiquidIslandGravityHighlight(
    accentColor: Color = Color.White,
    enabled: Boolean = true,
    extraDegrees: Float = -45f
): State<Highlight?> {
    val tiltState = rememberDeviceTilt()
    return remember(tiltState, accentColor, enabled, extraDegrees) {
        derivedStateOf {
            if (!enabled) return@derivedStateOf null
            val tilt = tiltState.value
            val gx = tilt.gravityX
            val gy = tilt.gravityY
            val gMagSq = gx * gx + gy * gy
            val stepRad = (3.0 * PI / 180.0).toFloat()
            val baseAngle = if (gMagSq > 0.01f) {
                (atan2(gy, gx) / stepRad).roundToInt() * stepRad
            } else {
                (-PI / 2.0).toFloat()
            }
            val rad = baseAngle + (extraDegrees * PI / 180.0).toFloat()
            val baseStyle = liquidIslandBaseHighlight.style as BloomStroke
            val secondaryTint = if (accentColor != Color.Transparent && accentColor != Color.White) {
                lerp(Color.White, accentColor, 0.55f)
            } else {
                Color(0xFFBAE6FD)
            }
            liquidIslandBaseHighlight.copy(
                style = baseStyle.copy(
                    primaryLight = baseStyle.primaryLight.copy(
                        position = LightPosition(
                            x = 0.5f + cos(rad),
                            y = 0.7f + sin(rad),
                            z = baseStyle.primaryLight.position.z
                        )
                    ),
                    secondaryLight = baseStyle.secondaryLight.copy(
                        color = secondaryTint
                    )
                )
            )
        }
    }
}

// =========================================================================================
// 2. AGSL 9-TAP DIRECTIONAL VELOCITY MOTION BLUR ENGINE (Dynamic Motion 1.2 Physics)
// =========================================================================================

private const val AGSL_DIRECTIONAL_VELOCITY_BLUR_SHADER = """
uniform shader content;
uniform float2 resolution;
uniform float2 velocityPx;

half4 main(float2 fragCoord) {
    float speed = length(velocityPx);
    if (speed < 0.35) {
        return content.eval(fragCoord);
    }
    float2 dir = velocityPx / max(speed, 0.0001);
    float blurSpan = clamp(speed * 0.42, 0.0, 26.0);
    float2 stepVec = dir * (blurSpan / 4.0);

    // 9-tap normalized Gaussian kernel along instantaneous velocity vector
    half4 sum = half4(0.0);
    sum += content.eval(clamp(fragCoord - stepVec * 4.0, float2(0.0), resolution)) * 0.028;
    sum += content.eval(clamp(fragCoord - stepVec * 3.0, float2(0.0), resolution)) * 0.066;
    sum += content.eval(clamp(fragCoord - stepVec * 2.0, float2(0.0), resolution)) * 0.124;
    sum += content.eval(clamp(fragCoord - stepVec * 1.0, float2(0.0), resolution)) * 0.179;
    sum += content.eval(fragCoord) * 0.206;
    sum += content.eval(clamp(fragCoord + stepVec * 1.0, float2(0.0), resolution)) * 0.179;
    sum += content.eval(clamp(fragCoord + stepVec * 2.0, float2(0.0), resolution)) * 0.124;
    sum += content.eval(clamp(fragCoord + stepVec * 3.0, float2(0.0), resolution)) * 0.066;
    sum += content.eval(clamp(fragCoord + stepVec * 4.0, float2(0.0), resolution)) * 0.028;
    return sum;
}
"""

@Composable
fun rememberIslandVelocityVector(
    currentWidthDp: Dp,
    currentHeightDp: Dp,
    enabled: Boolean
): Offset {
    val density = LocalDensity.current
    var velocity by remember { mutableStateOf(Offset.Zero) }
    var prevW by remember { mutableFloatStateOf(with(density) { currentWidthDp.toPx() }) }
    var prevH by remember { mutableFloatStateOf(with(density) { currentHeightDp.toPx() }) }

    LaunchedEffect(currentWidthDp, currentHeightDp, enabled) {
        if (!enabled) {
            velocity = Offset.Zero
            return@LaunchedEffect
        }
        val targetW = with(density) { currentWidthDp.toPx() }
        val targetH = with(density) { currentHeightDp.toPx() }
        val dw = targetW - prevW
        val dh = targetH - prevH
        prevW = targetW
        prevH = targetH
        if (abs(dw) > 0.4f || abs(dh) > 0.4f) {
            velocity = Offset(dw * 1.6f, dh * 1.6f)
            withFrameNanos { }
            delay(32)
            velocity = Offset(velocity.x * 0.35f, velocity.y * 0.35f)
            delay(32)
            velocity = Offset.Zero
        } else {
            velocity = Offset.Zero
        }
    }
    return velocity
}

@Composable
fun Modifier.liquidVelocityMotionBlur(
    velocityPx: Offset,
    enabled: Boolean
): Modifier {
    if (!enabled) return this
    val speed = hypot(velocityPx.x, velocityPx.y)
    if (speed < 0.5f) return this

    val shaderHolder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(AGSL_DIRECTIONAL_VELOCITY_BLUR_SHADER) }.getOrNull()
        } else null
    }

    return this.graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderHolder != null && size.width > 1f && size.height > 1f) {
            runCatching {
                shaderHolder.setFloatUniform("resolution", size.width, size.height)
                shaderHolder.setFloatUniform("velocityPx", velocityPx.x, velocityPx.y)
                renderEffect = AndroidRenderEffect
                    .createRuntimeShaderEffect(shaderHolder, "content")
                    .asComposeRenderEffect()
            }
        } else {
            val stretchX = (1f + (abs(velocityPx.x) / 380f).coerceIn(0f, 0.08f))
            val squashY = (1f - (abs(velocityPx.x) / 760f).coerceIn(0f, 0.04f))
            scaleX *= stretchX
            scaleY *= squashY
        }
    }
}

// =========================================================================================
// 3. TRUE OPTICAL LIQUID GLASS CONTAINER FOR DYNAMIC ISLAND (Compact Pill, Bubble & Cards)
// =========================================================================================

@Composable
fun LiquidIslandOpticalBox(
    settings: IslandSettings,
    shape: Shape,
    accentColor: Color = Color.Transparent,
    isExpandedCard: Boolean = false,
    albumArtBitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val isUltraOled = settings.ultraPowerSaverEnabled && settings.ultraTrueBlackOled
    if (isUltraOled) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(Color.Black)
                .border(0.8.dp, Color.White.copy(alpha = 0.14f), shape),
            contentAlignment = contentAlignment,
            content = content
        )
        return
    }

    val internalBackdrop = rememberLayerBackdrop()
    val coroutineScope = rememberCoroutineScope()
    val interactiveHighlight = remember(coroutineScope) {
        InteractiveHighlight(
            animationScope = coroutineScope,
            position = { size, touchOffset ->
                if (touchOffset != Offset.Zero) {
                    Offset(
                        x = touchOffset.x.coerceIn(0f, size.width),
                        y = touchOffset.y.coerceIn(0f, size.height)
                    )
                } else {
                    Offset(size.width * 0.5f, size.height * 0.5f)
                }
            }
        )
    }

    val effectiveAccent = if (accentColor != Color.Transparent) {
        accentColor
    } else {
        when (settings.accentColorKey.uppercase(Locale.US)) {
            "PURPLE" -> Color(0xFFA855F7)
            "PINK" -> Color(0xFFEC4899)
            "RED" -> Color(0xFFEF4444)
            "ORANGE" -> Color(0xFFF97316)
            "GREEN" -> Color(0xFF22C55E)
            "CYAN" -> Color(0xFF06B6D4)
            else -> Color(0xFF38BDF8)
        }
    }

    val gravityHighlight = rememberLiquidIslandGravityHighlight(
        accentColor = effectiveAccent,
        enabled = settings.dynamicLightGlow,
        extraDegrees = if (isExpandedCard) -35f else 90f
    )

    // Animated caustic phase for subsurface liquid refraction
    val runCaustics = !settings.ultraPowerSaverEnabled || !settings.ultraStaticIndicators
    val causticPhase = if (runCaustics) {
        val transition = rememberInfiniteTransition(label = "liquid_island_caustics")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (isExpandedCard) 5400 else 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "causticPhase"
        )
        p
    } else {
        0.75f
    }

    val userOpacity = ((settings.glassOpacity / 100f) * settings.islandOpacity).coerceIn(0.22f, 0.96f)
    val refractionMul = settings.liquidRefraction.coerceIn(0.50f, 2.00f)
    val lensHeightDp = if (isExpandedCard) (22.dp * refractionMul) else (14.dp * refractionMul)
    val lensAmountDp = if (isExpandedCard) (26.dp * refractionMul) else (18.dp * refractionMul)
    val dispersionStrength = (0.18f * refractionMul).coerceIn(0.08f, 0.38f)

    Box(
        modifier = modifier.clip(shape),
        contentAlignment = contentAlignment
    ) {
        // Layer 1: Subsurface Optical Caustic & Album Gradient Field recorded into LayerBackdrop
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(internalBackdrop)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    // Deep optical obsidian-sapphire base so camera cutout blends cleanly while rims refract
                    val topBase = Color(0xFF050811).copy(alpha = (userOpacity + 0.14f).coerceIn(0.45f, 0.96f))
                    val bottomBase = Color(0xFF0B1324).copy(alpha = (userOpacity * 0.82f).coerceIn(0.28f, 0.88f))
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(topBase, bottomBase)
                        )
                    )

                    // Orbiting subsurface caustic light lobes that get refracted by AGSL lens() at the rim
                    val lobe1X = w * (0.28f + 0.18f * cos(causticPhase))
                    val lobe1Y = h * (0.32f + 0.22f * sin(causticPhase))
                    val lobe2X = w * (0.74f + 0.16f * sin(causticPhase * 0.85f))
                    val lobe2Y = h * (0.68f + 0.20f * cos(causticPhase * 0.85f))
                    val radius1 = maxOf(w, h) * (if (isExpandedCard) 0.62f else 0.78f)
                    val radius2 = maxOf(w, h) * (if (isExpandedCard) 0.55f else 0.68f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                effectiveAccent.copy(alpha = if (isExpandedCard) 0.36f else 0.28f),
                                effectiveAccent.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(lobe1X, lobe1Y),
                            radius = radius1.coerceAtLeast(1f)
                        ),
                        center = Offset(lobe1X, lobe1Y),
                        radius = radius1.coerceAtLeast(1f)
                    )

                    val secondaryCaustic = lerp(effectiveAccent, Color.White, 0.45f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryCaustic.copy(alpha = if (isExpandedCard) 0.24f else 0.20f),
                                secondaryCaustic.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = Offset(lobe2X, lobe2Y),
                            radius = radius2.coerceAtLeast(1f)
                        ),
                        center = Offset(lobe2X, lobe2Y),
                        radius = radius2.coerceAtLeast(1f)
                    )

                    // Top optical meniscus horizon line
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.24f),
                            0.18f to Color.White.copy(alpha = 0.05f),
                            0.82f to Color.Transparent,
                            1.0f to effectiveAccent.copy(alpha = 0.16f)
                        )
                    )
                }
        ) {
            if (isExpandedCard && albumArtBitmap != null && !albumArtBitmap.isRecycled) {
                Image(
                    bitmap = albumArtBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer {
                            scaleX = 1.32f
                            scaleY = 1.32f
                            alpha = 0.58f
                        }
                )
            }
        }

        // Layer 2: AGSL Optical Lens Refraction + Vibrancy + 3D InnerShadow + Gravity BloomStroke Rim
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBackdrop(
                    backdrop = internalBackdrop,
                    shape = { shape },
                    effects = {
                        padding = maxOf(padding, 28.dp.toPx())
                        vibrancy()
                        blur(
                            if (isExpandedCard) 3.5.dp.toPx() else 2.5.dp.toPx(),
                            if (isExpandedCard) 3.5.dp.toPx() else 2.5.dp.toPx()
                        )
                        lens(
                            refractionHeight = lensHeightDp.toPx(),
                            refractionAmount = lensAmountDp.toPx(),
                            depthEffect = true,
                            chromaticAberration = dispersionStrength
                        )
                    },
                    highlight = { gravityHighlight.value },
                    onDrawSurface = {
                        // Subtle glass frost film + top/bottom Fresnel sheen
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.White.copy(alpha = 0.14f),
                                0.35f to Color(0xFF0A101F).copy(alpha = (userOpacity * 0.42f).coerceIn(0.12f, 0.55f)),
                                1.0f to Color(0xFF050811).copy(alpha = (userOpacity * 0.65f).coerceIn(0.20f, 0.78f))
                            )
                        )
                    }
                )
                .innerShadow(
                    shape = shape,
                    shadow = {
                        InnerShadow(
                            radius = if (isExpandedCard) 12.dp else 7.dp,
                            offset = DpOffset(0.dp, if (isExpandedCard) 2.5.dp else 1.5.dp),
                            color = Color.White.copy(alpha = 0.28f),
                            alpha = 1f,
                            blendMode = BlendMode.SrcOver
                        )
                    }
                )
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier)
        )

        // Layer 3: Foreground Island Content
        content()
    }
}

// =========================================================================================
// 4. SMOOTH-MINIMUM (smin) LIQUID METABALL BRIDGE BETWEEN MAIN PILL & SECONDARY BUBBLE
// =========================================================================================

private const val AGSL_METABALL_BRIDGE_SHADER = """
uniform float2 resolution;
uniform float2 mainCenter;
uniform float2 mainHalfSize;
uniform float mainRadius;
uniform float2 bubbleCenter;
uniform float bubbleRadius;
uniform float sminK;
uniform float4 glassColor;
uniform float4 rimColor;

float sdRoundedBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float sdCircle(float2 p, float r) {
    return length(p) - r;
}

float sminPoly(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / max(k, 0.001), 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

half4 main(float2 fragCoord) {
    float dMain = sdRoundedBox(fragCoord - mainCenter, mainHalfSize, mainRadius);
    float dBub = sdCircle(fragCoord - bubbleCenter, bubbleRadius);
    float dUnion = sminPoly(dMain, dBub, sminK);

    // Only draw the liquid bridge neck between the two bodies so we don't double-darken their interiors
    if (dUnion > 0.5 || dMain < -2.0 || dBub < -2.0) {
        return half4(0.0);
    }

    float edgeAlpha = 1.0 - smoothstep(-1.2, 0.5, dUnion);
    float rimFactor = smoothstep(-3.2, -0.4, dUnion) * edgeAlpha;
    float verticalHighlight = clamp(1.0 - (fragCoord.y / max(resolution.y, 1.0)), 0.0, 1.0);

    float4 baseCol = mix(glassColor, rimColor, rimFactor * (0.45 + 0.55 * verticalHighlight));
    return half4(baseCol.rgb * edgeAlpha, baseCol.a * edgeAlpha);
}
"""

@Composable
fun LiquidMetaballBridgeCanvas(
    mainPillWidth: Dp,
    mainPillHeight: Dp,
    mainPillCenterX: Dp,
    bubbleSize: Dp,
    bubbleCenterX: Dp,
    surfaceTension: Float,
    isLiquidGlass: Boolean,
    accentColor: Color,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.01f || mainPillWidth <= 4.dp) return
    val density = LocalDensity.current
    val mainRightEdge = mainPillCenterX + (mainPillWidth / 2f)
    val mainLeftEdge = mainPillCenterX - (mainPillWidth / 2f)
    val bubbleLeftEdge = bubbleCenterX - (bubbleSize / 2f)
    val bubbleRightEdge = bubbleCenterX + (bubbleSize / 2f)

    // Compute edge-to-edge gap regardless of whether bubble is on the right or left
    val gapDp = if (bubbleCenterX >= mainPillCenterX) {
        bubbleLeftEdge - mainRightEdge
    } else {
        mainLeftEdge - bubbleRightEdge
    }

    // Surface tension (1..8) controls the maximum reach of the liquid bridge before snapping
    val maxReachDp = (10f + surfaceTension * 2.4f).dp
    if (gapDp > maxReachDp || gapDp < (-bubbleSize * 0.65f)) return

    val bridgeStretch = (1f - (gapDp.value / maxReachDp.value)).coerceIn(0f, 1f)
    if (bridgeStretch <= 0.01f) return

    val canvasWidthDp = mainPillWidth + bubbleSize * 2f + 48.dp
    val canvasHeightDp = maxOf(mainPillHeight, bubbleSize)

    val agslShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(AGSL_METABALL_BRIDGE_SHADER) }.getOrNull()
        } else null
    }

    Canvas(
        modifier = modifier
            .width(canvasWidthDp)
            .height(canvasHeightDp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val mainCenterPx = Offset(cx + mainPillCenterX.toPx(), cy)
        val bubbleCenterPx = Offset(cx + bubbleCenterX.toPx(), cy)
        val mainHalfW = mainPillWidth.toPx() / 2f
        val mainHalfH = mainPillHeight.toPx() / 2f
        val bubbleR = bubbleSize.toPx() / 2f
        val kPx = (6f + surfaceTension * 2.8f).dp.toPx() * bridgeStretch

        // Analytic cubic-bezier hourglass meniscus path between main pill and secondary bubble
        val isRight = bubbleCenterPx.x >= mainCenterPx.x
        val anchorMainX = if (isRight) mainCenterPx.x + mainHalfW - 4.dp.toPx() else mainCenterPx.x - mainHalfW + 4.dp.toPx()
        val anchorBubX = if (isRight) bubbleCenterPx.x - bubbleR + 4.dp.toPx() else bubbleCenterPx.x + bubbleR - 4.dp.toPx()
        val midX = (anchorMainX + anchorBubX) * 0.5f

        val anchorSpreadY = minOf(mainHalfH, bubbleR) * (0.48f + 0.36f * bridgeStretch)
        val waistHalfH = minOf(mainHalfH, bubbleR) * (0.14f + 0.58f * bridgeStretch * bridgeStretch)

        val neckPath = Path().apply {
            moveTo(anchorMainX, cy - anchorSpreadY)
            cubicTo(
                x1 = (anchorMainX + midX) * 0.5f,
                y1 = cy - waistHalfH,
                x2 = (midX + anchorBubX) * 0.5f,
                y2 = cy - waistHalfH,
                x3 = anchorBubX,
                y3 = cy - anchorSpreadY
            )
            lineTo(anchorBubX, cy + anchorSpreadY)
            cubicTo(
                x1 = (midX + anchorBubX) * 0.5f,
                y1 = cy + waistHalfH,
                x2 = (anchorMainX + midX) * 0.5f,
                y2 = cy + waistHalfH,
                x3 = anchorMainX,
                y3 = cy + anchorSpreadY
            )
            close()
        }

        if (isLiquidGlass) {
            val rimTint = if (accentColor != Color.Transparent) {
                lerp(Color.White, accentColor, 0.45f)
            } else {
                Color(0xFFBAE6FD)
            }
            drawPath(
                path = neckPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A).copy(alpha = 0.88f * bridgeStretch),
                        Color(0xFF050811).copy(alpha = 0.92f * bridgeStretch),
                        Color(0xFF0F172A).copy(alpha = 0.86f * bridgeStretch)
                    ),
                    startY = cy - anchorSpreadY,
                    endY = cy + anchorSpreadY
                )
            )
            drawPath(
                path = neckPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        rimTint.copy(alpha = 0.52f * bridgeStretch),
                        Color.White.copy(alpha = 0.12f * bridgeStretch),
                        rimTint.copy(alpha = 0.36f * bridgeStretch)
                    ),
                    startY = cy - anchorSpreadY,
                    endY = cy + anchorSpreadY
                ),
                style = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            drawPath(
                path = neckPath,
                color = Color.Black.copy(alpha = bridgeStretch)
            )
        }
    }
}

// =========================================================================================
// 5. 3-PHASE EDGE-BASED STATUS BAR LIQUID ABSORPTION & RELEASE (Dynamic Motion 1.2)
// =========================================================================================

@Composable
fun LiquidStatusBarAbsorptionOverlay(
    compactWidth: Dp,
    compactHeight: Dp,
    restingWidth: Dp,
    surfaceTension: Float,
    accentColor: Color,
    isLiquidGlass: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLiquidGlass || compactWidth <= restingWidth + 6.dp) return
    val expansionDelta = (compactWidth - restingWidth).value.coerceAtLeast(0f)
    // Phase A (Approach: 6..28dp) -> Phase B (Contact & Stretch: 28..58dp) -> Phase C (Absorption: >58dp)
    val phaseProgress = (expansionDelta / 68f).coerceIn(0f, 1f)
    if (phaseProgress <= 0.02f || phaseProgress >= 0.98f) return

    // Bell-shaped absorption intensity peaking during Contact/Absorption transition
    val bellIntensity = sin(phaseProgress * PI.toFloat()).coerceIn(0f, 1f)
    val dropletScaleX = 1.0f + 0.55f * bellIntensity * (surfaceTension / 4f).coerceIn(0.6f, 1.5f)
    val dropletScaleY = (1.0f - 0.28f * bellIntensity).coerceAtLeast(0.55f)
    val dropletTone = if (accentColor != Color.Transparent) {
        lerp(Color.White, accentColor, 0.35f)
    } else {
        Color(0xFFE0F2FE)
    }

    val totalOverlayWidth = compactWidth + 40.dp
    Canvas(
        modifier = modifier
            .width(totalOverlayWidth)
            .height(compactHeight)
    ) {
        val cy = size.height / 2f
        val halfPillW = compactWidth.toPx() / 2f
        val centerX = size.width / 2f
        val leftEdgeX = centerX - halfPillW
        val rightEdgeX = centerX + halfPillW

        // Distance of absorbed status bar indicator droplet from island rim
        val pullDistPx = (1f - phaseProgress) * 14.dp.toPx()
        val dropW = 7.dp.toPx() * dropletScaleX
        val dropH = 4.5.dp.toPx() * dropletScaleY

        // Left & Right absorbed status-bar meniscus droplets with motion-blurred tail
        for (sign in intArrayOf(-1, 1)) {
            val rimX = if (sign < 0) leftEdgeX else rightEdgeX
            val dropCenterX = rimX + sign * pullDistPx
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = if (sign < 0) {
                        listOf(
                            Color.Transparent,
                            dropletTone.copy(alpha = 0.32f * bellIntensity),
                            dropletTone.copy(alpha = 0.72f * bellIntensity)
                        )
                    } else {
                        listOf(
                            dropletTone.copy(alpha = 0.72f * bellIntensity),
                            dropletTone.copy(alpha = 0.32f * bellIntensity),
                            Color.Transparent
                        )
                    },
                    startX = dropCenterX - dropW,
                    endX = dropCenterX + dropW
                ),
                topLeft = Offset(dropCenterX - dropW / 2f, cy - dropH / 2f),
                size = Size(dropW, dropH),
                cornerRadius = CornerRadius(dropH / 2f, dropH / 2f)
            )
        }
    }
}

// =========================================================================================
// 6. iOS PRIVACY INDICATOR DOT (Camera Green / Microphone Orange)
// =========================================================================================

@Composable
fun LiquidPrivacyIndicatorDot(
    priority: IslandPriority,
    isCallActive: Boolean,
    isVoiceMemoActive: Boolean,
    isRecordingActive: Boolean,
    offsetPx: Int,
    modifier: Modifier = Modifier
) {
    val isMicInUse = isCallActive || isVoiceMemoActive || priority == IslandPriority.CALL || priority == IslandPriority.VOICE_MEMO
    val isCamOrScreenInUse = isRecordingActive || priority == IslandPriority.SCREEN_RECORDING
    if (!isMicInUse && !isCamOrScreenInUse) return

    val dotColor = if (isCamOrScreenInUse) Color(0xFF30D158) else Color(0xFFFF9500)
    val pulseTransition = rememberInfiniteTransition(label = "privacy_dot_pulse")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "privacyGlowAlpha"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(x = -offsetPx, y = 0) }
            .size(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dotColor.copy(alpha = 0.45f * glowAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(5.5.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = glowAlpha))
        )
    }
}

// =========================================================================================
// 7. LIQUID ISLAND 6-BAR HARMONIC WAVEFORM VISUALIZER (Dynamic Motion 1.2 Media & Call)
// =========================================================================================

@Composable
fun LiquidSixBarVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    barCount: Int = 6,
    maxHeightDp: Dp = 18.dp,
    barWidthDp: Dp = 2.6.dp,
    modifier: Modifier = Modifier
) {
    val baseColor = if (accentColor != Color.Transparent) {
        lerp(Color.White, accentColor, 0.65f)
    } else {
        Color(0xFF38BDF8)
    }

    if (!isPlaying) {
        Row(
            modifier = modifier.height(maxHeightDp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.2.dp)
        ) {
            val idleFracs = listOf(0.26f, 0.42f, 0.32f, 0.48f, 0.30f, 0.22f)
            for (i in 0 until barCount) {
                val f = idleFracs[i % idleFracs.size]
                Box(
                    modifier = Modifier
                        .width(barWidthDp)
                        .height(maxHeightDp * f)
                        .clip(RoundedCornerShape(barWidthDp / 2f))
                        .background(baseColor.copy(alpha = 0.55f))
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "liquid_six_bar_eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 780, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eqPhase"
    )

    val phaseOffsets = remember { floatArrayOf(0.0f, 1.15f, 2.35f, 0.75f, 1.95f, 3.10f) }
    val ampScale = remember { floatArrayOf(0.68f, 0.95f, 0.82f, 1.00f, 0.78f, 0.62f) }

    Row(
        modifier = modifier.height(maxHeightDp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.2.dp)
    ) {
        for (i in 0 until barCount) {
            val wave = (0.28f + 0.72f * abs(sin(phase + phaseOffsets[i % phaseOffsets.size]))) * ampScale[i % ampScale.size]
            val barColor = lerp(baseColor, Color.White, (wave * 0.38f).coerceIn(0f, 0.4f))
            Box(
                modifier = Modifier
                    .width(barWidthDp)
                    .height((maxHeightDp * wave).coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(barWidthDp / 2f))
                    .background(barColor)
            )
        }
    }
}

// =========================================================================================
// 8. LIQUID ISLAND EXPANDED MEDIA PLAYER (96942.mp4 00:13 & 01:25 "Album Gradient" + Glass)
// =========================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidExpandedMediaView(
    media: MediaInfo?,
    settings: IslandSettings,
    onCollapse: () -> Unit,
    scale: Float = 1.0f
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSeeking by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var isSeekPending by remember { mutableStateOf(false) }
    var isFavoriteLocal by remember(media?.title) { mutableStateOf(media?.isLiked == true) }

    val rawDuration = media?.durationMs ?: 0L
    val hasValidDuration = rawDuration > 0L
    val duration = if (hasValidDuration) rawDuration else 0L

    var livePosition by remember(media?.packageName, media?.title) {
        mutableLongStateOf(media?.computeCurrentPosition() ?: 0L)
    }

    LaunchedEffect(media?.isPlaying, media?.positionMs, media?.lastPositionUpdateTimeMs, isSeeking, isSeekPending) {
        if (!isSeeking && !isSeekPending) {
            livePosition = media?.computeCurrentPosition() ?: 0L
            if (media?.isPlaying == true) {
                while (isActive) {
                    delay(200)
                    if (!isSeeking && !isSeekPending) {
                        livePosition = media?.computeCurrentPosition() ?: 0L
                    }
                }
            }
        }
    }

    val currentPosition = when {
        isSeeking -> if (hasValidDuration) (scrubFraction * duration).toLong().coerceIn(0L, duration) else livePosition
        else -> livePosition
    }

    val sliderProgress = when {
        isSeeking -> scrubFraction
        hasValidDuration -> (livePosition.toFloat() / duration).coerceIn(0f, 1f)
        else -> 0f
    }

    val displayArt = media?.albumArt ?: media?.appIcon
    val mediaAccent = remember(displayArt) {
        val extracted = extractMediaAccentColor(displayArt)
        if (extracted != Color.Transparent) extracted else Color(0xFF38BDF8)
    }
    val brightAccent = remember(mediaAccent) { lerp(mediaAccent, Color.White, 0.35f) }

    val artSize = (54.dp * scale).coerceIn(44.dp, 64.dp)
    val isPlaying = media?.isPlaying == true
    val vinylFraction by animateFloatAsState(
        targetValue = if (settings.spinningAlbumArt && isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 300f),
        label = "liquidVinylFrac"
    )
    val artCornerRadius = 14.dp + ((artSize / 2f) - 14.dp) * vinylFraction
    val vinylRotation = remember { Animatable(0f) }
    LaunchedEffect(settings.spinningAlbumArt, isPlaying) {
        if (settings.spinningAlbumArt && isPlaying) {
            while (isActive) {
                val startRot = vinylRotation.value % 360f
                vinylRotation.snapTo(startRot)
                vinylRotation.animateTo(
                    targetValue = startRot + 360f,
                    animationSpec = tween(durationMillis = 9000, easing = LinearEasing)
                )
            }
        }
    }

    val resolvedLayers = if (settings.mediaLayersOrder.isNotEmpty()) settings.mediaLayersOrder else DEFAULT_MEDIA_LAYERS

    val headerLayer: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover with optical glass rim border
            Box(
                modifier = Modifier
                    .size(artSize)
                    .graphicsLayer {
                        if (settings.spinningAlbumArt || vinylFraction > 0.01f) {
                            rotationZ = vinylRotation.value * vinylFraction
                        }
                    }
                    .clip(RoundedCornerShape(artCornerRadius))
                    .background(Color(0xFF1E293B))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.55f),
                                brightAccent.copy(alpha = 0.25f)
                            )
                        ),
                        shape = RoundedCornerShape(artCornerRadius)
                    )
                    .clickable {
                        launchAppOrIntent(context, media?.packageName, media?.sessionActivity)
                        onCollapse()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (displayArt != null && !displayArt.isRecycled) {
                    Image(
                        bitmap = displayArt.asImageBitmap(),
                        contentDescription = "Ảnh bìa",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = brightAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media?.title?.ifBlank { "Không có nhạc đang phát" } ?: "Không có nhạc đang phát",
                    color = Color.White,
                    fontSize = (16.5f * scale).coerceIn(14.5f, 18.5f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1000,
                        velocity = 30.dp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = media?.artist?.ifBlank { "Nghệ sĩ không xác định" } ?: "Nghệ sĩ không xác định",
                    color = Color.White.copy(alpha = 0.74f),
                    fontSize = (13f * scale).coerceIn(11.5f, 14.5f).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1500,
                        velocity = 26.dp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (settings.eqShowEqualizer) {
                LiquidSixBarVisualizer(
                    isPlaying = isPlaying && settings.eqRealTimeVisuals,
                    accentColor = brightAccent,
                    barCount = 6,
                    maxHeightDp = 20.dp
                )
            }
        }
    }

    val sliderLayer: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatLiquidDuration(currentPosition),
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(10.dp))

            if (settings.wavyProgressBar) {
                WavyMediaScrubber(
                    progress = sliderProgress,
                    enabled = hasValidDuration,
                    isPlaying = isPlaying && !isSeeking,
                    activeColor = brightAccent,
                    inactiveColor = Color.White.copy(alpha = 0.22f),
                    onValueChange = { frac ->
                        isSeeking = true
                        scrubFraction = frac
                    },
                    onValueChangeFinished = {
                        if (hasValidDuration) {
                            val seekTargetMs = (scrubFraction * duration).toLong().coerceIn(0L, duration)
                            livePosition = seekTargetMs
                            isSeekPending = true
                            isSeeking = false
                            IslandStateManager.onSeekTo?.invoke(seekTargetMs)
                            coroutineScope.launch {
                                delay(650)
                                isSeekPending = false
                            }
                        } else {
                            isSeeking = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                )
            } else {
                Slider(
                    value = sliderProgress,
                    enabled = hasValidDuration,
                    onValueChange = { frac ->
                        isSeeking = true
                        scrubFraction = frac
                    },
                    onValueChangeFinished = {
                        if (hasValidDuration) {
                            val seekTargetMs = (scrubFraction * duration).toLong().coerceIn(0L, duration)
                            livePosition = seekTargetMs
                            isSeekPending = true
                            isSeeking = false
                            IslandStateManager.onSeekTo?.invoke(seekTargetMs)
                            coroutineScope.launch {
                                delay(650)
                                isSeekPending = false
                            }
                        } else {
                            isSeeking = false
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = brightAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            val remainingMs = if (hasValidDuration) (duration - currentPosition).coerceAtLeast(0L) else 0L
            Text(
                text = if (hasValidDuration) "-${formatLiquidDuration(remainingMs)}" else "--:--",
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    val controlsLayer: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Favorite / Like Glass Pill Button
            LiquidGlassIconPillButton(
                onClick = { isFavoriteLocal = !isFavoriteLocal },
                size = (38.dp * scale).coerceIn(32.dp, 42.dp),
                accentColor = if (isFavoriteLocal) Color(0xFFFF2D55) else Color.White
            ) {
                Icon(
                    imageVector = if (isFavoriteLocal) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (isFavoriteLocal) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center transport cluster (Previous, Liquid Glass Play/Pause Pill, Next)
            Row(
                horizontalArrangement = Arrangement.spacedBy((16.dp * scale).coerceIn(12.dp, 22.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconPillButton(
                    onClick = { IslandStateManager.onSkipPrevious?.invoke() },
                    size = (42.dp * scale).coerceIn(36.dp, 46.dp),
                    accentColor = brightAccent
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Bài trước",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Primary Liquid Glass Play/Pause Capsule
                Box(
                    modifier = Modifier
                        .width((58.dp * scale).coerceIn(48.dp, 64.dp))
                        .height((44.dp * scale).coerceIn(38.dp, 50.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    lerp(Color.White, brightAccent, 0.25f).copy(alpha = 0.88f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { IslandStateManager.onPlayPause?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Phát/Tạm dừng",
                        tint = Color(0xFF090D16),
                        modifier = Modifier.size(26.dp)
                    )
                }

                LiquidGlassIconPillButton(
                    onClick = { IslandStateManager.onSkipNext?.invoke() },
                    size = (42.dp * scale).coerceIn(36.dp, 46.dp),
                    accentColor = brightAccent
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Bài tiếp",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Right: AirPlay / Audio Output Route Button (matches 00:13 in 96942.mp4)
            LiquidGlassIconPillButton(
                onClick = {
                    runCatching {
                        val panelIntent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(panelIntent)
                    }.onFailure {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                    }
                    onCollapse()
                },
                size = (38.dp * scale).coerceIn(32.dp, 42.dp),
                accentColor = brightAccent
            ) {
                Icon(
                    imageVector = Icons.Default.SpeakerGroup,
                    contentDescription = "Đầu ra âm thanh",
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        resolvedLayers.forEach { layerKey ->
            when (layerKey) {
                "COVER_AND_TITLE" -> headerLayer()
                "SLIDER" -> sliderLayer()
                "CONTROLS" -> controlsLayer()
            }
        }
    }
}

@Composable
fun LiquidGlassIconPillButton(
    onClick: () -> Unit,
    size: Dp,
    accentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        accentColor.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                width = 0.9.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

private fun formatLiquidDuration(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}


fun resolveLiquidPriorityAccentColor(
    priority: IslandPriority?,
    mediaAccentColor: Color = Color.Transparent
): Color = when (priority) {
    IslandPriority.MEDIA -> if (mediaAccentColor != Color.Transparent) mediaAccentColor else Color(0xFF38BDF8)
    IslandPriority.CALL -> Color(0xFF30D158)
    IslandPriority.CHARGING -> Color(0xFF7CE8A0)
    IslandPriority.VOICE_MEMO, IslandPriority.SCREEN_RECORDING -> Color(0xFFFF453A)
    IslandPriority.ALARM, IslandPriority.WEATHER -> Color(0xFFFF9F0A)
    IslandPriority.FLASHLIGHT -> Color(0xFFD2D6FF)
    IslandPriority.FPS -> Color(0xFF30D158)
    IslandPriority.PROGRESS, IslandPriority.POP_NOTIFICATION, IslandPriority.NETWORK_SPEED -> Color(0xFF38BDF8)
    else -> Color(0xFF38BDF8)
}
