package com.doapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.doapp.DoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Alarms don't survive a reboot or an app update — put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        workScope.launch {
            try {
                val app = appContext as DoApplication

                // BOOT_COMPLETED is one of the few places a foreground service may be started
                // from the background, and the only chance to restore it without opening the app.
                if (app.reminderSettings.keepAlive.value) {
                    runCatching { KeepAliveService.apply(appContext, true) }
                        .onFailure { Log.w(TAG, "Failed to restore keep-alive service", it) }
                }

                val store = app.tasks
                store.pruneExpiredTrashAndAwait()
                Reminders.syncAll(appContext, store.tasks.value)
                // Reminders that came due while the phone was off — the alarms for those are gone.
                Reminders.deliverMissedAndAwait(appContext, store)
                TrashExpiry.sync(appContext, store.tasks.value)
                // AlarmManager drops countdown alarms on reboot and package replacement too.
                FocusAlarm.sync(appContext, app.focus.active.value?.countdownEndsAt())
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to restore background schedules", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
        val workScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
