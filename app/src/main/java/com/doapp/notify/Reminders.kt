package com.doapp.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.doapp.MainActivity
import com.doapp.data.Task
import com.doapp.data.TaskStore
import com.doapp.data.isReminderDue
import kotlinx.coroutines.Deferred

object Reminders {

    // A new channel id lets existing installs recover from an old muted channel.
    const val CHANNEL_ID = "task_reminders_v2"

    /** How late a missed reminder may still be worth delivering. */
    private const val MISSED_GRACE_MILLIS = 48L * 60L * 60L * 1000L

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TITLE = "task_title"
    const val EXTRA_NOTE = "task_note"

    fun sync(context: Context, task: Task) {
        val at = task.reminderAt
        // A trashed task keeps its reminder so restoring it brings the reminder back — but it
        // must not fire while the task sits in the bin, and syncAll() sees the whole list.
        if (task.done || task.isTrashed || task.notifiedAt != null ||
            at == null || at <= System.currentTimeMillis()
        ) {
            cancel(context, task.id)
        } else {
            schedule(context, task, at)
        }
    }

    fun syncAll(context: Context, tasks: List<Task>) = tasks.asSequence()
        // A task that has never had a reminder cannot own a stale PendingIntent. Skipping it is
        // important after a large import and keeps boot recovery comfortably within its deadline.
        .filter { it.reminderAt != null }
        .forEach { sync(context, it) }

    /**
     * Delivers reminders that came due while nothing was listening. An alarm is held by the
     * system, not by us, and it is dropped when the phone is powered off or when the app is
     * force-stopped — which on a lot of Chinese ROMs is what swiping the app away does. Without
     * this pass those reminders are simply gone, which is the "只有挂着后台才收得到" symptom.
     *
     * Runs at every app start and at boot. Each reminder goes out at most once, tracked by
     * [Task.notifiedAt].
     */
    fun deliverMissed(context: Context, store: TaskStore): Deferred<Unit>? {
        val now = System.currentTimeMillis()
        val deliveredOrRetired = mutableSetOf<String>()
        store.tasks.value
            .filter { it.isReminderDue(now) }
            .forEach { task ->
                // Anything older than the grace window is retired quietly. Firing a notification
                // for something three weeks overdue is noise, not a reminder.
                val withinGrace = now - (task.reminderAt ?: 0L) <= MISSED_GRACE_MILLIS
                // A denied notification permission is not a delivery. Keep the reminder pending
                // so the next app start can retry after the user enables notifications. Very old
                // reminders are intentionally retired, otherwise they would be reconsidered on
                // every launch forever.
                val handled = !withinGrace ||
                    Notifications.post(context, task.id, task.title, task.note, late = true)
                if (handled) deliveredOrRetired += task.id
            }
        return store.markNotified(deliveredOrRetired, now)
    }

    /** Delivers the catch-up batch and returns only after its de-duplication state is durable. */
    suspend fun deliverMissedAndAwait(context: Context, store: TaskStore) {
        deliverMissed(context, store)?.await()
    }

    fun cancel(context: Context, taskId: String) {
        alarmManager(context).cancel(pendingIntent(context, taskId, null, null, mutable = false))
    }

    private fun schedule(context: Context, task: Task, at: Long) {
        val manager = alarmManager(context)
        val intent = pendingIntent(context, task.id, task.title, task.note, mutable = false)

        // Exact alarms need a user-granted permission on Android 12+. Without it we still fire,
        // just batched by the system — a reminder that is a few minutes late beats none at all.
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (canBeExact) {
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(at, showIntent(context, task.id)),
                intent,
            )
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    private fun showIntent(context: Context, taskId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            "open_$taskId".hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(
        context: Context,
        taskId: String,
        title: String?,
        note: String?,
        mutable: Boolean,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            // The action keeps otherwise-identical intents distinct, so cancel() matches the
            // right alarm even though extras are ignored by PendingIntent equality.
            action = "com.doapp.REMIND.$taskId"
            putExtra(EXTRA_TASK_ID, taskId)
            title?.let { putExtra(EXTRA_TITLE, it) }
            note?.let { putExtra(EXTRA_NOTE, it) }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, taskId.hashCode(), intent, flags)
    }
}
