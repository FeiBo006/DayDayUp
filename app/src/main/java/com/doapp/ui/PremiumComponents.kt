package com.doapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials

/**
 * The app's single minimal surface: one flat fill and one quiet hairline.
 */
@Composable
fun Modifier.premiumSurface(
    shape: Shape,
    pressed: Boolean = false,
    elevation: Dp = 8.dp,
): Modifier {
    val m = materials
    val base = if (pressed) m.cardPressed else m.card
    return this
        .clip(shape)
        .background(base)
        .border(1.dp, m.hairline, shape)
}

/** Flat primary action with a short scale response. */
@Composable
fun AppPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val m = materials
    val shape = appShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = Motion.Press,
        label = "primaryActionPress",
    )
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.46f
            }
            .clip(shape)
            .background(m.accent)
            .then(if (enabled) Modifier.pressableNoRipple(interactionSource, onClick) else Modifier)
            .heightIn(min = 50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onColor(m.accent),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = onColor(m.accent),
        )
    }
}

/** A small tinted tile that gives recurring row icons one consistent visual home. */
@Composable
fun AppIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val m = materials
    val shape = appShape(13.dp)
    val fill = m.accent.copy(alpha = if (emphasized) 0.16f else 0.10f)
    Box(
        modifier
            .size(42.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, m.accent.copy(alpha = 0.14f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = m.accent,
            modifier = Modifier.size(21.dp),
        )
    }
}
