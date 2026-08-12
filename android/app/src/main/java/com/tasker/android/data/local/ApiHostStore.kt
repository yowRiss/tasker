package com.tasker.android.data.local

import android.content.Context
import com.tasker.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiHostStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_FILE = "tasker_api_config"
        private const val KEY_HOST = "api_host"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    fun getHost(): String {
        val saved = prefs.getString(KEY_HOST, null)
        val host = if (saved.isNullOrBlank()) BuildConfig.API_BASE_URL else saved
        return normalizeUrlScheme(host)
    }

    fun setHost(url: String) {
        val normalized = normalizeUrlScheme(url.trim().removeSuffix("/"))
        prefs.edit().putString(KEY_HOST, normalized).apply()
    }

    private fun normalizeUrlScheme(url: String): String {
        if (url.isBlank()) return url
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            return url
        }
        return "https://$url"
    }

    fun isCustomHost(): Boolean = !prefs.getString(KEY_HOST, null).isNullOrBlank()

    fun resetToDefault() = prefs.edit().remove(KEY_HOST).apply()
}
