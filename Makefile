SHELL := /bin/bash

.PHONY: help install frontend-install backend-install setup-env dev build run check frontend-check backend-check backend-test test format frontend-format backend-format android-build clean

# Default target when running 'make'
help:
	@echo "========================================================================"
	@echo "                       Tasker Developer Commands                        "
	@echo "========================================================================"
	@echo "  make setup-env       - Copy .env.example templates to local .env files"
	@echo "  make install         - Install frontend and backend dependencies"
	@echo "  make dev             - Start Frontend Vite HMR and Go API concurrently"
	@echo "  make check           - Run all quality checks (lint, format, vet, test)"
	@echo "  make test            - Run all test suites"
	@echo "  make format          - Auto-format frontend (Prettier) and backend (gofmt)"
	@echo "  make build           - Build embedded production binary at dist/tasker"
	@echo "  make run             - Execute built production binary"
	@echo "  make clean           - Remove build artifacts and temporary files"
	@echo "========================================================================"

# Environment initialization
setup-env:
	@[ -f backend/.env ] || cp backend/.env.example backend/.env
	@[ -f frontend/.env.local ] || cp frontend/.env.example frontend/.env.local
	@echo "✓ Environment templates initialized. Update backend/.env with your Supabase secrets."

# Dependencies
install: frontend-install backend-install

frontend-install:
	npm --prefix frontend install

backend-install:
	go -C backend mod download

# Local Development
dev:
	@set -e; \
		npm --prefix frontend run dev & vite_pid=$$!; \
		set -a; [ -f backend/.env ] && . backend/.env; set +a; go -C backend run ./cmd/api & api_pid=$$!; \
		trap 'kill $$vite_pid $$api_pid 2>/dev/null || true' EXIT INT TERM; \
		wait -n $$vite_pid $$api_pid

# Quality Assurance & Testing
check: frontend-check backend-check backend-test
	@echo "✓ All checks passed successfully!"

test: backend-test

frontend-check:
	npm --prefix frontend run lint
	npm --prefix frontend run format:check
	npm --prefix frontend run build

backend-check:
	go -C backend fmt ./...
	go -C backend vet ./...
	@if command -v golangci-lint >/dev/null 2>&1; then \
		golangci-lint run ./backend/...; \
	fi

backend-test:
	go -C backend test -v ./...

# Formatting
format: frontend-format backend-format
	@echo "✓ Code formatted."

frontend-format:
	npm --prefix frontend run format

backend-format:
	go -C backend fmt ./...

# Production Single-Binary Build
build:
	npm --prefix frontend run build
	mkdir -p dist
	go -C backend build -o ../dist/tasker ./cmd/api
	@echo "✓ Single-binary built at dist/tasker"

run:
	./dist/tasker

# Mobile Android Build
android-build:
	cd android && ./gradlew :app:assembleRelease

# Cleanup
clean:
	rm -rf dist frontend/dist backend/bin backend/api backend/tasker release-apks
	@echo "✓ Cleaned build artifacts."
