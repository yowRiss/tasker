# Engineering / Agent Guide

## Project Summary

This repository will contain a personal, single-user web workspace for tasks and Markdown notes with private image embeds. The product scope, user experience, and explicit exclusions live in [PRD.md](PRD.md); treat that document as the source of truth for what belongs in v1. Build with enterprise-quality separation, typing, security, and testing, but do not introduce collaboration or multi-tenant product complexity beyond secure per-user isolation.

## Tech Stack & Key Decisions

- **Frontend build/runtime:** Vue 3 with the Composition API, Vite, and TypeScript with `strict: true`. Use Vue Router for `/tasks`, `/tasks/:taskId`, `/notes`, `/notes/:noteId`, and `/search`; route views may be lazy-loaded. Do not introduce a Vue meta-framework or server-rendering layer for v1.
- **Styling:** component-scoped plain CSS plus global CSS custom properties in `frontend/src/styles/tokens.css` and `frontend/src/styles/global.css`. No UI component suite, Tailwind, CSS-in-JS runtime, or icon pack unless a measured requirement justifies it. Use small inline SVG Vue components for the needed icon set.
- **Frontend data/state:** domain-specific Composition API composables and a small typed `fetch` client are the default. Keep form state in the component and session/sidebar state in focused composables. Do not add Pinia unless a demonstrated cross-route, shared client-state problem cannot be solved cleanly with composables; do not add a query library in v1.
- **Go API:** use Go with `github.com/go-chi/chi/v5`, `pgx/v5` and `pgxpool`. Chi is a small, idiomatic `net/http` router with first-class middleware and no framework lock-in; pgx provides a fast PostgreSQL pool and explicit SQL without an ORM. The API is the only application component that queries Supabase Postgres or calls Supabase Storage REST.
- **Authentication:** Supabase Auth, with magic-link email as the default sign-in experience. Email/password can be enabled later without changing ownership or RLS. The Vue app uses `@supabase/supabase-js` **only** for Auth sign-in, session refresh, and access-token retrieval, because it correctly handles the Supabase browser session lifecycle. It must not import or call `from()`, `rpc()`, or `storage`. The API receives the access token in `Authorization: Bearer <jwt>` and verifies it against Supabase’s cached JWKS before protected requests run.
- **Notes:** Markdown is the canonical note format. Use a small, sanitized Markdown renderer with a source editor and preview; do not add a WYSIWYG editor. Persist image references as `note-image:<note_image_id>` in Markdown and resolve them to short-lived signed URLs when rendering.
- **Images:** validate and, where beneficial, resize/compress images in the browser before sending them to the Go API. The API authorizes the note owner, uploads to private Supabase Storage through its REST API, and writes image metadata to Postgres. The browser never uploads to Storage directly.
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
        AuthGate.vue
        useAuth.ts
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
    auth/                    # JWT verifier, JWKS cache, authenticated principal
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
| System and identity | `GET /healthz`; `GET /v1/me` | Liveness/readiness and authenticated identity/session diagnostics. Authentication itself remains with Supabase Auth in the frontend. |
| Tasks | `GET, POST /v1/tasks`; `GET, PATCH, DELETE /v1/tasks/{taskId}`; `PATCH /v1/tasks/{taskId}/completion` | List/filter/search, create, edit/delete, and explicitly complete or reopen a task. List parameters cover the PRD filters, sort, direction, `limit`, and cursor. |
| Projects and tags | `GET, POST /v1/projects`; `PATCH, DELETE /v1/projects/{projectId}`; `GET, POST /v1/tags`; `PATCH, DELETE /v1/tags/{tagId}` | Manage the user-owned organizational data. Task/note create and patch payloads set tag IDs atomically. |
| Notes | `GET, POST /v1/notes`; `GET, PATCH, DELETE /v1/notes/{noteId}` | List/search notes with bounded summaries; fetch a full Markdown body only for a selected note; create, edit, and delete. |
| Note/task links | `PUT, DELETE /v1/notes/{noteId}/tasks/{taskId}` | Add or remove the existing many-to-many note/task link. |
| Note images | `POST /v1/notes/{noteId}/images`; `PATCH, DELETE /v1/note-images/{imageId}`; `GET /v1/note-images/{imageId}/access` | Upload an image as `multipart/form-data`, edit alt text, delete an image, or obtain a short-lived display URL for an authorized image. The upload response includes the image metadata and its `note-image:<id>` Markdown token. |
| Search | `GET /v1/search?q={query}&scope=all|tasks|notes&limit={n}&cursor={cursor}` | Return bounded, typed global-search results. |

