/**
 * CE-U08 UIUX evidence — content-module review loop
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: CMRL-001…007 (dashboard partitions, lifecycle, rejectionReason column, timeline)
 */
import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import {
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  createRejectedContentModuleForRework,
  createSubmittedContentModuleForReview,
} from './helpers/content-modules-api'
import {
  captureCeU08LocatorScreenshot,
  captureCeU08Screenshot,
  CE_U08_VIEWPORT,
  ensureCeU08EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openDashboardTasks(page: Page) {
  await page.goto('/dashboard#tasks-section')
  await dismissOnboardingTourIfPresent(page)
  const tasks = page.locator('#tasks-section')
  await expect(tasks).toBeVisible({ timeout: 30_000 })
  return tasks
}

test.describe('CE-U08 content module review loop UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeU08EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
  })

  test('01–04 dual-brand: dashboard review partition + lifecycle decision rail', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U08_VIEWPORT)
    const fixture = await createSubmittedContentModuleForReview(request, {
      name: `E2E CMRL UIUX Review ${Date.now()}`,
    })

    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    const tasks = await openDashboardTasks(page)
    const reviewPartition = tasks.locator('[data-partition-id="content-module-review"]')
    await expect(
      reviewPartition.getByRole('heading', { name: /standard clauses to review/i }),
    ).toBeVisible({ timeout: 30_000 })
    await expect(reviewPartition.getByText(fixture.name, { exact: false })).toBeVisible({
      timeout: 30_000,
    })

    await captureCeU08Screenshot(page, '01-dashboard-cm-review-redbc-1920x1080.png')
    await captureCeU08LocatorScreenshot(
      reviewPartition,
      '01b-cm-review-partition-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(reviewPartition.getByText(fixture.name, { exact: false })).toBeVisible()
    await captureCeU08Screenshot(page, '02-dashboard-cm-review-greenbc-1920x1080.png')
    await captureCeU08LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02b-brand-header-greenbc-crop.png',
    )

    await switchBrand(page, 'REDBC')
    const row = reviewPartition.getByRole('row', { name: new RegExp(fixture.name, 'i') })
    await row.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(
      new RegExp(`/content-modules/${fixture.moduleId}.*workspaceTab=lifecycle`),
      { timeout: 30_000 },
    )
    await expect(page.getByRole('button', { name: /^approve$/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /^reject$/i })).toBeVisible()
    await expect(page.locator('.el-timeline, .history-card').first()).toBeVisible({
      timeout: 15_000,
    })
    await captureCeU08Screenshot(page, '03-lifecycle-approve-reject-redbc-1920x1080.png')

    await switchBrand(page, 'GREENBC')
    await expect(page.getByRole('button', { name: /^approve$/i })).toBeVisible()
    await captureCeU08Screenshot(page, '04-lifecycle-approve-reject-greenbc-1920x1080.png')
  })

  test('05–08 dual-brand: rework partition, rejection reason, versions column', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U08_VIEWPORT)
    const fixture = await createRejectedContentModuleForRework(request, {
      name: `E2E CMRL UIUX Rework ${Date.now()}`,
      rejectionReason: 'Wording not acceptable',
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    const tasks = await openDashboardTasks(page)
    const reworkPartition = tasks.locator('[data-partition-id="content-module-rework"]')
    await expect(
      reworkPartition.getByRole('heading', { name: /standard clauses to fix/i }),
    ).toBeVisible({ timeout: 30_000 })
    await expect(reworkPartition.getByText(fixture.name, { exact: false })).toBeVisible({
      timeout: 30_000,
    })
    await captureCeU08Screenshot(page, '05-dashboard-cm-rework-redbc-1920x1080.png')
    await captureCeU08LocatorScreenshot(
      reworkPartition,
      '05b-cm-rework-partition-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await captureCeU08Screenshot(page, '06-dashboard-cm-rework-greenbc-1920x1080.png')

    await switchBrand(page, 'REDBC')
    const row = reworkPartition.getByRole('row', { name: new RegExp(fixture.name, 'i') })
    await row.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(
      new RegExp(`/content-modules/${fixture.moduleId}.*workspaceTab=lifecycle`),
      { timeout: 30_000 },
    )
    await expect(page.getByText(fixture.rejectionReason, { exact: false })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByRole('button', { name: /submit for approval/i })).toBeVisible()
    await captureCeU08Screenshot(page, '07-lifecycle-rejection-rework-redbc-1920x1080.png')

    await page.goto(`/content-modules/${fixture.moduleId}?workspaceTab=versions`)
    await expect(page.getByRole('heading', { level: 1, name: fixture.name })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByText(/^rejection reason$/i).first()).toBeVisible()
    await expect(page.getByText(fixture.rejectionReason, { exact: true })).toBeVisible()
    await captureCeU08Screenshot(page, '08-versions-rejection-reason-redbc-1920x1080.png')

    await switchBrand(page, 'GREENBC')
    await expect(page.getByText(fixture.rejectionReason, { exact: true })).toBeVisible()
    await captureCeU08Screenshot(page, '09-versions-rejection-reason-greenbc-1920x1080.png')
    await captureCeU08LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '09b-brand-header-greenbc-crop.png',
    )
    await switchBrand(page, 'REDBC')
    await captureCeU08LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '10-brand-header-redbc-crop.png',
    )
  })
})
