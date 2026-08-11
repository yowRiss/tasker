package com.tasker.android.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasker.android.data.model.Project
import com.tasker.android.data.model.Task
import com.tasker.android.ui.theme.TaskerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onTaskClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val tasks by viewModel.tasks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val filters by viewModel.filters.collectAsState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search tasks...", color = colors.textTertiary) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.textSecondary) },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Outlined.Clear, "Clear", tint = colors.textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.surfaceAlt,
                        unfocusedContainerColor = colors.surfaceAlt,
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status filters
                    item {
                        FilterChip(
                            selected = filters.status == "open",
                            onClick = { viewModel.setStatusFilter("open") },
                            label = { Text("Open") },
                            colors = filterChipColors()
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.status == "completed",
                            onClick = { viewModel.setStatusFilter("completed") },
                            label = { Text("Completed") },
                            colors = filterChipColors()
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.status == "all",
                            onClick = { viewModel.setStatusFilter("all") },
                            label = { Text("All") },
                            colors = filterChipColors()
                        )
                    }

                    // Project filter chips
                    items(projects) { proj ->
                        FilterChip(
                            selected = filters.projectId == proj.id,
                            onClick = {
                                viewModel.setProjectFilter(
                                    if (filters.projectId == proj.id) null else proj.id
                                )
                            },
                            label = { Text(proj.name) },
                            colors = filterChipColors()
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = colors.accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Task")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
        ) {
            if (tasks.isEmpty()) {
                EmptyTasksView()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = TaskerTheme.colors
    val isCompleted = task.status == "completed"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(28.dp).offset(y = (-2).dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "Mark incomplete" else "Mark complete",
                    tint = if (isCompleted) colors.accent else colors.textTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) colors.textTertiary else colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!task.description.isNull_or_blank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata row (Project, Priority, Due Date, Subtasks)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Badge
                    if (task.priority > 0) {
                        val (pColor, pText) = when (task.priority) {
                            1 -> colors.accent to "Low"
                            2 -> colors.warning to "Medium"
                            3 -> colors.destructive to "High"
                            else -> colors.textTertiary to ""
                        }
                        Surface(
                            color = pColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = pText,
                                color = pColor,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Project Chip
                    task.project?.let { proj ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Folder,
                                null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = proj.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textTertiary
                            )
                        }
                    }

                    // Due Date
                    task.dueDate?.let { dueDate ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CalendarToday,
                                null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = dueDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textTertiary
                            )
                        }
                    }

                    // Subtasks count
                    if (task.subtasks.isNotEmpty()) {
                        val completedCount = task.subtasks.count { it.completed }
                        Text(
                            text = "$completedCount/${task.subtasks.size} subtasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary
                        )
                    }
                }
            }

            // Delete action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete task",
                    tint = colors.textTertiary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyTasksView() {
    val colors = TaskerTheme.colors
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = colors.textTertiary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tasks found",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap + to create a new task",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = TaskerTheme.colors.accentSubtle,
    selectedLabelColor = TaskerTheme.colors.accent,
    containerColor = TaskerTheme.colors.surfaceAlt,
    labelColor = TaskerTheme.colors.textSecondary
)

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
