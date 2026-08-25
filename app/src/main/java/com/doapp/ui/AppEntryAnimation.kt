package com.doapp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doapp.data.Appearance
import com.doapp.ui.theme.materials
import kotlinx.coroutines.delay

private const val Brand = "DayDayUp"
private const val LetterDelayMillis = 46L
private const val LetterDurationMillis = 340
private const val HoldMillis = 110L
private const val ExitMillis = 140
private val EntryEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
fun AppEntryAnimation(
    appearance: Appearance,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val reduceMotion = rememberReduceMotion()
    val overlayAlpha = remember { Animatable(1f) }
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        val visibleMillis = if (reduceMotion) {
            150L
        } else {
            Brand.lastIndex * LetterDelayMillis + LetterDurationMillis + HoldMillis
        }
        delay(visibleMillis)
        overlayAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = if (reduceMotion) 120 else ExitMillis,
                easing = EntryEaseOut,
            ),
        )
        finish()
    }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Keep cold start paint-only. Decoding and blurring a user photo while the letters move
        // can monopolize the GPU exactly when the first impression is being formed.
        Box(
            Modifier
                .fillMaxSize()
                .background(presetOf(appearance.presetId).colors.first()),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (m.isDark) Color.Black.copy(alpha = 0.10f)
                    else Color.White.copy(alpha = 0.06f),
                ),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .height(68.dp)
                    .clipToBounds()
                    .clearAndSetSemantics { contentDescription = Brand },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Brand.forEachIndexed { index, letter ->
                    BrandLetter(letter, index, reduceMotion)
                }
            }

            Spacer(Modifier.height(18.dp))
            val lineProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
            LaunchedEffect(reduceMotion) {
                if (!reduceMotion) {
                    delay(Brand.lastIndex * LetterDelayMillis + 200L)
                    lineProgress.animateTo(1f, tween(220, easing = EntryEaseOut))
                }
            }
            Box(
                Modifier
                    .width(64.dp)
                    .height(4.dp)
                    .graphicsLayer {
                        scaleX = lineProgress.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .background(m.accent),
            )
        }
    }
}

@Composable
private fun BrandLetter(letter: Char, index: Int, reduceMotion: Boolean) {
    val m = materials
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            delay(index * LetterDelayMillis)
            progress.animateTo(1f, tween(LetterDurationMillis, easing = EntryEaseOut))
        }
    }

    Text(
        text = letter.toString(),
        style = MaterialTheme.typography.displaySmall.copy(
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-1.2).sp,
        ),
        color = m.label,
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 52.dp.toPx()
        },
    )
}
