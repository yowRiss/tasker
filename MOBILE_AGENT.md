# Tasker Android — Agent Guide (Kotlin/Jetpack Compose)

> Companion to `MOBILE_PRD.md`. Read that document first for full feature scope, data
> model, API contract, and sync protocol. This guide covers implementation conventions,
> module structure, library choices, and rules agents must follow when writing or
> modifying the Kotlin codebase.

---

## Stack

| Concern              | Library / API                                    | Notes                               |
|---------------------|--------------------------------------------------|-------------------------------------|
| Language             | Kotlin (JVM target 17)                           |                                     |
| UI                   | Jetpack Compose + Material 3                     | No legacy View system               |
| Navigation           | Navigation Compose                               | Bottom nav + nested graphs          |
| DI                   | Hilt                                             | Minimal — only what's needed        |
| Local DB             | Room (SQLite)                                    | WAL mode; FK enforcement ON         |
| Networking           | Retrofit 2 + OkHttp 4                           |                                     |
| JSON                 | Kotlin Serialization (`kotlinx.serialization`)   | Not Gson/Moshi                      |
| Image loading        | Coil 3 (Compose extension)                       | Lighter than Glide for Compose      |
| Camera               | CameraX (`ImageCapture` use-case)                |                                     |
| Background sync      | WorkManager                                      | Periodic + one-shot                 |
| Coroutines           | Kotlin Coroutines + Flow                         | Flows from Room, StateFlow in VMs   |
| Secure storage       | `EncryptedSharedPreferences` (Jetpack Security)  |                                     |
| Connectivity         | `ConnectivityManager.NetworkCallback`            |                                     |
| Build                | Gradle (Kotlin DSL), R8 / ProGuard enabled       |                                     |
| Min SDK              | 29 (Android 10)                                 |                                     |

**Library justifications:**
- **Coil over Glide**: Coil is Kotlin-first, Coroutine-native, and has a Compose
  extension that avoids the View-layer interop cost Glide requires in Compose contexts.
  Smaller binary contribution for equivalent functionality.
- **Kotlin Serialization over Gson**: No reflection at runtime; compatible with R8
  full-mode shrinking without additional keep rules.
- **Compose Canvas for charts**: Avoids pulling in a charting library (MPAndroidChart
  ~1 MB, Vico ~500 KB) for the basic bar/line visuals needed on the dashboard.

---

## Build & CI/CD Environment Rules

> **IMPORTANT FOR AGENTS / AI ASSISTANTS**:
> - **No Local Builds**: Do NOT attempt to run Gradle builds (`./gradlew assembleDebug`, `./gradlew build`, etc.) on the local machine.
> - **GitHub CI/CD**: Android APK/AAB compilation and build verification are performed remotely in **GitHub Actions CI**.
> - **Agent Responsibility**: Focus on writing clean Kotlin code, verifying models/DAOs/UI logic, and updating contracts. GitHub CI handles actual build execution.

---

## Project Structure

