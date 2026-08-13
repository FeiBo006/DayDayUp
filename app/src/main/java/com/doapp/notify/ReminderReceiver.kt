package com.doapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doapp.DoApplication

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(Reminders.EXTRA_TASK_ID) ?: return

        // The alarm may outlive the task. Trust the store, not the extras, for current state.
        val store = (context.applicationContext as? DoApplication)?.tasks
        val task = store?.tasks?.value?.firstOrNull { it.id == taskId }
        if (task != null && (task.done || task.isTrashed)) return

        val title = task?.title ?: intent.getStringExtra(Reminders.EXTRA_TITLE) ?: return
        val note = task?.note ?: intent.getStringExtra(Reminders.EXTRA_NOTE)

        if (Notifications.post(context, taskId, title, note, late = false)) {
            // Recorded so the catch-up pass doesn't deliver this one a second time.
            store?.markNotified(taskId)
        }
    }
}
