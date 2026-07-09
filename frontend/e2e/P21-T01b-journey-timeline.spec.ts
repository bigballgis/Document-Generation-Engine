import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T01b RoleJourneyTimeline (§12.4)', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.` })
  })

  test('template author sees journey section with six steps above tasks', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')

    const journeySection = page.locator('#journey-section')
    const tasksSection = page.locator('#tasks-section')
    await expect(journeySection).toBeVisible()
    await expect(tasksSection).toBeVisible()
    await expect(journeySection.getByRole('heading', { name: /template authoring workflow/i })).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(6)
  })

  test('approver-only session renders journey section with three steps', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await page.goto('/dashboard')

    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(3)
    await expect(page.locator('#tasks-section')).toBeVisible()
  })

  test('filtered deep link still shows journey section for tester', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')

    await expect(page.locator('#journey-section')).toBeVisible()
    await expect(page.locator('#journey-section [data-journey-step]')).toHaveCount(3)
    await expect(page.getByRole('heading', { level: 1, name: /waiting on my testing/i })).toBeVisible()
  })
})
