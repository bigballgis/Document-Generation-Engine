/**
 * CE-G04 UIUX evidence — Legal Hold admin (list / create / release)
 * Dual-brand REDBC/GREENBC @1440×900 (Stage 7).
 * BDD surfaces: BDD-CE-G04-015…017 visual/UX; GLOBAL_ADMIN catalog.
 *
 * Acceptance stack (Stage 5/7): Docker FE :4173 + API :8080
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/CE-G04-legal-hold-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import {
  createLegalHoldViaApi,
  ensureLegalHoldTemplateFixture,
  releaseLegalHoldViaApi,
  type LegalHoldView,
} from './helpers/legal-holds-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { selectElementPlusOption } from './helpers/ui'
import {
  captureCeG04LocatorScreenshot,
  captureCeG04Screenshot,
  CE_G04_NARROW_VIEWPORT,
  CE_G04_VIEWPORT,
  ensureCeG04EvidenceDirs,
  switchBrand,
  switchLocale,
} from './helpers/uiux-evidence'

/** Default 4173 to match playwright.docker.config + stack-readiness; override via E2E_BASE_URL / FRONTEND_PORT. */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const LEGAL_HOLDS_PATH = '/governance/legal-holds'

const UUID_LIKE =
  /[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/i

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
    timeout: 30_000,
  })
  await expect(page.getByText(/unable to load legal holds/i)).not.toBeVisible()
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

