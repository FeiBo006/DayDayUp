package com.doapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doapp.ui.theme.materials

/** Readable foreground for use on top of [color]: black on light colors, white on dark. */
fun onColor(color: Color): Color = if (color.luminance() > 0.5f) Color.Black else Color.White

/** A restrained shadow used only where elevation communicates navigation or active state. */
fun Modifier.styleShadow(
    shape: Shape,
    elevation: Dp,
    spotColor: Color,
    ambientColor: Color = spotColor,
): Modifier {
    return this.shadow(elevation, shape, ambientColor = ambientColor, spotColor = spotColor)
}

/** A consistent hairline border for the single minimal visual language. */
fun Modifier.styleBorder(
    shape: Shape,
    color: Color,
    width: Dp = 1.dp,
): Modifier {
    return this.border(width, color, shape)
}

/**
 * A tap target with no Material ripple. Visual press feedback belongs to the surface using this
 * modifier; haptics stay reserved for meaningful thresholds such as completing or committing.
 */
@Composable
fun Modifier.pressableNoRipple(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier {
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
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
        animationSpec = if (reduceMotion) Motion.Instant else Motion.Momentum,
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
            drawCircle(
                color = m.success,
                radius = radius * (0.88f + 0.12f * fill),
                alpha = fill,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(15.dp)
                .graphicsLayer {
                    alpha = fill
                    scaleX = 0.86f + 0.14f * fill
                    scaleY = 0.86f + 0.14f * fill
                },
        )
    }
}
