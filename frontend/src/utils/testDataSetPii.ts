import type { VariableSchema } from '@/types/template'

/** CE-G03 variable PII categories (UPPER_SNAKE_CASE). */
const VARIABLE_PII_CATEGORIES = [
  'NONE',
  'PERSONAL_NAME',
  'GOVERNMENT_ID',
  'FINANCIAL_ACCOUNT',
  'CONTACT',
  'ADDRESS',
  'OTHER_SENSITIVE',
] as const

export type VariablePiiCategory = (typeof VARIABLE_PII_CATEGORIES)[number]

export type TestDataSetPiiHandling = 'SYNTHETIC' | 'EXPLICIT_SENSITIVE'

export interface TestDataSetSavePayload {
  variables: Record<string, unknown>
  piiHandling?: TestDataSetPiiHandling
  piiConfirmReason?: string
  secondaryConfirmed?: boolean
}

function isEmptyValue(value: unknown): boolean {
  if (value === undefined || value === null) {
    return true
  }
  if (typeof value === 'string' && value.trim() === '') {
    return true
  }
  return false
}

/** PII-governed field = piiCategory present and not NONE (G03-C4). */
export function isPiiMarkedField(
  variable: Pick<VariableSchema, 'piiCategory'>,
): boolean {
  const category = variable.piiCategory
  return category != null && category !== 'NONE'
}

/**
 * True when schema has at least one PII-marked field with a non-empty value
 * in the payload (G03-C7).
 */
export function payloadTouchesPiiFields(
  schema: VariableSchema[],
  variables: Record<string, unknown>,
): boolean {
  for (const variable of schema) {
    if (!isPiiMarkedField(variable)) {
      continue
    }
    if (!isEmptyValue(variables[variable.variableKey])) {
      return true
    }
  }
  return false
}

export function normalizePiiCategory(
  category: string | null | undefined,
): VariablePiiCategory {
  if (category == null || category === '') {
    return 'NONE'
  }
  if ((VARIABLE_PII_CATEGORIES as readonly string[]).includes(category)) {
    return category as VariablePiiCategory
  }
  return 'NONE'
}
