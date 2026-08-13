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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doapp.data.Appearance
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

@Composable
fun HomeScreen(
    tasks: List<Task>,
    appearance: Appearance,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onCompose: (Bucket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val listState = rememberLazyListState()

    // Left to plain LocalDate.now() this only refreshes when something else recomposes, so an app
    // left open overnight would still be showing yesterday and holding back today's Plan tasks.
    var todayEpochDay by remember { mutableStateOf(LocalDate.now().toEpochDay()) }
    LaunchedEffect(todayEpochDay) {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        delay(Duration.between(now, midnight).toMillis().coerceAtLeast(1_000L))
        todayEpochDay = LocalDate.now().toEpochDay()
    }
    val today = LocalDate.ofEpochDay(todayEpochDay)

    // A Plan task whose planned day has arrived belongs to Today — the auto-promote.
    fun effectiveBucket(task: Task): Bucket =
        if (task.bucket == Bucket.LATER && task.planDay != null && task.planDay <= todayEpochDay) Bucket.TODAY
        else task.bucket

    val active = tasks.filterNot { it.isTrashed }
    val todayOpen = active.filter { !it.done && effectiveBucket(it) == Bucket.TODAY }
    val todayDone = active.filter { it.done && effectiveBucket(it) == Bucket.TODAY }
        .sortedByDescending { it.completedAt ?: 0L }
    val planOpen = active.filter { !it.done && effectiveBucket(it) == Bucket.LATER }
        .sortedBy { it.planDay ?: Long.MAX_VALUE }
    val planDone = active.filter { it.done && effectiveBucket(it) == Bucket.LATER }
        .sortedByDescending { it.completedAt ?: 0L }

    // Group open Plan tasks by their planned day, so each day reads as its own section.
    val planGroups = planOpen.groupBy { it.planDay }

    Box(modifier.fillMaxSize()) {
        WallpaperBackground(appearance)

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
                    selfReminder = appearance.selfReminder,
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

            items(todayOpen, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { onToggle(task) },
                    onOpen = { onOpenTask(promoted(task, todayEpochDay)) },
                    modifier = Modifier.animateItem(),
                )
            }

            // Completed work sits directly beneath today's list, dimmed — done, not gone.
            if (todayDone.isNotEmpty()) {
                item(key = "today-done-header") { CompletedDivider(todayDone.size) }
                items(todayDone, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggle(task) },
                        onOpen = { onOpenTask(promoted(task, todayEpochDay)) },
                        modifier = Modifier.animateItem(),
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
                items(dayTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggle(task) },
                        onOpen = { onOpenTask(task) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (planDone.isNotEmpty()) {
                item(key = "plan-done-header") { CompletedDivider(planDone.size) }
                items(planDone, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggle(task) },
                        onOpen = { onOpenTask(task) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        // Floating chrome. Content scrolls underneath it rather than being cut off by a bar.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .then(
                    if (m.isNeoBrutalist) Modifier.background(m.chrome)
                    else Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                if (m.isDark) Color.Black.copy(alpha = 0.45f)
                                else Color.White.copy(alpha = 0.55f),
                            )
                        )
                    )
                )
        )

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
    selfReminder: String,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val m = materials
    Column(modifier.fillMaxWidth()) {
        Text(
            text = "DayDayUp",
            style = MaterialTheme.typography.displaySmall,
            color = m.label,
        )
        Text(
            text = if (remaining == 0) "${formatToday(today)} · 今天清空了"
            else "${formatToday(today)} · 还有 $remaining 件",
            style = MaterialTheme.typography.bodyMedium,
            color = m.secondaryLabel,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (selfReminder.isNotBlank()) {
            Text(
                text = selfReminder,
                style = MaterialTheme.typography.bodyMedium,
                color = m.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp, end = 18.dp),
            )
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
        animationSpec = Motion.Snappy,
        label = "sectionPress",
    )

    Row(
        modifier.fillMaxWidth().padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .styleShadow(
                    shape = shape,
                    elevation = if (m.isNeoBrutalist) 8.dp else 14.dp,
                    spotColor = if (m.isNeoBrutalist) Color.Black else Color.Black.copy(alpha = 0.4f),
                )
                .clip(shape)
                .background(m.chrome)
                .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
                .pressableNoRipple(interactionSource, onClick)
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = m.label)
            AnimatedVisibility(
                visible = count > 0,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180)),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = m.tertiaryLabel,
                )
            }
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
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Feedback lands on press, not on release — waiting for the lift reads as lag.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = Motion.Snappy,
        label = "addPress",
    )

    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .styleShadow(
                shape = appShape(26.dp),
                elevation = if (m.isNeoBrutalist) 8.dp else 14.dp,
                spotColor = if (m.isNeoBrutalist) Color.Black else Color.Black.copy(alpha = 0.4f),
            )
            .clip(appShape(26.dp))
            .background(m.accent)
            .then(
                when {
                    m.isDoodle -> Modifier.doodleBorder()
                    m.isNeoBrutalist -> Modifier.border(3.dp, Color.Black, appShape(26.dp))
                    else -> Modifier
                }
            )
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            "新任务",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}
