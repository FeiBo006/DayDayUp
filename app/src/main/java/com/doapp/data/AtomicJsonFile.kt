package com.doapp.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicLong

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
    private val queuedVersion = AtomicLong(0L)
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

    /** Queues an already-built payload and returns a result that propagates write failures. */
    fun write(payload: String): Deferred<Unit> = write { payload }

    /**
     * Queues a lazily-built payload. Assigning the version before serialization preserves mutation
     * order while keeping JSON encoding off the caller thread. Awaiting the returned deferred means
     * a snapshot at least this new has reached disk; an I/O failure is rethrown to the awaiter.
     */
    fun write(payload: () -> String): Deferred<Unit> {
        val version = queuedVersion.incrementAndGet()
        return io.async {
            writeLock.withLock {
                if (version < writtenVersion) return@withLock
                writeAtomically(payload())
                writtenVersion = version
            }
        }
    }

    private fun writeAtomically(payload: String) {
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
