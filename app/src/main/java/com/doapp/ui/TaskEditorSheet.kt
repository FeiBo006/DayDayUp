package com.doapp.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.doapp.data.Bucket
import com.doapp.data.Task
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Create or edit one task. Opens as a modal sheet: the list dims behind it, because this is a
 * focused, blocking task rather than something you do alongside the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    initial: Task?,
    defaultBucket: Bucket,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit,
) {
    val m = materials
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var bucket by remember { mutableStateOf(initial?.bucket ?: defaultBucket) }

    val initialReminder = initial?.reminderAt?.toLocalDateTime()
    var reminderOn by remember { mutableStateOf(initial?.reminderAt != null) }
    var reminderDate by remember {
        mutableStateOf(initialReminder?.toLocalDate() ?: LocalDate.now())
    }
    var reminderTime by remember {
        mutableStateOf(initialReminder?.toLocalTime() ?: defaultReminderTime())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // The planned day only exists for Plan tasks; Today has no date picker on purpose.
    var planDay by remember {
        mutableStateOf(initial?.planDay ?: LocalDate.now().plusDays(1).toEpochDay())
    }
    var showPlanDatePicker by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> reminderOn = granted }
    val notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    val backgroundAllowed = isIgnoringBatteryOptimizations(context)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // A solid surface: translucent-on-translucent would destroy legibility.
        containerColor = if (m.isNeoBrutalist || m.isDoodle) m.card
        else if (m.isDark) Color(0xFF1C1C1E) else Color(0xFFF7F7F9),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (initial == null) "新任务" else "编辑",
                style = MaterialTheme.typography.headlineSmall,
                color = m.label,
            )

            FieldSurface {
                PlainTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "想做点什么？",
                    style = MaterialTheme.typography.bodyLarge,
                    imeAction = ImeAction.Next,
                )
            }

            FieldSurface {
                PlainTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "备注（可选）",
                    style = MaterialTheme.typography.bodyMedium,
                    imeAction = ImeAction.Done,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SegmentedBuckets(
                    selected = bucket,
                    onSelect = { bucket = it },
                )

                AnimatedVisibility(
                    visible = bucket == Bucket.LATER,
                    enter = fadeIn(animationSpec = tween(160)) +
                        expandVertically(
                            animationSpec = spring(dampingRatio = 1f, stiffness = 439f),
                            expandFrom = Alignment.Top,
                        ),
                    exit = fadeOut(animationSpec = tween(100)) +
                        shrinkVertically(
                            animationSpec = spring(dampingRatio = 1f, stiffness = 439f),
                            shrinkTowards = Alignment.Top,
                        ),
                ) {
                    FieldSurface {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Event,
                                contentDescription = null,
                                tint = m.accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "计划日期",
                                style = MaterialTheme.typography.bodyLarge,
                                color = m.label,
                                modifier = Modifier.weight(1f),
                            )
                            ValueChip(formatPlanDay(planDay)) { showPlanDatePicker = true }
                        }
                    }
                }
            }

            FieldSurface(padded = false) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = m.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "提醒",
                            style = MaterialTheme.typography.bodyLarge,
                            color = m.label,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = reminderOn,
                            onCheckedChange = { want ->
                                if (want && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // Ask at the moment the permission is actually needed.
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    reminderOn = want
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = m.success),
                        )
                    }

                    AnimatedVisibility(
                        visible = reminderOn,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(m.hairline))
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ValueChip(formatDate(LocalDateTime.of(reminderDate, reminderTime))) {
                                    showDatePicker = true
                                }
                                ValueChip(formatTime(LocalDateTime.of(reminderDate, reminderTime))) {
                                    showTimePicker = true
                                }
                            }
                            if (!canScheduleExact(context)) {
                                ExactAlarmHint(
                                    onOpenSettings = { openExactAlarmSettings(context) },
                                    modifier = Modifier.padding(
                                        start = 14.dp, end = 14.dp, bottom = 12.dp
                                    ),
                                )
                            }
                            if (!notificationsAllowed) {
                                NotificationPermissionHint(
                                    onOpenSettings = { openNotificationSettings(context) },
                                    modifier = Modifier.padding(
                                        start = 14.dp, end = 14.dp, bottom = 12.dp
                                    ),
                                )
                            }
                            if (!backgroundAllowed) {
                                BackgroundReminderHint(
                                    onOpenSettings = { openBatteryOptimizationSettings(context) },
                                    modifier = Modifier.padding(
                                        start = 14.dp, end = 14.dp, bottom = 12.dp
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (initial != null) {
                    DestructiveButton(onClick = { onDelete(initial) })
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    label = if (initial == null) "添加" else "保存",
                    enabled = title.isNotBlank(),
                    onClick = {
                        val at = if (reminderOn) {
                            LocalDateTime.of(reminderDate, reminderTime).toEpochMillis()
                        } else null
                        val task = (initial ?: Task(title = "", createdAt = System.currentTimeMillis()))
                            .copy(
                                title = title.trim(),
                                note = note.trim(),
                                bucket = bucket,
                                reminderAt = at,
                                planDay = if (bucket == Bucket.LATER) planDay else null,
                            )
                        onSave(task)
                    },
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = reminderDate.atStartOfDay()
                .toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        reminderDate = LocalDateTime.ofEpochSecond(it / 1000, 0, ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("好") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = reminderTime.hour,
            initialMinute = reminderTime.minute,
            is24Hour = true,
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = LocalTime.of(state.hour, state.minute)
                    showTimePicker = false
                }) { Text("好") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        ) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        }
    }

    if (showPlanDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(planDay).atStartOfDay()
                .toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPlanDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        planDay = LocalDateTime.ofEpochSecond(it / 1000, 0, ZoneOffset.UTC)
                            .toLocalDate().toEpochDay()
                    }
                    showPlanDatePicker = false
                }) { Text("好") }
            },
            dismissButton = {
                TextButton(onClick = { showPlanDatePicker = false }) { Text("取消") }
            },
        ) { DatePicker(state = state) }
    }
}

