import { expect, test, type Locator, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  prepareDraftTemplateWithCleanBinding,
  prepareDraftTemplateWithImageScalingBinding,
  type StructuredAuthoringFixture,
} from './helpers/structured-authoring-api'
import {
  captureP18T10LocatorScreenshot,
  captureP18T10Screenshot,
  ensureP18T10EvidenceDirs,
  P18_T10_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function pasteHtmlIntoEditor(page: Page, html: string) {
  await page.evaluate((sourceHtml) => {
    const el = document.querySelector('[data-testid="editor-paste-area"]')
    if (!el) {
      throw new Error('Structured editor paste area was not found')
    }
    const dataTransfer = new DataTransfer()
    dataTransfer.setData('text/html', sourceHtml)
    dataTransfer.setData('text/plain', sourceHtml.replace(/<[^>]+>/g, ''))
    el.dispatchEvent(
      new ClipboardEvent('paste', {
        bubbles: true,
        cancelable: true,
        clipboardData: dataTransfer,
      } as ClipboardEventInit),
    )
  }, html)
}

async function openAddBindingDialog(page: Page): Promise<Locator> {
  await page.getByRole('tab', { name: /^authoring$/i }).click()
  await page.getByRole('button', { name: /add binding/i }).click()
  const dialog = page.locator('.el-dialog').filter({ hasText: /add binding/i })
  await expect(dialog).toBeVisible()
  await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
  return dialog
}

test.describe('P18-T10 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let cleanFixture: StructuredAuthoringFixture
  let warningFixture: StructuredAuthoringFixture

  test.beforeAll(async ({ request }) => {
    ensureP18T10EvidenceDirs()

    let backendReady = false
    let frontendReady = false
    try {
      const backend = await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })
      backendReady = backend.ok()
    } catch {
      backendReady = false
    }
    try {
      const frontend = await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })
      frontendReady = frontend.ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )

    cleanFixture = await prepareDraftTemplateWithCleanBinding(request)
    warningFixture = await prepareDraftTemplateWithImageScalingBinding(request)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P18_T10_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('capture controlled editor, paste summary, and fidelity warning surfaces', async ({ page }) => {
    await page.goto(`/templates/${cleanFixture.templateId}`)
    await expect(page.getByRole('heading', { level: 1, name: cleanFixture.name })).toBeVisible()

    const dialog = await openAddBindingDialog(page)
    const editor = dialog.getByTestId('controlled-structured-content-editor')
    await expect(editor).toBeVisible()
    await captureP18T10LocatorScreenshot(
      dialog,
      '01-binding-dialog-editor-toolbar-redbc-1440x900.png',
    )

    const disabledItem = dialog.getByTestId('disabled-toolbar-item').first()
    await disabledItem.hover()
    await expect(
      page.locator('.el-popper').getByText(/arbitrary html and css are not supported/i),
    ).toBeVisible()
    await captureP18T10Screenshot(page, '02-binding-dialog-disabled-tooltip-redbc-1440x900.png')

    await dialog.getByTestId('style-picker').click()
    await expect(page.locator('.el-select-dropdown').last()).toBeAttached()
    await captureP18T10Screenshot(page, '03-binding-dialog-style-picker-open-redbc-1440x900.png')
    await page.keyboard.press('Escape')

    await pasteHtmlIntoEditor(page, '<p>Paste UIUX evidence paragraph</p>')
    const pasteDialog = page.getByTestId('paste-cleaning-summary-dialog')
    await expect(pasteDialog).toBeVisible({ timeout: 15_000 })
    await captureP18T10LocatorScreenshot(
      pasteDialog,
      '04-paste-summary-transformed-redbc-1440x900.png',
    )
    await pasteDialog.getByTestId('paste-summary-cancel').click()

    await pasteHtmlIntoEditor(page, '<script>alert(1)</script><p>blocked</p>')
    await expect(pasteDialog).toBeVisible({ timeout: 15_000 })
    await expect(pasteDialog.getByText(/blocked/i).first()).toBeVisible()
    await captureP18T10LocatorScreenshot(
      pasteDialog,
      '05-paste-summary-blocked-redbc-1440x900.png',
    )
    await pasteDialog.getByTestId('paste-summary-cancel').click()
    await expect(pasteDialog).not.toBeVisible()
    await dialog.locator('.el-dialog__footer').getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).not.toBeVisible()

    await switchBrand(page, 'GREENBC')
    await openAddBindingDialog(page)
    await captureP18T10LocatorScreenshot(
      page.locator('.el-dialog').filter({ hasText: /add binding/i }),
      '06-binding-dialog-editor-greenbc-1440x900.png',
    )

    await pasteHtmlIntoEditor(page, '<p>GreenBC paste summary</p>')
    await expect(pasteDialog).toBeVisible({ timeout: 15_000 })
    await captureP18T10LocatorScreenshot(
      pasteDialog,
      '07-paste-summary-greenbc-1440x900.png',
    )
    await pasteDialog.getByTestId('paste-summary-cancel').click()
    await expect(pasteDialog).not.toBeVisible()
    await page
      .locator('.el-dialog')
      .filter({ hasText: /add binding/i })
      .locator('.el-dialog__footer')
      .getByRole('button', { name: /^cancel$/i })
      .click()
    await expect(page.locator('.el-dialog').filter({ hasText: /add binding/i })).not.toBeVisible()

    await captureP18T10LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '12-brand-header-greenbc-1440x900.png',
    )
    await switchBrand(page, 'REDBC')
    await captureP18T10LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '11-brand-header-redbc-1440x900.png',
    )

    await page.goto(`/templates/${warningFixture.templateId}`)
    await page.getByRole('tab', { name: /overview/i }).click()
    await page.getByRole('button', { name: /test generate/i }).click()
    await expect(page.locator('.el-message').getByText(/test generation started/i)).toBeVisible()

    await page.getByRole('tab', { name: /^authoring$/i }).click()
    await expect(page.getByRole('heading', { name: /preview & comparison/i })).toBeVisible()

    const warningList = page.getByTestId('fidelity-warning-list')
    await expect(warningList).toBeVisible()
    await expect(warningList).toContainText('IMAGE_SCALING_ADJUSTED')
    await captureP18T10Screenshot(page, '08-preview-fidelity-warnings-populated-redbc-1440x900.png')

    await page.getByTestId('filter-warning-code').fill('IMAGE_SCALING')
    await expect(warningList.locator('.el-table__row')).toHaveCount(1)
    await captureP18T10Screenshot(page, '09-preview-fidelity-filters-applied-redbc-1440x900.png')

    await page.goto(`/templates/${cleanFixture.templateId}`)
    await page.getByRole('tab', { name: /overview/i }).click()
    await page.getByRole('button', { name: /test generate/i }).click()
    await expect(page.locator('.el-message').getByText(/test generation started/i)).toBeVisible()
    await page.getByRole('tab', { name: /^authoring$/i }).click()
    await expect(page.getByText(/no fidelity warnings/i)).toBeVisible()
    await captureP18T10Screenshot(page, '10-preview-fidelity-empty-redbc-1440x900.png')
  })
})
