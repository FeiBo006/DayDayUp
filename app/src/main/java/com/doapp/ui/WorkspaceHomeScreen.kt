package com.doapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doapp.data.Bucket
import com.doapp.data.RepeatRule
import com.doapp.data.Task
import java.time.LocalDate

@Composable
fun WorkspaceHomeScreen(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onCompose: (Bucket) -> Unit,
    onMoveToday: (Task, Int) -> Unit,
    onStartFocus: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = rememberWorkspaceToday()
    val groups = remember(tasks, today) { groupHomeTasks(tasks, today.toEpochDay()) }
    val openCount = groups.todayOpen.size
    val doneCount = groups.todayDone.size
    val total = openCount + doneCount
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else doneCount.toFloat() / total,
        animationSpec = Motion.Select,
        label = "workspaceTodayProgress",
    )

    WorkspacePage(modifier) { layout ->
        WorkspaceHeader(
            eyebrow = "DayDayUp / Do",
            title = "今天",
            subtitle = formatToday(today),
            action = {
                WorkspaceActionButton(
                    label = "新任务",
                    icon = Icons.Rounded.Add,
                    onClick = { onCompose(Bucket.TODAY) },
                )
            },
        )

        WorkspacePanel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("今日进度", color = WorkspaceColors.Secondary, style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = if (total == 0) "还没有安排" else "$doneCount / $total 已完成",
                            color = WorkspaceColors.Ink,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = WorkspaceColors.Secondary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E4E8)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .graphicsLayer {
                                scaleX = progress
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                            .clip(CircleShape)
                            .background(WorkspaceColors.Do),
                    )
                }
        }

        if (layout.usesWideColumns) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.weight(1f)) {
                    TodaySection(groups, today, openCount, onCompose, onToggle, onOpenTask, onMoveToday, onStartFocus)
                }
                Box(Modifier.weight(1f)) {
                    PlanSection(groups, today, onCompose, onToggle, onOpenTask, onStartFocus)
                }
            }
        } else {
            TodaySection(groups, today, openCount, onCompose, onToggle, onOpenTask, onMoveToday, onStartFocus)
            PlanSection(groups, today, onCompose, onToggle, onOpenTask, onStartFocus)
        }
    }
}

@Composable
private fun TodaySection(
    groups: HomeTaskGroups,
    today: LocalDate,
    openCount: Int,
    onCompose: (Bucket) -> Unit,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onMoveToday: (Task, Int) -> Unit,
    onStartFocus: (Task) -> Unit,
) = TaskSection(
    title = "Today",
    caption = if (openCount == 0) "现在没有待完成任务" else "$openCount 条待完成",
    tasks = groups.todayOpen + groups.todayDone,
    todayEpochDay = today.toEpochDay(),
    onAdd = { onCompose(Bucket.TODAY) },
    onToggle = onToggle,
    onOpenTask = onOpenTask,
    onMove = onMoveToday,
    onStartFocus = onStartFocus,
)

@Composable
private fun PlanSection(
    groups: HomeTaskGroups,
    today: LocalDate,
    onCompose: (Bucket) -> Unit,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onStartFocus: (Task) -> Unit,
) = TaskSection(
    title = "Plan",
    caption = if (groups.planOpen.isEmpty()) "以后要做的事放在这里" else "${groups.planOpen.size} 条计划",
    tasks = groups.planOpen + groups.planDone,
    todayEpochDay = today.toEpochDay(),
    onAdd = { onCompose(Bucket.LATER) },
    onToggle = onToggle,
    onOpenTask = onOpenTask,
    onMove = null,
    onStartFocus = onStartFocus,
)

