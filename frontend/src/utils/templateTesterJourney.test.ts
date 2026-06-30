import { describe, expect, it } from 'vitest'
import {
  hasAllEvidenceViewed,
  hasAnyEvidenceViewed,
  hasTemplatePreviewArtifact,
  resolveTemplateTesterDashboardJourneyIndex,
  resolveTemplateTesterJourneyIndex,
  shouldShowTemplateTesterJourney,
  templateTesterStepCtaKey,
} from '@/utils/templateTesterJourney'
import type {
  TemplateTesterDashboardTemplate,
  TemplateTesterTestWorkItem,
} from '@/utils/templateTesterJourney'

function templateSummary(
  overrides: Partial<TemplateTesterDashboardTemplate> &
    Pick<TemplateTesterDashboardTemplate, 'id' | 'lifecycleStatus'>,
): TemplateTesterDashboardTemplate {
  return {
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Retail letter',
    releaseVersion: null,
    releaseVersionCount: 0,
    masterId: 'master-1',
    updatedBy: '10000005',
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

function testWorkItem(
  overrides: Partial<TemplateTesterTestWorkItem> &
    Pick<TemplateTesterTestWorkItem, 'templateId'>,
): TemplateTesterTestWorkItem {
  return {
    createdAt: '2026-06-25T10:00:00Z',
    ...overrides,
  }
}

describe('evidence signal helpers', () => {
  it('hasTemplatePreviewArtifact is true only when flag is set', () => {
    expect(hasTemplatePreviewArtifact({ hasPreviewArtifact: true })).toBe(true)
    expect(hasTemplatePreviewArtifact({ hasPreviewArtifact: false })).toBe(false)
    expect(hasTemplatePreviewArtifact({})).toBe(false)
  })

  it('hasAnyEvidenceViewed detects any viewed confirmation flag', () => {
    expect(
      hasAnyEvidenceViewed({
        fidelityViewedConfirmed: true,
      }),
    ).toBe(true)
    expect(hasAnyEvidenceViewed({})).toBe(false)
  })

  it('hasAllEvidenceViewed requires all three confirmation flags', () => {
    expect(
      hasAllEvidenceViewed({
        fidelityViewedConfirmed: true,
        coverageViewedConfirmed: true,
        previewViewedConfirmed: true,
      }),
    ).toBe(true)
    expect(
      hasAllEvidenceViewed({
        fidelityViewedConfirmed: true,
        coverageViewedConfirmed: true,
      }),
    ).toBe(false)
  })
})

describe('resolveTemplateTesterJourneyIndex (§12.7 entity mapping)', () => {
  it('maps non-TESTING lifecycle to null without guidance', () => {
    expect(
      resolveTemplateTesterJourneyIndex({
        lifecycleStatus: 'DRAFT',
      }),
    ).toEqual({ currentStepIndex: null })
  })

  it('maps TESTING without evidence to reviewRequest step 0', () => {
    expect(
      resolveTemplateTesterJourneyIndex({
        lifecycleStatus: 'TESTING',
      }),
    ).toEqual({ currentStepIndex: 0, activeStepId: 'reviewRequest' })
  })

  it('maps TESTING with preview artifact to checkEvidence step 1', () => {
    expect(
      resolveTemplateTesterJourneyIndex({
        lifecycleStatus: 'TESTING',
        hasPreviewArtifact: true,
      }),
    ).toEqual({ currentStepIndex: 1, activeStepId: 'checkEvidence' })
  })

  it('maps TESTING with partial evidence viewed to checkEvidence step 1', () => {
    expect(
      resolveTemplateTesterJourneyIndex({
        lifecycleStatus: 'TESTING',
        coverageViewedConfirmed: true,
      }),
    ).toEqual({ currentStepIndex: 1, activeStepId: 'checkEvidence' })
  })

  it('maps TESTING with all evidence viewed to recordResult step 2', () => {
    expect(
      resolveTemplateTesterJourneyIndex({
        lifecycleStatus: 'TESTING',
        hasPreviewArtifact: true,
        fidelityViewedConfirmed: true,
        coverageViewedConfirmed: true,
        previewViewedConfirmed: true,
      }),
    ).toEqual({ currentStepIndex: 2, activeStepId: 'recordResult' })
  })
})

describe('resolveTemplateTesterDashboardJourneyIndex (§12.7 dashboard mapping)', () => {
  it('maps empty queues to null with empty guidance', () => {
    expect(resolveTemplateTesterDashboardJourneyIndex([], [])).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.TEMPLATE_TESTER.empty.guidance',
    })
  })

  it('prioritizes newest OPEN TEST work item over TESTING templates', () => {
    const resolution = resolveTemplateTesterDashboardJourneyIndex(
      [
        templateSummary({
          id: 'tpl-testing',
          lifecycleStatus: 'TESTING',
          updatedAt: '2026-06-27T10:00:00Z',
        }),
      ],
      [
        testWorkItem({
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

  it('picks newest TEST work item when multiple exist', () => {
    const resolution = resolveTemplateTesterDashboardJourneyIndex(
      [],
      [
        testWorkItem({
          templateId: 'older',
          createdAt: '2026-06-20T10:00:00Z',
        }),
        testWorkItem({
          templateId: 'newer',
          createdAt: '2026-06-29T10:00:00Z',
          updatedAt: '2026-06-29T11:00:00Z',
        }),
      ],
    )
    expect(resolution.targetTemplateId).toBe('newer')
  })

  it('falls back to newest TESTING template when no work items', () => {
    const resolution = resolveTemplateTesterDashboardJourneyIndex(
      [
        templateSummary({
          id: 'older-testing',
          lifecycleStatus: 'TESTING',
          updatedAt: '2026-06-20T10:00:00Z',
        }),
        templateSummary({
          id: 'newer-testing',
          lifecycleStatus: 'TESTING',
          updatedAt: '2026-06-29T10:00:00Z',
        }),
      ],
      [],
    )
    expect(resolution).toMatchObject({
      currentStepIndex: 1,
      activeStepId: 'checkEvidence',
      targetTemplateId: 'newer-testing',
    })
  })

  it('assumes preview artifact for TESTING templates on dashboard', () => {
    expect(
      resolveTemplateTesterDashboardJourneyIndex(
        [
          templateSummary({
            id: 'tpl-testing',
            lifecycleStatus: 'TESTING',
          }),
        ],
        [],
      ).currentStepIndex,
    ).toBe(1)
  })
})

describe('templateTesterStepCtaKey', () => {
  it('builds stable CTA keys under journey.roles.TEMPLATE_TESTER.steps', () => {
    expect(templateTesterStepCtaKey('reviewRequest')).toBe(
      'journey.roles.TEMPLATE_TESTER.steps.reviewRequest.cta',
    )
  })
})

describe('shouldShowTemplateTesterJourney', () => {
  it('returns true when decideTests capability is granted', () => {
    expect(shouldShowTemplateTesterJourney({ decideTests: true })).toBe(true)
  })

  it('returns false without decideTests', () => {
    expect(shouldShowTemplateTesterJourney({ decideTests: false })).toBe(false)
  })
})
