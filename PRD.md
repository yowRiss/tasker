# Personal Tasks + Notes — Product Requirements Document

## Overview

Personal Tasks + Notes + Money is a fast, lightweight web app for one person to manage actionable work, reference material, and personal finances in one focused workspace. It combines a simple task manager, Markdown-based notes with embedded images, and practical day-to-day money management without the overhead of a team productivity suite.

## Goals & Non-Goals

### Goals

- Provide one calm, responsive application shell for Tasks, Notes, and Money.
- Make common actions—capture a task, complete it, write a note, log a transaction, and find information—feel immediate.
- Keep personal data private through local-admin JWT verification, Postgres Row Level Security (RLS), and private image storage.
- Use clean domain boundaries and a data model that could later support more users without building collaboration features now.

### Non-Goals

- This is a single-user, personal product. It is not a team, enterprise, or project-management tool.
- V1 has no shared workspaces, teammates, task assignment, comments, mentions, approvals, roles, administration, or data sharing.
- V1 does not attempt to replace a full document editor, calendar, or knowledge-base platform.

## Core Features

### Shared workspace

- Persistent app shell with a compact sidebar/top navigation for Tasks, Notes, and Search.
- Responsive layout: a comfortable multi-column workspace on desktop and a single-column, drawer-based experience on mobile.
- Clear empty, loading, saving, error, and offline-state feedback.

### Task Manager

- Create, view, edit, and permanently delete a task.
- Each task has a required title; optional description, due date, priority, project, and tags.
- Mark a task complete or incomplete. Completing it records a completion timestamp; reopening clears it.
- Use four priority levels: None, Low, Medium, and High.
- Organize tasks into optional projects and reusable tags. Projects may be archived to keep them out of normal selection without deleting their tasks.
- View open and completed tasks; filter by completion state, project, tag, priority, and due-date grouping (overdue, today, upcoming, no due date).
- Sort task results by due date, priority, creation date, or last updated date, in an explicit ascending/descending direction where relevant.
- Search task titles, descriptions, project names, and tag names. Search is debounced and never requires leaving the Tasks area.
- V1 deliberately excludes recurring tasks, dependencies, reminders, custom workflows, and subtasks. A future version can add them without changing the core task identity or ownership model.

### Notes

- Create, view, rename, edit, and permanently delete notes.
- Notes use Markdown as the canonical content format, with an editing view and a formatted preview. This is the lightweight v1 alternative to a heavy rich-text editor.
- Support common Markdown formatting: headings, lists, checkboxes, links, emphasis, code, quotes, and images.
- Upload JPEG, PNG, WebP, or GIF images from a note; store them in Supabase Storage and insert an embedded image reference at the current cursor position.
- Render image references using short-lived signed URLs at view time; never persist signed URLs in note content.
- Store per-image metadata such as original filename, MIME type, size, alt text, and dimensions when available. A user can change alt text or remove an image from a note.
- Organize notes using the same reusable tags as tasks. Folders are intentionally not included in v1; tags and search avoid a second organizational system.
- Optionally link a note to one or more tasks. The task and note views show linked-item chips, and a link can be added or removed from either view.
- Search note titles, Markdown content, and tag names. Results show a concise, safely rendered text excerpt rather than raw Markdown.

### Money Management

- Manage multiple personal accounts: cash, bank, e-wallet, and credit card. An account has a name and ISO 4217 currency; its displayed current balance is always derived from posted transactions, never manually edited.
- V1 supports Indonesian Rupiah (`IDR`) only. Foreign-exchange conversion and multi-currency consolidated reporting are explicitly deferred.
- Record dated income, expense, and transfer entries. Income and expenses have a positive exact-decimal amount, account, category, and optional description; a transfer atomically moves the same amount from one owned account to another and has no category.
- Attach one optional JPEG, PNG, WebP, or GIF receipt image to a transaction. Receipts are private, use the same safe upload/display model as note images, and are never publicly exposed.
- Create, edit, archive, and—only when unused—delete income and expense categories. Categories can have an optional icon and color. New users receive editable defaults including Food, Transport, Bills, Salary, Shopping, and Health.
- Define per-expense-category budgets for a calendar month or any explicit start/end-date period. Show spent amount, remaining amount, percentage, and a clear over-budget state. Budget progress is calculated from posted expense transactions, not client-side cached totals.
- Create recurring income or expense templates for common entries such as rent, salary, and subscriptions. V1 creates a due-item prompt and lets the user confirm, skip, or dismiss it; it does not silently create financial transactions in the background.
- View a Money dashboard with total balance, period income versus expenses, spending by category, and a date trend. Reports are server-aggregated and bounded to the selected period.
- Search and filter transactions by free-text description, date range, category, account, type, and inclusive amount range.
- Preserve financial history: an account or category with transactions cannot be permanently deleted. It can be archived so it is unavailable for new entries while historical reports stay correct; deletion requires the user to first remove or reassign dependent records.

