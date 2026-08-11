package com.tasker.android.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthEvent {
    data object LoggedOut : AuthEvent
}

/**
 * Application-scoped event bus for auth state changes.
 * AuthInterceptor posts here on 401; MainActivity/NavGraph observes and redirects to login.
 */
@Singleton
class AuthEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun postLogout() {
        _events.tryEmit(AuthEvent.LoggedOut)
    }
}
