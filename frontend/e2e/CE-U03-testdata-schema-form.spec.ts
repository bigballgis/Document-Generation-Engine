/**
 * CE-U03 — Schema-driven test data set form (BDD-CE-U03-TESTDATA-SCHEMA-001)
 *
 * Covers frontend acceptance: S1, S2, S3, S4, S5, S6, S7, S8, S9, S10 (+ happy-path save).
 * Docker acceptance: http://127.0.0.1:4173
 */
import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  createTestDataSetAsTester,
  prepareCeU03CompactSchemaFixture,
  prepareCeU03LargeSchemaFixture,
  type CeU03SchemaFixture,
} from './helpers/ce-u03-testdata-schema-api'
import { openFolDevEditorTestingTab } from './helpers/template-testing-api'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

function editDialog(page: Page) {
  return page.locator('.test-data-set-edit-dialog')
}

async function openCreateDialog(page: Page) {
  const panel = page.locator('.test-data-set-panel')
  await panel.getByRole('button', { name: /^create data set$/i }).click()
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

test.describe('CE-U03 testdata schema form (BDD-CE-U03-TESTDATA-SCHEMA-001)', () => {
  test.describe.configure({ mode: 'serial' })

  let compact: CeU03SchemaFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    compact = await prepareCeU03CompactSchemaFixture(request)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('S1/S5/S8: create dialog renders schema form, skips compute, no Sample hardcode', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    await expect(dialog.getByText(/variables/i).first()).toBeVisible()
    await expect(dialog.getByTestId('generate-schema-skeleton')).toBeVisible()
    await expect(dialog.getByTestId('advanced-json-collapse')).toBeVisible()

    for (const key of compact.enterableKeys) {
      await expect(dialog.getByTestId(`schema-field-${key}`)).toBeVisible()
      await expect(dialog.getByTestId(`schema-input-${key}`)).toBeVisible()
    }
    await expect(dialog.getByTestId(`schema-field-${compact.computeKey}`)).toHaveCount(0)
    await expect(dialog.getByTestId(`schema-input-${compact.computeKey}`)).toHaveCount(0)

    // S1: customerName marked required (Element Plus required asterisk / form-item)
    const customerField = dialog.getByTestId('schema-field-customerName')
    await expect(customerField).toHaveClass(/is-required/)

    // S5: must not hardcode {"customerName":"Sample"}
    const editor = await ensureAdvancedJsonExpanded(dialog)
    const jsonText = await editor.inputValue()
    expect(jsonText).not.toContain('"Sample"')
    expect(jsonText).not.toMatch(/customerName"\s*:\s*"Sample"/)
    // Auto-skeleton (U03-C12) applies schema default
    expect(jsonText).toContain('Acme')
    expect(jsonText).not.toContain(compact.computeKey)
  })

  test('S4: Generate from schema fills defaults and boolean placeholder', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    // Mutate so overwrite confirm appears, then regenerate
    await dialog.getByTestId('schema-input-customerName').fill('Mutated')
    await dialog.getByTestId('generate-schema-skeleton').click()

    const confirm = page.locator('.el-message-box').filter({ hasText: /overwrite variables/i })
    await expect(confirm).toBeVisible({ timeout: 10_000 })
    await confirm.locator('.el-button--primary').click()

    await expect(dialog.getByTestId('schema-input-customerName')).toHaveValue('Acme')
    const editor = await ensureAdvancedJsonExpanded(dialog)
    const jsonText = await editor.inputValue()
    expect(jsonText).toContain('"customerName": "Acme"')
    expect(jsonText).toMatch(/"flag"\s*:\s*false/)
    expect(jsonText).not.toContain(compact.computeKey)
  })

  test('S3: required empty blocks Save without API request', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    await dialog.getByTestId('test-data-set-name').fill(`E2E CE-U03 required ${Date.now()}`)
    await dialog.getByTestId('schema-input-customerName').fill('')

    let createPosted = false
    page.on('request', (req) => {
      if (req.method() === 'POST' && req.url().includes('/test-data-sets')) {
        createPosted = true
      }
    })

    await dialog.getByTestId('test-data-set-save').click()
    await expect(dialog.getByTestId('field-error-summary')).toBeVisible()
    await expect(dialog.getByTestId('field-error-summary')).toContainText('customerName')
    expect(createPosted).toBe(false)
    await expect(dialog).toBeVisible()
  })

  test('S2: type mismatch via Advanced JSON blocks Save without API request', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    await dialog.getByTestId('test-data-set-name').fill(`E2E CE-U03 type ${Date.now()}`)
    const editor = await ensureAdvancedJsonExpanded(dialog)
    await editor.fill(
      JSON.stringify(
        { customerName: 'Acme', amount: 'not-a-number', status: 'ACTIVE', flag: false },
        null,
        2,
      ),
    )

    let createPosted = false
    page.on('request', (req) => {
      if (req.method() === 'POST' && req.url().includes('/test-data-sets')) {
        createPosted = true
      }
    })

    await dialog.getByTestId('test-data-set-save').click()
    await expect(dialog.getByTestId('field-error-summary')).toBeVisible()
    await expect(dialog.getByTestId('field-error-summary')).toContainText(/amount/i)
    expect(createPosted).toBe(false)
  })

  test('S7: invalid Advanced JSON blocks Save without API request', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    await dialog.getByTestId('test-data-set-name').fill(`E2E CE-U03 json ${Date.now()}`)
    const editor = await ensureAdvancedJsonExpanded(dialog)
    await editor.fill('{not-valid-json')

    let createPosted = false
    page.on('request', (req) => {
      if (req.method() === 'POST' && req.url().includes('/test-data-sets')) {
        createPosted = true
      }
    })

    await dialog.getByTestId('test-data-set-save').click()
    await expect(dialog.getByTestId('field-error-summary')).toBeVisible()
    expect(createPosted).toBe(false)
  })

  test('S9: backend fieldErrors map into dialog summary (route stub)', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    await dialog.getByTestId('test-data-set-name').fill(`E2E CE-U03 fieldErrors ${Date.now()}`)
    await dialog.getByTestId('schema-input-customerName').fill('Acme')

    await page.route('**/api/management/v1/templates/*/test-data-sets', async (route) => {
      if (route.request().method() !== 'POST') {
        await route.continue()
        return
      }
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { requestId: 'e2e-ce-u03-s9', timestamp: new Date().toISOString() },
          result: null,
          error: {
            code: 'VALIDATION',
            category: 'VALIDATION',
            retryable: false,
            messageKey: 'api.error.template.testDataSetSchemaInvalid',
            message: 'Schema invalid',
            fieldErrors: [
              { field: 'customerName', reason: 'REQUIRED', message: 'required' },
            ],
          },
        }),
      })
    })

    await dialog.getByTestId('test-data-set-save').click()
    await expect(dialog.getByTestId('field-error-summary')).toBeVisible({ timeout: 15_000 })
    await expect(dialog.getByTestId('field-error-summary')).toContainText('customerName')
    await expect(dialog).toBeVisible()
    await page.unroute('**/api/management/v1/templates/*/test-data-sets')
  })

  test('Save-flow: skeleton → edit field → save creates row', async ({ page, request }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, compact.templateId, request)
    const dialog = await openCreateDialog(page)

    const setName = `E2E CE-U03 save ${Date.now()}`
    await dialog.getByTestId('test-data-set-name').fill(setName)

    // Generate skeleton (may confirm overwrite if auto-skeleton already filled)
    await dialog.getByTestId('generate-schema-skeleton').click()
    const confirm = page.locator('.el-message-box')
    if (await confirm.isVisible().catch(() => false)) {
      await confirm.getByRole('button', { name: /^confirm$/i }).click()
    }
    await expect(dialog.getByTestId('schema-input-customerName')).toHaveValue('Acme')

    // Edit one field, then save
    await dialog.getByTestId('schema-input-customerName').fill('Acme Corp')

    const createResponse = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-data-sets') &&
        !response.url().includes('/previews'),
      { timeout: 30_000 },
    )

    await dialog.getByTestId('test-data-set-save').click()
    const response = await createResponse
    expect(response.status()).toBe(201)
    await expect(dialog).toBeHidden({ timeout: 15_000 })
    await expect(page.locator('.test-data-set-panel')).toContainText(setName)
    await expect(page.locator('.el-message').getByText(/test data set created/i)).toBeVisible({
      timeout: 10_000,
    })
  })

  test('S6: ≥12 variables expands Advanced JSON; JSON edits sync to form', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000)
    const large = await prepareCeU03LargeSchemaFixture(request)
    await openFolDevEditorTestingTab(page, large.templateId, request)
    const dialog = await openCreateDialog(page)

    // Default expanded when enterable ≥ 12
    const editor = dialog.getByTestId('advanced-json-editor')
    await expect(editor).toBeVisible({ timeout: 10_000 })

    const payload: Record<string, string> = {}
    for (const key of large.largeKeys) {
      payload[key] = key === 'field01' ? 'SyncedFromJson' : `v-${key}`
    }
    await editor.fill(JSON.stringify(payload, null, 2))
    await expect(dialog.getByTestId('schema-input-field01')).toHaveValue('SyncedFromJson')
  })

  test('S10: TEMPLATE_TESTER cannot create test data sets (API fail-closed)', async ({
    request,
  }) => {
    const attempt = await createTestDataSetAsTester(request, compact.templateId, {
      name: `E2E CE-U03 tester deny ${Date.now()}`,
      variables: { customerName: 'Acme' },
    })
    expect([403, 401]).toContain(attempt.status)
    expect(attempt.body.error?.code ?? '').toMatch(/ACCESS_DENIED|UNAUTHORIZED|FORBIDDEN/i)
  })
})
