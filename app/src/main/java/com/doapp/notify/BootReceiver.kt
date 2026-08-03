package com.doapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doapp.data.TaskStore

/** Alarms don't survive a reboot or an app update — put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val tasks = TaskStore.readFromDisk(context)
        Reminders.syncAll(context, tasks)
        TrashExpiry.sync(context, tasks)
    }
}
