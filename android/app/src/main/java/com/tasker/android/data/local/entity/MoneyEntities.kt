package com.tasker.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "account_type")
    val accountType: String, // cash | bank | e_wallet | credit_card
    val currency: String = "IDR",
    @ColumnInfo(name = "archived_at")
    val archivedAt: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "category_type")
    val categoryType: String, // income | expense
    val icon: String? = null,
    val color: String? = null,
    @ColumnInfo(name = "archived_at")
    val archivedAt: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["transfer_account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["transfer_account_id"]),
        Index(value = ["category_id"]),
        Index(value = ["transaction_date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "transaction_type")
    val transactionType: String, // income | expense | transfer
    val amount: Double,
    @ColumnInfo(name = "transaction_date")
    val transactionDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "transfer_account_id")
    val transferAccountId: String? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,
    val description: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "transaction_receipts",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transaction_id"], unique = true)]
)
data class TransactionReceiptEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,
    @ColumnInfo(name = "bucket_id")
    val bucketId: String = "note-images",
    @ColumnInfo(name = "object_path")
    val objectPath: String? = null,
    @ColumnInfo(name = "local_uri")
    val localUri: String,
    @ColumnInfo(name = "original_filename")
    val originalFilename: String,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "byte_size")
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "pending", // pending | uploaded | failed
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["category_id"])]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "period_start")
    val periodStart: String, // YYYY-MM-DD
    @ColumnInfo(name = "period_end")
    val periodEnd: String, // YYYY-MM-DD
    @ColumnInfo(name = "amount_limit")
    val amountLimit: Double,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["account_id"]), Index(value = ["category_id"])]
)
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "transaction_type")
    val transactionType: String, // income | expense
    val amount: Double,
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    val description: String? = null,
    val cadence: String, // weekly | monthly | yearly
    @ColumnInfo(name = "next_due_date")
    val nextDueDate: String,
    @ColumnInfo(name = "ends_on")
    val endsOn: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "last_processed_on")
    val lastProcessedOn: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "targets",
    indices = [Index(value = ["category_id"]), Index(value = ["account_id"])]
)
data class TargetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "target_amount")
    val targetAmount: Double,
    @ColumnInfo(name = "current_amount")
    val currentAmount: Double = 0.0,
    @ColumnInfo(name = "target_date")
    val targetDate: String? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,
    @ColumnInfo(name = "account_id")
    val accountId: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val status: String = "active",
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

