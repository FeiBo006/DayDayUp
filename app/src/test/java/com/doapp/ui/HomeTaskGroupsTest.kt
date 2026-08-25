package com.doapp.ui

import com.doapp.data.Bucket
import com.doapp.data.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTaskGroupsTest {

    @Test
    fun groupsOnceWithPromotionOrderingAndTrashRules() {
        val tasks = listOf(
            task("today", Bucket.TODAY),
            task("due", Bucket.LATER, planDay = 100L),
            task("future", Bucket.LATER, planDay = 102L),
            task("unscheduled", Bucket.LATER),
            task("done-old", Bucket.TODAY, done = true, completedAt = 10L),
            task("done-new", Bucket.TODAY, done = true, completedAt = 20L),
            task("trashed", Bucket.TODAY, deletedAt = 1L),
        )

        val grouped = groupHomeTasks(tasks, todayEpochDay = 100L)

        assertEquals(listOf("today", "due"), grouped.todayOpen.map { it.id })
        assertEquals(listOf("done-new", "done-old"), grouped.todayDone.map { it.id })
        assertEquals(listOf("future", "unscheduled"), grouped.planOpen.map { it.id })
        assertEquals(emptyList<Task>(), grouped.planDone)
    }

    private fun task(
        id: String,
        bucket: Bucket,
        done: Boolean = false,
        planDay: Long? = null,
        completedAt: Long? = null,
        deletedAt: Long? = null,
    ) = Task(
        id = id,
        title = id,
        bucket = bucket,
        done = done,
        planDay = planDay,
        completedAt = completedAt,
        deletedAt = deletedAt,
    )
}
