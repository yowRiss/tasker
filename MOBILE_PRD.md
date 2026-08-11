# Tasker Mobile — Product Requirements Document (Kotlin/Jetpack Compose)

> **Rewrite context**: This document supersedes the React Native/Expo PRD. The functional
> scope, data model, API contract, offline-first requirement, and conflict-resolution
> strategy are **unchanged**. Only the platform implementation changes: React Native →
> native Kotlin + Jetpack Compose. The Go backend and Supabase schema are **not modified**.

---

## Overview

A native Android application for Tasker delivering full feature parity across Tasks,
Notes, and Money Management. The app is **offline-first**: every read and write hits
local Room/SQLite first; a background sync engine reconciles with the Go `/v1/*` API
when connectivity is available.

**Why native Kotlin?**  
The React Native/Expo build was ~85 MB and runs the Hermes JS engine + RN bridge at all
times — even at idle. A native Kotlin/Compose release build with R8/ProGuard shrinking
should land **under 15–20 MB** and use substantially less RAM because there is no JS
engine or bridge process.

---

## Platform Target

| Attribute         | Value                                      |
|------------------|--------------------------------------------|
| Language          | Kotlin                                     |
| UI toolkit        | Jetpack Compose (Material 3)               |
| Min SDK           | Android 10 / API 29                        |
| Target SDK        | Android 15 / API 35                        |
| Distribution      | Release APK + AAB (Play Store compatible)  |

---

## Size & Performance Targets

| Metric              | Target              | Baseline (RN)  |
|--------------------|---------------------|----------------|
| Release APK         | ≤ 20 MB             | ~85 MB         |
| Release AAB         | ≤ 15 MB             |                |
| Cold start (P90)    | < 1.5 s             |                |
| Local DB read       | < 50 ms             | < 50 ms        |
| Sync cycle          | < 5 s typical       | < 5 s          |
| Idle RAM            | < 80 MB             |                |

---

## Features

### Authentication
- Login screen: **username** + password (the backend uses `username`, not `email`)
- Single JWT token returned as `{ token, user }` from `POST /v1/auth/login`
- Token stored in **`EncryptedSharedPreferences`** (Jetpack Security)
- `remember_me: true` extends TTL to 7 days (default 24 h)
- On 401: clear token, navigate to login
- Logout: clears token; optional prompt to wipe local data

### Task Management
- Projects: create, list, archive — tasks grouped by project
- Tags: create, list, assign to tasks/notes
- Task list with filters: status (`open` / `completed` / `all` / `archived`), project,
  tag, priority (int 0–3), free-text search
- Create/edit task: title, description, due date, priority, project, tags, subtasks
- Subtasks: inline list; create/update/delete per task
- Swipe-to-complete gesture
- Task ↔ Note linking (via join-table operations)

### Notes
- Note list with search and tag filter
- Markdown editor (write + preview toggle)
- Image attachments: capture via CameraX or system photo picker; stored locally until
  synced; signed-URL access after upload
- Full-screen image viewer
- Notes can be linked to tasks

### Money Management
- **Accounts**: list, create, archive; balance shown (decimal string from server)
- **Categories**: list, create, archive; typed as `income` or `expense`
- **Transactions**: list (filterable by account, category, type, date range, amount,
  search); create/edit/delete; receipt image attach+upload
- **Recurring Transactions**: list, create, update, delete; confirm (creates real tx) or
  skip
- **Budgets**: list, create, update, delete; display computed `spent`, `remaining`,
  `percent_used`, `is_over_budget`
- **Money Dashboard**: total balance, income vs. expense summary, category spend
  breakdown, trend data — rendered with Compose Canvas (no heavy charting library)

### Settings
- Theme: light / dark / follow system
- Sync status: last sync time, pending queue count, failed count
- Manual sync trigger
- Change password (`PATCH /v1/auth/password`)
- Logout

### Sync Status UI
- Persistent online/offline badge on all main screens
- Pending mutation count badge
- In-line error card for permanently failed sync items

---

## Data Model (local Room schema)

The Room schema mirrors the actual backend domain model discovered from the live codebase.
Notable **corrections vs. the old RN PRD**:

