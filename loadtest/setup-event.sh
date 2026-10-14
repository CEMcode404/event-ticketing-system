#!/bin/bash
set -euo pipefail

: "${BASE_URL:?set BASE_URL, e.g. http://10.0.0.12:8080}"
: "${ADMIN_TOKEN:?set ADMIN_TOKEN to an Auth0 access token for the API}"

NAME="${NAME:-Load test $(date -u +%H:%M:%S)}"
SECTION="${SECTION:-VIP}"
ROWS="${ROWS:-10}"
SEATS_PER_ROW="${SEATS_PER_ROW:-20}"
PRICE_CENTS="${PRICE_CENTS:-5000}"
SALE_DELAY_SECONDS="${SALE_DELAY_SECONDS:-45}"

SALE_OPENS_AT=$(date -u -d "+${SALE_DELAY_SECONDS} seconds" +%Y-%m-%dT%H:%M:%SZ)
STARTS_AT=$(date -u -d "+1 day" +%Y-%m-%dT%H:%M:%SZ)

api() {
  local method=$1 path=$2 body=${3:-}
  local args=(-fsS -X "$method" "$BASE_URL$path"
    -H "Authorization: Bearer $ADMIN_TOKEN"
    -H "Content-Type: application/json")
  if [[ -n $body ]]; then
    args+=(-d "$body")
  fi
  curl "${args[@]}"
}

EVENT_ID=$(api POST /api/admin/events "$(jq -n \
  --arg name "$NAME" \
  --arg saleOpensAt "$SALE_OPENS_AT" \
  --arg startsAt "$STARTS_AT" \
  '{name: $name, venue: "Load Test Arena", description: "k6", saleOpensAt: $saleOpensAt, startsAt: $startsAt}')" \
  | jq -r .id)

api PATCH "/api/admin/events/$EVENT_ID/status" '{"status":"PUBLISHED"}' > /dev/null

api POST /api/admin/seats/bulk "$(jq -n \
  --arg eventId "$EVENT_ID" \
  --arg section "$SECTION" \
  --argjson rows "$ROWS" \
  --argjson seats "$SEATS_PER_ROW" \
  --argjson price "$PRICE_CENTS" \
  '{eventId: $eventId, section: $section, rowCount: $rows, seatsPerRow: $seats, priceCents: $price}')" > /dev/null

echo "Created $EVENT_ID: $((ROWS * SEATS_PER_ROW)) seats in section $SECTION, sale opens $SALE_OPENS_AT" >&2
sleep $((SALE_DELAY_SECONDS + 5))
echo "$EVENT_ID"