package com.droidbubble.app.ui.liquid

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.droidbubble.app.island.IslandStateManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.model.IslandSettings
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LiquidAnimationSettingsScreen(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val isVi = settings.appLanguage.lowercase() == "vi"

    var expandPreviewSection by remember { mutableStateOf(true) }
    var expandCustomSection by remember { mutableStateOf(true) }
    var expandPresetsSection by remember { mutableStateOf(true) }
    var expandExpansionSection by remember { mutableStateOf(true) }
    var expandCollapseSection by remember { mutableStateOf(true) }
    var expandMorphingSection by remember { mutableStateOf(false) }
    var expandSquashSection by remember { mutableStateOf(false) }
    var expandTouchSection by remember { mutableStateOf(false) }
    var expandMultitaskSection by remember { mutableStateOf(false) }
    var expandTransitionSection by remember { mutableStateOf(false) }
    var expandAdvancedSection by remember { mutableStateOf(false) }

    var isPreviewExpanded by remember { mutableStateOf(false) }

    val damping = (1f - (settings.animExpandBounce * 0.6f)).coerceIn(0.3f, 0.98f)
    val stiffness = (settings.animExpandSpeed * 2.5f).coerceIn(120f, 900f)

    val previewWidth by animateDpAsState(
        targetValue = if (isPreviewExpanded) 230.dp else 96.dp,
        animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
        label = "previewW"
    )
    val previewHeight by animateDpAsState(
        targetValue = if (isPreviewExpanded) 58.dp else 32.dp,
        animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
        label = "previewH"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val activeSliderId by IslandStateManager.activeSliderId.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .graphicsLayer { alpha = if (activeSliderId != null) 0f else 1f },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isVi) "Hoạt ảnh (Animation)" else "Animation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isVi) "Tùy chỉnh cảm giác mở rộng & thu gọn" else "Customize expand & collapse feel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Preview Section
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Xem trước (Preview)" else "Preview",
                    icon = Icons.Default.PlayArrow,
                    expanded = expandPreviewSection,
                    onToggle = { expandPreviewSection = !expandPreviewSection }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { isPreviewExpanded = !isPreviewExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(previewWidth)
                                .height(previewHeight)
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color.Black)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (isPreviewExpanded) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF0A84FF).copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color(0xFF0A84FF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isVi) "Xem trước" else "Preview",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isVi) "Chạm để đóng/mở" else "Tap to toggle",
                                            color = Color.White.copy(alpha = 0.65f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Custom Animations
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Hoạt ảnh tùy chỉnh" else "Custom Animations",
                    icon = Icons.Default.Tune,
                    expanded = expandCustomSection,
                    onToggle = { expandCustomSection = !expandCustomSection }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isVi) "Bật hoạt ảnh tùy chỉnh" else "Enable Custom Animations",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Switch(
                            checked = settings.customAnimationsEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateSettings { it.copy(customAnimationsEnabled = enabled) }
                            }
                        )
                    }
                }
            }

            // 3. Presets (Default, Bouncy, Smooth, Snappy, Elastic, Gentle)
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Bộ mẫu (Presets)" else "Presets",
                    icon = Icons.Default.GridView,
                    expanded = expandPresetsSection,
                    onToggle = { expandPresetsSection = !expandPresetsSection }
                ) {
                    val presets = listOf(
                        Triple("DEFAULT", if (isVi) "Mặc định" else "Default", if (isVi) "Cân bằng chuẩn iOS" else "Balanced iOS-like feel"),
                        Triple("BOUNCY", if (isVi) "Nảy (Bouncy)" else "Bouncy", if (isVi) "Lò xo sống động, độ nảy rõ" else "Playful spring with visible bounce"),
                        Triple("SMOOTH", if (isVi) "Mượt (Smooth)" else "Smooth", if (isVi) "Chuyển động êm, không nảy dư" else "Fluid motion, no bounce"),
                        Triple("SNAPPY", if (isVi) "Nhạy (Snappy)" else "Snappy", if (isVi) "Nhanh và chính xác" else "Fast and precise, minimal overshoot"),
                        Triple("ELASTIC", if (isVi) "Đàn hồi (Elastic)" else "Elastic", if (isVi) "Hiệu ứng dây thun co giãn" else "Stretchy rubber-band effect"),
                        Triple("GENTLE", if (isVi) "Nhẹ nhàng (Gentle)" else "Gentle", if (isVi) "Chậm rãi, cực kỳ êm ái" else "Slow and calm, ultra-soft transitions")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (rowIdx in 0 until 3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (colIdx in 0 until 2) {
                                    val (key, title, desc) = presets[rowIdx * 2 + colIdx]
                                    val isSelected = settings.animationPreset == key
                                    val activeColor = Color(0xFF30D158)
                                    Surface(
                                        onClick = {
                                            onUpdateSettings { s ->
                                                when (key) {
                                                    "BOUNCY" -> s.copy(animationPreset = key, expandDamping = 0.52f, expandStiffness = 360f, animExpandSpeed = 160, animExpandBounce = 0.95f)
                                                    "SMOOTH" -> s.copy(animationPreset = key, expandDamping = 0.88f, expandStiffness = 340f, animExpandSpeed = 140, animExpandBounce = 0.85f)
                                                    "SNAPPY" -> s.copy(animationPreset = key, expandDamping = 0.82f, expandStiffness = 560f, animExpandSpeed = 220, animExpandBounce = 0.65f)
                                                    "ELASTIC" -> s.copy(animationPreset = key, expandDamping = 0.46f, expandStiffness = 280f, animExpandSpeed = 130, animExpandBounce = 0.98f)
                                                    "GENTLE" -> s.copy(animationPreset = key, expandDamping = 0.92f, expandStiffness = 210f, animExpandSpeed = 100, animExpandBounce = 0.50f)
                                                    else -> s.copy(animationPreset = key, expandDamping = 0.74f, expandStiffness = 380f, animExpandSpeed = 150, animExpandBounce = 0.78f)
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isSelected) activeColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(92.dp)
                                            .then(
                                                if (isSelected) Modifier.border(1.5.dp, activeColor, RoundedCornerShape(18.dp))
                                                else Modifier
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when (key) {
                                                        "SMOOTH", "ELASTIC" -> Icons.Default.Waves
                                                        "SNAPPY" -> Icons.Default.Bolt
                                                        "GENTLE" -> Icons.Default.Spa
                                                        else -> Icons.Default.Animation
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = desc,
                                                fontSize = 11.sp,
                                                lineHeight = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Expansion
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Mở rộng (Expansion)" else "Expansion",
                    icon = Icons.Default.OpenInFull,
                    expanded = expandExpansionSection,
                    onToggle = { expandExpansionSection = !expandExpansionSection }
                ) {
                    LiquidSliderRow(
                        label = if (isVi) "Tốc độ (Speed)" else "Speed",
                        value = settings.animExpandSpeed.toFloat(),
                        valueRange = 60f..300f,
                        valueText = "${settings.animExpandSpeed}",
                        showValue = settings.showSliderValues,
                        onValueChange = { v ->
                            onUpdateSettings {
                                it.copy(
                                    animExpandSpeed = v.roundToInt(),
                                    expandStiffness = (v * 2.5f).coerceIn(150f, 800f)
                                )
                            }
                        }
                    )
                    LiquidSliderRow(
                        label = if (isVi) "Độ nảy (Bounce)" else "Bounce",
                        value = settings.animExpandBounce,
                        valueRange = 0.20f..1.00f,
                        valueText = String.format(Locale.US, "%.2f", settings.animExpandBounce),
                        showValue = settings.showSliderValues,
                        onValueChange = { v ->
                            onUpdateSettings {
                                it.copy(
                                    animExpandBounce = v,
                                    expandDamping = (1.15f - v * 0.7f).coerceIn(0.35f, 0.98f)
                                )
                            }
                        }
                    )
                    LiquidSliderRow(
                        label = if (isVi) "Độ vọt lố (Overshoot)" else "Overshoot",
                        value = settings.animExpandOvershoot,
                        valueRange = 0.90f..1.25f,
                        valueText = String.format(Locale.US, "%.2f", settings.animExpandOvershoot),
                        showValue = settings.showSliderValues,
                        onValueChange = { v ->
                            onUpdateSettings { it.copy(animExpandOvershoot = v) }
                        }
                    )
                }
            }

            // 5. Collapse
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Thu gọn (Collapse)" else "Collapse",
                    icon = Icons.Default.CloseFullscreen,
                    expanded = expandCollapseSection,
                    onToggle = { expandCollapseSection = !expandCollapseSection }
                ) {
                    LiquidSliderRow(
                        label = if (isVi) "Tốc độ (Speed)" else "Speed",
                        value = settings.animCollapseSpeed.toFloat(),
                        valueRange = 80f..350f,
                        valueText = "${settings.animCollapseSpeed}",
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(animCollapseSpeed = v.roundToInt()) } }
                    )
                    LiquidSliderRow(
                        label = if (isVi) "Độ nảy (Bounce)" else "Bounce",
                        value = settings.animCollapseBounce,
                        valueRange = 0.20f..1.00f,
                        valueText = String.format(Locale.US, "%.2f", settings.animCollapseBounce),
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(animCollapseBounce = v) } }
                    )
                    LiquidSliderRow(
                        label = if (isVi) "Độ thu nhỏ (Shrink)" else "Shrink",
                        value = settings.animCollapseShrink,
                        valueRange = 0.80f..1.00f,
                        valueText = String.format(Locale.US, "%.2f", settings.animCollapseShrink),
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(animCollapseShrink = v) } }
                    )
                }
            }

            // 6. Shape Morphing
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Biến đổi hình dạng (Shape Morphing)" else "Shape Morphing",
                    icon = Icons.Default.RoundedCorner,
                    expanded = expandMorphingSection,
                    onToggle = { expandMorphingSection = !expandMorphingSection }
                ) {
                    LiquidSliderRow(
                        label = if (isVi) "Tốc độ bo góc (Corner Speed)" else "Corner Speed",
                        value = settings.animCornerSpeed.toFloat(),
                        valueRange = 50f..350f,
                        valueText = "${settings.animCornerSpeed}",
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(animCornerSpeed = v.roundToInt()) } }
                    )
                }
            }

            // 7. Squash & Stretch
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Co giãn đàn hồi (Squash & Stretch)" else "Squash & Stretch",
                    icon = Icons.Default.SwapVert,
                    expanded = expandSquashSection,
                    onToggle = { expandSquashSection = !expandSquashSection }
                ) {
                    LiquidSliderRow(
                        label = if (isVi) "Thời lượng hiệu ứng (Effect Duration)" else "Effect Duration",
                        value = settings.animSquashDurationMs.toFloat(),
                        valueRange = 200f..1500f,
                        valueText = "${settings.animSquashDurationMs} ms",
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(animSquashDurationMs = v.roundToInt()) } }
                    )
                }
            }

            // 8. Touch Feedback
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Phản hồi chạm (Touch Feedback)" else "Touch Feedback",
                    icon = Icons.Default.TouchApp,
                    expanded = expandTouchSection,
                    onToggle = { expandTouchSection = !expandTouchSection }
                ) {
                    LiquidSliderRow(
                        label = if (isVi) "Tỷ lệ nhấn (Press Scale)" else "Press Scale",
                        value = settings.animPressScale,
                        valueRange = 0.80f..1.05f,
                        valueText = String.format(Locale.US, "%.2f", settings.animPressScale),
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(animPressScale = v) } }
                    )
                }
            }

            // 9. Multitasking
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Đa nhiệm (Multitasking)" else "Multitasking",
                    icon = Icons.Default.ViewStream,
                    expanded = expandMultitaskSection,
                    onToggle = { expandMultitaskSection = !expandMultitaskSection }
                ) {
                    LiquidSliderRow(
                        label = if (isVi) "Tốc độ bong bóng (Bubble Speed)" else "Bubble Speed",
                        value = settings.animBubbleSpeed.toFloat(),
                        valueRange = 150f..600f,
                        valueText = "${settings.animBubbleSpeed}",
                        showValue = settings.showSliderValues,
                        onValueChange = { v ->
                            onUpdateSettings {
                                it.copy(
                                    animBubbleSpeed = v.roundToInt(),
                                    splitStiffness = v
                                )
                            }
                        }
                    )
                    LiquidSliderRow(
                        label = if (isVi) "Khoảng cách bong bóng (Bubble Gap)" else "Bubble Gap",
                        value = settings.animBubbleGapDp.toFloat(),
                        valueRange = 2f..18f,
                        valueText = "${settings.animBubbleGapDp} dp",
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(animBubbleGapDp = v.roundToInt()) } }
                    )
                }
            }

            // 10. Content Transitions
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Chuyển cảnh nội dung (Content Transitions)" else "Content Transitions",
                    icon = Icons.Default.Link,
                    expanded = expandTransitionSection,
                    onToggle = { expandTransitionSection = !expandTransitionSection }
                ) {
                    LiquidSliderRow(
                        label = if (isVi) "Tốc độ mờ (Fade Speed)" else "Fade Speed",
                        value = settings.animFadeSpeedMs.toFloat(),
                        valueRange = 100f..1000f,
                        valueText = "${settings.animFadeSpeedMs} ms",
                        showValue = settings.showSliderValues,
                        onValueChange = { v -> onUpdateSettings { it.copy(animFadeSpeedMs = v.roundToInt()) } }
                    )
                }
            }

            // 11. Advanced
            item {
                ExpandableLiquidCard(
                    title = if (isVi) "Nâng cao (Advanced)" else "Advanced",
                    icon = Icons.Default.Settings,
                    expanded = expandAdvancedSection,
                    onToggle = { expandAdvancedSection = !expandAdvancedSection }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isVi) "Hiển thị giá trị thanh trượt" else "Show Slider Values",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Switch(
                            checked = settings.showSliderValues,
                            onCheckedChange = { show ->
                                onUpdateSettings { it.copy(showSliderValues = show) }
                            }
                        )
                    }
                }
            }

            // Reset to Defaults button
            item {
                Button(
                    onClick = {
                        onUpdateSettings {
                            it.copy(
                                animationPreset = "SMOOTH",
                                animExpandSpeed = 140,
                                animExpandBounce = 0.85f,
                                animExpandOvershoot = 1.03f,
                                animCollapseSpeed = 202,
                                animCollapseBounce = 0.90f,
                                animCollapseShrink = 0.98f,
                                animCornerSpeed = 170,
                                animSquashDurationMs = 800,
                                animPressScale = 0.91f,
                                animBubbleSpeed = 396,
                                animBubbleGapDp = 6,
                                animFadeSpeedMs = 500,
                                expandDamping = 0.74f,
                                expandStiffness = 380f,
                                appearDamping = 0.60f,
                                appearStiffness = 280f,
                                splitDamping = 0.63f,
                                splitStiffness = 320f
                            )
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF453A).copy(alpha = 0.14f),
                        contentColor = Color(0xFFFF453A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (isVi) "Đặt lại về mặc định (Reset to Defaults)" else "Reset to Defaults",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun ExpandableLiquidCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val activeSliderId by IslandStateManager.activeSliderId.collectAsState()
    val isDraggingAnySlider = activeSliderId != null
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isDraggingAnySlider) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isDraggingAnySlider) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (isDraggingAnySlider) 0f else 1f }
                    .clickable(enabled = !isDraggingAnySlider, onClick = onToggle)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconTint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun LiquidSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    showValue: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onValueChange: (Float) -> Unit
) {
    val activeSliderId by IslandStateManager.activeSliderId.collectAsState()
    var isDraggingThis by remember { mutableStateOf(false) }
    var localValue by remember(value, isDraggingThis) {
        mutableFloatStateOf(value.coerceIn(valueRange.start, valueRange.endInclusive))
    }
    val hideThisRow = activeSliderId != null && activeSliderId != label && !isDraggingThis

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (hideThisRow) 0f else 1f
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (showValue) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
        Slider(
            value = localValue.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { newVal ->
                isDraggingThis = true
                localValue = newVal
                if (IslandStateManager.activeSliderId.value != label) {
                    val currentPreview = IslandStateManager.livePreviewSettings.value
                    if (currentPreview != null) {
                        IslandStateManager.beginSliderDrag(label, currentPreview)
                    }
                }
                onValueChange(newVal)
            },
            onValueChangeFinished = {
                isDraggingThis = false
                IslandStateManager.endSliderDrag()
                onValueChange(localValue)
            },
            valueRange = valueRange,
            modifier = Modifier.height(28.dp)
        )
    }
}
