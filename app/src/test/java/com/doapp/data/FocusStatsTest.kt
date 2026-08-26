package com.doapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FocusStatsTest {

    @Test
    fun weekBoundsRunMondayThroughSunday() {
        val anchor = LocalDate.of(2026, 8, 23)

        assertEquals(
            LocalDate.of(2026, 8, 17) to LocalDate.of(2026, 8, 23),
            rangeBounds(StatRange.WEEK, anchor),
        )
    }

    @Test
    fun summaryFiltersByStartDayAndGroupsLabels() {
        val sessions = listOf(
            session("a", "写作", "2026-08-23T01:00:00Z", 20),
            session("b", "写作", "2026-08-23T02:00:00Z", 10),
            session("c", "阅读", "2026-08-23T03:00:00Z", 30),
            session("outside", "运动", "2026-08-22T03:00:00Z", 60),
        )

        val summary = summarize(
            sessions = sessions,
            from = LocalDate.of(2026, 8, 23),
            to = LocalDate.of(2026, 8, 23),
            zone = ZoneOffset.UTC,
        )

        assertEquals(3, summary.sessions)
        assertEquals(60L * 60_000L, summary.totalMillis)
        assertEquals(listOf("写作", "阅读"), summary.slices.map { it.label })
        assertTrue(summary.slices.all { kotlin.math.abs(it.fraction - 0.5f) < 0.0001f })
    }

    @Test
    fun sameVisibleLabelIsShownOnceWithCombinedDuration() {
        val sessions = listOf(
            session("a", "代码", "2026-08-23T01:00:00Z", 1),
            session("b", " 代码 ", "2026-08-23T02:00:00Z", 1),
            session("c", "阅读", "2026-08-23T03:00:00Z", 1),
        )

        val slices = aggregateFocusLabels(sessions)

        assertEquals(listOf("代码", "阅读"), slices.map { it.label })
        assertEquals(2L * 60_000L, slices.first().millis)
        assertEquals(1, slices.count { it.label == "代码" })
    }

    @Test
    fun dailySeriesKeepsEmptyDaysAndSessionDetails() {
        val sessions = listOf(
            session("a", "写作", "2026-08-21T01:00:00Z", 20),
            session("b", "阅读", "2026-08-23T02:00:00Z", 70),
        )

        val days = dailyFocusSummaries(
            sessions = sessions,
            from = LocalDate.of(2026, 8, 21),
            to = LocalDate.of(2026, 8, 23),
            zone = ZoneOffset.UTC,
        )

        assertEquals(listOf(1, 0, 1), days.map { it.sessionCount })
        assertEquals("阅读", days.last().sessions.single().label)
        assertEquals(70L * 60_000L, days.last().totalMillis)
    }

    @Test
    fun heatIntensityUsesStableStudyThresholds() {
        assertEquals(0, focusIntensity(0L))
        assertEquals(1, focusIntensity(29L * 60_000L))
        assertEquals(2, focusIntensity(30L * 60_000L))
        assertEquals(3, focusIntensity(60L * 60_000L))
        assertEquals(4, focusIntensity(120L * 60_000L))
    }

    private fun session(
        id: String,
        label: String,
        startedAt: String,
        minutes: Long,
    ): FocusSession {
        val start = Instant.parse(startedAt).toEpochMilli()
        return FocusSession(
            id = id,
            label = label,
            startedAt = start,
            endedAt = start + minutes * 60_000L,
        )
    }
}
