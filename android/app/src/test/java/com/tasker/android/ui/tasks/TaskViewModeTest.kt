package com.tasker.android.ui.tasks

import com.tasker.android.data.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskViewModeTest {
    @Test
    fun storedViewFallsBackToListWhenUnknown() {
        assertEquals(TaskViewMode.BOARD, TaskViewMode.fromStored("board"))
        assertEquals(TaskViewMode.LIST, TaskViewMode.fromStored("unknown"))
        assertEquals(TaskViewMode.LIST, TaskViewMode.fromStored(null))
    }

    @Test
    fun boardUsesAllStatusesAndRestoresThePreviousListFilter() {
        assertEquals("all", defaultTaskStatusFilter(TaskViewMode.BOARD))
        assertEquals("open", defaultTaskStatusFilter(TaskViewMode.TABLE))
        assertEquals(
            "all",
            taskStatusFilterAfterViewChange(
                currentView = TaskViewMode.LIST,
                nextView = TaskViewMode.BOARD,
                currentStatus = "completed",
                lastNonBoardStatus = "completed",
            ),
        )
        assertEquals(
            "completed",
            taskStatusFilterAfterViewChange(
                currentView = TaskViewMode.BOARD,
                nextView = TaskViewMode.LIST,
                currentStatus = "all",
                lastNonBoardStatus = "completed",
            ),
        )
    }

    @Test
    fun boardGroupsEveryTaskExactlyOnce() {
        val tasks = listOf(
            Task(id = "open", title = "Open task", status = "open"),
            Task(id = "done", title = "Done task", status = "completed"),
            Task(id = "legacy", title = "Legacy task", status = "archived"),
        )

        val columns = taskBoardColumns(tasks)
        val groupedIds = columns.flatMap { column -> column.tasks.map(Task::id) }

        assertEquals(listOf("open", "legacy"), columns.first().tasks.map(Task::id))
        assertEquals(listOf("done"), columns.last().tasks.map(Task::id))
        assertEquals(tasks.size, groupedIds.size)
        assertTrue(groupedIds.containsAll(tasks.map(Task::id)))
    }
}
