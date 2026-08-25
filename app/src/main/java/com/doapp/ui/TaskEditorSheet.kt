package com.doapp.ui

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.doapp.data.Bucket
import com.doapp.data.Task
import com.doapp.notify.BackgroundAccess
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
    draftKey: Int,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit,
) {
    val m = materials
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The session key is stable across configuration changes and changes for every fresh open.
    // That preserves an in-progress draft without leaking it into the next task or new-task sheet.
    var title by rememberSaveable(draftKey) { mutableStateOf(initial?.title.orEmpty()) }
    var note by rememberSaveable(draftKey) { mutableStateOf(initial?.note.orEmpty()) }
    var bucket by rememberSaveable(draftKey) {
        mutableStateOf(initial?.bucket ?: defaultBucket)
    }

    val initialReminder = initial?.reminderAt?.toLocalDateTime()
    val defaultReminder = remember(draftKey) { defaultReminderAt() }
    val initialReminderDate = initialReminder?.toLocalDate() ?: defaultReminder.toLocalDate()
    val initialReminderTime = initialReminder?.toLocalTime() ?: defaultReminder.toLocalTime()
    var reminderOn by rememberSaveable(draftKey) {
        mutableStateOf(initial?.reminderAt != null)
    }
    var reminderDateEpochDay by rememberSaveable(draftKey) {
        mutableLongStateOf(initialReminderDate.toEpochDay())
    }
    var reminderMinuteOfDay by rememberSaveable(draftKey) {
        mutableIntStateOf(initialReminderTime.hour * 60 + initialReminderTime.minute)
    }
    var showDatePicker by rememberSaveable(draftKey) { mutableStateOf(false) }
    var showTimePicker by rememberSaveable(draftKey) { mutableStateOf(false) }
    val reminderDate = LocalDate.ofEpochDay(reminderDateEpochDay)
    val reminderTime = LocalTime.of(reminderMinuteOfDay / 60, reminderMinuteOfDay % 60)
    // A reminder in the past is dropped by Reminders.sync, so say so rather than showing a bell
    // on the row that will never ring.
    val reminderAt = LocalDateTime.of(reminderDate, reminderTime)
    val reminderIsPast = !reminderAt.isAfter(LocalDateTime.now())

    // The planned day only exists for Plan tasks; Today has no date picker on purpose.
    var planDay by rememberSaveable(draftKey) {
        mutableLongStateOf(initial?.planDay ?: LocalDate.now().plusDays(1).toEpochDay())
    }
    var showPlanDatePicker by rememberSaveable(draftKey) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // Set when a permission request came back empty-handed. On a permanently denied permission
    // the system shows no dialog at all, so without this the switch just snaps back with no
    // explanation and no way forward.
    var notificationsDenied by rememberSaveable(draftKey) { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        reminderOn = granted
        notificationsDenied = !granted
    }

    // These live in system settings, and the user goes there and comes back. Re-read them on
    // every resume or the hints stay on screen after they've been dealt with.
    val resumes = rememberResumeCount()
    val notificationsAllowed = remember(resumes, notificationsDenied) {
        BackgroundAccess.notificationsAllowed(context)
    }
    val exactAllowed = remember(resumes) { BackgroundAccess.exactAlarmsAllowed(context) }
    val backgroundAllowed = remember(resumes) { BackgroundAccess.batteryUnrestricted(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = 640.dp,
        // A solid surface: translucent-on-translucent would destroy legibility.
        containerColor = m.card,
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier
                .imePadding()
                // With the keyboard up, or with all the permission hints showing, the save button
                // would otherwise be pushed off the bottom of the sheet.
                .verticalScroll(rememberScrollState())
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
                                ValueChip(formatDate(reminderAt)) { showDatePicker = true }
                                ValueChip(formatTime(reminderAt)) { showTimePicker = true }
                            }
                            if (reminderIsPast) {
                                PastReminderHint(
                                    modifier = Modifier.padding(
                                        start = 14.dp, end = 14.dp, bottom = 12.dp
                                    ),
                                )
                            }
                            if (!exactAllowed) {
                                ExactAlarmHint(
                                    onOpenSettings = { BackgroundAccess.openExactAlarmSettings(context) },
                                    modifier = Modifier.padding(
                                        start = 14.dp, end = 14.dp, bottom = 12.dp
                                    ),
                                )
                            }
                            if (!backgroundAllowed) {
                                BackgroundReminderHint(
                                    onOpenSettings = { BackgroundAccess.openBatterySettings(context) },
                                    modifier = Modifier.padding(
                                        start = 14.dp, end = 14.dp, bottom = 12.dp
                                    ),
                                )
                            }
                        }
                    }

                    // Deliberately outside the switch's AnimatedVisibility: when the permission
                    // is denied the switch can't stay on, so a hint nested in there could never
                    // be seen — which left the user with no way to ever enable reminders again.
                    if (!notificationsAllowed && (reminderOn || notificationsDenied)) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(m.hairline))
                            NotificationPermissionHint(
                                onOpenSettings = { BackgroundAccess.openNotificationSettings(context) },
                                modifier = Modifier.padding(14.dp),
                            )
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
                    // Each tap on a new task mints a fresh UUID, so a double tap before the sheet
                    // closes would add the same task twice.
                    enabled = title.isNotBlank() && !saving,
                    onClick = {
                        saving = true
                        val at = if (reminderOn) reminderAt.toEpochMillis() else null
                        val base = initial ?: Task(title = "", createdAt = System.currentTimeMillis())
                        onSave(
                            base.copy(
                                title = title.trim(),
                                note = note.trim(),
                                bucket = bucket,
                                reminderAt = at,
                                // Moving the reminder makes it a new one, so let it fire again
                                // rather than staying retired by the last delivery.
                                notifiedAt = if (at == base.reminderAt) base.notifiedAt else null,
                                planDay = if (bucket == Bucket.LATER) planDay else null,
                            )
                        )
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
                        reminderDateEpochDay =
                            LocalDateTime.ofEpochSecond(it / 1000, 0, ZoneOffset.UTC)
                                .toLocalDate().toEpochDay()
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
                    reminderMinuteOfDay = state.hour * 60 + state.minute
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

/**
 * Next round hour, never in the past — a reminder you've already missed is noise. This has to
 * carry the date too: LocalTime.plusHours wraps around midnight, so at 23:45 a time-only default
 * lands on 00:00 *today*, which is nineteen hours in the past.
 */
private fun defaultReminderAt(): LocalDateTime =
    LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)

