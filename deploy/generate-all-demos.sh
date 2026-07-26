#!/usr/bin/env bash
# FOS-W14-2 — Linux/macOS entry for generate-all-demos (thin wrapper → PowerShell Core).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if ! command -v pwsh >/dev/null 2>&1; then
  echo "pwsh (PowerShell Core) is required to run deploy/generate-all-demos.ps1" >&2
  exit 127
fi
exec pwsh -NoProfile -File "$ROOT/deploy/generate-all-demos.ps1" "$@"
