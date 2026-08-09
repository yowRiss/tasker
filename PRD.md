# Personal Tasks + Notes — Product Requirements Document

## Overview

Personal Tasks + Notes is a fast, lightweight web app for one person to manage actionable work and reference material in one focused workspace. It combines a simple task manager with Markdown-based notes, including embedded images, so daily planning and personal knowledge stay connected without the overhead of a team productivity suite.

## Goals & Non-Goals

### Goals

- Provide one calm, responsive application shell for Tasks and Notes.
- Make common actions—capture a task, complete it, write a note, and find either—feel immediate.
- Keep personal data private through Supabase Auth, Postgres Row Level Security (RLS), and private image storage.
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

- Every application record belongs to exactly one authenticated Supabase user.
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

### Search tasks and notes

1. The user opens global search and enters a query.
2. After a short debounce, the app returns ranked task and note matches with type, title, and contextual metadata/excerpts.
3. The user optionally scopes results to one module, selects a result, and opens it.

## Data Model (High Level)

- **Supabase Auth users** are the sole identity source. All app-owned rows carry a `user_id` that references `auth.users.id`.
- **tasks** hold an individual task’s title, optional description and due date, priority, completion state/timestamp, optional project, and timestamps.
- **projects** are optional task groupings owned by a user. A project has a name, optional color, archive state, and timestamps. One project can have many tasks; a task belongs to zero or one project.
- **tags** are reusable, user-owned labels shared by Tasks and Notes. **task_tags** and **note_tags** are many-to-many junction tables.
- **notes** hold a title, canonical Markdown content, and timestamps. A note can have many tags and can be linked to many tasks.
- **note_task_links** is a many-to-many junction table for the optional task/note connection. It carries ownership itself to make RLS simple and explicit.
- **note_images** stores image metadata and a private Supabase Storage object path for a particular note. The Markdown body stores a stable image ID reference, not a Storage URL. A note may have many images; deleting a note deletes its metadata and triggers removal of its Storage objects through the application cleanup flow.

## Tech Stack

- **Frontend:** Vue 3 with the Composition API, Vite, TypeScript (strict mode), and Vue Router. Vue’s small, approachable component model and Vite’s fast builds suit a responsive single-page workspace without a meta-framework.
- **Styling:** plain CSS organized by component, plus CSS custom properties for tokens (color, spacing, typography, elevation). This avoids a utility-CSS runtime or component-library dependency while remaining maintainable.
- **Client data/state:** a small typed `fetch` API client and Vue composables own server and UI state. Use Pinia only if a measured cross-route state need emerges; it is not a v1 default. The frontend does not query Supabase data or Storage directly.
- **Backend:** a thin Go JSON REST API, using `chi` and `pgx`, is the sole application integration with Supabase Postgres and Supabase Storage’s REST API. It verifies Supabase Auth JWTs, owns authorization, executes per-user data access, and handles note-image uploads.
- **Authentication:** Supabase Auth remains the identity provider. The frontend may use `@supabase/supabase-js` only for Auth sign-in, session refresh, and obtaining the access token; it sends that JWT as a Bearer token to the Go API and never uses the SDK for database or Storage calls.
- **Hosting:** deploy the Vite static frontend to Vercel or Netlify and the small Go API as a separate container/service (for example Fly.io or Render), preferably near the Supabase project. Configure frontend public values and backend secrets separately in the host environment, never in source control.

## Out of Scope (V1)

- Teams, sharing, collaboration, roles, admin tooling, comments, activity feeds, and audit logs.
- Real-time multi-user editing or presence.
- Push/email notifications, reminders, recurring tasks, dependencies, approvals, and calendar synchronization.
- A native iOS/Android app.
- Offline-first synchronization, multi-device conflict resolution, import/export pipelines, and a trash/recovery system.
- Folders, nested notes, note version history, attachments other than images, and a full WYSIWYG document editor.

## Success Criteria

V1 is successful when one authenticated user can, on desktop and mobile:

- Create, edit, filter, search, complete/reopen, and delete tasks with due dates, priorities, projects, and tags.
- Create, edit, tag, search, preview, and delete Markdown notes; upload, embed, view, and remove private note images.
- Link a note and a task from either item’s detail view.
- Search across both modules and reach the selected item quickly.
- Use the app without another account being able to access its rows or images, as verified by RLS and Storage-policy tests.
- Meet the stated initial-load and interaction performance targets with no critical console errors or mobile layout breakage.
