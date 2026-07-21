/**
 * Reminder timing settings IA / Task Master #153 — Stage 7 UIUX evidence.
 *
 * Surfaces: System settings Reminder timing (GLOBAL), Team settings dialog (GROUP),
 * Dashboard Overview without timeout panel. Dual-brand REDBC/GREENBC @1440×900.
 *
 * BDD SoT: docs/behavior/reminder-timing-settings-ia.md
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts `
 *     e2e/reminder-timing-settings-ia-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_ADMIN, E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureReminderTimingIaLocatorScreenshot,
  captureReminderTimingIaScreenshot,
  dismissOnboardingTourIfPresent,
  ensureReminderTimingIaEvidenceDirs,
  REMINDER_TIMING_IA_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const REMINDER_TIMING_PATH = '/system/settings/reminder-timing'
const SYSTEM_SETTINGS_NAV = /^system settings$/i
const TEAM_SETTINGS = /^team settings$/i
const REMINDER_TIMING = /reminder timing/i

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return { scrollWidth: doc.scrollWidth, clientWidth: doc.clientWidth }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function assertPrimaryBrandColor(page: Page, brand: 'REDBC' | 'GREENBC'): Promise<void> {
  const expected = brand === 'REDBC' ? '#db0011' : '#00847f'
  const primary = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim().toLowerCase(),
  )
  expect(primary, `expected --brand-primary ${expected} for ${brand}`).toBe(expected)
}

async function assertFluidLayout(page: Page): Promise<void> {
  const layout = page.locator('.app-page-layout').first()
  await expect(layout).toBeVisible({ timeout: 20_000 })
  await expect(layout).toHaveClass(/app-page-layout--fluid/)
  await expect(layout.locator('.app-page-layout__inner')).toHaveCount(0)
}

test.describe('Reminder timing settings IA UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensureReminderTimingIaEvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080).`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(REMINDER_TIMING_IA_VIEWPORT)
  })

  test('01–02 System settings Reminder timing dual-brand', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    const nav = managementNav(page)
    await expect(nav.getByRole('button', { name: SYSTEM_SETTINGS_NAV })).toBeVisible()
    await nav.getByRole('button', { name: SYSTEM_SETTINGS_NAV }).click()
    await expect(page).toHaveURL(new RegExp(`${REMINDER_TIMING_PATH}$`))
    await expect(page.getByRole('heading', { level: 1, name: REMINDER_TIMING })).toBeVisible()
    await assertFluidLayout(page)

    const panel = page.locator('.timeout-config-card')
    await expect(panel).toBeVisible()
    await expect(panel.getByRole('heading', { name: REMINDER_TIMING })).toBeVisible()
    await expect(panel.getByText(/notifications only/i)).toBeVisible()
    await expect(panel.getByText(/configuration scope/i)).toHaveCount(0)
    await expect(panel.getByLabel(/group code/i)).toHaveCount(0)
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await expect(page.locator('.el-skeleton')).toHaveCount(0)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureReminderTimingIaScreenshot(
        page,
        `01-system-settings-reminder-timing-${suffix}-1440x900.png`,
      )
      await captureReminderTimingIaLocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `01b-brand-header-${suffix}-crop.png`,
      )
      await captureReminderTimingIaLocatorScreenshot(
        panel,
        `01c-timeout-panel-${suffix}-crop.png`,
      )
    }
  })

  test('03 Dashboard Overview has no timeout panel (IA)', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.locator('.timeout-config-card')).toHaveCount(0)
    await expect(page.getByRole('heading', { name: REMINDER_TIMING })).toHaveCount(0)
    await expect(page.getByRole('button', { name: TEAM_SETTINGS })).toHaveCount(0)
    await assertNoViewportOverflow(page)

    await switchBrand(page, 'REDBC')
    await captureReminderTimingIaScreenshot(page, '02-dashboard-no-timeout-redbc-1440x900.png')
    await switchBrand(page, 'GREENBC')
    await captureReminderTimingIaScreenshot(page, '02-dashboard-no-timeout-greenbc-1440x900.png')
  })

  test('04–05 Team settings dialog dual-brand', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await expect(managementNav(page).getByRole('button', { name: SYSTEM_SETTINGS_NAV })).toHaveCount(
      0,
    )

    await page.goto('/entitlement/groups')
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
    await expect(page.getByRole('heading', { level: 1, name: /group management/i })).toBeVisible()
    await expect(page.getByTestId('team-settings-button')).toBeVisible()

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)

      await page.getByTestId('team-settings-button').click()
      const dialog = page.getByRole('dialog', { name: TEAM_SETTINGS })
      await expect(dialog).toBeVisible()
      const panel = dialog.locator('.timeout-config-card')
      await expect(panel.getByRole('heading', { name: REMINDER_TIMING })).toBeVisible()
      await expect(panel.getByText(/notifications only/i)).toBeVisible()
      await expect(panel.getByText(/configuration scope/i)).toHaveCount(0)
      const groupCodeInput = panel
        .locator('.el-form-item')
        .filter({ hasText: /group code/i })
        .locator('input')
      await expect(groupCodeInput).toBeVisible()
      await expect(groupCodeInput).toHaveAttribute('readonly', '')
      await expect(page.locator('.el-skeleton')).toHaveCount(0)

      const suffix = brand.toLowerCase()
      await captureReminderTimingIaScreenshot(
        page,
        `03-team-settings-dialog-${suffix}-1440x900.png`,
      )
      await captureReminderTimingIaLocatorScreenshot(
        dialog,
        `03b-team-settings-dialog-${suffix}-crop.png`,
      )
      await captureReminderTimingIaLocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `03c-brand-header-${suffix}-crop.png`,
      )

      await dialog.getByRole('button', { name: /close/i }).click()
      await expect(dialog).toBeHidden()
    }
  })

  test('06 Forbidden System settings for GROUP_ADMIN (no leak)', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    // Brand switcher is unavailable on the forbidden surface — set brand on a shell page first.
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await switchBrand(page, 'REDBC')
    await assertPrimaryBrandColor(page, 'REDBC')

    await page.goto(REMINDER_TIMING_PATH)
    await expect(page).toHaveURL(/\/forbidden/)
    await expect(page.getByText(/access denied/i)).toBeVisible()
    await expect(page.locator('.timeout-config-card')).toHaveCount(0)
    await assertNoViewportOverflow(page)
    await captureReminderTimingIaScreenshot(page, '04-forbidden-system-settings-redbc-1440x900.png')
  })

  test('07 GROUP_ADMIN dashboard also without timeout panel', async ({ page }) => {
    // Forbidden deep-link may clear shell session — re-authenticate as GROUP_ADMIN.
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.locator('.timeout-config-card')).toHaveCount(0)
    await expect(page.getByRole('button', { name: TEAM_SETTINGS })).toHaveCount(0)
    await captureReminderTimingIaScreenshot(
      page,
      '05-group-admin-dashboard-no-timeout-redbc-1440x900.png',
    )
  })
})
