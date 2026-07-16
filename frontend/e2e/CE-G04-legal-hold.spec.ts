/**
 * CE-G04 — Legal Hold management UI journeys (BDD-CE-G04-015…017 + fail-closed)
 *
 * Acceptance stack (Stage 5 for this slice): FRONTEND_PORT=4173 + backend :8080
 * BDD SoT: docs/behavior/ce-g04-legal-hold.md
 */
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Page } from '@playwright/test'

import {
  E2E_ADMIN,
  E2E_AUDIT_ADMIN,
  E2E_GROUP_ADMIN,
  loginAs,
} from './helpers/auth'
import {
  createLegalHoldViaApi,
  ensureLegalHoldTemplateFixture,
  findLegalHoldByReason,
  releaseLegalHoldViaApi,
  type LegalHoldTemplateFixture,
} from './helpers/legal-holds-api'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import { selectElementPlusOption } from './helpers/ui'
import { E2E_API_BASE_URL } from './helpers/masters-api'

// Docker :4173 is canonical for Stage 5/6 acceptance. Dev loop: FRONTEND_PORT=5173.
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ??
  (process.env.FRONTEND_PORT === '5173'
    ? 'http://127.0.0.1:5173'
    : 'http://127.0.0.1:4173')

const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
  'test-results',
  'ce-g04-legal-hold',
)

const LEGAL_HOLDS_PATH = '/governance/legal-holds'

/** AUDIT_ADMIN / GROUP_ADMIN may land on a denied default route — do not require shell. */
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

/** LR-C8 onboarding spotlight intercepts pointer events until skipped. */
async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openLegalHoldsPage(page: Page) {
  await page.goto(LEGAL_HOLDS_PATH)
  await expect(page).not.toHaveURL(/\/forbidden/)
  await expect(page.getByRole('heading', { level: 1, name: /^legal holds$/i })).toBeVisible({
    timeout: 20_000,
  })
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  await expect(page.getByText(/unable to load legal holds/i)).toHaveCount(0)
}

async function openCreateDialog(page: Page) {
  const emptyCreate = page.getByTestId('legal-hold-create-open-empty')
  const headerCreate = page.getByTestId('legal-hold-create-open')
  if (await emptyCreate.isVisible().catch(() => false)) {
    await emptyCreate.click()
  } else {
    await expect(headerCreate).toBeVisible()
    await headerCreate.click()
  }
  const dialog = page.getByTestId('legal-hold-create-dialog')
  await expect(dialog).toBeVisible({ timeout: 10_000 })
  await expect(page.getByTestId('legal-hold-create-form')).toBeVisible()
  return dialog
}

/**
 * Set Element Plus datetime picker to "now" via panel — EP may not forward data-testid.
 */
async function pickDateTimeNow(page: Page, fieldLabel: RegExp) {
  const item = page
    .getByTestId('legal-hold-create-form')
    .locator('.el-form-item')
    .filter({ has: page.getByText(fieldLabel) })
    .first()
  const editor = item.locator('.el-date-editor').first()
  await expect(editor).toBeVisible({ timeout: 10_000 })
  await editor.click()
  const panel = page.locator('.el-picker-panel:visible').last()
  await expect(panel).toBeVisible({ timeout: 10_000 })
  const nowLink = panel.locator('.el-picker-panel__link-btn').filter({ hasText: /^now$/i })
  if (await nowLink.count()) {
    await nowLink.click()
  } else {
    const today = panel.locator('.el-date-table td.available, .el-date-table td.current').first()
    await today.click()
    const confirm = panel.locator('.el-picker-panel__footer .el-button--primary')
    if (await confirm.count()) {
      await confirm.click()
    }
  }
  await expect(panel).toBeHidden({ timeout: 10_000 })
}

