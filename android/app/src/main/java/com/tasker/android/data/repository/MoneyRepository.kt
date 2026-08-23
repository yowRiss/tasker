package com.tasker.android.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.tasker.android.data.local.AppDatabase
import com.tasker.android.data.local.dao.AccountDao
import com.tasker.android.data.local.dao.BudgetDao
import com.tasker.android.data.local.dao.CategoryDao
import com.tasker.android.data.local.dao.RecurringDao
import com.tasker.android.data.local.dao.TransactionDao
import com.tasker.android.data.local.dao.TransactionReceiptDao
import com.tasker.android.data.local.entity.AccountEntity
import com.tasker.android.data.local.entity.BudgetEntity
import com.tasker.android.data.local.entity.CategoryEntity
import com.tasker.android.data.local.entity.RecurringTransactionEntity
import com.tasker.android.data.local.entity.SyncQueueEntity
import com.tasker.android.data.local.entity.TransactionEntity
import com.tasker.android.data.local.entity.TransactionReceiptEntity
import com.tasker.android.data.model.Account
import com.tasker.android.data.model.Budget
import com.tasker.android.data.model.Category
import com.tasker.android.data.model.CategorySpendItem
import com.tasker.android.data.model.CreateRecurringInput
import com.tasker.android.data.model.CreateTransactionInput
import com.tasker.android.data.model.MoneyDashboardData
import com.tasker.android.data.model.RecurringTransaction
import com.tasker.android.data.model.Transaction
import com.tasker.android.data.model.TransactionReceipt
import com.tasker.android.sync.SyncOutbox
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoneyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val receiptDao: TransactionReceiptDao,
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringDao,
    private val syncOutbox: SyncOutbox,
) {

    // ── Accounts ───────────────────────────────────────────────────

    fun observeAccounts(): Flow<List<Account>> =
        combine(accountDao.observeAll(), transactionDao.observeTransactionsFiltered()) { accounts, txs ->
            accounts.map { acc ->
                val balance = calculateAccountBalance(acc.id, txs)
                acc.toDomain(balance)
            }
        }

    private fun calculateAccountBalance(accountId: String, txs: List<TransactionEntity>): Double {
        var balance = 0.0
        for (tx in txs) {
            when (tx.transactionType) {
                "income" -> if (tx.accountId == accountId) balance += tx.amount
                "expense" -> if (tx.accountId == accountId) balance -= tx.amount
                "transfer" -> {
                    if (tx.accountId == accountId) balance -= tx.amount
                    if (tx.transferAccountId == accountId) balance += tx.amount
                }
            }
        }
        return balance
    }

    suspend fun createAccount(name: String, accountType: String): Account {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val entity = AccountEntity(
            id = id,
            name = name.trim(),
            accountType = accountType,
            currency = "IDR",
            createdAt = now,
            updatedAt = now,
        )
        val payload = buildJsonObject {
            put("name", name.trim())
            put("account_type", accountType)
        }.toString()

        db.withTransaction {
            accountDao.upsert(entity)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "account",
                    entityId = id,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }
        return entity.toDomain(0.0)
    }

    // ── Categories ─────────────────────────────────────────────────

    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun createCategory(name: String, categoryType: String, color: String? = null): Category {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val entity = CategoryEntity(
            id = id,
            name = name.trim(),
            categoryType = categoryType,
            color = color,
            createdAt = now,
            updatedAt = now,
        )
        val payload = buildJsonObject {
            put("name", name.trim())
            put("category_type", categoryType)
            if (color != null) put("color", color)
        }.toString()

        db.withTransaction {
            categoryDao.upsert(entity)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "category",
                    entityId = id,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }
        return entity.toDomain()
    }

    // ── Transactions ───────────────────────────────────────────────

    fun observeTransactions(
        accountId: String? = null,
        categoryId: String? = null,
        type: String? = null,
    ): Flow<List<Transaction>> =
        transactionDao.observeTransactionsFiltered(accountId, categoryId, type).map { txEntities ->
            txEntities.map { entity ->
                val account = accountDao.getById(entity.accountId)?.toDomain(0.0)
                val transferAccount = entity.transferAccountId?.let { accountDao.getById(it)?.toDomain(0.0) }
                val category = entity.categoryId?.let { categoryDao.getById(it)?.toDomain() }
                val receipt = receiptDao.getForTransaction(entity.id)?.toDomain()
                entity.toDomain(account, transferAccount, category, receipt)
            }
        }

    suspend fun createTransaction(input: CreateTransactionInput): Transaction {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        val entity = TransactionEntity(
            id = id,
            transactionType = input.transactionType,
            amount = input.amount,
            transactionDate = input.transactionDate,
            accountId = input.accountId,
            transferAccountId = input.transferAccountId,
            categoryId = input.categoryId,
            description = input.description?.trim(),
            createdAt = now,
            updatedAt = now,
        )

        val payload = buildJsonObject {
            put("transaction_type", input.transactionType)
            put("amount", input.amount.toString())
            put("transaction_date", input.transactionDate)
            put("account_id", input.accountId)
            put("transfer_account_id", input.transferAccountId)
            put("category_id", input.categoryId)
            put("description", input.description?.trim())
        }.toString()

        db.withTransaction {
            transactionDao.upsert(entity)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "transaction",
                    entityId = id,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }

        val account = accountDao.getById(entity.accountId)?.toDomain(0.0)
        val transferAccount = entity.transferAccountId?.let { accountDao.getById(it)?.toDomain(0.0) }
        val category = entity.categoryId?.let { categoryDao.getById(it)?.toDomain() }
        return entity.toDomain(account, transferAccount, category, null)
    }

    suspend fun getTransaction(id: String): Transaction? {
        val entity = transactionDao.getById(id) ?: return null
        val account = accountDao.getById(entity.accountId)?.toDomain(0.0)
        val transferAccount = entity.transferAccountId?.let { accountDao.getById(it)?.toDomain(0.0) }
        val category = entity.categoryId?.let { categoryDao.getById(it)?.toDomain() }
        val receipt = receiptDao.getForTransaction(entity.id)?.toDomain()
        return entity.toDomain(account, transferAccount, category, receipt)
    }

    suspend fun updateTransaction(id: String, input: CreateTransactionInput): Transaction {
        val existing = transactionDao.getById(id) ?: error("Transaction not found")
        val now = Instant.now().toString()
        val entity = existing.copy(
            transactionType = input.transactionType,
            amount = input.amount,
            transactionDate = input.transactionDate,
            accountId = input.accountId,
            transferAccountId = input.transferAccountId,
            categoryId = input.categoryId,
            description = input.description?.trim(),
            updatedAt = now,
        )
        val payload = buildJsonObject {
            put("transaction_type", input.transactionType)
            put("amount", input.amount.toString())
            put("transaction_date", input.transactionDate)
            put("account_id", input.accountId)
            put("transfer_account_id", input.transferAccountId)
            put("category_id", input.categoryId)
            put("description", input.description?.trim())
        }.toString()

        db.withTransaction {
            transactionDao.upsert(entity)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "transaction",
                    entityId = id,
                    operation = "UPDATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }

        return getTransaction(id) ?: error("Updated transaction not found")
    }

    suspend fun deleteTransaction(id: String) {
        val now = Instant.now().toString()
        db.withTransaction {
            transactionDao.softDelete(id, now)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "transaction",
                    entityId = id,
                    operation = "DELETE",
                    payload = "{}",
                    createdAt = now,
                )
            )
        }
    }

    suspend fun attachReceipt(transactionId: String, uri: Uri): TransactionReceipt? {
        val receiptId = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        val imagesDir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
        val destFile = File(imagesDir, "$receiptId.jpg")

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        }.getOrElse { return null }

        val entity = TransactionReceiptEntity(
            id = receiptId,
            transactionId = transactionId,
            localUri = destFile.absolutePath,
            originalFilename = "$receiptId.jpg",
            mimeType = "image/jpeg",
            byteSize = destFile.length(),
            createdAt = now,
            syncStatus = "pending"
        )

        val payload = buildJsonObject {
            put("transaction_id", transactionId)
            put("receipt_id", receiptId)
            put("local_uri", destFile.absolutePath)
        }.toString()

        db.withTransaction {
            receiptDao.upsert(entity)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "transaction_receipt",
                    entityId = receiptId,
                    operation = "UPLOAD_RECEIPT",
                    payload = payload,
                    createdAt = now,
                )
            )
        }

        return entity.toDomain()
    }

    // ── Budgets ────────────────────────────────────────────────────

    fun observeBudgets(): Flow<List<Budget>> =
        combine(budgetDao.observeAll(), transactionDao.observeTransactionsFiltered()) { budgets, txs ->
            budgets.map { bEntity ->
                val category = categoryDao.getById(bEntity.categoryId)?.toDomain()
                val spent = txs.filter {
                    it.categoryId == bEntity.categoryId &&
                    it.transactionType == "expense" &&
                    it.transactionDate >= bEntity.periodStart &&
                    it.transactionDate <= bEntity.periodEnd
                }.sumOf { it.amount }

                val remaining = (bEntity.amountLimit - spent).coerceAtLeast(0.0)
                val percentUsed = if (bEntity.amountLimit > 0) (spent / bEntity.amountLimit) * 100 else 0.0
                val isOver = spent > bEntity.amountLimit

                bEntity.toDomain(category, spent, remaining, percentUsed, isOver)
            }
        }

    suspend fun createBudget(categoryId: String, periodStart: String, periodEnd: String, amountLimit: Double): Budget {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()

        val entity = BudgetEntity(
            id = id,
            categoryId = categoryId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            amountLimit = amountLimit,
            createdAt = now,
            updatedAt = now,
        )

        val payload = buildJsonObject {
            put("category_id", categoryId)
            put("period_start", periodStart)
            put("period_end", periodEnd)
            put("amount_limit", amountLimit.toString())
        }.toString()

        db.withTransaction {
            budgetDao.upsert(entity)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "budget",
                    entityId = id,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }

        val category = categoryDao.getById(categoryId)?.toDomain()
        return entity.toDomain(category, 0.0, amountLimit, 0.0, false)
    }

    suspend fun deleteBudget(id: String) {
        val now = Instant.now().toString()
        db.withTransaction {
            budgetDao.softDelete(id, now)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "budget",
                    entityId = id,
                    operation = "DELETE",
                    payload = "{}",
                    createdAt = now,
                )
            )
        }
    }

    // ── Recurring ──────────────────────────────────────────────────

    fun observeRecurring(): Flow<List<RecurringTransaction>> =
        recurringDao.observeAll().map { rEntities ->
            rEntities.map { entity ->
                val account = accountDao.getById(entity.accountId)?.toDomain(0.0)
                val category = categoryDao.getById(entity.categoryId)?.toDomain()
                entity.toDomain(account, category)
            }
        }

    suspend fun createRecurring(input: CreateRecurringInput): RecurringTransaction {
        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val entity = RecurringTransactionEntity(
            id = id,
            transactionType = input.transactionType,
            amount = input.amount,
            accountId = input.accountId,
            categoryId = input.categoryId,
            description = input.description?.trim(),
            cadence = input.cadence,
            nextDueDate = input.nextDueDate,
            endsOn = input.endsOn,
            createdAt = now,
            updatedAt = now,
        )
        val payload = buildJsonObject {
            put("transaction_type", input.transactionType)
            put("amount", input.amount.toString())
            put("account_id", input.accountId)
            put("category_id", input.categoryId)
            if (input.description != null) put("description", input.description.trim())
            put("cadence", input.cadence)
            put("next_due_date", input.nextDueDate)
            if (input.endsOn != null) put("ends_on", input.endsOn)
            put("is_active", true)
        }.toString()

        db.withTransaction {
            recurringDao.upsert(entity)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "recurring_transaction",
                    entityId = id,
                    operation = "CREATE",
                    payload = payload,
                    createdAt = now,
                )
            )
        }

        val account = accountDao.getById(input.accountId)?.toDomain(0.0)
        val category = categoryDao.getById(input.categoryId)?.toDomain()
        return entity.toDomain(account, category)
    }

    suspend fun deleteRecurring(id: String) {
        val now = Instant.now().toString()
        db.withTransaction {
            recurringDao.softDelete(id, now)
            syncOutbox.enqueue(
                SyncQueueEntity(
                    entityType = "recurring_transaction",
                    entityId = id,
                    operation = "DELETE",
                    payload = "{}",
                    createdAt = now,
                )
            )
        }
    }

    // ── Mappers ────────────────────────────────────────────────────

    private fun AccountEntity.toDomain(balance: Double) = Account(
        id = id, name = name, accountType = accountType, currency = currency, balance = balance, archivedAt = archivedAt, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun CategoryEntity.toDomain() = Category(
        id = id, name = name, categoryType = categoryType, icon = icon, color = color, archivedAt = archivedAt, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun TransactionReceiptEntity.toDomain() = TransactionReceipt(
        id = id, transactionId = transactionId, localUri = localUri, objectPath = objectPath, originalFilename = originalFilename, mimeType = mimeType, byteSize = byteSize, syncStatus = syncStatus
    )

    private fun TransactionEntity.toDomain(account: Account?, transferAccount: Account?, category: Category?, receipt: TransactionReceipt?) = Transaction(
        id = id, transactionType = transactionType, amount = amount, transactionDate = transactionDate, accountId = accountId, account = account, transferAccountId = transferAccountId, transferAccount = transferAccount, categoryId = categoryId, category = category, description = description, receipt = receipt, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun BudgetEntity.toDomain(category: Category?, spent: Double, remaining: Double, percentUsed: Double, isOverBudget: Boolean) = Budget(
        id = id, categoryId = categoryId, category = category, periodStart = periodStart, periodEnd = periodEnd, amountLimit = amountLimit, spent = spent, remaining = remaining, percentUsed = percentUsed, isOverBudget = isOverBudget, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun RecurringTransactionEntity.toDomain(account: Account?, category: Category?) = RecurringTransaction(
        id = id, transactionType = transactionType, amount = amount, accountId = accountId, account = account, categoryId = categoryId, category = category, description = description, cadence = cadence, nextDueDate = nextDueDate, endsOn = endsOn, isActive = isActive, lastProcessedOn = lastProcessedOn, createdAt = createdAt, updatedAt = updatedAt
    )
}
