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
import { reLoginAs } from './helpers/ui'
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

  test('capture workbench panels, timeout config, and dual-brand evidence', async ({
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
    await page.goto('/workbench/tester')
    await expect(page.getByRole('heading', { name: /tester workbench/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.getByRole('row', { name: new RegExp(testerTemplate.name) })).toBeVisible()
    await captureP14T02Screenshot(page, '01-tester-workbench-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await captureP14T02Screenshot(page, '02-tester-workbench-greenbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await page.goto('/workbench/approver')
    await expect(page.getByRole('heading', { name: /approver workbench/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.getByRole('row', { name: new RegExp(approvalTemplate.name) })).toBeVisible()
    await captureP14T02Screenshot(page, '03-approver-workbench-greenbc-1440x900.png')

    await switchBrand(page, 'REDBC')
    await captureP14T02Screenshot(page, '04-approver-workbench-redbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await page.goto('/workbench/escalation')
    await expect(page.getByRole('heading', { name: /escalation workbench/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    const escalationRow = page.getByRole('row', { name: new RegExp(escalationTemplate.name) })
    await expect(escalationRow).toBeVisible()
    await expect(escalationRow.getByText(/exceeded.*threshold/i)).toBeVisible()
    await captureP14T02Screenshot(page, '05-escalation-workbench-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await captureP14T02Screenshot(page, '06-escalation-workbench-greenbc-1440x900.png')

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
      '07-dashboard-timeout-config-panel-greenbc-1440x900.png',
    )

    await switchBrand(page, 'REDBC')
    await captureP14T02LocatorScreenshot(
      timeoutPanel,
      '08-dashboard-timeout-config-panel-redbc-1440x900.png',
    )

    await expect(timeoutPanel.getByRole('button', { name: /save thresholds/i })).toBeVisible()
    await expect(timeoutPanel.getByLabel(/testing threshold/i)).toBeVisible()
  })
})
