/**
 * PQH N22 / TM #162 — Catalog row Edit/More via TableEditMoreActions.
 *
 * BDD SoT: docs/behavior/pqh-n22-catalog-row-actions.md
 *   BDD-PQH-N22-001…004 — Asset Library More-only + Disable confirm
 *   BDD-PQH-N22-006…007 — Legal Holds More-only + Release confirm
 *   BDD-PQH-N22-009…010 — API Invocations Open summary primary + settings under More
 *   BDD-PQH-N22-011 — Users/Groups Edit/More regression lock
 *   BDD-PQH-N22-012 — shared data-testid="table-edit-more-actions"
 *
 * Unit/component cover 001–012 mount contracts; this file locks user journeys.
 * BDD-005/008 (fail-closed entitlements) + 013/014 (deferred / no invent) are
 * unit + docs gates — not re-asserted as full journeys here.
 *
 * Acceptance stack (Stage 5/6): FRONTEND_PORT=4173 + backend :8080
 *
 * Run:
 *   pnpm -C frontend exec playwright test e2e/PQH-N22-catalog-row-actions.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Locator, type Page } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_ADMIN,
  E2E_AUDIT_ADMIN,
  E2E_GROUP_ADMIN,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import {
  createLegalHoldViaApi,
  ensureLegalHoldTemplateFixture,
  releaseLegalHoldViaApi,
} from './helpers/legal-holds-api'
import {
  ensureActiveLibraryAsset,
  uniqueE2eAssetKey,
} from './helpers/library-assets-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { dismissOnboardingTourIfPresent } from './helpers/uiux-evidence'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const EVIDENCE_DIR = path.join(__dirname, 'evidence', 'PQH-N22')
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function captureEvidence(page: Page, filename: string) {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  await page.screenshot({ path: path.join(EVIDENCE_DIR, filename), fullPage: true })
}

/** AUDIT_ADMIN may land on a denied default route — do not require shell. */
async function signInWithoutShellAssert(
  page: Page,
  credentials: { username: string; password: string },
) {
  await page.context().clearCookies()
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.reload({ waitUntil: 'domcontentloaded' })
  await expect(page.getByPlaceholder('10000001')).toBeVisible({ timeout: 15_000 })
  await page.getByPlaceholder('10000001').fill(credentials.username)
  await page.locator('input[type="password"]').fill(credentials.password)
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
}

