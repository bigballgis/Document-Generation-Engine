/**
 * SYS-NORM Wave 3 / #147 UIUX evidence — External services ops dual-brand @1440×900.
 *
 * Surfaces: dashboard `/api/policies`, invocations `/api/invocations`,
 * package settings deep-link, nav membership, optional invocation drawer.
 *
 * Evidence prefix: SYS-NORM-W3-UIUX
 * BDD SoT: docs/behavior/sys-norm-external-ops.md
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts `
 *     e2e/SYS-NORM-W3-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Page } from '@playwright/test'

import {
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { ensureDemoFullFlowPublished } from './helpers/content-modules-api'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureSysNormW3LocatorScreenshot,
  captureSysNormW3Screenshot,
  dismissOnboardingTourIfPresent,
  ensureSysNormW3EvidenceDirs,
  switchBrand,
  SYS_NORM_W3_VIEWPORT,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const OVERVIEW_NAV = /^external services overview$/i
const INVOCATIONS_NAV = /^invocation records$/i

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

async function openExternalServicesOverview(page: Page): Promise<void> {
  await page.goto('/api/policies')
  await expect(page).toHaveURL(/\/api\/policies\/?$/, { timeout: 20_000 })
  await expect(page.locator('.page-header h1')).toHaveText(
    /external services overview|对外服务概览/i,
  )
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })
}

async function openInvocationRecords(page: Page): Promise<void> {
  await page.goto('/api/invocations')
  await expect(page).toHaveURL(/\/api\/invocations/, { timeout: 20_000 })
  await expect(page.locator('.page-header h1')).toHaveText(/invocation records|调用记录/i)
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })
}

test.describe('SYS-NORM-W3-UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let publishedTemplateId = ''

  test.beforeAll(async ({ request }) => {
    ensureSysNormW3EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080).`,
    })
    const published = await ensureDemoFullFlowPublished(request)
    publishedTemplateId = published.templateId
  })

  test('01–02 Dashboard ops dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W3_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await openExternalServicesOverview(page)
      await assertFluidLayout(page)

      await expect(page.getByTestId('api-readiness-summary')).toBeVisible()
      await expect(page.getByTestId('api-ops-summary')).toBeVisible()
      await expect(page.getByRole('heading', { name: /^published packages$|^已发布包$/i })).toHaveCount(
        0,
      )
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureSysNormW3Screenshot(page, `01-dashboard-ops-${suffix}-1440x900.png`)
      await captureSysNormW3LocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `01b-brand-header-${suffix}-crop.png`,
      )
      await captureSysNormW3LocatorScreenshot(
        page.getByTestId('api-readiness-summary'),
        `01c-readiness-summary-${suffix}-crop.png`,
      )
      await captureSysNormW3LocatorScreenshot(
        page.getByTestId('api-ops-summary'),
        `01d-ops-summary-${suffix}-crop.png`,
      )
    }
  })

  test('03–04 Invocations page dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W3_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await openInvocationRecords(page)
      await assertFluidLayout(page)

      const filters = page.getByTestId('api-invocations-filters')
      await expect(filters).toBeVisible()
      await expect(page.getByTestId('api-invocations-filter-status')).toBeVisible()
      await expect(page.getByTestId('api-invocations-filter-package')).toBeVisible()
      await expect(page.getByTestId('api-invocations-table-card')).toBeVisible()
      await assertNoViewportOverflow(page)

      // Filter control types: enum → select; package entity → AppSearchSelect host.
      await expect(page.getByTestId('api-invocations-filter-status').locator('input')).toBeVisible()
      await expect(page.locator('[data-testid="api-invocations-filter-package"]')).toBeVisible()

      const suffix = brand.toLowerCase()
      await captureSysNormW3Screenshot(page, `02-invocations-page-${suffix}-1440x900.png`)
      await captureSysNormW3LocatorScreenshot(filters, `02b-invocations-filters-${suffix}-crop.png`)
      await captureSysNormW3LocatorScreenshot(
        page.getByTestId('api-invocations-table-card'),
        `02c-invocations-table-${suffix}-crop.png`,
      )
    }
  })

  test('05 Invocation detail drawer + More→settings (when rows exist)', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W3_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await openInvocationRecords(page)

    const rows = page.locator('[data-testid="api-invocations-table-card"] .el-table__body tr')
    const rowCount = await rows.count()
    test.skip(rowCount === 0, 'No invocation rows — drawer evidence skipped (honest empty)')

    const row = rows.first()
    // N22: primary Open summary is TableEditMoreActions Edit slot.
    const actions = row.getByTestId('table-edit-more-actions')
    await expect(actions).toBeVisible()
    await actions.getByRole('button', { name: /open summary|打开摘要/i }).click()
    const drawer = page.getByTestId('invocation-summary-drawer')
    await expect(drawer).toBeVisible({ timeout: 20_000 })
    await expect(drawer.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(drawer.getByText(/variables|variableValues|parameters/i)).toHaveCount(0)
    await assertNoViewportOverflow(page)

    await captureSysNormW3Screenshot(page, '03-invocation-drawer-redbc-1440x900.png')
    await captureSysNormW3LocatorScreenshot(drawer, '03b-invocation-drawer-redbc-crop.png')

    await drawer.getByRole('button', { name: /close|关闭/i }).click().catch(async () => {
      await page.keyboard.press('Escape')
    })
    await expect(drawer).toBeHidden({ timeout: 10_000 })

    // N22: API settings under More — menu items teleport to body; scope :visible.
    await actions.getByRole('button', { name: /^more$/i }).click()
    const moreMenu = page.locator('.el-dropdown-menu:visible')
    const settingsItem = moreMenu.getByTestId('api-invocations-open-settings')
    await expect(settingsItem).toBeVisible()
    await captureSysNormW3LocatorScreenshot(moreMenu, '03c-more-menu-settings-redbc-crop.png')
    await settingsItem.click()
    await expect(page).toHaveURL(/\/api\/packages\/[^/]+\/settings/, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })
    await captureSysNormW3Screenshot(page, '03d-settings-via-more-redbc-1440x900.png')
  })

  test('06–07 Package settings complete dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W3_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await page.goto(`/api/packages/${publishedTemplateId}/settings`)
      await expect(page).toHaveURL(
        new RegExp(
          `/api/packages/${publishedTemplateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/settings`,
        ),
        { timeout: 20_000 },
      )
      await expect(page.getByTestId('api-package-settings-interim-banner')).toHaveCount(0)
      await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })
      await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
      await expect(page.getByText(/under construction|interim shell|临时壳/i)).toHaveCount(0)
      await assertFluidLayout(page)
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureSysNormW3Screenshot(page, `04-package-settings-${suffix}-1440x900.png`)
      await captureSysNormW3LocatorScreenshot(
        page.getByTestId('api-package-settings-panel'),
        `04b-package-settings-panel-${suffix}-crop.png`,
      )
      await captureSysNormW3LocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `04c-brand-header-${suffix}-crop.png`,
      )
    }
  })

  test('08 Nav membership + icons dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W3_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await page.goto('/dashboard')

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)

      const nav = managementNav(page)
      await expect(nav.getByRole('heading', { name: /external services|对外服务/i })).toBeVisible()
      const overview = nav.getByRole('button', { name: OVERVIEW_NAV })
      const invocations = nav.getByRole('button', { name: INVOCATIONS_NAV })
      await expect(overview).toBeVisible()
      await expect(invocations).toBeVisible()
      await expect(overview.locator('.el-icon')).toHaveCount(1)
      await expect(invocations.locator('.el-icon')).toHaveCount(1)
      await expect(nav.getByRole('button', { name: /^api settings$|^package api settings$/i })).toHaveCount(
        0,
      )
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureSysNormW3Screenshot(page, `05-nav-external-services-${suffix}-1440x900.png`)
      await captureSysNormW3LocatorScreenshot(
        nav.locator('.nav-group').filter({ hasText: /external services|对外服务/i }),
        `05b-nav-external-services-${suffix}-crop.png`,
      )
    }
  })

  test('09 Forbidden fail-closed (no data leak)', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W3_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)

    await page.goto('/api/policies')
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByText(/access denied/i)).toBeVisible()
    await expect(page.getByTestId('api-readiness-summary')).toHaveCount(0)
    await expect(page.getByTestId('api-ops-summary')).toHaveCount(0)
    await assertNoViewportOverflow(page)

    await captureSysNormW3Screenshot(page, '06-forbidden-policies-redbc-1440x900.png')
  })
})
