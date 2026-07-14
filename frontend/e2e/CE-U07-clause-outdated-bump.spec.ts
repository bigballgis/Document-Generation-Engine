import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { prepareDraftTemplateWithOutdatedClauseReference } from './helpers/content-modules-api'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

/**
 * CE-U07 — clause out-of-date badge + one-click bump + dashboard author todo.
 * BDD: docs/behavior/ce-u07-clause-outdated-bump.md (BDD-CE-U07-COB-001…004)
 */
test.describe('CE-U07 clause outdated bump (BDD-CE-U07-COB)', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('BDD-CE-U07-COB-001/002 — out-of-date badge and one-click bump', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithOutdatedClauseReference(request)

    await page.goto(
      `/templates/${fixture.templateId}/dev/${fixture.inFlightDevVersionId}?workspaceTab=design&designTab=contentModules`,
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await page.locator('.design-sub-tabs').getByRole('tab', { name: /^clause references$/i }).click()

    const outdatedBadge = page.getByTestId('clause-reference-outdated-badge')
    await expect(outdatedBadge).toBeVisible({ timeout: 30_000 })

    const bumpButton = page.getByTestId('clause-reference-bump-button')
    await expect(bumpButton).toBeVisible()
    await bumpButton.click()

    await expect(outdatedBadge).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByText('1.1.0')).toBeVisible()
  })

  test('BDD-CE-U07-COB-004 — dashboard author todo deep links to clause panel', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithOutdatedClauseReference(request)

    await page.goto('/dashboard#tasks-section')
    const skipTour = page.getByTestId('onboarding-tour-skip')
    if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await skipTour.click()
      await expect(skipTour).toHaveCount(0)
    }

    const tasks = page.locator('#tasks-section')
    await expect(tasks).toBeVisible({ timeout: 30_000 })
    await expect(
      tasks.getByRole('heading', { name: /clause references to update/i }),
    ).toBeVisible({ timeout: 30_000 })
    await expect(tasks.getByText(fixture.externalId, { exact: false })).toBeVisible({
      timeout: 30_000,
    })

    const row = tasks.getByRole('row', { name: new RegExp(fixture.externalId, 'i') })
    await row.getByRole('button', { name: /^open$/i }).click()

    await expect(page).toHaveURL(
      new RegExp(
        `/templates/${fixture.templateId}/dev/${fixture.inFlightDevVersionId}.*designTab=contentModules`,
      ),
      { timeout: 30_000 },
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('clause-reference-outdated-badge')).toBeVisible({
      timeout: 30_000,
    })
  })
})
