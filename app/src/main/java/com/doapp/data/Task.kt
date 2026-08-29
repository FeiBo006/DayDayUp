package com.doapp.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/**
 * Where a task lives. The app only has two horizons on purpose: what you decided to do
 * today, and everything you've promised yourself for some later day.
 */
@Serializable
enum class Bucket { TODAY, LATER }

@Serializable
enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY }

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
    /**
     * When the reminder notification actually went out. Alarms don't survive a force-stop or a
     * powered-off phone, so a reminder can come due with nothing listening; this is what lets the
     * app deliver that one late instead of losing it, and deliver it only once.
     */
    val notifiedAt: Long? = null,
    /** Epoch day (LocalDate.toEpochDay) this task is planned for, when it lives in Plan. */
    val planDay: Long? = null,
    /** Lower values appear earlier in Today. Zero means the task has not been manually ordered. */
    val todayOrder: Long = 0L,
    /** How the task creates its next occurrence after completion. */
    val repeatRule: RepeatRule = RepeatRule.NONE,
    /** Set when the task sits in the trash; null while it's active. */
    val deletedAt: Long? = null,
) {
    val hasReminder: Boolean get() = reminderAt != null
    val isTrashed: Boolean get() = deletedAt != null
    val repeats: Boolean get() = repeatRule != RepeatRule.NONE
}

/** Current store state is authoritative when an alarm arrives; stale intents are not. */
fun Task.isReminderDue(now: Long): Boolean =
    !done && !isTrashed && notifiedAt == null && reminderAt?.let { it <= now } == true

fun Task.todaySortKey(): Long = when {
    todayOrder > 0L -> todayOrder
    createdAt > 0L -> createdAt
    else -> Long.MAX_VALUE
}

fun RepeatRule.nextDate(after: LocalDate): LocalDate? = when (this) {
    RepeatRule.NONE -> null
    RepeatRule.DAILY -> after.plusDays(1)
    RepeatRule.WEEKLY -> after.plusWeeks(1)
    RepeatRule.MONTHLY -> after.plusMonths(1)
}
