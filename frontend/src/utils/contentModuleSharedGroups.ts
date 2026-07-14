/** Normalize shared group codes for payload / comparison (sorted, uppercased, distinct). */
export function normalizeSharedGroupCodes(codes: string[] | undefined | null): string[] {
  return [
    ...new Set(
      (codes ?? [])
        .map((code) => code?.trim().toUpperCase() ?? '')
        .filter((code) => code.length > 0),
    ),
  ].sort()
}

/** Drop the owning group from a shared selection (owner already implies access). */
export function excludeOwnerFromSharedGroupCodes(
  codes: string[] | undefined | null,
  ownerGroupCode: string,
): string[] {
  const owner = ownerGroupCode.trim().toUpperCase()
  return normalizeSharedGroupCodes(codes).filter((code) => code !== owner)
}

export function sharedGroupSelectionChanged(
  current: string[] | undefined | null,
  baseline: string[] | undefined | null,
): boolean {
  return (
    normalizeSharedGroupCodes(current).join('\0') !== normalizeSharedGroupCodes(baseline).join('\0')
  )
}

export function formatSharedGroupCodesLabel(codes: string[] | undefined | null): string {
  return normalizeSharedGroupCodes(codes).join(', ')
}
