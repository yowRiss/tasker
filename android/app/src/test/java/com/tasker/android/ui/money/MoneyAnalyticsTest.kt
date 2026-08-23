package com.tasker.android.ui.money

import com.tasker.android.data.model.Account
import com.tasker.android.data.model.Category
import com.tasker.android.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MoneyAnalyticsTest {
    private val today = LocalDate.of(2026, 8, 23)
    private val bank = Account(id = "bank", name = "Bank", accountType = "bank", balance = 700.0)
    private val cash = Account(id = "cash", name = "Cash", accountType = "cash", balance = 300.0)
    private val food = Category(id = "food", name = "Food", categoryType = "expense")

    @Test
    fun thisMonthDashboardCalculatesCashFlowWithoutCountingTransfers() {
        val transactions = listOf(
            transaction("income", 1_000.0, "2026-08-01"),
            transaction("expense", 250.0, "2026-08-10", category = food),
            transaction("transfer", 200.0, "2026-08-15", transferAccount = cash),
            transaction("expense", 99.0, "2026-07-20", category = food),
        )

        val dashboard = calculateMoneyDashboard(
            accounts = listOf(bank, cash),
            transactions = transactions,
            state = MoneyUiState(selectedPeriod = MoneyPeriod.THIS_MONTH),
            today = today,
        )

        assertEquals(1_000.0, dashboard.totalBalance, 0.0)
        assertEquals(1_000.0, dashboard.totalIncome, 0.0)
        assertEquals(250.0, dashboard.totalExpense, 0.0)
        assertEquals(750.0, dashboard.netCashFlow, 0.0)
        assertEquals(3, dashboard.transactionCount)
        assertEquals(250.0, dashboard.categorySpend.single().amount, 0.0)
    }

    @Test
    fun filtersSearchCategoryAccountTypeAndPreviousMonthTogether() {
        val matching = transaction(
            type = "expense",
            amount = 80.0,
            date = "2026-07-12",
            description = "Team lunch",
            category = food,
        )
        val transactions = listOf(
            matching,
            transaction("expense", 10.0, "2026-08-12", description = "Lunch", category = food),
            transaction("income", 80.0, "2026-07-12", description = "Team lunch", category = food),
        )

        val result = filterMoneyTransactions(
            transactions = transactions,
            state = MoneyUiState(
                query = "food",
                selectedAccountId = bank.id,
                selectedCategoryId = food.id,
                selectedType = "expense",
                selectedPeriod = MoneyPeriod.LAST_MONTH,
            ),
            today = today,
        )

        assertEquals(listOf(matching), result)
    }

    private fun transaction(
        type: String,
        amount: Double,
        date: String,
        description: String? = null,
        category: Category? = null,
        transferAccount: Account? = null,
    ) = Transaction(
        id = "$type-$date-$amount",
        transactionType = type,
        amount = amount,
        transactionDate = date,
        accountId = bank.id,
        account = bank,
        transferAccountId = transferAccount?.id,
        transferAccount = transferAccount,
        categoryId = category?.id,
        category = category,
        description = description,
    )
}
