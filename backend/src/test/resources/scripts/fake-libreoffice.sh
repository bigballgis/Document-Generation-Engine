#!/usr/bin/env bash
set -euo pipefail
OUTDIR=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --outdir)
      OUTDIR="$2"
      shift 2
      ;;
    *)
      shift
      ;;
  esac
done
if [[ -z "$OUTDIR" ]]; then
  exit 1
fi
printf '%%PDF-1.4\n' > "$OUTDIR/input.pdf"
exit 0
