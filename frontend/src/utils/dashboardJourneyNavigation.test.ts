import { describe, expect, it } from 'vitest'
import { ROUTE_KEYS, ROUTE_PATH_BY_KEY } from '@/routing/routeKeys'
import { buildDashboardJourneyPath } from '@/utils/dashboardJourneyNavigation'

describe('dashboardJourneyNavigation', () => {
  it('builds template dev-editor deep link when dev version is known', () => {
    expect(
      buildDashboardJourneyPath({
        kind: 'DOCUMENT_AUTHOR',
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

  it('builds master package hub path for letterhead journey', () => {
    expect(
      buildDashboardJourneyPath({
        kind: 'DOCUMENT_AUTHOR_LETTERHEAD',
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

  it('deep-links GLOBAL_ADMIN setReminderDefaults to System settings Reminder timing', () => {
    expect(
      buildDashboardJourneyPath({
        kind: 'GLOBAL_ADMIN',
        activeStepId: 'setReminderDefaults',
      }),
    ).toBe(ROUTE_PATH_BY_KEY[ROUTE_KEYS.systemSettingsReminderTiming])
    expect(ROUTE_PATH_BY_KEY[ROUTE_KEYS.systemSettingsReminderTiming]).toBe(
      '/system/settings/reminder-timing',
    )
  })

  it('does not send GROUP_ADMIN reminder edit to System settings', () => {
    expect(
      buildDashboardJourneyPath({
        kind: 'GROUP_ADMIN',
        activeStepId: 'setReminderDefaults',
      }),
    ).toBeNull()
    expect(
      buildDashboardJourneyPath({
        kind: 'GROUP_ADMIN',
        activeStepId: 'confirmGoLive',
        targetTemplateId: 'tpl-1',
      }),
    ).toBe('/templates/tpl-1')
    expect(
      buildDashboardJourneyPath({
        kind: 'GROUP_ADMIN',
        activeStepId: 'setReminderDefaults',
      }),
    ).not.toBe(ROUTE_PATH_BY_KEY[ROUTE_KEYS.systemSettingsReminderTiming])
  })
})
