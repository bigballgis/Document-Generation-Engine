/**
 * Storage keys (e.g. artifacts/prev-1.docx) must never be treated as anchor ids
 * or shown raw in the Artifact column.
 */
export function isArtifactStorageKey(value: string | null | undefined): boolean {
  if (!value) {
    return false
  }
  const trimmed = value.trim()
  return trimmed.includes('/') || trimmed.startsWith('artifacts')
}

export function friendlyArtifactLabel(
  artifact: string | null | undefined,
  artifactHint: string | null | undefined,
  fallback: string,
): string {
  const primary = artifact?.trim()
  if (primary && !isArtifactStorageKey(primary)) {
    return primary
  }
  const hint = artifactHint?.trim() || (primary && isArtifactStorageKey(primary) ? primary : '')
  if (!hint) {
    return fallback
  }
  const base = hint.split('/').pop()?.trim()
  return base && base.length > 0 ? base : fallback
}

/** Prefer a real location/anchor; never return a storage key. */
export function resolveFidelityEditAnchorId(warning: {
  location?: string | null
  artifact?: string | null
}): string | null {
  const location = warning.location?.trim()
  if (location && !isArtifactStorageKey(location)) {
    return location
  }
  const artifact = warning.artifact?.trim()
  if (artifact && !isArtifactStorageKey(artifact)) {
    return artifact
  }
  return null
}
