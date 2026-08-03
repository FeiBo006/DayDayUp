package com.doapp.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The whole persistence layer: one JSON file, held in memory, rewritten on every mutation.
 * A todo list is a few hundred rows at most — a database would be ceremony without payoff.
 */
class TaskStore(context: Context) {

    private val file = File(context.filesDir, "tasks.json")
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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

    /** Returns the task as it looks after the toggle, so callers can (re)schedule reminders. */
    fun setDone(id: String, done: Boolean): Task? {
        val updated = _tasks.value.firstOrNull { it.id == id }?.copy(
            done = done,
            completedAt = if (done) System.currentTimeMillis() else null,
        ) ?: return null
        update(updated)
        return updated
    }

    fun clearCompleted(bucket: Bucket) = mutate { list ->
        list.filterNot { it.done && it.bucket == bucket }
    }

    private fun mutate(block: (List<Task>) -> List<Task>) {
        persist(block(_tasks.value))
    }

    private fun persist(next: List<Task>) {
        _tasks.value = next
        io.launch { runCatching { file.writeText(json.encodeToString(next)) } }
    }

    companion object {
        const val TRASH_RETENTION_MILLIS = 7L * 24L * 60L * 60L * 1000L

        /** Blocking read — used by the store itself and by the boot receiver. */
        fun readFromDisk(file: File, json: Json = Json { ignoreUnknownKeys = true }): List<Task> =
            runCatching {
                if (!file.exists()) emptyList()
                else json.decodeFromString<List<Task>>(file.readText())
            }.getOrDefault(emptyList())

        fun readFromDisk(context: Context): List<Task> =
            readFromDisk(File(context.filesDir, "tasks.json"))
    }
}