async function expectNoCriticalAxeViolations(page: Page, label: string) {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
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

test.describe('CE-G04 Legal Hold UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let fixtureHold: LegalHoldView
  let fixtureReason = ''

  test.beforeAll(async ({ request }) => {
    ensureCeG04EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Acceptance stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    const template = await ensureLegalHoldTemplateFixture(request)
    fixtureReason = `E2E CE-G04 UIUX hold ${Date.now()}`
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

  test('01–08 dual-brand: list, create dialogs, release confirm, empty, a11y', async ({
    page,
  }) => {
    await page.setViewportSize(CE_G04_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    // --- REDBC ---
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    await assertPrimaryBrandColor(page, 'REDBC')

    await openLegalHoldsPage(page)
    await expect(page.getByTestId('legal-hold-filters')).toBeVisible()
    await expect(page.getByTestId('legal-hold-status-filter')).toBeVisible()
    await expect(page.getByTestId('legal-hold-create-open')).toBeVisible()
    await expect(page.getByTestId('legal-hold-table')).toBeVisible({ timeout: 20_000 })

    const row = page
      .getByTestId('legal-hold-table')
      .locator('.el-table__row')
      .filter({ hasText: fixtureReason })
    await expect(row).toBeVisible({ timeout: 20_000 })
    await expect(row.getByTestId('legal-hold-status-ACTIVE')).toBeVisible()
    // N22: Release lives under TableEditMoreActions → More (teleports to body).
    const actions = row.getByTestId('table-edit-more-actions')
    await expect(actions).toBeVisible()
    await expect(actions.locator('.table-edit-more-actions__edit')).toHaveCount(0)
    await expect(actions.getByRole('button', { name: /^more$/i })).toBeVisible()
    await expect(row.getByText(fixtureHold.holdExternalId)).toBeVisible()

    const tableText = await page.getByTestId('legal-hold-table').innerText()
    expect(tableText, 'raw UUID must not appear as primary table text').not.toMatch(UUID_LIKE)

    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'legal-holds list REDBC')

    await captureCeG04Screenshot(page, '01-legal-hold-list-loaded-redbc-1440x900.png')
    await captureCeG04LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01b-brand-header-redbc-crop.png',
    )
    await captureCeG04LocatorScreenshot(
      page.getByTestId('legal-hold-filters'),
      '01c-status-filter-redbc-crop.png',
    )
    await captureCeG04LocatorScreenshot(row, '01d-active-row-redbc-crop.png')
    await captureCeG04LocatorScreenshot(actions, '01e-more-only-actions-redbc-crop.png')

    // Create dialog — TEMPLATE_WINDOW default
    const dialog = await openCreateDialog(page)
    await expect(dialog.getByTestId('legal-hold-scope-type')).toBeVisible()
    await expect(dialog.getByTestId('legal-hold-template')).toBeVisible()
    await expect(
      dialog.locator('.el-form-item').filter({ hasText: /^effective from/i }).locator('.el-date-editor'),
    ).toBeVisible()
    await expect(page.getByTestId('legal-hold-create-submit')).toBeVisible()
    await expect(page.getByTestId('legal-hold-create-cancel')).toBeVisible()
    await captureCeG04LocatorScreenshot(
      dialog,
      '02-create-dialog-template-window-redbc-1440x900.png',
    )

    // Create dialog — INVOCATION_SET fields
    await dialog.getByTestId('legal-hold-scope-type').click()
    await selectElementPlusOption(page, /invocation set/i)
    await expect(dialog.getByTestId('legal-hold-invocation-ids')).toBeVisible()
    await captureCeG04LocatorScreenshot(
      dialog,
      '03-create-dialog-invocation-set-redbc-1440x900.png',
    )
    await page.getByTestId('legal-hold-create-cancel').click()
    await expect(dialog).toBeHidden({ timeout: 10_000 })

    // Release via More → teleported menu (cancel — do not mutate fixture yet)
    await actions.getByRole('button', { name: /^more$/i }).click()
    const moreMenu = page.locator('.el-dropdown-menu:visible')
    await expect(moreMenu.getByTestId('legal-hold-release')).toBeVisible()
    await captureCeG04LocatorScreenshot(moreMenu, '03b-more-menu-release-redbc-crop.png')
    await moreMenu.getByTestId('legal-hold-release').click()
    const confirmBox = page.locator('.el-message-box')
    await expect(confirmBox).toBeVisible()
    await expect(confirmBox.getByText(/release legal hold/i)).toBeVisible()
    await expect(confirmBox.getByText(fixtureHold.holdExternalId)).toBeVisible()
    await expect(confirmBox.getByRole('button', { name: /^release hold$/i })).toBeVisible()
    await expect(confirmBox.getByRole('button', { name: /^cancel$/i })).toBeVisible()
    await captureCeG04Screenshot(page, '04b-release-confirm-fullpage-redbc-1440x900.png')
    await captureCeG04LocatorScreenshot(
      confirmBox,
      '04-release-confirm-redbc-1440x900.png',
    )
    await confirmBox.getByRole('button', { name: /^cancel$/i }).click()
    await expect(confirmBox).toHaveCount(0)

    // Empty state via status filter with no matching rows (synthetic: Released-only when fixture is Active)
    await page.getByTestId('legal-hold-status-filter').click()
    await selectElementPlusOption(page, /^released$/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 20_000 })
    // If other RELEASED holds exist from prior runs, still capture; prefer empty panel when present
    const emptyPanel = page.locator('.empty-state-panel, [data-testid="empty-state-panel"]').first()
    const emptyTitle = page.getByText(/no legal holds yet/i)
    if (await emptyTitle.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await expect(page.getByTestId('legal-hold-create-open-empty')).toBeVisible()
      await captureCeG04Screenshot(page, '05-legal-hold-empty-released-filter-redbc-1440x900.png')
      if (await emptyPanel.isVisible().catch(() => false)) {
        await captureCeG04LocatorScreenshot(
          emptyPanel,
          '05b-empty-state-panel-redbc-crop.png',
        )
      }
    } else {
      // Fallback: loaded RELEASED list still documents filter + density
      await captureCeG04Screenshot(page, '05-legal-hold-empty-released-filter-redbc-1440x900.png')
    }

    // Restore Active filter for GREENBC loaded capture
    await page.getByTestId('legal-hold-status-filter').click()
    await selectElementPlusOption(page, /^active$/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 20_000 })
    await expect(row).toBeVisible({ timeout: 20_000 })

    // Locale spot-check (layout must not break)
    await switchLocale(page, 'zh-CN')
    await assertNoViewportOverflow(page)
    await captureCeG04Screenshot(page, '05c-legal-hold-list-zh-CN-redbc-1440x900.png')
    await switchLocale(page, 'en')

    // Narrow desktop spot-check (table may scroll horizontally — capture only; OA gate is 1440)
    await page.setViewportSize(CE_G04_NARROW_VIEWPORT)
    await captureCeG04Screenshot(page, '05d-legal-hold-list-redbc-1280x800.png')
    await page.setViewportSize(CE_G04_VIEWPORT)

    // --- GREENBC ---
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')

    await openLegalHoldsPage(page)
    await page.getByTestId('legal-hold-status-filter').click()
    await selectElementPlusOption(page, /^active$/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 20_000 })
    const greenRow = page
      .getByTestId('legal-hold-table')
      .locator('.el-table__row')
      .filter({ hasText: fixtureReason })
    await expect(greenRow).toBeVisible({ timeout: 20_000 })
    await expect(page.getByTestId('legal-hold-create-open')).toBeVisible()
    await assertNoViewportOverflow(page)

    await captureCeG04Screenshot(page, '06-legal-hold-list-loaded-greenbc-1440x900.png')
    await captureCeG04LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '06b-brand-header-greenbc-crop.png',
    )

    const greenDialog = await openCreateDialog(page)
    await expect(greenDialog.getByTestId('legal-hold-create-form')).toBeVisible()
    await captureCeG04LocatorScreenshot(
      greenDialog,
      '07-create-dialog-template-window-greenbc-1440x900.png',
    )
    await page.getByTestId('legal-hold-create-cancel').click()

    const greenActions = greenRow.getByTestId('table-edit-more-actions')
    await expect(greenActions).toBeVisible()
    await greenActions.getByRole('button', { name: /^more$/i }).click()
    const greenMenu = page.locator('.el-dropdown-menu:visible')
    await expect(greenMenu.getByTestId('legal-hold-release')).toBeVisible()
    await greenMenu.getByTestId('legal-hold-release').click()
    const greenConfirm = page.locator('.el-message-box')
    await expect(greenConfirm).toBeVisible()
    await captureCeG04LocatorScreenshot(
      greenConfirm,
      '08-release-confirm-greenbc-1440x900.png',
    )
    await greenConfirm.getByRole('button', { name: /^cancel$/i }).click()

    const logoSrc = await page
      .locator('.shell-header .header-brand img, .shell-header .brand-logo img')
      .first()
      .getAttribute('src')
    expect(logoSrc ?? '', 'brand logo src should reference greenbc after switch').toMatch(
      /greenbc/i,
    )
  })
})
