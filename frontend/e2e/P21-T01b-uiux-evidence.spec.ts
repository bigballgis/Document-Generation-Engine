import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { reLoginAs } from './helpers/ui'
import {
  captureP21T01bLocatorScreenshot,
  captureP21T01bScreenshot,
  ensureP21T01bEvidenceDirs,
  P21_T01B_VIEWPORT,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T01b UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    ensureP21T01bEvidenceDirs()

    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start backend on :8080 and pnpm dev on :5173.` })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P21_T01B_VIEWPORT)
  })

  test('capture RoleJourneyTimeline evidence — author, tester, approver hidden, focus-visible', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')
    const authorJourney = page.locator('#journey-section')
    await expect(authorJourney).toBeVisible()
    await expect(page.locator('#tasks-section')).toBeVisible()
    await expect(
      authorJourney.getByRole('heading', { name: /template authoring workflow/i }),
    ).toBeVisible()
    await expect(authorJourney.locator('[data-journey-step]')).toHaveCount(6)
    await expect(authorJourney.locator('[data-journey-guidance]')).toBeVisible()
    await expect(authorJourney.locator('[aria-current="step"]')).toHaveCount(0)
    await captureP21T01bLocatorScreenshot(
      authorJourney,
      '01-author-journey-six-steps-redbc-1440x900.png',
    )

    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard')
    const testerJourney = page.locator('#journey-section')
    await expect(testerJourney).toBeVisible()
    await expect(
      testerJourney.getByRole('heading', { name: /template testing workflow/i }),
    ).toBeVisible()
    await expect(testerJourney.locator('[data-journey-step]')).toHaveCount(3)
    await captureP21T01bLocatorScreenshot(
      testerJourney,
      '02-tester-journey-three-steps-redbc-1440x900.png',
    )

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await page.goto('/dashboard')
    await expect(page.locator('#journey-section')).toHaveCount(0)
    await expect(page.locator('#tasks-section')).toBeVisible()
    await captureP21T01bScreenshot(page, '03-approver-no-journey-section-redbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')
    await expect(page.locator('#journey-section')).toBeVisible()
    const firstStep = page.locator('#journey-section [data-journey-step]').first()
    await firstStep.focus()
    await expect(firstStep).toBeFocused()
    await page.keyboard.press('ArrowRight')
    const secondStep = page.locator('#journey-section [data-journey-step]').nth(1)
    await expect(secondStep).toBeFocused()
    await captureP21T01bLocatorScreenshot(
      page.locator('#journey-section'),
      '04-journey-step-focus-visible-redbc-1440x900.png',
    )
  })
})
