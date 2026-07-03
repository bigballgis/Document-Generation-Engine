#!/usr/bin/env bash
# SOR-C04 — run constitution gates before docker deploy (Linux parity)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "$ROOT/scripts/p0-gate.sh"
