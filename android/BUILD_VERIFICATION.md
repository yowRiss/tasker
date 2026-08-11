# Tasker Native Kotlin — Build & Verification Report

## 1. Project Overview & Architecture

The React Native/Expo mobile app has been completely rewritten natively in **Kotlin + Jetpack Compose** to achieve smaller app size, better runtime performance, and significantly lower RAM footprint.

### Key Architecture Components

- **UI Layer**: Jetpack Compose with Material 3 design system, adhering to all tokens in `design.md`.
- **Architecture**: MVVM with unidirectional data flow (`StateFlow`).
- **Local Database**: Room (SQLite) with 17 entities, WAL mode enabled, foreign key constraints enforced.
- **Offline Sync Engine**:
  - `SyncQueueEntity`: Stores offline mutations (`CREATE`, `UPDATE`, `DELETE`, `UPLOAD_IMAGE`, `UPLOAD_RECEIPT`).
  - `QueueProcessor`: Executes per-item HTTP mutations against Retrofit API.
  - `IdRemapper`: Atomically translates temporary client UUIDs to server canonical UUIDs inside Room transactions.
  - `PullSync`: Fetches remote records, respects pending local mutations, and reconciles via `updated_at` timestamps.
  - `NetworkMonitor`: Registers `ConnectivityManager.NetworkCallback` for real-time online/offline state updates.
  - `WorkManager`: Handles periodic 15-minute background sync and one-shot reconnect sync jobs.
- **Secure Auth**: `EncryptedSharedPreferences` (Jetpack Security Crypto AES-256) storing JWT token.

---

## 2. Size & Performance Benchmarking Targets

| Metric | React Native Baseline | Native Kotlin Target | Optimization Mechanism |
|---|---|---|---|
| **Release APK Size** | ~85 MB | **< 15–20 MB** | R8 full-mode minification + resource shrinking |
| **Release AAB Size** | ~70 MB | **< 10–15 MB** | Dynamic feature splits & language resource filtering |
| **Cold Start Time (P90)** | ~3.5 s | **< 1.2 s** | No Hermes JS engine initialization or RN bridge startup |
| **Idle RAM Footprint** | ~140–180 MB | **< 60–80 MB** | Direct native Android runtime without V8/Hermes engine |
| **Local DB Read Latency** | ~40–60 ms | **< 5–15 ms** | Room SQLite direct WAL query via Coroutine Dispatchers.IO |

---

## 3. Verification Test Pass Guidelines

### Test 1: Full Airplane-Mode Offline Test Pass
1. Turn on Airplane Mode on device/emulator.
2. Launch app, log in (using cached session credentials).
3. Create a Task, a Note with local camera/gallery image attachment, an Account, and a Transaction.
4. Verify all items render immediately in local lists with pending status indicators.
5. Verify `sync_queue` table contains corresponding mutation records.
6. Turn off Airplane Mode / re-enable Network.
7. Verify `NetworkMonitor` detects connection, `SyncManager` triggers push-before-pull cycle, images upload via multipart, server IDs are remapped, and queue clears clean.

### Test 2: Flaky Connection / Mid-Sync Interrupt Test Pass
1. Enqueue 10 offline mutations.
2. Connect to network and initiate sync.
3. Toggle network OFF mid-sync (after 3 items processed).
4. Verify processed items are deleted from `sync_queue`, remaining items revert to `pending`, and no duplicate records or state corruption occur on Room DB.
5. Reconnect network; verify sync resumes seamlessly from item #4.

### Test 3: Force-Close Mid-Sync Test Pass
1. Enqueue offline mutations and trigger sync.
2. Force-stop the app process during active network transmission.
3. Re-launch the app.
4. Verify `AppDatabase` integrity via SQLite WAL recovery, pending items in `sync_queue` remain intact, and `SyncWorker` / `SyncManager` re-runs sync cleanly without data loss.

---

## 4. How to Build & Package in Android Studio

1. Open the `android/` directory in Android Studio.
2. Sync Gradle files (`Gradle Sync`).
3. To build a debug APK:
   ```bash
   ./gradlew :app:assembleDebug
   ```
4. To build an optimized release APK with R8 minification:
   ```bash
   ./gradlew :app:assembleRelease
   ```
5. To build an Android App Bundle (AAB) for Google Play:
   ```bash
   ./gradlew :app:bundleRelease
   ```
