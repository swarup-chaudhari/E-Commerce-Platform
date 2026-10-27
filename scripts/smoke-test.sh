#!/usr/bin/env bash
# End-to-end integration test of the saga through the API gateway. Requires: curl, jq.
#   ./scripts/smoke-test.sh [gateway-url]      (default http://localhost:8080)
set -uo pipefail
GW="${1:-http://localhost:8080}"
PASS=0; FAIL=0
ok()   { echo "  PASS: $1"; PASS=$((PASS+1)); }
bad()  { echo "  FAIL: $1"; FAIL=$((FAIL+1)); }
check(){ if [ "$2" = "$3" ]; then ok "$1 ($3)"; else bad "$1 (expected '$3', got '$2')"; fi; }
login(){ curl -s -X POST "$GW/auth/login" -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jq -r .accessToken; }
wait_status(){ # token orderId expectedStatus
  for _ in $(seq 1 30); do
    s=$(curl -s "$GW/api/orders/$2" -H "Authorization: Bearer $1" | jq -r .status)
    [ "$s" = "$3" ] && { echo "$s"; return; }; sleep 1
  done; echo "$s"
}
order(){ # token productId qty unitPrice
  curl -s -X POST "$GW/api/orders" -H "Authorization: Bearer $1" -H 'Content-Type: application/json' \
    -d "{\"items\":[{\"productId\":\"$2\",\"quantity\":$3,\"unitPriceCents\":$4}]}"
}

echo "== 1. Authentication"
code=$(curl -s -o /dev/null -w '%{http_code}' "$GW/api/orders"); check "no token is rejected" "$code" "401"
ALICE=$(login alice alice123); ADMIN=$(login admin admin123)
[ -n "$ALICE" ] && [ "$ALICE" != null ] && ok "alice logged in" || { bad "login failed - is the stack up?"; exit 1; }
code=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$GW/api/inventory/P-1003/stock" -H "Authorization: Bearer $ALICE" -H 'Content-Type: application/json' -d '{"quantity":1}')
check "USER cannot change stock (RBAC)" "$code" "403"

echo "== 2. Happy path: reserve stock -> charge -> confirm"
curl -s -X PUT "$GW/api/inventory/P-1001/stock" -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"quantity":100}' >/dev/null
R=$(order "$ALICE" P-1001 2 4999); OID=$(echo "$R" | jq -r .id)
check "order accepted as PENDING" "$(echo "$R" | jq -r .status)" "PENDING"
check "order becomes CONFIRMED" "$(wait_status "$ALICE" "$OID" CONFIRMED)" "CONFIRMED"
check "stock deducted 100 -> 98" "$(curl -s "$GW/api/inventory/P-1001" -H "Authorization: Bearer $ALICE" | jq -r .availableQuantity)" "98"
check "payment COMPLETED" "$(curl -s "$GW/api/payments/order/$OID" -H "Authorization: Bearer $ALICE" | jq -r .status)" "COMPLETED"

echo "== 3. Event store"
EVENTS=$(curl -s "$GW/api/orders/$OID/events" -H "Authorization: Bearer $ALICE" | jq -r '[.[].eventType] | join(",")')
echo "     $EVENTS"
case "$EVENTS" in *OrderCreatedEvent*InventoryReservedEvent*|*OrderCreatedEvent*PaymentCompletedEvent*OrderConfirmedEvent*) ok "event stream recorded";; *) bad "unexpected event stream";; esac

echo "== 4. Compensation: payment declined -> stock released"
curl -s -X PUT "$GW/api/inventory/P-1002/stock" -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"quantity":50}' >/dev/null
R=$(order "$ALICE" P-1002 1 2000000); OID2=$(echo "$R" | jq -r .id)
check "order becomes REJECTED" "$(wait_status "$ALICE" "$OID2" REJECTED)" "REJECTED"
sleep 2
check "stock restored 50" "$(curl -s "$GW/api/inventory/P-1002" -H "Authorization: Bearer $ALICE" | jq -r .availableQuantity)" "50"
check "payment FAILED" "$(curl -s "$GW/api/payments/order/$OID2" -H "Authorization: Bearer $ALICE" | jq -r .status)" "FAILED"

echo "== 5. Compensation: cancel confirmed order -> refund + stock release"
curl -s -X POST "$GW/api/orders/$OID/cancel" -H "Authorization: Bearer $ALICE" | jq -r .status >/dev/null
check "order CANCELLED" "$(wait_status "$ALICE" "$OID" CANCELLED)" "CANCELLED"
sleep 2
check "payment REFUNDED" "$(curl -s "$GW/api/payments/order/$OID" -H "Authorization: Bearer $ALICE" | jq -r .status)" "REFUNDED"
check "stock back to 100" "$(curl -s "$GW/api/inventory/P-1001" -H "Authorization: Bearer $ALICE" | jq -r .availableQuantity)" "100"

echo "== 6. Synchronous pre-check (insufficient stock)"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/api/orders" -H "Authorization: Bearer $ALICE" -H 'Content-Type: application/json' -d '{"items":[{"productId":"P-1003","quantity":99999,"unitPriceCents":100}]}')
check "oversized order rejected with 409" "$code" "409"

echo "== 7. Access control"
BOB=$(login bob bob123)
code=$(curl -s -o /dev/null -w '%{http_code}' "$GW/api/orders/$OID" -H "Authorization: Bearer $BOB")
check "bob cannot read alice's order" "$code" "403"

echo; echo "Result: $PASS passed, $FAIL failed"; [ "$FAIL" -eq 0 ]
