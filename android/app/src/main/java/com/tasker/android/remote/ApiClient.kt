package com.tasker.android.remote

import com.tasker.android.BuildConfig
import com.tasker.android.data.local.ApiHostStore
import com.tasker.android.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth interceptor — attaches Bearer token to every request.
 * On 401: clears the token and posts a logout event via [AuthEventBus].
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val authEventBus: AuthEventBus,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = tokenStore.getToken()
        val isOfflineToken = token != null && (token.startsWith("offline_token_") || tokenStore.isOfflineSession())

        val requestBuilder = chain.request().newBuilder()
        if (token != null && !isOfflineToken) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())
        if (response.code == 401 && !isOfflineToken && tokenStore.getPendingRegistration() == null) {
            tokenStore.clear()
            authEventBus.postLogout()
        }
        return response
    }
}

@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val apiHostStore: ApiHostStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val hostInput = apiHostStore.getHost().trim()
        if (hostInput.isBlank()) return chain.proceed(original)

        val normalizedHost = when {
            hostInput.startsWith("http://", ignoreCase = true) || hostInput.startsWith("https://", ignoreCase = true) -> hostInput
            else -> "https://$hostInput"
        }
        val formattedHost = if (normalizedHost.endsWith("/")) normalizedHost else "$normalizedHost/"
        val newBaseUrl = formattedHost.toHttpUrlOrNull() ?: return chain.proceed(original)

        val newUrlBuilder = original.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)

        val basePathSegments = newBaseUrl.pathSegments.filter { it.isNotEmpty() }
        if (basePathSegments.isNotEmpty()) {
            val originalPathSegments = original.url.pathSegments
            newUrlBuilder.encodedPath("")
            for (segment in basePathSegments) {
                newUrlBuilder.addPathSegment(segment)
            }
            for (segment in originalPathSegments) {
                newUrlBuilder.addPathSegment(segment)
            }
        }

        val newRequest = original.newBuilder().url(newUrlBuilder.build()).build()
        return chain.proceed(newRequest)
    }
}

/** Shared JSON configuration — lenient to handle backend variations. */
val ApiJson = Json {
    ignoreUnknownKeys    = true
    isLenient            = true
    encodeDefaults       = true
    explicitNulls        = false
    coerceInputValues    = true
}

/** Factory function — called by DI module only. */
fun buildOkHttpClient(
    authInterceptor: AuthInterceptor,
    baseUrlInterceptor: BaseUrlInterceptor,
    debug: Boolean,
): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .apply {
            if (debug) addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        .build()

fun buildRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()
