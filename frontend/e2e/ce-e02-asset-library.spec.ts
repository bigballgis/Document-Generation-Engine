import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Page } from '@playwright/test'

import {
  E2E_ADMIN,
  E2E_AUDIT_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import { assertDockerStackReady } from './helpers/core-fortress-f7'
import {
  E2E_ASSET_PNG_PATH,
  ensureActiveLibraryAsset,
  listLibraryAssetsViaApi,
  uniqueE2eAssetKey,
  uploadLibraryAssetViaApi,
} from './helpers/library-assets-api'
import { reLoginAs, selectElementPlusOption } from './helpers/ui'

/**
 * CE-E02 — Asset library management surface (Task #79).
 * BDD-CE-E02-015 / 018 / 019 / 020 — functional journeys only.
 *
 *   pnpm -C frontend exec playwright test e2e/ce-e02-asset-library.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 */

const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'CE-E02-asset-library',
)

async function openAssetLibrary(page: Page) {
  await page.goto('/library/assets')
  await expect(page.getByRole('heading', { name: /^asset library$/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.getByText(/unable to load the asset library/i)).not.toBeVisible()
  await expect(page.getByText(/an internal error occurred/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
}

function uploadDialog(page: Page) {
  return page.getByRole('dialog', { name: /upload library asset/i })
}

async function fillUploadForm(
  page: Page,
  options: { assetKey: string; assetClass?: RegExp; filePath?: string },
) {
  const dialog = uploadDialog(page)
  await expect(dialog).toBeVisible()
  if (options.assetClass) {
    await dialog.locator('.el-select').click()
    await selectElementPlusOption(page, options.assetClass)
  }
  await dialog.getByRole('textbox', { name: /asset key/i }).fill(options.assetKey)
  await dialog.locator('input[type="file"]').setInputFiles(options.filePath ?? E2E_ASSET_PNG_PATH)
  await expect(dialog.getByRole('button', { name: /^upload$/i })).toBeEnabled()
}

async function confirmDisableMessageBox(page: Page) {
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await box.getByRole('button', { name: /^disable$/i }).click()
}

async function searchAssetLibrary(page: Page, assetKey: string) {
  const search = page.locator('.catalog-filter-toolbar__search input')
  await expect(search).toBeVisible({ timeout: 20_000 })
  await search.fill(assetKey)
}

/** AUDIT_ADMIN may land on a denied default route without shell — do not use loginAs. */
async function signInWithoutShellAssert(
  page: Page,
  credentials: { username: string; password: string },
) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await expect(page.getByPlaceholder('10000001')).toBeVisible()
  await page.getByPlaceholder('10000001').fill(credentials.username)
  await page.locator('input[type="password"]').fill(credentials.password)
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
}

test.describe('CE-E02 asset library management', () => {
  test.beforeAll(async ({ request }) => {
    const ready = await assertDockerStackReady(request)
    test.skip(!ready, 'Docker acceptance stack not ready at :4173/:8080')
  })

  test('BDD-CE-E02-018: author uploads IMAGE and list shows new row', async ({ page }) => {
    const assetKey = uniqueE2eAssetKey('E2E-IMG')
    const pageErrors: string[] = []
    page.on('pageerror', (error) => pageErrors.push(error.message))

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openAssetLibrary(page)

    await page.getByTestId('asset-library-upload-open').click()
    // Author defaults to IMAGE; still set key + file explicitly.
    await fillUploadForm(page, { assetKey })
    await uploadDialog(page).getByRole('button', { name: /^upload$/i }).click()

    await expect(page.getByText(/asset uploaded/i)).toBeVisible({ timeout: 20_000 })
    await expect(uploadDialog(page)).toHaveCount(0)

    await searchAssetLibrary(page, assetKey)
    await expect(page.getByTestId('asset-library-table').getByText(assetKey)).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByTestId('asset-library-table').getByText(/^image$/i)).toBeVisible()
    await expect(page.getByTestId('asset-library-table').getByText(/^active$/i).first()).toBeVisible()

    expect(pageErrors, `Unexpected page errors: ${pageErrors.join('; ')}`).toEqual([])
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-CE-E02-018-image-upload.png'),
      fullPage: true,
    })
  })

  test('BDD-CE-E02-019: author SEAL gate; approver can upload SEAL', async ({ page, request }) => {
    const assetKey = uniqueE2eAssetKey('E2E-SEAL')

    const authorDenied = await uploadLibraryAssetViaApi(request, {
      assetKey,
      assetClass: 'SEAL',
      credentials: E2E_TEMPLATE_AUTHOR,
    })
    expect(authorDenied.status).toBe(403)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openAssetLibrary(page)
    await page.getByTestId('asset-library-upload-open').click()
    const authorDialog = uploadDialog(page)
    await expect(authorDialog).toBeVisible()
    // Click the EP select wrapper — the inner combobox input is intercepted by the placeholder.
    await authorDialog.locator('.el-select').click()
    const authorDropdown = page.locator('.el-select-dropdown:visible')
    await expect(authorDropdown.getByRole('option', { name: /^image$/i })).toBeVisible()
    await expect(authorDropdown.getByRole('option', { name: /^other$/i })).toBeVisible()
    await expect(authorDropdown.getByRole('option', { name: /^seal$/i })).toHaveCount(0)
    await page.keyboard.press('Escape')
    await authorDialog.getByRole('button', { name: /^cancel$/i }).click()

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await openAssetLibrary(page)
    await page.getByTestId('asset-library-upload-open').click()
    // Approver options are SEAL-only; class is pre-selected — only key + file needed.
    await fillUploadForm(page, { assetKey })
    await uploadDialog(page).getByRole('button', { name: /^upload$/i }).click()

    await expect(page.getByText(/asset uploaded/i)).toBeVisible({ timeout: 20_000 })
    await searchAssetLibrary(page, assetKey)
    await expect(page.getByTestId('asset-library-table').getByText(assetKey)).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByTestId('asset-library-table').getByText(/^seal$/i)).toBeVisible()

    const listed = await listLibraryAssetsViaApi(request, {
      q: assetKey,
      status: 'ACTIVE',
      assetClass: 'SEAL',
      credentials: E2E_ADMIN,
    })
    expect(listed.some((row) => row.assetKey === assetKey)).toBe(true)

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-CE-E02-019-seal-gate.png'),
      fullPage: true,
    })
  })

  test('BDD-CE-E02-020: admin disables ACTIVE asset; default list hides it', async ({
    page,
    request,
  }) => {
    const assetKey = uniqueE2eAssetKey('E2E-DIS')
    await ensureActiveLibraryAsset(request, { assetKey, assetClass: 'IMAGE' })

    await loginAs(page, E2E_ADMIN)
    await openAssetLibrary(page)

    await searchAssetLibrary(page, assetKey)
    const row = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: assetKey,
    })
    await expect(row).toBeVisible({ timeout: 20_000 })
    await row.getByTestId('asset-library-disable').click()
    await confirmDisableMessageBox(page)

    await expect(page.getByText(/asset disabled/i)).toBeVisible({ timeout: 20_000 })
    await expect(
      page.getByTestId('asset-library-table').locator('.el-table__row').filter({ hasText: assetKey }),
    ).toHaveCount(0)

    const disabled = await listLibraryAssetsViaApi(request, {
      q: assetKey,
      status: 'DISABLED',
      credentials: E2E_ADMIN,
    })
    expect(disabled.some((row) => row.assetKey === assetKey && row.status === 'DISABLED')).toBe(
      true,
    )

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-CE-E02-020-disable.png'),
      fullPage: true,
    })
  })

  test('BDD-CE-E02-015: AUDIT_ADMIN deep-link is Forbidden', async ({ page }) => {
    await signInWithoutShellAssert(page, E2E_AUDIT_ADMIN)
    await page.goto('/library/assets')

    await expect(page).toHaveURL(/\/forbidden/)
    await expect(page.getByText(/access denied/i)).toBeVisible()
    await expect(page.getByTestId('asset-library-table')).toHaveCount(0)

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-CE-E02-015-audit-forbidden.png'),
      fullPage: true,
    })
  })
})
