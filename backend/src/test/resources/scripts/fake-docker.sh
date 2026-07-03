#!/usr/bin/env bash
# Test double for `docker cp` / `docker exec` used by DockerExecPdfConversionServiceTest.
set -euo pipefail

state_root="${DOCGEN_FAKE_DOCKER_STATE:-/tmp/docgen-fake-docker-state}"
mkdir -p "$state_root"

command_name="$1"
shift

if [[ "$command_name" == "cp" ]]; then
  src="$1"
  dest="$2"
  if [[ "$dest" == *:* ]]; then
    container="${dest%%:*}"
    remote_path="${dest#*:}"
    target="$state_root/$container$remote_path"
    mkdir -p "$(dirname "$target")"
    cp "$src" "$target"
    exit 0
  fi
  if [[ "$src" == *:* ]]; then
    container="${src%%:*}"
    remote_path="${src#*:}"
    host_dest="$dest"
    source_file="$state_root/$container$remote_path"
    if [[ -f "$source_file" ]]; then
      cp "$source_file" "$host_dest"
    else
      printf '%%PDF-1.4' > "$host_dest"
    fi
    exit 0
  fi
  exit 1
fi

if [[ "$command_name" == "exec" ]]; then
  container="$1"
  shift
  remapped=()
  for arg in "$@"; do
    if [[ "$arg" == /tmp/* ]]; then
      remapped+=("$state_root/$container$arg")
    else
      remapped+=("$arg")
    fi
  done
  exec "${remapped[@]}"
fi

exit 1
