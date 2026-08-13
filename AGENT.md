# Engineering / Agent Guide

## Project Summary

This repository contains a personal, single-user web workspace for tasks, Markdown notes with private image embeds, and money management. The product scope, user experience, and explicit exclusions live in [PRD.md](PRD.md); treat that document as the source of truth for what belongs in v1. Build with enterprise-quality separation, typing, security, and testing, but do not introduce collaboration or multi-tenant product complexity beyond secure per-user isolation.

## Tech Stack & Key Decisions

- **Frontend build/runtime:** Vue 3 with the Composition API, Vite, and TypeScript with `strict: true`. Use Vue Router for `/tasks`, `/tasks/:taskId`, `/notes`, `/notes/:noteId`, `/money`, `/money/accounts/:accountId`, `/money/transactions/:transactionId`, `/money/budgets`, `/money/recurring`, `/money/reports`, and `/search`; route views may be lazy-loaded. Do not introduce a Vue meta-framework or server-rendering layer for v1.
- **Styling:** component-scoped plain CSS plus global CSS custom properties in `frontend/src/styles/tokens.css` and `frontend/src/styles/global.css`. No UI component suite, Tailwind, CSS-in-JS runtime, or icon pack unless a measured requirement justifies it. Use small inline SVG Vue components for the needed icon set.
- **Frontend data/state:** domain-specific Composition API composables and a small typed `fetch` client are the default. Keep form state in the component and session/sidebar state in focused composables. Do not add Pinia unless a demonstrated cross-route, shared client-state problem cannot be solved cleanly with composables; do not add a query library in v1.
- **Go API:** use Go with `github.com/go-chi/chi/v5`, `pgx/v5` and `pgxpool`. Chi is a small, idiomatic `net/http` router with first-class middleware and no framework lock-in; pgx provides a fast PostgreSQL pool and explicit SQL without an ORM. The API is the only application component that queries Supabase Postgres or calls Supabase Storage REST.
- **Authentication:** the running app uses one local `public.admins` account with a bcrypt password hash. `POST /v1/auth/login` verifies that password in Go and returns a locally HMAC-SHA256-signed JWT; the frontend stores that token only through `lib/api/client.ts` and sends it as `Authorization: Bearer <jwt>`. Middleware verifies the local signature, expiry, and UUID `sub` before protected requests. Supabase Auth and `@supabase/supabase-js` are deliberately not used by this implementation; the browser must not use Supabase database or Storage APIs.
- **Notes:** Markdown is the canonical note format. Use a small, sanitized Markdown renderer with a source editor and preview; do not add a WYSIWYG editor. Persist image references as `note-image:<note_image_id>` in Markdown and resolve them to short-lived signed URLs when rendering.
- **Images:** validate and, where beneficial, resize/compress images in the browser before sending them to the Go API. The API authorizes the note owner, uploads to private Supabase Storage through its REST API, and writes image metadata to Postgres. The browser never uploads to Storage directly.
- **Money:** money amounts are `numeric(18,2)` exact decimals, never floats. V1 stores and reports Indonesian Rupiah (`IDR`) only. Account balances and budget/report totals are SQL aggregates of transactions, not mutable columns or client-side totals. Recurring templates produce due prompts only; the user explicitly confirms every generated transaction.
- **Charts:** use `uPlot`, lazy-loaded only by the reports view, for the time-series trend because it is small and fast compared with full dashboard/chart suites. Render the small category-spend graphic as a local accessible SVG/bar-list component rather than loading a second chart library; always show corresponding text/table data.
- **Why these choices:** the app needs a quick, maintainable SPA with minimal client dependencies while keeping credentials, data access, and Storage writes out of the browser. Vue + Vite keeps the UI light; Go + chi + pgx adds a small, explicit server boundary without a heavyweight backend framework.

## Folder / File Structure

Use one monorepo with independently deployable `frontend` and `backend` applications. This keeps the API contract, database migrations, and product documentation atomic for one personal product, without the coordination overhead of two repositories. Keep each domain’s UI, composables, validation, and API clients close enough to navigate easily, while retaining shared primitives in shared folders.

