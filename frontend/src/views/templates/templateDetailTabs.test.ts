import { describe, expect, it } from 'vitest'
import { resolveTemplateDetailTab, TEMPLATE_DETAIL_TABS } from '@/views/templates/templateDetailTabs'

describe('templateDetailTabs', () => {
  it('defaults unknown query values to releaseVersions', () => {
    expect(resolveTemplateDetailTab(undefined)).toBe('releaseVersions')
    expect(resolveTemplateDetailTab('invalid')).toBe('releaseVersions')
  })

  it('accepts every supported detail tab', () => {
    for (const tab of TEMPLATE_DETAIL_TABS) {
      expect(resolveTemplateDetailTab(tab)).toBe(tab)
    }
  })
})
