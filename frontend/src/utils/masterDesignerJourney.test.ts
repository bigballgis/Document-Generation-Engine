import { describe, expect, it } from 'vitest'
import {
  isMasterReworkState,
  resolveMasterDesignerDashboardJourneyIndex,
  resolveMasterDesignerJourneyIndex,
  shouldShowMasterDesignerJourney,
} from '@/utils/masterDesignerJourney'
import type { MasterReviewRecord } from '@/types/master'
import type { MasterDesignerDashboardMaster } from '@/utils/masterDesignerJourney'

function rejectedHistory(): MasterReviewRecord[] {
  return [
    {
      action: 'REJECTED',
      decision: 'REJECTED',
      changeSummary: null,
      commentSummary: 'Needs fixes',
      actorUsername: '10000001',
      createdAt: '2026-06-25T10:00:00Z',
    },
  ]
}

function masterSummary(
  overrides: Partial<MasterDesignerDashboardMaster> &
    Pick<MasterDesignerDashboardMaster, 'id' | 'status'>,
): MasterDesignerDashboardMaster {
  return {
    groupCode: 'RETAIL',
    name: 'Letterhead',
    originalFilename: 'letterhead.docx',
    anchorCount: 2,
    updatedBy: '10000005',
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

describe('resolveMasterDesignerJourneyIndex (§12.5 entity mapping)', () => {
  it('maps missing upload to step 0', () => {
    expect(
      resolveMasterDesignerJourneyIndex({
        status: 'DRAFT',
        originalFilename: '',
      }),
    ).toEqual({ currentStepIndex: 0, activeStepId: 'upload' })
  })

  it('maps file without placeholders to step 1', () => {
    expect(
      resolveMasterDesignerJourneyIndex({
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 0,
      }),
    ).toEqual({ currentStepIndex: 1, activeStepId: 'placeholders' })
  })

  it('maps rework via REJECTED status to step 3', () => {
    expect(
      resolveMasterDesignerJourneyIndex({
        status: 'REJECTED',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
      }),
    ).toEqual({ currentStepIndex: 3, activeStepId: 'rework' })
  })

  it('maps DRAFT with latest rejected review to step 3 (not step 2)', () => {
    expect(
      resolveMasterDesignerJourneyIndex({
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
        reviewHistory: rejectedHistory(),
      }),
    ).toEqual({ currentStepIndex: 3, activeStepId: 'rework' })
  })

  it('maps ready DRAFT to submit step 2', () => {
    expect(
      resolveMasterDesignerJourneyIndex({
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 3,
        reviewHistory: [],
      }),
    ).toEqual({ currentStepIndex: 2, activeStepId: 'submitReview' })
  })

  it('maps PENDING_REVIEW to null with waiting guidance', () => {
    expect(
      resolveMasterDesignerJourneyIndex({
        status: 'PENDING_REVIEW',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
      }),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.MASTER_DESIGNER.waitingReview.guidance',
    })
  })

  it('maps APPROVED to null with complete guidance', () => {
    expect(
      resolveMasterDesignerJourneyIndex({
        status: 'APPROVED',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
      }),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.MASTER_DESIGNER.complete.guidance',
    })
  })

  it('uses anchorsLength when anchorCount is absent on revision detail', () => {
    expect(
      resolveMasterDesignerJourneyIndex({
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorsLength: 1,
      }),
    ).toEqual({ currentStepIndex: 2, activeStepId: 'submitReview' })
  })
})

describe('isMasterReworkState', () => {
  it('detects REJECTED enum status', () => {
    expect(isMasterReworkState('REJECTED')).toBe(true)
  })

  it('detects DRAFT with rejected decision in history', () => {
    expect(isMasterReworkState('DRAFT', rejectedHistory())).toBe(true)
  })

  it('returns false for clean DRAFT', () => {
    expect(isMasterReworkState('DRAFT', [])).toBe(false)
  })
})