```text
api/
  openapi.yaml              # REST contract source of truth; update with API changes
frontend/
  .env.example              # development environment template
  .env.production           # committed same-origin production API setting
  src/
    app/
      App.vue               # app shell and router view
      router.ts              # Vue Router configuration and route guards
    views/                   # TasksView, TaskView, NotesView, NoteView, SearchView
    components/
      ui/                    # domain-neutral controls (BaseButton, BaseDialog, BaseInput)
      layout/                # Sidebar, MobileNav, WorkspaceHeader
    features/
      auth/
        AuthGate.vue          # local-admin login gate
        useAuth.ts            # local JWT session management
      tasks/
        components/         # TaskList, TaskEditor, TaskFilters, TaskDetail
        composables/        # useTasks, useTaskMutations
        task.api.ts          # typed calls to Go task/project/tag endpoints
        task.schemas.ts      # form/input validation and mapping
        task.types.ts        # UI/API DTO types, not database rows
      notes/
        components/         # NoteList, NoteEditor, MarkdownPreview, ImageUploader
        composables/
        note.api.ts
        note.schemas.ts
        note.types.ts
      money/
        components/         # AccountList, TransactionList, TransactionEditor, ReceiptUploader, BudgetList, RecurringTransactionEditor, MoneyDashboard, SpendingByCategory
        composables/        # useAccounts, useTransactions, useBudgets, useRecurringTransactions, useMoneyDashboard
        money.api.ts         # typed calls to Go money endpoints
        money.schemas.ts     # exact-decimal form validation and mapping
        money.types.ts       # UI/API DTO types, not database rows
      search/
        components/
        useGlobalSearch.ts
        search.api.ts
    lib/
      api/
        client.ts            # fetch wrapper; injects Bearer JWT and parses problems
        contracts.ts         # generated from api/openapi.yaml; never hand-edit
      auth/
        supabaseAuth.ts      # Auth-only Supabase client; no DB or Storage methods
      markdown/
        noteImage.ts         # parse/replace note-image:<uuid> references
    composables/             # genuinely cross-domain composables only
    styles/
      tokens.css
      global.css
    main.ts
  vite.config.ts
backend/
  cmd/api/main.go            # configuration, dependency wiring, HTTP server
  internal/
    auth/                    # local JWT signer/verifier and authenticated principal
    config/                  # validated environment configuration
    domain/                  # domain types and service interfaces
    httpapi/
      handlers/              # JSON decode/encode only; one area per domain
      middleware/            # auth, request ID, recovery, CORS, logging, limits
      response/              # RFC 9457 problem responses
      router.go
      web.go                 # embedded asset serving and SPA fallback
      webui/dist/            # generated Vite bundle; embedded at Go build time
    service/                 # authorization, use cases, transaction orchestration
    repository/postgres/     # pgx SQL queries and row-to-domain mapping
    storage/supabase/        # authenticated Supabase Storage REST adapter
    platform/dbtx/           # per-request transaction/RLS claim setup
  go.mod
  go.sum
supabase/
  migrations/               # timestamped schema, RLS, Storage policy migrations
  seed.sql                  # optional local-development sample data; no real data
```

Conventions:

- Put new UI code under `frontend/src/features/<domain>/` and new server code under the matching `backend/internal/<layer>/` area; do not create a generic `utils` dumping ground.
- A Vue single-file component contains one primary PascalCase component and uses `<script setup lang="ts">`. Keep component CSS in the SFC unless it is a broadly shared style.
- Vue components and composables call typed `*.api.ts` modules only; they must not construct raw `fetch` calls outside the shared API client and must never query Supabase data or Storage.
- Go handlers receive/return API DTOs, services own use cases and authorization, and repositories own SQL. A handler must not contain SQL, storage calls, or business rules.
- `api/openapi.yaml` is the REST schema contract. Regenerate `frontend/src/lib/api/contracts.ts` and any Go request/response types from it when it changes; do not treat Postgres rows as frontend types.
- Keep all external boundaries (HTTP DTOs, Postgres rows, Storage objects, form values, and Markdown image tokens) mapped and validated before they reach domain or presentational code.

## Go Backend Design

### API surface

All application routes are versioned below `/v1` and require a valid Bearer JWT, except `GET /healthz`. Use plural resources, JSON request/response bodies, ISO 8601 timestamps, date-only strings for `due_date`, opaque UUIDs, cursor pagination for lists, and the common problem-error format below.

