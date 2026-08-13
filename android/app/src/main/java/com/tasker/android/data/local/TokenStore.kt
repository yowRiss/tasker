package com.tasker.android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences (Jetpack Security).
 * The JWT from POST /v1/auth/login is stored here.
 * No sensitive data goes into plain SharedPreferences.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_FILE  = "tasker_secure_prefs"
        private const val KEY_TOKEN   = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PENDING_REG_USERNAME = "pending_reg_username"
        private const val KEY_PENDING_REG_PASSWORD = "pending_reg_password"
        private const val PREFIX_LOCAL_CRED = "cred_"
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveUser(userId: String, username: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun isLoggedIn(): Boolean = getToken() != null

    // ── Offline local authentication & credential sync ──────────────────

    fun saveLocalCredential(username: String, password: String) {
        prefs.edit()
            .putString("$PREFIX_LOCAL_CRED$username", password)
            .apply()
    }

    fun verifyLocalCredential(username: String, password: String): Boolean {
        val storedPassword = prefs.getString("$PREFIX_LOCAL_CRED$username", null)
        return storedPassword != null && storedPassword == password
    }

    fun savePendingRegistration(username: String, password: String) {
        prefs.edit()
            .putString(KEY_PENDING_REG_USERNAME, username)
            .putString(KEY_PENDING_REG_PASSWORD, password)
            .apply()
    }

    fun getPendingRegistration(): Pair<String, String>? {
        val u = prefs.getString(KEY_PENDING_REG_USERNAME, null)
        val p = prefs.getString(KEY_PENDING_REG_PASSWORD, null)
        return if (!u.isNullOrEmpty() && !p.isNullOrEmpty()) Pair(u, p) else null
    }

    fun clearPendingRegistration() {
        prefs.edit()
            .remove(KEY_PENDING_REG_USERNAME)
            .remove(KEY_PENDING_REG_PASSWORD)
            .apply()
    }

    fun isOfflineSession(): Boolean {
        val token = getToken()
        return token != null && token.startsWith("offline_token_")
    }

    fun clear() = prefs.edit().clear().apply()
}
