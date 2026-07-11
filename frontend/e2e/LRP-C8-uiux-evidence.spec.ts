/**
 * LR-C8 UIUX evidence — role onboarding tour (spotlight + OA action card).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C8-uiux-evidence.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Screenshots: frontend/e2e/evidence/LRP-C8/screenshots/
 * Manifest:    frontend/e2e/evidence/LRP-C8-uiux-manifest.md
 *
 * Note: `[data-testid=onboarding-tour]` host may be height:0; visual focus is
 * spotlight mask + fixed `.onboarding-tour__card`.
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureLrpC8LocatorScreenshot,
  captureLrpC8Screenshot,
  ensureLrpC8EvidenceDirs,
  LRP_C8_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

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

function tourHost(page: Page) {
  return page.getByTestId('onboarding-tour')
}

function tourCard(page: Page) {
  return tourHost(page).locator('.onboarding-tour__card')
}

async function expectTourStep1Visible(page: Page) {
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
  return replay
}

test.describe('LRP-C8 onboarding tour UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    ensureLrpC8EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL}).`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(LRP_C8_VIEWPORT)
    page.setDefaultTimeout(20_000)
  })

  test('onboarding tour — dual brand open / skip / help replay', async ({ page }) => {
    // --- REDBC: first-time open (step 1) ---
    await clearTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    await expectTourStep1Visible(page)

    // English-first chrome
    await expect(page.getByTestId('onboarding-tour-skip')).toHaveText('Skip')
    await expect(page.getByTestId('onboarding-tour-next')).toHaveText('Next')
    await expect(tourCard(page)).toHaveAttribute('role', 'dialog')
    await expect(tourCard(page)).toHaveAttribute('aria-modal', 'true')

    await captureLrpC8Screenshot(page, '01-tour-open-step1-redbc-en-1440x900.png')
    await captureLrpC8LocatorScreenshot(tourCard(page), '02-tour-action-card-redbc-en.png')
    await captureLrpC8LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03-brand-header-redbc-en.png',
    )

    // Focus-visible on primary Next
    const next = page.getByTestId('onboarding-tour-next')
    await next.focus()
    await expect(next).toBeFocused()
    await captureLrpC8LocatorScreenshot(tourCard(page), '04-tour-next-focus-redbc-en.png')

    // Skip → closed; Help visible
    await page.getByTestId('onboarding-tour-skip').click()
    await expectTourClosed(page)
    await expect(page.getByTestId('help-menu')).toBeVisible()
    await captureLrpC8Screenshot(page, '05-tour-skipped-shell-redbc-en-1440x900.png')
    await captureLrpC8LocatorScreenshot(
      page.locator('.shell-header'),
      '06-header-help-menu-redbc-en.png',
    )

    // Help → Replay menu open (before click)
    const replay = await openHelpReplay(page)
    await expect(replay).toHaveText('Replay role tour')
    await captureLrpC8Screenshot(page, '07-help-menu-replay-open-redbc-en-1440x900.png')
    // Capture dropdown item while still open — re-open if click closed it in capture race
    if ((await page.getByTestId('help-menu-replay-tour').count()) === 0) {
      await page.getByTestId('help-menu-trigger').click()
      await expect(page.getByTestId('help-menu-replay-tour')).toBeVisible()
    }
    await captureLrpC8LocatorScreenshot(
      page.getByTestId('help-menu-replay-tour'),
      '08-help-replay-item-redbc-en.png',
    )
    await page.getByTestId('help-menu-replay-tour').click()
    await expectTourStep1Visible(page)
    await captureLrpC8Screenshot(page, '09-tour-replayed-step1-redbc-en-1440x900.png')
    await captureLrpC8LocatorScreenshot(tourCard(page), '10-tour-replayed-card-redbc-en.png')

    await page.getByTestId('onboarding-tour-skip').click()
    await expectTourClosed(page)

    // --- GREENBC: seed dismiss then replay (stable closed start) ---
    await seedTourDismiss(page, E2E_TEMPLATE_AUTHOR.username)
    await page.goto('/dashboard', { waitUntil: 'domcontentloaded' })
    await expect(page.getByTestId('help-menu')).toBeVisible({ timeout: 20_000 })
    await expectTourStaysClosed(page)

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')

    await captureLrpC8LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '11-brand-header-greenbc-en.png',
    )
    await captureLrpC8LocatorScreenshot(
      page.locator('.shell-header'),
      '12-header-help-menu-greenbc-en.png',
    )

    await openHelpReplay(page)
    await expect(page.getByTestId('help-menu-replay-tour')).toHaveText('Replay role tour')
    await captureLrpC8Screenshot(page, '13-help-menu-replay-open-greenbc-en-1440x900.png')
    if ((await page.getByTestId('help-menu-replay-tour').count()) === 0) {
      await page.getByTestId('help-menu-trigger').click()
      await expect(page.getByTestId('help-menu-replay-tour')).toBeVisible()
    }
    await page.getByTestId('help-menu-replay-tour').click()
    await expectTourStep1Visible(page)

    await captureLrpC8Screenshot(page, '14-tour-open-step1-greenbc-en-1440x900.png')
    await captureLrpC8LocatorScreenshot(tourCard(page), '15-tour-action-card-greenbc-en.png')

    // Primary Next should use GREENBC teal
    await page.getByTestId('onboarding-tour-next').focus()
    await captureLrpC8LocatorScreenshot(tourCard(page), '16-tour-next-focus-greenbc-en.png')
  })
})