| Route group | Routes | Purpose |
| --- | --- | --- |
| System and identity | `GET /healthz`; `POST /v1/auth/login`; `PATCH /v1/auth/password`; `GET /v1/me` | Liveness/readiness, local-admin login/password change, and authenticated identity/session diagnostics. |
| Tasks | `GET, POST /v1/tasks`; `GET, PATCH, DELETE /v1/tasks/{taskId}`; `PATCH /v1/tasks/{taskId}/completion` | List/filter/search, create, edit/delete, and explicitly complete or reopen a task. List parameters cover the PRD filters, sort, direction, `limit`, and cursor. |
| Projects and tags | `GET, POST /v1/projects`; `PATCH, DELETE /v1/projects/{projectId}`; `GET, POST /v1/tags`; `PATCH, DELETE /v1/tags/{tagId}` | Manage the user-owned organizational data. Task/note create and patch payloads set tag IDs atomically. |
| Notes | `GET, POST /v1/notes`; `GET, PATCH, DELETE /v1/notes/{noteId}` | List/search notes with bounded summaries; fetch a full Markdown body only for a selected note; create, edit, and delete. |
| Note/task links | `PUT, DELETE /v1/notes/{noteId}/tasks/{taskId}` | Add or remove the existing many-to-many note/task link. |
| Note images | `POST /v1/notes/{noteId}/images`; `PATCH, DELETE /v1/note-images/{imageId}`; `GET /v1/note-images/{imageId}/access` | Upload an image as `multipart/form-data`, edit alt text, delete an image, or obtain a short-lived display URL for an authorized image. The upload response includes the image metadata and its `note-image:<id>` Markdown token. |
| Accounts | `GET, POST /v1/accounts`; `GET, PATCH, DELETE /v1/accounts/{accountId}` | List accounts with SQL-derived balances; create, rename/archive, or delete an unused account. A delete with transactions returns `409`; archive preserves history. |
| Categories | `GET, POST /v1/categories`; `GET, PATCH, DELETE /v1/categories/{categoryId}` | Manage user-owned income/expense categories. Defaults are ordinary editable categories. A delete with transactions or budgets returns `409`; archive preserves history. |
| Transactions | `GET, POST /v1/transactions`; `GET, PATCH, DELETE /v1/transactions/{transactionId}` | List/search with bounded cursor pagination and date/category/account/type/amount/text filters; create, edit, or delete income, expense, and atomic account-to-account transfers. Responses include refreshed affected-account balances. |
| Transaction receipts | `POST /v1/transactions/{transactionId}/receipt`; `DELETE /v1/transaction-receipts/{receiptId}`; `GET /v1/transaction-receipts/{receiptId}/access` | Upload, remove, or obtain a short-lived URL for the one optional receipt attached to an authorized transaction. |
| Budgets | `GET, POST /v1/budgets`; `GET, PATCH, DELETE /v1/budgets/{budgetId}` | Manage expense-category budgets for inclusive monthly or custom periods. GET returns server-calculated spent, remaining, percent, and over-budget values. |
| Recurring transactions | `GET, POST /v1/recurring-transactions`; `GET, PATCH, DELETE /v1/recurring-transactions/{recurringTransactionId}`; `GET /v1/recurring-transactions/due`; `POST /v1/recurring-transactions/{recurringTransactionId}/confirm`; `POST /v1/recurring-transactions/{recurringTransactionId}/skip` | Manage recurring income/expense templates, list due prompts, and explicitly confirm or skip a due occurrence. Confirmation creates exactly one transaction and advances its due date in one database transaction. |
| Money dashboard and reports | `GET /v1/money/dashboard?start_date={date}&end_date={date}`; `GET /v1/money/reports/spending?start_date={date}&end_date={date}&group_by=day|week|month` | Return bounded, server-side account-balance, income/expense, budget, category, and trend aggregates. |
| Search | `GET /v1/search?q={query}&scope=all|tasks|notes&limit={n}&cursor={cursor}` | Return bounded, typed global-search results. |

The Go API owns local-admin login and password-change endpoints; it must never expose an admin password hash or database credential. The frontend calls this API for every product-data operation and never calls Supabase Auth, Postgres, or Storage directly.

### Layers, transactions, and data access

1. **Chi router and middleware** establish request IDs, safe CORS, recovery, structured logs, request limits, and the authenticated principal.
2. **Handlers** decode and validate transport shapes, call one service method, and encode success or problem responses. They contain no SQL and no Supabase Storage logic.
3. **Services** enforce use-case rules, pass the verified principal to all data operations, coordinate database transactions, and run compensating cleanup where a Storage operation and database write cannot be one transaction.
4. **Postgres repositories** use parameterized `pgx` queries only. They map database rows to domain values and always scope access by the principal’s user ID, even though RLS is also enforced. Money writes that affect a transfer, a recurring confirmation, or receipt metadata must use one database transaction; a report/balance query must aggregate in SQL, not by fetching all transactions into Go or Vue.
5. **Storage adapter** is the only code that calls Supabase Storage’s REST API. It receives an already authorized user ID, note ID, and generated object path—not a browser-supplied path.

Use a dedicated non-owner Postgres login role for the Go API with `NOBYPASSRLS` and only the required table/sequence grants. Each protected request opens a transaction and runs `select set_config('request.jwt.claim.sub', $1, true)` with the verified local-admin UUID before repository queries. Money-table RLS policies compare `user_id` to `current_setting('request.jwt.claim.sub', true)::uuid`; this is the actual transaction-local claim set by `platform/dbtx`, not a Supabase Auth helper. The setting must be transaction-local; never put an end-user identity into pooled-connection session state. The repositories must also include `user_id = $principalID` predicates as defense in depth.

### Error handling

- Return errors as `application/problem+json` following RFC 9457, with `type`, `title`, `status`, a stable machine-readable `code`, and `request_id`. Include field-level validation details only when safe.
- Use `400` for malformed JSON/parameters, `401` for missing or invalid JWTs, `403` for authenticated but prohibited actions, `404` for an unavailable resource without revealing another user’s record, `409` for unique/consistency conflicts, `413` for over-limit uploads, `415` for unsupported media, `422` for validly parsed but invalid input, and `500` for unexpected failures.
- Log the underlying error with its request ID server-side. Never return SQL details, Storage credentials, JWT contents, or raw upstream Supabase errors to the client.

### Local JWT middleware

