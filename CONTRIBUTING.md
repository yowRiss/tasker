# Contributing to Tasker

Thank you for your interest in contributing to **Tasker**! We welcome contributions from developers of all skill levels. Whether you are fixing a bug, adding new features, improving documentation, or optimizing performance, this guide will help you get started quickly and smoothly.

---

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Monorepo Architecture](#monorepo-architecture)
3. [Prerequisites & Development Environment](#prerequisites--development-environment)
4. [Quick Start & Local Setup](#quick-start--local-setup)
5. [Development Workflows](#development-workflows)
   - [Web Frontend (Vue 3 + Vite)](#web-frontend-vue-3--vite)
   - [Backend API (Go + Chi)](#backend-api-go--chi)
   - [Native Android Mobile (Kotlin + Jetpack Compose)](#native-android-mobile-kotlin--jetpack-compose)
   - [Database & Supabase Migrations](#database--supabase-migrations)
   - [API Contracts (OpenAPI)](#api-contracts-openapi)
6. [Code Style & Standards](#code-style--standards)
7. [Running Tests & Quality Checks](#running-tests--quality-checks)
8. [Git Conventions & Pull Request Process](#git-conventions--pull-request-process)
9. [Reporting Bugs & Requesting Features](#reporting-bugs--requesting-features)
10. [CI/CD & Builds](#cicd--builds)

---

## Code of Conduct

All contributors and maintainers are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please treat everyone with respect, kindness, and empathy.

---

## Monorepo Architecture

Tasker is organized as a modular repository with distinct layers:

| Directory | Stack | Description |
| :--- | :--- | :--- |
| `frontend/` | Vue 3 (Composition API), Vite, TypeScript | Web Single Page Application (SPA), stores local-admin JWT sessions, communicates exclusively with `/v1/*` Go API. |
| `backend/` | Go 1.26+, Chi router, `pgx/v5`, JWT | API server connecting to Supabase Postgres (with RLS) and Supabase Storage for private note image uploads. In production, embeds frontend assets into a single binary. |
| `android/` | Kotlin, Jetpack Compose, Room, WorkManager | Native Android mobile application featuring offline-first local cache and bi-directional sync engine. |
| `api/` | OpenAPI 3.0 (`openapi.yaml`) | Single source of truth for the HTTP API contract shared between frontend, mobile, and backend. |
| `supabase/` | SQL migrations, RLS policies | Database migrations, Row Level Security policies, and Storage bucket configurations. |

Detailed specifications and architectural decisions are documented in:
- [PRD.md](PRD.md) — Product Requirements & Web Application Scope
- [AGENT.md](AGENT.md) — Web & Backend Engineering Architecture
- [MOBILE_PRD.md](MOBILE_PRD.md) — Native Mobile Product Specifications
- [MOBILE_AGENT.md](MOBILE_AGENT.md) — Android Architecture & Offline Sync Design

---

## Prerequisites & Development Environment

To work on all parts of Tasker, ensure you have the following tools installed:

- **Node.js**: `v22+` with `npm 10+`
- **Go**: `1.26+` (as declared in `backend/go.mod`)
- **Supabase CLI**: (optional, for local database development or schema migration)
- **Android Studio / JDK 17+**: (optional, only required if working on the native Android application)
- **Make & Bash**: Standard POSIX shell utilities for Makefile targets

---

## Quick Start & Local Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yowRiss/tasker.git
   cd tasker
   ```

2. **Initialize environment variables**:
   ```bash
   make setup-env
   ```
   This generates `backend/.env` and `frontend/.env.local` from their respective `.example` files.

3. **Configure your Supabase credentials**:
   Open `backend/.env` and supply your Supabase Postgres connection string, Supabase URL, Service Role Key, and JWT secrets:
   ```env
   DATABASE_URL=postgres://...
   SUPABASE_URL=https://<your-project>.supabase.co
   SUPABASE_SERVICE_ROLE_KEY=<service-role-key>
   SUPABASE_JWT_SECRET=<jwt-secret>
   ADMIN_PASSWORD=<local-admin-password>
   ```

4. **Install dependencies and start development server**:
   ```bash
   make install
   make dev
   ```
   - **Frontend**: http://localhost:5173
   - **Backend API**: http://localhost:8080

Press `Ctrl-C` to stop both the frontend and backend development processes gracefully.

---

## Development Workflows

### Web Frontend (Vue 3 + Vite)

- Navigate to `frontend/`:
  ```bash
  cd frontend
  npm install
  npm run dev
  ```
- Run linter and formatting checks:
  ```bash
  npm run lint
  npm run format:check
  ```
- To auto-format frontend code:
  ```bash
  npm run format
  ```

### Backend API (Go + Chi)

- Code is organized in `backend/internal/`:
  - `cmd/api/` — Application entrypoint and embedded asset handling.
  - `internal/httpapi/` — Chi routing, HTTP request handlers, and middleware.
  - `internal/service/` — Core business logic.
  - `internal/repository/` — Database access with `pgx/v5` and RLS claim injection.
  - `internal/storage/` — Supabase Storage client for private note image uploads.
  - `internal/auth/` — JWT authentication and admin verification.
- Run tests and static analysis:
  ```bash
  make backend-check
  make backend-test
  ```

### Native Android Mobile (Kotlin + Jetpack Compose)

- Open the `android/` directory in **Android Studio**.
- Ensure **JDK 17** is configured in Android Studio Project Settings.
- Run the app on an Android Virtual Device (AVD) or physical device via Android Studio.
- Note: Production Android APK/AAB builds are automatically performed in GitHub Actions CI.

### Database & Supabase Migrations

- All database schema changes, triggers, and Row Level Security (RLS) policies live in `supabase/migrations/*.sql`.
- When adding a new table or modifying columns:
  1. Add a new timestamped migration file in `supabase/migrations/` (e.g. `YYYYMMDDHHMMSS_feature_name.sql`).
  2. Ensure RLS policies are enabled on all tables (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY;`).
  3. Ensure policies verify `auth.uid() = user_id` or the appropriate transaction-local claims.
  4. Test locally using `supabase db reset` if using the local Supabase stack.

### API Contracts (OpenAPI)

- The OpenAPI 3.0 specification is maintained in `api/openapi.yaml`.
- Whenever you add, change, or deprecate an HTTP endpoint or payload schema:
  1. Update `api/openapi.yaml`.
  2. Ensure both the Go HTTP handlers and frontend API clients (`frontend/src/features/**/api.ts`) adhere strictly to the specification.

---

## Code Style & Standards

### General Principles
- **Clarity over cleverness**: Keep code maintainable, readable, and well-structured.
- **Security by default**: Always validate user inputs. Never bypass RLS or expose sensitive keys in frontend code.
- **Maintain documentation integrity**: Update relevant documentation (`README.md`, `PRD.md`, `AGENT.md`, OpenAPI spec) alongside feature additions or breaking changes.

### Go
- Always format code with `gofmt` (`go fmt ./...`).
- Run `go vet ./...` to check for common mistakes.
- Follow standard Go idioms, error-handling conventions (`if err != nil`), and clean layered separation.

### TypeScript / Vue
- Use Vue 3 `<script setup lang="ts">` and the Composition API.
- Do not mutate props directly; emit events (`update:*`) or pass update callbacks.
- Enforce strict typing—avoid `any` where possible.
- Use CSS variables defined in `src/styles/tokens.css` for consistent styling.

### Kotlin / Android
- Follow official Kotlin coding conventions and Jetpack Compose state hoisting patterns.
- Keep UI components decoupled from data layer using ViewModels and repository abstractions.
- All database mutations must go through the offline-first `sync_queue` pattern.

---

## Running Tests & Quality Checks

Before submitting a Pull Request, run the full validation suite using Make:

```bash
# Run all frontend and backend checks
make check

# Run tests
make test

# Auto-format frontend code
make format
```

Individual checks:
- `make frontend-check`: Runs ESLint, Prettier check, and Vue-TSC type verification.
- `make backend-check`: Runs `go fmt`, `go vet`, and Go linter.
- `make backend-test`: Runs the Go unit test suite.

---

## Git Conventions & Pull Request Process

### 1. Branch Naming
Create a descriptive branch for your work:
- `feat/task-recurring-dates`
- `fix/image-upload-content-type`
- `docs/update-contributing-guide`
- `refactor/money-dashboard-charts`

### 2. Commit Messages (Conventional Commits)
We follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat: add budget progress visualization in money view`
- `fix: resolve prop mutation error in TransactionFilters`
- `docs: update API documentation for note attachment endpoints`
- `refactor: extract date calculation helpers into shared module`
- `test: add unit tests for token verification`
- `ci: add GitHub Actions workflow for pull request validation`

### 3. Pull Request Submission
1. Push your branch to your fork or branch on the repository.
2. Open a Pull Request targeting the `master` (or `main`) branch.
3. Fill out the **Pull Request Template**:
   - Provide a concise summary of your changes.
   - Select the components modified (Frontend, Backend, Android, DB, etc.).
   - Confirm that `make check` and tests pass locally.
4. GitHub Actions CI will automatically run linting, formatting, and build checks.
5. Address any review feedback or CI check failures promptly.

---

## Reporting Bugs & Requesting Features

### Submitting a Bug Report
- Check existing [GitHub Issues](https://github.com/yowRiss/tasker/issues) to ensure the bug hasn't already been reported.
- Open a new issue using the **Bug Report** template.
- Include clear steps to reproduce, expected vs. actual behavior, and environment details (OS, Browser, Device).

### Requesting a Feature
- Open a new issue using the **Feature Request** template.
- Explain the motivation and use case for the proposed feature.
- Outline possible implementation strategies or UI/UX considerations.

### Security Vulnerabilities
If you discover a security vulnerability, please review our [Security Policy](SECURITY.md) to report it responsibly.

---

## CI/CD & Builds

> [!NOTE]
> All full application compilation, production single-binary builds, and Android APK/AAB packaging are automatically executed in **GitHub Actions CI/CD**. You do not need to generate or commit release binaries locally.

---

Thank you for helping make **Tasker** better for everyone! 🚀
