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
import androidx.compose.material.icons.outlined.Flag
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
import com.tasker.android.data.model.CreateTargetInput
import com.tasker.android.data.model.Target
import com.tasker.android.data.repository.MoneyRepository
import com.tasker.android.ui.theme.TaskerTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TargetsViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
) : ViewModel() {
    val targets: StateFlow<List<Target>> = moneyRepository.observeTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = moneyRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = moneyRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTarget(
        name: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: String?,
        categoryId: String?,
        accountId: String?,
        color: String?,
        notes: String?,
    ) {
        viewModelScope.launch {
            moneyRepository.createTarget(
                CreateTargetInput(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDate = targetDate,
                    categoryId = categoryId,
                    accountId = accountId,
                    color = color,
                    notes = notes,
                )
            )
        }
    }

    fun contributeTarget(id: String, amount: Double, isWithdraw: Boolean) {
        viewModelScope.launch {
            moneyRepository.contributeTarget(id, amount, isWithdraw)
        }
    }

    fun deleteTarget(id: String) {
        viewModelScope.launch { moneyRepository.deleteTarget(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsScreen(
    onBack: () -> Unit,
    viewModel: TargetsViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val targets by viewModel.targets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showNewTargetDialog by remember { mutableStateOf(false) }
    var targetPendingContribute by remember { mutableStateOf<Target?>(null) }
    var targetPendingDelete by remember { mutableStateOf<Target?>(null) }

    val activeCount = targets.count { it.status == "active" }
    val achievedCount = targets.count { it.isAchieved || it.status == "achieved" }
    val totalSaved = targets.sumOf { it.currentAmount }
    val totalGoal = targets.sumOf { it.targetAmount }
    val overallPercent = if (totalGoal > 0) ((totalSaved / totalGoal) * 100).toInt().coerceIn(0, 100) else 0

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Savings Targets", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTargetDialog = true },
                containerColor = colors.accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) { Icon(Icons.Rounded.Add, contentDescription = "Create Target") }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(colors.background)) {
            if (targets.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Flag, null, tint = colors.textTertiary, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No active savings targets", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to set a financial goal", style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Total Saved Toward Goals", style = MaterialTheme.typography.labelMedium, color = colors.textTertiary)
                                Text(
                                    text = formatCurrency(totalSaved),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = colors.accent
                                )
                                Text(
                                    text = "Goal: ${formatCurrency(totalGoal)} • $activeCount active, $achievedCount achieved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Spacer(Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { (overallPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = if (overallPercent >= 100) colors.success else colors.accent,
                                    trackColor = colors.surfaceAlt,
                                )

                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$overallPercent% Complete", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    Text("Remaining: ${formatCurrency(maxOf(0.0, totalGoal - totalSaved))}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                        }
                    }

                    // Target items
                    items(targets, key = { it.id }) { target ->
                        val isAchieved = target.isAchieved || target.status == "achieved"
                        val pct = target.progressPercent.toInt().coerceIn(0, 100)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = target.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.textPrimary
                                        )
                                        if (target.targetDate != null) {
                                            Text(
                                                text = "Target Date: ${target.targetDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textTertiary
                                            )
                                        }
                                    }

                                    if (isAchieved) {
                                        Surface(
                                            color = colors.success.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                "Achieved 🎉",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.success
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Saved", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
                                        Text(formatCurrency(target.currentAmount), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Goal", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
                                        Text(formatCurrency(target.targetAmount), style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (target.progressPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = if (isAchieved) colors.success else colors.accent,
                                    trackColor = colors.surfaceAlt,
                                )

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("$pct% Saved", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                    if (!isAchieved) {
                                        Text("${formatCurrency(target.remainingAmount)} left", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { targetPendingContribute = target },
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text("💰 Deposit / Withdraw")
                                    }

                                    IconButton(onClick = { targetPendingDelete = target }) {
                                        Icon(Icons.Outlined.Delete, "Delete Target", tint = colors.destructive)
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }

        // Create Dialog
        if (showNewTargetDialog) {
            var name by remember { mutableStateOf("") }
            var targetAmountStr by remember { mutableStateOf("") }
            var currentAmountStr by remember { mutableStateOf("0") }
            var targetDate by remember { mutableStateOf("") }
            var notes by remember { mutableStateOf("") }
            var selectedCategoryId by remember { mutableStateOf<String?>(null) }
            var selectedAccountId by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showNewTargetDialog = false },
                title = { Text("New Savings Target", color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Target Name") },
                            placeholder = { Text("e.g. Emergency Fund, Laptop") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = targetAmountStr,
                            onValueChange = { targetAmountStr = it },
                            label = { Text("Target Goal Amount (IDR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = currentAmountStr,
                            onValueChange = { currentAmountStr = it },
                            label = { Text("Currently Saved Amount (IDR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = targetDate,
                            onValueChange = { targetDate = it },
                            label = { Text("Target Date (YYYY-MM-DD, Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val goal = targetAmountStr.toDoubleOrNull() ?: 0.0
                            val saved = currentAmountStr.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && goal > 0) {
                                viewModel.createTarget(
                                    name = name.trim(),
                                    targetAmount = goal,
                                    currentAmount = saved,
                                    targetDate = targetDate.ifBlank { null },
                                    categoryId = selectedCategoryId,
                                    accountId = selectedAccountId,
                                    color = null,
                                    notes = notes.ifBlank { null },
                                )
                                showNewTargetDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) { Text("Create", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showNewTargetDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }

        // Contribute Dialog
        targetPendingContribute?.let { tgt ->
            var amountStr by remember { mutableStateOf("") }
            var isWithdraw by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { targetPendingContribute = null },
                title = { Text(if (isWithdraw) "Withdraw from ${tgt.name}" else "Deposit to ${tgt.name}", color = colors.textPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isWithdraw,
                                onClick = { isWithdraw = false },
                                label = { Text("Deposit (Add)") }
                            )
                            FilterChip(
                                selected = isWithdraw,
                                onClick = { isWithdraw = true },
                                label = { Text("Withdraw") }
                            )
                        }

                        Text("Current Balance: ${formatCurrency(tgt.currentAmount)}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)

                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Amount (IDR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick presets
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(50000.0, 100000.0, 500000.0, 1000000.0)) { preset ->
                                OutlinedButton(
                                    onClick = { amountStr = preset.toLong().toString() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("+${formatCurrency(preset)}")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.contributeTarget(tgt.id, amt, isWithdraw)
                                targetPendingContribute = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) { Text("Confirm", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { targetPendingContribute = null }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }

        // Delete confirmation
        targetPendingDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { targetPendingDelete = null },
                title = { Text("Delete Savings Target", color = colors.textPrimary) },
                text = { Text("Are you sure you want to delete '${target.name}'?", color = colors.textSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTarget(target.id)
                            targetPendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.destructive)
                    ) { Text("Delete", color = Color.White) }
                },

                dismissButton = {
                    TextButton(onClick = { targetPendingDelete = null }) { Text("Cancel", color = colors.textSecondary) }
                }
            )
        }
    }
}