describe('resolveMasterDesignerDashboardJourneyIndex (§12.5 dashboard mapping)', () => {
  it('maps empty catalog to step 0', () => {
    expect(resolveMasterDesignerDashboardJourneyIndex([])).toEqual({
      currentStepIndex: 0,
      activeStepId: 'upload',
      guidanceKey: undefined,
    })
  })

  it('prioritizes rework over ready-to-submit drafts', () => {
    const resolution = resolveMasterDesignerDashboardJourneyIndex([
      masterSummary({
        id: 'ready',
        status: 'DRAFT',
        updatedAt: '2026-06-27T10:00:00Z',
      }),
      masterSummary({
        id: 'rework',
        status: 'DRAFT',
        reviewHistory: rejectedHistory(),
        updatedAt: '2026-06-26T10:00:00Z',
      }),
    ])
    expect(resolution.currentStepIndex).toBe(3)
    expect(resolution.activeStepId).toBe('rework')
    expect(resolution.targetMasterId).toBe('rework')
  })

  it('maps single ready DRAFT to step 2', () => {
    expect(
      resolveMasterDesignerDashboardJourneyIndex([
        masterSummary({ id: 'm1', status: 'DRAFT' }),
      ]),
    ).toMatchObject({ currentStepIndex: 2, activeStepId: 'submitReview' })
  })

  it('maps file without anchors to step 1', () => {
    expect(
      resolveMasterDesignerDashboardJourneyIndex([
        masterSummary({ id: 'm1', status: 'DRAFT', anchorCount: 0 }),
      ]),
    ).toMatchObject({ currentStepIndex: 1, activeStepId: 'placeholders' })
  })

  it('maps in-progress master without file to step 0', () => {
    expect(
      resolveMasterDesignerDashboardJourneyIndex([
        masterSummary({ id: 'm1', status: 'DRAFT', originalFilename: '', anchorCount: 0 }),
      ]),
    ).toMatchObject({ currentStepIndex: 0, activeStepId: 'upload' })
  })

  it('maps pending-review-only catalog to waiting guidance', () => {
    expect(
      resolveMasterDesignerDashboardJourneyIndex([
        masterSummary({ id: 'm1', status: 'PENDING_REVIEW' }),
        masterSummary({ id: 'm2', status: 'APPROVED', updatedAt: '2026-06-20T10:00:00Z' }),
      ]),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.MASTER_DESIGNER.waitingReview.guidance',
    })
  })

  it('maps all-approved catalog to empty onboarding guidance', () => {
    expect(
      resolveMasterDesignerDashboardJourneyIndex([
        masterSummary({ id: 'm1', status: 'APPROVED' }),
      ]),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.MASTER_DESIGNER.empty.guidance',
    })
  })

  it('tie-breaks by most recent updatedAt within same priority', () => {
    const resolution = resolveMasterDesignerDashboardJourneyIndex([
      masterSummary({
        id: 'older',
        status: 'DRAFT',
        originalFilename: '',
        anchorCount: 0,
        updatedAt: '2026-06-20T10:00:00Z',
      }),
      masterSummary({
        id: 'newer',
        status: 'DRAFT',
        originalFilename: '',
        anchorCount: 0,
        updatedAt: '2026-06-28T10:00:00Z',
      }),
    ])
    expect(resolution.targetMasterId).toBe('newer')
  })
})

describe('shouldShowMasterDesignerJourney', () => {
  it('shows for MASTER_DESIGNER', () => {
    expect(
      shouldShowMasterDesignerJourney({
        roles: ['MASTER_DESIGNER'],
        manageMasters: true,
        reviewMasters: false,
      }),
    ).toBe(true)
  })

  it('hides for review-only admin', () => {
    expect(
      shouldShowMasterDesignerJourney({
        roles: ['GROUP_ADMIN'],
        manageMasters: false,
        reviewMasters: true,
      }),
    ).toBe(false)
  })

  it('hides admin reviewer journey on pending review (banner only)', () => {
    expect(
      shouldShowMasterDesignerJourney({
        roles: ['GROUP_ADMIN'],
        manageMasters: true,
        reviewMasters: true,
        status: 'PENDING_REVIEW',
      }),
    ).toBe(false)
  })

  it('hides for audit-only sessions', () => {
    expect(
      shouldShowMasterDesignerJourney({
        roles: ['AUDIT_ADMIN'],
        manageMasters: false,
        reviewMasters: false,
      }),
    ).toBe(false)
  })
})
