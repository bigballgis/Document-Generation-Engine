import { describe, expect, it } from 'vitest'
import {
  invalidLifecycleDecisionFields,
  isLifecycleDecisionFormValid,
} from '@/utils/templateLifecycleDecisionForm'

describe('templateLifecycleDecisionForm', () => {
  it('requires reason category and impact summary for negative decisions', () => {
    expect(isLifecycleDecisionFormValid({ reasonCategory: '', impactSummary: '' })).toBe(false)
    expect(invalidLifecycleDecisionFields({ reasonCategory: '', impactSummary: '' })).toEqual([
      'reasonCategory',
      'impactSummary',
    ])
  })

  it('accepts trimmed structured opinion fields', () => {
    expect(
      isLifecycleDecisionFormValid({
        reasonCategory: 'BINDING_ISSUE',
        impactSummary: 'Header binding invalid',
        commentSummary: 'Optional note',
      }),
    ).toBe(true)
  })

  it('rejects whitespace-only values', () => {
    expect(
      isLifecycleDecisionFormValid({
        reasonCategory: '   ',
        impactSummary: '   ',
      }),
    ).toBe(false)
  })
})
