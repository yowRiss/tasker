package com.tasker.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasker.android.data.local.entity.SyncMetadataEntity
import com.tasker.android.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status IN ('pending', 'failed') AND retry_count < 10 ORDER BY id ASC")
    suspend fun getPending(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('pending', 'processing')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'failed'")
    fun observeFailedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_queue SET status = :status, last_error = :error, retry_count = retry_count + 1 WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String?)

    @Query("UPDATE sync_queue SET status = 'pending', last_error = NULL, retry_count = 0 WHERE status = 'failed'")
    suspend fun retryAllFailed()

    @Query("UPDATE sync_queue SET entity_id = :newId WHERE entity_id = :oldId")
    suspend fun remapEntityId(oldId: String, newId: String)
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE table_name = :tableName")
    suspend fun getMetadata(tableName: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)
}
