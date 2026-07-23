/**
 * PQH N22 / TM #162 UIUX evidence — Catalog Edit/More (TableEditMoreActions)
 * Dual-brand REDBC/GREENBC @1440×900 (Stage 7).
 * BDD surfaces: BDD-PQH-N22-001…014 visual/spacing/hierarchy/a11y for Actions.
 *
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/PQH-N22-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Locator, type Page } from '@playwright/test'

import {
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import {
  createLegalHoldViaApi,
  ensureLegalHoldTemplateFixture,
  releaseLegalHoldViaApi,
  type LegalHoldView,
} from './helpers/legal-holds-api'
import {
  ensureActiveLibraryAsset,
  uniqueE2eAssetKey,
} from './helpers/library-assets-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  capturePqhN22LocatorScreenshot,
  capturePqhN22Screenshot,
  dismissOnboardingTourIfPresent,
  ensurePqhN22EvidenceDirs,
  PQH_N22_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

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

/** Row Actions use TableEditMoreActions; dropdown items teleport to body. */
async function openRowMoreMenu(row: Locator, options: { expectEditHidden?: boolean } = {}) {
  const actions = row.getByTestId('table-edit-more-actions')
  await expect(actions).toBeVisible({ timeout: 15_000 })
  if (options.expectEditHidden) {
    await expect(actions.locator('.table-edit-more-actions__edit')).toHaveCount(0)
  }
  await actions.getByRole('button', { name: /^more$/i }).click()
  return actions
}

