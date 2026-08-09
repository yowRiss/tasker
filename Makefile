SHELL := /bin/bash

.PHONY: frontend-install frontend-check backend-check check dev build run

frontend-install:
	npm --prefix frontend install

frontend-check:
	npm --prefix frontend run lint
	npm --prefix frontend run format:check
	npm --prefix frontend run build

backend-check:
	go -C backend fmt ./...
	go -C backend vet ./...
	golangci-lint run ./backend/...

check: frontend-check backend-check

# Starts Vite with HMR and the Go API together. Ctrl-C stops both processes.
dev:
	@set -e; \
		npm --prefix frontend run dev & vite_pid=$$!; \
		set -a; . backend/.env; set +a; go -C backend run ./cmd/api & api_pid=$$!; \
		trap 'kill $$vite_pid $$api_pid 2>/dev/null || true' EXIT INT TERM; \
		wait -n $$vite_pid $$api_pid

# Produces one self-contained binary: Vite assets are embedded by Go at compile time.
build:
	npm --prefix frontend run build
	mkdir -p dist
	go -C backend build -o ../dist/tasker ./cmd/api

run:
	./dist/tasker
