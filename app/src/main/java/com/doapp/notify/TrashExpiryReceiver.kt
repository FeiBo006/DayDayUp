package com.doapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doapp.DoApplication

class TrashExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.doapp.TRASH_EXPIRY") return
        // The app's own store, not a second one: a private copy would prune the file while the
        // live instance still held the old list, and the next edit would write the trash back.
        val store = (context.applicationContext as DoApplication).tasks
        store.pruneExpiredTrash()
        TrashExpiry.sync(context, store.tasks.value)
    }
}
