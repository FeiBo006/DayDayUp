package com.doapp.notify

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.doapp.MainActivity
import com.doapp.R

/**
 * Holds the process open so the system doesn't drop the app's pending alarms.
 *
 * Android keeps alarms across ordinary process death — it restarts the process to deliver them.
 * A *force-stop* is different: it clears every alarm the app has registered. On a lot of Chinese
 * ROMs, swiping the app out of recents is a force-stop, which is why reminders only arrive while
 * the app happens to be alive. A foreground service is the one thing those ROMs consistently
 * respect. The price is a permanent notification, so this is opt-in and off by default.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching { startForeground(NOTIFICATION_ID, notification()) }
        // If the system tears us down anyway, ask to come back.
        return START_STICKY
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.keep_alive_title))
            .setContentText(getString(R.string.keep_alive_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setShowWhen(false)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "keep_alive"
        private const val NOTIFICATION_ID = 1001

        /**
         * Starting a foreground service from the background throws on Android 12+, so callers have
         * to be in the foreground or inside a documented exemption (BOOT_COMPLETED is one). The
         * runCatching is there so a mistimed call can never take the app down with it.
         */
        fun apply(context: Context, enabled: Boolean) {
            val intent = Intent(context, KeepAliveService::class.java)
            runCatching {
                if (enabled) ContextCompat.startForegroundService(context, intent)
                else context.stopService(intent)
            }
        }
    }
}
