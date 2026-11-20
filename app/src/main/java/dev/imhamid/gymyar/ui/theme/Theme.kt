package dev.imhamid.gymyar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import dev.imhamid.gymyar.R

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_bold, FontWeight.ExtraBold),
)

/**
 * Extra semantic colors. The brand is a fresh lime green on soft off-white surfaces:
 * [lime] is for fills (buttons, active tab), [accent] is the readable green for text/icons on light surfaces.
 */
@Immutable
data class GymColors(
    val members: Color,
    val tuition: Color,
    val attendance: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val border: Color,
    val borderStrong: Color,
    val canvas: Color,
    val lime: Color,
    val onLime: Color,
    val accent: Color,
    val headerTop: Color,
    val tileYellow: Color,
    val tilePurple: Color,
    val tileBlue: Color,
    val tileGreen: Color,
    val ink: Color,
    val onInk: Color,
)

private val LimeFill = Color(0xFFB8F36B)

private val LightGym = GymColors(
    members = Color(0xFF7A4CD6),
    tuition = Color(0xFF2A7FD4),
    attendance = Color(0xFFD83486),
    success = Color(0xFF2E8B1F),
    warning = Color(0xFFB87400),
    danger = Color(0xFFD92D35),
    border = Color(0x0F111111),
    borderStrong = Color(0x1F111111),
    canvas = Color(0xFFF6F7F4),
    lime = LimeFill,
    onLime = Color(0xFF111111),
    accent = Color(0xFF3D7A0A),
    headerTop = Color(0xFFDDF6B5),
    tileYellow = Color(0xFFFFF3C4),
    tilePurple = Color(0xFFEADCF8),
    tileBlue = Color(0xFFDCEBFA),
    tileGreen = Color(0xFFE3F7C9),
    ink = Color(0xFF111111),
    onInk = Color.White,
)

private val DarkGym = GymColors(
    members = Color(0xFFC3A6FF),
    tuition = Color(0xFF7FB8F2),
    attendance = Color(0xFFFF8FBF),
    success = Color(0xFF7BDC5C),
    warning = Color(0xFFF3B94C),
    danger = Color(0xFFFF7A74),
    border = Color(0x14FFFFFF),
    borderStrong = Color(0x29FFFFFF),
    canvas = Color(0xFF0F110E),
    lime = LimeFill,
    onLime = Color(0xFF111111),
    accent = Color(0xFFB8F36B),
    headerTop = Color(0xFF1E2A12),
    tileYellow = Color(0xFF2E2A14),
    tilePurple = Color(0xFF282033),
    tileBlue = Color(0xFF16232F),
    tileGreen = Color(0xFF1F2B12),
    ink = Color(0xFFF2F4EF),
    onInk = Color(0xFF111111),
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = LimeFill,
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFFE3F7C9),
    onPrimaryContainer = Color(0xFF1F3A00),
    secondary = Color(0xFF111111),
    onSecondary = Color.White,
    background = Color(0xFFF6F7F4),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEEF0EB),
    onSurfaceVariant = Color(0xFF6B7066),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBF8),
    surfaceContainer = Color(0xFFF1F3EE),
    surfaceContainerHigh = Color(0xFFEBEEE7),
    surfaceContainerHighest = Color(0xFFE4E8DF),
    outline = Color(0xFFCDD1C6),
    outlineVariant = Color(0xFFE6E9E0),
    error = Color(0xFFD92D35),
    onError = Color.White,
    errorContainer = Color(0xFFFBE0E1),
    onErrorContainer = Color(0xFF6B0F14),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = LimeFill,
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF2B3F10),
    onPrimaryContainer = Color(0xFFD9F9AE),
    secondary = Color(0xFFF2F4EF),
    onSecondary = Color(0xFF111111),
    background = Color(0xFF0F110E),
    onBackground = Color(0xFFF2F4EF),
    surface = Color(0xFF181B16),
    onSurface = Color(0xFFF2F4EF),
    surfaceVariant = Color(0xFF22261F),
    onSurfaceVariant = Color(0xFF9AA093),
    surfaceContainerLowest = Color(0xFF0F110E),
    surfaceContainerLow = Color(0xFF141711),
    surfaceContainer = Color(0xFF1C1F1A),
    surfaceContainerHigh = Color(0xFF242820),
    surfaceContainerHighest = Color(0xFF2B3026),
    outline = Color(0xFF3D4236),
    outlineVariant = Color(0xFF2A2E25),
    error = Color(0xFFFF7A74),
    onError = Color(0xFF2B0807),
    errorContainer = Color(0xFF4A1512),
    onErrorContainer = Color(0xFFFFDAD7),
)

private fun TextStyle.fa(): TextStyle {
    val size = if (fontSize.value.isNaN()) 14f else fontSize.value
    return copy(fontFamily = Vazirmatn, lineHeight = (size * 1.7f).sp, letterSpacing = 0.sp)
}

private fun faTypography(): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.fa(), displayMedium = b.displayMedium.fa(), displaySmall = b.displaySmall.fa(),
        headlineLarge = b.headlineLarge.fa(), headlineMedium = b.headlineMedium.fa(), headlineSmall = b.headlineSmall.fa(),
        titleLarge = b.titleLarge.fa().copy(fontWeight = FontWeight.Bold),
        titleMedium = b.titleMedium.fa().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = b.titleSmall.fa().copy(fontWeight = FontWeight.Medium),
        bodyLarge = b.bodyLarge.fa(), bodyMedium = b.bodyMedium.fa(), bodySmall = b.bodySmall.fa(),
        labelLarge = b.labelLarge.fa().copy(fontWeight = FontWeight.Medium),
        labelMedium = b.labelMedium.fa().copy(fontWeight = FontWeight.Medium),
        labelSmall = b.labelSmall.fa().copy(fontWeight = FontWeight.Medium),
    )
}

private val GymShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

private val LocalGymColors = staticCompositionLocalOf { LightGym }

val MaterialTheme.gym: GymColors
    @Composable @ReadOnlyComposable get() = LocalGymColors.current

@Composable
fun GymyarTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(
        LocalGymColors provides (if (dark) DarkGym else LightGym),
        // The whole app is Persian, so the layout is always right-to-left regardless of the phone language.
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = faTypography(),
            shapes = GymShapes,
            content = content,
        )
    }
}
