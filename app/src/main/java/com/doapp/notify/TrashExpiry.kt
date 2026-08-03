package com.doapp.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.doapp.data.Task

object TrashExpiry {

    private const val ACTION = "com.doapp.TRASH_EXPIRY"
    private const val REQUEST_CODE = 701

    fun sync(context: Context, tasks: List<Task>) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context)
        manager.cancel(pending)

        val next = tasks.asSequence()
            .mapNotNull { it.deletedAt }
            .map { it + 7L * 24L * 60L * 60L * 1000L }
            .filter { it > System.currentTimeMillis() }
            .minOrNull()
            ?: return

        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
    }

    fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, TrashExpiryReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
