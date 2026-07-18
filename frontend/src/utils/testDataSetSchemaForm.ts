import type { VariableSchema } from '@/types/template'
import type { ApiFieldError } from '@/types/session'

/** Expand Advanced JSON when enterable vars ≥ this count (U03-C7). */
export const ADVANCED_JSON_VAR_THRESHOLD = 12

/** Expand Advanced JSON when serialized payload ≥ this size in bytes (U03-C7). */
const ADVANCED_JSON_SIZE_THRESHOLD = 2048

export type SchemaFieldErrorReason =
  | 'REQUIRED'
  | 'INVALID_TYPE'
  | 'INVALID_FORMAT'
  | 'ENUM_NOT_ALLOWED'
  | 'UNKNOWN_FIELD'
  | 'INVALID_JSON'

export interface SchemaFieldError {
  field: string
  reason: SchemaFieldErrorReason
  messageKey: string
}

export function isComputeVariable(
  variable: Pick<VariableSchema, 'variableType' | 'computeExpression'>,
): boolean {
  if (variable.variableType === 'COMPUTED') {
    return true
  }
  return Boolean(variable.computeExpression?.trim())
}

export function enterableVariables(schema: VariableSchema[]): VariableSchema[] {
  return schema.filter((variable) => !isComputeVariable(variable))
}

export function parseEnumValues(enumValues: string | null | undefined): string[] {
  if (!enumValues?.trim()) {
    return []
  }
  const trimmed = enumValues.trim()
  if (trimmed.startsWith('[')) {
    try {
      const parsed: unknown = JSON.parse(trimmed)
      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item)).filter((item) => item.length > 0)
      }
    } catch {
      // fall through to comma split
    }
  }
  return trimmed
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

function parseDefaultValue(variable: VariableSchema): unknown | undefined {
  const raw = variable.defaultValue
  if (raw == null || raw === '') {
    return undefined
  }
  const type = variable.variableType
  if (type === 'NUMBER' || type === 'AMOUNT') {
    const num = Number(raw)
    return Number.isFinite(num) ? num : undefined
  }
  if (type === 'BOOLEAN') {
    if (raw === 'true' || raw === 'TRUE' || raw === '1') {
      return true
    }
    if (raw === 'false' || raw === 'FALSE' || raw === '0') {
      return false
    }
    return undefined
  }
  if (type === 'LIST' || type === 'OBJECT') {
    try {
      return JSON.parse(raw) as unknown
    } catch {
      return undefined
    }
  }
  return raw
}

function typePlaceholder(variable: VariableSchema): unknown {
  switch (variable.variableType) {
    case 'NUMBER':
    case 'AMOUNT':
      return 0
    case 'BOOLEAN':
      return false
    case 'LIST':
      return []
    case 'OBJECT':
      return {}
    case 'ENUM': {
      const values = parseEnumValues(variable.enumValues)
      return values[0] ?? ''
    }
    case 'DATE':
    case 'TEXT':
    default:
      return ''
  }
}

/** Build typed skeleton from schema (skips compute fields). */
export function buildSchemaSkeleton(schema: VariableSchema[]): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  for (const variable of enterableVariables(schema)) {
    const fromDefault = parseDefaultValue(variable)
    result[variable.variableKey] = fromDefault !== undefined ? fromDefault : typePlaceholder(variable)
  }
  return result
}

export function stripComputeKeys(
  schema: VariableSchema[],
  variables: Record<string, unknown>,
): Record<string, unknown> {
  const computeKeys = new Set(
    schema.filter((variable) => isComputeVariable(variable)).map((variable) => variable.variableKey),
  )
  const result: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(variables)) {
    if (!computeKeys.has(key)) {
      result[key] = value
    }
  }
  return result
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

function isValidNumber(value: unknown): boolean {
  if (typeof value === 'number') {
    return Number.isFinite(value)
  }
  if (typeof value === 'string' && value.trim() !== '') {
    return Number.isFinite(Number(value))
  }
  return false
}

function isValidDate(value: unknown): boolean {
  if (typeof value !== 'string' || value.trim() === '') {
    return false
  }
  const parsed = Date.parse(value)
  return !Number.isNaN(parsed)
}

function isValidBoolean(value: unknown): boolean {
  return typeof value === 'boolean'
}

