package com.tasker.android.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
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
import com.tasker.android.data.model.Account
import com.tasker.android.data.model.Category
import com.tasker.android.data.model.CreateRecurringInput
import com.tasker.android.data.model.RecurringTransaction
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
class RecurringViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
) : ViewModel() {
    val recurring: StateFlow<List<RecurringTransaction>> = moneyRepository.observeRecurring()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = moneyRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = moneyRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createRecurring(input: CreateRecurringInput) {
        viewModelScope.launch { moneyRepository.createRecurring(input) }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch { moneyRepository.deleteRecurring(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val recurringList by viewModel.recurring.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var recurringPendingDelete by remember { mutableStateOf<RecurringTransaction?>(null) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Recurring Transactions", color = colors.textPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = colors.textPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = colors.accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create recurring transaction")
            }
        },
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
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rec.description ?: rec.category?.name ?: "Recurring", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${rec.cadence.capitalize()} • Next due ${rec.nextDueDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary,
                                    )
                                }
                                Text(
                                    text = "${if (rec.transactionType == "income") "+" else "-"}${formatCurrency(rec.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (rec.transactionType == "income") colors.success else colors.destructive,
                                )
                                IconButton(onClick = { recurringPendingDelete = rec }) {
                                    Icon(Icons.Outlined.Delete, "Delete recurring transaction", tint = colors.textTertiary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    recurringPendingDelete?.let { recurring ->
        AlertDialog(
            onDismissRequest = { recurringPendingDelete = null },
            title = { Text("Delete recurring transaction?") },
            text = { Text("Future entries will no longer be scheduled for ${recurring.description ?: recurring.category?.name ?: "this item"}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecurring(recurring.id)
                        recurringPendingDelete = null
                    },
                ) { Text("Delete", color = colors.destructive) }
            },
            dismissButton = {
                TextButton(onClick = { recurringPendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (showCreateDialog) {
        var transactionType by remember { mutableStateOf("expense") }
        var amountText by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
        var categoryId by remember { mutableStateOf("") }
        var cadence by remember { mutableStateOf("monthly") }
        var nextDueDate by remember { mutableStateOf(LocalDate.now().toString()) }
        val availableCategories = categories.filter { it.categoryType == transactionType }
        val amount = amountText.toDoubleOrNull()
        val validDate = runCatching { LocalDate.parse(nextDueDate) }.isSuccess
        val canCreate = amount != null && amount > 0 && accountId.isNotBlank() && categoryId.isNotBlank() && validDate

        LaunchedEffect(accounts) {
            if (accountId.isBlank()) accountId = accounts.firstOrNull()?.id.orEmpty()
        }
        LaunchedEffect(availableCategories) {
            if (availableCategories.none { it.id == categoryId }) {
                categoryId = availableCategories.firstOrNull()?.id.orEmpty()
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New recurring transaction") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("expense", "income")) { type ->
                            FilterChip(
                                selected = transactionType == type,
                                onClick = {
                                    transactionType = type
                                    categoryId = ""
                                },
                                label = { Text(type.capitalize()) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (IDR) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        singleLine = true,
                    )
                    Text("Account *", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(accounts) { account ->
                            FilterChip(
                                selected = accountId == account.id,
                                onClick = { accountId = account.id },
                                label = { Text(account.name) },
                            )
                        }
                    }
                    Text("Category *", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableCategories) { category ->
                            FilterChip(
                                selected = categoryId == category.id,
                                onClick = { categoryId = category.id },
                                label = { Text(category.name) },
                            )
                        }
                    }
                    Text("Cadence", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("weekly", "monthly", "yearly")) { option ->
                            FilterChip(
                                selected = cadence == option,
                                onClick = { cadence = option },
                                label = { Text(option.capitalize()) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = nextDueDate,
                        onValueChange = { nextDueDate = it },
                        label = { Text("Next due (YYYY-MM-DD) *") },
                        isError = !validDate,
                        supportingText = if (!validDate) {
                            { Text("Use YYYY-MM-DD format") }
                        } else {
                            null
                        },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canCreate,
                    onClick = {
                        viewModel.createRecurring(
                            CreateRecurringInput(
                                transactionType = transactionType,
                                amount = requireNotNull(amount),
                                accountId = accountId,
                                categoryId = categoryId,
                                description = description.ifBlank { null },
                                cadence = cadence,
                                nextDueDate = nextDueDate,
                            )
                        )
                        showCreateDialog = false
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            },
        )
    }
}

private fun formatCurrency(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)

private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
