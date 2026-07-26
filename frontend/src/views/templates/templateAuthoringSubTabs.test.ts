import { describe, expect, it } from 'vitest'
import {
  DEFAULT_TEMPLATE_AUTHORING_SUB_TAB,
  TEMPLATE_AUTHORING_SUB_TABS,
  resolveTemplateAuthoringSubTab,
  templateAuthoringSubTabLabelKey,
} from '@/views/templates/templateAuthoringSubTabs'

describe('templateAuthoringSubTabs', () => {
  it('orders sub-tabs without a standalone rules tab', () => {
    expect(TEMPLATE_AUTHORING_SUB_TABS).toEqual([
      'variables',
      'contentModules',
      'bindings',
    ])
  })

  it('BDD-CE-U16-APC-001: defaults unknown / missing query values to bindings', () => {
    expect(resolveTemplateAuthoringSubTab('rules')).toBe(DEFAULT_TEMPLATE_AUTHORING_SUB_TAB)
    expect(resolveTemplateAuthoringSubTab(undefined)).toBe('bindings')
    expect(DEFAULT_TEMPLATE_AUTHORING_SUB_TAB).toBe('bindings')
  })

  it('resolves label keys for each sub-tab', () => {
    for (const tab of TEMPLATE_AUTHORING_SUB_TABS) {
      expect(templateAuthoringSubTabLabelKey(tab)).toMatch(/^templates\.authoring\.subTabs\./)
    }
  })
})
