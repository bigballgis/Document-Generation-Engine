import type {
  TemplateAuthorDashboardTemplate,
  TemplateAuthorJourneyContext,
  TemplateAuthorJourneyResolution,
  TemplateAuthorRemediationItem,
} from '@/utils/templateAuthorJourneyTypes'

export {
  type TemplateAuthorDashboardTemplate,
  type TemplateAuthorJourneyContext,
  type TemplateAuthorJourneyResolution,
  type TemplateAuthorRemediationItem,
} from '@/utils/templateAuthorJourneyTypes'

const WAITING_TESTING_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.waitingTesting.guidance'
const WAITING_APPROVAL_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.waitingApproval.guidance'
const EMPTY_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.empty.guidance'
const REMEDIATION_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.remediation.guidance'
const TEAM_LEAD_GO_LIVE_GUIDANCE = 'journey.roles.DOCUMENT_AUTHOR.awaitGoLive.teamLeadGuidance'

function hasTemplateBindings(context: { bindingsCount?: number }): boolean {
  return (context.bindingsCount ?? 0) > 0
}

function hasSuccessfulTrialOutput(context: { hasSuccessfulTrialOutput?: boolean }): boolean {
  return context.hasSuccessfulTrialOutput === true
}

function remediationInnerStep(
  context: TemplateAuthorJourneyContext,
): Pick<TemplateAuthorJourneyResolution, 'currentStepIndex' | 'activeStepId'> {
  if (hasTemplateBindings(context) && hasSuccessfulTrialOutput(context)) {
    return { currentStepIndex: 3, activeStepId: 'submitTest' }
  }
  return { currentStepIndex: 1, activeStepId: 'design' }
}

function pickNewestTemplate(
  templates: TemplateAuthorDashboardTemplate[],
): TemplateAuthorDashboardTemplate | undefined {
  if (templates.length === 0) {
    return undefined
  }
  return [...templates].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  )[0]
}

function pickNewestRemediationItem(
  items: TemplateAuthorRemediationItem[],
): TemplateAuthorRemediationItem | undefined {
  if (items.length === 0) {
    return undefined
  }
  return [...items].sort(
    (left, right) =>
      Date.parse(right.updatedAt ?? right.createdAt) -
      Date.parse(left.updatedAt ?? left.createdAt),
  )[0]
}

function withTargetTemplate(
  resolution: Omit<TemplateAuthorJourneyResolution, 'targetTemplateId'>,
  template: TemplateAuthorDashboardTemplate | undefined,
): TemplateAuthorJourneyResolution {
  return {
    ...resolution,
    targetTemplateId: template?.id,
  }
}

function templateById(
  templates: TemplateAuthorDashboardTemplate[],
  templateId: string,
): TemplateAuthorDashboardTemplate | undefined {
  return templates.find((template) => template.id === templateId)
}

function isDraftReadyToSubmit(template: TemplateAuthorDashboardTemplate): boolean {
  return (
    template.lifecycleStatus === 'DRAFT' &&
    hasTemplateBindings(template) &&
    hasSuccessfulTrialOutput(template)
  )
}

function isDraftNeedsTrial(template: TemplateAuthorDashboardTemplate): boolean {
  return (
    template.lifecycleStatus === 'DRAFT' &&
    hasTemplateBindings(template) &&
    !hasSuccessfulTrialOutput(template)
  )
}

function isDraftNeedsDesign(template: TemplateAuthorDashboardTemplate): boolean {
  return template.lifecycleStatus === 'DRAFT' && !hasTemplateBindings(template)
}

export function resolveTemplateAuthorDashboardJourneyIndex(
  templates: TemplateAuthorDashboardTemplate[],
  remediationItems: TemplateAuthorRemediationItem[],
): TemplateAuthorJourneyResolution {
  if (templates.length === 0 && remediationItems.length === 0) {
    return { currentStepIndex: 0, activeStepId: 'create' }
  }

  if (remediationItems.length > 0) {
    const remediationTarget = pickNewestRemediationItem(remediationItems)
    const relatedTemplate = remediationTarget
      ? templateById(templates, remediationTarget.templateId)
      : undefined
    const context: TemplateAuthorJourneyContext = {
      lifecycleStatus: 'DRAFT',
      bindingsCount: relatedTemplate?.bindingsCount,
      hasSuccessfulTrialOutput: relatedTemplate?.hasSuccessfulTrialOutput,
      isRemediation: true,
    }
    return {
      ...remediationInnerStep(context),
      guidanceKey: REMEDIATION_GUIDANCE,
      targetTemplateId: remediationTarget?.templateId,
    }
  }

  const draftReady = templates.filter(isDraftReadyToSubmit)
  if (draftReady.length > 0) {
    const target = pickNewestTemplate(draftReady)
    return withTargetTemplate({ currentStepIndex: 3, activeStepId: 'submitTest' }, target)
  }

  const draftNeedsTrial = templates.filter(isDraftNeedsTrial)
  if (draftNeedsTrial.length > 0) {
    const target = pickNewestTemplate(draftNeedsTrial)
    return withTargetTemplate({ currentStepIndex: 2, activeStepId: 'trialGenerate' }, target)
  }

  const draftNeedsDesign = templates.filter(isDraftNeedsDesign)
  if (draftNeedsDesign.length > 0) {
    const target = pickNewestTemplate(draftNeedsDesign)
    return withTargetTemplate({ currentStepIndex: 1, activeStepId: 'design' }, target)
  }

  const pendingSubmit = templates.filter(
    (template) =>
      template.lifecycleStatus === 'APPROVAL' && template.approvalSubState === 'PENDING_SUBMIT',
  )
  if (pendingSubmit.length > 0) {
    const target = pickNewestTemplate(pendingSubmit)
    return withTargetTemplate({ currentStepIndex: 4, activeStepId: 'submitApproval' }, target)
  }

  const pendingRelease = templates.filter((template) => template.lifecycleStatus === 'PENDING_RELEASE')
  if (pendingRelease.length > 0) {
    const target = pickNewestTemplate(pendingRelease)
    return withTargetTemplate({
      currentStepIndex: 5,
      activeStepId: 'awaitGoLive',
      guidanceKey: TEAM_LEAD_GO_LIVE_GUIDANCE,
    }, target)
  }

  const hasTesting = templates.some((template) => template.lifecycleStatus === 'TESTING')
  if (hasTesting) {
    return { currentStepIndex: null, guidanceKey: WAITING_TESTING_GUIDANCE }
  }

  const hasWaitingApproval = templates.some(
    (template) =>
      template.lifecycleStatus === 'APPROVAL' &&
      template.approvalSubState !== 'PENDING_SUBMIT',
  )
  if (hasWaitingApproval) {
    return { currentStepIndex: null, guidanceKey: WAITING_APPROVAL_GUIDANCE }
  }

  if (templates.every((template) => template.lifecycleStatus === 'PUBLISHED')) {
    return { currentStepIndex: null, guidanceKey: EMPTY_GUIDANCE }
  }

  return { currentStepIndex: null }
}