@Composable
private fun PastReminderHint(modifier: Modifier = Modifier) {
    val m = materials
    Text(
        "这个时间已经过去了，保存后不会提醒。",
        style = MaterialTheme.typography.labelLarge,
        color = m.destructive,
        modifier = modifier,
    )
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
    val reduceMotion = rememberReduceMotion()
    val options = listOf(Bucket.TODAY to "Today", Bucket.LATER to "Plan")
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    var trackWidth by remember { mutableFloatStateOf(0f) }
    val fraction = animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = if (reduceMotion) Motion.Instant else Motion.Select,
        label = "segmentIndicator",
    )
    val density = LocalDensity.current

    Box(
        Modifier
            .fillMaxWidth()
            .clip(appShape(11.dp))
            .background(if (m.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            .styleBorder(appShape(11.dp), m.hairline)
            .padding(2.dp)
            .onGloballyPositioned { trackWidth = it.size.width.toFloat() }
    ) {
        val segmentWidth = with(density) { (trackWidth / options.size).toDp() }
        Box(
            Modifier
                .width(segmentWidth)
                .height(34.dp)
                .graphicsLayer {
                    translationX = (trackWidth / options.size) * fraction.value
                }
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
            .styleBorder(appShape(9.dp), m.hairline)
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
            .styleBorder(appShape(14.dp), m.hairline)
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
    AppPrimaryButton(
        label = label,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.width(112.dp),
    )
}

@Composable
private fun FieldSurface(padded: Boolean = true, content: @Composable () -> Unit) {
    val m = materials
    val shape = appShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (m.isDark) Color.White.copy(alpha = 0.07f) else Color.White)
            .styleBorder(shape, m.hairline)
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
