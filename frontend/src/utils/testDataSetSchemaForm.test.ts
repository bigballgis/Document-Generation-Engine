import { describe, expect, it } from 'vitest'
import type { VariableSchema } from '@/types/template'
import {
  ADVANCED_JSON_VAR_THRESHOLD,
  buildSchemaSkeleton,
  enterableVariables,
  isComputeVariable,
  parseVariablesJson,
  shouldExpandAdvancedJson,
  stripComputeKeys,
  validateVariablesAgainstSchema,
} from '@/utils/testDataSetSchemaForm'

function schema(
  variableKey: string,
  variableType: VariableSchema['variableType'],
  overrides: Partial<VariableSchema> = {},
): VariableSchema {
  return {
    variableKey,
    variableType,
    required: false,
    defaultValue: null,
    enumValues: null,
    description: null,
    computeExpression: null,
    ...overrides,
  }
}

describe('testDataSetSchemaForm', () => {
  it('detects compute via COMPUTED type or computeExpression', () => {
    expect(isComputeVariable(schema('a', 'COMPUTED'))).toBe(true)
    expect(isComputeVariable(schema('b', 'TEXT', { computeExpression: '${x}' }))).toBe(true)
    expect(isComputeVariable(schema('c', 'TEXT'))).toBe(false)
  })

  it('excludes compute fields from enterable list', () => {
    const list = enterableVariables([
      schema('principal', 'AMOUNT', { required: true }),
      schema('principalCn', 'COMPUTED'),
      schema('label', 'TEXT', { computeExpression: '  ${x}  ' }),
    ])
    expect(list.map((item) => item.variableKey)).toEqual(['principal'])
  })

  it('builds skeleton from defaults and type placeholders without Sample hardcode', () => {
    const skeleton = buildSchemaSkeleton([
      schema('customerName', 'TEXT', { defaultValue: 'Acme', required: true }),
      schema('flag', 'BOOLEAN'),
      schema('amount', 'AMOUNT'),
      schema('status', 'ENUM', { enumValues: 'ACTIVE,CLOSED' }),
      schema('items', 'LIST'),
      schema('meta', 'OBJECT'),
      schema('principalCn', 'TEXT', { computeExpression: '${principal}' }),
    ])
    expect(skeleton).toEqual({
      customerName: 'Acme',
      flag: false,
      amount: 0,
      status: 'ACTIVE',
      items: [],
      meta: {},
    })
    expect(skeleton).not.toHaveProperty('principalCn')
    expect(JSON.stringify(skeleton)).not.toContain('Sample')
  })

  it('returns empty skeleton when schema is empty', () => {
    expect(buildSchemaSkeleton([])).toEqual({})
  })

  it('validates required and type errors without unknown compute failures', () => {
    const variablesSchema = [
      schema('customerName', 'TEXT', { required: true }),
      schema('amount', 'AMOUNT'),
      schema('principalCn', 'COMPUTED', { required: true }),
    ]
    expect(validateVariablesAgainstSchema(variablesSchema, {})).toEqual([
      expect.objectContaining({ field: 'customerName', reason: 'REQUIRED' }),
    ])
    expect(
      validateVariablesAgainstSchema(variablesSchema, {
        customerName: 'Acme',
        amount: 'not-a-number',
      }),
    ).toEqual([expect.objectContaining({ field: 'amount', reason: 'INVALID_TYPE' })])
    expect(
      validateVariablesAgainstSchema(variablesSchema, { customerName: 'Acme', principal: 100 }),
    ).toEqual([expect.objectContaining({ field: 'principal', reason: 'UNKNOWN_FIELD' })])
  })

  it('validates ENUM_NOT_ALLOWED', () => {
    const errors = validateVariablesAgainstSchema(
      [schema('status', 'ENUM', { enumValues: 'ACTIVE,CLOSED' })],
      { status: 'NOPE' },
    )
    expect(errors).toEqual([expect.objectContaining({ field: 'status', reason: 'ENUM_NOT_ALLOWED' })])
  })

  it('parses illegal JSON as INVALID_JSON', () => {
    const result = parseVariablesJson('{not-json')
    expect(result.ok).toBe(false)
    if (!result.ok) {
      expect(result.error.reason).toBe('INVALID_JSON')
    }
  })

  it('strips compute keys from payload', () => {
    const stripped = stripComputeKeys(
      [schema('principal', 'AMOUNT'), schema('principalCn', 'COMPUTED')],
      { principal: 100, principalCn: '壹佰' },
    )
    expect(stripped).toEqual({ principal: 100 })
  })

  it('expands advanced JSON for large schemas', () => {
    const large = Array.from({ length: ADVANCED_JSON_VAR_THRESHOLD }, (_, index) =>
      schema(`field${index}`, 'TEXT'),
    )
    expect(shouldExpandAdvancedJson(large, '{}')).toBe(true)
    expect(shouldExpandAdvancedJson([schema('a', 'TEXT')], '{}')).toBe(false)
  })
})
