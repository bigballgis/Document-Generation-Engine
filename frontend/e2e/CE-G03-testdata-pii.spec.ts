/**
 * CE-G03 — Test data PII governance (BDD-CE-G03-012 / 013 / 014 + fail-closed / SYNTHETIC success)
 *
 * Docker acceptance: http://127.0.0.1:4173 + backend :8080
 */
import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  createTestDataSetWithoutPiiHandling,
  prepareCeG03PiiSchemaFixture,
  type CeG03PiiFixture,
} from './helpers/ce-g03-testdata-pii-api'
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

test.describe('CE-G03 testdata PII governance (BDD-CE-G03-012…014)', () => {
  test.describe.configure({ mode: 'serial' })

  let fixture: CeG03PiiFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    fixture = await prepareCeG03PiiSchemaFixture(request)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('BDD-012: create dialog shows PII badge on marked fields only', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page)

    await expect(dialog.getByTestId(`schema-field-${fixture.piiKey}`)).toBeVisible()
    await expect(dialog.getByTestId(`pii-badge-${fixture.piiKey}`)).toBeVisible()
    await expect(dialog.getByTestId(`pii-badge-${fixture.piiKey}`)).toContainText(/PII/i)
    await expect(dialog.getByTestId(`pii-badge-${fixture.nonPiiKey}`)).toHaveCount(0)

    // Default skeleton fills PII value → handling group visible with SYNTHETIC recommended
    await expect(dialog.getByTestId('pii-handling-group')).toBeVisible()
    await expect(dialog.getByTestId('pii-handling-synthetic')).toBeVisible()
    await expect(dialog.getByTestId('pii-handling-explicit')).toBeVisible()
  })

  test('BDD-013: save without piiHandling fail-closes (422 mapped; no silent success)', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page)

    const setName = `E2E CE-G03 no-handling ${Date.now()}`
    await dialog.getByTestId('test-data-set-name').fill(setName)
    await dialog.getByTestId(`schema-input-${fixture.piiKey}`).fill('Jane Doe')
    await expect(dialog.getByTestId('pii-handling-group')).toBeVisible()

    // UI defaults to SYNTHETIC (client gate). Strip piiHandling on the wire so the
    // authoritative backend gate enforces BDD-013 / G03-C9 (发出后映射 422).
    await page.route('**/api/management/v1/templates/*/test-data-sets', async (route) => {
      if (route.request().method() !== 'POST') {
        await route.continue()
        return
      }
      const original = route.request().postDataJSON() as Record<string, unknown>
      const rest = { ...original }
      delete rest.piiHandling
      delete rest.piiConfirmReason
      delete rest.secondaryConfirmed
      await route.continue({
        postData: JSON.stringify(rest),
        headers: {
          ...route.request().headers(),
          'content-type': 'application/json',
        },
      })
    })

    const createResponse = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-data-sets') &&
        !response.url().includes('/previews'),
      { timeout: 30_000 },
    )

    await dialog.getByTestId('test-data-set-save').click()
    const response = await createResponse
    expect(response.status()).toBe(422)

    await expect(dialog).toBeVisible()
    await expect(
      page
        .locator('.el-message, [data-testid="field-error-summary"], [data-testid="pii-handling-error"]')
        .filter({
          hasText: /PII|piiHandling|synthetic|explicit|handling/i,
        })
        .first(),
    ).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('.test-data-set-panel')).not.toContainText(setName)

    await page.unroute('**/api/management/v1/templates/*/test-data-sets')
  })

  test('BDD-014: EXPLICIT_SENSITIVE requires reason + secondary confirm before request', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page)

    const setName = `E2E CE-G03 explicit ${Date.now()}`
    await dialog.getByTestId('test-data-set-name').fill(setName)
    await dialog.getByTestId(`schema-input-${fixture.piiKey}`).fill('Sensitive Sample Name')

    await dialog.getByTestId('pii-handling-explicit').click()
    await dialog.getByTestId('test-data-set-save').click()

    const confirmDialog = page.getByTestId('pii-explicit-confirm-dialog')
    await expect(confirmDialog).toBeVisible({ timeout: 10_000 })

    let createPosted = false
    page.on('request', (req) => {
      if (req.method() === 'POST' && req.url().includes('/test-data-sets')) {
        createPosted = true
      }
    })

    // Submit without reason / secondary → blocked
    await confirmDialog.getByTestId('pii-explicit-confirm-submit').click()
    await expect(confirmDialog).toBeVisible()
    expect(createPosted).toBe(false)
    await expect(confirmDialog.getByText(/reason is required|secondary confirmation/i)).toBeVisible()

    await confirmDialog.getByTestId('pii-confirm-reason').fill(
      'E2E CE-G03 explicit sensitive confirmation for QA sample',
    )
    await confirmDialog.getByTestId('pii-explicit-confirm-submit').click()
    await expect(confirmDialog).toBeVisible()
    expect(createPosted).toBe(false)
    await expect(confirmDialog.getByText(/secondary confirmation is required/i)).toBeVisible()

    await confirmDialog.getByTestId('pii-secondary-confirm').click()

    const createResponse = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-data-sets') &&
        !response.url().includes('/previews'),
      { timeout: 30_000 },
    )

    await confirmDialog.getByTestId('pii-explicit-confirm-submit').click()
    const response = await createResponse
    expect(response.status()).toBe(201)

    const postBody = response.request().postDataJSON() as {
      piiHandling?: string
      piiConfirmReason?: string
      secondaryConfirmed?: boolean
      variables?: Record<string, unknown>
    }
    expect(postBody.piiHandling).toBe('EXPLICIT_SENSITIVE')
    expect(postBody.piiConfirmReason).toContain('E2E CE-G03')
    expect(postBody.secondaryConfirmed).toBe(true)
    expect(postBody.variables?.[fixture.piiKey]).toBe('Sensitive Sample Name')

    await expect(dialog).toBeHidden({ timeout: 15_000 })
    await expect(page.locator('.test-data-set-panel')).toContainText(setName)
  })

  test('SYNTHETIC path: recommended handling saves with piiHandling=SYNTHETIC', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    const dialog = await openCreateDialog(page)

    const setName = `E2E CE-G03 synthetic ${Date.now()}`
    await dialog.getByTestId('test-data-set-name').fill(setName)
    await dialog.getByTestId(`schema-input-${fixture.piiKey}`).fill('SYNTH-CUSTOMER-001')
    await expect(dialog.getByTestId('pii-handling-group')).toBeVisible()
    // Default is SYNTHETIC; click to be explicit
    await dialog.getByTestId('pii-handling-synthetic').click()

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

    const postBody = response.request().postDataJSON() as {
      piiHandling?: string
      variables?: Record<string, unknown>
    }
    expect(postBody.piiHandling).toBe('SYNTHETIC')
    expect(postBody.variables?.[fixture.piiKey]).toBe('SYNTH-CUSTOMER-001')

    await expect(dialog).toBeHidden({ timeout: 15_000 })
    await expect(page.locator('.test-data-set-panel')).toContainText(setName)
  })

  test('API fail-closed: missing piiHandling returns 422 (BDD-008/011)', async ({ request }) => {
    const attempt = await createTestDataSetWithoutPiiHandling(request, fixture.templateId, {
      name: `E2E CE-G03 api no-handling ${Date.now()}`,
      variables: { [fixture.piiKey]: 'Real Looking Name' },
    })
    expect(attempt.status).toBe(422)
    expect(attempt.body.error?.messageKey ?? '').toMatch(/testDataSetPiiHandlingRequired/i)
  })
})