| Old PRD assumption          | Actual backend                             |
|----------------------------|--------------------------------------------|
| `email` login field         | `username` login field                     |
| `status` text enum (todo/…) | `status` text (`open`/`completed`/`archived`) |
| `priority` text enum        | `priority` integer 0–3                     |
| No Projects entity          | `projects` is a first-class entity         |
| Tags on tasks via array     | `task_tags` join table                     |
| `content` note field        | `content_md` note field                    |
| Monolithic sync push/pull   | Per-entity list pulls + per-item push with ID remap |
| No receipt upload endpoint  | `POST /v1/transactions/:id/receipt` confirmed |

### Room entities (translated from actual SQLite schema)

**Tasks module**: `ProjectEntity`, `TagEntity`, `TaskEntity`, `SubtaskEntity`,
`TaskTagEntity`, `NoteTaskLinkEntity`

**Notes module**: `NoteEntity`, `NoteTagEntity`, `NoteImageEntity`

**Money module**: `AccountEntity`, `CategoryEntity`, `TransactionEntity`,
`TransactionReceiptEntity`, `BudgetEntity`, `RecurringTransactionEntity`

**Sync infrastructure**: `SyncQueueEntity`, `SyncMetadataEntity`

### `sync_queue` table

```sql
CREATE TABLE sync_queue (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_type  TEXT NOT NULL,              -- task | project | tag | note |
                                             --   note_image | account | category |
                                             --   transaction | transaction_receipt | budget
    entity_id    TEXT NOT NULL,              -- client-side UUID (may be remapped)
    operation    TEXT NOT NULL,              -- CREATE | UPDATE | DELETE |
                                             --   UPLOAD_IMAGE | UPLOAD_RECEIPT
    payload      TEXT NOT NULL,             -- JSON blob
    created_at   TEXT NOT NULL,             -- ISO 8601
    retry_count  INTEGER DEFAULT 0,
    last_error   TEXT,
    status       TEXT DEFAULT 'pending'     -- pending | processing | failed
);
```

### `sync_metadata` table

```sql
CREATE TABLE sync_metadata (
    table_name    TEXT PRIMARY KEY,
    last_synced_at TEXT NOT NULL,
    sync_cursor   TEXT            -- reserved for future cursor-based pagination
);
```

---

## Offline-First Architecture

### Sync Engine

The sync engine replicates the **actual RN implementation** (which deviates from the
original PRD's description of a `POST /v1/sync/push` + `GET /v1/sync/pull` batch
protocol). The real pattern, confirmed from the live codebase, is:

**Push (per-item)**:
1. Read rows from `sync_queue` where `status IN ('pending', 'failed')` and
   `retry_count < 10`, ordered by `id ASC`
2. For each item, call the specific REST endpoint:
   - `CREATE task` → `POST /v1/tasks`
   - `UPDATE task` → `PATCH /v1/tasks/:id`
   - `DELETE task` → `DELETE /v1/tasks/:id`
   - … same pattern for projects, tags, notes, accounts, categories, transactions, budgets
   - `UPLOAD_IMAGE` → `POST /v1/notes/:id/images` (multipart)
   - `UPLOAD_RECEIPT` → `POST /v1/transactions/:id/receipt` (multipart)
3. On success: if server returns a different UUID (`res.id != entity_id`), run an
   **ID remap** — atomically update all FK references across all local tables and the
   queue itself. Then delete the queue row.
4. On 401: pause queue processing, trigger re-auth / navigate to login.
5. On network/5xx (transient): increment `retry_count`, set `status = 'pending'`,
   pause batch (will retry on next sync trigger).
6. On 4xx validation (permanent): set `status = 'failed'`, surface to user.

**Pull (per-entity)**:
After push completes (or on reconnect with empty queue), pull each entity type independently:

