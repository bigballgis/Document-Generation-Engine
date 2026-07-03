#!/bin/sh
OUTDIR=""
while [ $# -gt 0 ]; do
  case "$1" in
    --outdir) OUTDIR="$2"; shift 2 ;;
    *) shift ;;
  esac
done
[ -z "$OUTDIR" ] && exit 1
printf "%%PDF-1.4\n" > "$OUTDIR/input.pdf"
exit 0
