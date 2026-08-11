package com.tasker.android.data.model

data class NoteImage(
    val id: String,
    val noteId: String,
    val localUri: String,
    val objectPath: String? = null,
    val originalFilename: String,
    val mimeType: String,
    val byteSize: Long,
    val altText: String? = null,
    val syncStatus: String = "pending",
)

data class Note(
    val id: String,
    val title: String,
    val contentMd: String = "",
    val tags: List<Tag> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val images: List<NoteImage> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

data class CreateNoteInput(
    val title: String,
    val contentMd: String = "",
    val tagIds: List<String> = emptyList(),
    val taskIds: List<String> = emptyList(),
)

data class UpdateNoteInput(
    val title: String? = null,
    val contentMd: String? = null,
    val tagIds: List<String>? = null,
    val taskIds: List<String>? = null,
)
