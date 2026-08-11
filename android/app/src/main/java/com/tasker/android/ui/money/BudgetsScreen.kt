package com.tasker.android.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.Budget
import com.tasker.android.data.model.Category
import com.tasker.android.data.repository.MoneyRepository
import com.tasker.android.ui.theme.TaskerTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
) : ViewModel() {
    val budgets: StateFlow<List<Budget>> = moneyRepository.observeBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = moneyRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createBudget(categoryId: String, amountLimit: Double) {
        val today = LocalDate.now()
        val start = today.withDayOfMonth(1).toString()
        val end = today.withDayOfMonth(today.lengthOfMonth()).toString()

        viewModelScope.launch {
            moneyRepository.createBudget(categoryId, start, end, amountLimit)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showNewBudgetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Budgets", color = colors.textPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = colors.textPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewBudgetDialog = true },
                containerColor = colors.accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) { Icon(Icons.Rounded.Add, contentDescription = "Create Budget") }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(colors.background)) {
            if (budgets.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.PieChart, null, tint = colors.textTertiary, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No active budgets", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(budgets, key = { it.id }) { budget ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(budget.category?.name ?: "Budget", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text(
                                        text = if (budget.isOverBudget) "Over Budget" else "${budget.percentUsed.toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (budget.isOverBudget) colors.destructive else colors.accent
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (budget.percentUsed / 100.0).toFloat().coerceIn(0f, 1f) },
                                    color = if (budget.isOverBudget) colors.destructive else colors.accent,
                                    trackColor = colors.surfaceAlt,
                                    modifier = Modifier.fillMaxWidth().height(8.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Spent: ${formatCurrency(budget.spent)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    Text("Limit: ${formatCurrency(budget.amountLimit)}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewBudgetDialog) {
        var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
        var limitText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewBudgetDialog = false },
            title = { Text("New Monthly Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Category", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(categories.filter { it.categoryType == "expense" }) { cat ->
                            FilterChip(
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = cat.id },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monthly Limit (IDR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val limit = limitText.toDoubleOrNull()
                    if (selectedCatId.isNotBlank() && limit != null && limit > 0) {
                        viewModel.createBudget(selectedCatId, limit)
                        showNewBudgetDialog = false
                    }
                }) { Text("Create", color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { showNewBudgetDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun formatCurrency(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
