#!/usr/bin/env bash
set -e

CONTAINER="${CONTAINER:-local_postgres}"
DB_USER="${DB_USER:-admin}"
DATABASE="${DATABASE:-media_app}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SQL_FILE:-$SCRIPT_DIR/seed.sql}"

echo "Seeding database '$DATABASE' in container '$CONTAINER' using '$SQL_FILE'..."

if [ ! -f "$SQL_FILE" ]; then
    echo "Seed SQL file not found at $SQL_FILE" >&2
    exit 1
fi

docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DATABASE" < "$SQL_FILE"

echo "Database seed completed successfully."
