import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_APPROVER, loginAs } from './helpers/auth'
import { demoApprovalTemplateDetailPath } from './helpers/content-modules-api'
import { BEHAVIOR_NAV_LABELS, myTodosNavSection } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T08 Template approver journey (§12.8)', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.` })
  })

  test('template approver dashboard journey reflects catalog with three steps', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await page.goto('/dashboard')

    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(
      journeySection.getByRole('heading', { name: /template approval workflow/i }),
    ).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(3)
    await expect(journeySection.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('template detail shows approver journey timeline above workflow banner', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    const detailPath = await demoApprovalTemplateDetailPath(request)
    await page.goto(detailPath)

    const timeline = page.locator('[data-journey-timeline]')
    await expect(timeline).toBeVisible()
    await expect(timeline.locator('[data-journey-step]')).toHaveCount(3)
    await expect(timeline.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('behavior nav approval label is Waiting on my approval', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await expect(
      myTodosNavSection(page).getByRole('button', { name: BEHAVIOR_NAV_LABELS.approval }),
    ).toBeVisible()
  })

  test('filtered APPROVAL hub shows journey section and waiting on my approval title', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await page.goto('/dashboard?queue=APPROVAL#tasks-section')

    await expect(page.locator('#journey-section')).toBeVisible()
    await expect(
      page.getByRole('heading', { level: 1, name: /waiting on my approval/i }),
    ).toBeVisible()
  })
})
