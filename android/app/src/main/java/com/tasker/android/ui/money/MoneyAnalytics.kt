package com.tasker.android.ui.money

import com.tasker.android.data.model.Account
import com.tasker.android.data.model.CategorySpendItem
import com.tasker.android.data.model.MoneyDashboardData
import com.tasker.android.data.model.Transaction
import java.time.LocalDate
import java.time.YearMonth

enum class MoneyPeriod(val label: String) {
    ALL("All time"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
}

data class MoneyUiState(
    val query: String = "",
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val selectedType: String? = null,
    val selectedPeriod: MoneyPeriod = MoneyPeriod.THIS_MONTH,
) {
    val hasTransactionFilters: Boolean
        get() = query.isNotBlank() || selectedCategoryId != null || selectedType != null
}

internal fun filterMoneyTransactions(
    transactions: List<Transaction>,
    state: MoneyUiState,
    today: LocalDate = LocalDate.now(),
): List<Transaction> {
    val normalizedQuery = state.query.trim().lowercase()
    return transactions.filter { transaction ->
        matchesAccount(transaction, state.selectedAccountId) &&
            matchesPeriod(transaction, state.selectedPeriod, today) &&
            (state.selectedCategoryId == null || transaction.categoryId == state.selectedCategoryId) &&
            (state.selectedType == null || transaction.transactionType == state.selectedType) &&
            (normalizedQuery.isEmpty() || transaction.searchableText().contains(normalizedQuery))
    }
}

internal fun calculateMoneyDashboard(
    accounts: List<Account>,
    transactions: List<Transaction>,
    state: MoneyUiState,
    today: LocalDate = LocalDate.now(),
): MoneyDashboardData {
    val scopedAccounts = accounts.filter { state.selectedAccountId == null || it.id == state.selectedAccountId }
    val periodTransactions = transactions.filter { transaction ->
        matchesAccount(transaction, state.selectedAccountId) &&
            matchesPeriod(transaction, state.selectedPeriod, today)
    }
    val income = periodTransactions
        .filter { it.transactionType == "income" }
        .sumOf { it.amount }
    val expenseTransactions = periodTransactions.filter { it.transactionType == "expense" }
    val expense = expenseTransactions.sumOf { it.amount }
    val categorySpend = expenseTransactions
        .filter { it.category != null }
        .groupBy { it.category!!.name }
        .map { (categoryName, categoryTransactions) ->
            CategorySpendItem(
                categoryName = categoryName,
                categoryColor = categoryTransactions.first().category?.color,
                amount = categoryTransactions.sumOf { it.amount },
            )
        }
        .sortedByDescending { it.amount }

    return MoneyDashboardData(
        totalBalance = scopedAccounts.sumOf { it.balance },
        totalIncome = income,
        totalExpense = expense,
        netCashFlow = income - expense,
        transactionCount = periodTransactions.size,
        periodLabel = state.selectedPeriod.label,
        categorySpend = categorySpend,
    )
}

private fun matchesAccount(transaction: Transaction, accountId: String?): Boolean =
    accountId == null || transaction.accountId == accountId || transaction.transferAccountId == accountId

private fun matchesPeriod(transaction: Transaction, period: MoneyPeriod, today: LocalDate): Boolean {
    if (period == MoneyPeriod.ALL) return true
    val transactionMonth = runCatching { YearMonth.from(LocalDate.parse(transaction.transactionDate)) }.getOrNull()
        ?: return false
    val currentMonth = YearMonth.from(today)
    return transactionMonth == when (period) {
        MoneyPeriod.THIS_MONTH -> currentMonth
        MoneyPeriod.LAST_MONTH -> currentMonth.minusMonths(1)
        MoneyPeriod.ALL -> return true
    }
}

private fun Transaction.searchableText(): String = listOfNotNull(
    description,
    category?.name,
    account?.name,
    transferAccount?.name,
    transactionType,
).joinToString(" ").lowercase()
