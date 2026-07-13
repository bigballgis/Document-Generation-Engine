/**
 * Publish-gate / External access diagnostics for AD Group readiness (SCEN-AOD-13…15).
 */

/** Parse `adGroupsConfigured=` from publish-gate item summary strings. */
export function parseAdGroupsConfiguredFromSummary(
  summary: string | null | undefined,
): boolean | null {
  if (!summary) {
    return null
  }
  if (summary.includes('adGroupsConfigured=false')) {
    return false
  }
  if (summary.includes('adGroupsConfigured=true')) {
    return true
  }
  return null
}

/** True when policy has at least one non-blank authorized AD group. */
export function hasConfiguredAdGroups(
  policy: { allowedAdGroups?: string[] | null } | null | undefined,
): boolean {
  return Boolean(policy?.allowedAdGroups?.some((group) => group.trim().length > 0))
}
