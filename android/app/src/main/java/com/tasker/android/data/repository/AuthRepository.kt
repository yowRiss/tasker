package com.tasker.android.data.repository

import com.tasker.android.data.local.TokenStore
import com.tasker.android.remote.api.AuthApi
import com.tasker.android.remote.dto.LoginRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    /**
     * Perform login against POST /v1/auth/login.
     * On success, stores the JWT in EncryptedSharedPreferences.
     */
    suspend fun login(
        username: String,
        password: String,
        rememberMe: Boolean = false,
    ): Result<Unit> = runCatching {
        val response = authApi.login(
            LoginRequest(
                username   = username,
                password   = password,
                rememberMe = rememberMe,
            )
        )
        tokenStore.saveToken(response.token)
        tokenStore.saveUser(response.user.id, response.user.username)
    }.recoverCatching { error ->
        if (error is retrofit2.HttpException) {
            val errorJson = error.response()?.errorBody()?.string()
            if (!errorJson.isNullOrBlank()) {
                try {
                    val problem = com.tasker.android.remote.ApiJson.decodeFromString<com.tasker.android.remote.dto.ProblemResponse>(errorJson)
                    if (problem.title.isNotBlank()) {
                        throw Exception(problem.title)
                    }
                } catch (e: Exception) {
                    if (e !is retrofit2.HttpException && e.message != null && e.message != error.message) {
                        throw e
                    }
                }
            }
        }
        throw error
    }

    fun logout() {
        tokenStore.clear()
    }

    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    fun getCurrentUserId(): String? = tokenStore.getUserId()

    fun getCurrentUsername(): String? = tokenStore.getUsername()
}
