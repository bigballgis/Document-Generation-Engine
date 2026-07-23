import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Page } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
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
import { reLoginAs } from './helpers/ui'

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

async function closeOpenSelectDropdowns(page: Page) {
  if ((await page.locator('.el-select-dropdown:visible').count()) > 0) {
    await page.keyboard.press('Escape')
    await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
  }
}

async function selectVisibleOption(page: Page, optionText: string | RegExp) {
  const dropdown = page.locator('.el-select-dropdown:visible').last()
  await expect(dropdown).toBeVisible()
  await dropdown.getByRole('option', { name: optionText }).click()
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
}

async function selectUploadGroup(page: Page, groupCode: string) {
  const dialog = uploadDialog(page)
  const group = dialog.getByTestId('asset-library-upload-group')
  await expect(group).toBeVisible()
  // Locked single-group actors may already have the group selected.
  if (await group.getByText(groupCode, { exact: true }).isVisible().catch(() => false)) {
    return
  }
  await closeOpenSelectDropdowns(page)
  await group.locator('.el-select').click()
  await selectVisibleOption(page, groupCode)
  await expect(group).toContainText(groupCode)
}

async function fillUploadForm(
  page: Page,
  options: { assetKey: string; assetClass?: RegExp; filePath?: string; groupCode?: string },
) {
  const dialog = uploadDialog(page)
  await expect(dialog).toBeVisible()
  await expect(dialog.getByTestId('asset-library-upload-form')).toBeVisible()
  await selectUploadGroup(page, options.groupCode ?? DEMO_GROUP_CODE)
  if (options.assetClass) {
    const classSelect = dialog
      .locator('.el-form-item')
      .filter({ hasText: /asset class/i })
      .locator('.el-select')
    await classSelect.click()
    await selectVisibleOption(page, options.assetClass)
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
    const imageRow = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: assetKey,
    })
    await expect(imageRow).toBeVisible({ timeout: 20_000 })
    await expect(imageRow.getByText(/^image$/i)).toBeVisible()
    await expect(imageRow.getByText(/^active$/i)).toBeVisible()

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
      groupCode: DEMO_GROUP_CODE,
      credentials: E2E_TEMPLATE_AUTHOR,
    })
    expect(authorDenied.status).toBe(403)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openAssetLibrary(page)
    await page.getByTestId('asset-library-upload-open').click()
    const authorDialog = uploadDialog(page)
    await expect(authorDialog).toBeVisible()
    // Asset class select (group ScopedGroupSelect is separate).
    await authorDialog
      .locator('.el-form-item')
      .filter({ hasText: /asset class/i })
      .locator('.el-select')
      .click()
    const authorDropdown = page.locator('.el-select-dropdown:visible').last()
    await expect(authorDropdown.getByRole('option', { name: /^image$/i })).toBeVisible()
    await expect(authorDropdown.getByRole('option', { name: /^other$/i })).toBeVisible()
    await expect(authorDropdown.getByRole('option', { name: /^seal$/i })).toHaveCount(0)
    await closeOpenSelectDropdowns(page)
    await authorDialog.getByRole('button', { name: /^cancel$/i }).click()

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await openAssetLibrary(page)
    await page.getByTestId('asset-library-upload-open').click()
    // Wave 5: former TEMPLATE_APPROVER is GROUP_ADMIN — can upload IMAGE/OTHER/SEAL; pick SEAL.
    await fillUploadForm(page, {
      assetKey,
      assetClass: /^seal$/i,
      groupCode: DEMO_GROUP_CODE,
    })
    await uploadDialog(page).getByRole('button', { name: /^upload$/i }).click()

    await expect(page.getByText(/asset uploaded/i)).toBeVisible({ timeout: 20_000 })
    await searchAssetLibrary(page, assetKey)
    const sealRow = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: assetKey,
    })
    await expect(sealRow).toBeVisible({ timeout: 20_000 })
    await expect(sealRow.getByText(/^seal$/i)).toBeVisible({ timeout: 10_000 })

    const listed = await listLibraryAssetsViaApi(request, {
      q: assetKey,
      status: 'ACTIVE',
      assetClass: 'SEAL',
      groupCode: DEMO_GROUP_CODE,
      credentials: E2E_ADMIN,
    })
    expect(listed.some((row) => row.assetKey === assetKey && row.groupCode === DEMO_GROUP_CODE)).toBe(
      true,
    )

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
    await ensureActiveLibraryAsset(request, {
      assetKey,
      assetClass: 'IMAGE',
      groupCode: DEMO_GROUP_CODE,
    })

    await loginAs(page, E2E_ADMIN)
    await openAssetLibrary(page)

    await searchAssetLibrary(page, assetKey)
    const row = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: assetKey,
    })
    await expect(row).toBeVisible({ timeout: 20_000 })
    // N22: Disable lives under TableEditMoreActions → More (not a bare Actions button).
    const actions = row.getByTestId('table-edit-more-actions')
    await expect(actions).toBeVisible()
    await actions.getByRole('button', { name: /^more$/i }).click()
    await page.locator('.el-dropdown-menu:visible').getByTestId('asset-library-disable').click()
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
