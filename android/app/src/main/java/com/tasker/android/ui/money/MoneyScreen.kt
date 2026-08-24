package com.tasker.android.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasker.android.data.model.Transaction
import com.tasker.android.ui.navigation.Screen
import com.tasker.android.ui.theme.TaskerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    onNavigate: (String) -> Unit,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showNewAccountDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var transactionPendingDelete by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Money", style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary) },
                actions = {
                    TextButton(onClick = { onNavigate(Screen.Targets.route) }) {
                        Text("Targets", color = colors.accent)
                    }
                    TextButton(onClick = { onNavigate(Screen.Budgets.route) }) {
                        Text("Budgets", color = colors.accent)
                    }
                    TextButton(onClick = { onNavigate(Screen.Recurring.route) }) {
                        Text("Recurring", color = colors.accent)
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(Screen.CreateTransaction.route) },
                containerColor = colors.accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Transaction")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Balance", style = MaterialTheme.typography.labelMedium, color = colors.textTertiary)
                        Text(
                            text = formatCurrency(dashboardData.totalBalance),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${dashboardData.periodLabel} cash flow • ${dashboardData.transactionCount} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SummaryMetric(
                                label = "Income",
                                amount = dashboardData.totalIncome,
                                color = colors.success,
                                icon = { Icon(Icons.Outlined.ArrowUpward, null, tint = colors.success, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f),
                            )
                            SummaryMetric(
                                label = "Expenses",
                                amount = dashboardData.totalExpense,
                                color = colors.destructive,
                                icon = { Icon(Icons.Outlined.ArrowDownward, null, tint = colors.destructive, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val cashFlowColor = if (dashboardData.netCashFlow >= 0) colors.success else colors.destructive
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Net cash flow", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                            Text(
                                text = formatSignedCurrency(dashboardData.netCashFlow),
                                style = MaterialTheme.typography.titleMedium,
                                color = cashFlowColor,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reporting period", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MoneyPeriod.entries) { period ->
                            FilterChip(
                                selected = uiState.selectedPeriod == period,
                                onClick = { viewModel.filterByPeriod(period) },
                                label = { Text(period.label) },
                                colors = filterChipColors(),
                            )
                        }
                    }
                }
            }

            // Accounts Carousel
            item {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Accounts", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        TextButton(onClick = { showNewAccountDialog = true }) {
                            Text("+ New Account", color = colors.accent)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.selectedAccountId == null,
                                onClick = { viewModel.filterByAccount(null) },
                                label = { Text("All Accounts") },
                                colors = filterChipColors()
                            )
                        }
                        items(accounts) { acc ->
                            FilterChip(
                                selected = uiState.selectedAccountId == acc.id,
                                onClick = { viewModel.filterByAccount(if (uiState.selectedAccountId == acc.id) null else acc.id) },
                                label = { Text("${acc.name} (${formatCurrency(acc.balance)})") },
                                colors = filterChipColors()
                            )
                        }
                    }
                }
            }

            // Category Spend Canvas Bar Chart
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Categories", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        TextButton(onClick = { showNewCategoryDialog = true }) {
                            Text("+ New Category", color = colors.accent)
                        }
                    }
                    if (categories.isEmpty()) {
                        Text(
                            "Add income and expense categories to organize transactions and budgets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { category ->
                                AssistChip(
                                    onClick = { viewModel.filterByCategory(category.id) },
                                    label = { Text(category.name) },
                                )
                            }
                        }
                    }
                }
            }

            if (dashboardData.categorySpend.isNotEmpty()) {
                item {
                    Text("Category Spending", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            CategorySpendCanvasChart(
                                items = dashboardData.categorySpend,
                                accentColor = colors.accent
                            )
                        }
                    }
                }
            }

            // Transactions Header
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search transactions") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (uiState.query.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(12.dp),
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Transactions", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        if (uiState.hasTransactionFilters) {
                            TextButton(onClick = viewModel::clearTransactionFilters) {
                                Text("Clear filters", color = colors.accent)
                            }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.selectedType == null,
                                onClick = { viewModel.filterByType(null) },
                                label = { Text("All types") },
                                colors = filterChipColors(),
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedType == "income",
                                onClick = { viewModel.filterByType("income") },
                                label = { Text("Income") },
                                colors = filterChipColors(),
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedType == "expense",
                                onClick = { viewModel.filterByType("expense") },
                                label = { Text("Expense") },
                                colors = filterChipColors(),
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedType == "transfer",
                                onClick = { viewModel.filterByType("transfer") },
                                label = { Text("Transfer") },
                                colors = filterChipColors(),
                            )
                        }
                    }

                    if (categories.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedCategoryId == null,
                                    onClick = { viewModel.filterByCategory(null) },
                                    label = { Text("All categories") },
                                    colors = filterChipColors(),
                                )
                            }
                            items(categories) { category ->
                                FilterChip(
                                    selected = uiState.selectedCategoryId == category.id,
                                    onClick = {
                                        viewModel.filterByCategory(
                                            if (uiState.selectedCategoryId == category.id) null else category.id
                                        )
                                    },
                                    label = { Text(category.name) },
                                    colors = filterChipColors(),
                                )
                            }
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("No matching transactions", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                        Text(
                            "Try another period or clear the transaction filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                        )
                        if (uiState.hasTransactionFilters) {
                            TextButton(onClick = viewModel::clearTransactionFilters) {
                                Text("Clear filters", color = colors.accent)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(transactions, key = { _, transaction -> transaction.id }) { index, transaction ->
                    if (index == 0 || transactions[index - 1].transactionDate != transaction.transactionDate) {
                        Text(
                            text = formatTransactionDate(transaction.transactionDate),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp),
                        )
                    }
                    TransactionCard(
                        transaction = transaction,
                        onClick = { onNavigate(Screen.TransactionDetail.route(transaction.id)) },
                        onDelete = { transactionPendingDelete = transaction },
                    )
                }
            }
        }
    }

    transactionPendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionPendingDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("This removes ${transaction.description ?: formatCurrency(transaction.amount)} from your local history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction.id)
                        transactionPendingDelete = null
                    },
                ) {
                    Text("Delete", color = colors.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionPendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    // Dialog: New Account
    if (showNewAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accType by remember { mutableStateOf("bank") }
        AlertDialog(
            onDismissRequest = { showNewAccountDialog = false },
            title = { Text("New Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text("Account Name") },
                        singleLine = true
                    )
                    Text("Type", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(listOf("bank", "cash", "e_wallet", "credit_card")) { type ->
                            FilterChip(
                                selected = accType == type,
                                onClick = { accType = type },
                                label = { Text(type.replace("_", " ")) },
                                colors = filterChipColors()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = accName.isNotBlank(),
                    onClick = {
                        viewModel.createAccount(accName, accType)
                        showNewAccountDialog = false
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewAccountDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        var categoryType by remember { mutableStateOf("expense") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("New category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Category name") },
                        singleLine = true,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("expense", "income")) { type ->
                            FilterChip(
                                selected = categoryType == type,
                                onClick = { categoryType = type },
                                label = { Text(type.capitalizeLabel()) },
                                colors = filterChipColors(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = categoryName.isNotBlank(),
                    onClick = {
                        viewModel.createCategory(categoryName, categoryType)
                        showNewCategoryDialog = false
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) { Text("Cancel") }
            },
        )
    }
}