| Entity           | Endpoint                                    | Notes                  |
|-----------------|---------------------------------------------|------------------------|
| Projects         | `GET /v1/projects`                          | `items[]`              |
| Tags             | `GET /v1/tags`                              | `items[]`              |
| Tasks            | `GET /v1/tasks?status=all&limit=1000`       | with subtasks + tags   |
| Notes            | `GET /v1/notes?limit=1000`                  | with tags              |
| Accounts         | `GET /v1/accounts`                          | `items[]`              |
| Categories       | `GET /v1/categories`                        | `items[]`              |
| Transactions     | `GET /v1/transactions?limit=1000`           | `items[]`              |
| Budgets          | `GET /v1/budgets`                           | computed fields inline |
| Recurring        | `GET /v1/recurring-transactions`            | `items[]`              |

For each pulled record:
- **Skip** if the entity has a pending mutation in `sync_queue` (local is source-of-truth
  until pushed).
- **Upsert** if server `updated_at >= local updated_at`.
- **Keep local** if local `updated_at > server updated_at` (local is newer — still queued).
- On next pull cycle, `last_synced_at` in `sync_metadata` is updated.

### ID Remapping

When the server assigns a canonical UUID different from the client's temp UUID (e.g., on
`CREATE`), an `IdRemapper` runs within a single SQLite transaction updating:
- The entity's own table PK
- All FK columns in dependent tables
- All `sync_queue` rows referencing the old `entity_id`

This must execute atomically before deleting the queue item.

### Sync Triggers

| Trigger               | Mechanism                                                 |
|-----------------------|-----------------------------------------------------------|
| Network reconnect      | `ConnectivityManager.NetworkCallback` → AVAILABLE          |
| Foreground periodic    | Coroutine timer every 60 s while app is in foreground     |
| Background periodic    | `WorkManager.PeriodicWorkRequest` (15–30 min minimum)     |
| Manual               | ViewModel call from pull-to-refresh / Settings button     |

> **Note**: Android enforces a 15-minute minimum interval for `PeriodicWorkRequest`.
> The sub-minute foreground sync is handled by a coroutine loop, not WorkManager.

### Conflict Resolution
- **Last-write-wins by `updated_at`**
- If a local record has a pending queue entry → **keep local** (do not overwrite during pull)
- If server `updated_at >= local updated_at` and no queue entry → **server wins**

### Image Upload
- Capture: **CameraX** `ImageCapture` use-case
- Pick: `ActivityResultContracts.PickVisualMedia`
- Storage: app-internal `context.filesDir/images/`
- Compression: max 1024 px long edge, JPEG 80% quality before upload
- Note images: enqueue `UPLOAD_IMAGE` op → `POST /v1/notes/:id/images` (multipart `file`)
- Receipt images: enqueue `UPLOAD_RECEIPT` op → `POST /v1/transactions/:id/receipt` (multipart `file`)
- After upload: store returned `object_path` / signed URL reference locally

### Connectivity Detection
- `ConnectivityManager.registerNetworkCallback(NetworkRequest` with
  `NET_CAPABILITY_INTERNET)`
- Online/offline state exposed as `StateFlow<Boolean>` in a `NetworkMonitor` singleton
- On AVAILABLE → trigger sync; on LOST → update UI badge

### Secure Storage
- `EncryptedSharedPreferences` (Jetpack Security Crypto) for JWT token
- No sensitive data in plain `SharedPreferences`
- HTTPS only; `NetworkSecurityConfig` banning cleartext traffic

---

## UI / UX

- **Navigation**: Jetpack Navigation Compose — bottom bar: **Tasks | Notes | Money |
  Settings**; nested NavGraphs per module; modal bottom sheets for create/edit flows
- **Swipe gestures**: `SwipeToDismiss` / `AnchoredDraggable` for task completion
- **Pull-to-refresh**: `PullRefreshIndicator` (Material 3) → manual sync
- **Haptic feedback**: `HapticFeedbackConstants` on key actions
- **Loading states**: shimmer skeleton screens, not spinners
- **Empty states**: centred illustration + primary CTA
- **Error states**: inline retry card with error message

---

## Design System (from `design.md`)

### Colors

