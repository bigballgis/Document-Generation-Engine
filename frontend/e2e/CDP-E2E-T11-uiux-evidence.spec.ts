import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_AUDIT_ADMIN, loginAs } from './helpers/auth'
import { expectMyTodosGroupAbsent } from './helpers/nav'
import { selectElementPlusOption } from './helpers/ui'
import {
  captureCdpE2eDecisionLocatorScreenshot,
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`
const TASK_ID = 'CDP-E2E-T11' as const

const MANAGEMENT_EVENTS_PATH = '/admin/audit/management-events'

/**
 * CD-E2E-T11 UIUX evidence — Audit admin Activity log filter + export @1920 REDBC
 * (BDD-CDP-AUDIT-001…002). Follows T09/T10 capture helpers + viewport; REDBC-only
 * (GREENBC optional / deferred to T12 dual-brand golden).
 */
test.describe('CDP-E2E-T11 UIUX evidence — audit Activity log @1920 (BDD-CDP-AUDIT-001…002)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + backend :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture Activity log filters, table/empty, view-only, export (REDBC @1920)', async ({
    page,
  }) => {
    await loginAs(page, E2E_AUDIT_ADMIN)
    await switchBrand(page, 'REDBC')
    await openActivityLogManagement(page)

    await expect(page.getByText(/view only — no actions/i).first()).toBeVisible()
    await expectMyTodosGroupAbsent(page)
    await expect(page.getByRole('button', { name: /^export$/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /^apply filters$/i })).toBeVisible()

    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-activity-log-shell-view-only-filters-redbc-1920x1080.png',
    )

    const filtersCard = page.locator('.filters-card')
    await expect(filtersCard.getByText(/^event type$/i)).toBeVisible()
    await captureCdpE2eDecisionLocatorScreenshot(
      filtersCard,
      TASK_ID,
      '02-activity-log-filters-card-redbc-1920x1080.png',
    )

    const eventTypeField = page
      .locator('.el-form-item')
      .filter({ has: page.getByText(/^event type$/i) })
      .locator('.el-select')
      .first()
    await eventTypeField.click()
    await selectElementPlusOption(page, /template go-live/i)

    const filterResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes(MANAGEMENT_EVENTS_PATH) &&
        !response.url().includes('/export') &&
        response.url().includes('eventType='),
      { timeout: 30_000 },
    )
    await page.getByRole('button', { name: /^apply filters$/i }).click()
    const filterResponse = await filterResponsePromise
    expect(filterResponse.ok()).toBeTruthy()

    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const tableOrEmpty = page.locator('.app-data-table, .el-table, .el-empty').first()
    await expect(tableOrEmpty).toBeVisible()

    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '03-activity-log-filtered-table-or-empty-redbc-1920x1080.png',
    )

    const exportButton = page.getByRole('button', { name: /^export$/i })
    await exportButton.click()
    const confirmBox = page.locator('.el-message-box')
    await expect(confirmBox).toBeVisible()
    await expect(confirmBox.getByText(/export activity records/i)).toBeVisible()
    await expect(confirmBox.getByRole('button', { name: /^download export$/i })).toBeVisible()

    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '04-activity-log-export-confirm-redbc-1920x1080.png',
    )
    await captureCdpE2eDecisionLocatorScreenshot(
      confirmBox,
      TASK_ID,
      '05-activity-log-export-confirm-detail-redbc-1920x1080.png',
    )

    // Dismiss without downloading — functional E2E covers download path.
    await confirmBox.getByRole('button', { name: /^cancel$/i }).click()
    await expect(confirmBox).toHaveCount(0)
  })
})

async function openActivityLogManagement(page: Page) {
  await page.goto('/audit')
  await expect(page.getByRole('heading', { level: 1, name: /^activity log$/i })).toBeVisible()
  await page.getByRole('tab', { name: /management activity/i }).click()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  await expect(page.getByRole('button', { name: /^export$/i })).toBeVisible()
}
