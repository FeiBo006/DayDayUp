package com.doapp.ui

import com.doapp.data.TaskStore
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

private val zone: ZoneId get() = ZoneId.systemDefault()

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

fun LocalDateTime.toEpochMillis(): Long = atZone(zone).toInstant().toEpochMilli()

/** Short, relative-where-it-helps reminder label: "14:30", "明天 09:00", "3月5日 09:00". */
fun formatReminder(epochMillis: Long): String {
    val dateTime = epochMillis.toLocalDateTime()
    val today = LocalDate.now()
    val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (dateTime.toLocalDate()) {
        today -> time
        today.plusDays(1) -> "明天 $time"
        today.minusDays(1) -> "昨天 $time"
        else -> "${dateTime.monthValue}月${dateTime.dayOfMonth}日 $time"
    }
}

/** The header subtitle: "7月27日 星期日". */
fun formatToday(date: LocalDate = LocalDate.now()): String {
    val weekday = date.dayOfWeek.getDisplayName(
        java.time.format.TextStyle.FULL,
        Locale.getDefault(),
    )
    return "${date.monthValue}月${date.dayOfMonth}日 $weekday"
}

/** A plan-day label: "明天", "8月3日 周五" … Relative where it helps, absolute elsewhere. */
fun formatPlanDay(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val today = LocalDate.now()
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return when (date) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        today.minusDays(1) -> "昨天"
        else -> "${date.monthValue}月${date.dayOfMonth}日 $weekday"
    }
}

fun formatDate(dateTime: LocalDateTime): String =
    "${dateTime.year}年${dateTime.monthValue}月${dateTime.dayOfMonth}日"

fun formatTime(dateTime: LocalDateTime): String =
    dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))

fun trashDaysRemaining(deletedAt: Long, now: Long = System.currentTimeMillis()): Int {
    val remaining = deletedAt + TaskStore.TRASH_RETENTION_MILLIS - now
    return max(0, ceil(remaining / (24L * 60L * 60L * 1000.0)).toInt())
}
