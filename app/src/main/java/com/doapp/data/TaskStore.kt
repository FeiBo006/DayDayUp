package com.doapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

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
    fun pruneExpiredTrash(now: Long = System.currentTimeMillis()) {
        val cutoff = now - TRASH_RETENTION_MILLIS
        val next = _tasks.value.filterNot { task ->
            val deletedAt = task.deletedAt
            deletedAt != null && deletedAt <= cutoff
        }
        if (next != _tasks.value) persist(next)
    }

    /**
     * Folds a backup into the current list, keeping the local copy whenever an id appears in
     * both. Merging rather than replacing means importing into an app that already has tasks
     * can't wipe them — and after a reinstall the list is empty, so the two amount to the same.
     *
     * @return how many tasks the import actually added.
     */
    fun merge(incoming: List<Task>): Int {
        val known = _tasks.value.mapTo(mutableSetOf()) { it.id }
        val added = incoming.filterNot { it.id in known }
        if (added.isNotEmpty()) persist(_tasks.value + added)
        return added.size
    }

    /** Records that a reminder went out, so it is never delivered a second time. */
    fun markNotified(id: String, at: Long = System.currentTimeMillis()) = mutate { list ->
        list.map { if (it.id == id) it.copy(notifiedAt = at) else it }
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

    private fun mutate(block: (List<Task>) -> List<Task>) {
        persist(block(_tasks.value))
    }

    private fun persist(next: List<Task>) {
        _tasks.value = next
        file.write(json.encodeToString(next))
    }

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
