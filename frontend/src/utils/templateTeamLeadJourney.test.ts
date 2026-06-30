import { describe, expect, it } from 'vitest'
import {
  isPendingReleaseTemplate,
  isPendingReviewMaster,
  resolveTemplateTeamLeadDashboardJourneyIndex,
  resolveTemplateTeamLeadJourneyIndex,
  shouldShowTemplateTeamLeadJourney,
  templateTeamLeadStepCtaKey,
} from '@/utils/templateTeamLeadJourney'
import type {
  TemplateTeamLeadDashboardMaster,
  TemplateTeamLeadDashboardTemplate,
  TemplateTeamLeadPendingReleaseWorkItem,
} from '@/utils/templateTeamLeadJourney'

function templateSummary(
  overrides: Partial<TemplateTeamLeadDashboardTemplate> &
    Pick<TemplateTeamLeadDashboardTemplate, 'id' | 'lifecycleStatus'>,
): TemplateTeamLeadDashboardTemplate {
  return {
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Retail letter',
    releaseVersion: null,
    releaseVersionCount: 0,
    masterId: 'master-1',
    updatedBy: '10000002',
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

function masterSummary(
  overrides: Partial<TemplateTeamLeadDashboardMaster> &
    Pick<TemplateTeamLeadDashboardMaster, 'id' | 'status'>,
): TemplateTeamLeadDashboardMaster {
  return {
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

function pendingReleaseWorkItem(
  overrides: Partial<TemplateTeamLeadPendingReleaseWorkItem> &
    Pick<TemplateTeamLeadPendingReleaseWorkItem, 'templateId'>,
): TemplateTeamLeadPendingReleaseWorkItem {
  return {
    createdAt: '2026-06-25T10:00:00Z',
    ...overrides,
  }
}

describe('isPendingReviewMaster', () => {
  it('is true only for PENDING_REVIEW status', () => {
    expect(isPendingReviewMaster({ status: 'PENDING_REVIEW' })).toBe(true)
    expect(isPendingReviewMaster({ status: 'DRAFT' })).toBe(false)
    expect(isPendingReviewMaster({ status: 'APPROVED' })).toBe(false)
  })
})

describe('isPendingReleaseTemplate', () => {
  it('is true only for PENDING_RELEASE lifecycle', () => {
    expect(isPendingReleaseTemplate({ lifecycleStatus: 'PENDING_RELEASE' })).toBe(true)
    expect(isPendingReleaseTemplate({ lifecycleStatus: 'APPROVAL' })).toBe(false)
  })
})

describe('resolveTemplateTeamLeadJourneyIndex', () => {
  it('maps non-PENDING_RELEASE lifecycle to null', () => {
    expect(
      resolveTemplateTeamLeadJourneyIndex({
        lifecycleStatus: 'APPROVAL',
      }),
    ).toEqual({ currentStepIndex: null })
  })

  it('maps PENDING_RELEASE without review flags to reviewGoLiveRequest step 1', () => {
    expect(
      resolveTemplateTeamLeadJourneyIndex({
        lifecycleStatus: 'PENDING_RELEASE',
      }),
    ).toEqual({ currentStepIndex: 1, activeStepId: 'reviewGoLiveRequest' })
  })

  it('maps go-live request reviewed to runPreReleaseChecks step 2', () => {
    expect(
      resolveTemplateTeamLeadJourneyIndex({
        lifecycleStatus: 'PENDING_RELEASE',
        goLiveRequestReviewedConfirmed: true,
      }),
    ).toEqual({ currentStepIndex: 2, activeStepId: 'runPreReleaseChecks' })
  })

  it('maps pre-release checks viewed to confirmGoLive step 3', () => {
    expect(
      resolveTemplateTeamLeadJourneyIndex({
        lifecycleStatus: 'PENDING_RELEASE',
        goLiveRequestReviewedConfirmed: true,
        preReleaseChecksViewed: true,
      }),
    ).toEqual({ currentStepIndex: 3, activeStepId: 'confirmGoLive' })
  })

  it('maps publish gate ready to confirmGoLive step 3', () => {
    expect(
      resolveTemplateTeamLeadJourneyIndex({
        lifecycleStatus: 'PENDING_RELEASE',
        publishGateReady: true,
      }),
    ).toEqual({ currentStepIndex: 3, activeStepId: 'confirmGoLive' })
  })
})

describe('resolveTemplateTeamLeadDashboardJourneyIndex', () => {
  it('maps empty queues to null with empty guidance', () => {
    expect(resolveTemplateTeamLeadDashboardJourneyIndex([], [], [])).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.GROUP_ADMIN.empty.guidance',
    })
  })

  it('prioritizes PENDING_REVIEW master over pending release work items', () => {
    const resolution = resolveTemplateTeamLeadDashboardJourneyIndex(
      [
        masterSummary({
          id: 'master-pending',
          status: 'PENDING_REVIEW',
          updatedAt: '2026-06-28T10:00:00Z',
        }),
      ],
      [
        templateSummary({
          id: 'tpl-pending',
          lifecycleStatus: 'PENDING_RELEASE',
        }),
      ],
      [
        pendingReleaseWorkItem({
          templateId: 'tpl-work-item',
          createdAt: '2026-06-29T10:00:00Z',
        }),
      ],
    )
    expect(resolution).toMatchObject({
      currentStepIndex: 0,
      activeStepId: 'reviewLetterhead',
      targetMasterId: 'master-pending',
    })
  })

  it('prioritizes newest OPEN PENDING_RELEASE work item over pending-release templates', () => {
    const resolution = resolveTemplateTeamLeadDashboardJourneyIndex(
      [],
      [
        templateSummary({
          id: 'tpl-pending',
          lifecycleStatus: 'PENDING_RELEASE',
          updatedAt: '2026-06-27T10:00:00Z',
        }),
      ],
      [
        pendingReleaseWorkItem({
          templateId: 'tpl-work-item',
          createdAt: '2026-06-28T10:00:00Z',
        }),
      ],
    )
    expect(resolution).toMatchObject({
      currentStepIndex: 1,
      activeStepId: 'reviewGoLiveRequest',
      targetTemplateId: 'tpl-work-item',
    })
  })

  it('falls back to newest PENDING_RELEASE template when no work items', () => {
    const resolution = resolveTemplateTeamLeadDashboardJourneyIndex(
      [],
      [
        templateSummary({
          id: 'older',
          lifecycleStatus: 'PENDING_RELEASE',
          updatedAt: '2026-06-20T10:00:00Z',
        }),
        templateSummary({
          id: 'newer',
          lifecycleStatus: 'PENDING_RELEASE',
          updatedAt: '2026-06-29T10:00:00Z',
        }),
      ],
      [],
    )
    expect(resolution).toMatchObject({
      currentStepIndex: 2,
      activeStepId: 'runPreReleaseChecks',
      targetTemplateId: 'newer',
    })
  })
})

describe('templateTeamLeadStepCtaKey', () => {
  it('builds stable CTA keys under journey.roles.GROUP_ADMIN.steps', () => {
    expect(templateTeamLeadStepCtaKey('reviewGoLiveRequest')).toBe(
      'journey.roles.GROUP_ADMIN.steps.reviewGoLiveRequest.cta',
    )
  })
})

describe('shouldShowTemplateTeamLeadJourney', () => {
  it('returns true when publishTemplates capability is granted', () => {
    expect(
      shouldShowTemplateTeamLeadJourney({ publishTemplates: true, reviewMasters: false }),
    ).toBe(true)
  })

  it('returns true when reviewMasters capability is granted', () => {
    expect(
      shouldShowTemplateTeamLeadJourney({ publishTemplates: false, reviewMasters: true }),
    ).toBe(true)
  })

  it('returns false without publishTemplates or reviewMasters', () => {
    expect(
      shouldShowTemplateTeamLeadJourney({ publishTemplates: false, reviewMasters: false }),
    ).toBe(false)
  })
})
