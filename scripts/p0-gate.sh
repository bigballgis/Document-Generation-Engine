#!/usr/bin/env bash
# SOR-C04 — Linux parity for constitution gates (mirrors scripts/p0-gate.ps1)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo '=== P0 Gate: Backend ==='
(cd "$ROOT/backend" && mvn -B -ntp verify)

echo '=== P0 Gate: Frontend ==='
(
  cd "$ROOT/frontend"
  if [[ ! -d node_modules ]]; then
    pnpm install
  fi
  pnpm lint
  pnpm type-check
  pnpm test
  pnpm build
)

echo '=== P0 Gate: PASSED ==='
