export interface RoleJourneyStep {
  id: string
  labelKey: string
  descriptionKey?: string
}

export type ClusterOneRole = 'MASTER_DESIGNER' | 'TEMPLATE_AUTHOR' | 'TEMPLATE_TESTER'

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

export function roleJourneyTitleKey(role: ClusterOneRole): string {
  return `${ROLE_PREFIX}.${role}.title`
}

export function roleJourneyEmptyGuidanceKey(role: ClusterOneRole): string {
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