@Composable
private fun TaskSection(
    title: String,
    caption: String,
    tasks: List<Task>,
    todayEpochDay: Long,
    onAdd: () -> Unit,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onMove: ((Task, Int) -> Unit)?,
    onStartFocus: (Task) -> Unit,
) {
    WorkspacePanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = WorkspaceColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(caption, color = WorkspaceColors.Secondary, style = MaterialTheme.typography.bodySmall)
            }
            MiniAddButton(onAdd)
        }
        if (tasks.isEmpty()) {
            Text(
                text = "空空的，挺好。",
                color = WorkspaceColors.Tertiary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
            )
        } else {
            Spacer(Modifier.height(10.dp))
            val movableLastIndex = tasks.indexOfLast { !it.done }
            tasks.forEachIndexed { index, task ->
                if (index > 0) WorkspaceDivider()
                val shownTask = promotedWorkspaceTask(task, todayEpochDay)
                key(task.id) {
                    WorkspaceTaskRow(
                        task = task,
                        canMoveUp = onMove != null && !task.done && index > 0,
                        canMoveDown = onMove != null && !task.done && index < movableLastIndex,
                        onToggle = { onToggle(shownTask) },
                        onClick = { onOpenTask(shownTask) },
                        onMove = { direction -> onMove?.invoke(task, direction) },
                        onStartFocus = { onStartFocus(shownTask) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceTaskRow(
    task: Task,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onStartFocus: () -> Unit,
) {
    val rowInteraction = remember { MutableInteractionSource() }
    val rowPressed by rowInteraction.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (rowPressed) 0.99f else 1f,
        animationSpec = Motion.Press,
        label = "workspaceTaskPress",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
            .pressableNoRipple(rowInteraction, onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val toggleInteraction = remember { MutableInteractionSource() }
        val togglePressed by toggleInteraction.collectIsPressedAsState()
        val toggleScale by animateFloatAsState(
            targetValue = if (togglePressed) 0.94f else 1f,
            animationSpec = Motion.Press,
            label = "workspaceTaskTogglePress",
        )
        Box(
            Modifier
                .size(44.dp)
                .pressableNoRipple(toggleInteraction, onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .graphicsLayer { scaleX = toggleScale; scaleY = toggleScale }
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (task.done) WorkspaceColors.Ink else Color.Transparent)
                    .border(1.5.dp, if (task.done) WorkspaceColors.Ink else WorkspaceColors.Tertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (task.done) {
                    Icon(Icons.Rounded.Check, contentDescription = "完成", tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = task.title,
                color = if (task.done) WorkspaceColors.Tertiary else WorkspaceColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                if (task.repeatRule != RepeatRule.NONE) add(task.repeatRule.workspaceLabel)
                task.planDay?.let { add(formatPlanDay(it)) }
                task.reminderAt?.let { add(formatReminder(it)) }
                if (task.note.isNotBlank()) add(task.note)
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    color = WorkspaceColors.Secondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (!task.done) {
            Row(
                modifier = Modifier.padding(start = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canMoveUp || canMoveDown) {
                    DragReorderHandle(
                        canMoveUp = canMoveUp,
                        canMoveDown = canMoveDown,
                        onMove = onMove,
                    )
                }
                IconTileButton(
                    icon = Icons.Rounded.PlayArrow,
                    contentDescription = "开始专注",
                    enabled = true,
                    emphasized = true,
                    onClick = onStartFocus,
                )
            }
        }
    }
}

@Composable
private fun DragReorderHandle(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(canMoveUp, canMoveDown) {
                detectVerticalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onDragEnd = { dragDistance = 0f },
                    onDragCancel = { dragDistance = 0f },
                ) { change, amount ->
                    change.consume()
                    dragDistance += amount
                    when {
                        dragDistance <= -threshold && canMoveUp -> {
                            onMove(-1)
                            dragDistance = 0f
                        }
                        dragDistance >= threshold && canMoveDown -> {
                            onMove(1)
                            dragDistance = 0f
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "拖动排序",
            tint = WorkspaceColors.Tertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun IconTileButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.94f else 1f,
        animationSpec = Motion.Press,
        label = "workspaceTaskActionPress",
    )
    val background = when {
        !enabled -> Color.Transparent
        emphasized -> WorkspaceColors.Do.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(if (enabled) Modifier.pressableNoRipple(interaction, onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> WorkspaceColors.Tertiary.copy(alpha = 0.40f)
                emphasized -> WorkspaceColors.Do
                else -> WorkspaceColors.Secondary
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MiniAddButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = Motion.Press,
        label = "workspaceMiniAddPress",
    )
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White)
            .border(1.dp, WorkspaceColors.Line, RoundedCornerShape(9.dp))
            .pressableNoRipple(interaction, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "添加", tint = WorkspaceColors.Ink, modifier = Modifier.size(20.dp))
    }
}

private fun promotedWorkspaceTask(task: Task, todayEpochDay: Long): Task =
    if (task.bucket == Bucket.LATER && task.planDay != null && task.planDay <= todayEpochDay) {
        task.copy(bucket = Bucket.TODAY, planDay = null)
    } else {
        task
    }

private val RepeatRule.workspaceLabel: String
    get() = when (this) {
        RepeatRule.NONE -> ""
        RepeatRule.DAILY -> "每天"
        RepeatRule.WEEKLY -> "每周"
        RepeatRule.MONTHLY -> "每月"
    }
