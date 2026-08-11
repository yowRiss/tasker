#!/usr/bin/env bash
set -e

# ==============================================================================
# Unified Tasker Build & Environment Setup Script
# ==============================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
MOBILE_DIR="$ROOT_DIR/mobile"

echo "========================================================"
echo "          Tasker Application Build Manager              "
echo "========================================================"

TARGET="${1:-all}"

# Ensure environment files exist
if [ ! -f "$BACKEND_DIR/.env" ]; then
  echo "--> Copying backend/.env.example to backend/.env..."
  cp "$BACKEND_DIR/.env.example" "$BACKEND_DIR/.env"
fi

if [ ! -f "$FRONTEND_DIR/.env.local" ]; then
  echo "--> Copying frontend/.env.example to frontend/.env.local..."
  cp "$FRONTEND_DIR/.env.example" "$FRONTEND_DIR/.env.local"
fi

if [ ! -f "$MOBILE_DIR/.env" ]; then
  echo "--> Copying mobile/.env.example to mobile/.env..."
  cp "$MOBILE_DIR/.env.example" "$MOBILE_DIR/.env"
fi

case "$TARGET" in
  web)
    echo "--> Building Frontend & Single-Binary Go API..."
    npm --prefix "$FRONTEND_DIR" run build
    mkdir -p "$ROOT_DIR/dist"
    go -C "$BACKEND_DIR" build -o "$ROOT_DIR/dist/tasker" ./cmd/api
    echo "✓ Web executable created at dist/tasker"
    ;;
  mobile)
    echo "--> Building Mobile Android APK..."
    bash "$ROOT_DIR/build-mobile.sh"
    ;;
  all)
    echo "--> Building Web Single Binary & Mobile APK..."
    npm --prefix "$FRONTEND_DIR" run build
    mkdir -p "$ROOT_DIR/dist"
    go -C "$BACKEND_DIR" build -o "$ROOT_DIR/dist/tasker" ./cmd/api
    echo "✓ Web executable created at dist/tasker"
    bash "$ROOT_DIR/build-mobile.sh" --api-url="http://10.0.2.2:8080"
    ;;
  *)
    echo "Usage: ./build.sh [web|mobile|all]"
    exit 1
    ;;
esac
