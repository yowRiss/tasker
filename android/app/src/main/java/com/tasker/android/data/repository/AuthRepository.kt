package com.tasker.android.data.repository

import com.tasker.android.data.local.TokenStore
import com.tasker.android.remote.api.AuthApi
import com.tasker.android.remote.dto.LoginRequest
import com.tasker.android.remote.dto.RegisterRequest
import com.tasker.android.remote.toUserFriendlyMessage
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    /**
     * Perform login against POST /v1/auth/login.
     * If main API is down, checks local encrypted credentials for offline login.
     */
    suspend fun login(
        username: String,
        password: String,
        rememberMe: Boolean = false,
    ): Result<Unit> = runCatching {
        val cleanUsername = username.trim()
        try {
            val response = authApi.login(
                LoginRequest(
                    username   = cleanUsername,
                    password   = password,
                    rememberMe = rememberMe,
                )
            )
            tokenStore.saveToken(response.token)
            tokenStore.saveUser(response.user.id, response.user.username)
            tokenStore.saveLocalCredential(cleanUsername, password)
        } catch (e: Exception) {
            if (isNetworkOrServerError(e)) {
                // Main API is down — attempt local offline authentication
                if (tokenStore.verifyLocalCredential(cleanUsername, password)) {
                    val offlineUserId = tokenStore.getUserId() ?: "offline_$cleanUsername"
                    val offlineToken = tokenStore.getToken() ?: "offline_token_${UUID.randomUUID()}"
                    tokenStore.saveToken(offlineToken)
                    tokenStore.saveUser(offlineUserId, cleanUsername)
                    return@runCatching
                } else {
                    throw Exception("Main API is unreachable and no matching local credentials were found.")
                }
            }
            throw e
        }
    }.recoverCatching { error ->
        throw Exception(error.toUserFriendlyMessage())
    }

    /**
     * Register a new account.
     * If the main API is UP: registers with server, saves credentials locally, and logs in.
     * If the main API is DOWN: saves credentials locally, creates an offline session,
     * and queues the credentials to sync when the API is available.
     */
    suspend fun register(
        username: String,
        password: String,
        rememberMe: Boolean = false,
    ): Result<Unit> = runCatching {
        val cleanUsername = username.trim()
        try {
            val response = authApi.register(
                RegisterRequest(
                    username   = cleanUsername,
                    password   = password,
                    rememberMe = rememberMe,
                )
            )
            tokenStore.saveToken(response.token)
            tokenStore.saveUser(response.user.id, response.user.username)
            tokenStore.saveLocalCredential(cleanUsername, password)
            tokenStore.clearPendingRegistration()
        } catch (e: Exception) {
            if (isNetworkOrServerError(e)) {
                // Main API is down — register offline & save for sync
                val offlineUserId = "offline_$cleanUsername"
                val offlineToken = "offline_token_${UUID.randomUUID()}"

                tokenStore.saveLocalCredential(cleanUsername, password)
                tokenStore.savePendingRegistration(cleanUsername, password)
                tokenStore.saveToken(offlineToken)
                tokenStore.saveUser(offlineUserId, cleanUsername)
                return@runCatching
            }
            throw e
        }
    }.recoverCatching { error ->
        throw Exception(error.toUserFriendlyMessage())
    }

    /**
     * Sync pending offline registration credentials to the main API when it comes back UP.
     */
    suspend fun syncPendingCredentials(): Result<Unit> = runCatching {
        val pending = tokenStore.getPendingRegistration() ?: return@runCatching
        val (username, password) = pending

        try {
            val response = authApi.register(
                RegisterRequest(
                    username   = username,
                    password   = password,
                    rememberMe = true,
                )
            )
            tokenStore.saveToken(response.token)
            tokenStore.saveUser(response.user.id, response.user.username)
            tokenStore.saveLocalCredential(username, password)
            tokenStore.clearPendingRegistration()
        } catch (e: Exception) {
            if (e is HttpException && (e.code() == 400 || e.code() == 409)) {
                // User may already be created on server — attempt login to get server token
                try {
                    val loginResp = authApi.login(LoginRequest(username, password, rememberMe = true))
                    tokenStore.saveToken(loginResp.token)
                    tokenStore.saveUser(loginResp.user.id, loginResp.user.username)
                    tokenStore.saveLocalCredential(username, password)
                    tokenStore.clearPendingRegistration()
                } catch (_: Exception) {
                    // Ignore login error, keep pending state
                }
            } else if (!isNetworkOrServerError(e)) {
                // Permanent failure — clear pending
                tokenStore.clearPendingRegistration()
            }
        }
    }

    fun logout() {
        tokenStore.clear()
    }

    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    fun getCurrentUserId(): String? = tokenStore.getUserId()

    fun getCurrentUsername(): String? = tokenStore.getUsername()

    private fun isNetworkOrServerError(e: Throwable): Boolean {
        return e is IOException || (e is HttpException && e.code() >= 500)
    }
}
