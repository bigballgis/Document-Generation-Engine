import { describe, expect, it } from 'vitest'
import {
  extractComputeVariableRoots,
  validateComputeExpressionClient,
} from '@/utils/computeExpressionValidate'

describe('extractComputeVariableRoots', () => {
  it('collects unique root keys from nested paths', () => {
    expect(
      extractComputeVariableRoots('SUM(FILTER(${items.line}, amount, GT, 0)) + ${principal}'),
    ).toEqual(expect.arrayContaining(['items', 'principal']))
  })
})

describe('validateComputeExpressionClient', () => {
  it('accepts whitelist SPELL_AMOUNT with known ref', () => {
    expect(
      validateComputeExpressionClient('SPELL_AMOUNT(${principal})', ['principal', 'principalCn']),
    ).toEqual({ valid: true })
  })

  it('rejects empty expression', () => {
    expect(validateComputeExpressionClient('   ', ['a']).messageKey).toBe(
      'templates.authoring.computeExpressionRequired',
    )
  })

  it('rejects expression over max length', () => {
    const tooLong = 'A'.repeat(2049)
    expect(validateComputeExpressionClient(tooLong, ['a']).messageKey).toBe(
      'templates.authoring.computeExpressionTooLong',
    )
  })

  it('rejects unknown function', () => {
    expect(validateComputeExpressionClient('FOO(${a})', ['a']).valid).toBe(false)
    expect(validateComputeExpressionClient('FOO(${a})', ['a']).messageKey).toBe(
      'templates.authoring.computeExpressionUnknownFunction',
    )
  })

  it('rejects missing reference', () => {
    expect(validateComputeExpressionClient('SUM(${missing})', ['principal']).messageKey).toBe(
      'templates.authoring.computeExpressionMissingReference',
    )
  })

  it('rejects method-call construct', () => {
    expect(validateComputeExpressionClient('${a}.toString()', ['a']).valid).toBe(false)
    expect(validateComputeExpressionClient('obj.method(${a})', ['a']).messageKey).toBe(
      'templates.authoring.computeExpressionIllegalConstruct',
    )
  })

  it('accepts nested whitelist composition', () => {
    expect(
      validateComputeExpressionClient(
        'FORMAT_AMOUNT(SUM(FILTER(${items}, amount, GT, 0)), en-US)',
        ['items'],
      ),
    ).toEqual({ valid: true })
  })
})