```
android/                          ← new directory at repo root (alongside mobile/)
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/tasker/
│   │       ├── MainActivity.kt
│   │       ├── TaskerApp.kt            ← Application class (@HiltAndroidApp)
│   │       │
│   │       ├── ui/                     ← Compose screens only
│   │       │   ├── navigation/
│   │       │   │   ├── AppNavGraph.kt
│   │       │   │   └── BottomNavBar.kt
│   │       │   ├── theme/
│   │       │   │   ├── Color.kt
│   │       │   │   ├── Theme.kt
│   │       │   │   └── Type.kt
│   │       │   ├── components/         ← reusable Composables
│   │       │   ├── tasks/
│   │       │   │   ├── TaskListScreen.kt
│   │       │   │   ├── TaskDetailScreen.kt
│   │       │   │   └── CreateEditTaskSheet.kt
│   │       │   ├── notes/
│   │       │   ├── money/
│   │       │   └── settings/
│   │       │
│   │       ├── viewmodel/
│   │       │   ├── tasks/TaskListViewModel.kt
│   │       │   ├── tasks/TaskDetailViewModel.kt
│   │       │   ├── notes/...
│   │       │   ├── money/...
│   │       │   └── settings/SettingsViewModel.kt
│   │       │
│   │       ├── data/
│   │       │   ├── repository/
│   │       │   │   ├── TaskRepository.kt
│   │       │   │   ├── NoteRepository.kt
│   │       │   │   ├── MoneyRepository.kt
│   │       │   │   └── AuthRepository.kt
│   │       │   └── model/              ← domain model data classes (shared layer)
│   │       │       ├── Task.kt
│   │       │       ├── Note.kt
│   │       │       ├── Money.kt
│   │       │       └── Sync.kt
│   │       │
│   │       ├── local/                  ← Room
│   │       │   ├── AppDatabase.kt
│   │       │   ├── entity/             ← @Entity classes
│   │       │   └── dao/                ← @Dao interfaces
│   │       │
│   │       ├── remote/                 ← Retrofit
│   │       │   ├── ApiClient.kt        ← OkHttp + Retrofit setup
│   │       │   ├── AuthInterceptor.kt
│   │       │   ├── api/
│   │       │   │   ├── TaskApi.kt
│   │       │   │   ├── NoteApi.kt
│   │       │   │   ├── MoneyApi.kt
│   │       │   │   └── AuthApi.kt
│   │       │   └── dto/                ← request/response DTOs
│   │       │
│   │       ├── sync/
│   │       │   ├── NetworkMonitor.kt
│   │       │   ├── SyncManager.kt      ← orchestrator singleton
│   │       │   ├── QueueProcessor.kt   ← per-item push + ID remap
│   │       │   ├── PullSync.kt         ← per-entity pull
│   │       │   ├── IdRemapper.kt       ← atomic FK cascade remap
│   │       │   └── SyncWorker.kt       ← WorkManager worker
│   │       │
│   │       └── di/
│   │           ├── DatabaseModule.kt
│   │           ├── NetworkModule.kt
│   │           ├── RepositoryModule.kt
│   │           └── SyncModule.kt
│   │
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Architecture Rules

1. **Screens never call repositories or API directly** — only through ViewModels.
2. **ViewModels expose `StateFlow<UiState>`** — screens observe and render state.
3. **Repositories are the single source of truth** — always read from Room; write to
   Room first, then enqueue to `sync_queue`; never write remote-only.
4. **All mutations enqueue to `sync_queue`** within the same Room transaction as the
   local write. Never enqueue without a corresponding local write.
5. **Room DAOs return `Flow<List<T>>`** for list screens so the UI recomposes
   automatically when the DB changes.
6. **No business logic in Composables** — UI layer only handles rendering and user
   events; logic lives in ViewModels and repositories.
7. **No coroutine `GlobalScope`** — use `viewModelScope` in ViewModels,
   `applicationScope` (Hilt-provided) for sync operations that must outlive a ViewModel.

---

## Auth Flow

```
POST /v1/auth/login
Body: { "username": "...", "password": "...", "remember_me": false }
Response: { "token": "<jwt>", "user": { "id": "uuid", "username": "..." } }
```

1. `AuthRepository.login()` calls the API, stores `token` in `EncryptedSharedPreferences`.
2. `AuthInterceptor` (OkHttp) reads the token and attaches `Authorization: Bearer <token>`.
3. On 401 response: `AuthInterceptor` clears the token from storage and posts a
   `LOGOUT` event to an `AuthEventBus` (a `SharedFlow`). `MainActivity` observes this
   and navigates to the login screen.
4. No automatic token refresh (backend issues single long-lived tokens).

```kotlin
// AuthInterceptor skeleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val authEventBus: AuthEventBus,
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = tokenStore.getToken()
        val request = chain.request().newBuilder()
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        val response = chain.proceed(request)
        if (response.code == 401) {
            tokenStore.clear()
            authEventBus.emit(AuthEvent.LoggedOut)
        }
        return response
    }
}
```

---

## Database (Room)

### Configuration

```kotlin
@Database(
    entities = [
        ProjectEntity::class, TagEntity::class,
        TaskEntity::class, SubtaskEntity::class, TaskTagEntity::class,
        NoteEntity::class, NoteTagEntity::class, NoteTaskLinkEntity::class,
        NoteImageEntity::class,
        AccountEntity::class, CategoryEntity::class,
        TransactionEntity::class, TransactionReceiptEntity::class,
        BudgetEntity::class, RecurringTransactionEntity::class,
        SyncQueueEntity::class, SyncMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
```

Enable WAL mode and FK enforcement in database builder:
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "tasker.db")
    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
    .build()
    // FK enforcement: override fun onOpen(db: SupportSQLiteDatabase) {
    //     db.execSQL("PRAGMA foreign_keys=ON")
    // }
```

### Key Entity Patterns

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val status: String,        // open | completed | archived
    val completedAt: String?,
    val dueDate: String?,
    val priority: Int,         // 0–3
    val projectId: String?,
    val createdAt: String,
    val updatedAt: String,
    val isDeleted: Int = 0,    // 0 | 1
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = CASCADE),
    ]
)
data class TaskTagEntity(val taskId: String, val tagId: String, val createdAt: String)
```

### DAOs

Each module has one or more DAOs. Key patterns:

```kotlin
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: String)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status IN ('pending','failed') AND retryCount < 10 ORDER BY id ASC")
    suspend fun getPending(): List<SyncQueueEntity>

    @Insert
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_queue SET status = :status, lastError = :error, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String?)
}
```

---

## Networking (Retrofit)

```kotlin
// Retrofit API interface example
interface TaskApi {
    @GET("v1/tasks")
    suspend fun listTasks(
        @Query("status") status: String = "all",
        @Query("limit") limit: Int = 1000,
        @Query("project_id") projectId: String? = null,
        @Query("q") search: String? = null,
    ): TaskListResponse

