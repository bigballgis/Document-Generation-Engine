import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_AUDIT_ADMIN, loginAs } from './helpers/auth'
import { expectMyTodosGroupAbsent } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T11 Audit admin journey (§12.11)', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.` })
  })

  test('audit admin lands on activity log with five-step journey', async ({ page }) => {
    await loginAs(page, E2E_AUDIT_ADMIN)
    await page.goto('/audit')

    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(
      journeySection.getByRole('heading', { name: /activity log workflow/i }),
    ).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(5)
    await expect(journeySection.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('activity log page shows view-only banner and business title', async ({ page }) => {
    await loginAs(page, E2E_AUDIT_ADMIN)
    await page.goto('/audit')

    await expect(page.getByRole('heading', { level: 1, name: /^activity log$/i })).toBeVisible()
    await expect(page.getByRole('alert').getByText(/view only — no actions/i)).toBeVisible()
    await expect(page.getByRole('heading', { level: 1, name: /audit console/i })).toHaveCount(0)
  })

  test('audit-only user does not see myTodos behavior nav group', async ({ page }) => {
    await loginAs(page, E2E_AUDIT_ADMIN)
    await expectMyTodosGroupAbsent(page)
  })
})
