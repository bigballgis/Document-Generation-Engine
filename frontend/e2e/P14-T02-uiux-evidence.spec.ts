import { expect, test } from '@playwright/test'

import {
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  ageCollaborationWorkItem,
  prepareOverdueTestWorkItem,
  prepareTemplateInTesting,
  requireOpenTestWorkItemForTemplate,
  seedCollaborationWorkItem,
  seedEscalationFromOverdueSource,
} from './helpers/collaboration-api'
import { dashboardTaskRow, filterDashboardTasksByItem, reLoginAs } from './helpers/ui'
import {
  captureP14T02LocatorScreenshot,
  captureP14T02Screenshot,
  ensureP14T02EvidenceDirs,
  P14_T02_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

test.describe('P14-T02 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(() => {
    ensureP14T02EvidenceDirs()
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P14_T02_VIEWPORT)
  })

  test('capture dashboard task hub, lifecycle tab, timeout config, and dual-brand evidence', async ({
    page,
    request,
  }) => {
    const testerTemplate = await prepareTemplateInTesting(request, {
      name: `E2E UIUX Tester ${Date.now()}`,
    })
    const testWorkItem = await requireOpenTestWorkItemForTemplate(request, testerTemplate)
    ageCollaborationWorkItem(testWorkItem.workItemId, "INTERVAL '3 hours'")

    const approvalTemplate = await prepareTemplateInTesting(request, {
      name: `E2E UIUX Approver ${Date.now()}`,
    })
    seedCollaborationWorkItem({
      templateId: approvalTemplate.templateId,
      templateExternalId: approvalTemplate.externalId,
      templateName: approvalTemplate.name,
      queue: 'APPROVAL',
      createdAgeInterval: "INTERVAL '5 hours'",
    })

    const { template: escalationTemplate, sourceWorkItem } = await prepareOverdueTestWorkItem(request)
    seedEscalationFromOverdueSource(sourceWorkItem, escalationTemplate)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard#tasks-section')
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: /pending actions/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await filterDashboardTasksByItem(page, testerTemplate.name)
    const testerTaskRow = await dashboardTaskRow(page, testerTemplate.name)
    await expect(testerTaskRow).toBeVisible({ timeout: 30_000 })
    await captureP14T02Screenshot(page, '01-dashboard-tasks-redbc-1440x900.png')

    await testerTaskRow.click()
    await expect(page).toHaveURL(/tab=lifecycle/)
    await expect(page.getByText(/an internal error occurred/i)).not.toBeVisible()
    await expect(page.locator('#template-lifecycle-panel')).toBeVisible({ timeout: 15_000 })
    await captureP14T02Screenshot(page, '04-template-lifecycle-tab-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await captureP14T02Screenshot(page, '03-template-lifecycle-tab-greenbc-1440x900.png')

    await page.goto('/dashboard#tasks-section')
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    await captureP14T02Screenshot(page, '02-dashboard-tasks-greenbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await page.goto('/dashboard#tasks-section')
    await filterDashboardTasksByItem(page, approvalTemplate.name)
    await expect(await dashboardTaskRow(page, approvalTemplate.name)).toBeVisible()
    await captureP14T02Screenshot(page, '05-dashboard-approver-tasks-redbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await page.goto('/dashboard#tasks-section')
    await filterDashboardTasksByItem(page, escalationTemplate.name)
    await expect(await dashboardTaskRow(page, escalationTemplate.name)).toBeVisible()
    await captureP14T02Screenshot(page, '06-dashboard-escalation-tasks-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await captureP14T02Screenshot(page, '07-dashboard-escalation-tasks-greenbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_ADMIN)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    const timeoutPanel = page.locator('.timeout-config-card')
    await expect(
      timeoutPanel.getByRole('heading', { name: /collaboration timeout thresholds/i }),
    ).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await captureP14T02LocatorScreenshot(
      timeoutPanel,
      '08-dashboard-timeout-config-panel-greenbc-1440x900.png',
    )

    await switchBrand(page, 'REDBC')
    await captureP14T02LocatorScreenshot(
      timeoutPanel,
      '09-dashboard-timeout-config-panel-redbc-1440x900.png',
    )

    await expect(timeoutPanel.getByRole('button', { name: /save thresholds/i })).toBeVisible()
    await expect(timeoutPanel.getByLabel(/testing threshold/i)).toBeVisible()
  })
})
