// Package postgres contains the Go API's pgx-backed data access adapters.
package postgres

import "github.com/jackc/pgx/v5/pgxpool"

// Pool is the connection-pool type used by future repositories.
// Per-request transaction and RLS claim setup will live in internal/platform/dbtx.
type Pool = pgxpool.Pool
