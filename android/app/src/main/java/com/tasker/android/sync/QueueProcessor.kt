package com.tasker.android.sync

import com.tasker.android.data.local.dao.NoteImageDao
import com.tasker.android.data.local.dao.SyncQueueDao
import com.tasker.android.remote.AuthEventBus
import com.tasker.android.remote.api.MoneyApi
import com.tasker.android.remote.api.NoteApi
import com.tasker.android.remote.api.TaskApi
import com.tasker.android.remote.dto.AccountCreateRequest
import com.tasker.android.remote.dto.BudgetCreateRequest
import com.tasker.android.remote.dto.CategoryCreateRequest
import com.tasker.android.remote.dto.CompletionRequest
import com.tasker.android.remote.dto.NoteCreateRequest
import com.tasker.android.remote.dto.NoteUpdateRequest
import com.tasker.android.remote.dto.ProjectCreateRequest
import com.tasker.android.remote.dto.TagCreateRequest
import com.tasker.android.remote.dto.TaskCreateRequest
import com.tasker.android.remote.dto.TaskUpdateRequest
import com.tasker.android.remote.dto.TransactionCreateRequest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
// asConfigFile does not exist — asRequestBody (line below) is the correct extension
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────
//  Supporting types for processQueue() return value
// ─────────────────────────────────────────────────────────────────
enum class PauseReason { AUTH_ERROR, TRANSIENT_ERROR }

data class QueueProcessorResult(
    val processedCount: Int = 0,
    val failedCount: Int = 0,
    val pauseReason: PauseReason? = null,
)

