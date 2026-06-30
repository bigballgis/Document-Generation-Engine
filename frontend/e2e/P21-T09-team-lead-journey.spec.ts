import { expect, test } from '@playwright/test'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { demoPendingReleaseTemplateDetailPath } from './helpers/content-modules-api'
import { BEHAVIOR_NAV_LABELS, myTodosNavSection } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T09 Team-lead go-live journey (§12.9)', () => {
  test.beforeAll(async ({ request }) => {
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
      `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.`,
    )
  })

  test('group admin dashboard journey reflects catalog with four steps', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard')

    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(
      journeySection.getByRole('heading', { name: /team-lead go-live workflow/i }),
    ).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(4)
    await expect(journeySection.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('template detail shows team-lead journey timeline above workflow banner', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    const detailPath = await demoPendingReleaseTemplateDetailPath(request)
    await page.goto(detailPath)

    const timeline = page.getByRole('heading', { name: /team-lead go-live workflow/i }).locator('..')
    await expect(timeline).toBeVisible()
    await expect(timeline.locator('[data-journey-step]')).toHaveCount(4)
    await expect(timeline.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('behavior nav pendingRelease label is Waiting to confirm go-live', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await expect(
      myTodosNavSection(page).getByRole('button', { name: BEHAVIOR_NAV_LABELS.pendingRelease }),
    ).toBeVisible()
  })

  test('filtered PENDING_RELEASE hub shows journey section and waiting to confirm go-live title', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard?queue=PENDING_RELEASE#tasks-section')

    await expect(page.locator('#journey-section')).toBeVisible()
    await expect(
      page.getByRole('heading', { level: 1, name: /waiting to confirm go-live/i }),
    ).toBeVisible()
  })
})
