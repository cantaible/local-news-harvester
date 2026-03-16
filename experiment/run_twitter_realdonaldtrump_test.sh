#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXPERIMENT_DIR="$ROOT_DIR/experiment"
BACKUP_DIR="$EXPERIMENT_DIR/backups"
REPORT_PATH="$EXPERIMENT_DIR/README.md"

API_BASE_URL="${API_BASE_URL:-http://localhost:9090}"
DB_CONTAINER="${DB_CONTAINER:-local-news-harvester-database-1}"
DB_NAME="${DB_NAME:-news_reader}"
DB_USER="${DB_USER:-reader}"
DB_PASSWORD="${DB_PASSWORD:-readerpass}"

FEED_NAME="${FEED_NAME:-X @realDonaldTrump}"
TWITTER_URL="${TWITTER_URL:-https://x.com/realDonaldTrump}"
SOURCE_TYPE="TWITTER"
CATEGORY="${CATEGORY:-UNCATEGORIZED}"

mkdir -p "$BACKUP_DIR"

timestamp="$(date -u +"%Y%m%dT%H%M%SZ")"
backup_path="$BACKUP_DIR/news_reader_before_twitter_test_${timestamp}.sql.gz"

echo "[1/5] Backing up database to $backup_path"
docker exec "$DB_CONTAINER" mariadb-dump \
  -u"$DB_USER" \
  -p"$DB_PASSWORD" \
  --single-transaction \
  --quick \
  "$DB_NAME" | gzip > "$backup_path"

echo "[2/5] Creating or reusing Twitter feed for realDonaldTrump"
create_response="$(
  curl -sS \
    -X POST "$API_BASE_URL/feeds/new" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "name=$FEED_NAME" \
    --data-urlencode "url=$TWITTER_URL" \
    --data-urlencode "sourceType=$SOURCE_TYPE" \
    --data-urlencode "enabled=true" \
    --data-urlencode "category=$CATEGORY" \
    -w $'\n%{http_code}'
)"

create_body="$(printf '%s' "$create_response" | sed '$d')"
create_status="$(printf '%s' "$create_response" | tail -n1)"

if [[ "$create_status" != "200" && "$create_status" != "201" && "$create_status" != "409" ]]; then
  echo "Feed creation failed with HTTP $create_status"
  printf '%s\n' "$create_body"
  exit 1
fi

echo "[3/5] Triggering refresh"
refresh_response="$(
  curl -sS \
    "$API_BASE_URL/api/newsarticles/refresh" \
    -w $'\n%{http_code}'
)"

refresh_body="$(printf '%s' "$refresh_response" | sed '$d')"
refresh_status="$(printf '%s' "$refresh_response" | tail -n1)"

if [[ "$refresh_status" != "200" ]]; then
  echo "Refresh failed with HTTP $refresh_status"
  printf '%s\n' "$refresh_body"
  exit 1
fi

echo "[4/5] Querying Twitter articles from database"
count_sql="$(cat <<'SQL'
SELECT COUNT(*)
FROM news_article
WHERE source_name = 'X @realDonaldTrump'
   OR sourceurl LIKE 'https://x.com/realDonaldTrump/status/%'
   OR sourceurl LIKE 'https://x.com/realdonaldtrump/status/%';
SQL
)"

query_sql="$(cat <<'SQL'
SELECT
  id,
  REPLACE(REPLACE(COALESCE(title, ''), CHAR(13), ' '), CHAR(10), ' '),
  COALESCE(sourceurl, ''),
  COALESCE(published_at, ''),
  REPLACE(REPLACE(COALESCE(summary, ''), CHAR(13), ' '), CHAR(10), ' ')
FROM news_article
WHERE source_name = 'X @realDonaldTrump'
   OR sourceurl LIKE 'https://x.com/realDonaldTrump/status/%'
   OR sourceurl LIKE 'https://x.com/realdonaldtrump/status/%'
ORDER BY id DESC;
SQL
)"

article_count="$(
  docker exec -i "$DB_CONTAINER" mariadb \
    -u"$DB_USER" \
    -p"$DB_PASSWORD" \
    -D "$DB_NAME" \
    --batch \
    --raw \
    --skip-column-names <<SQL
$count_sql
SQL
)"

article_rows="$(
  docker exec -i "$DB_CONTAINER" mariadb \
    -u"$DB_USER" \
    -p"$DB_PASSWORD" \
    -D "$DB_NAME" \
    --batch \
    --raw \
    --skip-column-names <<SQL
$query_sql
SQL
)"
article_count="$(printf '%s' "$article_count" | tr -d '[:space:]')"

echo "[5/5] Writing report to $REPORT_PATH"
{
  echo "# Twitter Fetch Experiment"
  echo
  echo "- Test time (UTC): $timestamp"
  echo "- API base URL: $API_BASE_URL"
  echo "- Feed name: $FEED_NAME"
  echo "- Twitter URL: $TWITTER_URL"
  echo "- Backup file: $(basename "$backup_path")"
  echo "- Feed creation HTTP status: $create_status"
  echo "- Refresh HTTP status: $refresh_status"
  echo "- Articles found in DB: $article_count"
  echo
  echo "## Feed creation response"
  echo
  echo '```json'
  printf '%s\n' "$create_body"
  echo '```'
  echo
  echo "## Refresh"
  echo
  echo "- \`/api/newsarticles/refresh\` HTTP status: $refresh_status"
  echo "- Note: this endpoint refreshes all enabled feeds, so its raw response may include non-Twitter articles."
  echo "- The authoritative Twitter result for this experiment is the database query in the next section."
  echo
  echo "## Articles"
  echo
  if [[ -z "$article_rows" ]]; then
    echo "_No matching Twitter articles were found in the database._"
  else
    while IFS=$'\t' read -r id title source_url published_at summary; do
      [[ -z "${id:-}" ]] && continue
      safe_title="${title//$'\r'/ }"
      safe_summary="${summary//$'\r'/ }"
      safe_summary="${safe_summary//$'\n'/ }"
      echo "### [$id] ${safe_title:-Untitled}"
      echo
      echo "- publishedAt: ${published_at:-N/A}"
      echo "- sourceURL: ${source_url:-N/A}"
      echo "- summary: ${safe_summary:-N/A}"
      echo
    done <<< "$article_rows"
  fi
} > "$REPORT_PATH"

echo "Experiment complete."
echo "Backup: $backup_path"
echo "Report: $REPORT_PATH"
