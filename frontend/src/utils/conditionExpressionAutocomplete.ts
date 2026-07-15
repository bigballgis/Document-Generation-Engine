/**
 * Variable-schema autocomplete helpers for conditionExpression inputs (CE-U13).
 * Insert shape is always `${variableKey}` (ConditionExpressionEvaluator dialect).
 */

export function filterVariableKeysForAutocomplete(
  variableKeys: string[],
  queryPrefix: string,
): string[] {
  const normalized = queryPrefix.trim().toLowerCase()
  const unique = [...new Set(variableKeys.filter(Boolean))]
  if (!normalized) {
    return unique
  }
  return unique.filter((key) => key.toLowerCase().startsWith(normalized))
}

/** Detect active `${prefix` token at caret for suggestion filtering. */
export function detectDollarBracePrefix(
  value: string,
  caretIndex: number,
): { start: number; prefix: string } | null {
  const before = value.slice(0, Math.max(0, caretIndex))
  const match = before.match(/\$\{([A-Za-z0-9_.-]*)$/)
  if (!match) {
    return null
  }
  return {
    start: before.length - match[0].length,
    prefix: match[1] ?? '',
  }
}

export function insertVariableReference(
  value: string,
  caretIndex: number,
  variableKey: string,
  options?: { replaceDollarBracePrefix?: boolean },
): { nextValue: string; nextCaret: number } {
  const token = `\${${variableKey}}`
  if (options?.replaceDollarBracePrefix) {
    const detected = detectDollarBracePrefix(value, caretIndex)
    if (detected) {
      const before = value.slice(0, detected.start)
      const after = value.slice(caretIndex)
      const nextValue = `${before}${token}${after}`
      return { nextValue, nextCaret: before.length + token.length }
    }
  }
  const before = value.slice(0, caretIndex)
  const after = value.slice(caretIndex)
  const nextValue = `${before}${token}${after}`
  return { nextValue, nextCaret: before.length + token.length }
}
