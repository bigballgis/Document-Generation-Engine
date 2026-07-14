/**
 * CE-U03 UIUX evidence — schema-driven test data dialog
 * Dual-brand REDBC/GREENBC @1920, zh-CN spot-check, large-payload collapse (Stage 7).
 */
import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  prepareCeU03CompactSchemaFixture,
  prepareCeU03LargeSchemaFixture,
  type CeU03LargeSchemaFixture,
  type CeU03SchemaFixture,
} from './helpers/ce-u03-testdata-schema-api'
import { openFolDevEditorTestingTab } from './helpers/template-testing-api'
import {
  captureCeU03LocatorScreenshot,
  captureCeU03Screenshot,
  CE_U03_VIEWPORT,
  ensureCeU03EvidenceDirs,
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

async function openCreateDialog(page: Page, createButtonName: RegExp = /^create data set$/i) {
  const panel = page.locator('.test-data-set-panel')
  await panel.getByRole('button', { name: createButtonName }).click()
  const dialog = editDialog(page)
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  await expect(dialog.getByTestId('test-data-set-edit-form')).toBeVisible()
  return dialog
}

async function ensureAdvancedJsonExpanded(dialog: ReturnType<typeof editDialog>) {
  const editor = dialog.getByTestId('advanced-json-editor')
  if (await editor.isVisible().catch(() => false)) {
    return editor
  }
  await dialog.getByTestId('advanced-json-collapse').click()
  await expect(editor).toBeVisible({ timeout: 10_000 })
  return editor
}

async function dismissOnboardingTourIfPresent(page: Page) {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function captureBrandHeader(page: Page, filename: string) {
  const header = page.locator('.management-shell__header, header.app-header, .app-header').first()
  if (await header.isVisible().catch(() => false)) {
    await captureCeU03LocatorScreenshot(header, filename)
    return
  }
  await captureCeU03Screenshot(page, filename)
}

test.describe('CE-U03 UIUX evidence — schema form dialog dual-brand @1920', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let compact: CeU03SchemaFixture
  let large: CeU03LargeSchemaFixture

  test.beforeAll(async ({ request }) => {
    ensureCeU03EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    compact = await prepareCeU03CompactSchemaFixture(request)
    large = await prepareCeU03LargeSchemaFixture(request)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CE_U03_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
  })

  test('01–04 REDBC: form, advanced JSON, fieldErrors, compute-skip density', async ({
    page,
    request,
  }) => {
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    await expect(dialog.getByTestId('generate-schema-skeleton')).toBeVisible()
    for (const key of compact.enterableKeys) {
      await expect(dialog.getByTestId(`schema-field-${key}`)).toBeVisible()
    }
    await expect(dialog.getByTestId(`schema-field-${compact.computeKey}`)).toHaveCount(0)
    await captureCeU03Screenshot(page, '01-schema-form-skeleton-redbc-en-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog, '01b-schema-form-dialog-crop-redbc-en.png')
    await captureBrandHeader(page, '01c-brand-header-redbc-en.png')

    const editor = await ensureAdvancedJsonExpanded(dialog)
    const jsonText = await editor.inputValue()
    expect(jsonText).not.toContain('"Sample"')
    expect(jsonText).not.toContain(compact.computeKey)
    await captureCeU03Screenshot(page, '02-advanced-json-expanded-redbc-en-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog, '02b-advanced-json-dialog-crop-redbc-en.png')

    await dialog.getByTestId('test-data-set-name').fill(`UIUX CE-U03 errors ${Date.now()}`)
    await dialog.getByTestId('schema-input-customerName').fill('')
    await dialog.getByTestId('test-data-set-save').click()
    await expect(dialog.getByTestId('field-error-summary')).toBeVisible()
    await expect(dialog.getByTestId('field-error-summary')).toContainText('customerName')
    await captureCeU03Screenshot(page, '03-field-errors-summary-redbc-en-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog, '03b-field-errors-dialog-crop-redbc-en.png')

    await dialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).toBeHidden({ timeout: 10_000 })
    const dialog2 = await openCreateDialog(page)
    await expect(dialog2.getByTestId('schema-field-customerName')).toBeVisible()
    await expect(dialog2.getByTestId(`schema-input-${compact.computeKey}`)).toHaveCount(0)
    const advancedOpen = dialog2.getByTestId('advanced-json-editor')
    if (await advancedOpen.isVisible().catch(() => false)) {
      await dialog2.getByTestId('advanced-json-collapse').click()
    }
    await captureCeU03Screenshot(page, '04-compute-skip-form-density-redbc-en-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog2, '04b-compute-skip-dialog-crop-redbc-en.png')
  })

  test('05–07 GREENBC: form, fieldErrors, brand header', async ({ page, request }) => {
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    await expect(dialog.getByTestId('generate-schema-skeleton')).toBeVisible()
    await expect(dialog.getByTestId('test-data-set-save')).toBeVisible()
    await captureCeU03Screenshot(page, '05-schema-form-skeleton-greenbc-en-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog, '05b-schema-form-dialog-crop-greenbc-en.png')
    await captureBrandHeader(page, '05c-brand-header-greenbc-en.png')

    await dialog.getByTestId('test-data-set-name').fill(`UIUX CE-U03 green ${Date.now()}`)
    await dialog.getByTestId('schema-input-customerName').fill('')
    await dialog.getByTestId('test-data-set-save').click()
    await expect(dialog.getByTestId('field-error-summary')).toBeVisible()
    await captureCeU03Screenshot(page, '06-field-errors-summary-greenbc-en-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog, '06b-field-errors-dialog-crop-greenbc-en.png')

    await dialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).toBeHidden({ timeout: 10_000 })
    const dialog2 = await openCreateDialog(page)
    await ensureAdvancedJsonExpanded(dialog2)
    await captureCeU03Screenshot(page, '07-advanced-json-expanded-greenbc-en-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog2, '07b-advanced-json-dialog-crop-greenbc-en.png')
  })

  test('08 large-payload: Advanced JSON auto-expanded (≥12 vars)', async ({ page, request }) => {
    await openFolDevEditorTestingTab(page, large.templateId, request)
    const dialog = await openCreateDialog(page)

    await expect(dialog.getByTestId('advanced-json-editor')).toBeVisible({ timeout: 10_000 })
    await expect(dialog.getByTestId('generate-schema-skeleton')).toBeVisible()
    await captureCeU03Screenshot(page, '08-large-payload-advanced-expanded-redbc-en-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog, '08b-large-payload-dialog-crop-redbc-en.png')

    // Collapse then re-expand — click collapse header (Element Plus toggle target)
    const collapseHeader = dialog.locator(
      '[data-testid="advanced-json-collapse"] .el-collapse-item__header',
    )
    await collapseHeader.click()
    await expect(dialog.getByTestId('advanced-json-editor')).toBeHidden({ timeout: 10_000 })
    await captureCeU03LocatorScreenshot(dialog, '08c-large-payload-collapsed-crop-redbc-en.png')
    await collapseHeader.click()
    await expect(dialog.getByTestId('advanced-json-editor')).toBeVisible({ timeout: 10_000 })
    await captureCeU03LocatorScreenshot(dialog, '08d-large-payload-reexpanded-crop-redbc-en.png')
  })

  test('09 zh-CN: create dialog labels + fieldErrors layout', async ({ page, request }) => {
    await dismissOnboardingTourIfPresent(page)
    await switchLocale(page, 'zh-CN')
    await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN')
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page, /新建数据集/)

    await expect(dialog.getByText('新建测试数据集')).toBeVisible()
    await expect(dialog.getByText('从 schema 生成骨架')).toBeVisible()
    await expect(dialog.getByText('高级 / 原始 JSON')).toBeVisible()
    await captureCeU03Screenshot(page, '09-schema-form-zhcn-redbc-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog, '09b-schema-form-dialog-crop-zhcn-redbc.png')

    await dialog.getByTestId('test-data-set-name').fill(`UIUX CE-U03 zh ${Date.now()}`)
    await dialog.getByTestId('schema-input-customerName').fill('')
    await dialog.getByTestId('test-data-set-save').click()
    await expect(dialog.getByTestId('field-error-summary')).toBeVisible()
    await expect(dialog.getByTestId('field-error-summary')).toContainText('此字段为必填项')
    await captureCeU03Screenshot(page, '10-field-errors-zhcn-redbc-1920x1080.png')
    await captureCeU03LocatorScreenshot(dialog, '10b-field-errors-dialog-crop-zhcn-redbc.png')
  })
})
