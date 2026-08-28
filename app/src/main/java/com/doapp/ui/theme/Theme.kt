package com.doapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import com.doapp.data.Appearance

/** iOS system colors, straight from the Human Interface Guidelines. */
object Palette {
    val Blue = Color(0xFF007AFF)
    val BlueDark = Color(0xFF0A84FF)
    val Green = Color(0xFF34C759)
    val GreenDark = Color(0xFF30D158)
    val Red = Color(0xFFFF3B30)
    val RedDark = Color(0xFFFF453A)
    val Orange = Color(0xFFFF9500)
}

/**
 * Translucent surfaces that sit over the wallpaper. Weight encodes hierarchy: cards are the
 * light material that draws the eye, chrome is thicker so it reads as structure.
 */
@Immutable
data class Materials(
    val card: Color,
    val cardPressed: Color,
    val chrome: Color,
    val hairline: Color,
    val topEdge: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val accent: Color,
    val success: Color,
    val destructive: Color,
    val isDark: Boolean,
)

private val LightMaterials = Materials(
    card = Color.White.copy(alpha = 0.94f),
    cardPressed = Color(0xFFF2F2F7).copy(alpha = 0.96f),
    chrome = Color.White.copy(alpha = 0.96f),
    hairline = Color(0xFF3C3C43).copy(alpha = 0.12f),
    topEdge = Color(0xFF3C3C43).copy(alpha = 0.12f),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.68f),
    tertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.42f),
    accent = Color(0xFF17191C),
    success = Palette.Green,
    destructive = Palette.Red,
    isDark = false,
)

private val DarkMaterials = Materials(
    card = Color(0xFF1C1C1E).copy(alpha = 0.94f),
    cardPressed = Color(0xFF2C2C2E).copy(alpha = 0.96f),
    chrome = Color(0xFF1C1C1E).copy(alpha = 0.96f),
    hairline = Color(0xFFEBEBF5).copy(alpha = 0.12f),
    topEdge = Color(0xFFEBEBF5).copy(alpha = 0.12f),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.68f),
    tertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.40f),
    accent = Color(0xFFFFFFFF),
    success = Palette.GreenDark,
    destructive = Palette.RedDark,
    isDark = true,
)

val LocalMaterials = staticCompositionLocalOf { LightMaterials }

/**
 * Tracking is size-specific: large text needs it pulled in, small text needs it opened up.
 * A single letter-spacing value would be wrong at one end or the other.
 */
private val DoTypography = Typography(
    displaySmall = TextStyle(              // Large title
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(             // Section title
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(               // Headline / emphasized row text
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(                 // Body
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(                // Subhead
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(                // Footnote
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(                // Caption
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun DoTheme(
    appearance: Appearance = Appearance(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The redesigned product is one intentional, white workspace. The system theme no longer
    // swaps in a second visual language; accessibility font and text-color choices still apply.
    val materials = LightMaterials
    val typography = DoTypography
    val colorScheme = lightColorScheme(
        primary = materials.accent,
        surface = Color.White,
        onSurface = materials.label,
        background = Color.White,
        surfaceVariant = Color(0xFFF6F7F8),
        outline = Color(0xFFE4E6E9),
    )

    CompositionLocalProvider(LocalMaterials provides materials) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

@Composable
fun appShape(radius: Dp): Shape = RoundedCornerShape(radius)

val materials: Materials
    @Composable @ReadOnlyComposable get() = LocalMaterials.current
