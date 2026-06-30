export interface RoleJourneyStep {
  id: string
  labelKey: string
  descriptionKey?: string
}

export type ClusterOneRole = 'MASTER_DESIGNER' | 'TEMPLATE_AUTHOR' | 'TEMPLATE_TESTER'

export type ClusterTwoRole = 'TEMPLATE_APPROVER' | 'GROUP_ADMIN'

export type ClusterThreeRole = 'GLOBAL_ADMIN'

export type ClusterFourRole = 'AUDIT_ADMIN'

export type JourneyRole = ClusterOneRole | ClusterTwoRole | ClusterThreeRole | ClusterFourRole

const ROLE_PREFIX = 'journey.roles'

function step(role: ClusterOneRole, id: string): RoleJourneyStep {
  return {
    id,
    labelKey: `${ROLE_PREFIX}.${role}.steps.${id}.label`,
  }
}

export const masterDesignerJourneySteps: RoleJourneyStep[] = [
  step('MASTER_DESIGNER', 'upload'),
  step('MASTER_DESIGNER', 'placeholders'),
  step('MASTER_DESIGNER', 'submitReview'),
  step('MASTER_DESIGNER', 'rework'),
]

export const templateAuthorJourneySteps: RoleJourneyStep[] = [
  step('TEMPLATE_AUTHOR', 'create'),
  step('TEMPLATE_AUTHOR', 'design'),
  step('TEMPLATE_AUTHOR', 'trialGenerate'),
  step('TEMPLATE_AUTHOR', 'submitTest'),
  step('TEMPLATE_AUTHOR', 'submitApproval'),
  step('TEMPLATE_AUTHOR', 'awaitGoLive'),
]

export const templateTesterJourneySteps: RoleJourneyStep[] = [
  step('TEMPLATE_TESTER', 'reviewRequest'),
  step('TEMPLATE_TESTER', 'checkEvidence'),
  step('TEMPLATE_TESTER', 'recordResult'),
]

function clusterTwoStep(role: ClusterTwoRole, id: string): RoleJourneyStep {
  return {
    id,
    labelKey: `${ROLE_PREFIX}.${role}.steps.${id}.label`,
  }
}

export const templateApproverJourneySteps: RoleJourneyStep[] = [
  clusterTwoStep('TEMPLATE_APPROVER', 'reviewRequest'),
  clusterTwoStep('TEMPLATE_APPROVER', 'reviewSubmission'),
  clusterTwoStep('TEMPLATE_APPROVER', 'recordDecision'),
]

export const templateTeamLeadJourneySteps: RoleJourneyStep[] = [
  clusterTwoStep('GROUP_ADMIN', 'reviewLetterhead'),
  clusterTwoStep('GROUP_ADMIN', 'reviewGoLiveRequest'),
  clusterTwoStep('GROUP_ADMIN', 'runPreReleaseChecks'),
  clusterTwoStep('GROUP_ADMIN', 'confirmGoLive'),
]

function clusterThreeStep(role: ClusterThreeRole, id: string): RoleJourneyStep {
  return {
    id,
    labelKey: `${ROLE_PREFIX}.${role}.steps.${id}.label`,
  }
}

export const globalAdminJourneySteps: RoleJourneyStep[] = [
  clusterThreeStep('GLOBAL_ADMIN', 'reviewOverview'),
  clusterThreeStep('GLOBAL_ADMIN', 'manageUsersGroups'),
  clusterThreeStep('GLOBAL_ADMIN', 'removeTemplates'),
  clusterThreeStep('GLOBAL_ADMIN', 'setReminderDefaults'),
  clusterThreeStep('GLOBAL_ADMIN', 'monitorOverdue'),
  clusterThreeStep('GLOBAL_ADMIN', 'reviewAllTodos'),
]

function clusterFourStep(role: ClusterFourRole, id: string): RoleJourneyStep {
  return {
    id,
    labelKey: `${ROLE_PREFIX}.${role}.steps.${id}.label`,
  }
}

