import { describe, expect, it } from 'vitest'
import { humanizeCamelCase, resolveVariableDisplayName } from '@/utils/variableDisplayName'

describe('humanizeCamelCase', () => {
  it('converts camelCase to title words', () => {
    expect(humanizeCamelCase('borrowerLegalName')).toBe('Borrower Legal Name')
    expect(humanizeCamelCase('cpItemDescription')).toBe('Cp Item Description')
  })
})

describe('resolveVariableDisplayName', () => {
  it('uses description when present', () => {
    expect(
      resolveVariableDisplayName({
        variableKey: 'borrowerLegalName',
        description: 'Borrower legal name',
      }),
    ).toBe('Borrower legal name')
  })
})
