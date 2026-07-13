/** Shared query helpers for template catalog APIs. */
export function normalizeGroupCode(groupCode: string | undefined): string | undefined {
  const trimmed = groupCode?.trim()
  return trimmed ? trimmed.toUpperCase() : undefined
}
