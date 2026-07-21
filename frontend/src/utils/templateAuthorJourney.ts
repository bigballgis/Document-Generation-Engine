import type { TemplateLifecycleStatus } from '@/types/template'
import type {
  TemplateAuthorJourneyContext,
  TemplateAuthorJourneyResolution,
} from '@/utils/templateAuthorJourneyTypes'

export type {
  TemplateAuthorDashboardTemplate,
  TemplateAuthorJourneyContext,
  TemplateAuthorJourneyResolution,
  TemplateAuthorRemediationItem,
} from '@/utils/templateAuthorJourneyTypes'

export { resolveTemplateAuthorDashboardJourneyIndex } from '@/utils/resolveTemplateAuthorDashboardJourney'

const WAITING_TESTING_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.waitingTesting.guidance'
const WAITING_APPROVAL_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.waitingApproval.guidance'
const COMPLETE_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.complete.guidance'
const REMEDIATION_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.remediation.guidance'
const TEAM_LEAD_GO_LIVE_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.awaitGoLive.teamLeadGuidance'

const TERMINAL_STATUSES: TemplateLifecycleStatus[] = ['STOPPED', 'DEPRECATED', 'DELETED']

function hasTemplateBindings(context: { bindingsCount?: number }): boolean {
  return (context.bindingsCount ?? 0) > 0
}

function hasSuccessfulTrialOutput(context: { hasSuccessfulTrialOutput?: boolean }): boolean {
  return context.hasSuccessfulTrialOutput === true
}

export function isTemplateInRemediation(
  templateId: string,
  openRemediationTemplateIds: ReadonlySet<string>,
): boolean {
  return openRemediationTemplateIds.has(templateId)
}

function draftInnerStep(context: TemplateAuthorJourneyContext): Pick<
  TemplateAuthorJourneyResolution,
  'currentStepIndex' | 'activeStepId'
> {
  if (!hasTemplateBindings(context)) {
    return { currentStepIndex: 1, activeStepId: 'design' }
  }
  if (!hasSuccessfulTrialOutput(context)) {
    return { currentStepIndex: 2, activeStepId: 'trialGenerate' }
  }
  return { currentStepIndex: 3, activeStepId: 'submitTest' }
}

function remediationInnerStep(
  context: TemplateAuthorJourneyContext,
): Pick<TemplateAuthorJourneyResolution, 'currentStepIndex' | 'activeStepId'> {
  if (hasTemplateBindings(context) && hasSuccessfulTrialOutput(context)) {
    return { currentStepIndex: 3, activeStepId: 'submitTest' }
  }
  return { currentStepIndex: 1, activeStepId: 'design' }
}

export function resolveTemplateAuthorJourneyIndex(
  context: TemplateAuthorJourneyContext,
): TemplateAuthorJourneyResolution {
  const { lifecycleStatus, approvalSubState } = context

  if (TERMINAL_STATUSES.includes(lifecycleStatus)) {
    return { currentStepIndex: null }
  }

  if (lifecycleStatus === 'TESTING') {
    return { currentStepIndex: null, guidanceKey: WAITING_TESTING_GUIDANCE }
  }

  if (lifecycleStatus === 'APPROVAL') {
    if (approvalSubState === 'PENDING_SUBMIT') {
      return { currentStepIndex: 4, activeStepId: 'submitApproval' }
    }
    // PENDING_DECISION / PENDING_LEGAL_DECISION / PENDING_COMPLIANCE_DECISION / missing
    return { currentStepIndex: null, guidanceKey: WAITING_APPROVAL_GUIDANCE }
  }

  if (lifecycleStatus === 'PENDING_RELEASE') {
    return {
      currentStepIndex: 5,
      activeStepId: 'awaitGoLive',
      guidanceKey: TEAM_LEAD_GO_LIVE_GUIDANCE,
    }
  }

  if (lifecycleStatus === 'PUBLISHED') {
    return { currentStepIndex: null, guidanceKey: COMPLETE_GUIDANCE }
  }

  if (lifecycleStatus === 'DRAFT') {
    if (context.isRemediation) {
      return {
        ...remediationInnerStep(context),
        guidanceKey: REMEDIATION_GUIDANCE,
      }
    }
    return draftInnerStep(context)
  }

  return { currentStepIndex: null }
}

export function templateAuthorStepCtaKey(stepId: string): string {
  return `journey.roles.DOCUMENT_AUTHOR.steps.${stepId}.cta`
}

export function shouldShowTemplateAuthorJourney(options: {
  authorTemplates: boolean
  roles: string[]
}): boolean {
  return options.authorTemplates
}