Do not create Go endpoints that proxy password or magic-link credentials. The frontend talks to Supabase Auth only for those Auth operations, then calls this API for every product-data operation.

### Layers, transactions, and data access

1. **Chi router and middleware** establish request IDs, safe CORS, recovery, structured logs, request limits, and the authenticated principal.
2. **Handlers** decode and validate transport shapes, call one service method, and encode success or problem responses. They contain no SQL and no Supabase Storage logic.
3. **Services** enforce use-case rules, pass the verified principal to all data operations, coordinate database transactions, and run compensating cleanup where a Storage operation and database write cannot be one transaction.
4. **Postgres repositories** use parameterized `pgx` queries only. They map database rows to domain values and always scope access by the principal’s user ID, even though RLS is also enforced.
5. **Storage adapter** is the only code that calls Supabase Storage’s REST API. It receives an already authorized user ID, note ID, and generated object path—not a browser-supplied path.

Use a dedicated non-owner Postgres login role for the Go API with `NOBYPASSRLS` and only the required table/sequence grants. Each protected request opens a transaction and runs `select set_config('request.jwt.claim.sub', $1, true)` with the verified subject before repository queries, so `auth.uid()` RLS policies apply to direct `pgx` connections. The setting must be transaction-local; never put an end-user identity into pooled-connection session state. The repositories must also include `user_id = $principalID` predicates as defense in depth.

### Error handling

- Return errors as `application/problem+json` following RFC 9457, with `type`, `title`, `status`, a stable machine-readable `code`, and `request_id`. Include field-level validation details only when safe.
- Use `400` for malformed JSON/parameters, `401` for missing or invalid JWTs, `403` for authenticated but prohibited actions, `404` for an unavailable resource without revealing another user’s record, `409` for unique/consistency conflicts, `413` for over-limit uploads, `415` for unsupported media, `422` for validly parsed but invalid input, and `500` for unexpected failures.
- Log the underlying error with its request ID server-side. Never return SQL details, Storage credentials, JWT contents, or raw upstream Supabase errors to the client.

### JWT middleware

- The frontend gets the Supabase Auth access token and sends `Authorization: Bearer <JWT>` on every protected API request. It refreshes the session through the Auth-only client before retrying an expired request.
- Middleware parses the token, validates the signature using Supabase’s JWKS with a bounded, refreshable cache, and validates issuer, audience (`authenticated`), expiry/not-before, and a UUID `sub` claim. The resulting principal contains the `sub` user ID and only the claims the services need.
- Reject malformed, expired, wrong-project, wrong-audience, and unverifiable tokens before a handler runs. Do not accept user identity in a JSON body, path, or query parameter as a substitute for the verified subject.
- Keep Supabase URL, expected issuer, and audience in backend configuration. Rotate keys by refreshing JWKS on cache miss/key-ID change and on a bounded interval; never hard-code a JWT signing secret in the frontend.

### Image upload flow

1. The Vue editor validates the chosen image, optionally reduces oversized pixel dimensions, and sends it as multipart data to `POST /v1/notes/{noteId}/images` with the Bearer token. It does not call Supabase Storage.
2. The Go handler applies `http.MaxBytesReader` with an 11 MiB request cap, separately enforces the 10 MiB file limit, MIME-sniffs the stream rather than trusting the filename, and accepts only the image types allowed in the Storage policy.
3. The service confirms that the selected note belongs to the JWT subject, creates an image UUID and the canonical `notes/{userId}/{noteId}/{uuid}-{sanitizedStem}.{ext}` object path, and passes the stream to the Storage adapter.
4. The Storage adapter uploads through Supabase Storage REST using a backend-only credential. After a successful object write, the repository inserts the `note_images` row in the user-scoped database transaction.
5. If the metadata insert fails, the service attempts Storage-object deletion and logs any cleanup failure with the request ID. It returns success only when both the object and metadata exist.
6. The API returns `201` with metadata and `note-image:<imageId>`; the Vue editor inserts that token only after success. For preview, it requests `/v1/note-images/{imageId}/access`; Go reauthorizes ownership and creates the short-lived signed URL. The browser uses that returned URL only as an image resource and never calls the Supabase SDK or Storage REST API itself.

