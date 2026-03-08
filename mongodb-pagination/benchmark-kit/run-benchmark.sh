#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_CMD="docker compose"
if ! docker compose version >/dev/null 2>&1; then
  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
  else
    echo "[error] docker compose or docker-compose is required"
    exit 1
  fi
fi

DB_NAME="${DB_NAME:-benchmark}"
COLL_NAME="${COLL_NAME:-posts}"
TOTAL_DOCS="${TOTAL_DOCS:-200000}"
BATCH_SIZE="${BATCH_SIZE:-5000}"
PAGE_SIZE="${PAGE_SIZE:-500}"
PAGES="${PAGES:-0,1,10,100,300}"

echo "[run] starting mongodb container"
$COMPOSE_CMD up -d

echo "[run] waiting for mongodb readiness"
for i in {1..60}; do
  if docker exec mongo-benchmark mongosh --quiet --eval "db.adminCommand({ ping: 1 }).ok" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  if [[ "$i" -eq 60 ]]; then
    echo "[error] mongodb did not become ready in time"
    exit 1
  fi
done

echo "[run] seeding dataset"
docker exec \
  -e DB_NAME="$DB_NAME" \
  -e COLL_NAME="$COLL_NAME" \
  -e TOTAL_DOCS="$TOTAL_DOCS" \
  -e BATCH_SIZE="$BATCH_SIZE" \
  mongo-benchmark \
  mongosh --quiet /scripts/seed.js

echo "[run] running benchmark"
docker exec \
  -e DB_NAME="$DB_NAME" \
  -e COLL_NAME="$COLL_NAME" \
  -e PAGE_SIZE="$PAGE_SIZE" \
  -e PAGES="$PAGES" \
  mongo-benchmark \
  mongosh --quiet /scripts/benchmark.js

echo "[run] done"
echo "[hint] stop container: $COMPOSE_CMD down"
