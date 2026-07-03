import { describe, expect, it } from 'vitest'
import {
  globalAdminStepCtaKey,
  isPendingReviewMaster,
  resolveGlobalAdminDashboardJourneyIndex,
  shouldShowGlobalAdminJourney,
} from '@/utils/globalAdminJourney'
import type {
  GlobalAdminCollaborationWorkItem,
  GlobalAdminDashboardMaster,
  GlobalAdminDashboardTemplate,
} from '@/utils/globalAdminJourney'

function masterSummary(
  overrides: Partial<GlobalAdminDashboardMaster> &
    Pick<GlobalAdminDashboardMaster, 'id' | 'status'>,
): GlobalAdminDashboardMaster {
  return {
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

function templateSummary(
  overrides: Partial<GlobalAdminDashboardTemplate> &
    Pick<GlobalAdminDashboardTemplate, 'id'>,
): GlobalAdminDashboardTemplate {
  return { ...overrides }
}

function workItem(
  overrides: Partial<GlobalAdminCollaborationWorkItem> &
    Pick<GlobalAdminCollaborationWorkItem, 'queue'>,
): GlobalAdminCollaborationWorkItem {
  return {
    createdAt: '2026-06-25T10:00:00Z',
    ...overrides,
  }
}

describe('isPendingReviewMaster', () => {
  it('is true only for PENDING_REVIEW status', () => {
    expect(isPendingReviewMaster({ status: 'PENDING_REVIEW' })).toBe(true)
    expect(isPendingReviewMaster({ status: 'DRAFT' })).toBe(false)
  })
})

describe('resolveGlobalAdminDashboardJourneyIndex', () => {
  it('maps empty queues to null with empty guidance at reviewOverview', () => {
    expect(resolveGlobalAdminDashboardJourneyIndex([], [], [])).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.GLOBAL_ADMIN.empty.guidance',
      activeStepId: 'reviewOverview',
    })
  })

  it('prioritizes OPEN ESCALATION work items to monitorOverdue step', () => {
    const resolution = resolveGlobalAdminDashboardJourneyIndex(
      [],
      [],
      [
        workItem({ queue: 'ESCALATION', createdAt: '2026-06-29T10:00:00Z' }),
        workItem({ queue: 'APPROVAL', createdAt: '2026-06-28T10:00:00Z' }),
      ],
    )
    expect(resolution).toEqual({
      currentStepIndex: 4,
      activeStepId: 'monitorOverdue',
    })
  })

  it('maps inherited PENDING_REVIEW master work to reviewOverview without highlighting a step', () => {
    const resolution = resolveGlobalAdminDashboardJourneyIndex(
      [
        masterSummary({
          id: 'master-pending',
          status: 'PENDING_REVIEW',
        }),
      ],
      [],
      [],
    )
    expect(resolution).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.GLOBAL_ADMIN.empty.guidance',
      activeStepId: 'reviewOverview',
    })
  })

  it('maps inherited PENDING_RELEASE work items to reviewOverview without highlighting a step', () => {
    const resolution = resolveGlobalAdminDashboardJourneyIndex(
      [],
      [],
      [workItem({ queue: 'PENDING_RELEASE' })],
    )
    expect(resolution).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.GLOBAL_ADMIN.empty.guidance',
      activeStepId: 'reviewOverview',
    })
  })

  it('maps multiple open queues to reviewOverview without highlighting a step', () => {
    const resolution = resolveGlobalAdminDashboardJourneyIndex(
      [],
      [],
      [
        workItem({ queue: 'TEST' }),
        workItem({ queue: 'APPROVAL' }),
      ],
    )
    expect(resolution).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.GLOBAL_ADMIN.empty.guidance',
      activeStepId: 'reviewOverview',
    })
  })

  it('prioritizes escalation over inherited and multi-queue signals', () => {
    const resolution = resolveGlobalAdminDashboardJourneyIndex(
      [masterSummary({ id: 'm1', status: 'PENDING_REVIEW' })],
      [],
      [
        workItem({ queue: 'ESCALATION' }),
        workItem({ queue: 'TEST' }),
        workItem({ queue: 'APPROVAL' }),
      ],
    )
    expect(resolution).toEqual({
      currentStepIndex: 4,
      activeStepId: 'monitorOverdue',
    })
  })

  it('highlights setReminderDefaults when idle and config maintenance is allowed', () => {
    const resolution = resolveGlobalAdminDashboardJourneyIndex([], [], [], {
      canMaintainCollaborationTimeoutConfig: true,
    })
    expect(resolution).toEqual({
      currentStepIndex: 3,
      activeStepId: 'setReminderDefaults',
    })
  })

  it('highlights removeTemplates when idle with deletable templates', () => {
    const resolution = resolveGlobalAdminDashboardJourneyIndex(
      [],
      [templateSummary({ id: 'tpl-1' })],
      [],
      { deleteTemplates: true },
    )
    expect(resolution).toEqual({
      currentStepIndex: 2,
      activeStepId: 'removeTemplates',
    })
  })
})

describe('globalAdminStepCtaKey', () => {
  it('builds stable CTA keys under journey.roles.GLOBAL_ADMIN.steps', () => {
    expect(globalAdminStepCtaKey('monitorOverdue')).toBe(
      'journey.roles.GLOBAL_ADMIN.steps.monitorOverdue.cta',
    )
  })
})

describe('shouldShowGlobalAdminJourney', () => {
  it('returns true when GLOBAL_ADMIN role is present', () => {
    expect(shouldShowGlobalAdminJourney({ roles: ['GLOBAL_ADMIN'] })).toBe(true)
  })

  it('returns false without GLOBAL_ADMIN role', () => {
    expect(shouldShowGlobalAdminJourney({ roles: ['GROUP_ADMIN'] })).toBe(false)
  })
})