- The frontend obtains a local-admin access token from `POST /v1/auth/login` and sends `Authorization: Bearer <JWT>` on every protected API request. It clears the session on a `401`; there is no refresh-token flow in this implementation.
- Middleware parses the token, requires HMAC-SHA256, validates the configured local signing secret, expiry/not-before, and UUID `sub` claim. The resulting principal contains the admin UUID and username only.
- Reject malformed, expired, incorrectly signed, and unverifiable tokens before a handler runs. Do not accept user identity in a JSON body, path, or query parameter as a substitute for the verified subject.
- Keep `JWT_SECRET` server-only in backend configuration, require it in production, rotate it through a deliberate sign-out/relogin deployment procedure, and never hard-code it in the frontend. Supabase URL and secret key remain server-only because they are used by the Storage adapter.

### Image upload flow

1. The Vue editor validates the chosen image, optionally reduces oversized pixel dimensions, and sends it as multipart data to `POST /v1/notes/{noteId}/images` with the Bearer token. It does not call Supabase Storage.
2. The Go handler applies `http.MaxBytesReader` with an 11 MiB request cap, separately enforces the 10 MiB file limit, MIME-sniffs the stream rather than trusting the filename, and accepts only the image types allowed in the Storage policy.
3. The service confirms that the selected note belongs to the JWT subject, creates an image UUID and the canonical `notes/{userId}/{noteId}/{uuid}-{sanitizedStem}.{ext}` object path, and passes the stream to the Storage adapter.
4. The Storage adapter uploads through Supabase Storage REST using a backend-only credential. After a successful object write, the repository inserts the `note_images` row in the user-scoped database transaction.
5. If the metadata insert fails, the service attempts Storage-object deletion and logs any cleanup failure with the request ID. It returns success only when both the object and metadata exist.
6. The API returns `201` with metadata and `note-image:<imageId>`; the Vue editor inserts that token only after success. For preview, it requests `/v1/note-images/{imageId}/access`; Go reauthorizes ownership and creates the short-lived signed URL. The browser uses that returned URL only as an image resource and never calls the Supabase SDK or Storage REST API itself.

### Transaction receipt upload flow

1. `ReceiptUploader` follows the same client validation, optional resize, multipart request, file cap, MIME sniffing, and retry behavior as `ImageUploader`; it posts to `POST /v1/transactions/{transactionId}/receipt` after the transaction exists.
2. The service authorizes transaction ownership, generates `receipts/{userId}/{transactionId}/{uuid}-{sanitizedStem}.{ext}`, and uploads to the existing private `note-images` bucket. The shared bucket is intentional: it avoids a duplicate policy/configuration surface while the distinct prefix and receipt metadata table retain clear ownership and lifecycle boundaries.
3. The service enforces one receipt with `unique (transaction_id)`. It writes receipt metadata only after Storage succeeds; metadata failure triggers best-effort object cleanup. Replacement must upload the new object, atomically switch metadata, then attempt old-object cleanup so an existing receipt is not silently lost.
4. The access endpoint reauthorizes through `transaction_receipts -> transactions`, returns a one-hour signed URL, and no signed URL or Storage path is persisted in a transaction description. Deleting a receipt removes object and metadata through the same retry-aware cleanup sequence used for note images.

## Supabase Setup

### Required extensions and shared database behavior

- Enable `pgcrypto` for `gen_random_uuid()`.
- Maintain a `set_updated_at()` trigger function and attach it to every mutable primary table (`admins`, `tasks`, `projects`, `tags`, `notes`, `accounts`, `categories`, `transactions`, `budgets`, and `recurring_transactions`). The database—not the browser clock—owns `updated_at`.
- Use `timestamptz` for creation/update/completion timestamps. Store a task’s date-only due date as `date`, avoiding accidental day shifts across time zones.
- All IDs are UUIDs. All user-owned app tables are in the `public` schema and are protected by RLS from the first migration, not retrofitted later. `public.admins` is backend-only identity data and is never exposed to the browser or Supabase Data API.
- The Vue application never receives a database connection string and never connects to Supabase Postgres. The Go API is the sole application database client.

### Tables and columns

`public.admins` is the local identity source for this implementation. It contains an opaque UUID, username, bcrypt password hash, and timestamps; it is created before every table that references it. Every application row references `public.admins(id)` through `user_id uuid not null`. Supabase Auth is not used by the running application.

