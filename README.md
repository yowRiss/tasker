# Tasker — Tasks, Notes & Money

[![Continuous Integration](https://github.com/yowRiss/tasker/actions/workflows/ci.yml/badge.svg)](https://github.com/yowRiss/tasker/actions/workflows/ci.yml)
[![Android Release](https://github.com/yowRiss/tasker/actions/workflows/android-release.yml/badge.svg)](https://github.com/yowRiss/tasker/actions/workflows/android-release.yml)
[![Go Version](https://img.shields.io/badge/Go-1.26+-00ADD8?style=flat&logo=go)](https://go.dev/)
[![Vue 3](https://img.shields.io/badge/Vue-3.5+-4FC08D?style=flat&logo=vue.js)](https://vuejs.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat&logo=kotlin)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## Highlights

- **⚡ Fast & Modern Web App**: Built with Vue 3 (Composition API), Vite, TypeScript, and semantic CSS tokens.
- **🔒 Secure Single-Binary Go Backend**: Chi HTTP router connecting to Supabase PostgreSQL with strict Row Level Security (RLS) and transaction-isolated user claims.
- **📱 Native Android Mobile App**: 100% Native Kotlin + Jetpack Compose (Material 3) with offline-first Room SQLite storage and background mutation synchronization engine.
- **📝 Markdown Notes & LaTeX Math**: Rich notes with private image uploads, inline LaTeX `$E=mc^2$` and block `$$\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}$$` math rendering.
- **💰 Money & Cash Flow**: Income & expense tracking, custom categories, monthly budgets, recurring transactions, receipt attachments, and financial trend dashboards.
- **📦 Zero-Nginx Production Deployment**: In production, the Go binary embeds the Vue SPA directly at compile time and serves both the API and frontend on a single port.

---

## Monorepo Layout

```
├── frontend/       # Vue 3 + Vite SPA (TypeScript, ESLint, Prettier, uPlot)
├── backend/        # Go 1.26+ Chi API & Supabase Postgres / Storage client
├── android/        # Native Android app (Kotlin, Jetpack Compose, Room, WorkManager)
├── api/            # OpenAPI 3.0 contract (openapi.yaml)
├── supabase/       # SQL migrations, RLS policies, Storage buckets
├── .github/        # GitHub Actions CI/CD workflows, Issue & PR templates
└── Makefile        # Developer command orchestrator
```

---

## Prerequisites

- **Node.js**: `v22+` with `npm 10+`
- **Go**: `1.26+` (as specified in [`backend/go.mod`](backend/go.mod))
- **Supabase Project**: For Postgres database, RLS, and private Storage buckets
- **Android Studio & JDK 17+**: *(Optional)* Required only for native mobile development

---

## Quick Start

### 1. Initialize Environment Files

```bash
make setup-env
```

This creates `backend/.env` and `frontend/.env.local` from their respective `.example` templates. Fill in your Supabase connection string and API keys in `backend/.env`.

### 2. Install Dependencies & Start Local Dev

```bash
make install
make dev
```

- **Frontend Web UI**: [http://localhost:5173](http://localhost:5173)
- **Backend API**: [http://localhost:8080](http://localhost:8080)

Press `Ctrl-C` to stop both the frontend and backend servers together.

---

## Developer Commands

Tasker includes a self-documenting `Makefile` for developer workflows:

| Command | Description |
| :--- | :--- |
| `make help` | Show all available developer commands |
| `make setup-env` | Initialize `.env` configuration files from templates |
| `make install` | Install all frontend and backend dependencies |
| `make dev` | Start Frontend Vite dev server and Go API concurrently |
| `make check` | Run all quality checks (ESLint, Prettier, Go vet, tests) |
| `make test` | Run backend test suites |
| `make format` | Auto-format frontend (Prettier) and backend (`gofmt`) |
| `make build` | Compile single self-contained production binary at `dist/tasker` |
| `make run` | Execute the built production binary |
| `make clean` | Clean build artifacts and distribution directories |

---

## Contributing

We welcome contributions from everyone! To get started:

1. Read our **[Contributing Guide](CONTRIBUTING.md)** for architecture details, coding conventions, and pull request steps.
2. Review our **[Code of Conduct](CODE_OF_CONDUCT.md)** to understand community standards.
3. For bug reports or feature proposals, please use our **[GitHub Issue Templates](.github/ISSUE_TEMPLATE/)**.
4. Before submitting a PR, ensure all checks pass with `make check`.

---

## Documentation Links

- 📖 **[PRD.md](PRD.md)** — Product Requirements & Web Application Scope
- 🏗️ **[AGENT.md](AGENT.md)** — Web & Backend Technical Architecture
- 📱 **[MOBILE_PRD.md](MOBILE_PRD.md)** — Native Android Mobile Product Specifications
- 🛠️ **[MOBILE_AGENT.md](MOBILE_AGENT.md)** — Android Offline Sync Engine & Architecture
- 🤝 **[CONTRIBUTING.md](CONTRIBUTING.md)** — Contributor Onboarding & Workflow Guide
- 🔒 **[SECURITY.md](SECURITY.md)** — Security Policy & Responsible Disclosure

---

## License

This project is licensed under the [MIT License](LICENSE).
