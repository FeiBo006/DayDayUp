package com.doapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTest {

    @Test
    fun pausedTimeDoesNotIncreaseElapsed() {
        val paused = ActiveFocus(
            label = "写作",
            mode = FocusMode.STOPWATCH,
            startedAt = 1_000L,
            runningSince = null,
            banked = 3_000L,
        )

        assertEquals(3_000L, paused.elapsed(100_000L))
        assertTrue(paused.isPaused)
        assertNull(paused.countdownEndsAt())
    }

    @Test
    fun countdownEndAccountsForBankedTime() {
        val countdown = ActiveFocus(
            label = "阅读",
            mode = FocusMode.COUNTDOWN,
            targetMillis = 10_000L,
            startedAt = 1_000L,
            runningSince = 5_000L,
            banked = 4_000L,
        )

        assertEquals(11_000L, countdown.countdownEndsAt())
        assertEquals(1_000L, countdown.remaining(10_000L))
        assertFalse(countdown.isComplete(10_000L))
        assertTrue(countdown.isComplete(11_000L))
    }
}
