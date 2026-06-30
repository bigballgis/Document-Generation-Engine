import type { TemplateLifecycleStatus, TemplateSummary } from '@/types/template'

export interface TemplateApproverJourneyContext {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION'
  submissionReviewedConfirmed?: boolean
  keyEvidenceViewedConfirmed?: boolean
}

export interface TemplateApproverJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
  targetTemplateId?: string
}

export type TemplateApproverDashboardTemplate = TemplateSummary

export interface TemplateApproverApprovalWorkItem {
  templateId: string
  createdAt: string
  updatedAt?: string
}

const EMPTY_GUIDANCE = 'journey.roles.TEMPLATE_APPROVER.empty.guidance'

export function isAwaitingApproverDecision(template: {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION'
}): boolean {
  return (
    template.lifecycleStatus === 'APPROVAL' && template.approvalSubState === 'PENDING_DECISION'
  )
}

function approvalInnerStep(
  context: TemplateApproverJourneyContext,
): Pick<TemplateApproverJourneyResolution, 'currentStepIndex' | 'activeStepId'> {
  if (context.keyEvidenceViewedConfirmed === true) {
    return { currentStepIndex: 2, activeStepId: 'recordDecision' }
  }
  if (context.submissionReviewedConfirmed === true) {
    return { currentStepIndex: 1, activeStepId: 'reviewSubmission' }
  }
  return { currentStepIndex: 0, activeStepId: 'reviewRequest' }
}

export function resolveTemplateApproverJourneyIndex(
  context: TemplateApproverJourneyContext,
): TemplateApproverJourneyResolution {
  if (
    context.lifecycleStatus !== 'APPROVAL' ||
    context.approvalSubState !== 'PENDING_DECISION'
  ) {
    return { currentStepIndex: null }
  }
  return approvalInnerStep(context)
}

function pickNewestApprovalWorkItem(
  items: TemplateApproverApprovalWorkItem[],
): TemplateApproverApprovalWorkItem | undefined {
  if (items.length === 0) {
    return undefined
  }
  return [...items].sort(
    (left, right) =>
      Date.parse(right.updatedAt ?? right.createdAt) -
      Date.parse(left.updatedAt ?? left.createdAt),
  )[0]
}

function pickNewestPendingDecisionTemplate(
  templates: TemplateApproverDashboardTemplate[],
): TemplateApproverDashboardTemplate | undefined {
  const pending = templates.filter(isAwaitingApproverDecision)
  if (pending.length === 0) {
    return undefined
  }
  return [...pending].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  )[0]
}

function dashboardContextForApprovalWorkItem(): TemplateApproverJourneyContext {
  return {
    lifecycleStatus: 'APPROVAL',
    approvalSubState: 'PENDING_DECISION',
  }
}

function dashboardContextForPendingDecisionTemplate(
  template: TemplateApproverDashboardTemplate | undefined,
): TemplateApproverJourneyContext {
  return {
    lifecycleStatus: 'APPROVAL',
    approvalSubState: template ? 'PENDING_DECISION' : undefined,
    submissionReviewedConfirmed: template ? true : undefined,
  }
}

export function resolveTemplateApproverDashboardJourneyIndex(
  templates: TemplateApproverDashboardTemplate[],
  approvalWorkItems: TemplateApproverApprovalWorkItem[],
): TemplateApproverJourneyResolution {
  const hasPendingDecision = templates.some(isAwaitingApproverDecision)
  if (approvalWorkItems.length === 0 && !hasPendingDecision) {
    return { currentStepIndex: null, guidanceKey: EMPTY_GUIDANCE }
  }

  const approvalTarget = pickNewestApprovalWorkItem(approvalWorkItems)
  if (approvalTarget) {
    return {
      ...approvalInnerStep(dashboardContextForApprovalWorkItem()),
      targetTemplateId: approvalTarget.templateId,
    }
  }

  const pendingTarget = pickNewestPendingDecisionTemplate(templates)
  return {
    ...approvalInnerStep(dashboardContextForPendingDecisionTemplate(pendingTarget)),
    targetTemplateId: pendingTarget?.id,
  }
}

export function templateApproverStepCtaKey(stepId: string): string {
  return `journey.roles.TEMPLATE_APPROVER.steps.${stepId}.cta`
}

export function shouldShowTemplateApproverJourney(options: { decideApprovals: boolean }): boolean {
  return options.decideApprovals
}
