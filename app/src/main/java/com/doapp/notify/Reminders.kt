package com.doapp.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.doapp.MainActivity
import com.doapp.data.Task

object Reminders {

    // A new channel id lets existing installs recover from an old muted channel.
    const val CHANNEL_ID = "task_reminders_v2"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TITLE = "task_title"
    const val EXTRA_NOTE = "task_note"

    fun sync(context: Context, task: Task) {
        val at = task.reminderAt
        if (task.done || at == null || at <= System.currentTimeMillis()) {
            cancel(context, task.id)
        } else {
            schedule(context, task, at)
        }
    }

    fun syncAll(context: Context, tasks: List<Task>) = tasks.forEach { sync(context, it) }

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
