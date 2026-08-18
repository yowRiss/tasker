package com.tasker.android.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tasker.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("tasker_update_prefs", Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var currentReleaseInfo: ReleaseInfo? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun checkForUpdates(isAutoCheck: Boolean = false) {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) {
            return
        }

        _updateState.value = UpdateState.Checking

        withContext(Dispatchers.IO) {
            try {
                val repo = BuildConfig.GITHUB_REPO
                val url = "https://api.github.com/repos/$repo/releases/latest"

                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Tasker-Android-App")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    if (!isAutoCheck) {
                        _updateState.value = UpdateState.Error("HTTP ${response.code}: Could not fetch updates")
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                    return@withContext
                }

                val responseBody = response.body?.string() ?: ""
                if (responseBody.isBlank()) {
                    if (!isAutoCheck) _updateState.value = UpdateState.Error("Empty release response")
                    else _updateState.value = UpdateState.Idle
                    return@withContext
                }

                val json = JSONObject(responseBody)
                val tagName = json.optString("tag_name", "")
                val title = json.optString("name", tagName)
                val publishedAt = json.optString("published_at", "")
                val releaseNotes = json.optString("body", "")
                val assetsArray = json.optJSONArray("assets")

                var apkDownloadUrl: String? = null
                var apkFileName: String? = null

                if (assetsArray != null) {
                    for (i in 0 until assetsArray.length()) {
                        val asset = assetsArray.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            apkFileName = name
                            if (name.contains("release", ignoreCase = true)) {
                                break
                            }
                        }
                    }
                }

                if (apkDownloadUrl.isNullOrBlank()) {
                    if (!isAutoCheck) _updateState.value = UpdateState.Error("No APK release asset found")
                    else _updateState.value = UpdateState.Idle
                    return@withContext
                }

                val releaseInfo = ReleaseInfo(
                    tagName = tagName,
                    title = if (title.isBlank()) tagName else title,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkDownloadUrl,
                    apkName = apkFileName ?: "tasker-release.apk",
                    publishedAt = publishedAt
                )

                currentReleaseInfo = releaseInfo

                val installedTag = prefs.getString("last_installed_tag", null) ?: "v${BuildConfig.VERSION_NAME}"
                val isNewer = isTagNewer(latestTag = tagName, installedTag = installedTag)

                if (isNewer) {
                    _updateState.value = UpdateState.UpdateAvailable(releaseInfo)
                    if (isAutoCheck) {
                        // Automatically start download when new release is detected
                        startDownload()
                    }
                } else {
                    _updateState.value = if (isAutoCheck) UpdateState.Idle else UpdateState.UpToDate
                }

            } catch (e: Exception) {
                if (!isAutoCheck) {
                    _updateState.value = UpdateState.Error("Update check failed: ${e.localizedMessage ?: "Unknown error"}")
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    suspend fun startDownload() {
        val releaseInfo = currentReleaseInfo ?: run {
            _updateState.value = UpdateState.Error("No release info available for download")
            return
        }

        _updateState.value = UpdateState.Downloading(releaseInfo, 0)

        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(releaseInfo.downloadUrl)
                    .header("User-Agent", "Tasker-Android-App")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    _updateState.value = UpdateState.Error("Download failed (HTTP ${response.code})")
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    _updateState.value = UpdateState.Error("Download body was empty")
                    return@withContext
                }

                val totalBytes = body.contentLength()
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val apkFile = File(downloadDir, "tasker-update-${releaseInfo.tagName}.apk")

                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(8192)
                var bytesCopied: Long = 0
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                    val progress = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                    _updateState.value = UpdateState.Downloading(releaseInfo, progress)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                _updateState.value = UpdateState.ReadyToInstall(releaseInfo, apkFile)

                // Auto-launch APK installer
                withContext(Dispatchers.Main) {
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Download error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) {
            _updateState.value = UpdateState.Error("APK file not found: ${apkFile.absolutePath}")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    "Please enable 'Allow from this source' for Tasker to install updates",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            currentReleaseInfo?.let { release ->
                prefs.edit().putString("last_installed_tag", release.tagName).apply()
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Failed to launch APK installer: ${e.localizedMessage}")
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    private fun isTagNewer(latestTag: String, installedTag: String): Boolean {
        if (latestTag.isBlank()) return false
        if (installedTag.isBlank() || installedTag != latestTag) return true
        return false
    }
}