| Token              | Light Mode  | Dark Mode   |
|-------------------|-------------|-------------|
| Background         | `#FAFAF9`   | `#141412`   |
| Surface            | `#FFFFFF`   | `#1E1D1B`   |
| Surface Alt        | `#F5F4F2`   | `#252422`   |
| Border             | `#E8E6E1`   | `#2E2C28`   |
| Text Primary       | `#1A1916`   | `#F0EDE8`   |
| Text Secondary     | `#6B6760`   | `#9C9891`   |
| Text Tertiary      | `#9C9891`   | `#6B6760`   |
| Accent             | `#4A7C59`   | `#5A9B6E`   |
| Accent Subtle      | `#EBF2ED`   | `#1A2E20`   |
| Destructive        | `#C0392B`   | `#E74C3C`   |
| Warning            | `#D97706`   | `#F59E0B`   |
| Success            | `#059669`   | `#10B981`   |

### Typography
- Font: **Inter** (variable or subset bundled)
- Scale: 12 / 14 / 16 / 18 / 20 / 24 / 28 / 32 / 40 sp
- Line height: 1.4× body, 1.2× headings, 1.6× reading content
- Letter spacing: −0.01 em headings, 0 em body

### Spacing & Shape
- Base unit: 4 dp — scale 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64 dp
- Corner radii: 4 / 8 / 12 / 16 / 24 dp

---

## Security

| Concern          | Implementation                                             |
|-----------------|------------------------------------------------------------|
| Token            | `EncryptedSharedPreferences`                               |
| Network          | HTTPS enforced via `NetworkSecurityConfig`                 |
| DB               | Unencrypted Room/SQLite in v1; SQLCipher is post-v1        |
| Images           | App-internal storage (`MODE_PRIVATE`)                      |

---

## Execution Phases (pause at each for review)

| Phase | Scope |
|-------|-------|
| **1** | Project scaffold: Kotlin/Compose skeleton, Navigation Compose, Material 3 theme (design.md tokens), Hilt DI |
| **2** | Room schema + DAOs + local-only CRUD for the **Tasks module** (tasks, subtasks, projects, tags) |
| **3** | Sync engine end-to-end for Tasks: NetworkMonitor, WorkManager, per-item push, per-entity pull, ID remapper, conflict resolution |
| **4** | Notes (with offline image capture/upload) + Money Management (accounts, categories, transactions, receipts, budgets, recurring, dashboard) |
| **5** | Sync status UI (online/offline badge, pending count, failed-item error cards) + overall UI polish |
| **6** | Release build with R8/shrinking; measure APK/AAB size, cold-start, idle RAM; airplane-mode pass, flaky-connection pass, force-close-mid-sync pass; compare vs. RN baseline |

---

## Assumptions & Open Questions

1. **Auth token structure**: The backend returns a single `{ token, user }` — no
   refresh token. On 401, the user is sent to the login screen. A `remember_me: true`
   flag on login extends token TTL to 7 days.

2. **Pull strategy**: The actual mobile app does full per-entity list pulls (not the
   batch `GET /v1/sync/pull` endpoint documented in `AGENT.md`). The Kotlin
   implementation will mirror this. The batch sync endpoint may not exist or may be unused.

3. **WorkManager minimum interval**: 15 min. Foreground "every 60 s" sync runs as a
   coroutine loop, not WorkManager.

4. **Currency**: The local DB schema defaults `currency = 'IDR'` (matching the RN app).
   The Kotlin app will carry this same default, pending a settings preference.

5. **SQLCipher**: Room is unencrypted in v1, consistent with the RN version.

6. **iOS**: Android-only. iOS requires a separate Swift/SwiftUI implementation.

7. **Receipt upload**: Confirmed as `POST /v1/transactions/:id/receipt` multipart —
   no ambiguity remaining.

8. **Note image access**: Requires a separate call to
   `GET /v1/note-images/:imageId/access` → signed URL (expires in 3600 s). The Kotlin
   implementation must cache the signed URL and refresh it before expiry.

9. **`optionalString` fields in PATCH**: `transfer_account_id`, `category_id`, and
   `description` on transactions use an optional-string pattern — explicit JSON `null`
   clears the field; omitting the key leaves it unchanged. The Kotlin DTO/serializer
   must handle this correctly (custom `JsonSerializer` or sealed wrapper type).
