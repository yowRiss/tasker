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
    fun onContentChange(value: String) = _uiState.update { it.copy(contentMd = value) }
    fun togglePreviewMode() = _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }

    fun attachImage(uri: Uri) {
        val noteId = _uiState.value.noteId
        viewModelScope.launch {
            if (noteId == null) {
                // First save note to get an ID
                saveNoteInternal { savedNoteId ->
                    attachImageToNote(savedNoteId, uri)
                }
            } else {
                attachImageToNote(noteId, uri)
            }
        }
    }

    private suspend fun attachImageToNote(noteId: String, uri: Uri) {
        val attached = noteRepository.attachImage(noteId, uri)
        if (attached != null) {
            _uiState.update { state ->
                state.copy(images = state.images + attached)
            }
        }
    }

    fun saveNote() {
        saveNoteInternal {
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun saveNoteInternal(onSuccess: (String) -> Unit = {}) {
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
