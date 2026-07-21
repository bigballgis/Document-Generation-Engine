import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en'
import zhCn from '@/i18n/locales/zh-CN'
import {
  auditAdminJourneySteps,
  documentAuthorJourneySteps,
  globalAdminJourneySteps,
  masterDesignerJourneySteps,
  resolveClusterOneJourney,
  resolvePrimaryClusterOneRole,
  templateApproverJourneySteps,
  templateAuthorJourneySteps,
  templateTeamLeadJourneySteps,
  templateTesterJourneySteps,
} from '@/constants/roleJourneyDefinitions'

const FORBIDDEN_L1_NOUNS = [
  'policy',
  'credential',
  'lifecycle',
  'gate',
  'semver',
  'anchor',
  'escalation',
]

describe('roleJourneyDefinitions', () => {
  const i18nEn = createI18n({ legacy: false, locale: 'en', messages: { en } })
  const i18nZh = createI18n({ legacy: false, locale: 'zh-CN', messages: { 'zh-CN': zhCn } })
  const tEn = i18nEn.global.t
  const tZh = i18nZh.global.t

  it('masterDesignerJourneySteps has exactly 4 letterhead steps under DOCUMENT_AUTHOR', () => {
    expect(masterDesignerJourneySteps).toHaveLength(4)
    expect(masterDesignerJourneySteps.map((step) => step.id)).toEqual([
      'upload',
      'placeholders',
      'submitReview',
      'rework',
    ])
    expect(masterDesignerJourneySteps[0].labelKey).toContain('DOCUMENT_AUTHOR.letterhead')
  })

  it('documentAuthorJourneySteps has exactly 6 steps in Spec B order', () => {
    expect(documentAuthorJourneySteps).toHaveLength(6)
    expect(templateAuthorJourneySteps).toBe(documentAuthorJourneySteps)
    expect(documentAuthorJourneySteps.map((step) => step.id)).toEqual([
      'create',
      'design',
      'trialGenerate',
      'submitTest',
      'submitApproval',
      'awaitGoLive',
    ])
  })

  it('templateTesterJourneySteps has exactly 3 steps in Spec B order', () => {
    expect(templateTesterJourneySteps).toHaveLength(3)
    expect(templateTesterJourneySteps.map((step) => step.id)).toEqual([
      'reviewRequest',
      'checkEvidence',
      'recordResult',
    ])
  })

  it('globalAdminJourneySteps has exactly 5 steps in Spec B order', () => {
    expect(globalAdminJourneySteps).toHaveLength(5)
    expect(globalAdminJourneySteps.map((step) => step.id)).toEqual([
      'reviewOverview',
      'manageUsersGroups',
      'removeTemplates',
      'setReminderDefaults',
      'monitorOverdue',
    ])
  })

  it('auditAdminJourneySteps has exactly 5 steps in Spec B order', () => {
    expect(auditAdminJourneySteps).toHaveLength(5)
    expect(auditAdminJourneySteps.map((step) => step.id)).toEqual([
      'openActivityLog',
      'searchAndFilter',
      'reviewEntries',
      'exportRecords',
      'viewOnlyMode',
    ])
  })

  it('templateApproverJourneySteps has exactly 3 compliance steps under GROUP_ADMIN', () => {
    expect(templateApproverJourneySteps).toHaveLength(3)
    expect(templateApproverJourneySteps.map((step) => step.id)).toEqual([
      'reviewRequest',
      'reviewSubmission',
      'recordDecision',
    ])
    expect(templateApproverJourneySteps[0].labelKey).toContain('GROUP_ADMIN.compliance')
  })

  it('templateTeamLeadJourneySteps has exactly 4 steps in Spec B order', () => {
    expect(templateTeamLeadJourneySteps).toHaveLength(4)
    expect(templateTeamLeadJourneySteps.map((step) => step.id)).toEqual([
      'reviewLetterhead',
      'reviewGoLiveRequest',
      'runPreReleaseChecks',
      'confirmGoLive',
    ])
  })

  it('resolves non-empty en and zh-CN strings for every cluster-① step labelKey', () => {
    const allSteps = [
      ...masterDesignerJourneySteps,
      ...documentAuthorJourneySteps,
      ...templateTesterJourneySteps,
    ]
    for (const step of allSteps) {
      expect(tEn(step.labelKey)).not.toBe(step.labelKey)
      expect(tEn(step.labelKey).length).toBeGreaterThan(0)
      expect(tZh(step.labelKey)).not.toBe(step.labelKey)
      expect(tZh(step.labelKey).length).toBeGreaterThan(0)
    }
  })

  it('resolvePrimaryClusterOneRole prefers DOCUMENT_AUTHOR over TEMPLATE_TESTER', () => {
    expect(resolvePrimaryClusterOneRole(['TEMPLATE_TESTER', 'DOCUMENT_AUTHOR'])).toBe(
      'DOCUMENT_AUTHOR',
    )
  })

  it('resolvePrimaryClusterOneRole returns null for cluster-①-excluded roles', () => {
    expect(resolvePrimaryClusterOneRole(['GROUP_ADMIN'])).toBeNull()
    expect(resolvePrimaryClusterOneRole(['AUDIT_ADMIN'])).toBeNull()
    expect(resolvePrimaryClusterOneRole(['GLOBAL_ADMIN'])).toBeNull()
  })

  it('resolveClusterOneJourney returns the matching catalog', () => {
    expect(resolveClusterOneJourney('DOCUMENT_AUTHOR')).toBe(documentAuthorJourneySteps)
    expect(resolveClusterOneJourney('TEMPLATE_TESTER')).toBe(templateTesterJourneySteps)
  })

  it('cluster-① step label en values avoid forbidden L1 nouns', () => {
    const allSteps = [
      ...masterDesignerJourneySteps,
      ...documentAuthorJourneySteps,
      ...templateTesterJourneySteps,
    ]
    for (const step of allSteps) {
      const value = tEn(step.labelKey).toLowerCase()
      for (const noun of FORBIDDEN_L1_NOUNS) {
        expect(value).not.toMatch(new RegExp(`\\b${noun}\\b`))
      }
    }
  })

  it('resolves non-empty en and zh-CN strings for every GLOBAL_ADMIN step labelKey', () => {
    for (const step of globalAdminJourneySteps) {
      expect(tEn(step.labelKey)).not.toBe(step.labelKey)
      expect(tEn(step.labelKey).length).toBeGreaterThan(0)
      expect(tZh(step.labelKey)).not.toBe(step.labelKey)
      expect(tZh(step.labelKey).length).toBeGreaterThan(0)
    }
  })

  it('GLOBAL_ADMIN step label en values avoid forbidden L1 nouns', () => {
    for (const step of globalAdminJourneySteps) {
      const value = tEn(step.labelKey).toLowerCase()
      for (const noun of FORBIDDEN_L1_NOUNS) {
        expect(value).not.toMatch(new RegExp(`\\b${noun}\\b`))
      }
    }
  })

  it('resolves non-empty en and zh-CN strings for every AUDIT_ADMIN step labelKey', () => {
    for (const step of auditAdminJourneySteps) {
      expect(tEn(step.labelKey)).not.toBe(step.labelKey)
      expect(tEn(step.labelKey).length).toBeGreaterThan(0)
      expect(tZh(step.labelKey)).not.toBe(step.labelKey)
      expect(tZh(step.labelKey).length).toBeGreaterThan(0)
    }
  })

  it('AUDIT_ADMIN step label en values avoid forbidden L1 nouns', () => {
    for (const step of auditAdminJourneySteps) {
      const value = tEn(step.labelKey).toLowerCase()
      for (const noun of FORBIDDEN_L1_NOUNS) {
        expect(value).not.toMatch(new RegExp(`\\b${noun}\\b`))
      }
    }
  })
})
