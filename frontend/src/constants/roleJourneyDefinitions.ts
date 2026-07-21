export interface RoleJourneyStep {
  id: string
  labelKey: string
  descriptionKey?: string
}

/** Cluster-① assignable roles after ADR-0070 (letterhead+author → DOCUMENT_AUTHOR). */
export type ClusterOneRole = 'DOCUMENT_AUTHOR' | 'TEMPLATE_TESTER'

type ClusterTwoRole = 'LEGAL_REVIEWER' | 'GROUP_ADMIN'

type ClusterThreeRole = 'GLOBAL_ADMIN'

type ClusterFourRole = 'AUDIT_ADMIN'

export type JourneyRole = ClusterOneRole | ClusterTwoRole | ClusterThreeRole | ClusterFourRole

const ROLE_PREFIX = 'journey.roles'

function step(role: ClusterOneRole, id: string): RoleJourneyStep {
  return {
    id,
    labelKey: `${ROLE_PREFIX}.${role}.steps.${id}.label`,
  }
}

/** Letterhead workflow steps — DOCUMENT_AUTHOR union capability (not a separate assignable role). */
export const masterDesignerJourneySteps: RoleJourneyStep[] = [
  {
    id: 'upload',
    labelKey: `${ROLE_PREFIX}.DOCUMENT_AUTHOR.letterhead.steps.upload.label`,
  },
  {
    id: 'placeholders',
    labelKey: `${ROLE_PREFIX}.DOCUMENT_AUTHOR.letterhead.steps.placeholders.label`,
  },
  {
    id: 'submitReview',
    labelKey: `${ROLE_PREFIX}.DOCUMENT_AUTHOR.letterhead.steps.submitReview.label`,
  },
  {
    id: 'rework',
    labelKey: `${ROLE_PREFIX}.DOCUMENT_AUTHOR.letterhead.steps.rework.label`,
  },
]

/** @deprecated Prefer documentAuthorJourneySteps — alias kept for existing imports. */
export const templateAuthorJourneySteps: RoleJourneyStep[] = [
  step('DOCUMENT_AUTHOR', 'create'),
  step('DOCUMENT_AUTHOR', 'design'),
  step('DOCUMENT_AUTHOR', 'trialGenerate'),
  step('DOCUMENT_AUTHOR', 'submitTest'),
  step('DOCUMENT_AUTHOR', 'submitApproval'),
  step('DOCUMENT_AUTHOR', 'awaitGoLive'),
]

export const documentAuthorJourneySteps = templateAuthorJourneySteps

export const templateTesterJourneySteps: RoleJourneyStep[] = [
  step('TEMPLATE_TESTER', 'reviewRequest'),
  step('TEMPLATE_TESTER', 'checkEvidence'),
  step('TEMPLATE_TESTER', 'recordResult'),
]

function clusterTwoStep(role: ClusterTwoRole | 'GROUP_ADMIN.compliance', id: string): RoleJourneyStep {
  return {
    id,
    labelKey: `${ROLE_PREFIX}.${role}.steps.${id}.label`,
  }
}

/** Compliance / single-track approval workflow — GROUP_ADMIN decideApprovals (ex-TEMPLATE_APPROVER). */
export const templateApproverJourneySteps: RoleJourneyStep[] = [
  clusterTwoStep('GROUP_ADMIN.compliance', 'reviewRequest'),
  clusterTwoStep('GROUP_ADMIN.compliance', 'reviewSubmission'),
  clusterTwoStep('GROUP_ADMIN.compliance', 'recordDecision'),
]

export const templateLegalReviewerJourneySteps: RoleJourneyStep[] = [
  clusterTwoStep('LEGAL_REVIEWER', 'reviewRequest'),
  clusterTwoStep('LEGAL_REVIEWER', 'reviewSubmission'),
  clusterTwoStep('LEGAL_REVIEWER', 'recordDecision'),
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

const ROLE_JOURNEY_DEFINITIONS: Record<ClusterOneRole, RoleJourneyStep[]> = {
  DOCUMENT_AUTHOR: documentAuthorJourneySteps,
  TEMPLATE_TESTER: templateTesterJourneySteps,
}

const CLUSTER_ONE_PRIORITY: ClusterOneRole[] = ['DOCUMENT_AUTHOR', 'TEMPLATE_TESTER']

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

export function stepGuidanceKeyFromLabel(labelKey: string): string {
  return labelKey.replace(/\.label$/, '.guidance')
}

export function emptyGuidanceKeyFromStepLabel(labelKey: string): string {
  return labelKey.replace(/\.steps\.[^.]+\.label$/, '.empty.guidance')
}
