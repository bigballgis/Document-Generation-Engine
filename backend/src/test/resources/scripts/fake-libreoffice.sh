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
printf '%s\n' "$CONVERT_TO" > "$OUTDIR/convert-to.txt"
if [[ "$CONVERT_TO" == "docx" ]]; then
  out_name="assembled-in.docx"
  if [[ -n "$INPUT" ]]; then
    out_name="$(basename "$INPUT")"
  fi
  out_path="$OUTDIR/$out_name"
  if [[ -n "$INPUT" && -f "$INPUT" ]]; then
    if [[ "$INPUT" -ef "$out_path" ]]; then
      : # already in place (normalization writes into the same temp dir)
    else
      cp "$INPUT" "$out_path"
    fi
  else
    printf 'PK\x03\x04' > "$out_path"
  fi
  exit 0
fi
pdf_name="input.pdf"
if [[ -n "$INPUT" ]]; then
  base="$(basename "$INPUT")"
  pdf_name="${base%.*}.pdf"
fi
printf '%%PDF-1.4\nconvert-to=%s\n' "$CONVERT_TO" > "$OUTDIR/$pdf_name"
exit 0
