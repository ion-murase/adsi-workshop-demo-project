#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

node "$SCRIPT_DIR/sagemaker-proxy.mjs" &
PROXY_PID=$!
trap "kill $PROXY_PID 2>/dev/null" EXIT

echo "Proxy started (PID: $PROXY_PID)"

cd "$PROJECT_DIR/packages/frontend"
NEXT_PUBLIC_BASE_PATH=/ports/3000 npx next dev --webpack -p 3001
