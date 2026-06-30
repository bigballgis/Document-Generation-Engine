/**
 * Collect dotted key paths for string leaf values in a nested locale object.
 */
export function collectLeafKeys(
  obj: Record<string, unknown>,
  prefix = '',
): string[] {
  const keys: string[] = []

  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key

    if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
      keys.push(...collectLeafKeys(value as Record<string, unknown>, path))
      continue
    }

    if (typeof value === 'string') {
      keys.push(path)
    }
  }

  return keys.sort()
}

/**
 * Resolve a dotted key path against a nested locale object.
 */
export function resolveLeafValue(
  obj: Record<string, unknown>,
  dottedKey: string,
): unknown {
  return dottedKey.split('.').reduce<unknown>((current, segment) => {
    if (current === null || typeof current !== 'object') {
      return undefined
    }
    return (current as Record<string, unknown>)[segment]
  }, obj)
}
