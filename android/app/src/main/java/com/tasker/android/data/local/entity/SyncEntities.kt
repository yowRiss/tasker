package com.tasker.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "entity_type")
    val entityType: String, // task | project | tag | note | note_image | account | category | transaction | transaction_receipt | budget

    @ColumnInfo(name = "entity_id")
    val entityId: String, // client UUID

    @ColumnInfo(name = "operation")
    val operation: String, // CREATE | UPDATE | DELETE | UPLOAD_IMAGE | UPLOAD_RECEIPT

    @ColumnInfo(name = "payload")
    val payload: String, // JSON payload string

    @ColumnInfo(name = "created_at")
    val createdAt: String, // ISO 8601 string

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "status")
    val status: String = "pending", // pending | processing | failed
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "table_name")
    val tableName: String,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: String,

    @ColumnInfo(name = "sync_cursor")
    val syncCursor: String? = null,
)
