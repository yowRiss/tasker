package com.tasker.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasker.android.data.local.entity.NoteEntity
import com.tasker.android.data.local.entity.NoteImageEntity
import com.tasker.android.data.local.entity.NoteTagEntity
import com.tasker.android.data.local.entity.NoteTaskLinkEntity
import com.tasker.android.data.local.entity.TagEntity
import com.tasker.android.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("""
        SELECT * FROM notes 
        WHERE is_deleted = 0 
        AND (:query = '' OR title LIKE '%' || :query || '%' OR content_md LIKE '%' || :query || '%')
        ORDER BY updated_at DESC
    """)
    fun observeNotesFiltered(query: String = ""): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Query("UPDATE notes SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE notes SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface NoteTagDao {
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN note_tags nt ON t.id = nt.tag_id
        WHERE nt.note_id = :noteId AND t.is_deleted = 0
    """)
    suspend fun getTagsForNote(noteId: String): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(noteTags: List<NoteTagEntity>)

    @Query("DELETE FROM note_tags WHERE note_id = :noteId")
    suspend fun deleteForNote(noteId: String)

    @Query("UPDATE note_tags SET note_id = :newNoteId WHERE note_id = :oldNoteId")
    suspend fun remapNoteId(oldNoteId: String, newNoteId: String)
}

@Dao
interface NoteTaskLinkDao {
    @Query("""
        SELECT tk.* FROM tasks tk
        INNER JOIN note_task_links ntl ON tk.id = ntl.task_id
        WHERE ntl.note_id = :noteId AND tk.is_deleted = 0
    """)
    suspend fun getTasksForNote(noteId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<NoteTaskLinkEntity>)

    @Query("DELETE FROM note_task_links WHERE note_id = :noteId")
    suspend fun deleteForNote(noteId: String)

    @Query("UPDATE note_task_links SET note_id = :newNoteId WHERE note_id = :oldNoteId")
    suspend fun remapNoteId(oldNoteId: String, newNoteId: String)
}

@Dao
interface NoteImageDao {
    @Query("SELECT * FROM note_images WHERE note_id = :noteId ORDER BY created_at ASC")
    fun observeImagesForNote(noteId: String): Flow<List<NoteImageEntity>>

    @Query("SELECT * FROM note_images WHERE note_id = :noteId ORDER BY created_at ASC")
    suspend fun getImagesForNote(noteId: String): List<NoteImageEntity>

    @Query("SELECT * FROM note_images WHERE id = :id")
    suspend fun getById(id: String): NoteImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(image: NoteImageEntity)

    @Query("DELETE FROM note_images WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE note_images SET sync_status = :status, object_path = :objectPath WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, objectPath: String?)

    @Query("UPDATE note_images SET note_id = :newNoteId WHERE note_id = :oldNoteId")
    suspend fun remapNoteId(oldNoteId: String, newNoteId: String)
}
