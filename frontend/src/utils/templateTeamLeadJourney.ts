import type { MasterDocumentSummary } from '@/types/master'
import type { TemplateLifecycleStatus, TemplateSummary } from '@/types/template'

export interface TemplateTeamLeadJourneyContext {
  lifecycleStatus?: TemplateLifecycleStatus
  goLiveRequestReviewedConfirmed?: boolean
  preReleaseChecksViewed?: boolean
  publishGateReady?: boolean
}

export interface TemplateTeamLeadJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
  targetTemplateId?: string
  targetMasterId?: string
}

export type TemplateTeamLeadDashboardTemplate = TemplateSummary

export type TemplateTeamLeadDashboardMaster = Pick<
  MasterDocumentSummary,
  'id' | 'status' | 'updatedAt'
>

export interface TemplateTeamLeadPendingReleaseWorkItem {
  templateId: string
  createdAt: string
  updatedAt?: string
}

const EMPTY_GUIDANCE = 'journey.roles.GROUP_ADMIN.empty.guidance'

export function isPendingReviewMaster(
  master: Pick<MasterDocumentSummary, 'status'>,
): boolean {
  return master.status === 'PENDING_REVIEW'
}

export function isPendingReleaseTemplate(template: {
  lifecycleStatus: TemplateLifecycleStatus
}): boolean {
  return template.lifecycleStatus === 'PENDING_RELEASE'
}

function goLiveInnerStep(
  context: TemplateTeamLeadJourneyContext,
): Pick<TemplateTeamLeadJourneyResolution, 'currentStepIndex' | 'activeStepId'> {
  if (context.publishGateReady === true || context.preReleaseChecksViewed === true) {
    return { currentStepIndex: 3, activeStepId: 'confirmGoLive' }
  }
  if (context.goLiveRequestReviewedConfirmed === true) {
    return { currentStepIndex: 2, activeStepId: 'runPreReleaseChecks' }
  }
  return { currentStepIndex: 1, activeStepId: 'reviewGoLiveRequest' }
}

export function resolveTemplateTeamLeadJourneyIndex(
  context: TemplateTeamLeadJourneyContext,
): TemplateTeamLeadJourneyResolution {
  if (context.lifecycleStatus !== 'PENDING_RELEASE') {
    return { currentStepIndex: null }
  }
  return goLiveInnerStep(context)
}

function pickNewestPendingReleaseWorkItem(
  items: TemplateTeamLeadPendingReleaseWorkItem[],
): TemplateTeamLeadPendingReleaseWorkItem | undefined {
  if (items.length === 0) {
    return undefined
  }
  return [...items].sort(
    (left, right) =>
      Date.parse(right.updatedAt ?? right.createdAt) -
      Date.parse(left.updatedAt ?? left.createdAt),
  )[0]
}

function pickNewestPendingReleaseTemplate(
  templates: TemplateTeamLeadDashboardTemplate[],
): TemplateTeamLeadDashboardTemplate | undefined {
  const pending = templates.filter(isPendingReleaseTemplate)
  if (pending.length === 0) {
    return undefined
  }
  return [...pending].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  )[0]
}

function pickNewestPendingReviewMaster(
  masters: TemplateTeamLeadDashboardMaster[],
): TemplateTeamLeadDashboardMaster | undefined {
  const pending = masters.filter(isPendingReviewMaster)
  if (pending.length === 0) {
    return undefined
  }
  return [...pending].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  )[0]
}

function dashboardContextForPendingReleaseWorkItem(): TemplateTeamLeadJourneyContext {
  return {
    lifecycleStatus: 'PENDING_RELEASE',
  }
}

function dashboardContextForPendingReleaseTemplate(
  template: TemplateTeamLeadDashboardTemplate | undefined,
): TemplateTeamLeadJourneyContext {
  return {
    lifecycleStatus: 'PENDING_RELEASE',
    goLiveRequestReviewedConfirmed: template ? true : undefined,
  }
}

export function resolveTemplateTeamLeadDashboardJourneyIndex(
  masters: TemplateTeamLeadDashboardMaster[],
  templates: TemplateTeamLeadDashboardTemplate[],
  pendingReleaseWorkItems: TemplateTeamLeadPendingReleaseWorkItem[],
): TemplateTeamLeadJourneyResolution {
  const pendingReviewMaster = pickNewestPendingReviewMaster(masters)
  if (pendingReviewMaster) {
    return {
      currentStepIndex: 0,
      activeStepId: 'reviewLetterhead',
      targetMasterId: pendingReviewMaster.id,
    }
  }

  const hasPendingRelease =
    pendingReleaseWorkItems.length > 0 || templates.some(isPendingReleaseTemplate)
  if (!hasPendingRelease) {
    return { currentStepIndex: null, guidanceKey: EMPTY_GUIDANCE }
  }

  const pendingReleaseTarget = pickNewestPendingReleaseWorkItem(pendingReleaseWorkItems)
  if (pendingReleaseTarget) {
    return {
      ...goLiveInnerStep(dashboardContextForPendingReleaseWorkItem()),
      targetTemplateId: pendingReleaseTarget.templateId,
    }
  }

  const pendingReleaseTemplate = pickNewestPendingReleaseTemplate(templates)
  return {
    ...goLiveInnerStep(dashboardContextForPendingReleaseTemplate(pendingReleaseTemplate)),
    targetTemplateId: pendingReleaseTemplate?.id,
  }
}

export function templateTeamLeadStepCtaKey(stepId: string): string {
  return `journey.roles.GROUP_ADMIN.steps.${stepId}.cta`
}

export function shouldShowTemplateTeamLeadJourney(options: {
  publishTemplates: boolean
  reviewMasters: boolean
}): boolean {
  return options.publishTemplates || options.reviewMasters
}
