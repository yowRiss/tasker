package com.tasker.android.ui.money

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tasker.android.data.model.CategorySpendItem
import com.tasker.android.data.model.Transaction
import com.tasker.android.ui.theme.TaskerTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = TaskerTheme.colors
    val (txColor, txSign) = when (transaction.transactionType) {
        "income" -> colors.success to "+"
        "expense" -> colors.destructive to "-"
        else -> colors.textPrimary to ""
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (transaction.transactionType) {
                    "income" -> Icons.Outlined.ArrowUpward
                    "expense" -> Icons.Outlined.ArrowDownward
                    else -> Icons.Outlined.SwapHoriz
                },
                contentDescription = null,
                tint = txColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: transaction.category?.name ?: transaction.transactionType.capitalizeLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = transactionContext(transaction),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (transaction.receipt != null) {
                Icon(
                    Icons.Outlined.Receipt,
                    contentDescription = "Receipt attached",
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp),
                )
            }
            Text(
                text = "$txSign${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = txColor,
                maxLines = 1,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete transaction",
                    tint = colors.textTertiary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
internal fun SummaryMetric(
    label: String,
    amount: Double,
    color: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TaskerTheme.colors.textTertiary)
            Text(formatCurrency(amount), style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
        }
    }
}

@Composable
internal fun CategorySpendCanvasChart(
    items: List<CategorySpendItem>,
    accentColor: Color,
) {
    val colors = TaskerTheme.colors
    val maxAmount = items.maxOfOrNull { it.amount } ?: 1.0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.take(5).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.width(90.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Canvas(modifier = Modifier.weight(1f).height(12.dp)) {
                    val progress = (item.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                    drawRoundRect(
                        color = colors.surfaceAlt,
                        size = Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    )
                    drawRoundRect(
                        color = accentColor,
                        size = Size(size.width * progress, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(formatCurrency(item.amount), style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
            }
        }
    }
}

internal fun formatCurrency(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)

internal fun formatSignedCurrency(amount: Double): String =
    "${if (amount >= 0) "+" else "-"}${formatCurrency(kotlin.math.abs(amount))}"

internal fun formatTransactionDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale("id", "ID")))
}.getOrDefault(value)

private fun transactionContext(transaction: Transaction): String = when (transaction.transactionType) {
    "transfer" -> "${transaction.account?.name ?: "Account"} to ${transaction.transferAccount?.name ?: "Account"}"
    else -> listOfNotNull(transaction.account?.name, transaction.category?.name)
        .joinToString(" • ")
        .ifBlank { "Account" }
}

@Composable
internal fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = TaskerTheme.colors.accentSubtle,
    selectedLabelColor = TaskerTheme.colors.accent,
    containerColor = TaskerTheme.colors.surfaceAlt,
    labelColor = TaskerTheme.colors.textSecondary,
)

internal fun String.capitalizeLabel(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