## Supabase Setup

### Required extensions and shared database behavior

- Enable `pgcrypto` for `gen_random_uuid()`.
- Maintain a `set_updated_at()` trigger function and attach it to every mutable primary table (`tasks`, `projects`, `tags`, and `notes`). The database—not the browser clock—owns `updated_at`.
- Use `timestamptz` for creation/update/completion timestamps. Store a task’s date-only due date as `date`, avoiding accidental day shifts across time zones.
- All IDs are UUIDs. All app tables are in the `public` schema and are protected by RLS from the first migration, not retrofitted later.
- The Vue application never receives a database connection string and never connects to Supabase Postgres. The Go API is the sole application database client.

### Tables and columns

`auth.users` is owned by Supabase Auth. Do not create a duplicate public `users` table in v1. Every application row references `auth.users(id)` through `user_id uuid not null`.

| Table | Required columns and constraints |
| --- | --- |
| `projects` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references auth.users(id) on delete cascade`, `name varchar(80) not null`, `color varchar(7) null`, `is_archived boolean not null default false`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require a non-blank trimmed name and a case-insensitive unique project name per user. Add `unique (id, user_id)` for ownership-safe composite foreign keys. |
| `tags` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references auth.users(id) on delete cascade`, `name varchar(40) not null`, `color varchar(7) null`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require non-blank names and a case-insensitive unique tag name per user. Add `unique (id, user_id)`. |
| `tasks` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references auth.users(id) on delete cascade`, `title varchar(280) not null`, `description text null`, `status text not null default 'open' check (status in ('open', 'completed'))`, `completed_at timestamptz null`, `due_date date null`, `priority smallint not null default 0 check (priority between 0 and 3)`, `project_id uuid null`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require a trimmed non-blank title, and require `completed_at` exactly when `status = 'completed'`. Use composite FK `(project_id, user_id) references projects(id, user_id) on delete set null (project_id)` so a task cannot reference another user’s project while retaining its required owner. Add `unique (id, user_id)`. |
| `task_tags` | `user_id uuid not null references auth.users(id) on delete cascade`, `task_id uuid not null`, `tag_id uuid not null`, `created_at timestamptz not null default now()`, primary key `(task_id, tag_id)`. Use composite FKs `(task_id, user_id) references tasks(id, user_id) on delete cascade` and `(tag_id, user_id) references tags(id, user_id) on delete cascade`; this prevents cross-account links. |
| `notes` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references auth.users(id) on delete cascade`, `title varchar(280) not null`, `content_md text not null default ''`, `created_at timestamptz not null default now()`, `updated_at timestamptz not null default now()`. Require a trimmed non-blank title. Add `unique (id, user_id)`. Markdown, including its `note-image:` IDs, is canonical content. |
| `note_tags` | `user_id uuid not null references auth.users(id) on delete cascade`, `note_id uuid not null`, `tag_id uuid not null`, `created_at timestamptz not null default now()`, primary key `(note_id, tag_id)`. Use the same ownership-safe composite-FK pattern as `task_tags`. |
| `note_task_links` | `user_id uuid not null references auth.users(id) on delete cascade`, `note_id uuid not null`, `task_id uuid not null`, `created_at timestamptz not null default now()`, primary key `(note_id, task_id)`. Composite FKs to `(notes.id, user_id)` and `(tasks.id, user_id)` prevent cross-user links. |
| `note_images` | `id uuid primary key default gen_random_uuid()`, `user_id uuid not null references auth.users(id) on delete cascade`, `note_id uuid not null`, `bucket_id text not null default 'note-images' check (bucket_id = 'note-images')`, `object_path text not null`, `original_filename varchar(255) not null`, `mime_type varchar(100) not null`, `byte_size integer not null check (byte_size > 0 and byte_size <= 10485760)`, `alt_text varchar(280) null`, `width integer null check (width is null or width > 0)`, `height integer null check (height is null or height > 0)`, `created_at timestamptz not null default now()`. Add `unique (bucket_id, object_path)` and a composite FK `(note_id, user_id) references notes(id, user_id) on delete cascade`. |

