package com.doapp.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.doapp.MainActivity
import com.doapp.R

/**
 * Builds and posts the one notification this app has. Both paths reach it: the alarm firing on
 * time, and the catch-up pass for reminders that came due while nothing was listening.
 */
object Notifications {

    /**
     * @param late the reminder is being delivered after its time, because the alarm never got to
     *   run. Saying so is better than a notification that looks like it arrived on schedule.
     * @return whether a notification actually went out.
     */
    fun post(context: Context, taskId: String, title: String, note: String?, late: Boolean): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return false

        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        if (manager.getNotificationChannel(Reminders.CHANNEL_ID)?.importance ==
            NotificationManager.IMPORTANCE_NONE
        ) return false

        val body = note?.takeIf { it.isNotBlank() }
            ?: if (late) "这条提醒没能准时送达" else "到时间了，记得完成这件事"

        val open = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (late) "$title（补发）" else title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        return runCatching {
            manager.notify(taskId.hashCode(), notification)
        }.isSuccess
    }
}
