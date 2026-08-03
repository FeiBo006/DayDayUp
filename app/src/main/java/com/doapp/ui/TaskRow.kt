package com.doapp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.doapp.data.Task
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One task. Swipe it left past the threshold to complete it — the row tracks the finger 1:1,
 * resists past the reveal width, and hands the release velocity to the spring that settles it.
 */
@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val cardShape = appShape(18.dp)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val density = LocalDensity.current

    val offsetX = remember { Animatable(0f) }
    var rowWidth by remember { mutableFloatStateOf(1f) }
    var armed by remember { mutableStateOf(false) }

    val commitPx = with(density) { 84.dp.toPx() }
    val revealPx = with(density) { 112.dp.toPx() }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.985f else 1f,
        animationSpec = Motion.Snappy,
        label = "pressScale",
    )
    // Completed work stays visible but recedes — present, no longer asking for attention.
    val dim by animateFloatAsState(
        targetValue = if (task.done) 0.45f else 1f,
        animationSpec = tween(220),
        label = "completedDim",
    )

    val dragState = rememberDraggableState { delta ->
        val raw = offsetX.value + delta
        // Left is the direction with an action behind it; the other way is a soft wall.
        val next =
            if (raw > 0f) rubberBand(raw, 0f, rowWidth)
            else rubberBand(raw, revealPx, rowWidth)
        scope.launch { offsetX.snapTo(next) }

        val nowArmed = -next >= commitPx
        if (nowArmed != armed) {
            armed = nowArmed
            if (nowArmed) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val progress = min(1f, abs(min(offsetX.value, 0f)) / commitPx)
    val actionColor = if (task.done) m.tertiaryLabel else m.success

    Box(modifier.fillMaxWidth()) {
        // The action revealed under the row. It only exists while the row is moved aside.
        if (progress > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(cardShape)
                    .background(actionColor.copy(alpha = 0.18f + 0.82f * progress)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = if (task.done) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 28.dp)
                        .size(26.dp)
                        .graphicsLayer {
                            // Hint toward the outcome: the mark grows as the gesture commits.
                            val s = 0.55f + 0.45f * progress + if (armed) 0.12f else 0f
                            scaleX = s
                            scaleY = s
                            alpha = progress
                        },
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .onSizeChanged { rowWidth = it.width.toFloat().coerceAtLeast(1f) }
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                    alpha = dim
                }
                .styleShadow(
                    shape = cardShape,
                    elevation = if (m.isNeoBrutalist) 8.dp else if (task.done) 2.dp else 7.dp,
                    ambientColor = if (m.isNeoBrutalist) Color.Black else Color.Black.copy(alpha = 0.35f),
                    spotColor = if (m.isNeoBrutalist) Color.Black else Color.Black.copy(alpha = 0.35f),
                )
                .clip(cardShape)
                .background(if (pressed) m.cardPressed else m.card)
                .then(
                    when {
                        m.isDoodle -> Modifier.doodleBorder()
                        m.isNeoBrutalist -> Modifier.border(3.dp, Color.Black, cardShape)
                        else -> Modifier.border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(listOf(m.topEdge, Color.Transparent)),
                            shape = cardShape,
                        )
                    }
                )
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    // A row still settling can be grabbed again — never wait for an animation.
                    startDragImmediately = offsetX.isRunning,
                    onDragStarted = { offsetX.stop() },
                    onDragStopped = { velocity ->
                        val projected = offsetX.value + projectMomentum(velocity)
                        val commit = projected <= -commitPx
                        armed = false
                        if (commit) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        }
                        // No seam between drag and animation: the spring starts at the
                        // finger's exact velocity.
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = if (reduceMotion) Motion.Snappy else Motion.Momentum,
                            initialVelocity = velocity,
                        )
                    },
                )
                .pressableNoRipple(interactionSource, onOpen)
                .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        ) {
            CompletionCircle(
                done = task.done,
                onToggle = onToggle,
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.done) m.secondaryLabel else m.label,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = m.secondaryLabel,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                task.reminderAt?.let { at ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = if (task.done) m.tertiaryLabel else m.accent,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = formatReminder(at),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.done) m.tertiaryLabel else m.accent,
                        )
                    }
                }
            }
        }
    }
}
