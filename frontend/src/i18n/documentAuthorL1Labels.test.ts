import { describe, expect, it } from 'vitest'
import { MANAGEMENT_ROLE_VALUES } from '@/types/identity'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'

/**
 * BDD-N18-L1-008…010 — DOCUMENT_AUTHOR L1 display labels (ADR-0070 P-Q1).
 * Role ID stays DOCUMENT_AUTHOR; L1 copy locks EN/ZH without interim suffix.
 */
describe('DOCUMENT_AUTHOR L1 labels (BDD-N18-L1-008…010)', () => {
  it('BDD-N18-L1-008 — English L1 is Document author without interim', () => {
    expect(en.identity.roles.DOCUMENT_AUTHOR).toBe('Document author')
    expect(en.identity.roles.DOCUMENT_AUTHOR.toLowerCase()).not.toContain('interim')
  })

  it('BDD-N18-L1-009 — Chinese L1 is 文档作者 without interim', () => {
    expect(zhCN.identity.roles.DOCUMENT_AUTHOR).toBe('文档作者')
    expect(zhCN.identity.roles.DOCUMENT_AUTHOR).not.toContain('interim')
    expect(zhCN.identity.roles.DOCUMENT_AUTHOR).not.toContain('（interim）')
  })

  it('BDD-N18-L1-010 — assignable role code remains DOCUMENT_AUTHOR', () => {
    expect(MANAGEMENT_ROLE_VALUES).toContain('DOCUMENT_AUTHOR')
    expect(MANAGEMENT_ROLE_VALUES.filter((role) => role.includes('AUTHOR'))).toEqual([
      'DOCUMENT_AUTHOR',
    ])
  })
})
