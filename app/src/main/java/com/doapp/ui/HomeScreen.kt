package com.doapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doapp.data.Bucket
import com.doapp.data.Task
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** How much room the bottom dock needs, so content and the add button clear it. */
private val DockClearance = 88.dp

internal data class HomeTaskGroups(
    val todayOpen: List<Task>,
    val todayDone: List<Task>,
    val planOpen: List<Task>,
    val planDone: List<Task>,
)

/**
 * Classifies the task list once per data/day change. Keeping this outside composition also makes
 * the auto-promotion and ordering rules directly testable.
 */
internal fun groupHomeTasks(tasks: List<Task>, todayEpochDay: Long): HomeTaskGroups {
    val todayOpen = mutableListOf<Task>()
    val todayDone = mutableListOf<Task>()
    val planOpen = mutableListOf<Task>()
    val planDone = mutableListOf<Task>()

    tasks.forEach { task ->
        if (task.isTrashed) return@forEach
        val belongsToToday = task.bucket == Bucket.TODAY ||
            (task.planDay != null && task.planDay <= todayEpochDay)
        when {
            belongsToToday && task.done -> todayDone += task
            belongsToToday -> todayOpen += task
            task.done -> planDone += task
            else -> planOpen += task
        }
    }

    return HomeTaskGroups(
        todayOpen = todayOpen,
        todayDone = todayDone.sortedByDescending { it.completedAt ?: 0L },
        planOpen = planOpen.sortedBy { it.planDay ?: Long.MAX_VALUE },
        planDone = planDone.sortedByDescending { it.completedAt ?: 0L },
    )
}

@Composable
fun HomeScreen(
    tasks: List<Task>,
    selfReminder: String,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onCompose: (Bucket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val listState = rememberLazyListState()
    val reduceMotion = rememberReduceMotion()

    // Left to plain LocalDate.now() this only refreshes when something else recomposes, so an app
    // left open overnight would still be showing yesterday and holding back today's Plan tasks.
    var todayEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    LaunchedEffect(todayEpochDay) {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        delay(Duration.between(now, midnight).toMillis().coerceAtLeast(1_000L))
        todayEpochDay = LocalDate.now().toEpochDay()
    }
    val today = LocalDate.ofEpochDay(todayEpochDay)

    val grouped = remember(tasks, todayEpochDay) { groupHomeTasks(tasks, todayEpochDay) }
    val todayOpen = grouped.todayOpen
    val todayDone = grouped.todayDone
    val planOpen = grouped.planOpen
    val planDone = grouped.planDone

    // Group open Plan tasks by their planned day, so each day reads as its own section.
    val planGroups = remember(planOpen) { planOpen.groupBy { it.planDay } }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DockClearance + 80.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                Header(
                    remaining = todayOpen.size,
                    completed = todayDone.size,
                    selfReminder = selfReminder,
                    today = today,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            item(key = "today-header") {
                SectionHeader(
                    title = "Today",
                    count = todayOpen.size,
                    onClick = { onCompose(Bucket.TODAY) },
                )
            }

            if (todayOpen.isEmpty() && todayDone.isEmpty()) {
                item(key = "today-empty") { EmptyHint("今天还没有安排。点下面的新任务写一条。") }
            }

            items(todayOpen, key = { it.id }, contentType = { "task" }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { onToggle(task) },
                    onOpen = { onOpenTask(promoted(task, todayEpochDay)) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(if (reduceMotion) 80 else 140, easing = Motion.EaseOut),
                        placementSpec = tween(if (reduceMotion) 1 else 220, easing = Motion.EaseInOut),
                        fadeOutSpec = tween(if (reduceMotion) 60 else 90, easing = Motion.EaseOut),
                    ),
                )
            }

            // Completed work sits directly beneath today's list, dimmed — done, not gone.
            if (todayDone.isNotEmpty()) {
                item(key = "today-done-header") { CompletedDivider(todayDone.size) }
                items(todayDone, key = { it.id }, contentType = { "task" }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggle(task) },
                        onOpen = { onOpenTask(promoted(task, todayEpochDay)) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(if (reduceMotion) 80 else 140, easing = Motion.EaseOut),
                            placementSpec = tween(if (reduceMotion) 1 else 220, easing = Motion.EaseInOut),
                            fadeOutSpec = tween(if (reduceMotion) 60 else 90, easing = Motion.EaseOut),
                        ),
                    )
                }
            }

            item(key = "plan-header") {
                SectionHeader(
                    title = "Plan",
                    count = planOpen.size,
                    onClick = { onCompose(Bucket.LATER) },
                    modifier = Modifier.padding(top = 18.dp),
                )
            }

            if (planOpen.isEmpty() && planDone.isEmpty()) {
                item(key = "plan-empty") { EmptyHint("把还没到时候的事放这里，选个日期，到点自动进 Today。") }
            }

            planGroups.forEach { (day, dayTasks) ->
                item(key = "plan-day-${day ?: "none"}") {
                    PlanDayHeader(day?.let(::formatPlanDay) ?: "待定")
                }
                items(dayTasks, key = { it.id }, contentType = { "task" }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggle(task) },
                        onOpen = { onOpenTask(task) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(if (reduceMotion) 80 else 140, easing = Motion.EaseOut),
                            placementSpec = tween(if (reduceMotion) 1 else 220, easing = Motion.EaseInOut),
                            fadeOutSpec = tween(if (reduceMotion) 60 else 90, easing = Motion.EaseOut),
                        ),
                    )
                }
            }

            if (planDone.isNotEmpty()) {
                item(key = "plan-done-header") { CompletedDivider(planDone.size) }
                items(planDone, key = { it.id }, contentType = { "task" }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggle(task) },
                        onOpen = { onOpenTask(task) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(if (reduceMotion) 80 else 140, easing = Motion.EaseOut),
                            placementSpec = tween(if (reduceMotion) 1 else 220, easing = Motion.EaseInOut),
                            fadeOutSpec = tween(if (reduceMotion) 60 else 90, easing = Motion.EaseOut),
                        ),
                    )
                }
            }
        }

        AddButton(
            onClick = { onCompose(Bucket.TODAY) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + DockClearance + 6.dp
                ),
        )
    }
}