async function openAssetLibrary(page: Page) {
  await page.goto('/library/assets')
  await expect(page.getByRole('heading', { name: /^asset library$/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.getByText(/unable to load the asset library/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
}

async function searchAssetLibrary(page: Page, assetKey: string) {
  const search = page.locator('.catalog-filter-toolbar__search input')
  await expect(search).toBeVisible({ timeout: 20_000 })
  await search.fill(assetKey)
}

async function openLegalHolds(page: Page) {
  await page.goto('/governance/legal-holds')
  await expect(page).not.toHaveURL(/\/forbidden/)
  await expect(page.getByRole('heading', { level: 1, name: /^legal holds$/i })).toBeVisible({
    timeout: 20_000,
  })
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
}

/**
 * Row Actions use TableEditMoreActions; dropdown items teleport to body.
 * Assert More-only (no Edit) when showEdit=false catalogs are under test.
 */
async function openRowMoreMenu(row: Locator, options: { expectEditHidden?: boolean } = {}) {
  const actions = row.getByTestId('table-edit-more-actions')
  await expect(actions).toBeVisible({ timeout: 15_000 })
  if (options.expectEditHidden) {
    await expect(actions.locator('.table-edit-more-actions__edit')).toHaveCount(0)
    await expect(actions.getByRole('button', { name: /^edit$/i })).toHaveCount(0)
  }
  await actions.getByRole('button', { name: /^more$/i }).click()
  return actions
}

async function confirmDisableMessageBox(page: Page) {
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await expect(box.getByText(/disable asset/i)).toBeVisible()
  await box.getByRole('button', { name: /^disable$/i }).click()
}

async function confirmReleaseMessageBox(page: Page) {
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await expect(box.getByText(/release legal hold/i)).toBeVisible()
  await box.getByRole('button', { name: /^release hold$/i }).click()
}

test.describe('PQH N22 catalog row Edit/More (BDD-PQH-N22)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Acceptance stack required (${FRONTEND_BASE_URL} + :8080). Stage 5 uses FRONTEND_PORT=4173.`,
    })
  })

  test('BDD-PQH-N22-001/002/003/004/012: Asset Library More-only Disable + confirm', async ({
    page,
    request,
  }) => {
    const assetKey = uniqueE2eAssetKey('E2E-N22-AL')
    await ensureActiveLibraryAsset(request, {
      assetKey,
      assetClass: 'IMAGE',
      groupCode: DEMO_GROUP_CODE,
    })

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openAssetLibrary(page)
    await searchAssetLibrary(page, assetKey)

    const row = page.getByTestId('asset-library-table').locator('.el-table__row').filter({
      hasText: assetKey,
    })
    await expect(row).toBeVisible({ timeout: 20_000 })

    // Shared primitive; not a bare standalone danger button as sole Actions chrome.
    await expect(row.getByTestId('table-edit-more-actions')).toBeVisible()
    await expect(row.getByTestId('asset-library-disable')).toHaveCount(0)

    await openRowMoreMenu(row, { expectEditHidden: true })
    const disableItem = page
      .locator('.el-dropdown-menu:visible')
      .getByTestId('asset-library-disable')
    await expect(disableItem).toBeVisible()
    await expect(disableItem).toHaveText(/^disable$/i)
    await disableItem.click()
    await confirmDisableMessageBox(page)

    await expect(page.getByText(/asset disabled/i)).toBeVisible({ timeout: 20_000 })
    await expect(
      page.getByTestId('asset-library-table').locator('.el-table__row').filter({ hasText: assetKey }),
    ).toHaveCount(0)

    await captureEvidence(page, 'BDD-PQH-N22-004-asset-library-disable.png')
  })

  test('BDD-PQH-N22-005: Asset Library Actions fail-closed without entitlement', async ({
    page,
  }) => {
    await signInWithoutShellAssert(page, E2E_AUDIT_ADMIN)
    await page.goto('/library/assets')
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByTestId('asset-library-table')).toHaveCount(0)
    await expect(page.getByTestId('table-edit-more-actions')).toHaveCount(0)
  })

  test('BDD-PQH-N22-006/007/012: Legal Holds More-only Release + confirm', async ({
    page,
    request,
  }) => {
    const templateFixture = await ensureLegalHoldTemplateFixture(request)
    const reason = `E2E PQH-N22 hold ${Date.now()}`
    const hold = await createLegalHoldViaApi(request, {
      scopeType: 'TEMPLATE_WINDOW',
      reason,
      templateId: templateFixture.templateId,
      effectiveFrom: new Date().toISOString(),
    })

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openLegalHolds(page)

    const row = page
      .getByTestId('legal-hold-table')
      .locator('.el-table__row')
      .filter({ hasText: reason })
      .first()
    await expect(row).toBeVisible({ timeout: 20_000 })
    await expect(row.getByTestId('legal-hold-status-ACTIVE')).toBeVisible()

    await expect(row.getByTestId('table-edit-more-actions')).toBeVisible()
    await expect(row.getByTestId('legal-hold-release')).toHaveCount(0)

    await openRowMoreMenu(row, { expectEditHidden: true })
    const releaseItem = page
      .locator('.el-dropdown-menu:visible')
      .getByTestId('legal-hold-release')
    await expect(releaseItem).toBeVisible()
    await expect(releaseItem).toHaveText(/^release$/i)
    await releaseItem.click()
    await confirmReleaseMessageBox(page)

    await expect(row.getByTestId('legal-hold-status-RELEASED')).toBeVisible({ timeout: 20_000 })
    await expect(row.getByTestId('table-edit-more-actions')).toHaveCount(0)

    // Idempotent cleanup if UI path left anything ACTIVE (should not).
    if (hold.status === 'ACTIVE') {
      try {
        await releaseLegalHoldViaApi(request, hold.id)
      } catch {
        // already released via UI
      }
    }

    await captureEvidence(page, 'BDD-PQH-N22-007-legal-hold-release.png')
  })

  test('BDD-PQH-N22-008: Legal Holds Actions fail-closed without entitlement', async ({
    page,
  }) => {
    await signInWithoutShellAssert(page, E2E_AUDIT_ADMIN)
    await page.goto('/governance/legal-holds')
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByTestId('legal-hold-table')).toHaveCount(0)
    await expect(page.getByTestId('table-edit-more-actions')).toHaveCount(0)
  })

  test('BDD-PQH-N22-009/010/012: API Invocations Open summary + settings under More', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await page.goto('/api/invocations')
    await expect(page.locator('.page-header h1')).toHaveText(/invocation records|调用记录/i, {
      timeout: 20_000,
    })
    await expect(page.getByTestId('api-invocations-table-card')).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })

    const rows = page.locator('[data-testid="api-invocations-table-card"] .el-table__body tr')
    const empty = page.getByText(/no invocation records|暂无调用记录/i)
    await expect(empty.or(rows.first())).toBeVisible({ timeout: 30_000 })

    const rowCount = await rows.count()
    test.skip(
      rowCount === 0,
      'No in-scope invocation rows — Stage 5 stack has honest empty; Open summary/settings journey needs ≥1 row',
    )

    const row = rows.first()
    const actions = row.getByTestId('table-edit-more-actions')
    await expect(actions).toBeVisible()

    const openDetail = actions.getByRole('button', { name: /open summary|打开摘要/i })
    await expect(openDetail).toBeVisible()
    await openDetail.click()

    const drawer = page.getByTestId('invocation-summary-drawer')
    await expect(drawer).toBeVisible({ timeout: 20_000 })
    await expect(drawer.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await drawer.getByRole('button', { name: /close|关闭/i }).click().catch(async () => {
      await page.keyboard.press('Escape')
    })
    await expect(drawer).toBeHidden({ timeout: 10_000 })

    await openRowMoreMenu(row)
    // EP keeps one teleported menu per row; scope to the visible popper.
    const settingsItem = page
      .locator('.el-dropdown-menu:visible')
      .getByTestId('api-invocations-open-settings')
    await expect(settingsItem).toBeVisible()
    await expect(settingsItem).toHaveText(/api settings|api 设置/i)
    await settingsItem.click()

    await expect(page).toHaveURL(/\/api\/packages\/[^/]+\/settings/, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })

    await captureEvidence(page, 'BDD-PQH-N22-010-api-invocations-settings.png')
  })

  test('BDD-PQH-N22-011: Users/Groups Edit/More regression lock', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    await page.goto('/entitlement/users')
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByText(/unable to load users/i)).not.toBeVisible()
    const userActions = page.getByTestId('table-edit-more-actions').first()
    await expect(userActions).toBeVisible({ timeout: 30_000 })
    await expect(userActions.getByRole('button', { name: /^edit$/i })).toBeVisible()
    await expect(userActions.getByRole('button', { name: /^more$/i })).toBeVisible()
    await userActions.getByRole('button', { name: /^more$/i }).click()
    await expect(page.getByRole('menuitem', { name: /enable|disable/i })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: /reset password/i })).toBeVisible()
    await page.keyboard.press('Escape')

    await page.goto('/entitlement/groups')
    await expect(page.getByRole('heading', { name: /group management/i })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByText(/unable to load groups/i)).not.toBeVisible()
    const groupActions = page.getByTestId('table-edit-more-actions').first()
    await expect(groupActions).toBeVisible({ timeout: 30_000 })
    await expect(groupActions.getByRole('button', { name: /^edit$/i })).toBeVisible()
    await expect(groupActions.getByRole('button', { name: /^more$/i })).toBeVisible()
    await groupActions.getByRole('button', { name: /^more$/i }).click()
    await expect(page.getByRole('menuitem', { name: /enable|disable/i })).toBeVisible()
    await page.keyboard.press('Escape')

    await captureEvidence(page, 'BDD-PQH-N22-011-users-groups-edit-more.png')
  })
})
