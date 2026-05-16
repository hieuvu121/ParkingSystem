#!/usr/bin/env zsh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$ROOT_DIR/src/main/resources/db/manual/2026-05-15-seed-predictions.sql"

: "${PGDB_USERNAME:?PGDB_USERNAME is required}"
: "${PGDB_PASSWORD:?PGDB_PASSWORD is required}"

DB_NAME="${1:-parkingSystem}"

PGPASSWORD="$PGDB_PASSWORD" psql -U "$PGDB_USERNAME" -d "$DB_NAME" -f "$SQL_FILE"

