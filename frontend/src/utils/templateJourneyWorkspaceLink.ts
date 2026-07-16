import type { CollaborationWorkItemQueue } from '@/types/collaboration'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'

export type TemplateJourneyWorkspaceRole = 'AUTHOR' | 'TESTER' | 'APPROVER' | 'TEAM_LEAD'

export type TemplateJourneyWorkspaceQuery = Record<string, string> & {
  workspaceTab: TemplateDevWorkspaceTab
}

export type LifecycleHubDeepLinkTarget = {
  workspaceTab: TemplateDevWorkspaceTab
  extraQuery: Record<string, string>
}

const DEV_WORKSPACE_TABS = new Set<string>(['design', 'testing', 'approval'])

export function resolveTemplateJourneyWorkspaceQuery(
  role: TemplateJourneyWorkspaceRole,
  stepId: string | undefined,
): TemplateJourneyWorkspaceQuery | null {
  if (!stepId) {
    return null
  }

  switch (role) {
    case 'AUTHOR':
      return resolveAuthorStepQuery(stepId)
    case 'TESTER':
      return resolveTesterStepQuery(stepId)
    case 'APPROVER':
      return resolveApproverStepQuery(stepId)
    case 'TEAM_LEAD':
      return resolveTeamLeadStepQuery(stepId)
    default:
      return null
  }
}

/** CE-U14: map collaboration queue → same decision surface as CDP journey CTAs. */
export function resolveCollaborationQueueWorkspaceQuery(
  queue: CollaborationWorkItemQueue,
): TemplateJourneyWorkspaceQuery {
  switch (queue) {
    case 'TEST':
      return resolveTesterStepQuery('recordResult') ?? { workspaceTab: 'testing' }
    case 'APPROVAL':
      return resolveApproverStepQuery('reviewRequest') ?? { workspaceTab: 'approval' }
    case 'PENDING_RELEASE':
      return resolveTeamLeadStepQuery('confirmGoLive') ?? { workspaceTab: 'approval' }
    case 'REMEDIATION':
    case 'ESCALATION':
      return { workspaceTab: 'approval' }
    default:
      return { workspaceTab: 'approval' }
  }
}

/**
 * Hub `?tab=lifecycle` redirect target (CE-U14-D3).
 * Prefers explicit workspace query params, then queue mapping, else legacy approval.
 */
export function resolveLifecycleHubDeepLinkTarget(query: {
  queue?: unknown
  workspaceTab?: unknown
  testingTab?: unknown
  approvalTab?: unknown
  designTab?: unknown
}): LifecycleHubDeepLinkTarget {
  const explicitTab =
    typeof query.workspaceTab === 'string' && DEV_WORKSPACE_TABS.has(query.workspaceTab)
      ? (query.workspaceTab as TemplateDevWorkspaceTab)
      : null

  if (explicitTab) {
    const extraQuery: Record<string, string> = {}
    if (typeof query.testingTab === 'string' && query.testingTab) {
      extraQuery.testingTab = query.testingTab
    }
    if (typeof query.approvalTab === 'string' && query.approvalTab) {
      extraQuery.approvalTab = query.approvalTab
    }
    if (typeof query.designTab === 'string' && query.designTab) {
      extraQuery.designTab = query.designTab
    }
    return { workspaceTab: explicitTab, extraQuery }
  }

  if (typeof query.queue === 'string' && isCollaborationQueue(query.queue)) {
    const mapped = resolveCollaborationQueueWorkspaceQuery(query.queue)
    const { workspaceTab, ...extra } = mapped
    return { workspaceTab, extraQuery: extra }
  }

  return { workspaceTab: 'approval', extraQuery: {} }
}

function isCollaborationQueue(value: string): value is CollaborationWorkItemQueue {
  return (
    value === 'TEST' ||
    value === 'APPROVAL' ||
    value === 'PENDING_RELEASE' ||
    value === 'REMEDIATION' ||
    value === 'ESCALATION'
  )
}

function resolveAuthorStepQuery(stepId: string): TemplateJourneyWorkspaceQuery | null {
  switch (stepId) {
    case 'design':
      return { workspaceTab: 'design', designTab: 'variables' }
    case 'trialGenerate':
    case 'submitTest':
      return { workspaceTab: 'testing', testingTab: 'dataSets' }
    case 'submitApproval':
      return { workspaceTab: 'approval', approvalTab: 'submitApproval' }
    case 'awaitGoLive':
      return { workspaceTab: 'approval', approvalTab: 'publishReadiness' }
    default:
      return null
  }
}

function resolveTesterStepQuery(stepId: string): TemplateJourneyWorkspaceQuery | null {
  switch (stepId) {
    case 'reviewRequest':
      return { workspaceTab: 'testing', testingTab: 'dataSets' }
    case 'checkEvidence':
      return { workspaceTab: 'testing', testingTab: 'coverage' }
    case 'recordResult':
      return { workspaceTab: 'testing', testingTab: 'previewRuns' }
    default:
      return null
  }
}

function resolveApproverStepQuery(stepId: string): TemplateJourneyWorkspaceQuery | null {
  switch (stepId) {
    case 'reviewRequest':
      return { workspaceTab: 'approval', approvalTab: 'submitApproval' }
    case 'reviewSubmission':
      return { workspaceTab: 'approval', approvalTab: 'riskConfig' }
    case 'recordDecision':
      return { workspaceTab: 'approval', approvalTab: 'publishReadiness' }
    default:
      return null
  }
}

function resolveTeamLeadStepQuery(stepId: string): TemplateJourneyWorkspaceQuery | null {
  switch (stepId) {
    case 'reviewGoLiveRequest':
    case 'runPreReleaseChecks':
    case 'confirmGoLive':
      return { workspaceTab: 'approval', approvalTab: 'publishReadiness' }
    default:
      return null
  }
}
