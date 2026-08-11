# Personal Tasks + Notes + Money — Mobile Product Requirements Document (Android)

## Overview

The Mobile Client for Personal Tasks + Notes + Money is an offline-first Android application built with React Native (Expo) that communicates with the existing Go backend and Supabase Postgres/Storage instance. 

The primary goal of the mobile client is to provide a fluid, instant, and reliable personal productivity experience on mobile devices regardless of network conditions. Reads and writes operate against local storage first, ensuring zero network latency for user interactions. Local mutations are queued and automatically synchronized with the Go API in the background when connectivity is available.

---

## Goals & Non-Goals

### Goals

- **100% Offline Capability**: Allow full access to create, view, edit, complete, search, and delete tasks, notes, and financial transactions without an active internet connection.
- **Local-First Responsiveness**: All UI reads render immediately from local storage (SQLite). All UI writes commit to local storage synchronously with zero network blocking.
- **Background Synchronization**: Automatically flush pending changes to the Go `/v1/*` REST API when network connectivity is detected, and pull server changes to update local storage.
- **Offline Image & Receipt Capture**: Enable users to take photos or pick images for notes and transaction receipts while offline, storing them locally and uploading them seamlessly once online.
- **Clear Sync Status Visibility**: Give users persistent, unobtrusive visual feedback regarding network state (online/offline) and pending sync queue item counts.
- **Resilience to Network Flakiness**: Gracefully handle partial connectivity, request timeouts, and unexpected app terminations mid-sync without duplicating data or corrupting the queue.

### Non-Goals

- **Multi-User Collaboration / Real-Time Editing**: V1 is single-user across devices (phone + web). Multi-user sync, live cursor presence, or Operational Transform / CRDT engines are explicitly out of scope.
- **Duplicate Backend Logic**: The mobile app does not implement a separate backend or shadow database logic; the server remains the ultimate source of truth for long-term storage and cross-device state.
- **iOS Support in V1**: The initial release targets Android exclusively. However, cross-platform React Native / Expo conventions are maintained so iOS support can be added in a future release without re-architecting native code.

---

## Target Platform & Android Specifics

- **Target Platform**: Android 8.0 (API Level 26) and higher.
- **Framework**: React Native with Expo Managed Workflow.
- **Android Native Behaviors & Permissions**:
  - **Camera & Storage Permissions**: Handled via standard Android runtime permission prompts when adding note images or transaction receipts (`CAMERA`, `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`).
  - **Scoped Storage**: Local images are stored securely within the app's sandboxed document directory (`FileSystem.documentDirectory`).
  - **Background Task Execution**: Respects Android Doze mode and OEM battery optimizations by executing sync primarily on app foreground / network reconnect triggers, with optional background sync registered via Android JobScheduler / Expo TaskManager.

---

## Feature Parity & Mobile Scope

| Feature Module | Functional Scope | Mobile Adaptations & Offline Behavior |
| :--- | :--- | :--- |
| **App Shell & Nav** | Shared workspace navigation, authentication state, sync status header. | Bottom tab navigation for Tasks, Notes, Money, and Search. Header status bar showing online/offline status and pending sync count. |
| **Task Manager** | Tasks, subtasks, projects, tags, priorities (0–3), due dates, completion state. | Instant offline CRUD. Subtasks and tag associations stored locally in SQLite. Due date selection uses native Android date pickers. |
| **Notes & Images** | Markdown editor, preview mode, tag assignment, task links, note image attachments. | Full offline Markdown editing. Images captured via camera/gallery are saved locally, displayed instantly via `file://` URIs, inserted as `note-image:<id>` references, and queued for background upload. |
| **Money Management** | Accounts (cash, bank, e-wallet, credit card), Income/Expense/Transfer transactions, Categories, Budgets, Recurring Transaction prompts, Money Dashboard summaries. | Local SQLite computes real-time account balances, period spending, and budget progress offline. Offline receipt attachment following the same local-file-first image model. |
| **Unified Search** | Cross-module search for tasks, notes, and transactions. | Instant full-text search executed locally against SQLite tables using debounced input. |

---

## Offline-First Operating Model

### 1. Read Path
All screens read data exclusively from the local SQLite database. When a view mounts or query parameters change, the UI subscribes to local database state. No HTTP requests are issued on the critical render path.

