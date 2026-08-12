package com.tasker.android.remote

import com.tasker.android.remote.dto.ProblemResponse
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps raw throwables (HttpException, SerializationException, Network Exceptions)
 * into clean, user-friendly error messages for UI display.
 */
fun Throwable.toUserFriendlyMessage(): String {
    if (this is HttpException) {
        val httpCode = code()
        val errorBodyStr = response()?.errorBody()?.string()?.trim()
        if (!errorBodyStr.isNullOrBlank() && errorBodyStr.startsWith("{") && errorBodyStr.endsWith("}")) {
            try {
                val problem = ApiJson.decodeFromString<ProblemResponse>(errorBodyStr)
                if (problem.title.isNotBlank()) {
                    return problem.title
                }
            } catch (_: Exception) {
                // Non-standard JSON body; fallback to HTTP status code message
            }
        }
        return when (httpCode) {
            400 -> "Bad request (HTTP 400) — please check input fields and server URL"
            401 -> "Invalid username or password (HTTP 401)"
            403 -> "Access forbidden (HTTP 403)"
            404 -> "API endpoint not found (HTTP 404) — please check server URL"
            502 -> "Bad Gateway (HTTP 502) — backend server is down or unreachable"
            503 -> "Service Unavailable (HTTP 503) — server is temporarily overloaded"
            504 -> "Gateway Timeout (HTTP 504) — server took too long to respond"
            else -> "Server error (HTTP $httpCode)"
        }
    }
    if (this is SerializationException) {
        return "Invalid server response format — server returned non-JSON response"
    }
    if (this is UnknownHostException || this is ConnectException) {
        return "Unable to connect to server — check network connection or server URL"
    }
    if (this is SocketTimeoutException) {
        return "Connection timed out — server did not respond in time"
    }
    return message?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred"
}
