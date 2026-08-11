package com.tasker.android.ui.money

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasker.android.data.model.Account
import com.tasker.android.data.model.CategorySpendItem
import com.tasker.android.data.model.Transaction
import com.tasker.android.ui.navigation.Screen
import com.tasker.android.ui.theme.TaskerTheme
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    onNavigate: (String) -> Unit,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showNewAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Money", style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary) },
                actions = {
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ArrowUpward, null, tint = colors.success, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Text("Income", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
                                    Text(formatCurrency(dashboardData.totalIncome), style = MaterialTheme.typography.titleSmall, color = colors.success)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ArrowDownward, null, tint = colors.destructive, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Text("Expenses", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
                                    Text(formatCurrency(dashboardData.totalExpense), style = MaterialTheme.typography.titleSmall, color = colors.destructive)
                                }
                            }
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
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Transactions", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.selectedType == null,
                                onClick = { viewModel.filterByType(null) },
                                label = { Text("All") },
                                colors = filterChipColors()
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedType == "income",
                                onClick = { viewModel.filterByType("income") },
                                label = { Text("Income") },
                                colors = filterChipColors()
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.selectedType == "expense",
                                onClick = { viewModel.filterByType("expense") },
                                label = { Text("Expense") },
                                colors = filterChipColors()
                            )
                        }
                    }
                }
            }

            // Transaction List
            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions found", style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        onDelete = { viewModel.deleteTransaction(tx.id) }
                    )
                }
            }
        }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("bank", "cash", "e_wallet", "credit_card").forEach { type ->
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
                TextButton(onClick = {
                    viewModel.createAccount(accName, accType)
                    showNewAccountDialog = false
                }) { Text("Create", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showNewAccountDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    onDelete: () -> Unit,
) {
    val colors = TaskerTheme.colors
    val (txColor, txSign) = when (transaction.transactionType) {
        "income" -> colors.success to "+"
        "expense" -> colors.destructive to "-"
        else -> colors.textPrimary to ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (transaction.transactionType) {
                    "income" -> Icons.Outlined.ArrowUpward
                    "expense" -> Icons.Outlined.ArrowDownward
                    else -> Icons.Outlined.AccountBalance
                },
                contentDescription = null,
                tint = txColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: transaction.category?.name ?: transaction.transactionType.capitalize(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.account?.name ?: "Account"} • ${transaction.transactionDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary
                )
            }

            if (transaction.receipt != null) {
                Icon(
                    Icons.Outlined.Receipt,
                    contentDescription = "Receipt attached",
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                )
            }

            Text(
                text = "$txSign${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = txColor
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                Icon(Icons.Outlined.Delete, "Delete", tint = colors.textTertiary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CategorySpendCanvasChart(
    items: List<CategorySpendItem>,
    accentColor: Color
) {
    val colors = TaskerTheme.colors
    val maxAmount = items.maxOfOrNull { it.amount } ?: 1.0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.take(5).forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.width(90.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Canvas(modifier = Modifier.weight(1f).height(12.dp)) {
                    val progress = (item.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                    drawRoundRect(
                        color = colors.surfaceAlt,
                        size = Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6dp.toPx())
                    )
                    drawRoundRect(
                        color = accentColor,
                        size = Size(size.width * progress, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6dp.toPx())
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatCurrency(item.amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textPrimary
                )
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount)
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = TaskerTheme.colors.accentSubtle,
    selectedLabelColor = TaskerTheme.colors.accent,
    containerColor = TaskerTheme.colors.surfaceAlt,
    labelColor = TaskerTheme.colors.textSecondary
)

private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
