package com.doapp.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class FocusMode {
    /** Counts up from zero. For work with no agreed end. */
    STOPWATCH,

    /** Counts down to a target and announces itself. The pomodoro shape. */
    COUNTDOWN,
}

/**
 * One finished stretch of focused work.
 *
 * [label] is a snapshot of the task's title rather than a lookup through [taskId], because the
 * statistics have to outlive the task. Finishing and clearing a task you spent forty hours on
 * must not erase the forty hours.
 */
@Serializable
data class FocusSession(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String? = null,
    val label: String,
    val mode: FocusMode = FocusMode.STOPWATCH,
    val startedAt: Long,
    val endedAt: Long,
) {
    val durationMillis: Long get() = (endedAt - startedAt).coerceAtLeast(0L)
}

/**
 * A focus run in progress, persisted so closing the app — or the system killing it — doesn't
 * lose the session.
 *
 * Time is derived from wall-clock stamps rather than counted by a ticker, so the elapsed value
 * stays right no matter how long the process was away. [banked] holds what earlier run segments
 * contributed; [runningSince] is when the current segment began, and is null while paused.
 */
@Serializable
data class ActiveFocus(
    val taskId: String? = null,
    val label: String,
    val mode: FocusMode,
    /** Target length for [FocusMode.COUNTDOWN]; ignored for a stopwatch. */
    val targetMillis: Long = 0L,
    val startedAt: Long,
    val runningSince: Long?,
    val banked: Long = 0L,
) {
    val isPaused: Boolean get() = runningSince == null

    fun elapsed(now: Long): Long = banked + (runningSince?.let { now - it } ?: 0L).coerceAtLeast(0L)

    /** Countdown only: what's left, floored at zero. */
    fun remaining(now: Long): Long = (targetMillis - elapsed(now)).coerceAtLeast(0L)

    fun isComplete(now: Long): Boolean =
        mode == FocusMode.COUNTDOWN && elapsed(now) >= targetMillis

    /** Wall-clock moment a running countdown will hit zero, or null when it can't be known. */
    fun countdownEndsAt(): Long? {
        if (mode != FocusMode.COUNTDOWN) return null
        val since = runningSince ?: return null
        return since + (targetMillis - banked).coerceAtLeast(0L)
    }
}
