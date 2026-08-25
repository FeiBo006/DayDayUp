package com.doapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.floor

@Composable
fun LauncherScreen(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (animate) UiverseBoxLoader()
    }
}

@Composable
private fun UiverseBoxLoader() {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "uiverseLoader")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4_000,
                delayMillis = 1_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "uiverseLoaderPhase",
    )

    Canvas(
        Modifier
            .size(112.dp)
            .semantics { contentDescription = "加载动画" },
    ) {
        val unit = 64.dp.toPx()
        val small = 48.dp.toPx()
        val large = 112.dp.toPx()
        val border = 16.dp.toPx()
        val p = phase.coerceIn(0f, 0.9999f)

        fun value(vararg stops: Float): Float {
            val scaled = p * 8f
            val index = floor(scaled).toInt().coerceIn(0, 7)
            val local = FastOutSlowInEasing.transform(scaled - index)
            return stops[index] + (stops[index + 1] - stops[index]) * local
        }

        fun box(x: Float, y: Float, width: Float, height: Float) {
            drawRect(Color(0xFFF5F5F5), Offset(x, y), Size(width, height))
            drawRect(
                Color.White,
                Offset(x + border, y + border),
                Size((width - border * 2).coerceAtLeast(0f), (height - border * 2).coerceAtLeast(0f)),
            )
        }

        box(
            x = value(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            y = value(unit, unit, unit, unit, unit, unit, 0f, 0f, 0f),
            width = value(large, small, small, small, small, small, small, small, small),
            height = value(small, small, small, small, small, small, large, small, small),
        )
        box(
            x = value(0f, 0f, 0f, 0f, 0f, unit, unit, unit, unit),
            y = value(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            width = value(small, small, small, small, large, small, small, small, small),
            height = value(small, small, small, small, small, small, small, small, small),
        )
        box(
            x = value(unit, unit, unit, unit, unit, unit, unit, unit, 0f),
            y = value(0f, 0f, 0f, unit, unit, unit, unit, unit, unit),
            width = value(small, small, small, small, small, small, small, small, large),
            height = value(small, small, large, small, small, small, small, small, small),
        )
    }
}
