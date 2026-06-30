import { describe, expect, it } from 'vitest'
import {
  DEFAULT_TEMPLATE_DETAIL_TAB,
  normalizeTemplateDetailQuery,
  resolveTemplateDetailTab,
  resolveTemplateDetailTabFromQuery,
  TEMPLATE_DETAIL_TABS,
  TEMPLATE_DETAIL_TAB_LABEL_KEYS,
  templateDetailTabLabelKey,
} from '@/views/templates/templateDetailTabs'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'

describe('templateDetailTabs', () => {
  it('defaults unknown query values to overview', () => {
    expect(DEFAULT_TEMPLATE_DETAIL_TAB).toBe('overview')
    expect(resolveTemplateDetailTab(undefined)).toBe('overview')
    expect(resolveTemplateDetailTab('invalid')).toBe('overview')
  })

  it('accepts every supported detail tab', () => {
    for (const tab of TEMPLATE_DETAIL_TABS) {
      expect(resolveTemplateDetailTab(tab)).toBe(tab)
    }
  })

  it('resolves lifecycle from focus deep-link query', () => {
    expect(resolveTemplateDetailTabFromQuery({ focus: 'lifecycle' })).toBe('lifecycle')
    expect(resolveTemplateDetailTabFromQuery({ focus: 'lifecycle', tab: 'overview' })).toBe('lifecycle')
  })

  it('resolves tab query when focus is absent', () => {
    expect(resolveTemplateDetailTabFromQuery({ tab: 'authoring' })).toBe('authoring')
    expect(resolveTemplateDetailTabFromQuery({})).toBe('overview')
  })

  it('normalizes focus=lifecycle to tab=lifecycle and removes focus', () => {
    const normalized = normalizeTemplateDetailQuery({
      focus: 'lifecycle',
      queue: 'REMEDIATION',
    })
    expect(normalized).toEqual({
      query: { queue: 'REMEDIATION', tab: 'lifecycle' },
      tab: 'lifecycle',
    })
  })

  it('does not normalize queries without lifecycle focus', () => {
    expect(normalizeTemplateDetailQuery({ tab: 'overview' })).toBeNull()
    expect(normalizeTemplateDetailQuery({ focus: 'authoring' })).toBeNull()
  })

  it('maps every tab id to a stable label key', () => {
    for (const tab of TEMPLATE_DETAIL_TABS) {
      expect(templateDetailTabLabelKey(tab)).toBe(TEMPLATE_DETAIL_TAB_LABEL_KEYS[tab])
    }
  })

  it('uses business-friendly tab labels in English and zh-CN', () => {
    expect(en.templates.detail.tabs.releaseVersions).toBe('Published versions')
    expect(en.templates.detail.tabs.lifecycle).toBe('Workflow status')
    expect(en.templates.detail.tabs.apiAccess).toBe('External access')
    expect(zhCN.templates.detail.tabs.releaseVersions).toBe('已发布版本')
    expect(zhCN.templates.detail.tabs.lifecycle).toBe('流程状态')
    expect(zhCN.templates.detail.tabs.apiAccess).toBe('对外服务')
  })
})
