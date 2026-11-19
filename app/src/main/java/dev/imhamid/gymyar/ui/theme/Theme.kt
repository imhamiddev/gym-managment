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

/** Extra semantic colors, one hue per area of the app (same tokens as the old web app). */
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
)

private val LightGym = GymColors(
    members = Color(0xFF814EDF),
    tuition = Color(0xFF008B86),
    attendance = Color(0xFFD83486),
    success = Color(0xFF00904E),
    warning = Color(0xFFBC7300),
    danger = Color(0xFFDB2B33),
    border = Color(0x14111418),
    borderStrong = Color(0x29111418),
    canvas = Color(0xFFF5F7F9),
)

private val DarkGym = GymColors(
    members = Color(0xFFB394FF),
    tuition = Color(0xFF3ED0C8),
    attendance = Color(0xFFFE82B6),
    success = Color(0xFF46CE83),
    warning = Color(0xFFF3B94C),
    danger = Color(0xFFFF6F69),
    border = Color(0x12FFFFFF),
    borderStrong = Color(0x24FFFFFF),
    canvas = Color(0xFF0D0D0F),
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0A7CF0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFD),
    onPrimaryContainer = Color(0xFF06355F),
    secondary = Color(0xFF00BFC0),
    onSecondary = Color(0xFF05201F),
    background = Color(0xFFF5F7F9),
    onBackground = Color(0xFF111418),
    surface = Color.White,
    onSurface = Color(0xFF111418),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF656970),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFF1F3F5),
    surfaceContainerHigh = Color(0xFFECEFF2),
    surfaceContainerHighest = Color(0xFFE6E9ED),
    outline = Color(0xFFC9CDD2),
    outlineVariant = Color(0xFFE3E6EA),
    error = Color(0xFFDB2B33),
    onError = Color.White,
    errorContainer = Color(0xFFFBE0E1),
    onErrorContainer = Color(0xFF6B0F14),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF3B9CFF),
    onPrimary = Color(0xFF06101C),
    primaryContainer = Color(0xFF0F2B47),
    onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Color(0xFF2AD7D7),
    onSecondary = Color(0xFF032121),
    background = Color(0xFF0D0D0F),
    onBackground = Color(0xFFF0F2F4),
    surface = Color(0xFF151517),
    onSurface = Color(0xFFF0F2F4),
    surfaceVariant = Color(0xFF1D1D20),
    onSurfaceVariant = Color(0xFF8F9298),
    surfaceContainerLowest = Color(0xFF0D0D0F),
    surfaceContainerLow = Color(0xFF121214),
    surfaceContainer = Color(0xFF1D1D20),
    surfaceContainerHigh = Color(0xFF242428),
    surfaceContainerHighest = Color(0xFF2B2B30),
    outline = Color(0xFF3A3A40),
    outlineVariant = Color(0xFF26262A),
    error = Color(0xFFFF6F69),
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
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
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
