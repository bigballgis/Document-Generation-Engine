/**
 * SYS-NORM Wave 5 / #149 UIUX evidence — Six-role compression dual-brand @1440×900.
 *
 * Surfaces: Create-user role picker (six roles + Document author interim label),
 * DOCUMENT_AUTHOR / GROUP_ADMIN / TEMPLATE_TESTER dashboard + workflow journeys,
 * remapped GROUP_ADMIN users admin, no retired role labels in assignable UI.
 *
 * Evidence prefix: SYS-NORM-W5-UIUX
 * BDD SoT: docs/behavior/sys-norm-roles.md
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts `
 *     e2e/SYS-NORM-W5-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Page } from '@playwright/test'

import {
  E2E_DOCUMENT_AUTHOR,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_TESTER,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import { reLoginAs } from './helpers/ui'
import {
  captureSysNormW5LocatorScreenshot,
  captureSysNormW5Screenshot,
  dismissOnboardingTourIfPresent,
  ensureSysNormW5EvidenceDirs,
  switchBrand,
  SYS_NORM_W5_VIEWPORT,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const SIX_ROLE_LABELS = [
  /global administrator/i,
  /group administrator/i,
  /document author/i,
  /template tester/i,
  /legal reviewer/i,
  /audit administrator/i,
] as const

const RETIRED_LABELS = [
  /template approver/i,
  /master designer/i,
  /^template author$/i,
] as const

const RETIRED_CODES = ['TEMPLATE_APPROVER', 'MASTER_DESIGNER', 'TEMPLATE_AUTHOR'] as const

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

async function openUsersAdmin(page: Page): Promise<void> {
  await page.goto('/entitlement/users')
  await expect(page).toHaveURL(/\/entitlement\/users/, { timeout: 20_000 })
  await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
    timeout: 20_000,
  })
  await expect(page.getByText(/unable to load users/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })
}

async function openCreateUserRoleDropdown(page: Page) {
  const headerCreate = page.locator('.panel-header').getByRole('button', { name: /^create user$/i })
  if (await headerCreate.isVisible().catch(() => false)) {
    await headerCreate.click()
  } else {
    await page.getByRole('button', { name: /^create user$/i }).first().click()
  }
  const dialog = page.getByRole('dialog', { name: /^create user$/i })
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  const rolesSelect = dialog
    .locator('.el-form-item')
    .filter({ hasText: /\broles\b/i })
    .locator('.el-select')
    .first()
  await rolesSelect.click()
  const dropdown = page.locator('.el-select-dropdown:visible')
  await expect(dropdown.locator('.el-select-dropdown__item').first()).toBeVisible({
    timeout: 10_000,
  })
  return { dialog, dropdown }
}

async function openDashboardWorkflowJourney(page: Page) {
  await page.goto('/dashboard?tab=workflow')
  await dismissOnboardingTourIfPresent(page)
  const journey = page.locator('#journey-section')
  await expect(journey).toBeVisible({ timeout: 20_000 })
  return journey
}

test.describe('SYS-NORM-W5-UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensureSysNormW5EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080).`,
    })
  })

  test('01–02 Role picker six roles dual-brand (Document author interim)', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W5_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await openUsersAdmin(page)
      await assertFluidLayout(page)
      await assertNoViewportOverflow(page)

      const { dialog, dropdown } = await openCreateUserRoleDropdown(page)
      const options = dropdown.locator('.el-select-dropdown__item')
      await expect(options).toHaveCount(6)

      for (const label of SIX_ROLE_LABELS) {
        await expect(dropdown.getByRole('option', { name: label })).toBeVisible()
      }
      for (const retired of RETIRED_LABELS) {
        await expect(dropdown.getByRole('option', { name: retired })).toHaveCount(0)
      }
      const optionTexts = (await options.allTextContents()).map((t) => t.trim()).join('\n')
      for (const code of RETIRED_CODES) {
        expect(optionTexts).not.toContain(code)
      }
      await expect(dropdown.getByRole('option', { name: /document author/i })).toBeVisible()

      const suffix = brand.toLowerCase()
      await captureSysNormW5Screenshot(page, `01-role-picker-${suffix}-1440x900.png`)
      await captureSysNormW5LocatorScreenshot(dropdown, `01b-role-picker-options-${suffix}-crop.png`)
      await captureSysNormW5LocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `01c-brand-header-${suffix}-crop.png`,
      )

      await page.keyboard.press('Escape')
      await dialog.getByRole('button', { name: /cancel/i }).click()
      await expect(dialog).toHaveCount(0)
    }
  })

  test('03–04 DOCUMENT_AUTHOR dashboard + journey dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W5_VIEWPORT)
    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)

      await page.goto('/dashboard')
      await dismissOnboardingTourIfPresent(page)
      await expect(managementNav(page)).toBeVisible()
      await expect(page).not.toHaveURL(/\/forbidden/)
      await assertFluidLayout(page)
      await assertNoViewportOverflow(page)
      await expect(page.getByText(/TEMPLATE_APPROVER|MASTER_DESIGNER|TEMPLATE_AUTHOR/)).toHaveCount(0)

      const suffix = brand.toLowerCase()
      await captureSysNormW5Screenshot(page, `02-document-author-dashboard-${suffix}-1440x900.png`)
      await captureSysNormW5LocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `02b-brand-header-${suffix}-crop.png`,
      )

      const journey = await openDashboardWorkflowJourney(page)
      await expect(
        journey.getByRole('heading', { name: /document authoring workflow/i }),
      ).toBeVisible()
      await expect(journey.locator('[data-journey-step]')).toHaveCount(6)
      await expect(journey.getByText(/TEMPLATE_APPROVER|MASTER_DESIGNER|TEMPLATE_AUTHOR/)).toHaveCount(
        0,
      )
      await assertNoViewportOverflow(page)
      await captureSysNormW5Screenshot(page, `03-document-author-journey-${suffix}-1440x900.png`)
      await captureSysNormW5LocatorScreenshot(journey, `03b-document-author-journey-${suffix}-crop.png`)
    }
  })

  test('05–06 GROUP_ADMIN (remapped ex-approver) users + journey dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W5_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)

      await openUsersAdmin(page)
      await assertFluidLayout(page)
      await assertNoViewportOverflow(page)
      await expect(page.getByText(/template approver/i)).toHaveCount(0)

      const suffix = brand.toLowerCase()
      await captureSysNormW5Screenshot(page, `04-group-admin-users-${suffix}-1440x900.png`)
      await captureSysNormW5LocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `04b-brand-header-${suffix}-crop.png`,
      )

      const journey = await openDashboardWorkflowJourney(page)
      await expect(
        journey.getByRole('heading', { name: /team-lead go-live workflow/i }),
      ).toBeVisible()
      await expect(journey.locator('[data-journey-step]')).toHaveCount(4)
      await expect(journey.getByText(/TEMPLATE_APPROVER|MASTER_DESIGNER|TEMPLATE_AUTHOR/)).toHaveCount(
        0,
      )
      await assertNoViewportOverflow(page)
      await captureSysNormW5Screenshot(page, `05-group-admin-journey-${suffix}-1440x900.png`)
      await captureSysNormW5LocatorScreenshot(journey, `05b-group-admin-journey-${suffix}-crop.png`)
    }
  })

  test('07–08 TEMPLATE_TESTER tasks queue + journey dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W5_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)

      await page.goto('/dashboard?queue=TEST#tasks-section')
      await dismissOnboardingTourIfPresent(page)
      await expect(page.getByRole('tab', { name: /waiting on my testing/i })).toHaveAttribute(
        'aria-selected',
        'true',
      )
      await expect(page.locator('#tasks-section')).toBeVisible()
      await assertFluidLayout(page)
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureSysNormW5Screenshot(page, `06-tester-tasks-queue-${suffix}-1440x900.png`)
      await captureSysNormW5LocatorScreenshot(
        page.locator('#tasks-section'),
        `06b-tester-tasks-${suffix}-crop.png`,
      )

      const journey = await openDashboardWorkflowJourney(page)
      await expect(journey.locator('[data-journey-step]')).toHaveCount(3)
      await expect(journey.getByText(/TEMPLATE_APPROVER|MASTER_DESIGNER|TEMPLATE_AUTHOR/)).toHaveCount(
        0,
      )
      await assertNoViewportOverflow(page)
      await captureSysNormW5Screenshot(page, `07-tester-journey-${suffix}-1440x900.png`)
      await captureSysNormW5LocatorScreenshot(journey, `07b-tester-journey-${suffix}-crop.png`)
    }
  })

  test('09 Users admin shell density + English-first (GLOBAL_ADMIN REDBC)', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W5_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await openUsersAdmin(page)
    await assertFluidLayout(page)
    await assertNoViewportOverflow(page)

    await expect(page.getByRole('heading', { name: /^user management$/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /^create user$/i }).first()).toBeVisible()
    await expect(page.getByText(/template approver|master designer/i)).toHaveCount(0)

    await captureSysNormW5Screenshot(page, '08-users-admin-shell-redbc-1440x900.png')
    await captureSysNormW5LocatorScreenshot(
      page.locator('.page-header').first(),
      '08b-users-page-header-redbc-crop.png',
    )
  })

  // Keep reLoginAs path exercised for session switch polish (brand header intact).
  test('10 Session switch DOCUMENT_AUTHOR → GROUP_ADMIN (GREENBC crop)', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W5_VIEWPORT)
    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await dismissOnboardingTourIfPresent(page)
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await openUsersAdmin(page)
    await assertNoViewportOverflow(page)

    await captureSysNormW5Screenshot(page, '09-session-switch-group-admin-greenbc-1440x900.png')
    await captureSysNormW5LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '09b-brand-header-after-relogin-greenbc-crop.png',
    )
  })
})
