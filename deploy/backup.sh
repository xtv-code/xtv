#!/bin/sh
# Snapshot the database and public media. Run from the repo root or any cwd.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="${XTV_BACKUP_DIR:-$ROOT/data/backups}/$STAMP"
mkdir -p "$OUT"

if [ -n "${SPRING_DATASOURCE_URL:-}" ] && command -v mysqldump >/dev/null 2>&1; then
  DB="${SPRING_DATASOURCE_USERNAME:-xtv}"
  PASS="${SPRING_DATASOURCE_PASSWORD:-}"
  HOST="$(printf '%s' "$SPRING_DATASOURCE_URL" | sed -n 's#.*://\([^:/]*\).*#\1#p')"
  NAME="$(printf '%s' "$SPRING_DATASOURCE_URL" | sed -n 's#.*/\([^?]*\).*#\1#p')"
  mysqldump -h "${HOST:-127.0.0.1}" -u "$DB" ${PASS:+-p"$PASS"} --single-transaction --routines "$NAME" > "$OUT/xtv.sql"
else
  if [ -f "$ROOT/data/xtv.mv.db" ]; then
    cp "$ROOT/data/xtv.mv.db" "$OUT/xtv.mv.db"
  fi
fi

MEDIA="${XTV_STORAGE_ROOT:-$ROOT/data/media}"
if [ -d "$MEDIA" ]; then
  mkdir -p "$OUT/media"
  cp -R "$MEDIA/." "$OUT/media/"
fi

echo "$OUT"
