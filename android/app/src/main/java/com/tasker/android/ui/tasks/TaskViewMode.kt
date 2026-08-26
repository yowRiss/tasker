package com.tasker.android.ui.tasks

import com.tasker.android.data.model.Task

enum class TaskViewMode(
    val storedValue: String,
    val label: String,
) {
    LIST("list", "List"),
    BOARD("board", "Board"),
    TABLE("table", "Table");

    companion object {
        fun fromStored(value: String?): TaskViewMode = entries.firstOrNull {
            it.storedValue == value
        } ?: LIST
    }
}

data class TaskBoardColumn(
    val status: String,
    val title: String,
    val tasks: List<Task>,
)

fun taskBoardColumns(tasks: List<Task>): List<TaskBoardColumn> = listOf(
    TaskBoardColumn(
        status = "open",
        title = "To do",
        tasks = tasks.filter { it.status != "completed" },
    ),
    TaskBoardColumn(
        status = "completed",
        title = "Done",
        tasks = tasks.filter { it.status == "completed" },
    ),
)

fun defaultTaskStatusFilter(viewMode: TaskViewMode): String = when (viewMode) {
    TaskViewMode.BOARD -> "all"
    TaskViewMode.LIST,
    TaskViewMode.TABLE -> "open"
}

fun taskStatusFilterAfterViewChange(
    currentView: TaskViewMode,
    nextView: TaskViewMode,
    currentStatus: String,
    lastNonBoardStatus: String,
): String = when {
    nextView == TaskViewMode.BOARD -> "all"
    currentView == TaskViewMode.BOARD -> lastNonBoardStatus
    else -> currentStatus
}
