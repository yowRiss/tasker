package com.tasker.android.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material.icons.outlined.ViewKanban
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tasker.android.data.model.Task
import com.tasker.android.ui.theme.TaskerTheme

@Composable
fun TaskViewSelector(
    selected: TaskViewMode,
    onSelect: (TaskViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TaskerTheme.colors
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TaskViewMode.entries, key = { it.storedValue }) { mode ->
            val icon = when (mode) {
                TaskViewMode.LIST -> Icons.Outlined.ViewList
                TaskViewMode.BOARD -> Icons.Outlined.ViewKanban
                TaskViewMode.TABLE -> Icons.Outlined.TableRows
            }
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                label = { Text(mode.label) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.accentSubtle,
                    selectedLabelColor = colors.textPrimary,
                    selectedLeadingIconColor = colors.accent,
                    containerColor = colors.surfaceAlt,
                    labelColor = colors.textSecondary,
                    iconColor = colors.textSecondary,
                ),
            )
        }
    }
}

@Composable
fun TaskBoardView(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onMoveTask: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(taskBoardColumns(tasks), key = { it.status }) { column ->
            TaskBoardColumn(
                column = column,
                onTaskClick = onTaskClick,
                onMoveTask = onMoveTask,
            )
        }
    }
}

@Composable
private fun TaskBoardColumn(
    column: TaskBoardColumn,
    onTaskClick: (String) -> Unit,
    onMoveTask: (Task) -> Unit,
) {
    val colors = TaskerTheme.colors
    Card(
        modifier = Modifier
            .width(292.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceAlt),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                ) {
                    Text(
                        text = column.tasks.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            HorizontalDivider(color = colors.border)
            if (column.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        text = "No tasks in this group",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(column.tasks, key = { it.id }) { task ->
                        TaskBoardCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onMove = { onMoveTask(task) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskBoardCard(
    task: Task,
    onClick: () -> Unit,
    onMove: () -> Unit,
) {
    val colors = TaskerTheme.colors
    val isCompleted = task.status == "completed"
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .defaultMinSize(minHeight = 48.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                task.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TaskMetadata(task)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onMove,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Outlined.ViewKanban else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isCompleted) "Move to To do" else "Move to Done")
            }
        }
    }
}

@Composable
private fun TaskMetadata(task: Task) {
    val colors = TaskerTheme.colors
    if (task.project == null && task.dueDate == null && task.priority == 0) return
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        task.project?.let { project ->
            MetadataLine(Icons.Outlined.Folder, project.name)
        }
        task.dueDate?.let { dueDate ->
            MetadataLine(Icons.Outlined.CalendarToday, dueDate)
        }
        if (task.priority > 0) {
            Text(
                text = priorityLabel(task.priority),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun MetadataLine(
    icon: ImageVector,
    text: String,
) {
    val colors = TaskerTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TaskTableView(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TaskerTheme.colors
    val horizontalScrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState),
    ) {
        LazyColumn(
            modifier = Modifier
                .width(792.dp)
                .fillMaxHeight(),
        ) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TableCell("Name", 280.dp, FontWeight.SemiBold)
                    TableCell("Status", 112.dp, FontWeight.SemiBold)
                    TableCell("Project", 144.dp, FontWeight.SemiBold)
                    TableCell("Due", 120.dp, FontWeight.SemiBold)
                    TableCell("Priority", 104.dp, FontWeight.SemiBold)
                }
                HorizontalDivider(color = colors.border)
            }
            items(tasks, key = { it.id }) { task ->
                Row(
                    modifier = Modifier
                        .clickable { onTaskClick(task.id) }
                        .defaultMinSize(minHeight = 52.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TableCell(task.title, 280.dp)
                    TableCell(if (task.status == "completed") "Done" else "To do", 112.dp)
                    TableCell(task.project?.name ?: "—", 144.dp)
                    TableCell(task.dueDate ?: "—", 120.dp)
                    TableCell(priorityLabel(task.priority), 104.dp)
                }
                HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val colors = TaskerTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = fontWeight,
        color = colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width),
    )
}

private fun priorityLabel(priority: Int): String = when (priority) {
    1 -> "Low"
    2 -> "Medium"
    3 -> "High"
    else -> "—"
}
