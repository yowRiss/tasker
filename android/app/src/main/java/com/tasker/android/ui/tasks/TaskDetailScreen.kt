package com.tasker.android.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasker.android.ui.theme.TaskerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String?,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val tags by viewModel.tags.collectAsState()

    LaunchedEffect(taskId) {
        viewModel.initialize(taskId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newSubtaskText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "New Task" else "Edit Task", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                },
                actions = {
                    if (taskId != null) {
                        IconButton(onClick = viewModel::deleteTask) {
                            Icon(Icons.Outlined.Delete, "Delete", tint = colors.destructive)
                        }
                    }
                    IconButton(onClick = viewModel::saveTask, enabled = !uiState.isLoading) {
                        Icon(Icons.Rounded.Check, "Save", tint = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Message
            uiState.errorMessage?.let { err ->
                Surface(
                    color = colors.destructiveSubtle,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = colors.destructive,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Title Field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Task Title *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = taskerOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Description Field
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = taskerOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Priority Selector
            Text("Priority", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(0 to "None", 1 to "Low", 2 to "Medium", 3 to "High").forEach { (level, name) ->
                    FilterChip(
                        selected = uiState.priority == level,
                        onClick = { viewModel.onPriorityChange(level) },
                        label = { Text(name) },
                        colors = filterChipColors()
                    )
                }
            }

            // Project Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Project", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                TextButton(onClick = { showNewProjectDialog = true }) {
                    Text("+ New Project", color = colors.accent)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.projectId == null,
                        onClick = { viewModel.onProjectChange(null) },
                        label = { Text("No Project") },
                        colors = filterChipColors()
                    )
                }
                items(projects) { proj ->
                    FilterChip(
                        selected = uiState.projectId == proj.id,
                        onClick = { viewModel.onProjectChange(proj.id) },
                        label = { Text(proj.name) },
                        colors = filterChipColors()
                    )
                }
            }

            // Tags Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tags", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                TextButton(onClick = { showNewTagDialog = true }) {
                    Text("+ New Tag", color = colors.accent)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    val isSelected = uiState.selectedTagIds.contains(tag.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleTagSelection(tag.id) },
                        label = { Text(tag.name) },
                        colors = filterChipColors()
                    )
                }
            }

            // Subtasks Section
            Text("Subtasks", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)

            uiState.subtasks.forEachIndexed { index, subtask ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = subtask.completed,
                        onCheckedChange = { viewModel.toggleSubtask(index) },
                        colors = CheckboxDefaults.colors(checkedColor = colors.accent)
                    )
                    Text(
                        text = subtask.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.removeSubtask(index) }) {
                        Icon(Icons.Outlined.Close, "Remove subtask", tint = colors.textTertiary)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add subtask...", color = colors.textTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = taskerOutlinedTextFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.addSubtask(newSubtaskText)
                        newSubtaskText = ""
                    }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.addSubtask(newSubtaskText)
                        newSubtaskText = ""
                    },
                    modifier = Modifier.background(colors.accentSubtle, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Outlined.Add, "Add", tint = colors.accent)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = viewModel::saveTask,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Save Task", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }

    // Dialog: New Project
    if (showNewProjectDialog) {
        var projName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("New Project") },
            text = {
                OutlinedTextField(
                    value = projName,
                    onValueChange = { projName = it },
                    label = { Text("Project Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createProject(projName)
                    showNewProjectDialog = false
                }) { Text("Create", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: New Tag
    if (showNewTagDialog) {
        var tagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text("New Tag") },
            text = {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createTag(tagName)
                    showNewTagDialog = false
                }) { Text("Create", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = TaskerTheme.colors.accentSubtle,
    selectedLabelColor = TaskerTheme.colors.accent,
    containerColor = TaskerTheme.colors.surfaceAlt,
    labelColor = TaskerTheme.colors.textSecondary
)
