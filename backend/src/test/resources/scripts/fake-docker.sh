#!/usr/bin/env bash
# Test double for `docker cp` / `docker exec` used by DockerExecPdfConversionServiceTest.
set -euo pipefail

# Prefer per-test pointer (written by DockerExecPdfConversionServiceTest) over env,
# so a leaked DOCGEN_FAKE_DOCKER_STATE cannot steal profile logs.
pointer="${DOCGEN_FAKE_DOCKER_STATE_POINTER:-${TMPDIR:-/tmp}/docgen-fake-docker-state.pointer}"
if [[ -f "$pointer" ]]; then
  state_root="$(cat "$pointer")"
elif [[ -n "${DOCGEN_FAKE_DOCKER_STATE:-}" ]]; then
  state_root="$DOCGEN_FAKE_DOCKER_STATE"
else
  state_root="/tmp/docgen-fake-docker-state"
fi
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
  if [[ "${1:-}" == "rm" && "${2:-}" == "-rf" ]]; then
    shift 2
    for target in "$@"; do
      remapped_target="$state_root/$container$target"
      rm -rf "$remapped_target"
    done
    exit 0
  fi
  remapped=()
  for arg in "$@"; do
    if [[ "$arg" == /tmp/* ]]; then
      remapped+=("$state_root/$container$arg")
    elif [[ "$arg" == -env:UserInstallation=file://* ]]; then
      profile_path="${arg#-env:UserInstallation=file://}"
      remapped_profile="$state_root/$container$profile_path"
      mkdir -p "$remapped_profile"
      echo "$profile_path" >> "$state_root/profile-invocations.log"
      remapped+=("-env:UserInstallation=file://$remapped_profile")
    else
      remapped+=("$arg")
    fi
  done
  exec "${remapped[@]}"
fi

exit 1
