package com.doapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doapp.data.Appearance
import com.doapp.data.BackupFile
import com.doapp.data.Bucket
import com.doapp.notify.FocusAlarm
import com.doapp.notify.KeepAliveService
import com.doapp.notify.Reminders
import com.doapp.notify.TrashExpiry
import com.doapp.ui.AppPage
import com.doapp.ui.AppEntryAnimation
import com.doapp.ui.AppOpenTransition
import com.doapp.ui.DockBar
import com.doapp.ui.FocusRecordsSheet
import com.doapp.ui.FocusTimerSheet
import com.doapp.ui.LauncherScreen
import com.doapp.ui.ReminderSettingsSheet
import com.doapp.ui.TaskEditorSheet
import com.doapp.ui.TrashSheet
import com.doapp.ui.WallpaperSheet
import com.doapp.ui.WorkspaceFocusScreen
import com.doapp.ui.WorkspaceHomeScreen
import com.doapp.ui.WorkspaceSettingsScreen
import com.doapp.ui.theme.DoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The wallpaper runs edge to edge; system bars float over it.
        enableEdgeToEdge()
        val app = application as DoApplication
        val showEntry = !app.entryShownInProcess
        app.entryShownInProcess = true
        setContent {
            val appearance by app.appearance.appearance.collectAsStateWithLifecycle()
            DoTheme(appearance = appearance) { App(app, appearance, showEntry) }
        }
    }
}