### 2. Write Path
When a user creates, edits, completes, or deletes an item:
1. The mutation is saved immediately to the local SQLite database within a database transaction.
2. A corresponding mutation payload is written to the `sync_queue` table with status `pending`.
3. Local reactive subscriptions fire immediately, updating the UI in < 50 ms.
4. The Sync Engine is notified to process the queue if the device is currently online.

### 3. Offline Image & Receipt Strategy
1. **Selection/Capture**: When a user selects or captures an image, the raw file is copied to the app's persistent local directory (`FileSystem.documentDirectory + 'images/...'`).
2. **Local Reference**: A record is created in local `note_images` or `transaction_receipts` with `local_uri` set to the local file path and `sync_status = 'pending'`.
3. **Queue Entry**: A special queue operation (`UPLOAD_IMAGE` or `UPLOAD_RECEIPT`) is inserted into `sync_queue`.
4. **Immediate Display**: The note preview or transaction detail view displays the image immediately using the local `file://` URI.
5. **Background Upload**: Once online, the file is uploaded to Go `/v1/notes/{noteId}/images` or `/v1/transactions/{transactionId}/receipt` via multipart form data. On success, local state updates to `sync_status = 'synced'` and stores the remote access path.

---

## Conflict Resolution Strategy

### Strategy: Last-Write-Wins (LWW) with Soft Deletion

Since this application is designed for a single user operating across a phone and a web browser, true multi-user concurrent editing does not occur. Conflicts only arise when a user modifies the same entity offline on mobile while previously editing it on the web (or vice versa).

1. **Timestamp Comparison**:
   - Entities maintain ISO 8601 UTC `updated_at` timestamps.
   - When pulling server changes during sync, if `server.updated_at > local.updated_at` AND the local entity has no active unsynced mutations in `sync_queue`, the local SQLite record is updated with the server state.
   - When pushing queued local mutations to the server, the server updates `updated_at` upon receiving the request.

2. **Soft Deletion (`is_deleted`)**:
   - Deletions performed offline set `is_deleted = 1` and `updated_at = NOW()` locally, and enqueue a `DELETE` operation in `sync_queue`.
   - If a pull sync encounters a server deletion or soft-deleted record, `is_deleted = 1` takes precedence over older local edits (`updated_at` comparison), preventing "ghost resurrections" of deleted items.

3. **Permanent Failure Protection (Poison Pill Handling)**:
   - If a queued mutation fails on the server with a permanent error (e.g. `409 Conflict`, `422 Validation Error`, or broken foreign key constraint):
     - The mutation is marked as `failed` in `sync_queue` with the error message stored in `last_error`.
     - The item is NOT deleted from the queue, and queue processing continues for non-dependent items.
     - A non-blocking alert banner appears in the Sync Status UI, allowing the user to view the error, edit the record to fix validation, or manually discard the queued mutation. This guarantees **zero silent data loss**.

---

## Sync Status Visibility & User Experience

The application provides continuous visual feedback regarding connectivity and sync progress:

1. **Header Sync Badge**:
   - **Online (Synced)**: Subtle green indicator or clean header state when all changes are synced.
   - **Offline Mode**: Amber badge indicating `Offline — local changes saved`.
   - **Syncing in Progress**: Animated blue indicator displaying `Syncing (N remaining)...`.
   - **Sync Error**: Red alert badge indicating `N items failed to sync — tap to resolve`.

2. **Per-Item Status Badges**:
   - Items with pending offline writes exhibit a subtle clock/sync icon.
   - Items with failed uploads show a retry badge with a quick-action context menu (Retry / Edit / Discard).

3. **Sync Management Drawer / Screen**:
   - A dedicated view accessible from Settings/Header listing all items in `sync_queue`, their status (`pending`, `processing`, `failed`), retry attempts, and options for manual forced sync or clearing failed entries.

---

## Non-Functional Requirements

- **Cold Start Time**: Application launch to interactive local view in under 1.5 seconds on mid-range Android hardware.
- **Memory & Storage Footprint**: Base APK under 30 MB (Expo managed build). SQLite storage footprint capped efficiently through periodic WAL checkpointing.
- **Battery & Data Optimization**:
  - NetInfo listener debounces network state changes to avoid sync storms on unstable Wi-Fi/cellular transitions.
  - Backoff strategy prevents repeated aggressive API calls when server or network is unresponsive.
  - Upload streams compress oversized images client-side before sending to conserve mobile data.
- **Flaky Network Tolerance**: Request timeouts set to 15 seconds. If a connection drops mid-request, the transaction rolls back cleanly in Go and the queued mutation remains `pending` for the next retry attempt.
