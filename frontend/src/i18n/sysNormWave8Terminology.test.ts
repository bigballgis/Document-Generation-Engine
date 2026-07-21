import { describe, expect, it } from 'vitest'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'

/**
 * BDD-SYS-NORM-W8-007…011 — L1 Letterhead / 母版 terminology contract.
 * Keys stay stable; values must purge bare Master / 主文档 as L1 primary object nouns.
 * L2/L3 technical labels (e.g. Master ID) remain allowed.
 */
describe('SYS-NORM Wave 8 L1 Letterhead / 母版 terminology', () => {
  it('BDD-SYS-NORM-W8-007 — EN L1 primary surfaces use Letterhead', () => {
    expect(en.nav.items.masters).toBe('Letterhead templates')
    expect(en.masters.list.title).toBe('Letterhead templates')
    expect(en.masters.upload.open).toBe('New letterhead package')
    expect(en.nav.behaviorItems.masterReview).toBe('Letterheads to review')
    expect(en.templates.authoringPathGuide.steps.master).toBe('Letterhead')
    expect(en.templates.dependencies.masterRevision.title).toBe('Letterhead revision')
  })

  it('BDD-SYS-NORM-W8-008 — ZH L1 primary surfaces use 母版', () => {
    expect(zhCN.nav.items.masters).toBe('母版文档')
    expect(zhCN.masters.list.title).toBe('母版文档')
    expect(zhCN.masters.upload.open).toBe('新建母版包')
    expect(zhCN.nav.behaviorItems.masterReview).toBe('待审核母版')
    expect(zhCN.templates.authoringPathGuide.steps.master).toBe('母版')
    expect(zhCN.templates.dependencies.masterRevision.title).toBe('母版修订')
  })

  it('BDD-SYS-NORM-W8-009 — L1 EN values do not use bare Master as primary object noun', () => {
    const l1Surfaces = [
      en.nav.items.masters,
      en.masters.list.title,
      en.masters.upload.open,
      en.nav.behaviorItems.masterReview,
      en.templates.authoringPathGuide.steps.master,
      en.templates.dependencies.masterRevision.title,
      en.templates.authoringPathGuide.master.title,
    ]
    for (const value of l1Surfaces) {
      expect(value).not.toMatch(/\bMaster documents?\b/i)
      expect(value).not.toBe('Master')
      expect(value).not.toMatch(/^Master\b/)
    }
    // L2 technical field label remains allowed
    expect(en.templates.detail.masterId).toBe('Master ID')
  })

  it('BDD-SYS-NORM-W8-008 — ZH L1 values do not use 主文档 as primary object noun', () => {
    const l1Surfaces = [
      zhCN.nav.items.masters,
      zhCN.masters.list.title,
      zhCN.masters.upload.open,
      zhCN.nav.behaviorItems.masterReview,
      zhCN.templates.authoringPathGuide.steps.master,
      zhCN.templates.dependencies.masterRevision.title,
    ]
    for (const value of l1Surfaces) {
      expect(value).not.toContain('主文档')
    }
  })
})
