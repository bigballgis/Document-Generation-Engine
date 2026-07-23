/**
 * CE-E02 UIUX evidence — Asset library admin (list / upload / disable)
 * Dual-brand REDBC/GREENBC @1440×900 (Stage 7).
 * BDD surfaces: BDD-CE-E02-018…020 visual/UX; role-gated controls.
 *
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/CE-E02-asset-library-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  ensureActiveLibraryAsset,
  uniqueE2eAssetKey,
} from './helpers/library-assets-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureCeE02LocatorScreenshot,
  captureCeE02Screenshot,
  CE_E02_VIEWPORT,
  ensureCeE02EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const UUID_LIKE =
  /[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/i

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openAssetLibrary(page: Page) {
  await page.goto('/library/assets')
  await expect(page.getByRole('heading', { name: /^asset library$/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.getByText(/unable to load the asset library/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
}

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return {
      scrollWidth: doc.scrollWidth,
      clientWidth: doc.clientWidth,
    }
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

async function searchAssetLibrary(page: Page, assetKey: string) {
  const search = page.locator('.catalog-filter-toolbar__search input')
  await expect(search).toBeVisible({ timeout: 20_000 })
  await search.fill(assetKey)
}

test.describe('CE-E02 Asset Library UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let fixtureAssetKey = ''

  test.beforeAll(async ({ request }) => {
    ensureCeE02EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    fixtureAssetKey = uniqueE2eAssetKey('E2E-UIUX')
    await ensureActiveLibraryAsset(request, {
      assetKey: fixtureAssetKey,
      assetClass: 'IMAGE',
    })
  })

  test('01–06 dual-brand: list, upload dialog, disable confirm, brand header', async ({
    page,
  }) => {
    await page.setViewportSize(CE_E02_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    // --- REDBC ---
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    await assertPrimaryBrandColor(page, 'REDBC')

    await openAssetLibrary(page)
    await expect(page.getByTestId('asset-library-upload-open')).toBeVisible()
    await expect(page.locator('.catalog-filter-toolbar')).toBeVisible()
    await expect(page.getByTestId('asset-library-table')).toBeVisible({ timeout: 20_000 })

    await searchAssetLibrary(page, fixtureAssetKey)
    const row = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: fixtureAssetKey,
    })
    await expect(row).toBeVisible({ timeout: 20_000 })
    // N22: Disable lives under TableEditMoreActions → More (teleports to body).
    const actions = row.getByTestId('table-edit-more-actions')
    await expect(actions).toBeVisible()
    await expect(actions.locator('.table-edit-more-actions__edit')).toHaveCount(0)
    await expect(actions.getByRole('button', { name: /^more$/i })).toBeVisible()
    await expect(row.getByText(/^active$/i)).toBeVisible()

    // Entity display: asset key as human label, no raw UUID as primary cell text
    const tableText = await page.getByTestId('asset-library-table').innerText()
    expect(tableText, 'raw UUID must not appear as primary table text').not.toMatch(UUID_LIKE)

    await assertNoViewportOverflow(page)
    await captureCeE02Screenshot(page, '01-asset-library-list-redbc-1440x900.png')
    await captureCeE02LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01b-brand-header-redbc-crop.png',
    )
    await captureCeE02LocatorScreenshot(
      page.locator('.catalog-filter-toolbar'),
      '01c-catalog-filters-redbc-crop.png',
    )
    await captureCeE02LocatorScreenshot(actions, '01d-more-only-actions-redbc-crop.png')

    // Upload dialog
    await page.getByTestId('asset-library-upload-open').click()
    const uploadDialog = page.getByRole('dialog', { name: /upload library asset/i })
    await expect(uploadDialog).toBeVisible()
    await expect(uploadDialog.getByTestId('asset-library-upload-form')).toBeVisible()
    await expect(uploadDialog.getByRole('button', { name: /^upload$/i })).toBeVisible()
    await expect(uploadDialog.getByRole('button', { name: /^cancel$/i })).toBeVisible()
    await captureCeE02LocatorScreenshot(
      uploadDialog,
      '02-upload-dialog-redbc-1440x900.png',
    )
    await uploadDialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(uploadDialog).toHaveCount(0)

    // Disable via More → teleported menu (full-page + crop — EP message-box layout is platform-known)
    await actions.getByRole('button', { name: /^more$/i }).click()
    const moreMenu = page.locator('.el-dropdown-menu:visible')
    await expect(moreMenu.getByTestId('asset-library-disable')).toBeVisible()
    await captureCeE02LocatorScreenshot(moreMenu, '02b-more-menu-disable-redbc-crop.png')
    await moreMenu.getByTestId('asset-library-disable').click()
    const confirmBox = page.locator('.el-message-box')
    await expect(confirmBox).toBeVisible()
    await expect(confirmBox.getByRole('button', { name: /^disable$/i })).toBeVisible()
    await expect(confirmBox.getByRole('button', { name: /^cancel$/i })).toBeVisible()
    await captureCeE02Screenshot(page, '03b-disable-confirm-fullpage-redbc-1440x900.png')
    await captureCeE02LocatorScreenshot(
      confirmBox,
      '03-disable-confirm-redbc-1440x900.png',
    )
    await confirmBox.getByRole('button', { name: /^cancel$/i }).click()
    await expect(confirmBox).toHaveCount(0)

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
    const greenActions = greenRow.getByTestId('table-edit-more-actions')
    await expect(greenActions).toBeVisible()
    await expect(page.getByTestId('asset-library-upload-open')).toBeVisible()
    await assertNoViewportOverflow(page)

    await captureCeE02Screenshot(page, '04-asset-library-list-greenbc-1440x900.png')
    await captureCeE02LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04b-brand-header-greenbc-crop.png',
    )

    await page.getByTestId('asset-library-upload-open').click()
    const greenDialog = page.getByRole('dialog', { name: /upload library asset/i })
    await expect(greenDialog).toBeVisible()
    await captureCeE02LocatorScreenshot(
      greenDialog,
      '05-upload-dialog-greenbc-1440x900.png',
    )
    await greenDialog.getByRole('button', { name: /^cancel$/i }).click()

    await greenActions.getByRole('button', { name: /^more$/i }).click()
    const greenMenu = page.locator('.el-dropdown-menu:visible')
    await expect(greenMenu.getByTestId('asset-library-disable')).toBeVisible()
    await greenMenu.getByTestId('asset-library-disable').click()
    const greenConfirm = page.locator('.el-message-box')
    await expect(greenConfirm).toBeVisible()
    await captureCeE02LocatorScreenshot(
      greenConfirm,
      '06-disable-confirm-greenbc-1440x900.png',
    )
    await greenConfirm.getByRole('button', { name: /^cancel$/i }).click()

    // Logo assets present for both brands (header crop already captured)
    const logoSrc = await page.locator('.shell-header .header-brand img, .shell-header .brand-logo img').first().getAttribute('src')
    expect(logoSrc ?? '', 'brand logo src should reference greenbc after switch').toMatch(
      /greenbc/i,
    )
  })
})
