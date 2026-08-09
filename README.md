# Personal Tasks + Notes

A personal productivity workspace for tasks and Markdown notes with private image embeds. Product scope and engineering decisions are documented in [PRD.md](PRD.md) and [AGENT.md](AGENT.md).

## Repository layout

- `frontend/` — Vue 3 + Vite application. It communicates only with the Go API, except for Supabase Auth session handling.
- `backend/` — Go + chi API. It owns Supabase Postgres and Storage access.
- `api/` — OpenAPI contract shared by the frontend and backend.
- `supabase/` — database, RLS, and Storage migrations.

## Prerequisites

- Node.js 22+ and npm 10+
- Go version specified by [`backend/go.mod`](backend/go.mod) (currently 1.26.4)
- A Supabase project when configuring real Auth, database, and Storage access

## Local setup

1. Copy the example environment files and fill in values from your Supabase project. Do not commit the resulting files.

   ```bash
   cp frontend/.env.example frontend/.env.local
   cp backend/.env.example backend/.env
   ```

2. Install frontend dependencies, then start the Vite dev server and Go API together.

   ```bash
   npm --prefix frontend install
   make dev
   ```

`make dev` stops both processes when either one exits or when you press Ctrl-C. The frontend defaults to `http://localhost:5173`; the API defaults to `http://localhost:8080`.

## Production build and deployment

Build one self-contained Go executable with the Vue bundle embedded inside it:

```bash
make build
make run
```

`make build` writes `dist/tasker`; copy that binary and its backend environment variables to the VPS, then run `./tasker`. The Go binary does not load `.env` files itself, so export the variables or configure them with your service manager's environment-file mechanism. The binary serves both the Vue SPA and the existing `/v1/*` API on one port, including SPA fallback for Vue Router routes. Node, Vite, and Nginx are not required in production. See [AGENT.md](AGENT.md#deployment) for the development/prod split and environment details.

## Supabase migrations

The schema, RLS policies, and private `note-images` Storage bucket are managed by the timestamped SQL files in `supabase/migrations/`.

Link the repository to the target Supabase project, then apply them with the Supabase CLI:

```bash
supabase link --project-ref your-project-ref
supabase db push
```

For local Supabase development, `supabase start` followed by `supabase db reset` applies the migrations from a clean database. The bucket remains private and accepts only JPEG, PNG, WebP, and GIF objects up to 10 MiB. The API must generate object names as `notes/<user_id>/<note_id>/<uuid>-<sanitized-stem>.<ext>` and set the verified user ID in its transaction-local JWT claim before querying application tables.

## Checks

```bash
npm --prefix frontend run lint
npm --prefix frontend run format:check
npm --prefix frontend run build
go -C backend fmt ./...
go -C backend vet ./...
golangci-lint run ./backend/...
```

`golangci-lint` uses the root [`.golangci.yml`](.golangci.yml) configuration. Install it separately following its official installation instructions.
