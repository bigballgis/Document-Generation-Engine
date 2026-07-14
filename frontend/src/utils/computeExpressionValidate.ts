/** CE-K03 client-side compute expression checks (mirrors backend whitelist bounds lightly). */

const WHITELIST = new Set([
  'COALESCE',
  'SUM',
  'COUNT',
  'AVG',
  'FILTER',
  'FORMAT_AMOUNT',
  'FORMAT_DATE',
  'SPELL_AMOUNT',
])

const MAX_LENGTH = 2048

export function extractComputeVariableRoots(expression: string): string[] {
  const roots = new Set<string>()
  const pattern = /\$\{([A-Za-z_][A-Za-z0-9_.-]*)\}/g
  let match: RegExpExecArray | null
  while ((match = pattern.exec(expression)) !== null) {
    const path = match[1]
    roots.add(path.split('.')[0] ?? path)
  }
  return [...roots]
}

export function validateComputeExpressionClient(
  expression: string,
  knownVariableKeys: string[],
): { valid: boolean; messageKey?: string } {
  const trimmed = expression?.trim() ?? ''
  if (!trimmed) {
    return { valid: false, messageKey: 'templates.authoring.computeExpressionRequired' }
  }
  if (trimmed.length > MAX_LENGTH) {
    return { valid: false, messageKey: 'templates.authoring.computeExpressionTooLong' }
  }
  if (/\.\s*[A-Za-z_]\w*\s*\(/.test(trimmed) || /\$\{[^}]+\}\s*\./.test(trimmed)) {
    return { valid: false, messageKey: 'templates.authoring.computeExpressionIllegalConstruct' }
  }
  const fnPattern = /\b([A-Z][A-Z0-9_]*)\s*\(/g
  let match: RegExpExecArray | null
  while ((match = fnPattern.exec(trimmed)) !== null) {
    if (!WHITELIST.has(match[1])) {
      return { valid: false, messageKey: 'templates.authoring.computeExpressionUnknownFunction' }
    }
  }
  const known = new Set(knownVariableKeys)
  for (const root of extractComputeVariableRoots(trimmed)) {
    if (!known.has(root)) {
      return { valid: false, messageKey: 'templates.authoring.computeExpressionMissingReference' }
    }
  }
  return { valid: true }
}
