#!/usr/bin/env bash
# Thin wrapper — prefer: pwsh ./scripts/validate-doc-structure.ps1
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if command -v pwsh >/dev/null 2>&1; then
  exec pwsh -NoProfile -File "$ROOT/scripts/validate-doc-structure.ps1" "$@"
fi
if command -v powershell >/dev/null 2>&1; then
  exec powershell -NoProfile -File "$ROOT/scripts/validate-doc-structure.ps1" "$@"
fi
echo "validate-doc-structure: pwsh/powershell not found" >&2
exit 127
