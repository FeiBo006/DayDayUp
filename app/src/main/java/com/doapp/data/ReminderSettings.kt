package com.doapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How hard the app works to get a reminder delivered. Kept apart from [AppearanceStore] because
 * this is behaviour rather than looks.
 */
class ReminderSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    private val _keepAlive = MutableStateFlow(prefs.getBoolean(KEY_KEEP_ALIVE, false))

    /** Off by default — it costs a permanent notification, and not everyone wants to pay that. */
    val keepAlive: StateFlow<Boolean> = _keepAlive.asStateFlow()

    fun setKeepAlive(enabled: Boolean) {
        _keepAlive.value = enabled
        prefs.edit().putBoolean(KEY_KEEP_ALIVE, enabled).apply()
    }

    private companion object {
        const val KEY_KEEP_ALIVE = "keep_alive"
    }
}