function validateFieldType(variable: VariableSchema, value: unknown): SchemaFieldError | null {
  const field = variable.variableKey
  switch (variable.variableType) {
    case 'NUMBER':
    case 'AMOUNT':
      if (!isValidNumber(value)) {
        return {
          field,
          reason: 'INVALID_TYPE',
          messageKey: 'templates.testDataSets.validation.invalidType',
        }
      }
      return null
    case 'BOOLEAN':
      if (!isValidBoolean(value)) {
        return {
          field,
          reason: 'INVALID_TYPE',
          messageKey: 'templates.testDataSets.validation.invalidType',
        }
      }
      return null
    case 'DATE':
      if (!isValidDate(value)) {
        return {
          field,
          reason: 'INVALID_FORMAT',
          messageKey: 'templates.testDataSets.validation.invalidFormat',
        }
      }
      return null
    case 'ENUM': {
      const allowed = parseEnumValues(variable.enumValues)
      if (typeof value !== 'string' || (allowed.length > 0 && !allowed.includes(value))) {
        return {
          field,
          reason: 'ENUM_NOT_ALLOWED',
          messageKey: 'templates.testDataSets.validation.enumNotAllowed',
        }
      }
      return null
    }
    case 'LIST':
      if (!Array.isArray(value)) {
        return {
          field,
          reason: 'INVALID_TYPE',
          messageKey: 'templates.testDataSets.validation.invalidType',
        }
      }
      return null
    case 'OBJECT':
      if (value === null || typeof value !== 'object' || Array.isArray(value)) {
        return {
          field,
          reason: 'INVALID_TYPE',
          messageKey: 'templates.testDataSets.validation.invalidType',
        }
      }
      return null
    case 'TEXT':
    default:
      if (typeof value !== 'string' && typeof value !== 'number' && typeof value !== 'boolean') {
        return {
          field,
          reason: 'INVALID_TYPE',
          messageKey: 'templates.testDataSets.validation.invalidType',
        }
      }
      return null
  }
}

/** Client-side schema validation for enterable fields (U03-C5). */
export function validateVariablesAgainstSchema(
  schema: VariableSchema[],
  variables: Record<string, unknown>,
): SchemaFieldError[] {
  const errors: SchemaFieldError[] = []
  const enterable = enterableVariables(schema)
  const knownKeys = new Set(schema.map((variable) => variable.variableKey))

  for (const variable of enterable) {
    const value = variables[variable.variableKey]
    if (variable.required && isEmptyValue(value)) {
      errors.push({
        field: variable.variableKey,
        reason: 'REQUIRED',
        messageKey: 'templates.testDataSets.validation.required',
      })
      continue
    }
    if (isEmptyValue(value)) {
      continue
    }
    const typeError = validateFieldType(variable, value)
    if (typeError) {
      errors.push(typeError)
    }
  }

  for (const key of Object.keys(variables)) {
    if (!knownKeys.has(key)) {
      errors.push({
        field: key,
        reason: 'UNKNOWN_FIELD',
        messageKey: 'templates.testDataSets.validation.unknownField',
      })
    }
  }

  return errors
}

export function parseVariablesJson(jsonText: string): {
  ok: true
  value: Record<string, unknown>
} | {
  ok: false
  error: SchemaFieldError
} {
  try {
    const parsed: unknown = JSON.parse(jsonText)
    if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {
        ok: false,
        error: {
          field: 'variables',
          reason: 'INVALID_JSON',
          messageKey: 'templates.testDataSets.validation.invalidJson',
        },
      }
    }
    return { ok: true, value: parsed as Record<string, unknown> }
  } catch {
    return {
      ok: false,
      error: {
        field: 'variables',
        reason: 'INVALID_JSON',
        messageKey: 'templates.testDataSets.validation.invalidJson',
      },
    }
  }
}

export function stringifyVariablesJson(variables: unknown): string {
  return JSON.stringify(variables, null, 2)
}

export function shouldExpandAdvancedJson(
  schema: VariableSchema[],
  variablesJson: string,
): boolean {
  if (enterableVariables(schema).length >= ADVANCED_JSON_VAR_THRESHOLD) {
    return true
  }
  return new TextEncoder().encode(variablesJson).length >= ADVANCED_JSON_SIZE_THRESHOLD
}

export function isNonEmptyVariablesPayload(variables: Record<string, unknown>): boolean {
  return Object.keys(variables).length > 0
}

export function mapApiFieldErrors(fieldErrors: ApiFieldError[] | undefined): SchemaFieldError[] {
  if (!fieldErrors?.length) {
    return []
  }
  return fieldErrors.map((item) => ({
    field: item.field,
    reason: (item.reason as SchemaFieldErrorReason) || 'INVALID_TYPE',
    messageKey:
      item.reason === 'REQUIRED'
        ? 'templates.testDataSets.validation.required'
        : item.reason === 'ENUM_NOT_ALLOWED'
          ? 'templates.testDataSets.validation.enumNotAllowed'
          : item.reason === 'INVALID_FORMAT'
            ? 'templates.testDataSets.validation.invalidFormat'
            : item.reason === 'UNKNOWN_FIELD'
              ? 'templates.testDataSets.validation.unknownField'
              : 'templates.testDataSets.validation.invalidType',
  }))
}

export function fieldErrorsToMap(errors: SchemaFieldError[]): Record<string, string> {
  const map: Record<string, string> = {}
  for (const error of errors) {
    if (!map[error.field]) {
      map[error.field] = error.messageKey
    }
  }
  return map
}
