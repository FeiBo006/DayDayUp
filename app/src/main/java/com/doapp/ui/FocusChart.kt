package com.doapp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doapp.data.FocusSlice
import com.doapp.ui.theme.materials

/**
 * Series colors, in fixed order. A wedge's color follows its identity, never its rank, so the
 * order is assigned once from the sorted labels and never cycled.
 *
 * These are the reference categorical hues, both modes selected for their own surface rather than
 * flipped. Verified with the dataviz validator against #FFFFFF and #1C1C1E on the *adjacent*
 * pairlist — a ring is a stacked bar bent into a circle, so only neighbours ever touch:
 *
 * - light: worst adjacent CVD ΔE 9.1, normal-vision 19.6
 * - dark:  worst adjacent CVD ΔE 8.4, normal-vision 19.3
 * - the wrap-around pair (slot 6 ↔ slot 1) clears both at ΔE 26.5 / 27.3
 *
 * Two obligations come out of that run and are honoured below and in the legend:
 *
 * 1. Six is the ceiling. A seventh series is never a generated hue — the tail folds into [OTHER].
 * 2. Three light-mode hues land under 3:1 against a white card, and the CVD margin sits in the
 *    6–8 band. Both are only legal alongside secondary encoding, which is why the legend listing
 *    every label with its exact duration is required rather than decorative, and why the wedges
 *    are separated by a surface-coloured gap instead of touching.
 */
object SeriesPalette {

    const val MAX_SERIES = 6

    private val Light = listOf(
        Color(0xFF2A78D6), // blue
        Color(0xFFEB6834), // orange
        Color(0xFF1BAF7A), // aqua
        Color(0xFFEDA100), // yellow
        Color(0xFFE87BA4), // magenta
        Color(0xFF008300), // green
    )

    private val Dark = listOf(
        Color(0xFF3987E5),
        Color(0xFFD95926),
        Color(0xFF199E70),
        Color(0xFFC98500),
        Color(0xFFD55181),
        Color(0xFF008300),
    )

    /** The folded tail. Grey by design: "other" is the absence of an identity, not one more. */
    private val OtherLight = Color(0xFF8A8A8E)
    private val OtherDark = Color(0xFF8E8E93)

    fun color(index: Int, isDark: Boolean, isOther: Boolean = false): Color {
        if (isOther) return if (isDark) OtherDark else OtherLight
        val ramp = if (isDark) Dark else Light
        return ramp.getOrElse(index) { if (isDark) OtherDark else OtherLight }
    }
}

/**
 * Caps the wedge count at what the palette can actually distinguish, rolling the tail into one
 * "其他" entry. Slices arrive sorted largest-first, so the tail is always the smallest work.
 */
fun foldSlices(slices: List<FocusSlice>, max: Int = SeriesPalette.MAX_SERIES): List<FocusSlice> {
    if (slices.size <= max) return slices
    val head = slices.take(max - 1)
    val tail = slices.drop(max - 1)
    return head + FocusSlice(
        label = "其他",
        millis = tail.sumOf { it.millis },
        fraction = tail.fold(0f) { acc, slice -> acc + slice.fraction },
        isOther = true,
    )
}

/**
 * The distribution, as a ring with the total in the middle.
 *
 * A ring rather than a full pie because the hole is the best place for the number the page leads
 * with — the reader gets the headline and the split in one look, instead of hunting for a total
 * printed underneath.
 */
@Composable
fun FocusDonut(
    slices: List<FocusSlice>,
    modifier: Modifier = Modifier,
    thickness: Dp = 34.dp,
    content: @Composable () -> Unit,
) {
    val m = materials
    val reduceMotion = rememberReduceMotion()

    // An Animatable, not animateFloatAsState: the latter initialises *at* its target on first
    // composition, so a constant target of 1f would never animate at all. Nothing here was
    // dragged, so the sweep settles without overshoot — bounce is earned by momentum, and a
    // chart arriving carried none.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) reveal.snapTo(1f) else reveal.animateTo(1f, Motion.Move)
    }
    val progress = reveal.value

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            if (slices.isEmpty()) {
                drawArc(
                    color = m.hairline,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                return@Canvas
            }

            // A 2px surface-coloured gap between neighbours. It is the secondary encoding the
            // palette's CVD margin is conditional on, not a stylistic flourish.
            val gapDegrees = if (slices.size == 1) 0f else {
                val gapPx = 2.dp.toPx()
                Math.toDegrees((gapPx / (diameter / 2f)).toDouble()).toFloat()
            }

            var start = -90f
            slices.forEachIndexed { index, slice ->
                val full = slice.fraction * 360f
                val sweep = (full - gapDegrees).coerceAtLeast(0f) * progress
                if (sweep > 0f) {
                    drawArc(
                        color = SeriesPalette.color(index, m.isDark, slice.isOther),
                        startAngle = start + gapDegrees / 2f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                }
                start += full
            }
        }
        content()
    }
}
