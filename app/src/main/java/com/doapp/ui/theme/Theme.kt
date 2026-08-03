package com.doapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import com.doapp.data.Appearance
import com.doapp.data.AppearanceOptions
import com.doapp.ui.presetOf

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
    val isNeoBrutalist: Boolean,
    val isDoodle: Boolean = false,
)

private val LightMaterials = Materials(
    card = Color.White.copy(alpha = 0.72f),
    cardPressed = Color.White.copy(alpha = 0.88f),
    chrome = Color.White.copy(alpha = 0.80f),
    hairline = Color(0xFF3C3C43).copy(alpha = 0.18f),
    topEdge = Color.White.copy(alpha = 0.55f),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.68f),
    tertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.42f),
    accent = Palette.Blue,
    success = Palette.Green,
    destructive = Palette.Red,
    isDark = false,
    isNeoBrutalist = false,
)

private val DarkMaterials = Materials(
    card = Color(0xFF1C1C1E).copy(alpha = 0.66f),
    cardPressed = Color(0xFF2C2C2E).copy(alpha = 0.86f),
    chrome = Color(0xFF1C1C1E).copy(alpha = 0.78f),
    hairline = Color(0xFFEBEBF5).copy(alpha = 0.18f),
    topEdge = Color.White.copy(alpha = 0.14f),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.68f),
    tertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.40f),
    accent = Palette.BlueDark,
    success = Palette.GreenDark,
    destructive = Palette.RedDark,
    isDark = true,
    isNeoBrutalist = false,
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
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineSmall = TextStyle(             // Section title
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.35).sp,
    ),
    titleMedium = TextStyle(               // Headline / emphasized row text
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
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
        letterSpacing = 0.05.sp,
    ),
    labelSmall = TextStyle(                // Caption
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.12.sp,
    ),
)

@Composable
fun DoTheme(
    appearance: Appearance = Appearance(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkMaterials else LightMaterials
    val materials = materialsFor(base, appearance)
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = materials.accent,
            surface = Color(0xFF1C1C1E),
            onSurface = materials.label,
            background = Color(0xFF000000),
        )
    } else {
        lightColorScheme(
            primary = materials.accent,
            surface = Color(0xFFF7F7F9),
            onSurface = materials.label,
            background = Color(0xFFF2F1F6),
        )
    }

    CompositionLocalProvider(LocalMaterials provides materials) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DoTypography.withFont(appearance.fontId),
            content = content,
        )
    }
}

private fun materialsFor(base: Materials, appearance: Appearance): Materials {
    val text = when (appearance.textColorId) {
        AppearanceOptions.COLOR_INK -> Color(0xFF111111)
        AppearanceOptions.COLOR_NAVY -> Color(0xFF102A43)
        AppearanceOptions.COLOR_FOREST -> Color(0xFF174A35)
        AppearanceOptions.COLOR_BURGUNDY -> Color(0xFF6E1F2F)
        else -> base.label
    }
    val secondary = text.copy(alpha = if (base.isDark) 0.72f else 0.68f)
    val tertiary = text.copy(alpha = if (base.isDark) 0.44f else 0.48f)

    return when (appearance.styleId) {
        AppearanceOptions.STYLE_NEO_BRUTALIST -> {
            // Neo-brutalist keeps its forms — cream cards, black hairlines, hard shadows, square
            // corners — but takes its palette from the wallpaper, so the whole app stays one family.
            val wallpaperAccent = presetOf(appearance.presetId).accent(base.isDark)
            base.copy(
                card = if (base.isDark) Color(0xFFE9E4D0) else Color.White,
                cardPressed = if (base.isDark) Color(0xFFFFD166) else Color(0xFFFFF0A6),
                chrome = wallpaperAccent,
                hairline = Color.Black,
                topEdge = Color.Black,
                label = if (base.isDark) Color.Black else text,
                secondaryLabel = Color.Black.copy(alpha = 0.72f),
                tertiaryLabel = Color.Black.copy(alpha = 0.52f),
                accent = wallpaperAccent,
                success = wallpaperAccent,
                destructive = Color(0xFFD90429),
                isNeoBrutalist = true,
            )
        }

        AppearanceOptions.STYLE_DOODLE -> {
            // Hand-drawn doodle: opaque notebook paper, ink hairlines, marker colors.
            val paper = Color(0xFFFFFEF5)
            val ink = Color(0xFF2C2C2C)
            val doodleText = if (appearance.textColorId == AppearanceOptions.COLOR_DEFAULT) ink else text
            val markerAccent = presetOf(appearance.presetId).accent(base.isDark)
            base.copy(
                card = paper,
                cardPressed = Color(0xFFFFF7D6),
                chrome = paper,
                hairline = Color(0xFF78B9D4).copy(alpha = 0.34f),
                topEdge = ink,
                label = doodleText,
                secondaryLabel = doodleText.copy(alpha = 0.68f),
                tertiaryLabel = doodleText.copy(alpha = 0.45f),
                accent = markerAccent,
                success = Color(0xFF4ECDC4),
                destructive = Color(0xFFD90429),
                isDoodle = true,
            )
        }

        else -> {
            // The accent takes its cue from the wallpaper, so chrome and completion states
            // stay in the same family as the background instead of fighting it.
            val wallpaperAccent = presetOf(appearance.presetId).accent(base.isDark)
            base.copy(
                label = text,
                secondaryLabel = secondary,
                tertiaryLabel = tertiary,
                accent = wallpaperAccent,
                success = wallpaperAccent,
            )
        }
    }
}

private fun Typography.withFont(fontId: String): Typography {
    val family = when (fontId) {
        AppearanceOptions.FONT_SERIF -> FontFamily.Serif
        AppearanceOptions.FONT_MONO -> FontFamily.Monospace
        else -> FontFamily.Default
    }
    return copy(
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
}

@Composable
fun appShape(radius: Dp): Shape = when {
    materials.isNeoBrutalist -> RectangleShape
    materials.isDoodle -> RoundedCornerShape(5.dp)
    else -> RoundedCornerShape(radius)
}

val materials: Materials
    @Composable @ReadOnlyComposable get() = LocalMaterials.current
