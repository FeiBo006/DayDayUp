package com.doapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Finished focus sessions, plus whatever run is currently going.
 *
 * The active run lives in preferences rather than the session file: it changes on every pause and
 * resume, and it isn't history yet. Only a session that actually ended is written to the log.
 */
class FocusStore(context: Context) {

    private val file = AtomicJsonFile(File(context.filesDir, "focus.json"))
    private val prefs = context.getSharedPreferences("focus", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _sessions = MutableStateFlow(readSessions())
    val sessions: StateFlow<List<FocusSession>> = _sessions.asStateFlow()

    private val _active = MutableStateFlow(readActive())
    val active: StateFlow<ActiveFocus?> = _active.asStateFlow()

    // — the run in progress —

    fun start(
        label: String,
        taskId: String?,
        mode: FocusMode,
        targetMillis: Long = 0L,
        now: Long = System.currentTimeMillis(),
    ) {
        writeActive(
            ActiveFocus(
                taskId = taskId,
                label = label.ifBlank { DEFAULT_LABEL },
                mode = mode,
                targetMillis = targetMillis,
                startedAt = now,
                runningSince = now,
            )
        )
    }

    fun pause(now: Long = System.currentTimeMillis()) {
        val current = _active.value ?: return
        if (current.isPaused) return
        writeActive(current.copy(banked = current.elapsed(now), runningSince = null))
    }

    fun resume(now: Long = System.currentTimeMillis()) {
        val current = _active.value ?: return
        if (!current.isPaused) return
        writeActive(current.copy(runningSince = now))
    }

    /** Drops the run without recording anything. */
    fun cancel() = writeActive(null)

    /**
     * Ends the run and files it. Stretches under [MINIMUM_MILLIS] are thrown away rather than
     * logged — a run started and stopped by accident is noise in the statistics, not data.
     *
     * @return the session that was recorded, or null when there was nothing worth keeping.
     */
    fun finish(now: Long = System.currentTimeMillis()): FocusSession? {
        val current = _active.value ?: return null
        writeActive(null)

        val elapsed = when {
            // A completed countdown banks exactly its target, not the overshoot from a late stop.
            current.mode == FocusMode.COUNTDOWN && current.elapsed(now) >= current.targetMillis ->
                current.targetMillis
            else -> current.elapsed(now)
        }
        if (elapsed < MINIMUM_MILLIS) return null

        val session = FocusSession(
            taskId = current.taskId,
            label = current.label,
            mode = current.mode,
            startedAt = current.startedAt,
            endedAt = current.startedAt + elapsed,
        )
        persist(_sessions.value + session)
        return session
    }

    // — the log —

    fun delete(id: String) = persist(_sessions.value.filterNot { it.id == id })

    /** Merges an imported log, keeping local copies on an id clash. @return how many were added. */
    fun merge(incoming: List<FocusSession>): Int {
        val known = _sessions.value.mapTo(mutableSetOf()) { it.id }
        val added = incoming.filterNot { it.id in known }
        if (added.isNotEmpty()) persist(_sessions.value + added)
        return added.size
    }

    private fun persist(next: List<FocusSession>) {
        _sessions.value = next.sortedBy { it.startedAt }
        file.write(json.encodeToString(_sessions.value))
    }

    private fun writeActive(next: ActiveFocus?) {
        _active.value = next
        prefs.edit().apply {
            if (next == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, json.encodeToString(next))
        }.apply()
    }

    private fun readSessions(): List<FocusSession> {
        val text = file.read() ?: return emptyList()
        return runCatching { json.decodeFromString<List<FocusSession>>(text) }
            .getOrElse {
                file.quarantine()
                emptyList()
            }
    }

    private fun readActive(): ActiveFocus? {
        val text = prefs.getString(KEY_ACTIVE, null) ?: return null
        return runCatching { json.decodeFromString<ActiveFocus>(text) }.getOrNull()
    }

    companion object {
        const val DEFAULT_LABEL = "专注"

        /** Below this a run is a misfire, not a session. */
        const val MINIMUM_MILLIS = 60L * 1000L

        private const val KEY_ACTIVE = "active"
    }
}
