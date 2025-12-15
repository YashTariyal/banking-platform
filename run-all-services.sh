#!/usr/bin/env bash

set -euo pipefail

# Runs all Spring Boot microservices in this repo at once.
# Usage:
#   ./run-all-services.sh           # default profile
#   SPRING_PROFILES_ACTIVE=local ./run-all-services.sh
#
# Each service is started in the background with its own log file under logs/.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${ROOT_DIR}/logs"

mkdir -p "${LOG_DIR}"

services=(
  "services/identity-service"
  "services/customer-service"
  "services/kyc-service"
  "services/account-service"
  "services/ledger-service"
  "services/transaction-service"
  "services/payment-service"
  "services/card-service"
  "services/loan-service"
  "services/risk-service"
  "services/compliance-service"
  "services/support-service"
)

declare -a pids=()

start_service() {
  local svc_path="$1"
  local svc_name

  svc_name="$(basename "${svc_path}")"
  echo "Starting ${svc_name}..."

  (
    cd "${ROOT_DIR}/${svc_path}"
    mvn -q spring-boot:run
  ) >"${LOG_DIR}/${svc_name}.log" 2>&1 &

  local pid=$!
  pids+=("${pid}")
  echo "  -> ${svc_name} started with PID ${pid}, logging to ${LOG_DIR}/${svc_name}.log"
}

cleanup() {
  echo
  echo "Stopping all services..."
  for pid in "${pids[@]:-}"; do
    if kill -0 "${pid}" 2>/dev/null; then
      echo "  -> Killing PID ${pid}"
      kill "${pid}" 2>/dev/null || true
    fi
  done
}

trap cleanup EXIT

for svc in "${services[@]}"; do
  start_service "${svc}"
done

echo
echo "All services started. Tail logs with:"
echo "  tail -f logs/*.log"
echo
echo "Press Ctrl+C to stop all services."

# Keep script running so trap can clean up on Ctrl+C
wait

