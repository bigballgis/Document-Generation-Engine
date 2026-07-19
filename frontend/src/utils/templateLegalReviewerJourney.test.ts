import { describe, expect, it } from 'vitest'
import {
  isAwaitingLegalReviewerDecision,
  resolveTemplateLegalReviewerDashboardJourneyIndex,
  shouldShowTemplateLegalReviewerJourney,
  templateLegalReviewerStepCtaKey,
} from '@/utils/templateLegalReviewerJourney'
import type { TemplateSummary } from '@/types/template'

function template(
  overrides: Partial<TemplateSummary> & Pick<TemplateSummary, 'id'>,
): TemplateSummary {
  return {
    id: overrides.id,
    externalId: 'EXT',
    groupCode: 'RETAIL',
    name: 'Letter',
    lifecycleStatus: overrides.lifecycleStatus ?? 'APPROVAL',
    approvalSubState: overrides.approvalSubState ?? 'PENDING_LEGAL_DECISION',
    releaseVersion: null,
    releaseVersionCount: 0,
    masterId: 'm1',
    updatedBy: 'author',
    updatedAt: overrides.updatedAt ?? '2026-07-20T10:00:00Z',
  }
}

describe('templateLegalReviewerJourney (BDD-IBL-E3-016)', () => {
  it('detects PENDING_LEGAL_DECISION only', () => {
    expect(
      isAwaitingLegalReviewerDecision({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_LEGAL_DECISION',
      }),
    ).toBe(true)
    expect(
      isAwaitingLegalReviewerDecision({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
      }),
    ).toBe(false)
  })

  it('shows journey when decideLegalApprovals', () => {
    expect(shouldShowTemplateLegalReviewerJourney({ decideLegalApprovals: true })).toBe(true)
    expect(shouldShowTemplateLegalReviewerJourney({ decideLegalApprovals: false })).toBe(false)
  })

  it('targets newest LEGAL work item on dashboard', () => {
    const resolution = resolveTemplateLegalReviewerDashboardJourneyIndex(
      [template({ id: 'tpl-old', updatedAt: '2026-07-19T10:00:00Z' })],
      [
        { templateId: 'tpl-a', createdAt: '2026-07-18T10:00:00Z' },
        { templateId: 'tpl-b', createdAt: '2026-07-20T12:00:00Z' },
      ],
    )
    expect(resolution.targetTemplateId).toBe('tpl-b')
    expect(resolution.currentStepIndex).toBe(0)
    expect(resolution.activeStepId).toBe('reviewRequest')
  })

  it('builds stable CTA keys', () => {
    expect(templateLegalReviewerStepCtaKey('recordDecision')).toBe(
      'journey.roles.LEGAL_REVIEWER.steps.recordDecision.cta',
    )
  })
})