| Table | Required columns and constraints |
| --- | --- |
| `projects` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `name varchar(80) not null`, `color varchar(7) null`, `is_archived boolean not null default false`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require a non-blank trimmed name and a case-insensitive unique project name per user. Add `unique (id, user_id)` for ownership-safe composite foreign keys. |
| `tags` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `name varchar(40) not null`, `color varchar(7) null`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require non-blank names and a case-insensitive unique tag name per user. Add `unique (id, user_id)`. |
| `tasks` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `title varchar(280) not null`, `description text null`, `status text not null default 'open' check (status in ('open', 'completed'))`, `completed_at timestamptz null`, `due_date date null`, `priority smallint not null default 0 check (priority between 0 and 3)`, `project_id uuid null`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require a trimmed non-blank title, and require `completed_at` exactly when `status = 'completed'`. Use composite FK `(project_id, user_id) references projects(id, user_id) on delete set null (project_id)` so a task cannot reference another user’s project while retaining its required owner. Add `unique (id, user_id)`. |
| `task_tags` | `user_id uuid not null references public.admins(id) on delete cascade`, `task_id uuid not null`, `tag_id uuid not null`, `created_at timestamptz not null default now()`, primary key `(task_id, tag_id)`. Use composite FKs `(task_id, user_id) references tasks(id, user_id) on delete cascade` and `(tag_id, user_id) references tags(id, user_id) on delete cascade`; this prevents cross-account links. |
| `notes` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `title varchar(280) not null`, `content_md text not null default ''`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require a trimmed non-blank title. Add `unique (id, user_id)`. Markdown, including its `note-image:` IDs, is canonical content. |
| `note_tags` | `user_id uuid not null references public.admins(id) on delete cascade`, `note_id uuid not null`, `tag_id uuid not null`, `created_at timestamptz not null default now()`, primary key `(note_id, tag_id)`. Use the same ownership-safe composite-FK pattern as `task_tags`. |
| `note_task_links` | `user_id uuid not null references public.admins(id) on delete cascade`, `note_id uuid not null`, `task_id uuid not null`, `created_at timestamptz not null default now()`, primary key `(note_id, task_id)`. Composite FKs to `(notes.id, user_id)` and `(tasks.id, user_id)` prevent cross-user links. |
| `note_images` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `note_id uuid not null`, `bucket_id text not null default 'note-images' check (bucket_id = 'note-images')`, `object_path text not null`, `original_filename varchar(255) not null`, `mime_type varchar(100) not null`, `byte_size integer not null check (byte_size > 0 and byte_size <= 10485760)`, `alt_text varchar(280) null`, `width integer null check (width is null or width > 0)`, `height integer null check (height is null or height > 0)`, `created_at timestamptz not null default now()`. Add `unique (bucket_id, object_path)` and a composite FK `(note_id, user_id) references notes(id, user_id) on delete cascade`. |
| `accounts` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `name varchar(80) not null`, `account_type text not null check (account_type in ('cash', 'bank', 'e_wallet', 'credit_card'))`, `currency char(3) not null default 'IDR' check (currency = 'IDR')`, `archived_at timestamptz null`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require a non-blank trimmed name and case-insensitive unique account name per user. Add `unique (id, user_id)`. Do not add `balance` or `current_balance`: derive it from `transactions`. IDR is the confirmed v1 base currency. |
| `categories` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `name varchar(80) not null`, `category_type text not null check (category_type in ('income', 'expense'))`, `icon varchar(80) null`, `color varchar(7) null`, `archived_at timestamptz null`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require non-blank names and a case-insensitive unique `(user_id, category_type, name)`. Add `unique (id, user_id)`. Seed ordinary editable defaults (Food, Transport, Bills, Salary, Shopping, Health; include sensible income defaults such as Salary and Other Income) at user onboarding or first money-module use, idempotently in the service. |
| `transactions` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `transaction_type text not null check (transaction_type in ('income', 'expense', 'transfer'))`, `amount numeric(18,2) not null check (amount > 0)`, `transaction_date date not null`, `account_id uuid not null`, `transfer_account_id uuid null`, `category_id uuid null`, `description varchar(1000) null`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Use ownership-safe composite FKs `(account_id, user_id) references accounts(id, user_id) on delete restrict`, `(transfer_account_id, user_id) references accounts(id, user_id) on delete restrict`, and `(category_id, user_id) references categories(id, user_id) on delete restrict`. Check that income/expense has a category and no transfer account; transfer has a distinct transfer account and no category. The service also verifies category type matches income/expense. A transfer is one row and balances subtract `amount` from `account_id` and add it to `transfer_account_id`, guaranteeing equal legs atomically. Add `unique (id, user_id)`. |
| `transaction_receipts` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `transaction_id uuid not null`, `bucket_id text not null default 'note-images' check (bucket_id = 'note-images')`, `object_path text not null`, `original_filename varchar(255) not null`, `mime_type varchar(100) not null`, `byte_size integer not null check (byte_size > 0 and byte_size <= 10485760)`, `width integer null check (width is null or width > 0)`, `height integer null check (height is null or height > 0)`, `created_at timestamptz not null default now()`. Add `unique (transaction_id)`, `unique (bucket_id, object_path)`, and `(transaction_id, user_id) references transactions(id, user_id) on delete cascade`. |
| `budgets` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `category_id uuid not null`, `period_start date not null`, `period_end date not null`, `amount_limit numeric(18,2) not null check (amount_limit > 0)`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require `period_end >= period_start`; use `(category_id, user_id) references categories(id, user_id) on delete restrict`; add `unique (user_id, category_id, period_start, period_end)`. The service permits only expense categories. Monthly is a UI/API convenience that supplies the calendar month bounds. |
| `recurring_transactions` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references public.admins(id) on delete cascade`, `transaction_type text not null check (transaction_type in ('income', 'expense'))`, `amount numeric(18,2) not null check (amount > 0)`, `account_id uuid not null`, `category_id uuid not null`, `description varchar(1000) null`, `cadence text not null check (cadence in ('weekly', 'monthly', 'yearly'))`, `next_due_date date not null`, `ends_on date null check (ends_on is null or ends_on >= next_due_date)`, `is_active boolean not null default true`, `last_processed_on date null`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Use composite owner-safe FKs to accounts/categories with `on delete restrict`; the service verifies category type. `last_processed_on` plus a row lock during confirmation prevents duplicate occurrence creation. |

Add indexes appropriate to the list and search paths: `(user_id, status, due_date)`, `(user_id, project_id)`, `(user_id, updated_at desc)` for tasks; `(user_id, updated_at desc)` for notes; and indexes on all junction-table `user_id` and foreign-key columns. For Money, add `(user_id, archived_at)` to accounts/categories; `(user_id, transaction_date desc, id desc)`, `(user_id, account_id, transaction_date desc)`, `(user_id, category_id, transaction_date desc)`, and `(user_id, transaction_type, transaction_date desc)` to transactions; `(user_id, category_id, period_start, period_end)` to budgets; and partial `(user_id, next_due_date) where is_active` to recurring transactions. The account/category composite-FK columns are indexed as listed, and the receipt foreign key has an index. Add a maintained `tsvector`/GIN index for transaction descriptions only after basic parameterized `ILIKE` search has profiling evidence to justify it; the endpoint must stay user-scoped and cursor-bounded. Do not query every note body or every transaction row just to render a list or report.

### Row Level Security policy approach

1. Enable and force RLS on every public application table.
2. Existing Tasks/Notes policies are retained as-is. Every Money table uses explicit `SELECT`, `INSERT`, `UPDATE`, and `DELETE` policies comparing `user_id` to `current_setting('request.jwt.claim.sub', true)::uuid`, which is transaction-locally set only after Go verifies the local-admin JWT. Do not use a Supabase Auth role or `auth.uid()` for the Money tables.
3. Every Money insert policy must use that comparison in `with check`; every update policy must have both `using` and the same `with check`. Never trust a browser-supplied `user_id`; the Go JWT middleware supplies the verified subject to the transaction-local claim and the service/repository layer lets RLS enforce it.
4. The junction tables intentionally carry `user_id` and composite FKs. This makes their RLS policy equally simple and ensures their linked records have the same owner at the database level.
5. Test RLS with two real test users through the Go API and with a direct app-role database test. Verify joined reads, foreign-key writes, and image-metadata CRUD all reject the other user. Do not use privileged backend credentials as a workaround for a failed policy, and never expose them to the browser.
6. The new Money tables follow the same policy approach. Receipt access is authorized both by receipt `user_id` policy and by the service’s ownership join to `transactions`; test cross-user account/category/transaction/budget/recurring/receipt reads and mutations, including attempted foreign-key links to another user’s account or category.

### Storage bucket for note images and receipts

- Create a **private** Supabase Storage bucket named `note-images`; do not enable public reads.
- Set an allowed MIME type list of `image/jpeg`, `image/png`, `image/webp`, and `image/gif`, with a per-object limit of **10 MiB**. Reject SVG because it can carry active content; do not accept unsupported formats merely by filename extension.
- Object names must follow `notes/<user_id>/<note_id>/<uuid>-<sanitized-stem>.<ext>` or `receipts/<user_id>/<transaction_id>/<uuid>-<sanitized-stem>.<ext>`. The UUID prevents collisions; sanitize the filename stem and retain the original name separately in metadata.
- Reuse the existing private `note-images` bucket for receipt images. Its `storage.objects` policies must limit to `bucket_id = 'note-images'`, an allowed first folder of `notes` or `receipts`, and the transaction-local claim `(storage.foldername(name))[2] = current_setting('request.jwt.claim.sub', true)`; apply this ownership check consistently to select, insert, update, and delete. These remain a defence-in-depth boundary: the backend’s server-only Storage key bypasses them only after Go has authorized the local JWT subject, and the browser never calls Storage directly.
- Before uploading, the Go service verifies that the note belongs to the JWT subject. Insert `note_images` metadata only after the object upload succeeds. If metadata insertion fails, attempt to remove the newly uploaded object and return an actionable API error.
- For display, the Go API creates short-lived signed URLs (target one hour) after authorizing image ownership and returns them from its access endpoint. Persist `note-image:<uuid>` in Markdown, never a signed URL or a bare Storage path.
- On image removal, remove the Storage object and its metadata in a recoverable, retry-aware sequence. On note deletion, list/remove its image objects before deleting the note metadata; add a future scheduled orphan cleanup only if production evidence requires it.

### Environment variables and client setup

Use `frontend/.env.example` and `backend/.env.example` with placeholder values only:

```dotenv
# frontend/.env.example — every VITE_ value is public by design
# Empty means same-origin: Vite proxies to Go in development and the embedded
# Go server handles the API in production.
VITE_API_BASE_URL=

