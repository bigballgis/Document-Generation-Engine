import {
  auditAdminJourneySteps,
  globalAdminJourneySteps,
  resolveClusterOneJourney,
  resolvePrimaryClusterOneRole,
  templateLegalReviewerJourneySteps,
  templateTeamLeadJourneySteps,
  type JourneyRole,
  type RoleJourneyStep,
} from '@/constants/roleJourneyDefinitions'
import { MANAGEMENT_ROLES } from '@/auth/roles'
import { shouldShowAuditAdminJourney } from '@/utils/auditAdminJourney'
import { shouldShowGlobalAdminJourney } from '@/utils/globalAdminJourney'
import { shouldShowTemplateLegalReviewerJourney } from '@/utils/templateLegalReviewerJourney'
import { shouldShowTemplateTeamLeadJourney } from '@/utils/templateTeamLeadJourney'

export type TourRole = JourneyRole

export interface ResolvePrimaryTourRoleInput {
  roles: string[]
  decideApprovals: boolean
  decideLegalApprovals?: boolean
  publishTemplates: boolean
  reviewMasters: boolean
}

/**
 * Primary tour role — Dashboard journey priority (C8-C5) + AUDIT_ADMIN fallback.
 * Does not mutate Dashboard display logic; pure resolution for the onboarding tour.
 * ADR-0070: retired TEMPLATE_APPROVER / MASTER_DESIGNER / TEMPLATE_AUTHOR codes.
 */
export function resolvePrimaryTourRole(input: ResolvePrimaryTourRoleInput): TourRole | null {
  const roles = input.roles

  const clusterOne = resolvePrimaryClusterOneRole(roles)
  if (clusterOne) {
    return clusterOne
  }

  if (
    roles.includes(MANAGEMENT_ROLES.LEGAL_REVIEWER) &&
    shouldShowTemplateLegalReviewerJourney({
      decideLegalApprovals: input.decideLegalApprovals ?? false,
    })
  ) {
    return 'LEGAL_REVIEWER'
  }

  if (shouldShowGlobalAdminJourney({ roles })) {
    return 'GLOBAL_ADMIN'
  }

  if (
    roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN) &&
    shouldShowTemplateTeamLeadJourney({
      publishTemplates: input.publishTemplates,
      reviewMasters: input.reviewMasters,
    })
  ) {
    return 'GROUP_ADMIN'
  }

  if (shouldShowAuditAdminJourney({ roles })) {
    return 'AUDIT_ADMIN'
  }

  return null
}

/** Steps are the same arrays exported from roleJourneyDefinitions (no fork). */
export function resolveTourStepsForRole(role: TourRole): RoleJourneyStep[] {
  switch (role) {
    case 'DOCUMENT_AUTHOR':
    case 'TEMPLATE_TESTER':
      return resolveClusterOneJourney(role)
    case 'LEGAL_REVIEWER':
      return templateLegalReviewerJourneySteps
    case 'GLOBAL_ADMIN':
      return globalAdminJourneySteps
    case 'GROUP_ADMIN':
      return templateTeamLeadJourneySteps
    case 'AUDIT_ADMIN':
      return auditAdminJourneySteps
    default: {
      const _exhaustive: never = role
      return _exhaustive
    }
  }
}
