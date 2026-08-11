package com.tasker.android.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    @SerialName("content_md")
    val contentMd: String = "",
    val tags: List<TagDto>? = null,
    val tasks: List<TaskDto>? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class NoteCreateRequest(
    val title: String,
    @SerialName("content_md")
    val contentMd: String = "",
)

@Serializable
data class NoteUpdateRequest(
    val title: String? = null,
    @SerialName("content_md")
    val contentMd: String? = null,
)

@Serializable
data class NoteImageDto(
    val id: String,
    @SerialName("note_id")
    val noteId: String,
    @SerialName("original_filename")
    val originalFilename: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("byte_size")
    val byteSize: Long,
    @SerialName("alt_text")
    val altText: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class NoteImageUploadResponse(
    val image: NoteImageDto,
    val token: String,
)

@Serializable
data class SignedUrlResponse(
    val url: String,
    @SerialName("expires_in")
    val expiresIn: Long = 3600,
)
