package com.tasker.android.remote.api

import com.tasker.android.remote.dto.CompletionRequest
import com.tasker.android.remote.dto.ItemsResponse
import com.tasker.android.remote.dto.ProjectCreateRequest
import com.tasker.android.remote.dto.ProjectDto
import com.tasker.android.remote.dto.TagCreateRequest
import com.tasker.android.remote.dto.TagDto
import com.tasker.android.remote.dto.TaskCreateRequest
import com.tasker.android.remote.dto.TaskDto
import com.tasker.android.remote.dto.TaskUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApi {

    // ── Tasks ──────────────────────────────────────────────────────

    @GET("v1/tasks")
    suspend fun listTasks(
        @Query("status") status: String = "all",
        @Query("limit") limit: Int = 1000,
        @Query("project_id") projectId: String? = null,
        @Query("q") query: String? = null,
    ): ItemsResponse<TaskDto>

    @POST("v1/tasks")
    suspend fun createTask(@Body body: TaskCreateRequest): TaskDto

    @GET("v1/tasks/{id}")
    suspend fun getTask(@Path("id") id: String): TaskDto

    @PATCH("v1/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: TaskUpdateRequest): TaskDto

    @DELETE("v1/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>

    @PATCH("v1/tasks/{taskId}/completion")
    suspend fun toggleCompletion(
        @Path("taskId") taskId: String,
        @Body body: CompletionRequest,
    ): TaskDto

    // ── Projects ───────────────────────────────────────────────────

    @GET("v1/projects")
    suspend fun listProjects(): ItemsResponse<ProjectDto>

    @POST("v1/projects")
    suspend fun createProject(@Body body: ProjectCreateRequest): ProjectDto

    @PATCH("v1/projects/{id}")
    suspend fun updateProject(@Path("id") id: String, @Body body: ProjectCreateRequest): ProjectDto

    @DELETE("v1/projects/{id}")
    suspend fun deleteProject(@Path("id") id: String): Response<Unit>

    // ── Tags ───────────────────────────────────────────────────────

    @GET("v1/tags")
    suspend fun listTags(): ItemsResponse<TagDto>

    @POST("v1/tags")
    suspend fun createTag(@Body body: TagCreateRequest): TagDto

    @PATCH("v1/tags/{id}")
    suspend fun updateTag(@Path("id") id: String, @Body body: TagCreateRequest): TagDto

    @DELETE("v1/tags/{id}")
    suspend fun deleteTag(@Path("id") id: String): Response<Unit>
}