    @POST("v1/tasks")
    suspend fun createTask(@Body body: TaskCreateRequest): TaskResponse

    @PATCH("v1/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: TaskUpdateRequest): TaskResponse

    @DELETE("v1/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>

    @PATCH("v1/tasks/{taskId}/completion")
    suspend fun toggleCompletion(
        @Path("taskId") taskId: String,
        @Body body: CompletionRequest,
    ): TaskResponse
}
```

### `optionalString` PATCH fields

For transaction PATCH, fields `transfer_account_id`, `category_id`, and `description`
use an optional-string semantic — explicit `null` clears the field; omitting the key
leaves it unchanged. Implement with a sealed wrapper or a custom serializer:

```kotlin
// Option: use a custom JsonTransformingSerializer or encodeDefaults=false
// and wrap nullable fields that can be explicitly-nulled in ExplicitNull<T>
@Serializable
data class TransactionUpdateRequest(
    val transactionType: String? = null,
    val amount: String? = null,
    val transactionDate: String? = null,
    val accountId: String? = null,
    @Serializable(with = ExplicitNullSerializer::class)
    val transferAccountId: ExplicitNull<String> = ExplicitNull.Absent,
    @Serializable(with = ExplicitNullSerializer::class)
    val categoryId: ExplicitNull<String> = ExplicitNull.Absent,
    @Serializable(with = ExplicitNullSerializer::class)
    val description: ExplicitNull<String> = ExplicitNull.Absent,
)
```

### Image / File Upload

```kotlin
interface NoteApi {
    @Multipart
    @POST("v1/notes/{noteId}/images")
    suspend fun uploadImage(
        @Path("noteId") noteId: String,
        @Part file: MultipartBody.Part,
    ): NoteImageUploadResponse
}

// Building the MultipartBody.Part from a local file URI:
fun uriToMultipartPart(context: Context, uri: Uri, fieldName: String = "file"): MultipartBody.Part {
    val stream = context.contentResolver.openInputStream(uri)!!
    val bytes = stream.readBytes()
    stream.close()
    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
    val body = bytes.toRequestBody(mimeType.toMediaType())
    return MultipartBody.Part.createFormData(fieldName, "upload.jpg", body)
}
```

---

## Sync Engine

### `SyncManager` (orchestrator)

- Singleton provided by Hilt (`@Singleton`)
- Observes `NetworkMonitor.isOnline: StateFlow<Boolean>`
- On AVAILABLE: enqueues one-shot `SyncWorker` + starts foreground coroutine loop
- Every 60 s in foreground: calls `triggerSync()` if online and not already syncing
- Exposes `syncState: StateFlow<SyncState>` with `isOnline`, `isSyncing`,
  `pendingCount`, `failedCount`

### `QueueProcessor`

```kotlin
suspend fun processQueue(): QueueResult {
    val items = syncQueueDao.getPending()
    var processed = 0; var failed = 0

    for (item in items) {
        syncQueueDao.updateStatus(item.id, "processing", null)
        try {
            val result = executeMutation(item)
            if (result?.id != null && result.id != item.entityId) {
                idRemapper.remap(item.entityType, item.entityId, result.id)
            }
            syncQueueDao.delete(item.id)
            processed++
        } catch (e: HttpException) {
            when {
                e.code() == 401 -> {
                    syncQueueDao.updateStatus(item.id, "pending", e.message)
                    authEventBus.emit(AuthEvent.LoggedOut)
                    return QueueResult(processed, failed, PauseReason.AUTH_ERROR)
                }
                e.code() >= 500 -> {
                    syncQueueDao.updateStatus(item.id, "pending", e.message)
                    return QueueResult(processed, failed, PauseReason.TRANSIENT_ERROR)
                }
                else -> {
                    syncQueueDao.updateStatus(item.id, "failed", e.message)
                    failed++
                }
            }
        } catch (e: IOException) {
            syncQueueDao.updateStatus(item.id, "pending", e.message)
            return QueueResult(processed, failed, PauseReason.TRANSIENT_ERROR)
        }
    }
    return QueueResult(processed, failed)
}
```

### `PullSync`

Pull each entity type in parallel with `coroutineScope { launch { … } }` for
independent entities (tags, projects, accounts, categories), then sequentially for
dependent ones (tasks after projects+tags, transactions after accounts+categories).

For each entity, check `sync_queue` for pending mutations — skip those `entity_id`
values. Apply upsert only if server `updated_at >= local updated_at`.

### `IdRemapper`

```kotlin
suspend fun remap(entityType: String, oldId: String, newId: String) {
    if (oldId == newId) return
    db.withTransaction {
        when (entityType) {
            "task" -> {
                taskDao.remapId(oldId, newId)
                subtaskDao.remapTaskId(oldId, newId)
                taskTagDao.remapTaskId(oldId, newId)
                noteTaskLinkDao.remapTaskId(oldId, newId)
            }
            "note" -> {
                noteDao.remapId(oldId, newId)
                noteTagDao.remapNoteId(oldId, newId)
                noteTaskLinkDao.remapNoteId(oldId, newId)
                noteImageDao.remapNoteId(oldId, newId)
            }
            // … project, tag, account, category, transaction, budget
        }
        syncQueueDao.remapEntityId(oldId, newId)
    }
}
```

### `SyncWorker` (WorkManager)

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncManager.triggerSync()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "tasker_sync_periodic"
        const val WORK_NAME_RECONNECT = "tasker_sync_reconnect"

        fun periodicRequest() = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        fun oneshotRequest() = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
    }
}
```

---

## Repository Pattern

Each repository wraps a Room DAO (local) + Retrofit API (remote). The ViewModel
**only** calls the repository. Example:

```kotlin
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val syncQueueDao: SyncQueueDao,
    private val taskApi: TaskApi,           // only used in PullSync, not directly by VM
    private val db: AppDatabase,
) {
    fun observeTasks(filters: TaskFilters): Flow<List<Task>> =
        taskDao.observeAll(filters).map { it.map(TaskEntity::toDomain) }

    suspend fun createTask(input: CreateTaskInput): Task {
        val entity = input.toEntity()
        val queueItem = entity.toCreateQueueItem()
        db.withTransaction {
            taskDao.upsert(entity)
            if (input.tagIds.isNotEmpty()) taskTagDao.insertAll(...)
            if (input.subtasks.isNotEmpty()) subtaskDao.insertAll(...)
            syncQueueDao.enqueue(queueItem)
        }
        return entity.toDomain()
    }

    suspend fun updateTask(id: String, input: UpdateTaskInput) { … }
    suspend fun deleteTask(id: String) { … }
}
```

---

## API Reference (actual backend)

### Auth
```
POST /v1/auth/login
  body: { username, password, remember_me? }
  → { token, user: { id, username } }

PATCH /v1/auth/password
  body: { current_password, new_password }
  → 204

GET /v1/me → { id, username }
```

### Tasks
```
GET  /v1/tasks?status=all|open|completed|archived&limit=1000&project_id=&q=
POST /v1/tasks  body: { title, description?, due_date?, priority 0-3, project_id?, tag_ids?, subtasks? }
GET  /v1/tasks/{id}
PATCH /v1/tasks/{id}
DELETE /v1/tasks/{id} → 204
PATCH /v1/tasks/{id}/completion  body: { completed: bool }
POST /v1/tasks/{id}/subtasks  body: { title, position? }
PATCH /v1/tasks/{id}/subtasks/{subId}  body: { title?, completed?, position? }
DELETE /v1/tasks/{id}/subtasks/{subId} → 204

GET /v1/projects | POST | PATCH /v1/projects/{id} | DELETE → 204
GET /v1/tags     | POST | PATCH /v1/tags/{id}     | DELETE → 204
```

### Notes
```
GET  /v1/notes?q=&limit=1000
POST /v1/notes  body: { title, content_md }
GET  /v1/notes/{id}
PATCH /v1/notes/{id}  body: { title?, content_md? }
DELETE /v1/notes/{id} → 204
PUT    /v1/notes/{id}/tasks/{taskId} → 204  (link)
DELETE /v1/notes/{id}/tasks/{taskId} → 204  (unlink)

POST   /v1/notes/{id}/images  multipart field: file (JPEG/PNG/WebP/GIF, max 10 MiB)
  → { image: { id, note_id, original_filename, mime_type, byte_size, alt_text?, width?, height?, created_at }, token }
PATCH  /v1/note-images/{imageId}  body: { alt_text? }
DELETE /v1/note-images/{imageId} → 204
GET    /v1/note-images/{imageId}/access → { url, expires_in: 3600 }
```

### Money
```
GET /v1/accounts?include_archived=
POST /v1/accounts  body: { name, account_type }
GET/PATCH/DELETE /v1/accounts/{id}
  patch body: { name?, account_type?, is_archived? }

GET /v1/categories?type=income|expense&include_archived=
POST /v1/categories  body: { name, category_type, icon?, color? }
GET/PATCH/DELETE /v1/categories/{id}

GET /v1/transactions?start_date=&end_date=&account_id=&category_id=&type=&q=&min_amount=&max_amount=&limit=
POST /v1/transactions  body: { transaction_type, amount, transaction_date, account_id,
                               transfer_account_id?, category_id?, description? }
GET/PATCH/DELETE /v1/transactions/{id}
  patch: optionalString fields (explicit null clears, omit = no change) for
         transfer_account_id, category_id, description
POST   /v1/transactions/{id}/receipt  multipart field: file → receipt object
DELETE /v1/transaction-receipts/{receiptId} → 204
GET    /v1/transaction-receipts/{receiptId}/access → { url, expires_in: 3600 }

GET /v1/budgets
POST /v1/budgets  body: { category_id, period_start, period_end, amount_limit }
GET/PATCH/DELETE /v1/budgets/{id}
  response includes: spent, remaining, percent_used, is_over_budget (computed)

GET /v1/recurring-transactions
GET /v1/recurring-transactions/due
POST /v1/recurring-transactions  body: { transaction_type, amount, account_id, category_id,
                                         description?, cadence, next_due_date, ends_on?, is_active }
GET/PATCH/DELETE /v1/recurring-transactions/{id}
POST /v1/recurring-transactions/{id}/confirm → 201 Transaction
POST /v1/recurring-transactions/{id}/skip → 204

GET /v1/money/dashboard?start_date=&end_date=&group_by=
  → { total_balance, income, expense, category_spend[], trend[] }

GET /v1/search?q=&scope=tasks|notes|all&limit=
  → { tasks[], notes[] }
```

### Error format (RFC 7807)
```json
{ "type": "...", "title": "...", "status": 422, "detail": { "field": "message" } }
```

Common HTTP error codes: 400, 401, 404, 409, 413, 415, 422, 500.

---

## Release Build Configuration

### `app/build.gradle.kts`
```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}
```

### `proguard-rules.pro` must include keep rules for:
- Kotlin Serialization: `@Serializable` annotated classes
- Retrofit: API interfaces + response DTOs
- Room: entity and DAO classes
- Hilt: inject targets
- OkHttp / Okio (usually handled by their bundled consumer rules)

---

## Conventions

- **Column naming**: Room entities use `camelCase` field names; Room maps them to
  `snake_case` column names via `@ColumnInfo(name = "snake_case")` annotations.
- **Timestamps**: All stored as ISO 8601 strings (`TEXT` column). Parse with
  `java.time.Instant.parse()` or `java.time.LocalDate.parse()`.
- **Soft deletes**: `isDeleted = 1` — all queries must filter `WHERE isDeleted = 0`.
- **Currency amounts**: Stored as `TEXT` (decimal string from server, e.g. `"100.00"`)
  to avoid float precision loss. Parse with `java.math.BigDecimal`.
- **UUIDs**: Generated client-side with `java.util.UUID.randomUUID().toString()`.
- **No `runOnUiThread` / `Handler`**: All threading via coroutines and `Dispatchers`.

---

## Testing Strategy

| Layer           | Tool                     | Scope                                              |
|----------------|--------------------------|----------------------------------------------------|
| Room DAOs       | `Room.inMemoryDatabaseBuilder` + JUnit4 | CRUD, query filters, FK cascades |
| Repository      | JUnit + MockK            | Local read/write + queue enqueue logic              |
| QueueProcessor  | JUnit + MockK (mock API) | Push success, 401, 5xx, ID remap                   |
| PullSync        | JUnit + MockK            | Upsert logic, conflict resolution, skip-pending     |
| ViewModel       | JUnit + `TestCoroutineScope` | StateFlow emissions                            |
| UI              | Compose `createComposeRule` | Component rendering, state transitions           |
| E2E             | (Espresso / not in v1)  |                                                     |

---

## Phase Checklist

### Phase 1 — Scaffold
- [ ] New `android/` directory with Gradle wrapper, Kotlin DSL build files
- [ ] `AppDatabase` skeleton (no entities yet)
- [ ] Navigation Compose bottom bar: Tasks / Notes / Money / Settings (placeholder screens)
- [ ] Material 3 `Theme.kt` with full `design.md` color tokens in light + dark
- [ ] `Type.kt` with Inter font and correct scale
- [ ] Hilt `@HiltAndroidApp` + basic DI modules
- [ ] `NetworkSecurityConfig` banning cleartext

### Phase 2 — Tasks (local only)
- [ ] `ProjectEntity`, `TagEntity`, `TaskEntity`, `SubtaskEntity`, `TaskTagEntity`
- [ ] All DAOs with correct FK cascades and `Flow` query methods
- [ ] `TaskRepository` — `createTask`, `updateTask`, `deleteTask`, `observeTasks`
- [ ] `SyncQueueEntity` + `SyncQueueDao` — enqueue on every mutation
- [ ] `TaskListViewModel` + `TaskListScreen` rendering from Room Flow
- [ ] `CreateEditTaskSheet` form with project/tag pickers
- [ ] `TaskDetailScreen`

### Phase 3 — Sync engine (Tasks)
- [ ] `NetworkMonitor` with `StateFlow<Boolean>`
- [ ] `AuthInterceptor` + `AuthEventBus`
- [ ] `QueueProcessor` — per-item push, ID remap, retry/fail logic
- [ ] `PullSync.pullTasks()`, `pullProjects()`, `pullTags()`
- [ ] `SyncManager` orchestrator with foreground 60 s loop
- [ ] `SyncWorker` WorkManager integration (periodic + one-shot on reconnect)
- [ ] End-to-end test: create task offline → reconnect → verify server + local consistent

### Phase 4 — Notes + Money
- [ ] Notes: entities, DAOs, repo, screens, CameraX / photo picker, image upload queue
- [ ] Note image signed-URL fetch + caching (`GET /v1/note-images/:id/access`)
- [ ] Money: all entities, DAOs, repos
- [ ] `PullSync` extended for all entity types
- [ ] `QueueProcessor` extended for all entity+operation types
- [ ] Money dashboard with Compose Canvas chart

### Phase 5 — Sync status UI + polish
- [ ] Online/offline badge composable (injected at scaffold level)
- [ ] Pending count badge in bottom nav
- [ ] Failed sync error cards with retry action
- [ ] Shimmer skeleton screens for all list views
- [ ] Empty states + error states everywhere
- [ ] Haptic feedback on task complete, swipe dismiss

### Phase 6 — Verification & benchmarking
- [ ] R8 + resource shrinking enabled; confirm no missing keep rules
- [ ] Measure release APK size (target ≤ 20 MB) and AAB size (target ≤ 15 MB)
- [ ] Android Profiler: cold-start time (target < 1.5 s P90), idle RAM (target < 80 MB)
- [ ] Compare against RN baseline numbers
- [ ] Airplane-mode full offline test pass
- [ ] Flaky-connection test (toggle airplane mode mid-sync 10×)
- [ ] Force-close mid-sync test (verify queue integrity on restart)
- [ ] Document results
