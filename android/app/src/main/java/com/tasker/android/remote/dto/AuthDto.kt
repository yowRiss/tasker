package com.tasker.android.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Auth DTOs ─────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    @SerialName("remember_me")
    val rememberMe: Boolean = false,
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
)

@Serializable
data class MeResponse(
    val id: String,
    val username: String,
)

// ── Generic list wrapper ──────────────────────────────────────────

@Serializable
data class ItemsResponse<T>(
    val items: List<T> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null,
)

// ── Error response (RFC 7807 problem+json) ─────────────────────────

@Serializable
data class ProblemResponse(
    val type: String = "",
    val title: String = "",
    val status: Int = 0,
    val detail: Map<String, String> = emptyMap(),
)
