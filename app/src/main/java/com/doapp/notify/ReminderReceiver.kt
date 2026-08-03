package com.doapp.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.doapp.MainActivity
import com.doapp.R
import com.doapp.data.TaskStore

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(Reminders.EXTRA_TASK_ID) ?: return

        // The alarm may outlive the task. Trust the file, not the extras, for current state.
        val task = TaskStore.readFromDisk(context).firstOrNull { it.id == taskId }
        if (task != null && task.done) return

        val title = task?.title ?: intent.getStringExtra(Reminders.EXTRA_TITLE) ?: return
        val note = task?.note?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra(Reminders.EXTRA_NOTE)?.takeIf { it.isNotBlank() }
        val body = note ?: "到时间了，记得完成这件事"

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(Notification.PRIORITY_HIGH)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(taskId.hashCode(), notification)
    }
}
