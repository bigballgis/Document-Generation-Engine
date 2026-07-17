import { describe, expect, it } from 'vitest'
import { resolveHubSecondaryTab } from '@/views/templates/useTemplatePackageHubRouting'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'

describe('useTemplatePackageHubRouting', () => {
  it('BDD-CE-U19-DRV-001 — accepts dependencies hub secondary tab', () => {
    expect(resolveHubSecondaryTab('dependencies')).toBe('dependencies')
    expect(resolveHubSecondaryTab('overview')).toBe('overview')
    expect(resolveHubSecondaryTab('apiAccess')).toBe('apiAccess')
  })

  it('rejects unknown hub tab values', () => {
    expect(resolveHubSecondaryTab('authoring')).toBeUndefined()
    expect(resolveHubSecondaryTab('invalid')).toBeUndefined()
    expect(resolveHubSecondaryTab(undefined)).toBeUndefined()
  })

  it('exposes English-first Dependencies tab labels', () => {
    expect(en.templates.detail.tabs.dependencies).toBe('Dependencies')
    expect(zhCN.templates.detail.tabs.dependencies).toBe('依赖')
  })
})
