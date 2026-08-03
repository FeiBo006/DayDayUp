package com.doapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doapp.ui.theme.materials

/** Readable foreground for use on top of [color]: black on light colors, white on dark. */
fun onColor(color: Color): Color = if (color.luminance() > 0.5f) Color.Black else Color.White

/** Ink for hand-drawn strokes; marker teal for the hard offset shadow. */
private val DoodleInk = Color(0xFF2C2C2C)
private val DoodleShadowColor = Color(0xFF4ECDC4)

/**
 * Shadow that becomes a hard marker offset (no blur) in doodle style, a normal elevation
 * everywhere else. Call where you would call [androidx.compose.ui.draw.shadow].
 */
@Composable
fun Modifier.styleShadow(
    shape: Shape,
    elevation: Dp,
    spotColor: Color,
    ambientColor: Color = spotColor,
): Modifier {
    val m = materials
    return if (m.isDoodle) doodleShadow() else this.shadow(elevation, shape, ambientColor = ambientColor, spotColor = spotColor)
}

/**
 * Border that becomes a dashed hand-drawn stroke in doodle style, a normal border everywhere
 * else. Call where you would call [androidx.compose.foundation.border].
 */
@Composable
fun Modifier.styleBorder(
    shape: Shape,
    color: Color,
    width: Dp = 1.dp,
): Modifier {
    val m = materials
    return if (m.isDoodle) doodleBorder(width = width) else this.border(width, color, shape)
}

/** Hard offset shadow — a flat marker swipe instead of a soft blur. */
fun Modifier.doodleShadow(offset: Dp = 4.dp, cornerRadius: Dp = 5.dp): Modifier = drawBehind {
    drawRoundRect(
        color = DoodleShadowColor,
        topLeft = Offset(offset.toPx(), offset.toPx()),
        size = size,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
    )
}

/** Dashed stroke, like a pen line that lifts off the paper. */
fun Modifier.doodleBorder(
    color: Color = DoodleInk,
    width: Dp = 1.5.dp,
    cornerRadius: Dp = 5.dp,
    dash: Dp = 12.dp,
    gap: Dp = 7.dp,
): Modifier = drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
    )
    drawRoundRect(color = color, style = stroke, cornerRadius = CornerRadius(cornerRadius.toPx()))
}

/**
 * A tap target with press feedback but no Material ripple — the card's own scale is the feedback.
 * Every tap lands a haptic tick too, so any functional control announces itself in the hand.
 */
@Composable
fun Modifier.pressableNoRipple(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
    )
}

/**
 * The checkbox. Empty it's a hairline ring; filled it's a solid disc with a check that pops in.
 * The overshoot is earned here — completing something should feel like a small win.
 */
@Composable
fun CompletionCircle(
    done: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val reduceMotion = rememberReduceMotion()
    val fill by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = if (reduceMotion) Motion.Snappy else Motion.Momentum,
        label = "completionFill",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(44.dp)                       // a comfortable target; the drawing is smaller
            .pressableNoRipple(interactionSource, onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(23.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(
                color = m.tertiaryLabel,
                radius = radius - 1.dp.toPx(),
                style = Stroke(width = 1.6.dp.toPx()),
                alpha = 1f - fill,
            )
            if (fill > 0f) {
                drawCircle(color = m.success, radius = radius * fill)
            }
        }
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(15.dp)
                .graphicsLayer {
                    alpha = fill
                    scaleX = fill
                    scaleY = fill
                },
        )
    }
}

/**
 * A translucent surface. Bigger surfaces read as thicker material: more blur, more opacity.
 * Never stack one of these directly on another — legibility collapses.
 */
@Composable
fun GlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 0.dp,
    tint: Color = materials.chrome,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            .background(tint)
    ) { content() }
}
