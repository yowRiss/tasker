# Tasker — Tasks, Notes & Money

[![Continuous Integration](https://github.com/yowRiss/tasker/actions/workflows/ci.yml/badge.svg)](https://github.com/yowRiss/tasker/actions/workflows/ci.yml)
[![Android Release](https://github.com/yowRiss/tasker/actions/workflows/android-release.yml/badge.svg)](https://github.com/yowRiss/tasker/actions/workflows/android-release.yml)
[![Go Version](https://img.shields.io/badge/Go-1.26+-00ADD8?style=flat&logo=go)](https://go.dev/)
[![Vue 3](https://img.shields.io/badge/Vue-3.5+-4FC08D?style=flat&logo=vue.js)](https://vuejs.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat&logo=kotlin)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Tasker is a self-hosted personal productivity workspace for organizing tasks, writing connected notes, and managing day-to-day finances. It includes a responsive Vue web app, a Go API, and a native Android app with offline-first synchronization.

**Search tags:** `task-manager` · `notes-app` · `personal-finance` · `productivity` · `self-hosted` · `vue` · `golang` · `supabase` · `android` · `jetpack-compose` · `offline-first`

## Features

### Task management

- Create tasks with a description, due date, priority, project, and one or more color-coded tags.
- Break work into ordered subtasks and complete each item independently.
- Group tasks into projects, then filter the task list by project, tag, priority, due-date state, or status.
- Search task titles from the task list or use workspace-wide search to find tasks and notes together.
- Mark tasks complete, automatically move them to the archive, undo a recent completion, or reopen archived work.

### Notes and knowledge

- Write and preview Markdown notes with inline and block LaTeX math.
- Upload private image attachments, edit their metadata, and open them in a zoomable lightbox.
- Link notes to related tasks so reference material and actionable work stay connected.
- Add scheduled reminders with selectable notification offsets.
- Search note titles and content alongside tasks from a single search screen.

### Money management

- Maintain multiple accounts and track exact balances without floating-point rounding at the API boundary.
- Record income, expenses, and transfers with dates, categories, descriptions, and optional receipt attachments.
- Filter transaction history and organize spending with custom, archivable categories.
- Set category budgets for a date range and monitor spent, remaining, percentage-used, and over-budget status.
- Create recurring income or expense templates, review due items, and confirm or skip each occurrence.
- Define savings targets, optionally associate them with an account or category, contribute toward them, and track progress.
- Review total balance, income, expenses, spending by category, savings-target summaries, and cash-flow trends on the dashboard.

### Search, archive, and personalization

- Search across tasks and notes, or narrow results to a single content type.
- Use tags and projects to classify tasks, then browse and filter the workspace without changing the underlying data.
- Keep completed tasks out of the active view while retaining them in a dedicated archive.
- Switch between light and dark themes from the web interface.

### Native Android app

- Use native Material 3 screens for tasks, notes, calendar items, transactions, budgets, recurring transactions, savings targets, and settings.
- Store task, note, and money data locally with Room so core screens remain available offline.
- Queue local mutations in an outbox and synchronize them in the background with WorkManager when connectivity returns.
- See offline and synchronization status in the app, with ID remapping for records first created locally.
- Receive local note-reminder notifications and view attached images in a zoomable dialog.
- Check for and install published Android updates through the in-app update flow.

### Platform and security

- Authenticate users with signed tokens and isolate each user's data through PostgreSQL Row Level Security policies.
- Protect authentication endpoints with rate limiting and API responses with request IDs, recovery middleware, CORS controls, and security headers.
- Store note images and transaction receipts in private Supabase Storage buckets and expose them through time-limited access URLs.
- Use a versioned REST API documented by the OpenAPI contract in [`api/openapi.yaml`](api/openapi.yaml).
- Build the Vue SPA into the Go server for a single production binary that serves both the frontend and API without Nginx.

## How It Fits Together

```text
Vue 3 web app ─┐
               ├── Go REST API ── Supabase PostgreSQL + private Storage
Android app ───┘         │
                         └── embedded Vue production assets
```

The web client talks to the API directly during development. In production, the compiled Go binary serves the API and embedded SPA from one origin. The Android client uses the same API while Room and the synchronization outbox provide its offline workflow.

## Technology Stack

| Area           | Technology                                                   |
| -------------- | ------------------------------------------------------------ |
| Web            | Vue 3, TypeScript, Vue Router, Vite, uPlot                   |
| API            | Go, Chi, PostgreSQL driver, JWT authentication               |
| Data and files | Supabase PostgreSQL, Row Level Security, Supabase Storage    |
| Android        | Kotlin, Jetpack Compose, Material 3, Room, WorkManager, Hilt |
| API contract   | OpenAPI 3.0                                                  |
| Automation     | Make, GitHub Actions                                         |

## Monorepo Layout

```text
├── frontend/       # Vue 3 and Vite web application
├── backend/        # Go REST API and embedded web server
├── android/        # Native offline-first Android application
├── api/            # OpenAPI contract
├── supabase/       # Database migrations, RLS policies, and seed data
├── .github/        # CI/CD workflows and contribution templates
└── Makefile        # Common development commands
```

## Prerequisites

- **Node.js 22+** with **npm 10+**
- **Go 1.26+**, matching [`backend/go.mod`](backend/go.mod)
- A **Supabase project** for PostgreSQL and private file storage
- **Android Studio and JDK 17+** only when developing the Android app

## Quick Start

### 1. Create local environment files

```bash
make setup-env
```

This copies the example configuration to `backend/.env` and `frontend/.env.local` without overwriting existing files.

Configure these backend values before starting the app:

| Variable              | Purpose                                                 |
| --------------------- | ------------------------------------------------------- |
| `DATABASE_URL`        | PostgreSQL connection string used by the API            |
| `SUPABASE_URL`        | Base URL of the Supabase project                        |
| `SUPABASE_SECRET_KEY` | Server-only key used for private Storage operations     |
| `JWT_SECRET`          | Secret used to sign and verify application tokens       |
| `CORS_ALLOWED_ORIGIN` | Frontend origin allowed during split-origin development |
| `API_ADDR`            | API listen address; defaults to `:8080` in the example  |

`VITE_API_BASE_URL` is public frontend configuration. Leave it empty for the default same-origin setup and Vite development proxy.

> Never commit populated `.env` files or expose `SUPABASE_SECRET_KEY` and `JWT_SECRET` to the frontend.

### 2. Install dependencies

```bash
make install
```

### 3. Start web development

```bash
make dev
```

- Web app: [http://localhost:5173](http://localhost:5173)
- API: [http://localhost:8080](http://localhost:8080)
- Health check: [http://localhost:8080/healthz](http://localhost:8080/healthz)

Press `Ctrl-C` to stop both development processes.

## Production Build

```bash
make build
make run
```

The build creates `dist/tasker`, a single executable containing the Go API and compiled Vue application. Runtime database, Supabase, and JWT environment variables are still required.

## Android Development

Open the `android/` directory in Android Studio and configure the API host from the app's settings. To create a release APK through the repository command:

```bash
make android-build
```

Android build and release verification details are documented in [`android/BUILD_VERIFICATION.md`](android/BUILD_VERIFICATION.md).

## Developer Commands

| Command              | Description                                                                        |
| -------------------- | ---------------------------------------------------------------------------------- |
| `make help`          | List the common development commands                                               |
| `make setup-env`     | Create local environment files from the example templates                          |
| `make install`       | Install frontend packages and download Go modules                                  |
| `make dev`           | Run the Vite development server and Go API together                                |
| `make check`         | Run frontend lint, formatting and build checks, plus Go formatting, vet, and tests |
| `make test`          | Run the Go backend test suite                                                      |
| `make format`        | Format frontend and backend source files                                           |
| `make build`         | Create the embedded production binary at `dist/tasker`                             |
| `make run`           | Run the production binary                                                          |
| `make android-build` | Assemble the Android release APK                                                   |
| `make clean`         | Remove generated build artifacts                                                   |

## API

The API is available under `/v1`; `/api` and `/api/v1` requests are normalized to the same versioned routes. Most endpoints require a bearer token. The main resources are:

- Authentication and current user
- Projects, tags, tasks, and subtasks
- Notes, task links, note images, and workspace search
- Accounts, categories, transactions, receipts, budgets, recurring transactions, savings targets, and dashboard reports

See [`api/openapi.yaml`](api/openapi.yaml) for the machine-readable API contract.

## Contributing

1. Read the [Contributing Guide](CONTRIBUTING.md) for architecture, conventions, and pull request steps.
2. Follow the [Code of Conduct](CODE_OF_CONDUCT.md).
3. Use the [issue templates](.github/ISSUE_TEMPLATE/) for bug reports and feature proposals.
4. Run `make check` before opening a pull request.

## Documentation

- [Product requirements](PRD.md)
- [Web and backend architecture](AGENT.md)
- [Android product requirements](MOBILE_PRD.md)
- [Android architecture and offline synchronization](MOBILE_AGENT.md)
- [Design system](DESIGN.md)
- [Security policy](SECURITY.md)
- [Contributing guide](CONTRIBUTING.md)

## License

Tasker is available under the [MIT License](LICENSE).
