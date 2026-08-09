package dbtx

import (
	"context"
	"fmt"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Within starts an RLS-safe transaction. The claim is LOCAL and cannot leak through the pool.
func Within(ctx context.Context, pool *pgxpool.Pool, userID string, fn func(pgx.Tx) error) error {
	tx, err := pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin transaction: %w", err)
	}
	defer func() { _ = tx.Rollback(ctx) }()
	if _, err = tx.Exec(ctx, "select set_config('request.jwt.claim.sub', $1, true)", userID); err != nil {
		return fmt.Errorf("set RLS claim: %w", err)
	}
	if err = fn(tx); err != nil {
		return err
	}
	return tx.Commit(ctx)
}
