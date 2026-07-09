import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_APPROVER, loginAs } from './helpers/auth'
import { demoApprovalTemplateDetailPath } from './helpers/content-modules-api'
import {
  captureP21T08LocatorScreenshot,
  ensureP21T08EvidenceDirs,
  P21_T08_VIEWPORT,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T08 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    ensureP21T08EvidenceDirs()
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + :8080).` })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P21_T08_VIEWPORT)
  })

  test('capture template approver journey on dashboard and detail', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await page.goto('/dashboard')
    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible()
    await expect(journeySection.locator('[data-journey-step]')).toHaveCount(3)
    await captureP21T08LocatorScreenshot(
      journeySection,
      '01-dashboard-template-approver-journey-three-steps-redbc-1440x900.png',
    )

    const detailPath = await demoApprovalTemplateDetailPath(request)
    await page.goto(detailPath)
    const detailTimeline = page.locator('[data-journey-timeline]')
    await expect(detailTimeline).toBeVisible()
    await captureP21T08LocatorScreenshot(
      detailTimeline,
      '02-template-detail-approver-journey-timeline-redbc-1440x900.png',
    )
  })
})