async function selectTemplateInCreateDialog(page: Page, externalId: string) {
  const select = page.getByTestId('legal-hold-template')
  await select.click()
  const input = select.locator('input').first()
  await input.fill(externalId)
  await expect(
    page.locator('.el-select-dropdown__item').filter({ hasText: externalId }).first(),
  ).toBeVisible({ timeout: 20_000 })
  await selectElementPlusOption(page, new RegExp(externalId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i'))
}

test.describe('CE-G04 Legal Hold UI (BDD-CE-G04-015…017)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let templateFixture: LegalHoldTemplateFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Acceptance stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Stage 5 uses FRONTEND_PORT=4173.`,
    })
    templateFixture = await ensureLegalHoldTemplateFixture(request)
  })

  test('BDD-CE-G04-015b: status filter narrows ACTIVE vs RELEASED rows', async ({
    page,
    request,
  }) => {
    const reason = `E2E CE-G04 filter ${Date.now()}`
    const hold = await createLegalHoldViaApi(request, {
      scopeType: 'TEMPLATE_WINDOW',
      reason,
      templateExternalId: templateFixture.externalId,
      effectiveFrom: new Date().toISOString(),
    })
    expect(hold.status).toBe('ACTIVE')

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openLegalHoldsPage(page)

    const table = page.getByTestId('legal-hold-table')
    const statusFilter = page.getByTestId('legal-hold-status-filter')

    await statusFilter.click()
    await selectElementPlusOption(page, /^active$/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 20_000 })
    const activeRow = table.locator('.el-table__row').filter({ hasText: reason }).first()
    await expect(activeRow).toBeVisible({ timeout: 20_000 })
    await expect(activeRow.getByTestId('legal-hold-status-ACTIVE')).toBeVisible()

    await statusFilter.click()
    await selectElementPlusOption(page, /^released$/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 20_000 })
    await expect(table.locator('.el-table__row').filter({ hasText: reason })).toHaveCount(0)

    await releaseLegalHoldViaApi(request, hold.id)

    // Force refetch: watch only fires on filter value change
    await statusFilter.click()
    await selectElementPlusOption(page, /^all statuses$/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 20_000 })
    await statusFilter.click()
    await selectElementPlusOption(page, /^released$/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 20_000 })
    const releasedRow = table.locator('.el-table__row').filter({ hasText: reason }).first()
    await expect(releasedRow).toBeVisible({ timeout: 20_000 })
    await expect(releasedRow.getByTestId('legal-hold-status-RELEASED')).toBeVisible()

    await statusFilter.click()
    await selectElementPlusOption(page, /^active$/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 20_000 })
    await expect(table.locator('.el-table__row').filter({ hasText: reason })).toHaveCount(0)

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-CE-G04-015b-status-filter.png'),
      fullPage: true,
    })
  })

  test('BDD-CE-G04-015: GLOBAL_ADMIN sees Legal Holds page and can open create', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    const nav = managementNav(page)
    await expect(nav.getByRole('button', { name: /^legal holds$/i })).toBeVisible({
      timeout: 20_000,
    })
    await nav.getByRole('button', { name: /^legal holds$/i }).click()

    await expect(page).toHaveURL(new RegExp(`${LEGAL_HOLDS_PATH.replace(/\//g, '\\/')}$`))
    await expect(page.getByRole('heading', { level: 1, name: /^legal holds$/i })).toBeVisible()
    await expect(page.getByTestId('legal-hold-filters')).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const dialog = await openCreateDialog(page)
    await expect(dialog.getByTestId('legal-hold-scope-type')).toBeVisible()
    await expect(page.getByTestId('legal-hold-create-submit')).toBeVisible()
    await page.getByTestId('legal-hold-create-cancel').click()
    await expect(dialog).toBeHidden({ timeout: 10_000 })

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-CE-G04-015-list.png'),
      fullPage: true,
    })
  })

  test('BDD-CE-G04-016: non-GLOBAL_ADMIN deep-link is Forbidden; nav has no entry', async ({
    page,
  }) => {
    await signInWithoutShellAssert(page, E2E_AUDIT_ADMIN)
    await page.goto(LEGAL_HOLDS_PATH)

    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByText(/access denied/i)).toBeVisible()
    await expect(page.getByTestId('legal-hold-table')).toHaveCount(0)
    await expect(page.getByTestId('legal-hold-create-open')).toHaveCount(0)

    // Nav entry must be absent when shell is available; otherwise Forbidden alone satisfies.
    const nav = managementNav(page)
    if (await nav.isVisible().catch(() => false)) {
      await expect(nav.getByRole('button', { name: /^legal holds$/i })).toHaveCount(0)
    }

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-CE-G04-016-audit-forbidden.png'),
      fullPage: true,
    })

    // Second role: GROUP_ADMIN also fail-closed on deep-link
    await signInWithoutShellAssert(page, E2E_GROUP_ADMIN)
    await page.goto(LEGAL_HOLDS_PATH)
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByTestId('legal-hold-table')).toHaveCount(0)
  })

  test('BDD-CE-G04-017: UI create TEMPLATE_WINDOW then Release → ACTIVE → RELEASED', async ({
    page,
    request,
  }) => {
    const reason = `E2E CE-G04 hold ${Date.now()}`

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openLegalHoldsPage(page)

    const dialog = await openCreateDialog(page)
    await expect(dialog.getByTestId('legal-hold-scope-type')).toBeVisible()
    // Default scope is TEMPLATE_WINDOW
    await dialog.getByTestId('legal-hold-reason').fill(reason)
    await selectTemplateInCreateDialog(page, templateFixture.externalId)
    await pickDateTimeNow(page, /^effective from/i)

    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/legal-holds') &&
        !response.url().includes('/release'),
      { timeout: 30_000 },
    )

    await page.getByTestId('legal-hold-create-submit').click()
    const createResponse = await createResponsePromise
    expect(createResponse.status(), await createResponse.text()).toBe(201)
    await expect(dialog).toBeHidden({ timeout: 15_000 })

    const table = page.getByTestId('legal-hold-table')
    await expect(table).toBeVisible({ timeout: 20_000 })
    const row = table.locator('.el-table__row').filter({ hasText: reason }).first()
    await expect(row).toBeVisible({ timeout: 20_000 })
    await expect(row.getByTestId('legal-hold-status-ACTIVE')).toBeVisible()
    await expect(row.getByText(/template window/i)).toBeVisible()
    await expect(
      row.locator('.entity-link-cell__text').filter({ hasText: templateFixture.externalId }),
    ).toBeVisible()

    const created = await findLegalHoldByReason(request, reason)
    expect(created).toBeTruthy()
    expect(created!.status).toBe('ACTIVE')
    expect(created!.scopeType).toBe('TEMPLATE_WINDOW')

    await row.getByTestId('legal-hold-release').click()
    const confirmBox = page.locator('.el-message-box')
    await expect(confirmBox).toBeVisible()
    await expect(confirmBox.getByText(/release legal hold/i)).toBeVisible()
    await expect(confirmBox.getByText(created!.holdExternalId)).toBeVisible()

    const releaseResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' && response.url().includes('/release'),
      { timeout: 30_000 },
    )
    await confirmBox.getByRole('button', { name: /^release hold$/i }).click()
    const releaseResponse = await releaseResponsePromise
    expect(releaseResponse.status(), await releaseResponse.text()).toBe(200)

    await expect(row.getByTestId('legal-hold-status-RELEASED')).toBeVisible({ timeout: 20_000 })
    await expect(row.getByTestId('legal-hold-release')).toHaveCount(0)

    const released = await findLegalHoldByReason(request, reason)
    expect(released).toBeTruthy()
    expect(released!.status).toBe('RELEASED')
    expect(released!.releasedByUsername).toBeTruthy()

    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BDD-CE-G04-017-released.png'),
      fullPage: true,
    })
  })

  test('BDD-CE-G04-017b: UI create INVOCATION_SET hold shows ACTIVE', async ({ page, request }) => {
    const reason = `E2E CE-G04 inv-set ${Date.now()}`
    const invId = `E2E-INV-HOLD-${Date.now()}`

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openLegalHoldsPage(page)

    const dialog = await openCreateDialog(page)
    await dialog.getByTestId('legal-hold-scope-type').click()
    await selectElementPlusOption(page, /invocation set/i)
    await dialog.getByTestId('legal-hold-reason').fill(reason)
    await dialog.getByTestId('legal-hold-invocation-ids').fill(invId)

    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/legal-holds') &&
        !response.url().includes('/release'),
      { timeout: 30_000 },
    )
    await page.getByTestId('legal-hold-create-submit').click()
    const createResponse = await createResponsePromise
    expect(createResponse.status(), await createResponse.text()).toBe(201)
    await expect(dialog).toBeHidden({ timeout: 15_000 })

    const table = page.getByTestId('legal-hold-table')
    const row = table.locator('.el-table__row').filter({ hasText: reason }).first()
    await expect(row).toBeVisible({ timeout: 20_000 })
    await expect(row.getByTestId('legal-hold-status-ACTIVE')).toBeVisible()
    await expect(row.getByText(/invocation set/i)).toBeVisible()
    await expect(row.getByText(/1 invocation/i)).toBeVisible()

    const created = await findLegalHoldByReason(request, reason)
    expect(created?.scopeType).toBe('INVOCATION_SET')
    expect(created?.invocationCount).toBe(1)

    // Cleanup: release so list stays tidy for subsequent runs
    await row.getByTestId('legal-hold-release').click()
    const box = page.locator('.el-message-box')
    await expect(box).toBeVisible()
    await box.getByRole('button', { name: /^release hold$/i }).click()
    await expect(row.getByTestId('legal-hold-status-RELEASED')).toBeVisible({ timeout: 20_000 })
  })
})
