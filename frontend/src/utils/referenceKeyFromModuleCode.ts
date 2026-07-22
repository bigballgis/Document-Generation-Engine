/**
 * Client-side `referenceKey` suggestion from content-module `moduleCode` (BEI-C8 / BEI-C9).
 * No backend generate API — used by Add clause reference dialog only.
 */

/** Normalize moduleCode → UPPER_SNAKE (BEI-C8). Empty when nothing alphanumeric remains. */
export function normalizeModuleCodeToReferenceKey(moduleCode: string): string {
  const withUnderscores = moduleCode.replace(/[^A-Za-z0-9]+/g, '_')
  const collapsed = withUnderscores.replace(/_+/g, '_')
  const trimmed = collapsed.replace(/^_+|_+$/g, '')
  return trimmed.toUpperCase()
}

/**
 * Allocate a unique key against existing template reference keys (BEI-C9).
 * Prefers base; then `_2`, `_3`, … smallest unused `_<n>` for n≥2.
 */
export function allocateUniqueReferenceKey(
  baseKey: string,
  existingKeys: Iterable<string>,
): string {
  if (!baseKey) {
    return ''
  }
  const taken = new Set(existingKeys)
  if (!taken.has(baseKey)) {
    return baseKey
  }
  let suffix = 2
  while (taken.has(`${baseKey}_${suffix}`)) {
    suffix += 1
  }
  return `${baseKey}_${suffix}`
}

/** Normalize moduleCode then allocate against existing keys. */
export function suggestReferenceKey(
  moduleCode: string,
  existingKeys: Iterable<string>,
): string {
  const base = normalizeModuleCodeToReferenceKey(moduleCode)
  return allocateUniqueReferenceKey(base, existingKeys)
}
