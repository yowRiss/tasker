package com.tasker.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index(value = ["updated_at"])]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    @ColumnInfo(name = "content_md")
    val contentMd: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["note_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["note_id"]), Index(value = ["tag_id"])]
)
data class NoteTagEntity(
    @ColumnInfo(name = "note_id")
    val noteId: String,
    @ColumnInfo(name = "tag_id")
    val tagId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "note_task_links",
    primaryKeys = ["note_id", "task_id"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["note_id"]), Index(value = ["task_id"])]
)
data class NoteTaskLinkEntity(
    @ColumnInfo(name = "note_id")
    val noteId: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
)

@Entity(
    tableName = "note_images",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["note_id"])]
)
data class NoteImageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "note_id")
    val noteId: String,
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
    @ColumnInfo(name = "alt_text")
    val altText: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "pending", // pending | uploaded | failed
)