# backend/.env.example — never expose or prefix these with VITE_
DATABASE_URL=postgresql://app_api:password@host:6543/postgres?sslmode=require
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SECRET_KEY=your-server-only-secret-key
JWT_SECRET=your-32-byte-server-only-signing-key
# Optional. Set only when deliberately serving the frontend cross-origin.
CORS_ALLOWED_ORIGIN=http://localhost:5173
```

`frontend/src/lib/auth/supabaseAuth.ts` must fail clearly in development when Auth configuration is absent and instantiate an Auth-only browser client. `frontend/src/lib/api/client.ts` must require that `VITE_API_BASE_URL` is defined (an explicit empty value means same-origin) and attach the current Bearer token. `backend/internal/config` must fail startup when private database, Storage, or JWT configuration is absent; `CORS_ALLOWED_ORIGIN` is optional and only needed for a cross-origin frontend. Never commit `.env`, production secrets, access tokens, SQL dumps, database passwords, or `SUPABASE_SECRET_KEY`. Only the API base URL and Supabase public Auth configuration may use `VITE_` variables.

## Deployment

### Development

Development intentionally uses two processes so the Vue app retains Vite hot-module reload:

```bash
make dev
```

`make dev` starts Vite and Go together and stops both on Ctrl-C or when either process exits. Vite listens on `http://localhost:5173` and proxies `/v1` and `/healthz` to the Go API on `http://localhost:8080`. Keep `VITE_API_BASE_URL=` in `frontend/.env.local` (the value in `frontend/.env.example`) to use that proxy. `CORS_ALLOWED_ORIGIN` is optional in this mode because the browser calls Vite's origin; set it to `http://localhost:5173` only when deliberately configuring the frontend to call Go directly.

