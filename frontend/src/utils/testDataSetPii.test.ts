import { describe, expect, it } from 'vitest'
import type { VariableSchema } from '@/types/template'
import {
  isPiiMarkedField,
  normalizePiiCategory,
  payloadTouchesPiiFields,
} from '@/utils/testDataSetPii'

function schema(
  variableKey: string,
  overrides: Partial<VariableSchema> = {},
): VariableSchema {
  return {
    variableKey,
    variableType: 'TEXT',
    required: false,
    defaultValue: null,
    enumValues: null,
    description: null,
    computeExpression: null,
    ...overrides,
  }
}

describe('testDataSetPii', () => {
  it('treats NONE / omitted as non-PII', () => {
    expect(isPiiMarkedField(schema('a'))).toBe(false)
    expect(isPiiMarkedField(schema('a', { piiCategory: 'NONE' }))).toBe(false)
    expect(isPiiMarkedField(schema('a', { piiCategory: null }))).toBe(false)
  })

  it('marks PERSONAL_NAME and other categories as PII', () => {
    expect(isPiiMarkedField(schema('a', { piiCategory: 'PERSONAL_NAME' }))).toBe(true)
    expect(isPiiMarkedField(schema('a', { piiCategory: 'FINANCIAL_ACCOUNT' }))).toBe(true)
  })

  it('does not require handling when PII field is empty (G03-C5/C8)', () => {
    const variables = [schema('customerName', { piiCategory: 'PERSONAL_NAME' })]
    expect(payloadTouchesPiiFields(variables, {})).toBe(false)
    expect(payloadTouchesPiiFields(variables, { customerName: '' })).toBe(false)
    expect(payloadTouchesPiiFields(variables, { customerName: '   ' })).toBe(false)
  })

  it('requires handling when PII field has a non-empty value (G03-C7)', () => {
    const variables = [schema('customerName', { piiCategory: 'PERSONAL_NAME' })]
    expect(payloadTouchesPiiFields(variables, { customerName: 'Jane Doe' })).toBe(true)
  })

  it('normalizes missing category to NONE', () => {
    expect(normalizePiiCategory(undefined)).toBe('NONE')
    expect(normalizePiiCategory(null)).toBe('NONE')
    expect(normalizePiiCategory('PERSONAL_NAME')).toBe('PERSONAL_NAME')
  })
})
