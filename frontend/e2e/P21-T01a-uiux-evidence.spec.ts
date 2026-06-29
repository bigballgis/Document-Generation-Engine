import { expect, test } from '@playwright/test'

import { E2E_GROUP_ADMIN, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import {
  ageCollaborationWorkItem,
  prepareOverdueTestWorkItem,
  prepareTemplateInTesting,
  requireOpenTestWorkItemForTemplate,
  seedCollaborationWorkItem,
  seedEscalationFromOverdueSource,
} from './helpers/collaboration-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  dashboardTaskRow,
  filterDashboardTasksByItem,
  reLoginAs,
} from './helpers/ui'
import {
  captureP21T01aLocatorScreenshot,
  captureP21T01aScreenshot,
  ensureP21T01aEvidenceDirs,
  P21_T01A_VIEWPORT,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T01a UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    ensureP21T01aEvidenceDirs()

    let backendReady = false
    let frontendReady = false
    try {
      const backend = await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })
      backendReady = backend.ok()
    } catch {
      backendReady = false
    }
    try {
      const frontend = await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })
      frontendReady = frontend.ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start backend on :8080 and pnpm dev on :5173 (or docker on :4173).`,
    )
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P21_T01A_VIEWPORT)
  })

  test('capture task hub deepening evidence — partitions, columns, overdue badge, Open, segmented error', async ({
    page,
    request,
  }) => {
    const displayTemplate = await prepareTemplateInTesting(request, {
      name: `E2E UIUX P21-T01a Display ${Date.now()}`,
    })
    const displayWorkItem = await requireOpenTestWorkItemForTemplate(request, displayTemplate)
    ageCollaborationWorkItem(displayWorkItem.workItemId, "INTERVAL '2 hours'")

    const approvalTemplate = await prepareTemplateInTesting(request, {
      name: `E2E UIUX P21-T01a Approval ${Date.now()}`,
    })
    seedCollaborationWorkItem({
      templateId: approvalTemplate.templateId,
      templateExternalId: approvalTemplate.externalId,
      templateName: approvalTemplate.name,
      queue: 'APPROVAL',
      createdAgeInterval: "INTERVAL '5 hours'",
    })

    const { template: escalationTemplate, sourceWorkItem } =
      await prepareOverdueTestWorkItem(request)
    seedEscalationFromOverdueSource(sourceWorkItem, escalationTemplate)

    const { template: overdueTestTemplate } = await prepareOverdueTestWorkItem(request)

    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard#tasks-section')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.locator('[data-partition-id^="queue-"]').first()).toBeVisible({
      timeout: 30_000,
    })
    expect(await page.locator('[data-partition-id^="queue-"]').count()).toBeGreaterThanOrEqual(3)
    await captureP21T01aScreenshot(page, '01-unfiltered-hub-partitions-redbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await expect(
      page.getByRole('heading', { level: 1, name: /waiting on my testing/i }),
    ).toBeVisible()
    await expect(page.locator('[data-partition-id="queue-TEST"]')).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await filterDashboardTasksByItem(page, displayTemplate.name)
    const displayRow = await dashboardTaskRow(page, displayTemplate.name)
    await expect(displayRow).toBeVisible({ timeout: 30_000 })
    await expect(displayRow.locator('.summary-cell')).toContainText(/template submitted for testing/i)
    await expect(displayRow.getByText('2h', { exact: true })).toBeVisible()
    await captureP21T01aScreenshot(page, '02-queue-test-landing-title-redbc-1440x900.png')

    await captureP21T01aLocatorScreenshot(
      page.locator('[data-partition-id="queue-TEST"]'),
      '03-test-partition-restored-columns-redbc-1440x900.png',
    )

    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await filterDashboardTasksByItem(page, overdueTestTemplate.name)
    const overdueRow = await dashboardTaskRow(page, overdueTestTemplate.name)
    await expect(overdueRow).toBeVisible({ timeout: 30_000 })
    await expect(overdueRow.getByText(/overdue reminder/i)).toBeVisible()
    await captureP21T01aLocatorScreenshot(
      overdueRow,
      '04-overdue-badge-test-aged-redbc-1440x900.png',
    )

    await page.goto('/dashboard?queue=ESCALATION#tasks-section')
    await expect(
      page.getByRole('heading', { level: 1, name: /overdue to follow up/i }),
    ).toBeVisible()
    await expect(page.locator('[data-partition-id="queue-ESCALATION"]')).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await filterDashboardTasksByItem(page, escalationTemplate.name)
    const escalationRow = await dashboardTaskRow(page, escalationTemplate.name)
    await expect(escalationRow).toBeVisible({ timeout: 30_000 })
    await expect(escalationRow.getByText(/overdue reminder/i)).toBeVisible()
    await captureP21T01aScreenshot(page, '05-escalation-partition-overdue-badge-redbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await filterDashboardTasksByItem(page, displayTemplate.name)
    const openRow = await dashboardTaskRow(page, displayTemplate.name)
    await expect(openRow.getByRole('button', { name: /^open$/i })).toBeVisible()
    await openRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/\/templates\/[^/?]+\?tab=lifecycle/)
    await expect(page.getByText(/an internal error occurred/i)).not.toBeVisible()
    await expect(page.locator('#template-lifecycle-panel')).toBeVisible({ timeout: 15_000 })
    await captureP21T01aScreenshot(page, '06-open-button-lifecycle-tab-redbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await page.route('**/collaboration-work-items**', (route) =>
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-uiux-collab-fail' },
          error: {
            code: 'INTERNAL_ERROR',
            category: 'SYSTEM',
            retryable: true,
            message: 'Unable to load collaboration to-do items.',
            messageKey: 'collaboration.workItems.error.load',
          },
        }),
      }),
    )
    await page.goto('/dashboard#tasks-section')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.locator('.dashboard-stats')).toBeVisible()
    await expect(page.getByText(/unable to load collaboration to-do items/i)).toBeVisible()
    await expect(page.locator('#tasks-section')).toBeVisible()
    await expect(page).not.toHaveURL(/\/forbidden/)
    await captureP21T01aScreenshot(page, '07-collaboration-segmented-error-redbc-1440x900.png')
  })
})
