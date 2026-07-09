import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { demoTemplateDetailPath } from './helpers/content-modules-api'
import {
  captureP21T04LocatorScreenshot,
  ensureP21T04EvidenceDirs,
  P21_T04_VIEWPORT,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T04 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    ensureP21T04EvidenceDirs()
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + :8080).` })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P21_T04_VIEWPORT)
  })

  test('capture template author journey on dashboard and detail', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')
    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(6)
    await captureP21T04LocatorScreenshot(
      journeySection,
      '01-dashboard-template-author-journey-six-steps-redbc-1440x900.png',
    )

    const detailPath = await demoTemplateDetailPath(request)
    await page.goto(detailPath)
    const detailTimeline = page.locator('[data-journey-timeline]')
    await expect(detailTimeline).toBeVisible()
    await captureP21T04LocatorScreenshot(
      detailTimeline,
      '02-template-detail-journey-timeline-redbc-1440x900.png',
    )
  })
})
