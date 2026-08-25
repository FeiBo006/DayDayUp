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
 * The whole persistence layer: one JSON file, held in memory, rewritten on every mutation.
 * A todo list is a few hundred rows at most — a database would be ceremony without payoff.
 *
 * There is exactly one instance, owned by [com.doapp.DoApplication]. Receivers must reach for
 * that one rather than constructing their own: two instances would each hold a private copy of
 * the list and take turns overwriting the other's work.
 */
class TaskStore(context: Context) {

    private val file = AtomicJsonFile(File(context.filesDir, "tasks.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private val stateLock = Any()

    private val _tasks = MutableStateFlow(readFromDisk(file, json))
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    fun add(task: Task) = mutate { it + task }

    fun update(task: Task) = mutate { list ->
        list.map { if (it.id == task.id) task else it }
    }

    /** Soft-delete: the task moves to the trash and can be restored. */
    fun trash(id: String): Task? {
        val updated = _tasks.value.firstOrNull { it.id == id }
            ?.copy(deletedAt = System.currentTimeMillis()) ?: return null
        update(updated)
        return updated
    }

    /** Brings a trashed task back. Returns it so callers can reschedule reminders. */
    fun restore(id: String): Task? {
        val updated = _tasks.value.firstOrNull { it.id == id }
            ?.copy(deletedAt = null) ?: return null
        update(updated)
        return updated
    }

    /** Permanently removes a task. Trash only — the caller has already confirmed. */
    fun purge(id: String) = mutate { list -> list.filterNot { it.id == id } }

    fun clearTrash() = mutate { list -> list.filterNot { it.isTrashed } }

    /** Permanently removes trashed tasks that have spent seven days in the bin. */
    fun pruneExpiredTrash(now: Long = System.currentTimeMillis()): Deferred<Unit>? {
        val cutoff = now - TRASH_RETENTION_MILLIS
        return mutate { list ->
            list.filterNot { task ->
                val deletedAt = task.deletedAt
                deletedAt != null && deletedAt <= cutoff
            }
        }
    }

    /** Prunes and waits until the resulting snapshot is durable. */
    suspend fun pruneExpiredTrashAndAwait(now: Long = System.currentTimeMillis()) {
        pruneExpiredTrash(now)?.await()
    }

    /**
     * Folds a backup into the current list, keeping the local copy whenever an id appears in
     * both. Merging rather than replacing means importing into an app that already has tasks
     * can't wipe them — and after a reinstall the list is empty, so the two amount to the same.
     *
     * @return how many tasks the import actually added.
     */
    fun merge(incoming: List<Task>): Int {
        return queueMerge(incoming).added
    }

    /** Merges on a worker thread and returns only after the imported snapshot reaches disk. */
    suspend fun mergeAndAwait(incoming: List<Task>): Int {
        val operation = withContext(Dispatchers.Default) { queueMerge(incoming) }
        operation.write?.await()
        return operation.added
    }

    /** Records that a reminder went out, so it is never delivered a second time. */
    fun markNotified(id: String, at: Long = System.currentTimeMillis()): Deferred<Unit>? =
        markNotified(setOf(id), at)

    /** Marks a catch-up batch with one state update and one disk write. */
    fun markNotified(ids: Set<String>, at: Long = System.currentTimeMillis()): Deferred<Unit>? {
        if (ids.isEmpty()) return null
        return mutate { list ->
            list.map { task ->
                if (task.id in ids && task.notifiedAt == null) task.copy(notifiedAt = at) else task
            }
        }
    }

    /** Returns the task as it looks after the toggle, so callers can (re)schedule reminders. */
    fun setDone(id: String, done: Boolean): Task? {
        val updated = _tasks.value.firstOrNull { it.id == id }?.copy(
            done = done,
            completedAt = if (done) System.currentTimeMillis() else null,
        ) ?: return null
        update(updated)
        return updated
    }

    private fun queueMerge(incoming: List<Task>): MergeOperation = synchronized(stateLock) {
        val current = _tasks.value
        val known = current.mapTo(mutableSetOf()) { it.id }
        val added = distinctMissingById(incoming, known) { it.id }
        val write = if (added.isEmpty()) null else persistLocked(current + added)
        MergeOperation(added = added.size, write = write)
    }

    private fun mutate(block: (List<Task>) -> List<Task>): Deferred<Unit>? =
        synchronized(stateLock) {
            val current = _tasks.value
            val next = block(current)
            if (next == current) null else persistLocked(next)
        }

    private fun persistLocked(next: List<Task>): Deferred<Unit> {
        _tasks.value = next
        return file.write { json.encodeToString(next) }
    }

    private data class MergeOperation(
        val added: Int,
        val write: Deferred<Unit>?,
    )

    companion object {
        const val TRASH_RETENTION_MILLIS = 7L * 24L * 60L * 60L * 1000L

        private fun readFromDisk(file: AtomicJsonFile, json: Json): List<Task> {
            val text = file.read() ?: return emptyList()
            return runCatching { json.decodeFromString<List<Task>>(text) }
                .getOrElse {
                    // Never let a damaged file quietly become "you have no tasks".
                    file.quarantine()
                    emptyList()
                }
        }
    }
}

/** Keeps the first incoming value for each new id and always preserves ids already stored. */
internal fun <T> distinctMissingById(
    incoming: List<T>,
    knownIds: Set<String>,
    idOf: (T) -> String,
): List<T> {
    val seen = knownIds.toMutableSet()
    return incoming.filter { seen.add(idOf(it)) }
}
