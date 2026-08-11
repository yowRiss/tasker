package com.tasker.android.remote

import com.tasker.android.BuildConfig
import com.tasker.android.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
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
        val request = chain.request().newBuilder()
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        val response = chain.proceed(request)
        if (response.code == 401) {
            tokenStore.clear()
            authEventBus.postLogout()
        }
        return response
    }
}

/** Shared JSON configuration — lenient to handle backend variations. */
val ApiJson = Json {
    ignoreUnknownKeys    = true
    isLenient            = true
    encodeDefaults       = false
    explicitNulls        = false
    coerceInputValues    = true
}

/** Factory function — called by DI module only. */
fun buildOkHttpClient(authInterceptor: AuthInterceptor, debug: Boolean): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
        .addConverterFactory(ApiJson.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()
