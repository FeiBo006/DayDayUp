package com.doapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskTest {

    @Test
    fun repeatRulesAdvanceFromTheCompletedOccurrence() {
        val day = LocalDate.of(2026, 8, 29)

        assertEquals(null, RepeatRule.NONE.nextDate(day))
        assertEquals(day.plusDays(1), RepeatRule.DAILY.nextDate(day))
        assertEquals(day.plusWeeks(1), RepeatRule.WEEKLY.nextDate(day))
        assertEquals(day.plusMonths(1), RepeatRule.MONTHLY.nextDate(day))
    }

    @Test
    fun reminderMustStillBeCurrentAndDue() {
        val due = Task(id = "due", title = "交报告", reminderAt = 1_000L)

        assertFalse(due.isReminderDue(999L))
        assertTrue(due.isReminderDue(1_000L))
        assertFalse(due.copy(done = true).isReminderDue(2_000L))
        assertFalse(due.copy(deletedAt = 500L).isReminderDue(2_000L))
        assertFalse(due.copy(notifiedAt = 1_100L).isReminderDue(2_000L))
        assertFalse(due.copy(reminderAt = null).isReminderDue(2_000L))
    }

    @Test
    fun backupRejectsUnknownFutureFormat() {
        val supported = Backup(version = 1, exportedAt = 0L, tasks = emptyList())
        val future = supported.copy(version = Backup.FORMAT_VERSION + 1)

        assertTrue(BackupFile.isSupported(supported))
        assertFalse(BackupFile.isSupported(future))
    }

    @Test
    fun backupNormalizesDuplicateIdsWithFirstCopyWinning() {
        val first = Task(id = "same", title = "first")
        val duplicate = first.copy(title = "duplicate")
        val normalized = BackupFile.normalizeForImport(
            Backup(exportedAt = 0L, tasks = listOf(first, duplicate))
        )

        assertNotNull(normalized)
        assertEquals(listOf(first), normalized?.tasks)
    }

    @Test
    fun backupCapsCombinedEntryCount() {
        val atLimit = Backup(
            exportedAt = 0L,
            tasks = List(BackupFile.MAX_BACKUP_ENTRIES.toInt()) { index ->
                Task(id = index.toString(), title = "task")
            },
        )

        assertTrue(BackupFile.isSupported(atLimit))
        assertNull(
            BackupFile.normalizeForImport(
                atLimit.copy(
                    sessions = listOf(FocusSession(label = "focus", startedAt = 0L, endedAt = 1L))
                )
            )
        )
    }
}
