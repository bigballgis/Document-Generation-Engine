import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import { demoTestingTemplateDetailPath } from './helpers/content-modules-api'
import {
  captureP21T05LocatorScreenshot,
  ensureP21T05EvidenceDirs,
  P21_T05_VIEWPORT,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T05 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    ensureP21T05EvidenceDirs()
    let backendReady = false
    let frontendReady = false
    try {
      backendReady = (await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })).ok()
    } catch {
      backendReady = false
    }
    try {
      frontendReady = (await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })).ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Stack required (${FRONTEND_BASE_URL} + :8080).`,
    )
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P21_T05_VIEWPORT)
  })

  test('capture template tester journey on dashboard and detail', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard')
    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(3)
    await captureP21T05LocatorScreenshot(
      journeySection,
      '01-dashboard-template-tester-journey-three-steps-redbc-1440x900.png',
    )

    const detailPath = await demoTestingTemplateDetailPath(request)
    await page.goto(detailPath)
    const detailTimeline = page.locator('[data-journey-timeline]')
    await expect(detailTimeline).toBeVisible()
    await captureP21T05LocatorScreenshot(
      detailTimeline,
      '02-template-detail-tester-journey-timeline-redbc-1440x900.png',
    )
  })
})