@Composable
private fun App(app: DoApplication, appearance: Appearance, showEntry: Boolean) {
    val context = app.applicationContext
    val tasks by app.tasks.tasks.collectAsStateWithLifecycle()

    // Open state and identity are primitive/saveable; the task itself is resolved from the store.
    // A separate session id gives every new editor invocation a fresh draft while rotations keep
    // restoring the current one.
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editorTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorBucketName by rememberSaveable { mutableStateOf(Bucket.TODAY.name) }
    var editorSession by rememberSaveable { mutableIntStateOf(0) }
    var wallpaperOpen by rememberSaveable { mutableStateOf(false) }
    var trashOpen by rememberSaveable { mutableStateOf(false) }
    var reminderSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var timerOpen by rememberSaveable { mutableStateOf(false) }
    var recordsOpen by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf(AppPage.LANDING) }
    var entryVisible by remember { mutableStateOf(showEntry) }
    var launcherAnimationRunning by remember { mutableStateOf(false) }
    val sessions by app.focus.sessions.collectAsStateWithLifecycle()
    val activeFocus by app.focus.active.collectAsStateWithLifecycle()

    LaunchedEffect(page, entryVisible) {
        launcherAnimationRunning = false
        if (page == AppPage.LANDING && !entryVisible) {
            // Closing gets a clean white destination; the decorative loop resumes after the
            // app surface has settled instead of competing for the same frames.
            delay(240)
            launcherAnimationRunning = true
        }
    }
    // The bell for a countdown has to be an alarm: the run keeps going with the screen off.
    LaunchedEffect(activeFocus) { FocusAlarm.sync(context, activeFocus?.countdownEndsAt()) }

    val keepAlive by app.reminderSettings.keepAlive.collectAsStateWithLifecycle()
    // Applied from here rather than from Application.onCreate: starting a foreground service from
    // the background throws on Android 12+, and the process also starts for alarm broadcasts.
    LaunchedEffect(keepAlive) { KeepAliveService.apply(context, keepAlive) }

    LaunchedEffect(entryVisible) {
        if (entryVisible) return@LaunchedEffect
        // Keep startup and the user's first navigation uncontended. None of these maintenance
        // jobs is time-sensitive to the nearest few seconds.
        delay(15_000)
        // Application can also be created for an alarm receiver. Startup maintenance belongs to
        // the actual foreground entry so a cold alarm does not get delivered once here and again
        // by ReminderReceiver a moment later.
        runCatching {
            withContext(Dispatchers.Default) {
                app.tasks.pruneExpiredTrashAndAwait()
                Reminders.syncAll(context, app.tasks.tasks.value)
                Reminders.deliverMissedAndAwait(context, app.tasks)
                TrashExpiry.sync(context, app.tasks.tasks.value)
            }
        }
        while (true) {
            delay(60L * 60L * 1000L)
            runCatching {
                withContext(Dispatchers.Default) {
                    app.tasks.pruneExpiredTrashAndAwait()
                    // Pruning changes which task expires next, so the alarm has to move with it.
                    TrashExpiry.sync(context, app.tasks.tasks.value)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LauncherScreen(animate = launcherAnimationRunning)

        AppOpenTransition(current = page) { displayedPage ->
            when (displayedPage) {
            AppPage.LANDING -> Unit
            AppPage.DO -> WorkspaceHomeScreen(
                tasks = tasks,
                onToggle = { task ->
                    app.tasks.setDone(task.id, !task.done)?.let { Reminders.sync(context, it) }
                },
                onOpenTask = { task ->
                    editorTaskId = task.id
                    editorBucketName = task.bucket.name
                    editorSession += 1
                    editorOpen = true
                },
                onCompose = { bucket ->
                    editorTaskId = null
                    editorBucketName = bucket.name
                    editorSession += 1
                    editorOpen = true
                },
            )

            AppPage.FOCUS -> WorkspaceFocusScreen(
                sessions = sessions,
                active = activeFocus,
                onOpenTimer = { timerOpen = true },
                onOpenRecords = { recordsOpen = true },
            )

            AppPage.SETTINGS -> WorkspaceSettingsScreen(
                trashedCount = tasks.count { it.isTrashed },
                onOpenWallpaper = { wallpaperOpen = true },
                onOpenTrash = { trashOpen = true },
                onOpenReminderSettings = { reminderSettingsOpen = true },
                onExport = { uri ->
                    BackupFile.write(context, uri, app.tasks.tasks.value, app.focus.sessions.value)
                },
                onImport = { uri ->
                    // Once an import starts, finish both durable writes even if the Settings
                    // composition leaves (for example on rotation or a quick tab switch).
                    withContext(NonCancellable) {
                        BackupFile.read(context, uri)?.let { backup ->
                            val added = BackupFile.importInto(backup, app.tasks, app.focus)
                                ?: return@withContext null
                            // Restored tasks bring their reminders back with them.
                            withContext(Dispatchers.Default) {
                                Reminders.syncAll(context, app.tasks.tasks.value)
                            }
                            added
                        }
                    }
                },
            )
            }
        }

        DockBar(
            current = page,
            onSelect = { destination ->
                page = if (page == destination) AppPage.LANDING else destination
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (entryVisible) {
            AppEntryAnimation(
                appearance = appearance,
                onFinished = {
                    entryVisible = false
                },
            )
        }

    }

    if (editorOpen) {
        val editorBucket = Bucket.valueOf(editorBucketName)
        val editorInitial = editorTaskId?.let { id ->
            tasks.firstOrNull { it.id == id }?.let { stored ->
                // Home can open an overdue Plan task as an already-promoted Today task. Preserve
                // that editor input across recreation even though promotion is not persisted yet.
                if (stored.bucket == editorBucket) stored
                else stored.copy(
                    bucket = editorBucket,
                    planDay = if (editorBucket == Bucket.LATER) stored.planDay else null,
                )
            }
        }

        TaskEditorSheet(
            initial = editorInitial,
            defaultBucket = editorBucket,
            draftKey = editorSession,
            onDismiss = {
                editorOpen = false
                editorTaskId = null
            },
            onSave = { task ->
                if (tasks.any { it.id == task.id }) app.tasks.update(task) else app.tasks.add(task)
                Reminders.sync(context, task)
                editorOpen = false
                editorTaskId = null
            },
            onDelete = { task ->
                val deleted = app.tasks.trash(task.id)
                Reminders.cancel(context, task.id)
                deleted?.let { TrashExpiry.sync(context, app.tasks.tasks.value) }
                editorOpen = false
                editorTaskId = null
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
            onBlurChange = app.appearance::previewBlur,
            onDimChange = app.appearance::previewDim,
            onAdjustmentsFinished = app.appearance::persistWallpaperAdjustments,
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
