package com.tasker.android.remote.api

import com.tasker.android.remote.dto.LoginRequest
import com.tasker.android.remote.dto.LoginResponse
import com.tasker.android.remote.dto.MeResponse
import com.tasker.android.remote.dto.RegisterRequest
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApi {

    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    @GET("v1/me")
    suspend fun me(): MeResponse

    @PATCH("v1/auth/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)
}

// ── DTOs (auth-specific, defined inline for brevity) ──────────────

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)
