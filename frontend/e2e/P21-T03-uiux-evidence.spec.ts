import { expect, test } from '@playwright/test'

import { E2E_MASTER_DESIGNER, loginAs } from './helpers/auth'
import { demoMasterDetailPath } from './helpers/masters-api'
import {
  captureP21T03LocatorScreenshot,
  ensureP21T03EvidenceDirs,
  P21_T03_VIEWPORT,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T03 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    ensureP21T03EvidenceDirs()
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
    await page.setViewportSize(P21_T03_VIEWPORT)
  })

  test('capture master designer journey on dashboard and hub', async ({ page, request }) => {
    await loginAs(page, E2E_MASTER_DESIGNER)
    await page.goto('/dashboard')
    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(4)
    await captureP21T03LocatorScreenshot(
      journeySection,
      '01-dashboard-letterhead-journey-four-steps-redbc-1440x900.png',
    )

    const hubPath = await demoMasterDetailPath(request)
    await page.goto(hubPath)
    const hubTimeline = page.locator('[data-journey-timeline]')
    await expect(hubTimeline).toBeVisible()
    await captureP21T03LocatorScreenshot(
      hubTimeline,
      '02-master-hub-journey-timeline-redbc-1440x900.png',
    )
  })
})