### Unified Search

- A global search view searches both tasks and notes and labels each result type clearly.
- Queries are debounced, support keyboard navigation, and can be scoped to Tasks or Notes.
- Selecting a result opens its detail view while preserving a sensible back path to the search results.

## Non-Functional Requirements

### Performance

- On a simulated Fast 4G connection, render the authenticated application shell in under 1.5 seconds and the initial task or note list in under 2.5 seconds at the 75th percentile.
- Give visual feedback for local interactions within 100 ms. On a normal broadband connection, a create, update, complete, or delete operation should settle within 500 ms at the 95th percentile, excluding an explicit image upload.
- Target an initial JavaScript payload of no more than 250 KB gzipped, excluding browser extensions and assets fetched after route load. Load the Markdown preview and image-processing code only when needed.
- Paginate or limit lists and search results; do not fetch every note body merely to display a list.

### Responsiveness and accessibility

- Support current evergreen desktop and mobile browsers, with layouts usable from 320 px wide through large desktop screens.
- Support keyboard navigation, visible focus states, semantic controls and labels, and sufficient color contrast.
- Use touch-friendly targets and avoid hover-only interactions.

### Offline tolerance

- V1 is online-first; it does not promise offline creation, conflict resolution, or background synchronization.
- If the network drops, show an understandable offline/error state and preserve an in-progress task or note draft locally until the user can retry. Do not silently claim that unsaved data was synced.

### Privacy and data isolation

- Every application record belongs to exactly one authenticated local-admin account.
- RLS must prevent one account from reading or mutating another account’s rows, including junction tables and image metadata.
- Note images live in a private Storage bucket and are displayed through signed URLs or authenticated access only. No data is intentionally public by default.

## User Flows

### Create a task

1. The user opens Tasks and selects **New task**.
2. They enter a title and optionally choose a due date, priority, project, tags, and description.
3. They save with a button or keyboard shortcut.
4. The new task appears in the active list with an immediate pending/saved state.

### Complete a task

1. The user finds an open task in a list or detail view.
2. They activate its completion control.
3. The UI updates immediately, records the completion time after the save succeeds, and moves the task according to the current completed-task filter.
4. The user may activate the control again to reopen it.

### Create a note

1. The user opens Notes and selects **New note**.
2. They enter a title and Markdown content, optionally adding tags and links to relevant tasks.
3. They switch to preview when desired and save.
4. The note is available in the notes list and global search.

### Upload an image into a note

1. While editing a saved note, the user selects an image file or drags one into the editor.
2. The app validates type and size, optionally compresses/resizes the image in the browser, and uploads it to the user’s private Storage path.
3. After Storage and metadata writes succeed, the app inserts a stable `note-image:<image-id>` Markdown reference at the cursor.
4. The preview resolves that reference to a signed image URL. Failed uploads leave no broken Markdown reference and offer a retryable error.

### Add a transaction

1. The user opens Money, selects **New transaction**, and chooses income, expense, or transfer.
2. For income/expense, they enter an exact amount, date, account, category, and optional description; for a transfer, they choose different source and destination accounts.
3. They may attach a receipt image, which is validated and uploaded privately before the transaction save completes.
4. The API records the transaction atomically. The list, account balance, relevant budget, and dashboard summary refresh with a pending/saved state.

### Create a budget

1. The user opens Budgets and selects **New budget**.
2. They select an expense category, choose the current month or a custom start and end date, and enter a positive limit in the base currency.
3. On save, the UI shows current posted spending, remaining amount, percentage used, and an over-budget indicator where applicable.

### View the money dashboard

1. The user opens Money Dashboard and chooses a preset or custom reporting period.
2. The app requests one bounded, server-aggregated summary for the selected period.
3. It shows total balance across active accounts, income versus expense, category spending, and a time trend, with accessible text summaries alongside charts.

### Attach a receipt image

1. While creating or editing a saved transaction, the user selects a receipt image.
2. The app validates type and size, optionally resizes it in the browser, and sends multipart data to the authenticated API.
3. The API authorizes the transaction owner, stores the object privately, and records receipt metadata only after upload succeeds.
4. The UI displays the receipt through a short-lived signed URL. A failed upload leaves the transaction unchanged and offers a retryable error.

### Search tasks and notes

1. The user opens global search and enters a query.
2. After a short debounce, the app returns ranked task and note matches with type, title, and contextual metadata/excerpts.
3. The user optionally scopes results to one module, selects a result, and opens it.

## Data Model (High Level)

