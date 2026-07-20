import { describe, expect, it } from 'vitest'
import { resolveHubSecondaryTab } from '@/views/templates/useTemplatePackageHubRouting'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'

describe('useTemplatePackageHubRouting (Wave 2 hub IA)', () => {
  it('BDD-SYS-NORM-W2-002 — hub secondary tabs are no longer valid surfaces', () => {
    expect(resolveHubSecondaryTab('dependencies')).toBeUndefined()
    expect(resolveHubSecondaryTab('overview')).toBeUndefined()
    expect(resolveHubSecondaryTab('apiAccess')).toBeUndefined()
  })

  it('keeps English-first Dependencies labels for per-version surfaces', () => {
    expect(en.templates.detail.tabs.dependencies).toBe('Dependencies')
    expect(zhCN.templates.detail.tabs.dependencies).toBe('依赖')
  })
})
