import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'

export type TemplateJourneyWorkspaceRole = 'AUTHOR' | 'TESTER' | 'APPROVER' | 'TEAM_LEAD'

export type TemplateJourneyWorkspaceQuery = Record<string, string> & {
  workspaceTab: TemplateDevWorkspaceTab
}

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
