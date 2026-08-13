package com.doapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.doapp.data.BackupFile
import com.doapp.data.Bucket
import com.doapp.data.Task
import com.doapp.notify.FocusAlarm
import com.doapp.notify.KeepAliveService
import com.doapp.notify.Reminders
import com.doapp.notify.TrashExpiry
import com.doapp.ui.AppPage
import com.doapp.ui.DockBar
import com.doapp.ui.FocusRecordsSheet
import com.doapp.ui.FocusScreen
import com.doapp.ui.FocusTimerSheet
import com.doapp.ui.HomeScreen
import com.doapp.ui.ReminderSettingsSheet
import com.doapp.ui.SettingsScreen
import com.doapp.ui.TaskEditorSheet
import com.doapp.ui.TrashSheet
import com.doapp.ui.WallpaperSheet
import com.doapp.ui.theme.DoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The wallpaper runs edge to edge; system bars float over it.
        enableEdgeToEdge()
        val app = application as DoApplication
        setContent {
            val appearance by app.appearance.appearance.collectAsState()
            DoTheme(appearance = appearance) { App(app) }
        }
    }
}

/** Which editor is open, and for what. Null means the sheet is closed. */
private data class EditorTarget(val task: Task?, val bucket: Bucket)

@Composable
private fun App(app: DoApplication) {
    val context = app.applicationContext
    val tasks by app.tasks.tasks.collectAsState()
    val appearance by app.appearance.appearance.collectAsState()

    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var wallpaperOpen by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var reminderSettingsOpen by remember { mutableStateOf(false) }
    var timerOpen by remember { mutableStateOf(false) }
    var recordsOpen by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(AppPage.DO) }

    val sessions by app.focus.sessions.collectAsState()
    val activeFocus by app.focus.active.collectAsState()
    // The bell for a countdown has to be an alarm: the run keeps going with the screen off.
    LaunchedEffect(activeFocus) { FocusAlarm.sync(context, activeFocus?.countdownEndsAt()) }

    val keepAlive by app.reminderSettings.keepAlive.collectAsState()
    // Applied from here rather than from Application.onCreate: starting a foreground service from
    // the background throws on Android 12+, and the process also starts for alarm broadcasts.
    LaunchedEffect(keepAlive) { KeepAliveService.apply(context, keepAlive) }

    LaunchedEffect(Unit) {
        while (true) {
            app.tasks.pruneExpiredTrash()
            // Pruning changes which task expires next, so the alarm has to move with it.
            TrashExpiry.sync(context, app.tasks.tasks.value)
            delay(60L * 60L * 1000L)
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (page) {
            AppPage.DO -> HomeScreen(
                tasks = tasks,
                appearance = appearance,
                onToggle = { task ->
                    app.tasks.setDone(task.id, !task.done)?.let { Reminders.sync(context, it) }
                },
                onOpenTask = { task -> editor = EditorTarget(task, task.bucket) },
                onCompose = { bucket -> editor = EditorTarget(null, bucket) },
            )

            AppPage.FOCUS -> FocusScreen(
                sessions = sessions,
                active = activeFocus,
                appearance = appearance,
                onOpenTimer = { timerOpen = true },
                onOpenRecords = { recordsOpen = true },
            )

            AppPage.SETTINGS -> SettingsScreen(
                appearance = appearance,
                trashedCount = tasks.count { it.isTrashed },
                onOpenWallpaper = { wallpaperOpen = true },
                onOpenTrash = { trashOpen = true },
                onOpenReminderSettings = { reminderSettingsOpen = true },
                onExport = { uri ->
                    BackupFile.write(context, uri, app.tasks.tasks.value, app.focus.sessions.value)
                },
                onImport = { uri ->
                    BackupFile.read(context, uri)?.let { backup ->
                        val added = app.tasks.merge(backup.tasks) + app.focus.merge(backup.sessions)
                        // Restored tasks bring their reminders back with them.
                        Reminders.syncAll(context, app.tasks.tasks.value)
                        added
                    }
                },
                onSetSelfReminder = app.appearance::setSelfReminder,
                onSelectStyle = app.appearance::selectStyle,
                onSelectFont = app.appearance::selectFont,
                onSelectTextColor = app.appearance::selectTextColor,
            )
        }

        DockBar(
            current = page,
            onSelect = { page = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    editor?.let { target ->
        TaskEditorSheet(
            initial = target.task,
            defaultBucket = target.bucket,
            onDismiss = { editor = null },
            onSave = { task ->
                if (tasks.any { it.id == task.id }) app.tasks.update(task) else app.tasks.add(task)
                Reminders.sync(context, task)
                editor = null
            },
            onDelete = { task ->
                val deleted = app.tasks.trash(task.id)
                Reminders.cancel(context, task.id)
                deleted?.let { TrashExpiry.sync(context, app.tasks.tasks.value) }
                editor = null
            },
        )
    }

    if (timerOpen) {
        FocusTimerSheet(
            active = activeFocus,
            tasks = tasks,
            onStart = { label, taskId, mode, target ->
                app.focus.start(label, taskId, mode, target)
            },
            onPause = app.focus::pause,
            onResume = app.focus::resume,
            onFinish = {
                app.focus.finish()
                timerOpen = false
            },
            onCancel = {
                app.focus.cancel()
                timerOpen = false
            },
            onDismiss = { timerOpen = false },
        )
    }

    if (recordsOpen) {
        FocusRecordsSheet(
            sessions = sessions,
            onDelete = { app.focus.delete(it.id) },
            onDismiss = { recordsOpen = false },
        )
    }

    if (reminderSettingsOpen) {
        ReminderSettingsSheet(
            keepAlive = keepAlive,
            onKeepAliveChange = app.reminderSettings::setKeepAlive,
            onDismiss = { reminderSettingsOpen = false },
        )
    }

    if (wallpaperOpen) {
        WallpaperSheet(
            appearance = appearance,
            onSelectPreset = app.appearance::selectPreset,
            onPickPhoto = { uri -> app.appearance.importPhoto(uri) },
            onBlurChange = app.appearance::setBlur,
            onDimChange = app.appearance::setDim,
            onDismiss = { wallpaperOpen = false },
        )
    }

    if (trashOpen) {
        TrashSheet(
            trashed = tasks.filter { it.isTrashed },
            onRestore = { task ->
                app.tasks.restore(task.id)?.let { Reminders.sync(context, it) }
                TrashExpiry.sync(context, app.tasks.tasks.value)
            },
            onPurge = { task ->
                app.tasks.purge(task.id)
                Reminders.cancel(context, task.id)
                TrashExpiry.sync(context, app.tasks.tasks.value)
            },
            onClearAll = {
                tasks.filter { it.isTrashed }.forEach { Reminders.cancel(context, it.id) }
                app.tasks.clearTrash()
                TrashExpiry.sync(context, app.tasks.tasks.value)
            },
            onDismiss = { trashOpen = false },
        )
    }
}
