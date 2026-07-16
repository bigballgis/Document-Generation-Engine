import { describe, expect, it } from 'vitest'
import {
  resolveCollaborationQueueWorkspaceQuery,
  resolveLifecycleHubDeepLinkTarget,
  resolveTemplateJourneyWorkspaceQuery,
} from '@/utils/templateJourneyWorkspaceLink'

describe('templateJourneyWorkspaceLink', () => {
  it('maps author journey steps to dev workspace query', () => {
    expect(resolveTemplateJourneyWorkspaceQuery('AUTHOR', 'design')).toEqual({
      workspaceTab: 'design',
      designTab: 'variables',
    })
    expect(resolveTemplateJourneyWorkspaceQuery('AUTHOR', 'trialGenerate')).toEqual({
      workspaceTab: 'testing',
      testingTab: 'dataSets',
    })
    expect(resolveTemplateJourneyWorkspaceQuery('AUTHOR', 'submitApproval')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'submitApproval',
    })
    expect(resolveTemplateJourneyWorkspaceQuery('AUTHOR', 'create')).toBeNull()
  })

  it('maps tester journey steps to evidence-focused testing sub-tabs', () => {
    expect(resolveTemplateJourneyWorkspaceQuery('TESTER', 'checkEvidence')).toEqual({
      workspaceTab: 'testing',
      testingTab: 'coverage',
    })
    expect(resolveTemplateJourneyWorkspaceQuery('TESTER', 'recordResult')).toEqual({
      workspaceTab: 'testing',
      testingTab: 'previewRuns',
    })
  })

  it('maps approver and team-lead steps to approval sub-tabs', () => {
    expect(resolveTemplateJourneyWorkspaceQuery('APPROVER', 'reviewSubmission')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'riskConfig',
    })
    expect(resolveTemplateJourneyWorkspaceQuery('TEAM_LEAD', 'confirmGoLive')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'publishReadiness',
    })
    expect(resolveTemplateJourneyWorkspaceQuery('TEAM_LEAD', 'reviewLetterhead')).toBeNull()
  })

  it('BDD-CE-U14: maps collaboration queues to journey decision surfaces', () => {
    expect(resolveCollaborationQueueWorkspaceQuery('TEST')).toEqual({
      workspaceTab: 'testing',
      testingTab: 'previewRuns',
    })
    expect(resolveCollaborationQueueWorkspaceQuery('APPROVAL')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'submitApproval',
    })
    expect(resolveCollaborationQueueWorkspaceQuery('PENDING_RELEASE')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'publishReadiness',
    })
    expect(resolveCollaborationQueueWorkspaceQuery('REMEDIATION')).toEqual({
      workspaceTab: 'approval',
    })
    expect(resolveCollaborationQueueWorkspaceQuery('ESCALATION')).toEqual({
      workspaceTab: 'approval',
    })
  })

  it('BDD-CE-U14-D3: hub lifecycle redirect prefers workspaceTab / queue over approval default', () => {
    expect(
      resolveLifecycleHubDeepLinkTarget({
        workspaceTab: 'testing',
        testingTab: 'previewRuns',
      }),
    ).toEqual({
      workspaceTab: 'testing',
      extraQuery: { testingTab: 'previewRuns' },
    })
    expect(resolveLifecycleHubDeepLinkTarget({ queue: 'TEST' })).toEqual({
      workspaceTab: 'testing',
      extraQuery: { testingTab: 'previewRuns' },
    })
    expect(resolveLifecycleHubDeepLinkTarget({ queue: 'APPROVAL' })).toEqual({
      workspaceTab: 'approval',
      extraQuery: { approvalTab: 'submitApproval' },
    })
    expect(resolveLifecycleHubDeepLinkTarget({ queue: 'PENDING_RELEASE' })).toEqual({
      workspaceTab: 'approval',
      extraQuery: { approvalTab: 'publishReadiness' },
    })
    expect(resolveLifecycleHubDeepLinkTarget({})).toEqual({
      workspaceTab: 'approval',
      extraQuery: {},
    })
  })
})
