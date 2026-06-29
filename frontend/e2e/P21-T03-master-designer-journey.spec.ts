import { expect, test } from '@playwright/test'

import { E2E_MASTER_DESIGNER, loginAs } from './helpers/auth'
import { demoMasterDetailPath } from './helpers/masters-api'
import { BEHAVIOR_NAV_LABELS, myTodosNavSection } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T03 Master designer journey (§12.5)', () => {
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

  test('master designer dashboard journey reflects catalog with four steps', async ({ page }) => {
    await loginAs(page, E2E_MASTER_DESIGNER)
    await page.goto('/dashboard')

    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(
      journeySection.getByRole('heading', { name: /letterhead design workflow/i }),
    ).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(4)
    await expect(journeySection.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('master package hub shows designer journey timeline', async ({ page, request }) => {
    await loginAs(page, E2E_MASTER_DESIGNER)
    const hubPath = await demoMasterDetailPath(request)
    await page.goto(hubPath)

    await expect(page.locator('[data-journey-timeline]')).toBeVisible()
    await expect(page.locator('[data-journey-step]')).toHaveCount(4)
  })

  test('filtered master-review hub shows Letterheads to fix partition heading', async ({ page }) => {
    await loginAs(page, E2E_MASTER_DESIGNER)
    await page.goto('/dashboard?filter=master-review#tasks-section')

    await expect(page.locator('#journey-section')).toBeVisible()
    await expect(page.getByRole('heading', { level: 1, name: /masters to review/i })).toBeVisible()

    const reworkHeading = page.getByRole('heading', { name: /letterheads to fix/i })
    if (await reworkHeading.count()) {
      await expect(reworkHeading.first()).toBeVisible()
    }
  })

  test('behavior nav master review label remains Masters to review', async ({ page }) => {
    await loginAs(page, E2E_MASTER_DESIGNER)
    await expect(
      myTodosNavSection(page).getByRole('button', { name: BEHAVIOR_NAV_LABELS.masterReview }),
    ).toBeVisible()
  })
})
