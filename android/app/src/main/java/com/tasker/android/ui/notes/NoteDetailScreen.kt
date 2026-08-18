package com.tasker.android.ui.notes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.tasker.android.data.model.NoteImage
import com.tasker.android.ui.components.ZoomableImageDialog
import com.tasker.android.ui.theme.TaskerTheme
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

private fun showDateTimePicker(context: Context, initialIso: String?, onSelected: (String) -> Unit) {
    val cal = Calendar.getInstance()
    if (!initialIso.isNullOrEmpty()) {
        try {
            val inst = Instant.parse(initialIso)
            cal.timeInMillis = inst.toEpochMilli()
        } catch (_: Exception) {}
    }

    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val iso = Instant.ofEpochMilli(selectedCal.timeInMillis).toString()
                    onSelected(iso)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatReminderDisplay(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "No reminder set"
    return try {
        val zdt = Instant.parse(isoString).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a").format(zdt)
    } catch (_: Exception) {
        isoString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String?,
    onBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var activeZoomImage by remember { mutableStateOf<NoteImage?>(null) }

    LaunchedEffect(noteId) {
        viewModel.initialize(noteId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.attachImage(it) }
    }

    activeZoomImage?.let { img ->
        val imageFile = remember(img.localUri, img.id) {
            val f = File(img.localUri)
            if (f.exists()) f
            else File(context.filesDir, "images/${img.id}.jpg")
        }
        val model = if (imageFile.exists()) imageFile else img.localUri
        ZoomableImageDialog(
            model = model,
            contentDescription = img.altText ?: "Note Image",
            onDismissRequest = { activeZoomImage = null },
            onDelete = {
                viewModel.deleteImage(img.id)
                activeZoomImage = null
            }
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else "Edit Note", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                },
                actions = {
                    // Preview / Edit toggle
                    IconButton(onClick = viewModel::togglePreviewMode) {
                        Icon(
                            imageVector = if (uiState.isPreviewMode) Icons.Outlined.Edit else Icons.Outlined.Visibility,
                            contentDescription = if (uiState.isPreviewMode) "Edit Mode" else "Preview Mode",
                            tint = colors.accent
                        )
                    }
                    if (!uiState.isPreviewMode) {
                        // Math insertion helper
                        IconButton(onClick = viewModel::insertMathTemplate) {
                            Icon(Icons.Outlined.Functions, "Insert Math Formula", tint = colors.accent)
                        }
                    }
                    // Photo picker
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Outlined.AddPhotoAlternate, "Attach Image", tint = colors.textPrimary)
                    }
                    if (noteId != null) {
                        IconButton(onClick = viewModel::deleteNote) {
                            Icon(Icons.Outlined.Delete, "Delete", tint = colors.destructive)
                        }
                    }
                    IconButton(onClick = viewModel::saveNote, enabled = !uiState.isLoading) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error banner
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

            // Note Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Note Title *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = taskerOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Google Calendar Style Reminder Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surfaceAlt),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.reminderAt != null) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = "Reminder",
                                tint = if (uiState.reminderAt != null) colors.accent else colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminder & Notification",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                        }

                        if (uiState.reminderAt != null) {
                            TextButton(onClick = viewModel::clearReminder) {
                                Text("Remove", color = colors.destructive)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatReminderDisplay(uiState.reminderAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.reminderAt != null) colors.accent else colors.textSecondary
                        )

                        Button(
                            onClick = {
                                showDateTimePicker(context, uiState.reminderAt) { iso ->
                                    viewModel.onReminderAtChange(iso)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.reminderAt != null) colors.accentSubtle else colors.accent,
                                contentColor = if (uiState.reminderAt != null) colors.accent else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (uiState.reminderAt != null) "Change" else "Set Reminder")
                        }
                    }

                    if (uiState.reminderAt != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Alert Timing (Google Calendar style):",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val presetOptions = listOf(
                            0 to "At time of event",
                            5 to "5m before",
                            10 to "10m before",
                            15 to "15m before",
                            30 to "30m before",
                            60 to "1h before",
                            1440 to "1d before"
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(presetOptions) { (offset, label) ->
                                val selected = uiState.reminderOffsets.contains(offset)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.toggleReminderOffset(offset) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accent,
                                        selectedLabelColor = Color.White,
                                        containerColor = colors.surface,
                                        labelColor = colors.textPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Attached Images Carousel
            if (uiState.images.isNotEmpty()) {
                Text("Images", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.images) { img ->
                        val imageFile = remember(img.localUri, img.id) {
                            val f = File(img.localUri)
                            if (f.exists()) f
                            else File(context.filesDir, "images/${img.id}.jpg")
                        }
                        val imageModel = if (imageFile.exists()) imageFile else img.localUri
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .size(110.dp)
                                .clickable { activeZoomImage = img }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageModel)
                                        .build(),
                                    contentDescription = img.altText ?: "Note Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { viewModel.deleteImage(img.id) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(26.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete Image",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (img.syncStatus == "pending") {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(topStart = 8.dp),
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    ) {
                                        Text(
                                            text = "Local",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Note Content Editor / Preview
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.isPreviewMode) {
                    // Preview Mode: render content with math and list formatting
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = NoteMathParser.formatMathAndMarkdown(uiState.contentMd),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary
                        )
                    }
                } else {
                    // Edit Mode: TextField for Markdown
                    OutlinedTextField(
                        value = uiState.contentMd,
                        onValueChange = viewModel::onContentChange,
                        placeholder = { Text("Write your markdown notes here... (Type '-' for auto list)", color = colors.textTertiary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun taskerOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TaskerTheme.colors.accent,
    unfocusedBorderColor = TaskerTheme.colors.border,
    focusedLabelColor = TaskerTheme.colors.accent,
    unfocusedLabelColor = TaskerTheme.colors.textTertiary,
    cursorColor = TaskerTheme.colors.accent,
    focusedContainerColor = TaskerTheme.colors.surfaceAlt,
    unfocusedContainerColor = TaskerTheme.colors.surfaceAlt,
)
