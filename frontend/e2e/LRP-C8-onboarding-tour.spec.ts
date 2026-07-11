/**
 * LR-C8 — Role onboarding tour (E2E) — BDD-LRP-C8-001…005 primary subset.
 *
 * Canonical run (after stage 5 DEPLOY_OK):
 *   pnpm -C frontend exec playwright test e2e/LRP-C8-onboarding-tour.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080.
 *
 * Scenario map (this file):
 *   BDD-001       — first-time TEMPLATE_AUTHOR → tour step 1 (Create template)
 *   BDD-002       — Skip closes + persists dismiss; no auto-reopen on reload
 *   BDD-003       — Help → Replay opens from step 1 despite dismiss
 *   BDD-004       — Finish writes dismiss (optional cheap)
 *   BDD-005       — Escape closes and writes dismiss (optional cheap)
 *
 * Note: `[data-testid=onboarding-tour]` host is height:0 (card is position:fixed).
 * Assert open/closed via the action card / Skip control, not host toBeVisible.
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { requireDockerStack } from './helpers/stack-readiness'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

const DISMISS_KEY_PREFIX = 'docgen.onboardingTour.dismissed.v1:'

const AUTHOR_STEP1_LABEL = 'Create template'
const AUTHOR_STEP1_GUIDANCE = 'Create a new template or open an existing draft.'

function dismissKeyFor(username: string): string {
  return `${DISMISS_KEY_PREFIX}${username}`
}

async function clearTourDismiss(page: Page, username: string) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await page.evaluate((key) => localStorage.removeItem(key), dismissKeyFor(username))
}

async function seedTourDismiss(page: Page, username: string) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await page.evaluate((key) => localStorage.setItem(key, '1'), dismissKeyFor(username))
}

async function readTourDismiss(page: Page, username: string): Promise<string | null> {
  return page.evaluate((key) => localStorage.getItem(key), dismissKeyFor(username))
}

function tourHost(page: Page) {
  return page.getByTestId('onboarding-tour')
}

function tourCard(page: Page) {
  return tourHost(page).locator('.onboarding-tour__card')
}

async function expectTourStep1Visible(page: Page) {
  // Host is height:0 — use card / Skip (stable testids) as open signal.
  await expect(page.getByTestId('onboarding-tour-skip')).toBeVisible({ timeout: 20_000 })
  await expect(tourCard(page)).toBeVisible()
  await expect(tourCard(page).locator('.onboarding-tour__title')).toHaveText(AUTHOR_STEP1_LABEL)
  await expect(tourCard(page).locator('.onboarding-tour__description')).toContainText(
    AUTHOR_STEP1_GUIDANCE,
  )
  await expect(page.getByTestId('onboarding-tour-next')).toBeVisible()
  await expect(tourHost(page)).not.toHaveAttribute('hidden')
}

async function expectTourClosed(page: Page) {
  await expect(page.getByTestId('onboarding-tour-skip')).toHaveCount(0)
  await expect(tourCard(page)).toHaveCount(0)
  await expect(tourHost(page)).toHaveAttribute('hidden', '')
}

/** Auto-open runs on shell mount nextTick/rAF — assert it stays closed across that window. */
async function expectTourStaysClosed(page: Page) {
  await expect(async () => {
    await expectTourClosed(page)
  }).toPass({ timeout: 3_000 })
}

async function openHelpReplay(page: Page) {
  await page.getByTestId('help-menu-trigger').click()
  const replay = page.getByTestId('help-menu-replay-tour')
  await expect(replay).toBeVisible({ timeout: 10_000 })
  await expect(replay).toBeEnabled()
  await replay.click()
}

test.describe('LRP-C8 onboarding tour', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL })
  })

  test.beforeEach(async ({ page }) => {
    page.setDefaultTimeout(20_000)
  })

  test('BDD-001/002: first TEMPLATE_AUTHOR login opens step 1; Skip persists', async ({
    page,
  }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    await clearTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    // loginAs already lands on dashboard; avoid remount race from a second goto.

    await expectTourStep1Visible(page)

    await page.getByTestId('onboarding-tour-skip').click()
    await expectTourClosed(page)
    await expect.poll(() => readTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)).toBe('1')

    await page.reload({ waitUntil: 'domcontentloaded' })
    await expect(page.getByTestId('help-menu')).toBeVisible({ timeout: 20_000 })
    await expectTourStaysClosed(page)
    await expect(await readTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)).toBe('1')
  })

  test('BDD-003: Help → Replay opens step 1 despite dismiss', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    await seedTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)

    await expect(page.getByTestId('help-menu')).toBeVisible({ timeout: 20_000 })
    await expectTourStaysClosed(page)

    await openHelpReplay(page)
    await expectTourStep1Visible(page)

    // Replay must not clear dismiss (auto-trigger still suppressed after close).
    await expect(await readTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)).toBe('1')
    await page.getByTestId('onboarding-tour-skip').click()
    await expectTourClosed(page)
    await expect(await readTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)).toBe('1')
  })

  test('BDD-004: Finish writes dismiss and closes tour', async ({ page }) => {
    test.setTimeout(120_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    await clearTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await expectTourStep1Visible(page)

    // Author journey has 6 steps — click Next until Finish appears.
    for (let i = 0; i < 5; i += 1) {
      const next = page.getByTestId('onboarding-tour-next')
      if (await next.isVisible().catch(() => false)) {
        await next.click()
      }
    }
    await expect(page.getByTestId('onboarding-tour-finish')).toBeVisible()
    await page.getByTestId('onboarding-tour-finish').click()
    await expectTourClosed(page)
    await expect.poll(() => readTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)).toBe('1')
  })

  test('BDD-005: Escape closes tour and writes dismiss', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    await clearTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await expectTourStep1Visible(page)

    await page.keyboard.press('Escape')
    await expectTourClosed(page)
    await expect.poll(() => readTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)).toBe('1')
  })
})
