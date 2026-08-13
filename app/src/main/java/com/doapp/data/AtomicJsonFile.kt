package com.doapp.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * One JSON file on disk, written safely. Both stores in the app are this shape, and the care is
 * all in the write path, so the write path lives here once.
 *
 * Two things it guarantees:
 *
 * - **Ordering.** Writes are serialized, and a snapshot that has been overtaken by a newer one is
 *   dropped rather than landing late and resurrecting stale state.
 * - **Atomicity.** `writeText` truncates before it writes, so a crash mid-write leaves JSON that
 *   parses as nothing at all — which reads to the user as the app eating everything. Writing to a
 *   temp file and renaming means the real file is either the old content or the new content.
 */
internal class AtomicJsonFile(private val file: File) {

    private val tempFile = File(file.parentFile, "${file.name}.tmp")
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()
    private var queuedVersion = 0L
    private var writtenVersion = 0L

    /** The file's contents, or null when it isn't there. */
    fun read(): String? =
        if (!file.exists()) null else runCatching { file.readText() }.getOrNull()

    /**
     * Sets a damaged file aside instead of letting it be silently overwritten, so the bytes can
     * still be recovered by hand. Call after a parse failure.
     */
    fun quarantine() {
        runCatching { file.renameTo(File(file.parentFile, "${file.name}.corrupt")) }
    }

    /** Queues a write. Callers are on the main thread, so the counter needs no synchronization. */
    fun write(payload: String) {
        val version = ++queuedVersion
        io.launch {
            writeLock.withLock {
                if (version < writtenVersion) return@withLock
                writtenVersion = version
                writeAtomically(payload)
            }
        }
    }

    private fun writeAtomically(payload: String) {
        runCatching {
            tempFile.writeText(payload)
            runCatching {
                Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            }.recoverCatching {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }.getOrThrow()
        }
    }
}
