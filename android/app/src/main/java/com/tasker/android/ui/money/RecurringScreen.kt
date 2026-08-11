package com.tasker.android.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.RecurringTransaction
import com.tasker.android.data.repository.MoneyRepository
import com.tasker.android.ui.theme.TaskerTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
) : ViewModel() {
    val recurring: StateFlow<List<RecurringTransaction>> = moneyRepository.observeRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val recurringList by viewModel.recurring.collectAsState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Recurring Transactions", color = colors.textPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = colors.textPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(colors.background)) {
            if (recurringList.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Repeat, null, tint = colors.textTertiary, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No recurring transactions", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recurringList, key = { it.id }) { rec ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(rec.description ?: rec.category?.name ?: "Recurring", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text(formatCurrency(rec.amount), style = MaterialTheme.typography.titleMedium, color = colors.accent)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Cadence: ${rec.cadence.capitalize()} • Next Due: ${rec.nextDueDate}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)

private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