export const auditAdminJourneySteps: RoleJourneyStep[] = [
  clusterFourStep('AUDIT_ADMIN', 'openActivityLog'),
  clusterFourStep('AUDIT_ADMIN', 'searchAndFilter'),
  clusterFourStep('AUDIT_ADMIN', 'reviewEntries'),
  clusterFourStep('AUDIT_ADMIN', 'exportRecords'),
  clusterFourStep('AUDIT_ADMIN', 'viewOnlyMode'),
]

export const ROLE_JOURNEY_DEFINITIONS: Record<ClusterOneRole, RoleJourneyStep[]> = {
  MASTER_DESIGNER: masterDesignerJourneySteps,
  TEMPLATE_AUTHOR: templateAuthorJourneySteps,
  TEMPLATE_TESTER: templateTesterJourneySteps,
}

const CLUSTER_ONE_PRIORITY: ClusterOneRole[] = [
  'MASTER_DESIGNER',
  'TEMPLATE_AUTHOR',
  'TEMPLATE_TESTER',
]

export function resolveClusterOneJourney(role: ClusterOneRole): RoleJourneyStep[] {
  return ROLE_JOURNEY_DEFINITIONS[role]
}

export function resolvePrimaryClusterOneRole(roles: string[]): ClusterOneRole | null {
  for (const role of CLUSTER_ONE_PRIORITY) {
    if (roles.includes(role)) {
      return role
    }
  }
  return null
}

export function roleJourneyTitleKey(role: JourneyRole): string {
  return `${ROLE_PREFIX}.${role}.title`
}

export function roleJourneyEmptyGuidanceKey(role: JourneyRole): string {
  return `${ROLE_PREFIX}.${role}.empty.guidance`
}

export function stepGuidanceKeyFromLabel(labelKey: string): string {
  return labelKey.replace(/\.label$/, '.guidance')
}

export function emptyGuidanceKeyFromStepLabel(labelKey: string): string {
  return labelKey.replace(/\.steps\.[^.]+\.label$/, '.empty.guidance')
}

export {
  isMasterReworkState,
  resolveMasterDesignerDashboardJourneyIndex,
  resolveMasterDesignerJourneyIndex,
} from '@/utils/masterDesignerJourney'

export {
  buildOpenRemediationTemplateIds,
  isTemplateInRemediation,
  resolveTemplateAuthorDashboardJourneyIndex,
  resolveTemplateAuthorJourneyIndex,
} from '@/utils/templateAuthorJourney'

export {
  hasAllEvidenceViewed,
  hasAnyEvidenceViewed,
  hasTemplatePreviewArtifact,
  resolveTemplateTesterDashboardJourneyIndex,
  resolveTemplateTesterJourneyIndex,
  shouldShowTemplateTesterJourney,
  templateTesterStepCtaKey,
} from '@/utils/templateTesterJourney'

export {
  isAwaitingApproverDecision,
  resolveTemplateApproverDashboardJourneyIndex,
  resolveTemplateApproverJourneyIndex,
  shouldShowTemplateApproverJourney,
  templateApproverStepCtaKey,
} from '@/utils/templateApproverJourney'

export {
  isPendingReleaseTemplate,
  isPendingReviewMaster,
  resolveTemplateTeamLeadDashboardJourneyIndex,
  resolveTemplateTeamLeadJourneyIndex,
  shouldShowTemplateTeamLeadJourney,
  templateTeamLeadStepCtaKey,
} from '@/utils/templateTeamLeadJourney'

export {
  globalAdminStepCtaKey,
  resolveGlobalAdminDashboardJourneyIndex,
  shouldShowGlobalAdminJourney,
} from '@/utils/globalAdminJourney'

export {
  auditAdminStepCtaKey,
  resolveAuditAdminJourneyIndex,
  shouldShowAuditAdminJourney,
} from '@/utils/auditAdminJourney'