/** Next round-ish hour, but never in the past — a reminder you've already missed is noise. */
private fun defaultReminderTime(): LocalTime {
    val next = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    return if (next < LocalTime.now()) LocalTime.of(23, 30) else next
}

private fun canScheduleExact(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return manager.canScheduleExactAlarms()
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(android.net.Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    return runCatching {
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName)
            ?: true
    }.getOrDefault(true)
}

private fun openBatteryOptimizationSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(android.net.Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun ExactAlarmHint(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier) {
        Text(
            "系统未允许精确闹钟，提醒可能会晚几分钟。",
            style = MaterialTheme.typography.labelLarge,
            color = m.secondaryLabel,
        )
        Text(
            "去开启",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = m.accent,
            modifier = Modifier
                .padding(top = 2.dp)
                .pressableNoRipple(interactionSource, onOpenSettings),
        )
    }
}

@Composable
private fun NotificationPermissionHint(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier) {
        Text(
            "通知权限未开启，到时间后无法显示提醒",
            style = MaterialTheme.typography.labelLarge,
            color = m.secondaryLabel,
        )
        Text(
            "去开启",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = m.accent,
            modifier = Modifier
                .padding(top = 2.dp)
                .pressableNoRipple(interactionSource, onOpenSettings),
        )
    }
}

@Composable
private fun BackgroundReminderHint(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier) {
        Text(
            "手机省电策略可能限制后台提醒",
            style = MaterialTheme.typography.labelLarge,
            color = m.secondaryLabel,
        )
        Text(
            "允许后台运行",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = m.accent,
            modifier = Modifier
                .padding(top = 2.dp)
                .pressableNoRipple(interactionSource, onOpenSettings),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetHandle() {
    val m = materials
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(36.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(m.tertiaryLabel)
        )
    }
}

/** Two horizons, one indicator that slides between them with the standard reposition spring. */
@Composable
private fun SegmentedBuckets(selected: Bucket, onSelect: (Bucket) -> Unit) {
    val m = materials
    val options = listOf(Bucket.TODAY to "Today", Bucket.LATER to "Plan")
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    var trackWidth by remember { mutableStateOf(0f) }
    val fraction by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = Motion.Move,
        label = "segmentIndicator",
    )
    val density = LocalDensity.current

    Box(
        Modifier
            .fillMaxWidth()
            .clip(appShape(11.dp))
            .background(if (m.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            .then(if (m.isDoodle) Modifier.styleBorder(appShape(11.dp), m.topEdge) else Modifier)
            .padding(2.dp)
            .onGloballyPositioned { trackWidth = it.size.width.toFloat() }
    ) {
        val segmentWidth = with(density) { (trackWidth / options.size).toDp() }
        Box(
            Modifier
                .padding(start = segmentWidth * fraction)
                .width(segmentWidth)
                .height(34.dp)
                .clip(appShape(9.dp))
                .background(if (m.isDark) Color(0xFF48484A) else Color.White)
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (value, label) ->
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .weight(1f)
                        .height(34.dp)
                        .pressableNoRipple(interactionSource) { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (i == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (i == index) m.label else m.secondaryLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueChip(label: String, onClick: () -> Unit) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .clip(appShape(9.dp))
            .background(if (m.isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f))
            .then(if (m.isDoodle) Modifier.styleBorder(appShape(9.dp), m.topEdge) else Modifier)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = m.label)
    }
}

@Composable
private fun DestructiveButton(onClick: () -> Unit) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .clip(appShape(14.dp))
            .background(m.destructive.copy(alpha = 0.12f))
            .then(if (m.isDoodle) Modifier.styleBorder(appShape(14.dp), m.topEdge) else Modifier)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.DeleteOutline,
            contentDescription = null,
            tint = m.destructive,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("删除", style = MaterialTheme.typography.titleMedium, color = m.destructive)
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.35f,
        animationSpec = Motion.Snappy,
        label = "primaryEnabled",
    )
    Box(
        Modifier
            .clip(appShape(14.dp))
            .background(m.accent.copy(alpha = alpha))
            .then(if (m.isDoodle) Modifier.styleBorder(appShape(14.dp), m.topEdge) else Modifier)
            .pressableNoRipple(interactionSource) { if (enabled) onClick() }
            .padding(horizontal = 26.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun FieldSurface(padded: Boolean = true, content: @Composable () -> Unit) {
    val m = materials
    val shape = appShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (m.isDoodle) m.card else if (m.isDark) Color.White.copy(alpha = 0.07f) else Color.White)
            .styleBorder(shape, m.topEdge)
            .then(if (padded) Modifier.padding(horizontal = 14.dp, vertical = 12.dp) else Modifier)
    ) { content() }
}

@Composable
private fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: androidx.compose.ui.text.TextStyle,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    val m = materials
    Box(modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(placeholder, style = style, color = m.tertiaryLabel)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style.copy(color = m.label),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(m.accent),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
