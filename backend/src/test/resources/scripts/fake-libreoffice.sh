#!/usr/bin/env bash
set -euo pipefail
OUTDIR=""
CONVERT_TO="pdf"
INPUT=""
PROFILE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --outdir)
      OUTDIR="$2"
      shift 2
      ;;
    --convert-to)
      CONVERT_TO="$2"
      shift 2
      ;;
    -env:UserInstallation=*)
      PROFILE="${1#-env:UserInstallation=}"
      PROFILE="${PROFILE#file://}"
      shift
      ;;
    *)
      if [[ -f "$1" ]]; then
        INPUT="$1"
      fi
      shift
      ;;
  esac
done
if [[ -n "$PROFILE" ]]; then
  mkdir -p "$PROFILE"
fi
if [[ -z "$OUTDIR" ]]; then
  exit 1
fi
if [[ "$CONVERT_TO" == "docx" ]]; then
  if [[ -n "$INPUT" && -f "$INPUT" ]]; then
    cp "$INPUT" "$OUTDIR/$(basename "$INPUT")"
  else
    printf 'PK\x03\x04' > "$OUTDIR/assembled-in.docx"
  fi
  exit 0
fi
printf '%%PDF-1.4\n' > "$OUTDIR/input.pdf"
exit 0
