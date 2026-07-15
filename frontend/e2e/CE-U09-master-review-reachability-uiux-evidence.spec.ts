/**
 * CE-U09 UIUX evidence — master review reachability
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: MRR-001…004 (Hub submit/approve + dashboard deep link)
 */
import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import {
  E2E_GROUP_ADMIN,
  E2E_MASTER_DESIGNER,
  loginAs,
} from './helpers/auth'
import {
  E2E_API_BASE_URL,
  createDraftMasterForHubSubmit,
  createPendingReviewMasterForDecide,
} from './helpers/masters-api'
import {
  captureCeU09LocatorScreenshot,
  captureCeU09Screenshot,
  CE_U09_VIEWPORT,
  ensureCeU09EvidenceDirs,
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

async function openMasterHub(page: Page, hubPath: string, masterName: string): Promise<void> {
  await page.goto(hubPath)
  await dismissOnboardingTourIfPresent(page)
  await expect(page.getByRole('heading', { level: 1, name: masterName })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
}

function hubActionButton(page: Page, name: RegExp) {
  return page.locator('.page-header__actions').getByRole('button', { name })
}

async function openDashboardMasterReviewTasks(page: Page) {
  await page.goto('/dashboard?filter=master-review#tasks-section')
  await dismissOnboardingTourIfPresent(page)
  const tasks = page.locator('#tasks-section')
  await expect(tasks).toBeVisible({ timeout: 30_000 })
  return tasks
}

test.describe('CE-U09 master review reachability UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeU09EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
  })

  test('01–04 dual-brand: Hub Submit for review + journey CTA', async ({ page, request }) => {
    await page.setViewportSize(CE_U09_VIEWPORT)
    const fixture = await createDraftMasterForHubSubmit(request, {
      name: `E2E-MRR-UIUX-Submit ${Date.now()}`,
    })

    await loginAs(page, E2E_MASTER_DESIGNER)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openMasterHub(page, fixture.hubPath, fixture.name)
    await expect(hubActionButton(page, /^submit for review$/i)).toBeVisible()
    await expect(page.locator('[data-master-journey-cta]')).toBeVisible()

    await captureCeU09Screenshot(page, '01-hub-submit-review-redbc-1920x1080.png')
    await captureCeU09LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01b-brand-header-redbc-crop.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(hubActionButton(page, /^submit for review$/i)).toBeVisible()
    await captureCeU09Screenshot(page, '02-hub-submit-review-greenbc-1920x1080.png')
    await captureCeU09LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02b-brand-header-greenbc-crop.png',
    )

    await switchBrand(page, 'REDBC')
    await hubActionButton(page, /^submit for review$/i).click()
    const submitDialog = page.locator('.el-dialog').filter({
      hasText: /submit letterhead for review/i,
    })
    await expect(submitDialog).toBeVisible()
    await captureCeU09Screenshot(page, '03-hub-submit-dialog-redbc-1920x1080.png')
    await submitDialog.getByRole('button', { name: /^cancel$/i }).click()
  })

  test('05–08 dual-brand: Hub Approve/Reject + dashboard deep link landing', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U09_VIEWPORT)
    const fixture = await createPendingReviewMasterForDecide(request, {
      name: `E2E-MRR-UIUX-Decide ${Date.now()}`,
    })

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    await openMasterHub(page, fixture.hubPath, fixture.name)
    await expect(hubActionButton(page, /^approve$/i)).toBeVisible()
    await expect(hubActionButton(page, /^reject$/i)).toBeVisible()
    await captureCeU09Screenshot(page, '04-hub-approve-reject-redbc-1920x1080.png')

    await switchBrand(page, 'GREENBC')
    await expect(hubActionButton(page, /^approve$/i)).toBeVisible()
    await captureCeU09Screenshot(page, '05-hub-approve-reject-greenbc-1920x1080.png')

    await switchBrand(page, 'REDBC')
    const tasks = await openDashboardMasterReviewTasks(page)
    const reviewPartition = tasks.locator('[data-partition-id="master-review"]')
    await expect(reviewPartition.getByText(fixture.name, { exact: false })).toBeVisible({
      timeout: 30_000,
    })
    await captureCeU09Screenshot(page, '06-dashboard-master-review-redbc-1920x1080.png')
    await captureCeU09LocatorScreenshot(
      reviewPartition,
      '06b-master-review-partition-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await captureCeU09Screenshot(page, '07-dashboard-master-review-greenbc-1920x1080.png')

    await switchBrand(page, 'REDBC')
    const row = reviewPartition.getByRole('row', { name: new RegExp(fixture.name, 'i') })
    await row.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/workspaceTab=approval/, { timeout: 30_000 })
    await expect(
      page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /^approve$/i }),
    ).toBeVisible()
    await captureCeU09Screenshot(page, '08-approval-tab-deep-link-redbc-1920x1080.png')

    await switchBrand(page, 'GREENBC')
    await expect(
      page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /^approve$/i }),
    ).toBeVisible()
    await captureCeU09Screenshot(page, '09-approval-tab-deep-link-greenbc-1920x1080.png')
    await captureCeU09LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '09b-brand-header-greenbc-crop.png',
    )
  })
})
