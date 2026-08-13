package com.doapp.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.doapp.DoApplication

/**
 * The bell at the end of a countdown.
 *
 * The running clock on screen is derived from timestamps, so it needs no ticker — but a countdown
 * that ends while the phone is in a pocket has to announce itself, and only an alarm can do that.
 * Reuses the same exact-alarm route as task reminders.
 */
object FocusAlarm {

    const val ACTION = "com.doapp.FOCUS_DONE"
    private const val REQUEST_CODE = 802

    /** Points the alarm at [endsAt], or clears it when nothing is counting down. */
    fun sync(context: Context, endsAt: Long?) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context)
        manager.cancel(pending)
        if (endsAt == null || endsAt <= System.currentTimeMillis()) return

        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (exact) {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(endsAt, pending), pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pending)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, FocusReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FocusAlarm.ACTION) return
        val app = context.applicationContext as? DoApplication ?: return
        val active = app.focus.active.value ?: return
        if (!active.isComplete(System.currentTimeMillis())) return

        Notifications.post(
            context = context,
            taskId = FOCUS_NOTIFICATION_KEY,
            title = "专注完成 · ${active.label}",
            note = "这一轮已经走完，去应用里收下它。",
            late = false,
        )
    }

    private companion object {
        /** A fixed key: only one countdown can be running, so it only ever needs one slot. */
        const val FOCUS_NOTIFICATION_KEY = "focus-countdown"
    }
}
