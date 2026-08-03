package com.doapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.doapp.data.AppearanceStore
import com.doapp.data.TaskStore
import com.doapp.notify.Reminders
import com.doapp.notify.TrashExpiry

class DoApplication : Application() {

    lateinit var tasks: TaskStore
        private set
    lateinit var appearance: AppearanceStore
        private set

    override fun onCreate() {
        super.onCreate()
        tasks = TaskStore(this)
        appearance = AppearanceStore(this)
        createNotificationChannel()
        tasks.pruneExpiredTrash()
        TrashExpiry.sync(this, tasks.tasks.value)
        Reminders.syncAll(this, tasks.tasks.value)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Reminders.CHANNEL_ID,
            getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.channel_reminders_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 100, 250)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
