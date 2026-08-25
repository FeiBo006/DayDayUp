package com.doapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doapp.DoApplication
import com.doapp.data.isReminderDue

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(Reminders.EXTRA_TASK_ID) ?: return

        // An alarm can race with deleting, completing, or rescheduling a task. The persisted task
        // is authoritative; stale intent extras must never resurrect a ghost reminder.
        val store = (context.applicationContext as? DoApplication)?.tasks ?: return
        val task = store.tasks.value.firstOrNull { it.id == taskId } ?: return
        val now = System.currentTimeMillis()
        if (!task.isReminderDue(now)) return

        if (Notifications.post(context, taskId, task.title, task.note, late = false)) {
            // A receiver process may be killed as soon as onReceive returns. goAsync keeps it
            // alive until the atomic JSON write records the delivery.
            val pendingResult = goAsync()
            val write = runCatching { store.markNotified(taskId, now) }.getOrNull()
            if (write == null) pendingResult.finish()
            else write.invokeOnCompletion { pendingResult.finish() }
        }
    }
}
