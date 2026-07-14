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
  getContentModuleDetailViaApi,
} from './helpers/content-modules-api'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openDashboardTasks(page: Page): Promise<ReturnType<Page['locator']>> {
  await page.goto('/dashboard#tasks-section')
  await dismissOnboardingTourIfPresent(page)
  const tasks = page.locator('#tasks-section')
  await expect(tasks).toBeVisible({ timeout: 30_000 })
  return tasks
}

/**
 * CE-U08 — content-module review loop (Dashboard todos + rejection reason).
 * BDD: docs/behavior/ce-u08-content-module-review-loop.md (BDD-CE-U08-CMRL-001…007)
 */
test.describe('CE-U08 content module review loop (BDD-CE-U08-CMRL)', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-CE-U08-CMRL-001 — dashboard pending-review todo deep links to lifecycle', async ({
    page,
    request,
  }) => {
    const fixture = await createSubmittedContentModuleForReview(request, {
      name: `E2E CMRL Review ${Date.now()}`,
    })

    await loginAs(page, E2E_TEMPLATE_APPROVER)
    const tasks = await openDashboardTasks(page)

    const reviewPartition = tasks.locator('[data-partition-id="content-module-review"]')
    await expect(reviewPartition.getByRole('heading', { name: /standard clauses to review/i })).toBeVisible({
      timeout: 30_000,
    })
    await expect(reviewPartition.getByText(fixture.name, { exact: false })).toBeVisible({
      timeout: 30_000,
    })

    const row = reviewPartition.getByRole('row', { name: new RegExp(fixture.name, 'i') })
    await row.getByRole('button', { name: /^open$/i }).click()

    await expect(page).toHaveURL(
      new RegExp(`/content-modules/${fixture.moduleId}.*workspaceTab=lifecycle`),
      { timeout: 30_000 },
    )
    await expect(page.getByRole('heading', { level: 1, name: fixture.name })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByRole('button', { name: /^approve$/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /^reject$/i })).toBeVisible()
  })

  test('BDD-CE-U08-CMRL-002 — dashboard rework todo shows rejection and lifecycle', async ({
    page,
    request,
  }) => {
    const fixture = await createRejectedContentModuleForRework(request, {
      name: `E2E CMRL Rework ${Date.now()}`,
      rejectionReason: 'Wording not acceptable',
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const tasks = await openDashboardTasks(page)

    const reworkPartition = tasks.locator('[data-partition-id="content-module-rework"]')
    await expect(reworkPartition.getByRole('heading', { name: /standard clauses to fix/i })).toBeVisible({
      timeout: 30_000,
    })
    await expect(reworkPartition.getByText(fixture.name, { exact: false })).toBeVisible({
      timeout: 30_000,
    })

    const row = reworkPartition.getByRole('row', { name: new RegExp(fixture.name, 'i') })
    await row.getByRole('button', { name: /^open$/i }).click()

    await expect(page).toHaveURL(
      new RegExp(`/content-modules/${fixture.moduleId}.*workspaceTab=lifecycle`),
      { timeout: 30_000 },
    )
    await expect(page.getByRole('heading', { level: 1, name: fixture.name })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByText(fixture.rejectionReason, { exact: false })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByRole('button', { name: /submit for approval/i })).toBeVisible()
  })

  test('BDD-CE-U08-CMRL-003 — versions table shows rejectionReason after reject', async ({
    page,
    request,
  }) => {
    const fixture = await createRejectedContentModuleForRework(request, {
      name: `E2E CMRL Rejection Column ${Date.now()}`,
      rejectionReason: 'Wording not acceptable',
    })

    const detail = await getContentModuleDetailViaApi(request, fixture.moduleId)
    const rejectedVersion = detail.versions.find(
      (version) => version.reviewState === 'DRAFT' && version.rejectionReason === fixture.rejectionReason,
    )
    expect(rejectedVersion).toBeTruthy()
    expect(rejectedVersion?.rejectionReason).toBe(fixture.rejectionReason)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(`/content-modules/${fixture.moduleId}?workspaceTab=versions`)
    await expect(page.getByRole('heading', { level: 1, name: fixture.name })).toBeVisible({
      timeout: 30_000,
    })

    await expect(page.getByText(/^rejection reason$/i).first()).toBeVisible()
    await expect(page.getByText(/^draft$/i).first()).toBeVisible()
    await expect(page.getByText(fixture.rejectionReason, { exact: true })).toBeVisible()
  })
})
