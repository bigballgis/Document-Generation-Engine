/**
 * CE-G03 UIUX evidence — PII badges, handling radios, EXPLICIT confirm, schema piiCategory
 * Dual-brand REDBC/GREENBC @1440x900 (+ narrow spot-check). Stage 7.
 * BDD: BDD-CE-G03-012 / 013 / 014 visual surfaces.
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  prepareCeG03PiiSchemaFixture,
  type CeG03PiiFixture,
} from './helpers/ce-g03-testdata-pii-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { openFolDevEditorTestingTab } from './helpers/template-testing-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  captureCeG03LocatorScreenshot,
  captureCeG03Screenshot,
  CE_G03_NARROW_VIEWPORT,
  CE_G03_VIEWPORT,
  ensureCeG03EvidenceDirs,
  switchBrand,
  switchLocale,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

function editDialog(page: Page) {
  return page.locator('.test-data-set-edit-dialog')
}

async function dismissOnboardingTourIfPresent(page: Page) {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openCreateDialog(page: Page, createButtonName: RegExp = /^create data set$/i) {
  const panel = page.locator('.test-data-set-panel')
  await panel.getByRole('button', { name: createButtonName }).click()
  const dialog = editDialog(page)
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  await expect(dialog.getByTestId('test-data-set-edit-form')).toBeVisible()
  return dialog
}

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

async function expectNoCriticalAxeViolations(page: Page, label: string) {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

async function captureBrandHeader(page: Page, filename: string) {
  const header = page
    .locator('.shell-header .header-brand, .management-shell__header, header.app-header')
    .first()
  if (await header.isVisible().catch(() => false)) {
    await captureCeG03LocatorScreenshot(header, filename)
    return
  }
  await captureCeG03Screenshot(page, filename)
}

async function openDesignVariablesTab(
  page: Page,
  templateId: string,
  request: import('@playwright/test').APIRequestContext,
) {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight) {
    throw new Error(`No IN_FLIGHT line for template ${templateId}`)
  }
  await page.goto(`/templates/${templateId}/dev/${inFlight.devVersionId}?workspaceTab=design`)
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
  const designSubTabs = page.locator('.design-sub-tabs')
  await designSubTabs.getByRole('tab', { name: /^variables$/i }).click()
  await expect(page.locator('.variable-tree-panel')).toBeVisible({ timeout: 20_000 })
}

test.describe('CE-G03 UIUX evidence — PII governance dual-brand @1440', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let fixture: CeG03PiiFixture

  test.beforeAll(async ({ request }) => {
    ensureCeG03EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    fixture = await prepareCeG03PiiSchemaFixture(request)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CE_G03_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
  })

  test('01 REDBC: PII badge + handling group (BDD-012)', async ({ page, request }) => {
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page)

    await expect(dialog.getByTestId(`pii-badge-${fixture.piiKey}`)).toBeVisible()
    await expect(dialog.getByTestId(`pii-badge-${fixture.piiKey}`)).toContainText(/PII/i)
    await expect(dialog.getByTestId(`pii-badge-${fixture.nonPiiKey}`)).toHaveCount(0)
    await expect(dialog.getByTestId('pii-handling-group')).toBeVisible()
    await expect(dialog.getByTestId('pii-handling-synthetic')).toBeVisible()
    await expect(dialog.getByTestId('pii-handling-explicit')).toBeVisible()

    await assertNoViewportOverflow(page)
    await captureCeG03Screenshot(page, '01-pii-badge-handling-redbc-en-1440x900.png')
    await captureCeG03LocatorScreenshot(dialog, '01b-pii-dialog-crop-redbc-en.png')
    await captureCeG03LocatorScreenshot(
      dialog.getByTestId('pii-handling-group'),
      '01c-pii-handling-group-crop-redbc-en.png',
    )
    await captureBrandHeader(page, '01d-brand-header-redbc-en.png')
  })

  test('02 REDBC: EXPLICIT confirm dialog validation + filled (BDD-014)', async ({ page, request }) => {
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page)

    await dialog.getByTestId('test-data-set-name').fill(`UIUX CE-G03 explicit ${Date.now()}`)
    await dialog.getByTestId(`schema-input-${fixture.piiKey}`).fill('Sensitive Sample Name')
    await dialog.getByTestId('pii-handling-explicit').click()
    await dialog.getByTestId('test-data-set-save').click()

    const confirmDialog = page.getByTestId('pii-explicit-confirm-dialog')
    await expect(confirmDialog).toBeVisible({ timeout: 10_000 })
    await captureCeG03Screenshot(page, '02-explicit-confirm-empty-redbc-en-1440x900.png')
    await captureCeG03LocatorScreenshot(confirmDialog, '02b-explicit-confirm-crop-redbc-en.png')

    await confirmDialog.getByTestId('pii-explicit-confirm-submit').click()
    await expect(confirmDialog.getByText(/reason is required|secondary confirmation/i)).toBeVisible()
    await captureCeG03LocatorScreenshot(confirmDialog, '02c-explicit-confirm-validation-crop-redbc-en.png')

    await confirmDialog.getByTestId('pii-confirm-reason').fill(
      'UIUX CE-G03 explicit sensitive confirmation sample for dual-brand evidence',
    )
    await confirmDialog.getByTestId('pii-secondary-confirm').click()
    await captureCeG03LocatorScreenshot(confirmDialog, '02d-explicit-confirm-filled-crop-redbc-en.png')

    await confirmDialog.getByTestId('pii-explicit-confirm-cancel').click()
    await expect(confirmDialog).toBeHidden({ timeout: 10_000 })
  })

  test('03 GREENBC: badge + handling + confirm + brand header', async ({ page, request }) => {
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page)

    await expect(dialog.getByTestId(`pii-badge-${fixture.piiKey}`)).toBeVisible()
    await expect(dialog.getByTestId('pii-handling-group')).toBeVisible()
    await captureCeG03Screenshot(page, '03-pii-badge-handling-greenbc-en-1440x900.png')
    await captureCeG03LocatorScreenshot(dialog, '03b-pii-dialog-crop-greenbc-en.png')
    await captureBrandHeader(page, '03c-brand-header-greenbc-en.png')

    await dialog.getByTestId('test-data-set-name').fill(`UIUX CE-G03 green ${Date.now()}`)
    await dialog.getByTestId(`schema-input-${fixture.piiKey}`).fill('Green Sensitive Sample')
    await dialog.getByTestId('pii-handling-explicit').click()
    await dialog.getByTestId('test-data-set-save').click()

    const confirmDialog = page.getByTestId('pii-explicit-confirm-dialog')
    await expect(confirmDialog).toBeVisible({ timeout: 10_000 })
    await captureCeG03Screenshot(page, '04-explicit-confirm-greenbc-en-1440x900.png')
    await captureCeG03LocatorScreenshot(confirmDialog, '04b-explicit-confirm-crop-greenbc-en.png')
    await confirmDialog.getByTestId('pii-explicit-confirm-cancel').click()
  })

  test('05 Schema Variables: piiCategory select (REDBC)', async ({ page, request }) => {
    await openDesignVariablesTab(page, fixture.templateId, request)

    // camelCase keys nest under a folder (customerName -> Customer / Name); search expands matches.
    const search = page.locator('.variable-tree-panel .search-input input').first()
    await search.fill(fixture.piiKey)
    await expect(page.locator('.variable-tree')).toBeVisible()

    // Expand caret on first collapsed folder if still collapsed.
    const expandIcon = page.locator('.variable-tree .el-tree-node__expand-icon:not(.is-leaf)').first()
    if (await expandIcon.isVisible().catch(() => false)) {
      const expanded = await expandIcon.evaluate((el) => el.classList.contains('expanded'))
      if (!expanded) {
        await expandIcon.click()
      }
    }

    await captureCeG03Screenshot(page, '05-variables-tree-search-redbc-en-1440x900.png')

    const treeBadge = page.getByTestId(`variable-pii-badge-${fixture.piiKey}`)
    if ((await treeBadge.count()) > 0 && (await treeBadge.isVisible().catch(() => false))) {
      await captureCeG03Screenshot(page, '05b-variables-tree-with-pii-badge-redbc-en-1440x900.png')
    }

    const editButton = page
      .locator('.variable-tree .el-tree-node')
      .filter({ hasText: fixture.piiKey })
      .getByRole('button', { name: /^edit$/i })
      .first()
    await expect(editButton).toBeVisible({ timeout: 10_000 })
    await editButton.click()

    const piiSelect = page.getByTestId('variable-pii-category')
    await expect(piiSelect).toBeVisible({ timeout: 10_000 })
    await captureCeG03Screenshot(page, '05c-variable-pii-category-select-redbc-en-1440x900.png')
    await captureCeG03LocatorScreenshot(
      page.locator('.el-dialog').filter({ has: piiSelect }).first(),
      '05d-variable-edit-dialog-crop-redbc-en.png',
    )

    await piiSelect.click()
    await expect(
      page.getByRole('option', { name: /personal name|none \(not pii/i }).first(),
    ).toBeVisible({ timeout: 5_000 })
    await captureCeG03Screenshot(page, '05e-pii-category-options-redbc-en-1440x900.png')
    await page.keyboard.press('Escape')
  })

  test('06 a11y: no critical axe on PII create + confirm dialogs', async ({ page, request }) => {
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page)
    await expect(dialog.getByTestId('pii-handling-group')).toBeVisible()
    await expectNoCriticalAxeViolations(page, 'CE-G03 create dialog with PII handling')

    await dialog.getByTestId('test-data-set-name').fill(`UIUX CE-G03 a11y ${Date.now()}`)
    await dialog.getByTestId(`schema-input-${fixture.piiKey}`).fill('A11y Sample')
    await dialog.getByTestId('pii-handling-explicit').click()
    await dialog.getByTestId('test-data-set-save').click()
    const confirmDialog = page.getByTestId('pii-explicit-confirm-dialog')
    await expect(confirmDialog).toBeVisible({ timeout: 10_000 })
    await expectNoCriticalAxeViolations(page, 'CE-G03 EXPLICIT confirm dialog')
    await captureCeG03LocatorScreenshot(confirmDialog, '06-a11y-explicit-confirm-crop-redbc-en.png')
  })

  test('07 narrow + zh-CN spot-check (REDBC)', async ({ page, request }) => {
    await page.setViewportSize(CE_G03_NARROW_VIEWPORT)
    await dismissOnboardingTourIfPresent(page)
    await switchLocale(page, 'zh-CN')
    await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN')

    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page, /新建数据集/)
    await expect(dialog.getByTestId(`pii-badge-${fixture.piiKey}`)).toBeVisible()
    await expect(dialog.getByTestId('pii-handling-group')).toBeVisible()
    await assertNoViewportOverflow(page)
    await captureCeG03Screenshot(page, '07-pii-dialog-zhcn-narrow-redbc-1280x800.png')
    await captureCeG03LocatorScreenshot(dialog, '07b-pii-dialog-crop-zhcn-narrow-redbc.png')
  })
})
