import { describe, expect, it } from 'vitest'
import { resolveTemplateDetailTab, TEMPLATE_DETAIL_TABS } from '@/views/templates/templateDetailTabs'

describe('templateDetailTabs', () => {
  it('defaults unknown query values to overview', () => {
    expect(resolveTemplateDetailTab(undefined)).toBe('overview')
    expect(resolveTemplateDetailTab('invalid')).toBe('overview')
  })

  it('accepts every supported detail tab', () => {
    for (const tab of TEMPLATE_DETAIL_TABS) {
      expect(resolveTemplateDetailTab(tab)).toBe(tab)
    }
  })
})
