import { describe, expect, it } from 'vitest'
import {
  resolveTemplateDetailTab,
  TEMPLATE_DETAIL_TABS,
  TEMPLATE_DETAIL_TAB_LABEL_KEYS,
  templateDetailTabLabelKey,
} from '@/views/templates/templateDetailTabs'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'

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
