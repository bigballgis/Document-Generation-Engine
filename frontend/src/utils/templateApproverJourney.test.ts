import { describe, expect, it } from 'vitest'
import {
  isAwaitingApproverDecision,
  resolveTemplateApproverDashboardJourneyIndex,
  resolveTemplateApproverJourneyIndex,
  shouldShowTemplateApproverJourney,
  templateApproverStepCtaKey,
} from '@/utils/templateApproverJourney'
import type {
  TemplateApproverApprovalWorkItem,
  TemplateApproverDashboardTemplate,
} from '@/utils/templateApproverJourney'

function templateSummary(
  overrides: Partial<TemplateApproverDashboardTemplate> &
    Pick<TemplateApproverDashboardTemplate, 'id' | 'lifecycleStatus'>,
): TemplateApproverDashboardTemplate {
  return {
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Retail letter',
    releaseVersion: null,
    releaseVersionCount: 0,
    masterId: 'master-1',
    updatedBy: '10000006',
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

function approvalWorkItem(
  overrides: Partial<TemplateApproverApprovalWorkItem> &
    Pick<TemplateApproverApprovalWorkItem, 'templateId'>,
): TemplateApproverApprovalWorkItem {
  return {
    createdAt: '2026-06-25T10:00:00Z',
    ...overrides,
  }
}

describe('isAwaitingApproverDecision', () => {
  it('is true only for APPROVAL with PENDING_DECISION', () => {
    expect(
      isAwaitingApproverDecision({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
      }),
    ).toBe(true)
    expect(
      isAwaitingApproverDecision({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_SUBMIT',
      }),
    ).toBe(false)
    expect(isAwaitingApproverDecision({ lifecycleStatus: 'TESTING' })).toBe(false)
  })
})

describe('resolveTemplateApproverJourneyIndex', () => {
  it('maps non-APPROVAL lifecycle to null', () => {
    expect(
      resolveTemplateApproverJourneyIndex({
        lifecycleStatus: 'TESTING',
      }),
    ).toEqual({ currentStepIndex: null })
  })

  it('maps APPROVAL PENDING_SUBMIT to null', () => {
    expect(
      resolveTemplateApproverJourneyIndex({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_SUBMIT',
      }),
    ).toEqual({ currentStepIndex: null })
  })

  it('maps PENDING_DECISION without review flags to reviewRequest step 0', () => {
    expect(
      resolveTemplateApproverJourneyIndex({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
      }),
    ).toEqual({ currentStepIndex: 0, activeStepId: 'reviewRequest' })
  })

  it('maps submission reviewed to reviewSubmission step 1', () => {
    expect(
      resolveTemplateApproverJourneyIndex({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
        submissionReviewedConfirmed: true,
      }),
    ).toEqual({ currentStepIndex: 1, activeStepId: 'reviewSubmission' })
  })

  it('maps key evidence viewed to recordDecision step 2', () => {
    expect(
      resolveTemplateApproverJourneyIndex({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
        submissionReviewedConfirmed: true,
        keyEvidenceViewedConfirmed: true,
      }),
    ).toEqual({ currentStepIndex: 2, activeStepId: 'recordDecision' })
  })
})

describe('resolveTemplateApproverDashboardJourneyIndex', () => {
  it('maps empty queues to null with empty guidance', () => {
    expect(resolveTemplateApproverDashboardJourneyIndex([], [])).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.GROUP_ADMIN.compliance.empty.guidance',
    })
  })

  it('ignores APPROVAL templates with PENDING_SUBMIT only', () => {
    expect(
      resolveTemplateApproverDashboardJourneyIndex(
        [
          templateSummary({
            id: 'tpl-submit',
            lifecycleStatus: 'APPROVAL',
            approvalSubState: 'PENDING_SUBMIT',
          }),
        ],
        [],
      ),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.GROUP_ADMIN.compliance.empty.guidance',
    })
  })

  it('prioritizes newest OPEN APPROVAL work item over pending-decision templates', () => {
    const resolution = resolveTemplateApproverDashboardJourneyIndex(
      [
        templateSummary({
          id: 'tpl-pending',
          lifecycleStatus: 'APPROVAL',
          approvalSubState: 'PENDING_DECISION',
          updatedAt: '2026-06-27T10:00:00Z',
        }),
      ],
      [
        approvalWorkItem({
          templateId: 'tpl-work-item',
          createdAt: '2026-06-28T10:00:00Z',
        }),
      ],
    )
    expect(resolution).toMatchObject({
      currentStepIndex: 0,
      activeStepId: 'reviewRequest',
      targetTemplateId: 'tpl-work-item',
    })
  })

  it('falls back to newest PENDING_DECISION template when no work items', () => {
    const resolution = resolveTemplateApproverDashboardJourneyIndex(
      [
        templateSummary({
          id: 'older',
          lifecycleStatus: 'APPROVAL',
          approvalSubState: 'PENDING_DECISION',
          updatedAt: '2026-06-20T10:00:00Z',
        }),
        templateSummary({
          id: 'newer',
          lifecycleStatus: 'APPROVAL',
          approvalSubState: 'PENDING_DECISION',
          updatedAt: '2026-06-29T10:00:00Z',
        }),
      ],
      [],
    )
    expect(resolution).toMatchObject({
      currentStepIndex: 1,
      activeStepId: 'reviewSubmission',
      targetTemplateId: 'newer',
    })
  })
})

describe('templateApproverStepCtaKey', () => {
  it('builds stable CTA keys under journey.roles.GROUP_ADMIN.compliance.steps', () => {
    expect(templateApproverStepCtaKey('reviewRequest')).toBe(
      'journey.roles.GROUP_ADMIN.compliance.steps.reviewRequest.cta',
    )
  })
})

describe('shouldShowTemplateApproverJourney', () => {
  it('returns true when decideApprovals capability is granted', () => {
    expect(shouldShowTemplateApproverJourney({ decideApprovals: true })).toBe(true)
  })

  it('returns false without decideApprovals', () => {
    expect(shouldShowTemplateApproverJourney({ decideApprovals: false })).toBe(false)
  })
})
