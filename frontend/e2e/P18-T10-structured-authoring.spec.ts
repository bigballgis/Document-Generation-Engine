import { expect, test, type APIRequestContext, type Locator, type Page } from '@playwright/test'

import { isDockerStackReady } from './helpers/stack-readiness'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  prepareDraftTemplateWithCleanBinding,
  prepareDraftTemplateWithImageScalingBinding,
  type StructuredAuthoringFixture,
} from './helpers/structured-authoring-api'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function assertDockerStackReady(request: APIRequestContext): Promise<boolean> {
  return isDockerStackReady(request, { frontendBaseUrl: FRONTEND_BASE_URL })
}

async function openAuthoringTab(page: Page) {
  await page.getByRole('tab', { name: /^authoring$/i }).click()
  await expect(page.getByRole('heading', { name: /template authoring/i })).toBeVisible()
}

async function openAddBindingDialog(page: Page): Promise<Locator> {
  await openAuthoringTab(page)
  await page.getByRole('button', { name: /add binding/i }).click()
  const dialog = page.locator('.el-dialog').filter({ hasText: /add binding/i })
  await expect(dialog).toBeVisible()
  await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
  return dialog
}

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

async function runTestGenerate(page: Page) {
  await page.getByRole('tab', { name: /overview/i }).click()
  await page.getByRole('button', { name: /test generate/i }).click()
  await expect(page.locator('.el-message').getByText(/test generation started/i)).toBeVisible()
}

async function openPreviewSection(page: Page) {
  await openAuthoringTab(page)
  await expect(page.getByRole('heading', { name: /preview & comparison/i })).toBeVisible()
}

test.describe('P18-T10 controlled structured authoring', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let cleanFixture: StructuredAuthoringFixture
  let warningFixture: StructuredAuthoringFixture

  test.beforeAll(async ({ request }) => {
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )

    cleanFixture = await prepareDraftTemplateWithCleanBinding(request)
    warningFixture = await prepareDraftTemplateWithImageScalingBinding(request)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  // BDD: Template author opens binding dialog → controlled editor toolbar visible;
  // disabled items exist with reasons.
  test('binding dialog shows controlled toolbar and disabled capability reasons', async ({ page }) => {
    await page.goto(`/templates/${cleanFixture.templateId}`)
    await expect(page.getByRole('heading', { level: 1, name: cleanFixture.name })).toBeVisible()

    const dialog = await openAddBindingDialog(page)

    await expect(page.getByRole('toolbar', { name: /structured content toolbar/i })).toBeVisible()
    await expect(dialog.getByTestId('insert-block-node').first()).toBeVisible()
    await expect(dialog.getByTestId('insert-variable')).toBeVisible()
    await expect(dialog.getByTestId('style-picker')).toBeVisible()

    const disabledItems = dialog.getByTestId('disabled-toolbar-item')
    await expect(disabledItems).toHaveCount(3)
    for (const item of await disabledItems.all()) {
      await expect(item).toBeDisabled()
    }

    await disabledItems.first().hover()
    await expect(
      page.locator('.el-popper').getByText(/arbitrary html and css are not supported/i),
    ).toBeVisible()
  })

  // BDD: Insert confirmed node (paragraph/variable) via toolbar → save binding succeeds.
  test('insert paragraph and variable via toolbar then save binding', async ({ page }) => {
    const anchorId = `E2E_BIND_${Date.now().toString(36).toUpperCase()}`

    await page.goto(`/templates/${cleanFixture.templateId}`)
    const dialog = await openAddBindingDialog(page)

    await dialog.getByTestId('insert-block-node').filter({ hasText: /^paragraph$/i }).click()
    await dialog.getByTestId('insert-variable').click()

    const jsonPreview = dialog.locator('.json-preview pre')
    await expect(jsonPreview).toContainText('"type":"paragraph"')
    await expect(jsonPreview).toContainText('"type":"variable"')

    await dialog.locator('.el-form-item').filter({ hasText: /^anchor id$/i }).locator('input').fill(anchorId)
    await dialog.getByRole('button', { name: /^save$/i }).click()

    await expect(page.locator('.el-message').getByText(/binding saved/i)).toBeVisible()
    await expect(page.getByRole('row', { name: new RegExp(anchorId) })).toBeVisible()
  })

  // BDD: Paste HTML triggers paste-clean summary modal → cancel restores pre-paste;
  // apply accepts cleaned JSON.
  test('paste HTML summary modal cancel restores and apply accepts cleaned JSON', async ({ page }) => {
    await page.goto(`/templates/${cleanFixture.templateId}`)
    const dialog = await openAddBindingDialog(page)

    const jsonPreview = dialog.locator('.json-preview pre')
    const initialJson = await jsonPreview.textContent()
    expect(initialJson).toBeTruthy()

    await pasteHtmlIntoEditor(page, '<p>Pasted paragraph from E2E</p>')

    const pasteDialog = page.getByTestId('paste-cleaning-summary-dialog')
    await expect(pasteDialog).toBeVisible({ timeout: 15_000 })
    await expect(pasteDialog.getByText(/paste cleaning summary/i)).toBeVisible()
    await expect(pasteDialog.getByText(/transformed/i).first()).toBeVisible()

    await pasteDialog.getByTestId('paste-summary-cancel').click()
    await expect(pasteDialog).not.toBeVisible()
    await expect(jsonPreview).toHaveText(initialJson ?? '')

    await pasteHtmlIntoEditor(page, '<p>Pasted paragraph from E2E</p>')
    await expect(pasteDialog).toBeVisible({ timeout: 15_000 })

    await pasteDialog.getByTestId('paste-summary-accept').click()
    await expect(pasteDialog).not.toBeVisible()
    await expect(jsonPreview).not.toHaveText(initialJson ?? '')
    await expect(jsonPreview).toContainText('"type":"paragraph"')
  })

  // BDD: Test generate → preview tab shows fidelity warnings; filter by warning code works.
  test('test generate shows fidelity warnings and warning code filter works', async ({ page }) => {
    await page.goto(`/templates/${warningFixture.templateId}`)
    await expect(page.getByRole('heading', { level: 1, name: warningFixture.name })).toBeVisible()

    await runTestGenerate(page)
    await openPreviewSection(page)

    const warningList = page.getByTestId('fidelity-warning-list')
    await expect(warningList).toBeVisible()
    await expect(warningList).toContainText('IMAGE_SCALING_ADJUSTED')
    await expect(page.getByRole('heading', { name: /fidelity warnings/i })).toBeVisible()

    await page.getByTestId('filter-warning-code').fill('IMAGE_SCALING')
    await expect(warningList.locator('.el-table__row')).toHaveCount(1)
    await expect(warningList).toContainText('IMAGE_SCALING_ADJUSTED')

    await page.getByTestId('filter-warning-code').fill('UNRESOLVED_VARIABLE')
    await expect(page.getByText(/no warnings match the current filters/i)).toBeVisible()
  })

  // BDD: Clean binding → no fidelity warnings (no stub).
  test('clean binding test generate shows no fidelity warnings', async ({ page }) => {
    await page.goto(`/templates/${cleanFixture.templateId}`)
    await expect(page.getByRole('heading', { level: 1, name: cleanFixture.name })).toBeVisible()

    await runTestGenerate(page)
    await openPreviewSection(page)

    await expect(page.getByTestId('fidelity-warning-list')).toHaveCount(0)
    await expect(page.getByText(/no fidelity warnings/i)).toBeVisible()
    await expect(page.getByText(/CONTROLLED_STYLE_FALLBACK/i)).toHaveCount(0)
  })
})
