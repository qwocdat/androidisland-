package com.droidbubble.app.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.droidbubble.app.R

val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex)
)

data class IslandFontOption(
    val key: String,
    val name: String,
    val family: FontFamily
)

val ISLAND_FONTS = listOf(
    IslandFontOption("DEFAULT", "Mặc định hệ thống", FontFamily.Default),
    IslandFontOption("GOOGLE_SANS", "Google Sans Flex", GoogleSansFlex),
    IslandFontOption("SANS_SERIF", "Sans gọn gàng", FontFamily.SansSerif),
    IslandFontOption("SERIF", "Serif cổ điển", FontFamily.Serif),
    IslandFontOption("MONOSPACE", "Đơn cách (Monospace)", FontFamily.Monospace),
    IslandFontOption("ROUNDED", "Bo tròn hiện đại", FontFamily(Typeface.create("sans-serif-rounded", Typeface.NORMAL))),
    IslandFontOption("CONDENSED", "Chữ hẹp (Condensed)", FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))),
    IslandFontOption("CURSIVE", "Chữ viết tay", FontFamily.Cursive)
)

fun getIslandFontFamily(key: String): FontFamily {
    return ISLAND_FONTS.firstOrNull { it.key == key }?.family ?: FontFamily.Default
}

fun createTypography(fontFamily: FontFamily): Typography {
    val defaultTypography = Typography()
    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily)
    )
}

val Typography = createTypography(GoogleSansFlex)
