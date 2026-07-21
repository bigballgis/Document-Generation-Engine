import {
  ROUTE_KEYS,
  ROUTE_PATH_BY_KEY,
  masterDetailPath,
  templateDevVersionPath,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import {
  resolveTemplateJourneyWorkspaceQuery,
  type TemplateJourneyWorkspaceRole,
} from '@/utils/templateJourneyWorkspaceLink'

export type DashboardJourneyKind =
  | 'DOCUMENT_AUTHOR'
  | 'DOCUMENT_AUTHOR_LETTERHEAD'
  | 'TEMPLATE_TESTER'
  | 'COMPLIANCE_APPROVER'
  | 'LEGAL_REVIEWER'
  | 'GROUP_ADMIN'
  | 'GLOBAL_ADMIN'

function templateWorkspaceRole(kind: DashboardJourneyKind): TemplateJourneyWorkspaceRole | null {
  switch (kind) {
    case 'DOCUMENT_AUTHOR':
      return 'AUTHOR'
    case 'TEMPLATE_TESTER':
      return 'TESTER'
    case 'COMPLIANCE_APPROVER':
      return 'APPROVER'
    case 'LEGAL_REVIEWER':
      return 'LEGAL_REVIEWER'
    case 'GROUP_ADMIN':
      return 'TEAM_LEAD'
    default:
      return null
  }
}

export function buildDashboardJourneyPath(input: {
  kind: DashboardJourneyKind
  activeStepId?: string
  targetTemplateId?: string
  targetMasterId?: string
  devVersionId?: string | null
}): string | null {
  const { kind, activeStepId, targetTemplateId, targetMasterId, devVersionId } = input
  if (!activeStepId) {
    return null
  }

  if (kind === 'DOCUMENT_AUTHOR_LETTERHEAD' && targetMasterId) {
    return masterDetailPath(targetMasterId)
  }

  const templateRole = templateWorkspaceRole(kind)
  if (templateRole && targetTemplateId) {
    const workspaceQuery = resolveTemplateJourneyWorkspaceQuery(templateRole, activeStepId)
    if (devVersionId && workspaceQuery) {
      const { workspaceTab, ...extra } = workspaceQuery
      return templateDevVersionPath(targetTemplateId, devVersionId, undefined, {
        workspaceTab,
        ...extra,
      })
    }
    return templatePackageHubPath(targetTemplateId)
  }

  if (kind === 'GROUP_ADMIN') {
    if (targetMasterId && activeStepId === 'reviewLetterhead') {
      return masterDetailPath(targetMasterId)
    }
    if (targetTemplateId) {
      const workspaceQuery = resolveTemplateJourneyWorkspaceQuery('TEAM_LEAD', activeStepId)
      const templateDevVersionId = devVersionId
      if (templateDevVersionId && workspaceQuery) {
        const { workspaceTab, ...extra } = workspaceQuery
        return templateDevVersionPath(targetTemplateId, templateDevVersionId, undefined, {
          workspaceTab,
          ...extra,
        })
      }
      return templatePackageHubPath(targetTemplateId)
    }
  }

  if (kind === 'GLOBAL_ADMIN') {
    switch (activeStepId) {
      case 'manageUsersGroups':
        return ROUTE_PATH_BY_KEY[ROUTE_KEYS.identityAdministration]
      case 'monitorOverdue':
        return '/dashboard?queue=ESCALATION#tasks-section'
      case 'reviewAllTodos':
        return '/dashboard#tasks-section'
      default:
        return null
    }
  }

  return null
}