### Production

Production is one Go binary on one port. `make build` runs Vite in `frontend/`, writes its generated bundle to `backend/internal/httpapi/webui/dist/`, and then compiles it into the Go executable through `embed`. Go serves static files, preserves `/healthz` and all existing `/v1/*` API handlers, and returns the embedded `index.html` for non-API paths so Vue Router refreshes do not 404.

```bash
make build                 # creates dist/tasker
make run                   # runs the already-built dist/tasker

# Equivalent explicit build steps
npm --prefix frontend run build
go -C backend build -o ../dist/tasker ./cmd/api
```

Deploy `dist/tasker` with the backend environment variables and run `./tasker`; the Go application does not load a `.env` file itself, so inject them through the VPS service manager or export them before launch. No Node, Vite, Nginx, external static-files process, or static-assets path configuration is required at runtime. `frontend/.env.production` deliberately sets `VITE_API_BASE_URL=` so the compiled SPA calls the Go API on the same origin. Leave `CORS_ALLOWED_ORIGIN` unset in this deployment. This keeps the production footprint appropriate for a 2 GB VPS: only the Go process and its normal database/network connections remain resident.

## Build & CI/CD Environment Rules

> **IMPORTANT FOR AGENTS / AI ASSISTANTS**:
> - **No Local Builds**: Do NOT attempt to compile full production binaries or run Android Gradle builds (`./gradlew assemble`, APK/AAB builds, etc.) on this local machine.
> - **GitHub CI/CD**: All builds, binary packaging, and APK/AAB compilations are executed automatically in **GitHub Actions CI/CD**.
> - **Agent Workflow**: Focus on code accuracy, unit tests, static checks (`go vet`, `npm run lint`), and documentation. Let GitHub CI handle compilation and artifact builds.

## Coding Conventions

- Use TypeScript strict mode in `frontend`. Avoid `any`; use `unknown` at untrusted boundaries and narrow it. Prefer discriminated unions for mutation/loading/error states. Use `go vet`-clean idiomatic Go, explicit error wrapping with `%w`, and `context.Context` as the first parameter of request-scoped Go methods.
- Use PascalCase for Vue components and TypeScript type/interface names, camelCase for functions/variables/composables, and kebab-case for non-component frontend filenames. Vue composables start with `use`. Use short lowercase Go package names, exported Go identifiers only when needed across packages, and no package-level mutable request state.
- Prefer named TypeScript exports for composables, helpers, and types. Vue route components may be imported by default where the router benefits. Keep Go dependencies injected through constructors/interfaces at the service and adapter boundaries rather than globals.
- Keep components small and accessible: semantic HTML first, real `<button>` and `<label>` elements, keyboard support, visible focus, and no clickable non-interactive elements.
- Validate user input in Vue for immediate feedback, in Go services for trusted API enforcement, and with database constraints for integrity. Trim titles/names before validation; enforce the database’s length limits in the UI and API.
- Markdown rendering must be sanitized. Do not allow raw HTML, arbitrary URL protocols, or unvalidated HTML attributes. `note-image:` is a controlled custom token whose API image-access request must be authorized for the current note/user.
- Centralize date formatting/parsing and render date-only task due dates in local calendar terms. Do not serialize date-only values through `new Date()` in a way that shifts time zones.
- Use conventional commit messages: `feat(tasks): add priority filter`, `fix(notes): retain image alt text`, `docs: clarify storage policy`, `chore(api): regenerate contract types`. Keep commits focused and do not mix refactors with behavior changes without a clear reason.

## Performance Guardrails

