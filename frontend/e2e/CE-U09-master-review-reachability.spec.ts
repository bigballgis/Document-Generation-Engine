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
  getMasterDetailViaApi,
} from './helpers/masters-api'

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

/** Hub page-header action rail (excludes journey step/CTA duplicates). */
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

/**
 * CE-U09 — master review reachability (Hub CTAs + dashboard deep link).
 * BDD: docs/behavior/ce-u09-master-review-reachability.md (BDD-CE-U09-MRR-001…007)
 */
test.describe('CE-U09 master review reachability (BDD-CE-U09-MRR)', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-CE-U09-MRR-001 — Hub Submit for review on DRAFT current', async ({
    page,
    request,
  }) => {
    const fixture = await createDraftMasterForHubSubmit(request, {
      name: `E2E-MRR-001-Submit ${Date.now()}`,
    })

    await loginAs(page, E2E_MASTER_DESIGNER)
    await openMasterHub(page, fixture.hubPath, fixture.name)

    await expect(page.getByText(/^draft$/i).first()).toBeVisible()
    const submitButton = hubActionButton(page, /^submit for review$/i)
    await expect(submitButton).toBeVisible()
    await expect(page.locator('[data-master-journey-cta]')).toBeVisible()

    await submitButton.click()
    const submitDialog = page.locator('.el-dialog').filter({
      hasText: /submit letterhead for review/i,
    })
    await expect(submitDialog).toBeVisible()
    await submitDialog.locator('textarea').fill('CE-U09 Hub submit change summary')
    await submitDialog.getByRole('button', { name: /^submit$/i }).click()

    await expect(
      page.locator('.el-message').getByText(/letterhead submitted for review/i),
    ).toBeVisible({ timeout: 30_000 })
    await expect(page.getByText(/^pending review$/i).first()).toBeVisible({ timeout: 30_000 })
    await expect(hubActionButton(page, /^submit for review$/i)).toHaveCount(0)

    const detail = await getMasterDetailViaApi(request, fixture.masterId)
    expect(detail.status).toBe('PENDING_REVIEW')
  })

  test('BDD-CE-U09-MRR-002 — Hub Approve on PENDING_REVIEW', async ({ page, request }) => {
    const fixture = await createPendingReviewMasterForDecide(request, {
      name: `E2E-MRR-002-Approve ${Date.now()}`,
    })

    await loginAs(page, E2E_GROUP_ADMIN)
    await openMasterHub(page, fixture.hubPath, fixture.name)

    await expect(page.getByText(/^pending review$/i).first()).toBeVisible()
    await expect(hubActionButton(page, /^approve$/i)).toBeVisible()
    await expect(hubActionButton(page, /^reject$/i)).toBeVisible()
    await expect(hubActionButton(page, /^submit for review$/i)).toHaveCount(0)

    await hubActionButton(page, /^approve$/i).click()
    const approveDialog = page.locator('.el-dialog').filter({ hasText: /approve letterhead/i })
    await expect(approveDialog).toBeVisible()
    await approveDialog.locator('textarea').fill('CE-U09 Hub approve')
    await approveDialog.getByRole('button', { name: /^approve$/i }).click()

    await expect(page.locator('.el-message').getByText(/letterhead approved/i)).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByText(/^approved$/i).first()).toBeVisible({ timeout: 30_000 })
    await expect(hubActionButton(page, /^approve$/i)).toHaveCount(0)

    const detail = await getMasterDetailViaApi(request, fixture.masterId)
    expect(detail.status).toBe('APPROVED')
  })

  test('BDD-CE-U09-MRR-003 — fail-closed: designer has no Approve/Reject on pending Hub', async ({
    page,
    request,
  }) => {
    const fixture = await createPendingReviewMasterForDecide(request, {
      name: `E2E-MRR-003-FailClosed ${Date.now()}`,
    })

    await loginAs(page, E2E_MASTER_DESIGNER)
    await openMasterHub(page, fixture.hubPath, fixture.name)

    await expect(page.getByText(/^pending review$/i).first()).toBeVisible()
    await expect(hubActionButton(page, /^approve$/i)).toHaveCount(0)
    await expect(hubActionButton(page, /^reject$/i)).toHaveCount(0)
    await expect(hubActionButton(page, /^submit for review$/i)).toHaveCount(0)
  })

  test('BDD-CE-U09-MRR-004 — Dashboard master-review deep links to approval tab', async ({
    page,
    request,
  }) => {
    const fixture = await createPendingReviewMasterForDecide(request, {
      name: `E2E-MRR-004-DeepLink ${Date.now()}`,
    })

    await loginAs(page, E2E_GROUP_ADMIN)
    const tasks = await openDashboardMasterReviewTasks(page)

    const reviewPartition = tasks.locator('[data-partition-id="master-review"]')
    await expect(reviewPartition.getByRole('heading', { name: /review letterhead/i })).toBeVisible({
      timeout: 30_000,
    })
    await expect(reviewPartition.getByText(fixture.name, { exact: false })).toBeVisible({
      timeout: 30_000,
    })

    const row = reviewPartition.getByRole('row', { name: new RegExp(fixture.name, 'i') })
    await row.getByRole('button', { name: /^open$/i }).click()

    await expect(page).toHaveURL(
      new RegExp(
        `/masters/${fixture.masterId}/revisions/${fixture.currentRevisionLineId}.*workspaceTab=approval`,
      ),
      { timeout: 30_000 },
    )
    await expect(page.getByRole('tab', { name: /letterhead review/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(
      page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /^approve$/i }),
    ).toBeVisible()
    await expect(
      page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /^reject$/i }),
    ).toBeVisible()
  })
})
