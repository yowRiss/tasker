package com.tasker.android.ui.money

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.tasker.android.ui.components.ZoomableImageDialog
import com.tasker.android.ui.theme.TaskerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    txId: String?,
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val colors = TaskerTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    var showReceiptZoom by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.attachReceipt(it) }
    }

    if (showReceiptZoom && uiState.receiptUri != null) {
        ZoomableImageDialog(
            model = uiState.receiptUri!!,
            contentDescription = "Receipt",
            onDismissRequest = { showReceiptZoom = false }
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(if (txId == null) "New Transaction" else "Edit Transaction", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::saveTransaction, enabled = !uiState.isLoading) {
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
            // Error banner
            uiState.errorMessage?.let { err ->
                Surface(color = colors.destructiveSubtle, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = err, color = colors.destructive, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            // Transaction Type Selector
            Text("Type", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("expense" to "Expense", "income" to "Income", "transfer" to "Transfer").forEach { (type, label) ->
                    FilterChip(
                        selected = uiState.transactionType == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(label) },
                        colors = filterChipColors()
                    )
                }
            }

            // Amount Input
            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount (IDR) *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = taskerOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Description Input
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = taskerOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.transactionDate,
                onValueChange = viewModel::onDateChange,
                label = { Text("Date (YYYY-MM-DD) *") },
                supportingText = { Text("Use a date such as ${java.time.LocalDate.now()}") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = taskerOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            // Account Selector
            Text("Account *", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts) { acc ->
                    FilterChip(
                        selected = uiState.accountId == acc.id,
                        onClick = { viewModel.onAccountChange(acc.id) },
                        label = { Text(acc.name) },
                        colors = filterChipColors()
                    )
                }
            }

            // Transfer Destination Account (if Transfer)
            if (uiState.transactionType == "transfer") {
                Text("Transfer To Account *", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts.filter { it.id != uiState.accountId }) { acc ->
                        FilterChip(
                            selected = uiState.transferAccountId == acc.id,
                            onClick = { viewModel.onTransferAccountChange(acc.id) },
                            label = { Text(acc.name) },
                            colors = filterChipColors()
                        )
                    }
                }
            }

            // Category Selector (if Income / Expense)
            if (uiState.transactionType != "transfer") {
                Text("Category", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                val filteredCategories = categories.filter { it.categoryType == uiState.transactionType }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredCategories) { cat ->
                        FilterChip(
                            selected = uiState.categoryId == cat.id,
                            onClick = { viewModel.onCategoryChange(cat.id) },
                            label = { Text(cat.name) },
                            colors = filterChipColors()
                        )
                    }
                }
            }

            // Receipt Attachment
            Text("Receipt", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            if (uiState.receiptUri != null) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(120.dp)
                        .clickable { showReceiptZoom = true }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(uiState.receiptUri).build(),
                        contentDescription = "Receipt",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Receipt Photo", color = colors.textPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = viewModel::saveTransaction,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text(
                        if (uiState.isEditing) "Update Transaction" else "Save Transaction",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
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