Add indexes appropriate to the list and search paths: `(user_id, status, due_date)`, `(user_id, project_id)`, `(user_id, updated_at desc)` for tasks; `(user_id, updated_at desc)` for notes; and indexes on all junction-table `user_id` and foreign-key columns. Once basic search is working, add maintained `tsvector` search columns/indexes for task title/description and note title/content. Do not query every note body just to render a list.

### Row Level Security policy approach

1. Enable and force RLS on every public application table.
2. For each table above, create explicit `SELECT`, `INSERT`, `UPDATE`, and `DELETE` policies whose essential condition is `auth.uid() = user_id`.
3. Every insert policy must use `with check (auth.uid() = user_id)`; every update policy must have both `using (auth.uid() = user_id)` and the same `with check`. Never trust a browser-supplied `user_id`; the Go JWT middleware supplies the verified subject to a transaction-local claim and the service/repository layer lets RLS enforce it.
4. The junction tables intentionally carry `user_id` and composite FKs. This makes their RLS policy equally simple and ensures their linked records have the same owner at the database level.
5. Test RLS with two real test users through the Go API and with a direct app-role database test. Verify joined reads, foreign-key writes, and image-metadata CRUD all reject the other user. Do not use privileged backend credentials as a workaround for a failed policy, and never expose them to the browser.

### Storage bucket for note images

- Create a **private** Supabase Storage bucket named `note-images`; do not enable public reads.
- Set an allowed MIME type list of `image/jpeg`, `image/png`, `image/webp`, and `image/gif`, with a per-object limit of **10 MiB**. Reject SVG because it can carry active content; do not accept unsupported formats merely by filename extension.
- Object names must follow `notes/<user_id>/<note_id>/<uuid>-<sanitized-stem>.<ext>`. The UUID prevents collisions; sanitize the filename stem and retain the original name separately in metadata.
- Create `storage.objects` policies limited to `bucket_id = 'note-images'`, `(storage.foldername(name))[1] = 'notes'`, and `(storage.foldername(name))[2] = auth.uid()::text`. Apply this ownership check consistently to select, insert, update, and delete. These remain a defence-in-depth boundary; the backend service credential may bypass them only after the API has authorized the JWT subject.
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
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_PUBLISHABLE_KEY=your-publishable-anon-key

# backend/.env.example — never expose or prefix these with VITE_
DATABASE_URL=postgresql://app_api:password@host:6543/postgres?sslmode=require
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SECRET_KEY=your-server-only-secret-key
SUPABASE_JWT_ISSUER=https://your-project.supabase.co/auth/v1
SUPABASE_JWT_AUDIENCE=authenticated
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
- [ ] Documentation, `api/openapi.yaml`, and both `.env.example` files are updated when setup, behavior, or configuration changes.

## Open Questions / Assumptions

These are deliberate recommendations made to make scaffolding possible; confirm or revise them before implementation where preference matters.

- **Auth:** magic-link email is the default. Confirm whether password sign-in, a particular email provider, or single-device-only access is desired.
- **Editor:** v1 uses Markdown with a sanitized preview instead of a WYSIWYG editor. This keeps the app fast and makes image references portable; confirm if a visual editor is a requirement.
- **Organization:** tags are shared across tasks and notes; projects organize tasks only; note folders are excluded. Confirm whether folders are more valuable than tags for personal notes.
- **Task scope:** v1 has no subtasks, recurring tasks, reminders, or calendar sync. These are intentionally postponed rather than partially modeled.
- **Deletion:** v1 permanently deletes tasks, notes, and removed images, with no trash/recovery window. Confirm if a recoverable trash is needed before users store important material.
- **Image policy:** JPEG, PNG, WebP, and GIF up to 10 MiB are supported. Confirm if HEIC support, animated GIF handling, or image annotation is needed.
- **Search:** v1 starts with private Postgres-backed search and pagination, not a third-party search service. Confirm if fuzzy matching or advanced query syntax is important.
- **Hosting:** Production deploys as the single embedded Go binary documented above. Confirm the preferred VPS operating-system service manager and Supabase region before deployment.