- **Local admin accounts** in `public.admins` are the sole identity source. All app-owned rows carry a `user_id` that references `public.admins.id`.
- **tasks** hold an individual task’s title, optional description and due date, priority, completion state/timestamp, optional project, and timestamps.
- **projects** are optional task groupings owned by a user. A project has a name, optional color, archive state, and timestamps. One project can have many tasks; a task belongs to zero or one project.
- **tags** are reusable, user-owned labels shared by Tasks and Notes. **task_tags** and **note_tags** are many-to-many junction tables.
- **notes** hold a title, canonical Markdown content, and timestamps. A note can have many tags and can be linked to many tasks.
- **note_task_links** is a many-to-many junction table for the optional task/note connection. It carries ownership itself to make RLS simple and explicit.
- **note_images** stores image metadata and a private Supabase Storage object path for a particular note. The Markdown body stores a stable image ID reference, not a Storage URL. A note may have many images; deleting a note deletes its metadata and triggers removal of its Storage objects through the application cleanup flow.
- **accounts** hold a user-owned account name, type, ISO 4217 currency, archive state, and timestamps. They intentionally do not store a mutable balance: balances are aggregates of owned transactions. A user has many accounts.
- **categories** hold user-owned income or expense category names with optional icon/color and archive state. A category has many transactions and budgets. Default categories are created for a user without making them special or immutable.
- **transactions** hold a positive `numeric` amount, date, kind (`income`, `expense`, or `transfer`), account, optional category/description, and timestamps. An income adds to its account; an expense subtracts from it; a transfer references a distinct destination account and moves the same amount between the two accounts. An optional transaction may later link to one task or one note, but no Tasks/Notes schema or route changes are part of this feature unless that explicit cross-link is approved.
- **transaction_receipts** stores optional private image metadata and the object path for exactly one receipt per transaction. It uses the existing private image bucket under a receipt-specific prefix.
- **budgets** hold one positive limit for a user-owned expense category over an inclusive start/end date. Monthly budgets are represented by the corresponding calendar-month dates; custom periods use the same model. Progress is the sum of matching expense transactions.
- **recurring_transactions** hold an active income/expense template, cadence, next due date, optional end date, and the account/category/description/amount to prefill a confirmed transaction. They never create a completed transaction without an explicit user confirmation in v1.

## Tech Stack

- **Frontend:** Vue 3 with the Composition API, Vite, TypeScript (strict mode), and Vue Router. Vue’s small, approachable component model and Vite’s fast builds suit a responsive single-page workspace without a meta-framework.
- **Styling:** plain CSS organized by component, plus CSS custom properties for tokens (color, spacing, typography, elevation). This avoids a utility-CSS runtime or component-library dependency while remaining maintainable.
- **Client data/state:** a small typed `fetch` API client and Vue composables own server and UI state. Use Pinia only if a measured cross-route state need emerges; it is not a v1 default. The frontend does not query Supabase data or Storage directly.
- **Backend:** a thin Go JSON REST API, using `chi` and `pgx`, is the sole application integration with Supabase Postgres and Supabase Storage’s REST API. It verifies locally issued JWTs, owns authorization, executes per-user data access, and handles image uploads.
- **Authentication:** one local `public.admins` account is the v1 identity provider. Go verifies the bcrypt password on login and issues a short-lived HMAC-SHA256 JWT; the frontend sends it as a Bearer token to the Go API. The frontend never uses Supabase Auth, database, or Storage APIs.
- **Hosting:** deploy the Vite static frontend to Vercel or Netlify and the small Go API as a separate container/service (for example Fly.io or Render), preferably near the Supabase project. Configure frontend public values and backend secrets separately in the host environment, never in source control.

## Out of Scope (V1)

- Teams, sharing, collaboration, roles, admin tooling, comments, activity feeds, and audit logs.
- Real-time multi-user editing or presence.
- Push/email notifications, reminders, recurring tasks, dependencies, approvals, and calendar synchronization.
- A native iOS/Android app.
- Bank sync, Plaid/open-banking integrations, account sharing, financial advice, tax calculations, investments, debt planning, and automatic foreign-exchange conversion.
- Offline-first synchronization, multi-device conflict resolution, import/export pipelines, and a trash/recovery system.
- Folders, nested notes, note version history, attachments other than images, and a full WYSIWYG document editor.

## Success Criteria

V1 is successful when one authenticated user can, on desktop and mobile:

- Create, edit, filter, search, complete/reopen, and delete tasks with due dates, priorities, projects, and tags.
- Create, edit, tag, search, preview, and delete Markdown notes; upload, embed, view, and remove private note images.
- Link a note and a task from either item’s detail view.
- Log an income, expense, or transfer and see derived account balances and dashboard totals update in under 1 second at the 95th percentile on a normal broadband connection, excluding an explicit receipt upload.
- Create budgets and accurately see spent, remaining, and over-budget status for the selected period.
- Filter/search a bounded transaction list by date, category, account, type, amount, and description without another user accessing financial rows or receipts.
- Confirm or skip a due recurring transaction without duplicate or silently generated financial entries.
- Search across both modules and reach the selected item quickly.
- Use the app without another account being able to access its rows or images, as verified by RLS and Storage-policy tests.
- Meet the stated initial-load and interaction performance targets with no critical console errors or mobile layout breakage.
