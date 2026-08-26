package com.tasker.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tasker.android.data.local.entity.ProjectEntity
import com.tasker.android.data.local.entity.SubtaskEntity
import com.tasker.android.data.local.entity.TagEntity
import com.tasker.android.data.local.entity.TaskEntity
import com.tasker.android.data.local.entity.TaskTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE is_deleted = 0 ORDER BY LOWER(name) ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("UPDATE projects SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE projects SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE is_deleted = 0 ORDER BY LOWER(name) ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Query("UPDATE tags SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE tags SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface TaskDao {
    @Query("SELECT COUNT(*) FROM tasks WHERE is_deleted = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
        AND (:status = 'all' OR status = :status)
        AND (:projectId IS NULL OR project_id = :projectId)
        AND (:priority IS NULL OR priority = :priority)
        AND (:query = '' OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY 
            CASE WHEN due_date IS NULL THEN 1 ELSE 0 END ASC,
            due_date ASC,
            priority DESC,
            updated_at DESC
    """)
    fun observeTasksFiltered(
        status: String = "open",
        projectId: String? = null,
        priority: Int? = null,
        query: String = ""
    ): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, completed_at = :completedAt, updated_at = :now WHERE id = :id")
    suspend fun updateCompletion(id: String, status: String, completedAt: String?, now: String)

    @Query("UPDATE tasks SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)

    @Query("UPDATE tasks SET id = :newId WHERE id = :oldId")
    suspend fun remapId(oldId: String, newId: String)
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE task_id = :taskId AND is_deleted = 0 ORDER BY position ASC")
    fun observeSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE task_id = :taskId AND is_deleted = 0 ORDER BY position ASC")
    suspend fun getSubtasksForTask(taskId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE task_id = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Query("UPDATE subtasks SET task_id = :newTaskId WHERE task_id = :oldTaskId")
    suspend fun remapTaskId(oldTaskId: String, newTaskId: String)
}

@Dao
interface TaskTagDao {
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN task_tags tt ON t.id = tt.tag_id
        WHERE tt.task_id = :taskId AND t.is_deleted = 0
    """)
    fun observeTagsForTask(taskId: String): Flow<List<TagEntity>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN task_tags tt ON t.id = tt.tag_id
        WHERE tt.task_id = :taskId AND t.is_deleted = 0
    """)
    suspend fun getTagsForTask(taskId: String): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(taskTags: List<TaskTagEntity>)

    @Query("DELETE FROM task_tags WHERE task_id = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Query("UPDATE task_tags SET task_id = :newTaskId WHERE task_id = :oldTaskId")
    suspend fun remapTaskId(oldTaskId: String, newTaskId: String)

    @Query("UPDATE task_tags SET tag_id = :newTagId WHERE tag_id = :oldTagId")
    suspend fun remapTagId(oldTagId: String, newTagId: String)
}
