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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

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

    fun add(task: Task) = mutate { list ->
        list + task.withInsertDefaults(list, System.currentTimeMillis())
    }

    fun update(task: Task) = mutate { list ->
        list.map { stored ->
            if (stored.id == task.id) {
                task.withUpdateDefaults(stored, list, System.currentTimeMillis())
            } else {
                stored
            }
        }
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

    /** Returns affected tasks as they look after the toggle, so callers can sync reminders. */
    fun setDone(id: String, done: Boolean): List<Task> = synchronized(stateLock) {
        val current = _tasks.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized emptyList()

        val now = System.currentTimeMillis()
        val stored = current[index]
        if (stored.done == done) return@synchronized emptyList()
        val updated = stored.copy(
            done = done,
            completedAt = if (done) now else null,
        )
        val next = current.toMutableList()
        next[index] = updated

        val affected = mutableListOf(updated)
        if (done && !stored.done) {
            nextRepeatOccurrence(stored, now)?.let { occurrence ->
                val ordered = occurrence.withInsertDefaults(next, now)
                next += ordered
                affected += ordered
            }
        }

        if (next == current) emptyList() else {
            persistLocked(next)
            affected
        }
    }

    fun moveToday(id: String, direction: Int, todayEpochDay: Long): List<Task> =
        synchronized(stateLock) {
            if (direction == 0) return@synchronized emptyList()
            val current = _tasks.value
            val todayOpen = orderedTodayOpen(current, todayEpochDay)
            val from = todayOpen.indexOfFirst { it.id == id }
            if (from < 0) return@synchronized emptyList()
            val to = (from + direction).coerceIn(0, todayOpen.lastIndex)
            if (from == to) return@synchronized emptyList()

            val reordered = todayOpen.toMutableList().apply {
                add(to, removeAt(from))
            }
            val orderById = reordered.mapIndexed { index, task ->
                task.id to ((index + 1L) * ORDER_STEP)
            }.toMap()

            val affected = mutableListOf<Task>()
            val next = current.map { task ->
                val order = orderById[task.id]
                if (order == null) {
                    task
                } else {
                    task.copy(bucket = Bucket.TODAY, planDay = null, todayOrder = order)
                        .also { affected += it }
                }
            }
            persistLocked(next)
            affected
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
        private const val ORDER_STEP = 1_000L

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

private fun Task.withInsertDefaults(current: List<Task>, now: Long): Task {
    val created = if (createdAt > 0L) createdAt else now
    val order = if (bucket == Bucket.TODAY && todayOrder <= 0L) nextTodayOrder(current) else todayOrder
    return copy(createdAt = created, todayOrder = order)
}

private fun Task.withUpdateDefaults(stored: Task, current: List<Task>, now: Long): Task {
    val created = if (createdAt > 0L) createdAt else stored.createdAt.takeIf { it > 0L } ?: now
    val order = when {
        bucket != Bucket.TODAY -> todayOrder
        todayOrder > 0L -> todayOrder
        stored.todayOrder > 0L -> stored.todayOrder
        else -> nextTodayOrder(current)
    }
    return copy(createdAt = created, todayOrder = order)
}

private fun nextTodayOrder(tasks: List<Task>): Long {
    val maxOrder = tasks
        .filter { it.bucket == Bucket.TODAY && !it.isTrashed && !it.done }
        .maxOfOrNull { it.todayOrder.takeIf { order -> order > 0L } ?: 0L }
        ?: 0L
    return maxOrder + 1_000L
}

private fun orderedTodayOpen(tasks: List<Task>, todayEpochDay: Long): List<Task> =
    tasks.filter { task ->
        !task.isTrashed &&
            !task.done &&
            (task.bucket == Bucket.TODAY || (task.planDay != null && task.planDay <= todayEpochDay))
    }.sortedWith(compareBy<Task> { it.todaySortKey() }.thenBy { it.createdAt })

private fun nextRepeatOccurrence(task: Task, now: Long): Task? {
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    val scheduled = task.planDay?.let(LocalDate::ofEpochDay)
    val baseDate = if (scheduled != null && scheduled.isAfter(today)) scheduled else today
    val nextDate = task.repeatRule.nextDate(baseDate) ?: return null
    return task.copy(
        id = UUID.randomUUID().toString(),
        done = false,
        completedAt = null,
        deletedAt = null,
        notifiedAt = null,
        createdAt = now,
        bucket = Bucket.LATER,
        planDay = nextDate.toEpochDay(),
        todayOrder = 0L,
        reminderAt = task.reminderAt?.atDate(nextDate),
    )
}

private fun Long.atDate(date: LocalDate): Long {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(this).atZone(zone).toLocalTime()
    return LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
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