test.describe('PQH N22 UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  let fixtureAssetKey = ''
  let fixtureHold: LegalHoldView
  let fixtureReason = ''

  test.beforeAll(async ({ request }) => {
    ensurePqhN22EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080).`,
    })
    fixtureAssetKey = uniqueE2eAssetKey('E2E-N22-UIUX')
    await ensureActiveLibraryAsset(request, {
      assetKey: fixtureAssetKey,
      assetClass: 'IMAGE',
    })
    const template = await ensureLegalHoldTemplateFixture(request)
    fixtureReason = `E2E PQH-N22 UIUX hold ${Date.now()}`
    const from = new Date().toISOString().replace(/\.\d{3}Z$/, 'Z')
    fixtureHold = await createLegalHoldViaApi(request, {
      scopeType: 'TEMPLATE_WINDOW',
      reason: fixtureReason,
      templateId: template.templateId,
      effectiveFrom: from,
    })
  })

  test.afterAll(async ({ request }) => {
    if (fixtureHold?.id && fixtureHold.status === 'ACTIVE') {
      await releaseLegalHoldViaApi(request, fixtureHold.id).catch(() => undefined)
    }
  })

  test('01 Asset Library More-only Disable dual-brand', async ({ page }) => {
    await page.setViewportSize(PQH_N22_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await page.goto('/library/assets')
      await expect(page.getByRole('heading', { name: /^asset library$/i })).toBeVisible({
        timeout: 30_000,
      })
      await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
      const search = page.locator('.catalog-filter-toolbar__search input')
      await search.fill(fixtureAssetKey)
      const row = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
        hasText: fixtureAssetKey,
      })
      await expect(row).toBeVisible({ timeout: 20_000 })
      const actions = await openRowMoreMenu(row, { expectEditHidden: true })
      const moreMenu = page.locator('.el-dropdown-menu:visible')
      await expect(moreMenu.getByTestId('asset-library-disable')).toBeVisible()
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await capturePqhN22Screenshot(page, `01-asset-library-more-menu-${suffix}-1440x900.png`)
      await capturePqhN22LocatorScreenshot(actions, `01b-asset-more-only-${suffix}-crop.png`)
      await capturePqhN22LocatorScreenshot(moreMenu, `01c-asset-disable-menu-${suffix}-crop.png`)
      await page.keyboard.press('Escape')
    }
  })

  test('02 Legal Holds More-only Release dual-brand', async ({ page }) => {
    await page.setViewportSize(PQH_N22_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await page.goto('/governance/legal-holds')
      await expect(page.getByRole('heading', { level: 1, name: /^legal holds$/i })).toBeVisible({
        timeout: 30_000,
      })
      await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
      const row = page
        .getByTestId('legal-hold-table')
        .locator('.el-table__row')
        .filter({ hasText: fixtureReason })
      await expect(row).toBeVisible({ timeout: 20_000 })
      const actions = await openRowMoreMenu(row, { expectEditHidden: true })
      const moreMenu = page.locator('.el-dropdown-menu:visible')
      await expect(moreMenu.getByTestId('legal-hold-release')).toBeVisible()
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await capturePqhN22Screenshot(page, `02-legal-hold-more-menu-${suffix}-1440x900.png`)
      await capturePqhN22LocatorScreenshot(actions, `02b-legal-more-only-${suffix}-crop.png`)
      await capturePqhN22LocatorScreenshot(moreMenu, `02c-legal-release-menu-${suffix}-crop.png`)
      await page.keyboard.press('Escape')
    }
  })

  test('03 API Invocations Open summary + settings under More', async ({ page }) => {
    await page.setViewportSize(PQH_N22_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await assertPrimaryBrandColor(page, 'REDBC')
    await page.goto('/api/invocations')
    await expect(page.locator('.page-header h1')).toHaveText(/invocation records|调用记录/i, {
      timeout: 20_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })

    const rows = page.locator('[data-testid="api-invocations-table-card"] .el-table__body tr')
    const rowCount = await rows.count()
    test.skip(rowCount === 0, 'No invocation rows — settings/More evidence skipped (honest empty)')

    const row = rows.first()
    const actions = row.getByTestId('table-edit-more-actions')
    await expect(actions).toBeVisible()
    await expect(actions.getByRole('button', { name: /open summary|打开摘要/i })).toBeVisible()
    await capturePqhN22LocatorScreenshot(actions, '03b-invocations-edit-more-redbc-crop.png')

    await openRowMoreMenu(row)
    const moreMenu = page.locator('.el-dropdown-menu:visible')
    await expect(moreMenu.getByTestId('api-invocations-open-settings')).toBeVisible()
    await assertNoViewportOverflow(page)
    await capturePqhN22Screenshot(page, '03-invocations-more-settings-redbc-1440x900.png')
    await capturePqhN22LocatorScreenshot(moreMenu, '03c-invocations-settings-menu-redbc-crop.png')
    await page.keyboard.press('Escape')

    await switchBrand(page, 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')
    await page.goto('/api/invocations')
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })
    const greenRow = page
      .locator('[data-testid="api-invocations-table-card"] .el-table__body tr')
      .first()
    await openRowMoreMenu(greenRow)
    const greenMenu = page.locator('.el-dropdown-menu:visible')
    await expect(greenMenu.getByTestId('api-invocations-open-settings')).toBeVisible()
    await capturePqhN22Screenshot(page, '03d-invocations-more-settings-greenbc-1440x900.png')
    await capturePqhN22LocatorScreenshot(greenMenu, '03e-invocations-settings-menu-greenbc-crop.png')
    await page.keyboard.press('Escape')
  })

  test('04 Users/Groups Edit/More regression dual-brand', async ({ page }) => {
    await page.setViewportSize(PQH_N22_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)

      await page.goto('/entitlement/users')
      await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
        timeout: 20_000,
      })
      const userActions = page.getByTestId('table-edit-more-actions').first()
      await expect(userActions).toBeVisible({ timeout: 30_000 })
      await expect(userActions.getByRole('button', { name: /^edit$/i })).toBeVisible()
      await expect(userActions.getByRole('button', { name: /^more$/i })).toBeVisible()
      await userActions.getByRole('button', { name: /^more$/i }).click()
      await expect(page.locator('.el-dropdown-menu:visible').getByRole('menuitem').first()).toBeVisible()
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await capturePqhN22Screenshot(page, `04-users-edit-more-${suffix}-1440x900.png`)
      await capturePqhN22LocatorScreenshot(userActions, `04b-users-edit-more-${suffix}-crop.png`)
      await capturePqhN22LocatorScreenshot(
        page.locator('.el-dropdown-menu:visible'),
        `04c-users-more-menu-${suffix}-crop.png`,
      )
      await page.keyboard.press('Escape')

      await page.goto('/entitlement/groups')
      await expect(page.getByRole('heading', { name: /group management/i })).toBeVisible({
        timeout: 20_000,
      })
      const groupActions = page.getByTestId('table-edit-more-actions').first()
      await expect(groupActions).toBeVisible({ timeout: 30_000 })
      await expect(groupActions.getByRole('button', { name: /^edit$/i })).toBeVisible()
      await expect(groupActions.getByRole('button', { name: /^more$/i })).toBeVisible()
      await groupActions.getByRole('button', { name: /^more$/i }).click()
      await expect(page.locator('.el-dropdown-menu:visible').getByRole('menuitem').first()).toBeVisible()
      await capturePqhN22Screenshot(page, `04d-groups-edit-more-${suffix}-1440x900.png`)
      await capturePqhN22LocatorScreenshot(groupActions, `04e-groups-edit-more-${suffix}-crop.png`)
      await page.keyboard.press('Escape')
    }
  })
})
