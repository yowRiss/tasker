package com.tasker.android.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    @SerialName("account_type")
    val accountType: String,
    val currency: String = "IDR",
    val balance: String = "0.00",
    @SerialName("archived_at")
    val archivedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class AccountCreateRequest(
    val name: String,
    @SerialName("account_type")
    val accountType: String,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    @SerialName("category_type")
    val categoryType: String,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("archived_at")
    val archivedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class CategoryCreateRequest(
    val name: String,
    @SerialName("category_type")
    val categoryType: String,
    val icon: String? = null,
    val color: String? = null,
)

@Serializable
data class TransactionDto(
    val id: String,
    @SerialName("transaction_type")
    val transactionType: String,
    val amount: String, // decimal string from server
    @SerialName("transaction_date")
    val transactionDate: String,
    @SerialName("account_id")
    val accountId: String,
    @SerialName("transfer_account_id")
    val transferAccountId: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class TransactionCreateRequest(
    @SerialName("transaction_type")
    val transactionType: String,
    val amount: String,
    @SerialName("transaction_date")
    val transactionDate: String,
    @SerialName("account_id")
    val accountId: String,
    @SerialName("transfer_account_id")
    val transferAccountId: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    val description: String? = null,
)

@Serializable
data class BudgetDto(
    val id: String,
    @SerialName("category_id")
    val categoryId: String,
    @SerialName("period_start")
    val periodStart: String,
    @SerialName("period_end")
    val periodEnd: String,
    @SerialName("amount_limit")
    val amountLimit: String,
    val spent: String = "0.00",
    val remaining: String = "0.00",
    @SerialName("percent_used")
    val percentUsed: String = "0.00",
    @SerialName("is_over_budget")
    val isOverBudget: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class BudgetCreateRequest(
    @SerialName("category_id")
    val categoryId: String,
    @SerialName("period_start")
    val periodStart: String,
    @SerialName("period_end")
    val periodEnd: String,
    @SerialName("amount_limit")
    val amountLimit: String,
)

@Serializable
data class RecurringDto(
    val id: String,
    @SerialName("transaction_type")
    val transactionType: String,
    val amount: String,
    @SerialName("account_id")
    val accountId: String,
    @SerialName("category_id")
    val categoryId: String,
    val description: String? = null,
    val cadence: String,
    @SerialName("next_due_date")
    val nextDueDate: String,
    @SerialName("ends_on")
    val endsOn: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("last_processed_on")
    val lastProcessedOn: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class RecurringCreateRequest(
    @SerialName("transaction_type")
    val transactionType: String,
    val amount: String,
    @SerialName("account_id")
    val accountId: String,
    @SerialName("category_id")
    val categoryId: String,
    val description: String? = null,
    val cadence: String,
    @SerialName("next_due_date")
    val nextDueDate: String,
    @SerialName("ends_on")
    val endsOn: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
)

@Serializable
data class TargetDto(
    val id: String,
    val name: String,
    @SerialName("target_amount")
    val targetAmount: String,
    @SerialName("current_amount")
    val currentAmount: String = "0.00",
    @SerialName("target_date")
    val targetDate: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("category_name")
    val categoryName: String? = null,
    @SerialName("account_id")
    val accountId: String? = null,
    @SerialName("account_name")
    val accountName: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val status: String = "active",
    val notes: String? = null,
    @SerialName("progress_percent")
    val progressPercent: String = "0.00",
    @SerialName("remaining_amount")
    val remainingAmount: String = "0.00",
    @SerialName("is_achieved")
    val isAchieved: Boolean = false,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class TargetCreateRequest(
    val name: String,
    @SerialName("target_amount")
    val targetAmount: String,
    @SerialName("current_amount")
    val currentAmount: String? = null,
    @SerialName("target_date")
    val targetDate: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("account_id")
    val accountId: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val status: String? = null,
    val notes: String? = null,
)

@Serializable
data class TargetUpdateRequest(
    val name: String? = null,
    @SerialName("target_amount")
    val targetAmount: String? = null,
    @SerialName("current_amount")
    val currentAmount: String? = null,
    @SerialName("target_date")
    val targetDate: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("account_id")
    val accountId: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val status: String? = null,
    val notes: String? = null,
)

@Serializable
data class TargetContributeRequest(
    val amount: String,
    @SerialName("is_withdraw")
    val isWithdraw: Boolean = false,
)