- Before adding a dependency, prefer platform APIs or a small local component. Record why any dependency that materially increases the bundle is necessary.
- Keep the initial JavaScript payload at or below the PRD target of 250 KB gzipped. Inspect production builds for regressions; code-split Markdown preview, image compression, and any infrequently used modal/editor UI.
- Fetch selected fields, paginate list/search API responses, debounce text search (~200–300 ms), and cancel/ignore stale HTTP responses. Do not issue a request per list row.
- Use optimistic UI only where rollback is clear (task completion and simple metadata edits). Surface a retryable error and restore state if the mutation fails.
- Avoid unnecessary Vue updates: keep form state local, derive state with `computed`, use stable list keys based on database IDs, and do not add `watch`/reactivity layers without a clear data-flow need.
- Create image thumbnails/previews client-side where possible, strip oversized dimensions before sending uploads to Go, and use lazy image loading. Stream multipart data through Go rather than buffering it unnecessarily; keep original uploads within the 10 MiB hard limit and target substantially smaller files for normal note images.
- Do not poll for changes or subscribe to Supabase Realtime in v1. The API returns fresh data deliberately after mutations.

## Definition of Done

An agent may consider a feature complete only when all applicable items are true:

- [ ] The feature maps to a stated [PRD.md](PRD.md) requirement and does not expand v1 scope unintentionally.
- [ ] Vue UI/composable/API-client and Go handler/service/repository/storage changes live in the correct layers; no component has raw API/SQL/Supabase access and no handler contains business logic.
- [ ] TypeScript passes in strict mode with no new `any`, unsafe casts, or ignored errors; Go is formatted, builds, and passes `go vet` with no ignored errors.
- [ ] Database migrations, API contract/types, constraints, indexes, and RLS policies are updated together when the data model or API changes.
- [ ] RLS and private Storage behavior are respected; the browser has no database, Storage, service-role, or database-password credential, and backend access is protected by verified JWT middleware.
- [ ] Loading, empty, validation, success, failure, and retry states are handled; there are no uncaught promise rejections or console errors.
- [ ] The result is usable with keyboard and touch, and has been checked at mobile (~320–390 px) and desktop widths.
- [ ] Lists/search remain bounded, images are validated, and no unnecessary heavyweight dependency or N+1 request was introduced.
- [ ] Relevant Vue unit checks, Go handler/service/repository tests, and a manual happy-path test pass, including a second-user isolation and JWT-rejection test for data-access changes.
- [ ] Money calculations are checked against independently calculated transaction sums: income adds, expense subtracts, a transfer affects both distinct accounts equally and does not affect income/expense totals, and archived accounts/categories remain visible in historical data.
- [ ] Budget aggregation includes only expense transactions for the selected category and inclusive period; boundary dates, edits/deletes, and over-budget transitions are covered by tests.
- [ ] Recurring confirmation is idempotent under a duplicate/retried request and never creates a transaction automatically; due, skip, and end-date behavior are covered by tests.
- [ ] Receipt uploads use the private shared bucket and prefix policy, leave no metadata for failed uploads, preserve an old receipt until a replacement succeeds, and cannot be read by another user.
- [ ] The lazy-loaded reports route stays within the initial bundle target; its `uPlot` trend and native category graphic have accessible text alternatives.
- [ ] Documentation, `api/openapi.yaml`, and both `.env.example` files are updated when setup, behavior, or configuration changes.

## Open Questions / Assumptions

These are deliberate recommendations made to make scaffolding possible; confirm or revise them before implementation where preference matters.

- **Auth:** v1 uses the confirmed local-admin username/password and backend-signed JWT model. Supabase Auth migration is deliberately out of scope for this feature.
- **Editor:** v1 uses Markdown with a sanitized preview instead of a WYSIWYG editor. This keeps the app fast and makes image references portable; confirm if a visual editor is a requirement.
- **Organization:** tags are shared across tasks and notes; projects organize tasks only; note folders are excluded. Confirm whether folders are more valuable than tags for personal notes.
- **Task scope:** v1 has no subtasks, recurring tasks, reminders, or calendar sync. These are intentionally postponed rather than partially modeled.
- **Deletion:** v1 permanently deletes tasks, notes, and removed images, with no trash/recovery window. Confirm if a recoverable trash is needed before users store important material.
- **Image policy:** JPEG, PNG, WebP, and GIF up to 10 MiB are supported. Confirm if HEIC support, animated GIF handling, or image annotation is needed.
- **Search:** v1 starts with private Postgres-backed search and pagination, not a third-party search service. Confirm if fuzzy matching or advanced query syntax is important.
- **Hosting:** Production deploys as the single embedded Go binary documented above. Confirm the preferred VPS operating-system service manager and Supabase region before deployment.
- **Money currency:** Confirmed: v1 stores and reports `IDR` only; no exchange-rate or cross-currency totals are built.
- **Recurring money:** Confirmed: v1 uses an explicit due prompt followed by user confirmation or skip, not unattended background transaction creation.
- **Financial deletion:** Accounts/categories with historical transactions or budgets are archived instead of deleted; hard deletion is blocked until dependencies are intentionally removed or reassigned. Confirm this preservation-first behavior before Phase A.
