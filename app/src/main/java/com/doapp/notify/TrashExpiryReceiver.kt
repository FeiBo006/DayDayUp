package com.doapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doapp.data.TaskStore

class TrashExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.doapp.TRASH_EXPIRY") return
        val store = TaskStore(context)
        store.pruneExpiredTrash()
        TrashExpiry.sync(context, store.tasks.value)
    }
}
