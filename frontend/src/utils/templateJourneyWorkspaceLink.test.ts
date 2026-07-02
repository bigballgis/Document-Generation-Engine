import { describe, expect, it } from 'vitest'
import { resolveTemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'

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
})
