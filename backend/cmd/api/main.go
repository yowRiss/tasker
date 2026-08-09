package main

import (
	"context"
	"github.com/jackc/pgx/v5/pgxpool"
	"log"
	"log/slog"
	"net/http"
	"tasker/backend/internal/auth"
	"tasker/backend/internal/config"
	"tasker/backend/internal/httpapi"
	"tasker/backend/internal/httpapi/handlers"
	"tasker/backend/internal/repository/postgres"
	"tasker/backend/internal/service"
	storage "tasker/backend/internal/storage/supabase"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatal(err)
	}
	pool, err := pgxpool.New(context.Background(), cfg.DatabaseURL)
	if err != nil {
		log.Fatal(err)
	}
	defer pool.Close()
	repo := postgres.New()
	svc := service.New(pool, repo, storage.New(cfg.SupabaseURL, cfg.ServiceRoleKey))

	ctx := context.Background()
	if err := svc.EnsureDefaultAdmin(ctx, "admin", "admin"); err != nil {
		log.Printf("Default admin check/creation warning: %v", err)
	}

	verifier := auth.NewVerifier(cfg.JWTSecret)
	router := httpapi.New(handlers.New(svc, repo, verifier), verifier, cfg.CORSAllowedOrigin, slog.Default())
	log.Printf("API listening on %s", cfg.APIAddr)
	log.Fatal(http.ListenAndServe(cfg.APIAddr, router))
}
