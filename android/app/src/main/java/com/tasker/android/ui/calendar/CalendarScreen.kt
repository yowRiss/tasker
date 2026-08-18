package com.tasker.android.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasker.android.ui.theme.TaskerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNoteClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { LocalDate.now() }

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val dayHeaderFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = monthFormatter.format(uiState.currentMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::jumpToToday) {
                        Icon(Icons.Rounded.Today, contentDescription = "Today", tint = colors.accent)
                    }
                    IconButton(onClick = viewModel::toggleViewMode) {
                        Icon(
                            imageVector = if (uiState.isAgendaView) Icons.Outlined.GridOn else Icons.Outlined.FormatListBulleted,
                            contentDescription = "Toggle View Mode",
                            tint = colors.textPrimary,
                        )
                    }
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous Month", tint = colors.textPrimary)
                    }
                    IconButton(onClick = viewModel::nextMonth) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next Month", tint = colors.textPrimary)
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
        ) {
            if (!uiState.isAgendaView) {
                // Days of week header (Sun -> Sat)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val daysOfWeek = remember {
                        listOf(
                            DayOfWeek.SUNDAY,
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY,
                            DayOfWeek.SATURDAY
                        )
                    }
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Month Calendar Grid
                val daysInGrid = remember(uiState.currentMonth) { getDaysInGrid(uiState.currentMonth) }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(daysInGrid) { date ->
                        val isCurrentMonth = date.month == uiState.currentMonth.month
                        val isSelected = date == uiState.selectedDate
                        val isToday = date == today
                        val dayItems = uiState.itemsByDate[date] ?: emptyList()
                        val hasNotes = dayItems.any { it is CalendarItem.NoteReminderItem }
                        val hasTasks = dayItems.any { it is CalendarItem.TaskItem }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isSelected -> colors.accent
                                        isToday -> colors.accentSubtle
                                        else -> colors.surface
                                    }
                                )
                                .border(
                                    width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                    color = if (isToday && !isSelected) colors.accent else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.selectDate(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> Color.White
                                        !isCurrentMonth -> colors.textTertiary.copy(alpha = 0.4f)
                                        else -> colors.textPrimary
                                    }
                                )

                                // Indicator Dots for Notes / Tasks
                                if (dayItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (hasNotes) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(
                                                        if (isSelected) Color.White else colors.accent,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                        if (hasTasks) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(
                                                        if (isSelected) Color.Yellow else Color(0xFFFF9800),
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(
                    color = colors.border,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // Agenda / Schedule List for Selected Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dayHeaderFormatter.format(uiState.selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Text(
                    text = "${uiState.selectedDateItems.size} scheduled",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }

            if (uiState.selectedDateItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Event,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No note reminders or tasks for this date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedDateItems, key = { it.id }) { item ->
                        when (item) {
                            is CalendarItem.NoteReminderItem -> {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNoteClick(item.noteId) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(colors.accentSubtle, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.NotificationsActive,
                                                contentDescription = "Note Reminder",
                                                tint = colors.accent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.contentMd.isNotBlank()) {
                                                Text(
                                                    text = item.contentMd.replace(Regex("[#*_`>-]"), " ").trim(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = colors.accentSubtle,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.displayTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.accent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            is CalendarItem.TaskItem -> {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTaskClick(item.taskId) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFFFFF3E0), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.PushPin,
                                                contentDescription = "Task Due",
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Status: ${item.status.replaceFirstChar { it.uppercase() }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = Color(0xFFFFF3E0),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.displayTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFE65100),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDaysInGrid(yearMonth: YearMonth): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstOfMonth.dayOfWeek
    // Calculate leading padding days to start on Sunday
    val paddingDays = firstDayOfWeek.value % 7

    val startDate = firstOfMonth.minusDays(paddingDays.toLong())
    val days = mutableListOf<LocalDate>()
    var curr = startDate
    for (i in 0 until 42) {
        days.add(curr)
        curr = curr.plusDays(1)
    }
    return days
}
