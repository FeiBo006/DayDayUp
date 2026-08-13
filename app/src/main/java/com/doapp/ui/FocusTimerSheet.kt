package com.doapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doapp.data.ActiveFocus
import com.doapp.data.Bucket
import com.doapp.data.FocusMode
import com.doapp.data.Task
import com.doapp.data.formatClock
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Countdown lengths worth one tap. Anything else is what the stopwatch is for. */
private val CountdownMinutes = listOf(15, 25, 45, 60)

/**
 * Start a focus run, or watch the one that's going.
 *
 * The clock is computed from wall-clock stamps rather than counted up by the ticker, so the
 * displayed time survives the process being killed, the screen going off, or the app sitting in
 * the background for an hour. The ticker only decides how often to re-read it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerSheet(
    active: ActiveFocus?,
    tasks: List<Task>,
    onStart: (label: String, taskId: String?, mode: FocusMode, targetMillis: Long) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val m = materials
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (m.isNeoBrutalist || m.isDoodle) m.card
        else if (m.isDark) Color(0xFF1C1C1E) else Color(0xFFF7F7F9),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (active == null) {
                StartFocus(tasks = tasks, onStart = onStart)
            } else {
                RunningFocus(
                    active = active,
                    onPause = onPause,
                    onResume = onResume,
                    onFinish = onFinish,
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun StartFocus(
    tasks: List<Task>,
    onStart: (String, String?, FocusMode, Long) -> Unit,
) {
    val m = materials
    val today = LocalDate.now().toEpochDay()

    // Today's open work, plus whatever Plan items have come due — the same promotion rule the
    // list uses, so what you can focus on matches what the list says you're doing.
    val candidates = remember(tasks, today) {
        tasks.filterNot { it.isTrashed || it.done }
            .filter { it.bucket == Bucket.TODAY || (it.planDay != null && it.planDay <= today) }
    }

    var selected by remember(candidates) { mutableStateOf(candidates.firstOrNull()) }
    var mode by remember { mutableStateOf(FocusMode.STOPWATCH) }
    var minutes by remember { mutableStateOf(25) }

    Text("开始专注", style = MaterialTheme.typography.headlineSmall, color = m.label)

    if (candidates.isEmpty()) {
        Text(
            "今天还没有待办。也可以不挑任务，直接开始一段专注。",
            style = MaterialTheme.typography.bodyMedium,
            color = m.secondaryLabel,
        )
    } else {
        Text("专注在", style = MaterialTheme.typography.bodyMedium, color = m.secondaryLabel)
        Column(
            Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            candidates.forEach { task ->
                TaskChoiceRow(
                    title = task.title,
                    selected = selected?.id == task.id,
                    onClick = { selected = if (selected?.id == task.id) null else task },
                )
            }
        }
    }

    Text("方式", style = MaterialTheme.typography.bodyMedium, color = m.secondaryLabel)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusChip(
            label = "正计时",
            selected = mode == FocusMode.STOPWATCH,
            onClick = { mode = FocusMode.STOPWATCH },
            modifier = Modifier.weight(1f),
        )
        FocusChip(
            label = "倒计时",
            selected = mode == FocusMode.COUNTDOWN,
            onClick = { mode = FocusMode.COUNTDOWN },
            modifier = Modifier.weight(1f),
        )
    }

    if (mode == FocusMode.COUNTDOWN) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountdownMinutes.forEach { value ->
                FocusChip(
                    label = "$value 分",
                    selected = minutes == value,
                    onClick = { minutes = value },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    FocusPrimaryButton(
        label = "开始",
        onClick = {
            val task = selected
            onStart(
                task?.title ?: "专注",
                task?.id,
                mode,
                if (mode == FocusMode.COUNTDOWN) minutes * 60_000L else 0L,
            )
        },
    )

    Text(
        "不满一分钟的专注不会被记录。",
        style = MaterialTheme.typography.labelLarge,
        color = m.tertiaryLabel,
    )
}

@Composable
private fun RunningFocus(
    active: ActiveFocus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    val m = materials

    // Re-read the clock rather than count it. A ticker that increments a counter drifts and lies
    // after the process sleeps; a timestamp read every second cannot.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active.isPaused) {
        while (!active.isPaused) {
            now = System.currentTimeMillis()
            delay(250)
        }
        now = System.currentTimeMillis()
    }

    val elapsed = active.elapsed(now)
    val countdownDone = active.isComplete(now)
    val display = when {
        active.mode == FocusMode.COUNTDOWN && !countdownDone -> active.remaining(now)
        active.mode == FocusMode.COUNTDOWN -> 0L
        else -> elapsed
    }

    Text(
        text = active.label,
        style = MaterialTheme.typography.headlineSmall,
        color = m.label,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = when {
            countdownDone -> "这一轮走完了"
            active.isPaused -> "已暂停"
            active.mode == FocusMode.COUNTDOWN -> "剩余"
            else -> "已专注"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = m.secondaryLabel,
    )

    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            text = formatClock(display),
            // Tabular figures: a proportional font makes the digits jitter every second, which
            // reads as the layout twitching rather than time passing.
            fontFamily = FontFamily.Monospace,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = if (countdownDone) m.success else m.label,
        )
    }

    if (active.mode == FocusMode.COUNTDOWN && countdownDone) {
        Text(
            "已经记满 ${formatClock(active.targetMillis)}，超出的部分不会计入。",
            style = MaterialTheme.typography.labelLarge,
            color = m.secondaryLabel,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FocusChip(
            label = if (active.isPaused) "继续" else "暂停",
            icon = if (active.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            selected = false,
            onClick = { if (active.isPaused) onResume() else onPause() },
            modifier = Modifier.weight(1f),
        )
        FocusChip(
            label = "放弃",
            selected = false,
            destructive = true,
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        )
    }

    FocusPrimaryButton(label = "结束并记录", onClick = onFinish)
}

@Composable
private fun TaskChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val m = materials
    val shape = appShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) m.accent.copy(alpha = 0.16f) else m.chrome.copy(alpha = 0.40f))
            .styleBorder(shape, if (selected) m.accent else m.topEdge)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = m.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FocusChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    destructive: Boolean = false,
) {
    val m = materials
    val shape = appShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Feedback on press, not release — waiting for the lift reads as lag.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = Motion.Snappy,
        label = "focusChipPress",
    )
    val tint = when {
        destructive -> m.destructive
        selected -> onColor(m.accent)
        else -> m.label
    }

    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(
                when {
                    selected -> m.accent
                    destructive -> m.destructive.copy(alpha = 0.12f)
                    else -> m.chrome.copy(alpha = if (m.isNeoBrutalist) 1f else 0.40f)
                }
            )
            .styleBorder(shape, if (m.isNeoBrutalist) Color.Black else m.topEdge)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = tint)
    }
}

@Composable
private fun FocusPrimaryButton(label: String, onClick: () -> Unit) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = Motion.Snappy,
        label = "focusPrimaryPress",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(appShape(14.dp))
            .background(m.accent)
            .styleBorder(appShape(14.dp), if (m.isNeoBrutalist) Color.Black else m.topEdge)
            .pressableNoRipple(interactionSource, onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = onColor(m.accent))
    }
}
