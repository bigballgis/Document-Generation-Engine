import { describe, expect, it } from 'vitest'
import {
  detectDollarBracePrefix,
  filterVariableKeysForAutocomplete,
  insertVariableReference,
} from '@/utils/conditionExpressionAutocomplete'

describe('conditionExpressionAutocomplete', () => {
  it('filters keys by prefix', () => {
    expect(
      filterVariableKeysForAutocomplete(['borrowerLegalName', 'showNotice', 'amount'], 'bo'),
    ).toEqual(['borrowerLegalName'])
  })

  it('detects ${prefix at caret', () => {
    expect(detectDollarBracePrefix('${bor', 5)).toEqual({ start: 0, prefix: 'bor' })
    expect(detectDollarBracePrefix('x == ${sho', 10)).toEqual({ start: 5, prefix: 'sho' })
    expect(detectDollarBracePrefix('plain', 5)).toBeNull()
  })

  it('inserts ${variableKey} at caret', () => {
    expect(insertVariableReference('', 0, 'borrowerLegalName')).toEqual({
      nextValue: '${borrowerLegalName}',
      nextCaret: '${borrowerLegalName}'.length,
    })
  })

  it('replaces active ${prefix token when inserting a suggestion', () => {
    expect(
      insertVariableReference('${bor', 5, 'borrowerLegalName', { replaceDollarBracePrefix: true }),
    ).toEqual({
      nextValue: '${borrowerLegalName}',
      nextCaret: '${borrowerLegalName}'.length,
    })
  })
})
