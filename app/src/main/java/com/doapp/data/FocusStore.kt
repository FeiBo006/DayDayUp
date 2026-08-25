package com.doapp.data

import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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
    private val json = Json { ignoreUnknownKeys = true }
    private val sessionsLock = Any()

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
        mutateSessions { it + session }
        return session
    }

    // — the log —

    fun delete(id: String) = mutateSessions { list -> list.filterNot { it.id == id } }

    /** Merges an imported log, keeping local copies on an id clash. @return how many were added. */
    fun merge(incoming: List<FocusSession>): Int {
        return queueMerge(incoming).added
    }

    /** Merges on a worker thread and returns only after the imported snapshot reaches disk. */
    suspend fun mergeAndAwait(incoming: List<FocusSession>): Int {
        val operation = withContext(Dispatchers.Default) { queueMerge(incoming) }
        operation.write?.await()
        return operation.added
    }

    private fun queueMerge(incoming: List<FocusSession>): MergeOperation =
        synchronized(sessionsLock) {
            val current = _sessions.value
            val known = current.mapTo(mutableSetOf()) { it.id }
            val added = distinctMissingById(incoming, known) { it.id }
            val write = if (added.isEmpty()) null else persistLocked(current + added)
            MergeOperation(added = added.size, write = write)
        }

    private fun mutateSessions(
        block: (List<FocusSession>) -> List<FocusSession>,
    ): Deferred<Unit>? = synchronized(sessionsLock) {
        val current = _sessions.value
        val next = block(current)
        if (next == current) null else persistLocked(next)
    }

    private fun persistLocked(next: List<FocusSession>): Deferred<Unit> {
        val sorted = next.sortedBy { it.startedAt }
        _sessions.value = sorted
        return file.write { json.encodeToString(sorted) }
    }

    private data class MergeOperation(
        val added: Int,
        val write: Deferred<Unit>?,
    )

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
