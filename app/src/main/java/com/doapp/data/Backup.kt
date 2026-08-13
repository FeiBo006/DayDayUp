package com.doapp.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    fun write(
        context: Context,
        uri: Uri,
        tasks: List<Task>,
        sessions: List<FocusSession>,
    ): Boolean = runCatching {
        val payload = json.encodeToString(
            Backup(exportedAt = System.currentTimeMillis(), tasks = tasks, sessions = sessions)
        )
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(payload.toByteArray())
        } ?: return false
    }.isSuccess

    /** Returns the backup, or null when the file isn't one this app can read. */
    fun read(context: Context, uri: Uri): Backup? = runCatching {
        val text = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes().decodeToString() } ?: return null
        json.decodeFromString<Backup>(text)
    }.getOrNull()
}
