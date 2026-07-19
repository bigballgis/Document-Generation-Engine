import type { ApprovalSubState } from '@/types/approvalMatrix'
import type { TemplateLifecycleStatus, TemplateSummary } from '@/types/template'

export interface TemplateLegalReviewerJourneyContext {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: ApprovalSubState | null
  submissionReviewedConfirmed?: boolean
  keyEvidenceViewedConfirmed?: boolean
}

export interface TemplateLegalReviewerJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
  targetTemplateId?: string
}

export type TemplateLegalReviewerDashboardTemplate = TemplateSummary

export interface TemplateLegalApprovalWorkItem {
  templateId: string
  createdAt: string
  updatedAt?: string
}

const EMPTY_GUIDANCE = 'journey.roles.LEGAL_REVIEWER.empty.guidance'

export function isAwaitingLegalReviewerDecision(template: {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: ApprovalSubState | null
}): boolean {
  return (
    template.lifecycleStatus === 'APPROVAL' &&
    template.approvalSubState === 'PENDING_LEGAL_DECISION'
  )
}

function legalInnerStep(
  context: TemplateLegalReviewerJourneyContext,
): Pick<TemplateLegalReviewerJourneyResolution, 'currentStepIndex' | 'activeStepId'> {
  if (context.keyEvidenceViewedConfirmed === true) {
    return { currentStepIndex: 2, activeStepId: 'recordDecision' }
  }
  if (context.submissionReviewedConfirmed === true) {
    return { currentStepIndex: 1, activeStepId: 'reviewSubmission' }
  }
  return { currentStepIndex: 0, activeStepId: 'reviewRequest' }
}

export function resolveTemplateLegalReviewerJourneyIndex(
  context: TemplateLegalReviewerJourneyContext,
): TemplateLegalReviewerJourneyResolution {
  if (
    context.lifecycleStatus !== 'APPROVAL' ||
    context.approvalSubState !== 'PENDING_LEGAL_DECISION'
  ) {
    return { currentStepIndex: null }
  }
  return legalInnerStep(context)
}

function pickNewestLegalWorkItem(
  items: TemplateLegalApprovalWorkItem[],
): TemplateLegalApprovalWorkItem | undefined {
  if (items.length === 0) {
    return undefined
  }
  return [...items].sort(
    (left, right) =>
      Date.parse(right.updatedAt ?? right.createdAt) -
      Date.parse(left.updatedAt ?? left.createdAt),
  )[0]
}

function pickNewestPendingLegalTemplate(
  templates: TemplateLegalReviewerDashboardTemplate[],
): TemplateLegalReviewerDashboardTemplate | undefined {
  const pending = templates.filter(isAwaitingLegalReviewerDecision)
  if (pending.length === 0) {
    return undefined
  }
  return [...pending].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  )[0]
}

export function resolveTemplateLegalReviewerDashboardJourneyIndex(
  templates: TemplateLegalReviewerDashboardTemplate[],
  legalWorkItems: TemplateLegalApprovalWorkItem[],
): TemplateLegalReviewerJourneyResolution {
  const hasPendingLegal = templates.some(isAwaitingLegalReviewerDecision)
  if (legalWorkItems.length === 0 && !hasPendingLegal) {
    return { currentStepIndex: null, guidanceKey: EMPTY_GUIDANCE }
  }

  const legalTarget = pickNewestLegalWorkItem(legalWorkItems)
  if (legalTarget) {
    return {
      ...legalInnerStep({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_LEGAL_DECISION',
      }),
      targetTemplateId: legalTarget.templateId,
    }
  }

  const pendingTarget = pickNewestPendingLegalTemplate(templates)
  return {
    ...legalInnerStep({
      lifecycleStatus: 'APPROVAL',
      approvalSubState: pendingTarget ? 'PENDING_LEGAL_DECISION' : undefined,
      submissionReviewedConfirmed: pendingTarget ? true : undefined,
    }),
    targetTemplateId: pendingTarget?.id,
  }
}

export function templateLegalReviewerStepCtaKey(stepId: string): string {
  return `journey.roles.LEGAL_REVIEWER.steps.${stepId}.cta`
}

export function shouldShowTemplateLegalReviewerJourney(options: {
  decideLegalApprovals: boolean
}): boolean {
  return options.decideLegalApprovals
}
