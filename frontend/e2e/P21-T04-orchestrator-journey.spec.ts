import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { demoTemplateDetailPath } from './helpers/content-modules-api'
import { BEHAVIOR_NAV_LABELS, myTodosNavSection } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T04 Template author journey (§12.6)', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.` })
  })

  test('template author dashboard journey reflects catalog with six steps', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')

    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(
      journeySection.getByRole('heading', { name: /template authoring workflow/i }),
    ).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(6)
    await expect(journeySection.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('dashboard journey is read-only and open workspace deep-links to package hub', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')

    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(page.locator('[data-template-journey-cta]')).toHaveCount(0)

    const workspaceLink = page.locator('[data-dashboard-journey-link]')
    await expect(workspaceLink).toBeVisible()
    await workspaceLink.click()

    await expect(page).toHaveURL(/\/templates\/[0-9a-f-]{36}(?:\/|$|\?)/i, { timeout: 15_000 })
  })

  test('template detail shows author journey timeline above workflow banner', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const detailPath = await demoTemplateDetailPath(request)
    await page.goto(detailPath)

    const timeline = page.locator('[data-journey-timeline]')
    await expect(timeline).toBeVisible()
    await expect(timeline.locator('[data-journey-step]')).toHaveCount(6)
    await expect(timeline.locator('[data-journey-guidance]')).toBeVisible()
  })

  test('behavior nav remediation label is Waiting on my fixes', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await expect(
      myTodosNavSection(page).getByRole('button', { name: BEHAVIOR_NAV_LABELS.remediation }),
    ).toBeVisible()
  })

  test('filtered remediation hub shows journey section and Needs fixes partition', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard?queue=REMEDIATION#tasks-section')

    await expect(page.locator('#journey-section')).toBeVisible()
    await expect(
      page.getByRole('heading', { level: 1, name: /waiting on my fixes/i }),
    ).toBeVisible()

    const needsFixesHeading = page.getByRole('heading', { name: /^needs fixes$/i })
    if (await needsFixesHeading.count()) {
      await expect(needsFixesHeading.first()).toBeVisible()
    }
  })

  test('pending release template shows team-lead waiting guidance on detail when applicable', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/templates')

    const pendingRow = page.getByRole('row').filter({ hasText: /pending release|awaiting go-live/i })
    if ((await pendingRow.count()) === 0) {
      test.skip(true, 'No PENDING_RELEASE template in catalog for author — skip guidance assertion.')
    }

    await pendingRow.first().click()
    const guidance = page.locator('[data-journey-guidance]')
    await expect(guidance).toBeVisible()
    await expect(guidance).toContainText(/awaiting team-lead go-live/i)
    await expect(page.locator('[data-template-journey-cta]')).toHaveCount(0)

    void request
  })
})
