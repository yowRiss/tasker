package com.tasker.android.ui.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.CreateNoteInput
import com.tasker.android.data.model.NoteImage
import com.tasker.android.data.model.UpdateNoteInput
import com.tasker.android.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailUiState(
    val noteId: String? = null,
    val title: String = "",
    val contentMd: String = "",
    val images: List<NoteImage> = emptyList(),
    val isPreviewMode: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    fun initialize(noteId: String?) {
        if (noteId == null || noteId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, noteId = noteId) }
            val note = noteRepository.getNote(noteId)
            if (note != null) {
                _uiState.update {
                    it.copy(
                        noteId = note.id,
                        title = note.title,
                        contentMd = note.contentMd,
                        images = note.images,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Note not found") }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun onContentChange(value: String) {
        val oldText = _uiState.value.contentMd
        val updated = processAutoList(oldText, value)
        _uiState.update { it.copy(contentMd = updated) }
    }

    private fun processAutoList(oldText: String, newText: String): String {
        if (newText.length == oldText.length + 1 && newText.endsWith("\n")) {
            val lines = oldText.split("\n")
            val lastLine = lines.lastOrNull() ?: ""

            val bulletMatch = Regex("^(\\s*-\\s+)(.*)$").find(lastLine)
            if (bulletMatch != null) {
                val indentAndDash = bulletMatch.groupValues[1]
                val rest = bulletMatch.groupValues[2].trim()

                if (rest.isEmpty()) {
                    val prefixLines = lines.dropLast(1)
                    return if (prefixLines.isNotEmpty()) prefixLines.joinToString("\n") + "\n" else ""
                } else {
                    return "$newText$indentAndDash"
                }
            }
        }
        return newText
    }

    fun insertMathTemplate() {
        _uiState.update { state ->
            val snippet = "\n$$ f(x) = x^2 $$\n"
            val newContent = if (state.contentMd.isBlank()) snippet.trim() else "${state.contentMd}$snippet"
            state.copy(contentMd = newContent)
        }
    }

    fun togglePreviewMode() = _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentNoteId = _uiState.value.noteId
            val targetNoteId = if (currentNoteId == null) {
                val currentTitle = _uiState.value.title.ifBlank { "Untitled Note" }
                val created = noteRepository.createNote(
                    CreateNoteInput(
                        title = currentTitle,
                        contentMd = _uiState.value.contentMd
                    )
                )
                _uiState.update { it.copy(noteId = created.id, title = created.title) }
                created.id
            } else {
                currentNoteId
            }
            attachImageToNote(targetNoteId, uri)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun attachImageToNote(noteId: String, uri: Uri) {
        val attached = noteRepository.attachImage(noteId, uri)
        if (attached != null) {
            _uiState.update { state ->
                val imageToken = "![image](note-image:${attached.id})"
                val updatedMd = when {
                    state.contentMd.contains(attached.id) -> state.contentMd
                    state.contentMd.isBlank() -> imageToken
                    else -> "${state.contentMd}\n\n$imageToken"
                }
                viewModelScope.launch {
                    noteRepository.updateNote(noteId, UpdateNoteInput(contentMd = updatedMd))
                }
                state.copy(
                    images = state.images + attached,
                    contentMd = updatedMd
                )
            }
        }
    }

    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            noteRepository.deleteNoteImage(imageId)
            val currentNoteId = _uiState.value.noteId
            _uiState.update { state ->
                val updatedImages = state.images.filter { it.id != imageId }
                val tokenRegex = Regex("!\\[.*?\\]\\(note-image:$imageId\\)|note-image:$imageId")
                val updatedContent = state.contentMd.replace(tokenRegex, "").trim()

                if (currentNoteId != null && updatedContent != state.contentMd) {
                    viewModelScope.launch {
                        noteRepository.updateNote(currentNoteId, UpdateNoteInput(contentMd = updatedContent))
                    }
                }

                state.copy(
                    images = updatedImages,
                    contentMd = updatedContent
                )
            }
        }
    }

    fun saveNote() {
        saveNoteInternal {
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun saveNoteInternal(onSuccess: suspend (String) -> Unit = {}) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (state.noteId == null) {
                val created = noteRepository.createNote(
                    CreateNoteInput(
                        title = state.title.trim(),
                        contentMd = state.contentMd
                    )
                )
                _uiState.update { it.copy(noteId = created.id, isLoading = false) }
                onSuccess(created.id)
            } else {
                noteRepository.updateNote(
                    state.noteId,
                    UpdateNoteInput(
                        title = state.title.trim(),
                        contentMd = state.contentMd
                    )
                )
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(state.noteId)
            }
        }
    }

    fun deleteNote() {
        val noteId = _uiState.value.noteId ?: return
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
