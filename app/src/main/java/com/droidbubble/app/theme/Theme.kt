package com.droidbubble.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getAccentColorForKey(key: String): Color {
    return when (key.uppercase()) {
        "BLUE" -> Color(0xFF0A84FF)
        "PURPLE" -> Color(0xFFBF5AF2)
        "PINK" -> Color(0xFFFF375F)
        "RED" -> Color(0xFFFF453A)
        "ORANGE" -> Color(0xFFFF9F0A)
        "GREEN" -> Color(0xFF30D158)
        "CYAN" -> Color(0xFF64D2FF)
        else -> Color(0xFF0A84FF)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DroidbubbleTheme(
    themeMode: String = "LIGHT",
    accentColorKey: String = "BLUE",
    fontFamily: androidx.compose.ui.text.font.FontFamily = GoogleSansFlex,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.uppercase()) {
        "DARK", "OLED" -> true
        "LIGHT" -> false
        else -> systemDark
    }
    val isOled = themeMode.uppercase() == "OLED"
    val primaryAccent = getAccentColorForKey(accentColorKey)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.22f),
            onPrimaryContainer = primaryAccent,
            secondary = primaryAccent,
            secondaryContainer = if (isOled) Color(0xFF121318) else Color(0xFF23252F),
            onSecondaryContainer = Color.White,
            background = if (isOled) Color.Black else Color(0xFF0F1116),
            onBackground = Color(0xFFF2F4F8),
            surface = if (isOled) Color.Black else Color(0xFF171922),
            onSurface = Color(0xFFF2F4F8),
            surfaceVariant = if (isOled) Color(0xFF121318) else Color(0xFF222530),
            onSurfaceVariant = Color(0xFF9BA1B0),
            surfaceContainerLowest = if (isOled) Color.Black else Color(0xFF0B0C10),
            surfaceContainerLow = if (isOled) Color(0xFF0D0E12) else Color(0xFF181A22),
            surfaceContainer = if (isOled) Color(0xFF121318) else Color(0xFF1D202A),
            surfaceContainerHigh = if (isOled) Color(0xFF171920) else Color(0xFF252834),
            surfaceContainerHighest = if (isOled) Color(0xFF1E2028) else Color(0xFF2E3240)
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.14f),
            onPrimaryContainer = primaryAccent,
            secondary = primaryAccent,
            secondaryContainer = Color(0xFFE8EEF7),
            onSecondaryContainer = Color(0xFF1B222C),
            background = Color(0xFFEFF2F7),
            onBackground = Color(0xFF14181F),
            surface = Color(0xFFF8FAFD),
            onSurface = Color(0xFF14181F),
            surfaceVariant = Color(0xFFE3E8F0),
            onSurfaceVariant = Color(0xFF5E6675),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF7F9FC),
            surfaceContainer = Color(0xFFF1F4F9),
            surfaceContainerHigh = Color(0xFFEAEFF6),
            surfaceContainerHighest = Color(0xFFE2E8F0)
        )
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = createTypography(fontFamily),
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
