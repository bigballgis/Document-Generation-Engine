import { describe, expect, it } from 'vitest'
import {
  allocateUniqueReferenceKey,
  normalizeModuleCodeToReferenceKey,
  suggestReferenceKey,
} from '@/utils/referenceKeyFromModuleCode'

describe('normalizeModuleCodeToReferenceKey (BEI-C8)', () => {
  it('BDD-BEI-012 maps hyphenated moduleCode to UPPER_SNAKE', () => {
    expect(normalizeModuleCodeToReferenceKey('loan-disclosure')).toBe('LOAN_DISCLOSURE')
    expect(normalizeModuleCodeToReferenceKey('LOAN-DISCLOSURE')).toBe('LOAN_DISCLOSURE')
  })

  it('collapses non-alphanumeric runs and trims underscores', () => {
    expect(normalizeModuleCodeToReferenceKey('  loan--disclosure__v2  ')).toBe('LOAN_DISCLOSURE_V2')
    expect(normalizeModuleCodeToReferenceKey('loan.disclosure/v2')).toBe('LOAN_DISCLOSURE_V2')
  })

  it('BDD-BEI-018 returns empty when nothing alphanumeric remains', () => {
    expect(normalizeModuleCodeToReferenceKey('---')).toBe('')
    expect(normalizeModuleCodeToReferenceKey('___')).toBe('')
    expect(normalizeModuleCodeToReferenceKey('')).toBe('')
  })
})

describe('allocateUniqueReferenceKey (BEI-C9)', () => {
  it('prefers base key when free', () => {
    expect(allocateUniqueReferenceKey('LOAN_DISCLOSURE', [])).toBe('LOAN_DISCLOSURE')
    expect(allocateUniqueReferenceKey('LOAN_DISCLOSURE', ['OTHER_KEY'])).toBe('LOAN_DISCLOSURE')
  })

  it('BDD-BEI-013 appends _2 when base is taken', () => {
    expect(allocateUniqueReferenceKey('LOAN_DISCLOSURE', ['LOAN_DISCLOSURE'])).toBe(
      'LOAN_DISCLOSURE_2',
    )
  })

  it('BDD-BEI-014 appends _3 when base and _2 are taken', () => {
    expect(
      allocateUniqueReferenceKey('LOAN_DISCLOSURE', ['LOAN_DISCLOSURE', 'LOAN_DISCLOSURE_2']),
    ).toBe('LOAN_DISCLOSURE_3')
  })

  it('keeps incrementing until free', () => {
    expect(
      allocateUniqueReferenceKey('LOAN_DISCLOSURE', [
        'LOAN_DISCLOSURE',
        'LOAN_DISCLOSURE_2',
        'LOAN_DISCLOSURE_3',
        'LOAN_DISCLOSURE_4',
      ]),
    ).toBe('LOAN_DISCLOSURE_5')
  })

  it('returns empty when base is empty', () => {
    expect(allocateUniqueReferenceKey('', ['LOAN_DISCLOSURE'])).toBe('')
  })
})

describe('suggestReferenceKey', () => {
  it('combines normalize + conflict allocation', () => {
    expect(suggestReferenceKey('loan-disclosure', ['LOAN_DISCLOSURE'])).toBe('LOAN_DISCLOSURE_2')
  })

  it('returns empty when normalize yields empty', () => {
    expect(suggestReferenceKey('***', ['ANY'])).toBe('')
  })
})
