package com.doapp.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The spans the statistics page can be read over. */
enum class StatRange { DAY, WEEK, MONTH, CUSTOM }

/** One wedge: a label, what it cost, and its share of the whole. */
data class FocusSlice(
    val label: String,
    val millis: Long,
    val fraction: Float,
    /**
     * True only for the folded tail. It has to be carried explicitly rather than inferred from
     * the label or the position: "other" is the absence of an identity, so it takes the neutral
     * grey instead of the next categorical hue, and a task legitimately named 其他 must not.
     */
    val isOther: Boolean = false,
)

data class FocusSummary(
    val sessions: Int,
    val totalMillis: Long,
    val slices: List<FocusSlice>,
) {
    val isEmpty: Boolean get() = sessions == 0

    companion object {
        val Empty = FocusSummary(sessions = 0, totalMillis = 0L, slices = emptyList())
    }
}

/**
 * The dates a range covers, given the date it is anchored on. Weeks run Monday to Sunday.
 * [StatRange.CUSTOM] ignores the anchor and uses the explicit bounds, ordered defensively so a
 * backwards pair still describes a real span.
 */
fun rangeBounds(
    range: StatRange,
    anchor: LocalDate,
    customStart: LocalDate = anchor,
    customEnd: LocalDate = anchor,
): Pair<LocalDate, LocalDate> = when (range) {
    StatRange.DAY -> anchor to anchor
    StatRange.WEEK -> {
        val start = anchor.with(DayOfWeek.MONDAY)
        start to start.plusDays(6)
    }
    StatRange.MONTH -> anchor.withDayOfMonth(1) to anchor.withDayOfMonth(anchor.lengthOfMonth())
    StatRange.CUSTOM ->
        if (customStart.isAfter(customEnd)) customEnd to customStart else customStart to customEnd
}

/** Moves the anchor by one whole unit. [StatRange.CUSTOM] has nothing to step through. */
fun stepAnchor(range: StatRange, anchor: LocalDate, forward: Boolean): LocalDate {
    val delta = if (forward) 1L else -1L
    return when (range) {
        StatRange.DAY -> anchor.plusDays(delta)
        StatRange.WEEK -> anchor.plusWeeks(delta)
        StatRange.MONTH -> anchor.plusMonths(delta)
        StatRange.CUSTOM -> anchor
    }
}

/**
 * Rolls the sessions inside a date span into a total and a set of wedges, largest first.
 *
 * A session belongs to the day it *started*, so a stretch that runs past midnight counts toward
 * the evening it belongs to rather than splitting across two days and reading as two short ones.
 */
fun summarize(
    sessions: List<FocusSession>,
    from: LocalDate,
    to: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): FocusSummary {
    val inRange = sessions.filter { session ->
        val day = Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
        !day.isBefore(from) && !day.isAfter(to)
    }
    if (inRange.isEmpty()) return FocusSummary.Empty

    val total = inRange.sumOf { it.durationMillis }
    val slices = inRange
        .groupBy { it.label }
        .map { (label, group) -> label to group.sumOf { it.durationMillis } }
        .sortedByDescending { it.second }
        .map { (label, millis) ->
            FocusSlice(
                label = label,
                millis = millis,
                fraction = if (total == 0L) 0f else millis.toFloat() / total,
            )
        }

    return FocusSummary(sessions = inRange.size, totalMillis = total, slices = slices)
}

/** "9 小时 3 分钟" — the long form, for totals that want to be read carefully. */
fun formatDurationLong(millis: Long): String {
    val minutes = millis / 60_000L
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h 小时 $m 分钟"
        h > 0 -> "$h 小时"
        else -> "$m 分钟"
    }
}

/** "4小时0分" — the compact form, for list rows and wedge labels. */
fun formatDurationShort(millis: Long): String {
    val minutes = millis / 60_000L
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}小时${m}分" else "${m}分"
}

/** "01:23:45" / "23:45" — the running clock, where digits must not jump around. */
fun formatClock(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
