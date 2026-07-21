import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Locator, type Page } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_ADMIN,
  FOL_GROUP_CODE,
  loginAs,
} from './helpers/auth'
import {
  E2E_ASSET_PNG_PATH,
  ensureActiveLibraryAsset,
  listLibraryAssetsViaApi,
  uniqueE2eAssetKey,
} from './helpers/library-assets-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'

/**
 * Asset library group isolation — FE journeys BDD-ALGI-015 / BDD-ALGI-016 (Task #154).
 *
 *   pnpm -C frontend exec playwright test e2e/asset-library-group-isolation.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 */

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'asset-library-group-isolation',
)

async function openAssetLibrary(page: Page) {
  await page.goto('/library/assets')
  await expect(page.getByRole('heading', { name: /^asset library$/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.getByText(/unable to load the asset library/i)).not.toBeVisible()
  await expect(page.getByText(/an internal error occurred/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
  await expect(page.getByTestId('asset-library-group-filter')).toBeVisible({ timeout: 20_000 })
}

function uploadDialog(page: Page) {
  return page.getByRole('dialog', { name: /upload library asset/i })
}

function groupFilterRoot(page: Page) {
  return page.getByTestId('asset-library-group-filter')
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

async function selectScopedGroup(root: Locator, page: Page, groupCode: string) {
  await closeOpenSelectDropdowns(page)
  await root.locator('.el-select').click()
  const dropdown = page
    .locator('.el-select-dropdown:visible')
    .filter({ has: page.getByRole('option', { name: groupCode, exact: true }) })
  await expect(dropdown).toBeVisible()
  await dropdown.getByRole('option', { name: groupCode, exact: true }).click()
  await expect(root).toContainText(groupCode)
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
}

async function clearScopedGroupFilter(page: Page) {
  const filter = groupFilterRoot(page)
  const select = filter.locator('.el-select')
  await select.hover()
  const clear = select.locator('.el-select__clear')
  await expect(clear).toBeVisible({ timeout: 10_000 })
  await clear.click()
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
}

async function waitForAssetsList(
  page: Page,
  predicate: (url: URL) => boolean,
): Promise<void> {
  await page.waitForResponse((response) => {
    if (response.request().method() !== 'GET' || !response.ok()) {
      return false
    }
    const url = new URL(response.url())
    if (!url.pathname.includes('/library/assets')) {
      return false
    }
    return predicate(url)
  })
}

async function searchAssetLibrary(page: Page, assetKey: string) {
  const search = page.locator('.catalog-filter-toolbar__search input')
  await expect(search).toBeVisible({ timeout: 20_000 })
  await search.fill(assetKey)
}

async function confirmDisableMessageBox(page: Page) {
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await box.getByRole('button', { name: /^disable$/i }).click()
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

test.describe('Asset library group isolation (BDD-ALGI-015/016)', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-ALGI-015: group filter present; upload requires group; scoped upload + disable', async ({
    page,
  }) => {
    const assetKey = uniqueE2eAssetKey('E2E-ALGI015')
    const pageErrors: string[] = []
    page.on('pageerror', (error) => pageErrors.push(error.message))

    await loginAs(page, E2E_ADMIN)
    await openAssetLibrary(page)

    // GLOBAL sees clearable ScopedGroupSelect filter.
    const filter = groupFilterRoot(page)
    await expect(filter.getByText(/group/i).first()).toBeVisible()
    await expect(filter.locator('.el-select')).toBeVisible()

    await openUploadDialog(page)
    const dialog = uploadDialog(page)

    // Fill class / key / file but leave group empty → Upload stays disabled (ALGI-C7 / ALGI-C10).
    const classSelect = dialog.locator('.el-form-item').filter({ hasText: /asset class/i }).locator('.el-select')
    await classSelect.click()
    await selectVisibleOption(page, /^image$/i)
    await dialog.getByRole('textbox', { name: /asset key/i }).fill(assetKey)
    await dialog.locator('input[type="file"]').setInputFiles(E2E_ASSET_PNG_PATH)
    await expect(dialog.getByTestId('asset-library-upload-group')).not.toContainText(DEMO_GROUP_CODE)
    await expect(dialog.getByRole('button', { name: /^upload$/i })).toBeDisabled()

    // Select owning group → submit succeeds; list shows row with that group.
    await selectScopedGroup(dialog.getByTestId('asset-library-upload-group'), page, DEMO_GROUP_CODE)
    await expect(dialog.getByRole('button', { name: /^upload$/i })).toBeEnabled()
    await dialog.getByRole('button', { name: /^upload$/i }).click()

    await expect(page.getByText(/asset uploaded/i)).toBeVisible({ timeout: 20_000 })
    await expect(uploadDialog(page)).toHaveCount(0)

    // Ensure list is unscoped or filtered to RETAIL so the new row is visible.
    const filterSelect = filter.locator('.el-select')
    const filterValue = (await filterSelect.innerText()).trim()
    if (filterValue && !filterValue.includes(DEMO_GROUP_CODE) && !/all groups|filter by group/i.test(filterValue)) {
      await clearScopedGroupFilter(page)
    }
    if (!(await filterSelect.innerText()).includes(DEMO_GROUP_CODE)) {
      const listPromise = waitForAssetsList(page, (url) => url.searchParams.get('groupCode') === DEMO_GROUP_CODE)
      await selectScopedGroup(filter, page, DEMO_GROUP_CODE)
      await listPromise
    }

    await searchAssetLibrary(page, assetKey)
    const row = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: assetKey,
    })
    await expect(row).toBeVisible({ timeout: 20_000 })
    await expect(row.getByText(DEMO_GROUP_CODE, { exact: true })).toBeVisible()
    await expect(row.getByText(/^active$/i)).toBeVisible()

    // Disable still works for (groupCode, assetKey) identity.
    await row.getByTestId('asset-library-disable').click()
    await confirmDisableMessageBox(page)
    await expect(page.getByText(/asset disabled/i)).toBeVisible({ timeout: 20_000 })
    await expect(
      page.getByTestId('asset-library-table').locator('.el-table__row').filter({ hasText: assetKey }),
    ).toHaveCount(0)

    expect(pageErrors, `Unexpected page errors: ${pageErrors.join('; ')}`).toEqual([])
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-ALGI-015-upload-requires-group.png'),
      fullPage: true,
    })
  })

  test('BDD-ALGI-016: GLOBAL clear filter shows multi-group; filter scopes list', async ({
    page,
    request,
  }) => {
    const retailKey = uniqueE2eAssetKey('E2E-ALGI016-R')
    const corpKey = uniqueE2eAssetKey('E2E-ALGI016-C')

    await ensureActiveLibraryAsset(request, {
      assetKey: retailKey,
      assetClass: 'IMAGE',
      groupCode: DEMO_GROUP_CODE,
      credentials: E2E_ADMIN,
    })
    await ensureActiveLibraryAsset(request, {
      assetKey: corpKey,
      assetClass: 'IMAGE',
      groupCode: FOL_GROUP_CODE,
      credentials: E2E_ADMIN,
    })

    const seeded = await listLibraryAssetsViaApi(request, {
      status: 'ACTIVE',
      credentials: E2E_ADMIN,
    })
    expect(seeded.some((row) => row.assetKey === retailKey && row.groupCode === DEMO_GROUP_CODE)).toBe(
      true,
    )
    expect(seeded.some((row) => row.assetKey === corpKey && row.groupCode === FOL_GROUP_CODE)).toBe(
      true,
    )

    await loginAs(page, E2E_ADMIN)
    await openAssetLibrary(page)

    // Clear / omit filter → rows from ≥2 groups visible (search narrows to our keys).
    const filter = groupFilterRoot(page)
    const selectText = (await filter.locator('.el-select').innerText()).trim()
    if (selectText && !/all groups|filter by group/i.test(selectText)) {
      const clearPromise = waitForAssetsList(page, (url) => !url.searchParams.get('groupCode'))
      await clearScopedGroupFilter(page)
      await clearPromise
    }

    // Search for a shared prefix; both keys share E2E-ALGI016.
    await searchAssetLibrary(page, 'E2E-ALGI016')
    await expect(
      page.getByTestId('asset-library-table').locator('.el-table__row').filter({ hasText: retailKey }),
    ).toBeVisible({ timeout: 20_000 })
    await expect(
      page.getByTestId('asset-library-table').locator('.el-table__row').filter({ hasText: corpKey }),
    ).toBeVisible({ timeout: 20_000 })

    // Filter one group → only that group's seeded row remains among the pair.
    const corpFilterPromise = waitForAssetsList(
      page,
      (url) => url.searchParams.get('groupCode') === FOL_GROUP_CODE,
    )
    await selectScopedGroup(filter, page, FOL_GROUP_CODE)
    await corpFilterPromise

    await searchAssetLibrary(page, 'E2E-ALGI016')
    await expect(
      page.getByTestId('asset-library-table').locator('.el-table__row').filter({ hasText: corpKey }),
    ).toBeVisible({ timeout: 20_000 })
    await expect(
      page.getByTestId('asset-library-table').locator('.el-table__row').filter({ hasText: retailKey }),
    ).toHaveCount(0)

    const table = page.getByTestId('asset-library-table')
    const visibleRows = table.locator('.el-table__row')
    const rowCount = await visibleRows.count()
    for (let i = 0; i < rowCount; i += 1) {
      await expect(visibleRows.nth(i)).toContainText(FOL_GROUP_CODE)
    }

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-ALGI-016-global-filter.png'),
      fullPage: true,
    })
  })
})
