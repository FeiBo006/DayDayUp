package com.doapp.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Where a task lives. The app only has two horizons on purpose: what you decided to do
 * today, and everything you've promised yourself for some later day.
 */
@Serializable
enum class Bucket { TODAY, LATER }

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val bucket: Bucket = Bucket.TODAY,
    val done: Boolean = false,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    /** Epoch millis of the reminder, or null when the task has none. */
    val reminderAt: Long? = null,
    /** Epoch day (LocalDate.toEpochDay) this task is planned for, when it lives in Plan. */
    val planDay: Long? = null,
    /** Set when the task sits in the trash; null while it's active. */
    val deletedAt: Long? = null,
) {
    val hasReminder: Boolean get() = reminderAt != null
    val isTrashed: Boolean get() = deletedAt != null
}
