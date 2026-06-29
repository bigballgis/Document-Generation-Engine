import { describe, expect, it } from 'vitest'
import { isTemplateExportEligible } from '@/utils/templateExportEligibility'

describe('templateExportEligibility', () => {
  it('allows export for approved or published lifecycle states', () => {
    expect(isTemplateExportEligible('PENDING_RELEASE')).toBe(true)
    expect(isTemplateExportEligible('PUBLISHED')).toBe(true)
    expect(isTemplateExportEligible('STOPPED')).toBe(true)
    expect(isTemplateExportEligible('DEPRECATED')).toBe(true)
  })

  it('blocks export for in-flight draft workflow states', () => {
    expect(isTemplateExportEligible('DRAFT')).toBe(false)
    expect(isTemplateExportEligible('TESTING')).toBe(false)
    expect(isTemplateExportEligible('APPROVAL')).toBe(false)
  })
})
