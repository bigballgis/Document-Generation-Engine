import type { TemplateVersionLineSummary } from '@/types/template'
import { isInFlightVersionLine } from '@/utils/templateVersionLine'

/**
 * CE-U19 — pick which published line supplies the master pin for Dependencies.
 * Prefer default-route target; else first published line in the list.
 */
export function selectPinReleaseVersion(
  lines: readonly TemplateVersionLineSummary[],
): string | null {
  const published = lines.filter(
    (row) => !isInFlightVersionLine(row) && Boolean(row.releaseVersion?.trim()),
  )
  const defaultRoute = published.find((row) => row.defaultRouteTarget === true)
  return defaultRoute?.releaseVersion ?? published[0]?.releaseVersion ?? null
}

/** Truncate master file hash for secondary mono display (not a sole label). */
export function truncateMasterFileHash(hash: string, visible = 12): string {
  const trimmed = hash.trim()
  if (trimmed.length <= visible) {
    return trimmed
  }
  return `${trimmed.slice(0, visible)}…`
}
