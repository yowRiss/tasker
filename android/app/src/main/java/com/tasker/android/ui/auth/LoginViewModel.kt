package com.tasker.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.local.ApiHostStore
import com.tasker.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val apiHost: String = "",
    val showServerSettings: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiHostStore: ApiHostStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(apiHost = apiHostStore.getHost()) }
    }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onRememberMeChange(value: Boolean) = _uiState.update { it.copy(rememberMe = value) }
    fun onApiHostChange(value: String) {
        _uiState.update { it.copy(apiHost = value) }
        apiHostStore.setHost(value)
    }
    fun onToggleServerSettings() = _uiState.update { it.copy(showServerSettings = !it.showServerSettings) }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username and password are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(
                username   = state.username.trim(),
                password   = state.password,
                rememberMe = state.rememberMe,
            )
            result.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, isLoggedIn = true) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = error.message ?: "Login failed",
                        )
                    }
                },
            )
        }
    }
}
