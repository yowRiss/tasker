package com.tasker.android.data.model

data class Account(
    val id: String,
    val name: String,
    val accountType: String, // cash | bank | e_wallet | credit_card
    val currency: String = "IDR",
    val balance: Double = 0.0,
    val archivedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class Category(
    val id: String,
    val name: String,
    val categoryType: String, // income | expense
    val icon: String? = null,
    val color: String? = null,
    val archivedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class TransactionReceipt(
    val id: String,
    val transactionId: String,
    val localUri: String,
    val objectPath: String? = null,
    val originalFilename: String,
    val mimeType: String,
    val byteSize: Long,
    val syncStatus: String = "pending",
)

data class Transaction(
    val id: String,
    val transactionType: String, // income | expense | transfer
    val amount: Double,
    val transactionDate: String, // YYYY-MM-DD
    val accountId: String,
    val account: Account? = null,
    val transferAccountId: String? = null,
    val transferAccount: Account? = null,
    val categoryId: String? = null,
    val category: Category? = null,
    val description: String? = null,
    val receipt: TransactionReceipt? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class Budget(
    val id: String,
    val categoryId: String,
    val category: Category? = null,
    val periodStart: String,
    val periodEnd: String,
    val amountLimit: Double,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val percentUsed: Double = 0.0,
    val isOverBudget: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class RecurringTransaction(
    val id: String,
    val transactionType: String,
    val amount: Double,
    val accountId: String,
    val account: Account? = null,
    val categoryId: String,
    val category: Category? = null,
    val description: String? = null,
    val cadence: String, // weekly | monthly | yearly
    val nextDueDate: String,
    val endsOn: String? = null,
    val isActive: Boolean = true,
    val lastProcessedOn: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class MoneyDashboardData(
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categorySpend: List<CategorySpendItem> = emptyList(),
)

data class CategorySpendItem(
    val categoryName: String,
    val categoryColor: String? = null,
    val amount: Double,
)

data class CreateTransactionInput(
    val transactionType: String,
    val amount: Double,
    val transactionDate: String,
    val accountId: String,
    val transferAccountId: String? = null,
    val categoryId: String? = null,
    val description: String? = null,
)
