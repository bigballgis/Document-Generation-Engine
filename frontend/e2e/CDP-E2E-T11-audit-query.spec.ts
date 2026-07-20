import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_AUDIT_ADMIN, loginAs } from './helpers/auth'
import { expectMyTodosGroupAbsent } from './helpers/nav'
import { selectElementPlusOption } from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

const MANAGEMENT_EVENTS_PATH = '/admin/audit/management-events'
const MANAGEMENT_EXPORT_PATH = '/admin/audit/management-events/export'

/**
 * CD-E2E-T11 / CD-HARD-T06 re-evidence — Audit admin Activity log filter + export smoke.
 * BDD: docs/behavior/audit-admin-query-journey.md (BDD-CDP-AUDIT-001…002)
 * Pointer: docs/behavior/cd-hard-t06-audit-export-reevidence.md
 *
 * Harness: seed onboarding-tour dismiss for AUDIT_ADMIN so LR-C8 auto-open does not
 * `router.push('/dashboard')` (AUDIT_ADMIN has no dashboard route → Forbidden).
 * Product fix for that tour anchor belongs to frontend-engineer; out of T06 scope.
 */
test.describe('CDP-E2E-T11 Audit admin query smoke (BDD-CDP-AUDIT-001…002)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + backend :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await seedAuditAdminTourDismiss(page)
  })

  test('BDD-CDP-AUDIT-001 — filter by event type updates list; view-only; no My to-dos', async ({
    page,
  }) => {
    await loginAs(page, E2E_AUDIT_ADMIN)
    await page.goto('/audit')

    await expect(page.getByRole('heading', { level: 1, name: /^activity log$/i })).toBeVisible()
    await expect(page.getByText(/view only — no actions/i).first()).toBeVisible()
    await expectMyTodosGroupAbsent(page)

    // Prefer Management tab (smoke: either Management or Lifecycle is OK).
    const managementTab = page.getByRole('tab', { name: /management activity/i })
    await expect(managementTab).toBeVisible()
    await managementTab.click()

    await expect(page.getByRole('button', { name: /^apply filters$/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    // Event type filter (and/or date — event type alone satisfies BDD).
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
    expect(filterResponse.status()).not.toBe(401)
    expect(filterResponse.status()).not.toBe(403)
    expect(filterResponse.ok()).toBeTruthy()

    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    // List updated: table rows or legitimate empty state (both OK for smoke).
    const tableOrEmpty = page.locator('.app-data-table, .el-table, .el-empty').first()
    await expect(tableOrEmpty).toBeVisible()
    await expect(page.getByText(/unable to load management activity/i)).toHaveCount(0)
  })

  test('BDD-CDP-AUDIT-002 — Export confirm triggers JSON download (not 403)', async ({
    page,
  }) => {
    await loginAs(page, E2E_AUDIT_ADMIN)
    await openActivityLogManagement(page)

    const exportButton = page.getByRole('button', { name: /^export$/i })
    await expect(exportButton).toBeVisible()

    await exportButton.click()
    const confirmBox = page.locator('.el-message-box')
    await expect(confirmBox).toBeVisible()
    await expect(confirmBox.getByText(/export activity records/i)).toBeVisible()

    const exportResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' && response.url().includes(MANAGEMENT_EXPORT_PATH),
      { timeout: 30_000 },
    )
    const downloadPromise = page.waitForEvent('download', { timeout: 30_000 })

    await confirmBox.getByRole('button', { name: /^download export$/i }).click()

    const exportResponse = await exportResponsePromise
    expect(exportResponse.status()).not.toBe(401)
    expect(exportResponse.status()).not.toBe(403)
    expect(exportResponse.ok()).toBeTruthy()

    const download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/management-audit-export\.json/i)

    await expect(
      page.locator('.el-message').getByText(/management activity export downloaded/i),
    ).toBeVisible({ timeout: 15_000 })
  })
})

async function seedAuditAdminTourDismiss(page: Page) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await page.evaluate((username) => {
    localStorage.setItem(`docgen.onboardingTour.dismissed.v1:${username}`, '1')
  }, E2E_AUDIT_ADMIN.username)
}

async function openActivityLogManagement(page: Page) {
  await page.goto('/audit')
  await expect(page.getByRole('heading', { level: 1, name: /^activity log$/i })).toBeVisible()
  await page.getByRole('tab', { name: /management activity/i }).click()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  await expect(page.getByRole('button', { name: /^export$/i })).toBeVisible()
}
