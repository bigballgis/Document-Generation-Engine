import { describe, expect, it } from 'vitest'
import {
  auditAdminStepCtaKey,
  hasActiveAuditFilters,
  hasLoadedAuditEvents,
  resolveAuditAdminJourneyIndex,
  shouldShowAuditAdminJourney,
} from '@/utils/auditAdminJourney'

describe('hasActiveAuditFilters', () => {
  it('is false when all filter fields are empty', () => {
    expect(hasActiveAuditFilters({})).toBe(false)
    expect(hasActiveAuditFilters({ eventType: '  ', templateId: '' })).toBe(false)
  })

  it('is true when any filter field is set', () => {
    expect(hasActiveAuditFilters({ eventType: 'PUBLISH' })).toBe(true)
    expect(hasActiveAuditFilters({ eventAtFrom: '2026-06-01T00:00:00Z' })).toBe(true)
    expect(hasActiveAuditFilters({ templateId: 'tpl-1' })).toBe(true)
  })
})

describe('hasLoadedAuditEvents', () => {
  it('is true when management or lifecycle events are present', () => {
    expect(hasLoadedAuditEvents({ managementEventCount: 1 })).toBe(true)
    expect(hasLoadedAuditEvents({ lifecycleEventCount: 2 })).toBe(true)
    expect(hasLoadedAuditEvents({})).toBe(false)
  })
})

describe('resolveAuditAdminJourneyIndex', () => {
  it('maps idle state to openActivityLog with empty guidance', () => {
    expect(resolveAuditAdminJourneyIndex({})).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.AUDIT_ADMIN.empty.guidance',
      activeStepId: 'openActivityLog',
    })
  })

  it('maps active filters to searchAndFilter when no rows are loaded', () => {
    expect(
      resolveAuditAdminJourneyIndex({
        filters: { eventType: 'PUBLISH' },
        managementEventCount: 0,
      }),
    ).toEqual({
      currentStepIndex: 1,
      activeStepId: 'searchAndFilter',
    })
  })

  it('maps loaded events to reviewEntries', () => {
    expect(
      resolveAuditAdminJourneyIndex({
        managementEventCount: 3,
      }),
    ).toEqual({
      currentStepIndex: 2,
      activeStepId: 'reviewEntries',
    })
  })

  it('prioritizes export in progress over loaded events', () => {
    expect(
      resolveAuditAdminJourneyIndex({
        managementEventCount: 5,
        exportInProgress: true,
      }),
    ).toEqual({
      currentStepIndex: 3,
      activeStepId: 'exportRecords',
    })
  })

  it('maps export just completed to exportRecords', () => {
    expect(
      resolveAuditAdminJourneyIndex({
        exportJustCompleted: true,
      }),
    ).toEqual({
      currentStepIndex: 3,
      activeStepId: 'exportRecords',
    })
  })
})

describe('auditAdminStepCtaKey', () => {
  it('builds stable CTA keys under journey.roles.AUDIT_ADMIN.steps', () => {
    expect(auditAdminStepCtaKey('exportRecords')).toBe(
      'journey.roles.AUDIT_ADMIN.steps.exportRecords.cta',
    )
  })
})

describe('shouldShowAuditAdminJourney', () => {
  it('returns true when AUDIT_ADMIN role is present', () => {
    expect(shouldShowAuditAdminJourney({ roles: ['AUDIT_ADMIN'] })).toBe(true)
  })

  it('returns false without AUDIT_ADMIN role', () => {
    expect(shouldShowAuditAdminJourney({ roles: ['GLOBAL_ADMIN'] })).toBe(false)
  })
})
