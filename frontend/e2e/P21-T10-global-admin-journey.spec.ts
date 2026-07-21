import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { loginAsGlobalAdmin } from './helpers/auth'
import { BEHAVIOR_NAV_LABELS, myTodosNavSection } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T10 Global admin journey (§12.10)', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.` })
  })

  test('global admin dashboard journey reflects catalog with six steps', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await page.goto('/dashboard')

    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(
      journeySection.getByRole('heading', { name: /bank-wide administration workflow/i }),
    ).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(6)
    await expect(journeySection.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('unfiltered dashboard shows bank-wide task hub sections', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await page.goto('/dashboard')

    await expect(page.locator('#tasks-section')).toBeVisible()
    await expect(
      page.locator('#tasks-section').getByRole('heading', { level: 2, name: /my to-dos/i }),
    ).toBeVisible()
  })

  test('Reminder timing lives on System settings; absent from Dashboard Overview', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)
    await page.goto('/dashboard')
    await expect(page.locator('.timeout-config-card')).toHaveCount(0)
    await expect(page.getByRole('heading', { name: /reminder timing/i })).toHaveCount(0)

    await page.goto('/system/settings/reminder-timing')
    await expect(page.getByRole('heading', { level: 1, name: /reminder timing/i })).toBeVisible()
    await expect(
      page.locator('.timeout-config-card').getByRole('heading', { name: /reminder timing/i }),
    ).toBeVisible()
  })

  test('behavior nav escalation label is Overdue to follow up', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await expect(
      myTodosNavSection(page).getByRole('button', { name: BEHAVIOR_NAV_LABELS.escalation }),
    ).toBeVisible()
  })
})
