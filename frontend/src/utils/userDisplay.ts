export function formatUserDisplayLabel(username: string, displayName?: string | null): string {
  const trimmedDisplay = displayName?.trim()
  if (trimmedDisplay) {
    return trimmedDisplay
  }
  const trimmedUsername = username?.trim()
  if (trimmedUsername) {
    return trimmedUsername
  }
  return '—'
}

export function resolveUpdatedByDisplay(
  updatedBy: string,
  updatedByDisplayName?: string | null,
): string {
  return formatUserDisplayLabel(updatedBy, updatedByDisplayName)
}

export function resolveSubmitterDisplay(
  submitterUserId?: string,
  submitterDisplayName?: string | null,
): string {
  if (!submitterUserId?.trim() && !submitterDisplayName?.trim()) {
    return '—'
  }
  return formatUserDisplayLabel(submitterUserId ?? '', submitterDisplayName)
}