/** When a promoted Plan task is opened from the Today list, commit it as a Today task. */
private fun promoted(task: Task, todayEpochDay: Long): Task =
    if (task.bucket == Bucket.LATER && task.planDay != null && task.planDay <= todayEpochDay) {
        task.copy(bucket = Bucket.TODAY, planDay = null)
    } else task

@Composable
private fun Header(
    remaining: Int,
    completed: Int,
    selfReminder: String,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(26.dp)
    val total = remaining + completed
    val progress = if (total == 0) 0f else completed.toFloat() / total
    val reduceMotion = rememberReduceMotion()
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reduceMotion) Motion.Instant else Motion.Select,
        label = "todayProgress",
    )

    Column(
        modifier
            .fillMaxWidth()
            .premiumSurface(shape = shape, elevation = 12.dp)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "DAYDAYUP",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = m.accent,
                )
                Text(
                    text = formatToday(today),
                    style = MaterialTheme.typography.headlineSmall,
                    color = m.label,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(appShape(18.dp))
                    .background(m.accent.copy(alpha = 0.12f))
                    .border(1.dp, m.accent.copy(alpha = 0.18f), appShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (remaining == 0 && total > 0) "✓" else remaining.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = m.accent,
                )
            }
        }

        Text(
            text = when {
                total == 0 -> "今天想完成什么？"
                remaining == 0 -> "今日任务全部完成"
                else -> "还有 $remaining 件，稳稳推进"
            },
            style = MaterialTheme.typography.titleMedium,
            color = m.label,
        )

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("今日进度", style = MaterialTheme.typography.labelLarge, color = m.secondaryLabel)
                Spacer(Modifier.weight(1f))
                Text(
                    if (total == 0) "0 / 0" else "$completed / $total",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = m.label,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(appShape(4.dp))
                    .background(m.hairline)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            scaleX = animatedProgress.value
                            alpha = if (animatedProgress.value == 0f) 0f else 1f
                        }
                        .background(m.accent)
                )
            }
        }

        if (selfReminder.isNotBlank()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(appShape(14.dp))
                    .background(m.accent.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Text(
                    text = selfReminder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = m.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A section header that doubles as an entry into composing a task for that bucket — the same
 * frosted-pill language and scale as the floating add button, so it reads as another way in.
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(26.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = Motion.Press,
        label = "sectionPress",
    )

    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = m.label)
        if (count > 0) {
            Box(
                Modifier
                    .padding(start = 9.dp)
                    .clip(appShape(9.dp))
                    .background(m.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = m.accent,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .size(42.dp)
                .clip(appShape(14.dp))
                .background(m.accent)
                .styleBorder(
                    appShape(14.dp),
                    m.accent,
                    width = 1.dp,
                )
                .pressableNoRipple(interactionSource, onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "添加到 $title",
                tint = onColor(m.accent),
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun PlanDayHeader(label: String, modifier: Modifier = Modifier) {
    val m = materials
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = m.secondaryLabel,
        modifier = modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun CompletedDivider(count: Int, modifier: Modifier = Modifier) {
    val m = materials
    Row(
        modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "已完成 $count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = m.tertiaryLabel,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(m.hairline)
        )
    }
}

@Composable
private fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    val m = materials
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = m.secondaryLabel,
        modifier = modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppPrimaryButton(
        label = "新任务",
        icon = Icons.Rounded.Add,
        onClick = onClick,
        modifier = modifier.padding(horizontal = 28.dp),
    )
}
