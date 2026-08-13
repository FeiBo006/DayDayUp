package com.doapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doapp.DoApplication

/** Alarms don't survive a reboot or an app update — put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        // Application.onCreate always runs before any receiver in the process, so the store is
        // already loaded — read it from memory instead of parsing JSON on the main thread.
        val app = context.applicationContext as DoApplication

        // BOOT_COMPLETED is one of the few places a foreground service may be started from the
        // background, and it's the only chance to get the keep-alive back up if the user never
        // opens the app after a reboot.
        if (app.reminderSettings.keepAlive.value) KeepAliveService.apply(context, true)

        val store = app.tasks
        store.pruneExpiredTrash()
        Reminders.syncAll(context, store.tasks.value)
        // Reminders that came due while the phone was off — the alarms for those are gone.
        Reminders.deliverMissed(context, store)
        TrashExpiry.sync(context, store.tasks.value)
    }
}