@Singleton
class QueueProcessor @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val taskApi: TaskApi,
    private val noteApi: NoteApi,
    private val moneyApi: MoneyApi,
    private val noteImageDao: NoteImageDao,
    private val idRemapper: IdRemapper,
    private val authEventBus: AuthEventBus,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun processQueue(): QueueProcessorResult {
        val pendingItems = syncQueueDao.getPending()
        if (pendingItems.isEmpty()) return QueueProcessorResult()

        var processed = 0
        var failed = 0

        for (item in pendingItems) {
            syncQueueDao.updateStatus(item.id, "processing", null)
            try {
                val serverReturnedId = executeMutation(item)
                if (serverReturnedId != null && serverReturnedId != item.entityId) {
                    idRemapper.remapId(item.entityType, item.entityId, serverReturnedId)
                }
                syncQueueDao.delete(item.id)
                processed++
            } catch (e: HttpException) {
                when {
                    e.code() == 401 -> {
                        syncQueueDao.updateStatus(item.id, "pending", e.message)
                        authEventBus.postLogout()
                        return QueueProcessorResult(processed, failed, PauseReason.AUTH_ERROR)
                    }
                    e.code() >= 500 -> {
                        syncQueueDao.updateStatus(item.id, "pending", e.message)
                        return QueueProcessorResult(processed, failed, PauseReason.TRANSIENT_ERROR)
                    }
                    else -> {
                        syncQueueDao.updateStatus(item.id, "failed", e.message)
                        failed++
                    }
                }
            } catch (e: IOException) {
                syncQueueDao.updateStatus(item.id, "pending", e.message)
                return QueueProcessorResult(processed, failed, PauseReason.TRANSIENT_ERROR)
            } catch (e: Exception) {
                syncQueueDao.updateStatus(item.id, "failed", e.message)
                failed++
            }
        }

        return QueueProcessorResult(processedCount = processed, failedCount = failed)
    }

    private suspend fun executeMutation(item: com.tasker.android.data.local.entity.SyncQueueEntity): String? {
        return when (item.entityType) {
            "task" -> when (item.operation) {
                "CREATE" -> taskApi.createTask(json.decodeFromString<TaskCreateRequest>(item.payload)).id
                "UPDATE" -> {
                    if (item.payload.contains("\"completed\"")) {
                        taskApi.toggleCompletion(item.entityId, json.decodeFromString<CompletionRequest>(item.payload)).id
                    } else {
                        taskApi.updateTask(item.entityId, json.decodeFromString<TaskUpdateRequest>(item.payload)).id
                    }
                }
                "DELETE" -> { taskApi.deleteTask(item.entityId); null }
                else -> null
            }
            "project" -> when (item.operation) {
                "CREATE" -> taskApi.createProject(json.decodeFromString<ProjectCreateRequest>(item.payload)).id
                "UPDATE" -> taskApi.updateProject(item.entityId, json.decodeFromString<ProjectCreateRequest>(item.payload)).id
                "DELETE" -> { taskApi.deleteProject(item.entityId); null }
                else -> null
            }
            "tag" -> when (item.operation) {
                "CREATE" -> taskApi.createTag(json.decodeFromString<TagCreateRequest>(item.payload)).id
                "UPDATE" -> taskApi.updateTag(item.entityId, json.decodeFromString<TagCreateRequest>(item.payload)).id
                "DELETE" -> { taskApi.deleteTag(item.entityId); null }
                else -> null
            }
            "note" -> when (item.operation) {
                "CREATE" -> noteApi.createNote(json.decodeFromString<NoteCreateRequest>(item.payload)).id
                "UPDATE" -> noteApi.updateNote(item.entityId, json.decodeFromString<NoteUpdateRequest>(item.payload)).id
                "DELETE" -> { noteApi.deleteNote(item.entityId); null }
                else -> null
            }
            "note_image" -> when (item.operation) {
                "UPLOAD_IMAGE" -> {
                    val map = json.decodeFromString<Map<String, String>>(item.payload)
                    val fallbackNoteId = map["note_id"] ?: return null
                    val localUri = map["local_uri"] ?: return null
                    val file = File(localUri)
                    if (!file.exists()) return null

                    val imageEntity = noteImageDao.getById(item.entityId)
                    val noteId = imageEntity?.noteId?.takeIf { it.isNotBlank() } ?: fallbackNoteId
                    if (noteId.isBlank()) return null

                    val mimeType = imageEntity?.mimeType ?: "image/jpeg"
                    val reqFile = file.asRequestBody(mimeType.toMediaType())
                    val body = MultipartBody.Part.createFormData("file", file.name, reqFile)
                    val res = noteApi.uploadImage(noteId, body)
                    noteImageDao.updateSyncStatus(item.entityId, "uploaded", res.image.id)
                    res.image.id
                }
                else -> null
            }
            "account" -> when (item.operation) {
                "CREATE" -> moneyApi.createAccount(json.decodeFromString<AccountCreateRequest>(item.payload)).id
                "DELETE" -> { moneyApi.deleteAccount(item.entityId); null }
                else -> null
            }
            "category" -> when (item.operation) {
                "CREATE" -> moneyApi.createCategory(json.decodeFromString<CategoryCreateRequest>(item.payload)).id
                "DELETE" -> { moneyApi.deleteCategory(item.entityId); null }
                else -> null
            }
            "transaction" -> when (item.operation) {
                "CREATE" -> moneyApi.createTransaction(json.decodeFromString<TransactionCreateRequest>(item.payload)).id
                "DELETE" -> { moneyApi.deleteTransaction(item.entityId); null }
                else -> null
            }
            "transaction_receipt" -> when (item.operation) {
                "UPLOAD_RECEIPT" -> {
                    val map = json.decodeFromString<Map<String, String>>(item.payload)
                    val txId = map["transaction_id"] ?: return null
                    val localUri = map["local_uri"] ?: return null
                    val file = File(localUri)
                    if (!file.exists()) return null

                    val reqFile = file.asRequestBody("image/jpeg".toMediaType())
                    val body = MultipartBody.Part.createFormData("file", file.name, reqFile)
                    moneyApi.uploadReceipt(txId, body)
                    null
                }
                else -> null
            }
            "budget" -> when (item.operation) {
                "CREATE" -> moneyApi.createBudget(json.decodeFromString<BudgetCreateRequest>(item.payload)).id
                "DELETE" -> { moneyApi.deleteBudget(item.entityId); null }
                else -> null
            }
            else -> null
        }
    }
}
