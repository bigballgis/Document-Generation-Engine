/**
 * Asset library group isolation / Task Master #154 — Stage 7 UIUX evidence.
 *
 * Surfaces: Asset library list with ScopedGroupSelect filter; upload dialog with
 * required owning group. Dual-brand REDBC/GREENBC @1440×900.
 *
 * BDD SoT: docs/behavior/asset-library-group-isolation.md (BDD-ALGI-015/016 visual)
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts `
 *     e2e/asset-library-group-isolation-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Page } from '@playwright/test'

import { DEMO_GROUP_CODE, E2E_ADMIN, loginAs } from './helpers/auth'
import {
  ensureActiveLibraryAsset,
  uniqueE2eAssetKey,
} from './helpers/library-assets-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureAssetLibraryGroupIsolationLocatorScreenshot,
  captureAssetLibraryGroupIsolationScreenshot,
  dismissOnboardingTourIfPresent,
  ensureAssetLibraryGroupIsolationEvidenceDirs,
  ASSET_LIBRARY_GROUP_ISOLATION_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const UUID_LIKE =
  /[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/i

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

async function openAssetLibrary(page: Page) {
  await page.goto('/library/assets')
  await expect(page.getByRole('heading', { name: /^asset library$/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.getByText(/unable to load the asset library/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  await expect(page.getByTestId('asset-library-group-filter')).toBeVisible({ timeout: 20_000 })
}

function uploadDialog(page: Page) {
  return page.getByRole('dialog', { name: /upload library asset/i })
}

async function openUploadDialog(page: Page) {
  const headerOpen = page.getByTestId('asset-library-upload-open')
  const emptyOpen = page.getByTestId('asset-library-upload-open-empty')
  if (await headerOpen.isVisible().catch(() => false)) {
    await headerOpen.click()
  } else {
    await expect(emptyOpen).toBeVisible({ timeout: 20_000 })
    await emptyOpen.click()
  }
  await expect(uploadDialog(page)).toBeVisible()
  await expect(uploadDialog(page).getByTestId('asset-library-upload-form')).toBeVisible()
  await expect(uploadDialog(page).getByTestId('asset-library-upload-group')).toBeVisible()
}

async function searchAssetLibrary(page: Page, assetKey: string) {
  const search = page.locator('.catalog-filter-toolbar__search input')
  await expect(search).toBeVisible({ timeout: 20_000 })
  await search.fill(assetKey)
}

test.describe('Asset library group isolation UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let fixtureAssetKey = ''

  test.beforeAll(async ({ request }) => {
    ensureAssetLibraryGroupIsolationEvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    fixtureAssetKey = uniqueE2eAssetKey('E2E-ALGI-UIUX')
    await ensureActiveLibraryAsset(request, {
      assetKey: fixtureAssetKey,
      assetClass: 'IMAGE',
      groupCode: DEMO_GROUP_CODE,
      credentials: E2E_ADMIN,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(ASSET_LIBRARY_GROUP_ISOLATION_VIEWPORT)
  })

  test('01–05 dual-brand: list + group filter, upload group required, brand header', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    // --- REDBC ---
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    await assertPrimaryBrandColor(page, 'REDBC')

    await openAssetLibrary(page)
    await assertFluidLayout(page)
    await expect(page.getByTestId('asset-library-upload-open')).toBeVisible()
    await expect(page.locator('.catalog-filter-toolbar')).toBeVisible()
    await expect(page.getByTestId('asset-library-table')).toBeVisible({ timeout: 20_000 })

    const groupFilter = page.getByTestId('asset-library-group-filter')
    await expect(groupFilter.getByText(/group/i).first()).toBeVisible()
    await expect(groupFilter.locator('.el-select')).toBeVisible()

    await searchAssetLibrary(page, fixtureAssetKey)
    const row = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: fixtureAssetKey,
    })
    await expect(row).toBeVisible({ timeout: 20_000 })
    await expect(row.getByText(DEMO_GROUP_CODE, { exact: true })).toBeVisible()
    await expect(row.getByText(/^active$/i)).toBeVisible()

    const tableText = await page.getByTestId('asset-library-table').innerText()
    expect(tableText, 'raw UUID must not appear as primary table text').not.toMatch(UUID_LIKE)

    await assertNoViewportOverflow(page)
    await captureAssetLibraryGroupIsolationScreenshot(
      page,
      '01-asset-library-list-group-filter-redbc-1440x900.png',
    )
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01b-brand-header-redbc-crop.png',
    )
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      groupFilter,
      '01c-group-filter-redbc-crop.png',
    )
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      page.locator('.catalog-filter-toolbar'),
      '01d-catalog-filters-redbc-crop.png',
    )

    // Upload dialog — group required (Upload disabled until group selected)
    await openUploadDialog(page)
    const dialog = uploadDialog(page)
    await expect(dialog.getByTestId('asset-library-upload-group')).toBeVisible()
    await expect(dialog.getByRole('button', { name: /^upload$/i })).toBeDisabled()
    await expect(dialog.getByRole('button', { name: /^cancel$/i })).toBeVisible()
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      dialog,
      '02-upload-dialog-group-required-redbc-1440x900.png',
    )
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      dialog.getByTestId('asset-library-upload-group'),
      '02b-upload-group-field-redbc-crop.png',
    )
    await dialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).toHaveCount(0)

    // --- GREENBC ---
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')

    await openAssetLibrary(page)
    await searchAssetLibrary(page, fixtureAssetKey)
    const greenRow = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: fixtureAssetKey,
    })
    await expect(greenRow).toBeVisible({ timeout: 20_000 })
    await expect(page.getByTestId('asset-library-group-filter')).toBeVisible()
    await assertNoViewportOverflow(page)

    await captureAssetLibraryGroupIsolationScreenshot(
      page,
      '03-asset-library-list-group-filter-greenbc-1440x900.png',
    )
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03b-brand-header-greenbc-crop.png',
    )
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      page.getByTestId('asset-library-group-filter'),
      '03c-group-filter-greenbc-crop.png',
    )

    await openUploadDialog(page)
    const greenDialog = uploadDialog(page)
    await expect(greenDialog.getByTestId('asset-library-upload-group')).toBeVisible()
    await expect(greenDialog.getByRole('button', { name: /^upload$/i })).toBeDisabled()
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      greenDialog,
      '04-upload-dialog-group-required-greenbc-1440x900.png',
    )
    await captureAssetLibraryGroupIsolationLocatorScreenshot(
      greenDialog.getByTestId('asset-library-upload-group'),
      '04b-upload-group-field-greenbc-crop.png',
    )
    await greenDialog.getByRole('button', { name: /^cancel$/i }).click()

    const logoSrc = await page
      .locator('.shell-header .header-brand img, .shell-header .brand-logo img')
      .first()
      .getAttribute('src')
    expect(logoSrc ?? '', 'brand logo src should reference greenbc after switch').toMatch(
      /greenbc/i,
    )
  })
})
