package com.tasker.android.remote.api

import com.tasker.android.remote.dto.ItemsResponse
import com.tasker.android.remote.dto.NoteCreateRequest
import com.tasker.android.remote.dto.NoteDto
import com.tasker.android.remote.dto.NoteImageUploadResponse
import com.tasker.android.remote.dto.NoteUpdateRequest
import com.tasker.android.remote.dto.SignedUrlResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface NoteApi {

    @GET("v1/notes")
    suspend fun listNotes(
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 1000,
    ): ItemsResponse<NoteDto>

    @POST("v1/notes")
    suspend fun createNote(@Body body: NoteCreateRequest): NoteDto

    @GET("v1/notes/{id}")
    suspend fun getNote(@Path("id") id: String): NoteDto

    @PATCH("v1/notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body body: NoteUpdateRequest): NoteDto

    @DELETE("v1/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<Unit>

    @PUT("v1/notes/{noteId}/tasks/{taskId}")
    suspend fun linkTask(@Path("noteId") noteId: String, @Path("taskId") taskId: String): Response<Unit>

    @DELETE("v1/notes/{noteId}/tasks/{taskId}")
    suspend fun unlinkTask(@Path("noteId") noteId: String, @Path("taskId") taskId: String): Response<Unit>

    @Multipart
    @POST("v1/notes/{noteId}/images")
    suspend fun uploadImage(
        @Path("noteId") noteId: String,
        @Part file: MultipartBody.Part,
    ): NoteImageUploadResponse

    @DELETE("v1/note-images/{imageId}")
    suspend fun deleteImage(@Path("imageId") imageId: String): Response<Unit>

    @GET("v1/note-images/{imageId}/access")
    suspend fun getSignedUrl(@Path("imageId") imageId: String): SignedUrlResponse
}
