package com.doapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * A portable copy of the task list. Wrapped in an envelope rather than dumping the raw array so
 * a future format change has somewhere to announce itself.
 */
@Serializable
data class Backup(
    val version: Int = FORMAT_VERSION,
    val exportedAt: Long,
    val tasks: List<Task>,
    /** Added in format 2. Defaulted so a format-1 file still reads. */
    val sessions: List<FocusSession> = emptyList(),
) {
    companion object {
        const val FORMAT_VERSION = 2
    }
}

/**
 * Reads and writes backups through the document picker. Going through a user-chosen Uri is the
 * point: the file lands somewhere the app doesn't own, so uninstalling can't take it along.
 */
object BackupFile {

    const val MIME_TYPE = "application/json"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun write(
        context: Context,
        uri: Uri,
        tasks: List<Task>,
        sessions: List<FocusSession>,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = json.encodeToString(
                Backup(exportedAt = System.currentTimeMillis(), tasks = tasks, sessions = sessions)
            )
            val output = context.contentResolver.openOutputStream(uri)
                ?: return@withContext false
            output.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

    /** Returns the backup, or null when the file isn't one this app can read. */
    suspend fun read(context: Context, uri: Uri): Backup? = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext null
            val text = input.use(::readTextWithLimit) ?: return@withContext null
            normalizeForImport(json.decodeFromString<Backup>(text))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Applies a validated backup and does not report success until both store snapshots are on disk.
     * Existing synchronous merge APIs remain available for lightweight UI mutations.
     */
    suspend fun importInto(
        backup: Backup,
        tasks: TaskStore,
        focus: FocusStore,
    ): Int? = try {
        val safe = normalizeForImport(backup) ?: return null
        val addedTasks = tasks.mergeAndAwait(safe.tasks)
        val addedSessions = focus.mergeAndAwait(safe.sessions)
        addedTasks + addedSessions
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    /** A corrupt or hostile document must not be able to allocate the process out of memory. */
    private fun readTextWithLimit(input: InputStream): String? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_BACKUP_BYTES) return null
            output.write(buffer, 0, count)
        }
        return output.toByteArray().toString(Charsets.UTF_8)
    }

    internal fun isSupported(backup: Backup): Boolean =
        backup.version in 1..Backup.FORMAT_VERSION &&
            backup.tasks.size.toLong() + backup.sessions.size <= MAX_BACKUP_ENTRIES

    /** Normalizes untrusted input before any store sees it. The first copy of an id wins. */
    internal fun normalizeForImport(backup: Backup): Backup? {
        if (!isSupported(backup)) return null
        return backup.copy(
            tasks = distinctMissingById(backup.tasks, emptySet()) { it.id },
            sessions = distinctMissingById(backup.sessions, emptySet()) { it.id },
        )
    }

    internal const val MAX_BACKUP_BYTES = 5 * 1024 * 1024
    internal const val MAX_BACKUP_ENTRIES = 5_000L
}
