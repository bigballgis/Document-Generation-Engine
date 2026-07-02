import { describe, expect, it } from 'vitest'
import { buildDashboardJourneyPath } from '@/utils/dashboardJourneyNavigation'

describe('dashboardJourneyNavigation', () => {
  it('builds template dev-editor deep link when dev version is known', () => {
    expect(
      buildDashboardJourneyPath({
        kind: 'TEMPLATE_AUTHOR',
        activeStepId: 'trialGenerate',
        targetTemplateId: 'tpl-1',
        devVersionId: 'dev-1',
      }),
    ).toBe('/templates/tpl-1/dev/dev-1?workspaceTab=testing&testingTab=dataSets')
  })

  it('falls back to template package hub without dev version', () => {
    expect(
      buildDashboardJourneyPath({
        kind: 'TEMPLATE_TESTER',
        activeStepId: 'checkEvidence',
        targetTemplateId: 'tpl-1',
      }),
    ).toBe('/templates/tpl-1')
  })

  it('builds master package hub path for designer journey', () => {
    expect(
      buildDashboardJourneyPath({
        kind: 'MASTER_DESIGNER',
        activeStepId: 'submitReview',
        targetMasterId: 'master-1',
      }),
    ).toBe('/masters/master-1')
  })

  it('builds global admin task destinations', () => {
    expect(
      buildDashboardJourneyPath({
        kind: 'GLOBAL_ADMIN',
        activeStepId: 'monitorOverdue',
      }),
    ).toBe('/dashboard?queue=ESCALATION#tasks-section')
  })
})
