package com.tasker.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasker.android.data.local.entity.AccountEntity
import com.tasker.android.data.local.entity.BudgetEntity
import com.tasker.android.data.local.entity.CategoryEntity
import com.tasker.android.data.local.entity.RecurringTransactionEntity
import com.tasker.android.data.local.entity.TargetEntity
import com.tasker.android.data.local.entity.TransactionEntity
import com.tasker.android.data.local.entity.TransactionReceiptEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE is_deleted = 0 ORDER BY LOWER(name) ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE is_deleted = 0 ORDER BY LOWER(name) ASC")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Query("UPDATE accounts SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE accounts SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE is_deleted = 0 ORDER BY LOWER(name) ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_deleted = 0 ORDER BY LOWER(name) ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Query("UPDATE categories SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE categories SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface TransactionDao {
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 
        AND (:accountId IS NULL OR account_id = :accountId OR transfer_account_id = :accountId)
        AND (:categoryId IS NULL OR category_id = :categoryId)
        AND (:type IS NULL OR transaction_type = :type)
        ORDER BY transaction_date DESC, created_at DESC
    """)
    fun observeTransactionsFiltered(
        accountId: String? = null,
        categoryId: String? = null,
        type: String? = null
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Query("UPDATE transactions SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE transactions SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface TransactionReceiptDao {
    @Query("SELECT * FROM transaction_receipts WHERE transaction_id = :transactionId")
    suspend fun getForTransaction(transactionId: String): TransactionReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(receipt: TransactionReceiptEntity)

    @Query("DELETE FROM transaction_receipts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE is_deleted = 0 ORDER BY period_start DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("UPDATE budgets SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE budgets SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_transactions WHERE is_deleted = 0 ORDER BY next_due_date ASC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): RecurringTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recurring: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE recurring_transactions SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface TargetDao {
    @Query("SELECT * FROM targets WHERE is_deleted = 0 ORDER BY CASE WHEN status = 'active' THEN 0 WHEN status = 'achieved' THEN 1 ELSE 2 END, target_date ASC, created_at DESC")
    fun observeAll(): Flow<List<TargetEntity>>

    @Query("SELECT * FROM targets WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): TargetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(target: TargetEntity)

    @Query("UPDATE targets SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE targets SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

