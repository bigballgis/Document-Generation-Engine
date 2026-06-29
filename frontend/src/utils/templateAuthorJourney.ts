import type { TemplateLifecycleStatus, TemplateSummary } from '@/types/template'

export interface TemplateAuthorJourneyContext {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION'
  bindingsCount?: number
  hasSuccessfulTrialOutput?: boolean
  isRemediation?: boolean
}

export interface TemplateAuthorJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
  targetTemplateId?: string
}

export type TemplateAuthorDashboardTemplate = TemplateSummary & {
  bindingsCount?: number
  hasSuccessfulTrialOutput?: boolean
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION'
}

export interface TemplateAuthorRemediationItem {
  templateId: string
  createdAt: string
  updatedAt?: string
}

const WAITING_TESTING_GUIDANCE = 'journey.roles.TEMPLATE_AUTHOR.waitingTesting.guidance'
const WAITING_APPROVAL_GUIDANCE = 'journey.roles.TEMPLATE_AUTHOR.waitingApproval.guidance'
const COMPLETE_GUIDANCE = 'journey.roles.TEMPLATE_AUTHOR.complete.guidance'
const EMPTY_GUIDANCE = 'journey.roles.TEMPLATE_AUTHOR.empty.guidance'
const REMEDIATION_GUIDANCE = 'journey.roles.TEMPLATE_AUTHOR.remediation.guidance'
const TEAM_LEAD_GO_LIVE_GUIDANCE = 'journey.roles.TEMPLATE_AUTHOR.awaitGoLive.teamLeadGuidance'

const TERMINAL_STATUSES: TemplateLifecycleStatus[] = ['STOPPED', 'DEPRECATED', 'DELETED']

export function hasTemplateBindings(context: { bindingsCount?: number }): boolean {
  return (context.bindingsCount ?? 0) > 0
}

export function hasSuccessfulTrialOutput(context: { hasSuccessfulTrialOutput?: boolean }): boolean {
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

export function templateAuthorStepCtaKey(stepId: string): string {
  return `journey.roles.TEMPLATE_AUTHOR.steps.${stepId}.cta`
}

export function shouldShowTemplateAuthorJourney(options: {
  authorTemplates: boolean
  roles: string[]
}): boolean {
  return options.authorTemplates
}

export function buildOpenRemediationTemplateIds(
  remediationItems: TemplateAuthorRemediationItem[],
): Set<string> {
  return new Set(remediationItems.map((item) => item.templateId))
}
