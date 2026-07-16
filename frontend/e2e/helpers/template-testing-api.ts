import { expect, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR } from './auth'
import { listTemplateVersionLines } from './template-version-lines-api'
import { E2E_API_BASE_URL } from './masters-api'

interface ApiEnvelope<T> {
  result: T
}

interface ApiErrorBody {
  error?: {
    code: string
    message?: string
    messageKey?: string
  }
}

export interface TestDataSetSummary {
  testDataSetId: string
  name: string
  required: boolean
  locked?: boolean
}

export interface AsyncPreviewStarted {
  previewId: string
  streamUrl: string
}

export interface BatchTestStarted {
  runId: string
  streamUrl: string
}

/** CE-U18 per-sample result from GET .../batch-tests (sampleResults). */
export interface BatchTestHistorySampleResult {
  dataSetExternalId?: string | null
  success?: boolean | null
  errorDetail?: string | null
  docxKey?: string | null
  pdfKey?: string | null
  testDataSetId?: string | null
  previewId?: string | null
  status?: string | null
}

export interface BatchTestRunSummary {
  runId: string
  createdAt: string
  createdBy: string
  status: string
  totalSamples: number
  succeededCount: number
  failedCount: number
  gatePassed: boolean | null
  invalidatedAt: string | null
  anchorCoveragePct: number | null
  variableCoveragePct: number | null
  sampleCoveragePct: number | null
  /** CE-U18 — present on history list after sampleResults exposure. */
  sampleResults?: BatchTestHistorySampleResult[] | null
}

export interface SubmitTestEligibilityResult {
  eligible: boolean
  conditions: {
    hasValidTestResult: boolean
    allSamplesSucceeded: boolean
    coverageGatePassed: boolean
  }
  blockingDetails: {
    uncoveredAnchors: string[]
    uncoveredVariables: string[]
    uncoveredAnchorsTotal: number
    uncoveredVariablesTotal: number
    failedDataSetNames: string[]
  }
}

async function apiLogin(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: E2E_TEMPLATE_AUTHOR,
  })
  if (!response.ok()) {
    throw new Error(`API login failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<{ accessToken: string }>
  return body.result.accessToken
}

async function authorizedGet<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
): Promise<T> {
  const response = await request.get(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    throw new Error(`GET ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<T>
  return body.result
}

async function authorizedPost<T>(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  data?: unknown,
  expectedStatus = 200,
): Promise<{ status: number; body: ApiEnvelope<T> & ApiErrorBody }> {
  const response = await request.post(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  })
  const body = (await response.json()) as ApiEnvelope<T> & ApiErrorBody
  if (response.status() !== expectedStatus) {
    throw new Error(
      `POST ${pathSuffix} failed (expected ${expectedStatus}, got ${response.status()}): ${JSON.stringify(body)}`,
    )
  }
  return { status: response.status(), body }
}

async function authorizedDelete(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
): Promise<void> {
  const response = await request.delete(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    if (response.status() === 409) {
      return
    }
    throw new Error(`DELETE ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
}

async function authorizedPut(
  request: APIRequestContext,
  token: string,
  pathSuffix: string,
  data: unknown,
): Promise<void> {
  const response = await request.put(`${E2E_API_BASE_URL}${pathSuffix}`, {
    headers: { Authorization: `Bearer ${token}` },
    data,
  })
  if (!response.ok()) {
    throw new Error(`PUT ${pathSuffix} failed (${response.status()}): ${await response.text()}`)
  }
}

export async function listTestDataSets(
  request: APIRequestContext,
  templateId: string,
): Promise<TestDataSetSummary[]> {
  const token = await apiLogin(request)
  return authorizedGet<TestDataSetSummary[]>(request, token, `/templates/${templateId}/test-data-sets`)
}

export async function createTestDataSet(
  request: APIRequestContext,
  templateId: string,
  payload: {
    name: string
    required?: boolean
    variables: Record<string, unknown>
    scenarioName?: string
  },
): Promise<TestDataSetSummary> {
  const token = await apiLogin(request)
  const { body } = await authorizedPost<TestDataSetSummary>(
    request,
    token,
    `/templates/${templateId}/test-data-sets`,
    {
      required: payload.required ?? true,
      ...payload,
    },
    201,
  )
  return body.result
}

export async function deleteTestDataSet(
  request: APIRequestContext,
  templateId: string,
  testDataSetId: string,
): Promise<void> {
  const token = await apiLogin(request)
  await authorizedDelete(request, token, `/templates/${templateId}/test-data-sets/${testDataSetId}`)
}

export async function startAsyncPreviewWithStatus(
  request: APIRequestContext,
  templateId: string,
  testDataSetId: string,
): Promise<{ status: number; body: (ApiEnvelope<AsyncPreviewStarted> & ApiErrorBody) | ApiErrorBody }> {
  const token = await apiLogin(request)
  const response = await request.post(`${E2E_API_BASE_URL}/templates/${templateId}/previews/async-preview`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { testDataSetId },
  })
  const body = (await response.json()) as ApiEnvelope<AsyncPreviewStarted> & ApiErrorBody
  return { status: response.status(), body }
}

export async function runBatchTestViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<BatchTestStarted> {
  const token = await apiLogin(request)
  const { body } = await authorizedPost<BatchTestStarted>(
    request,
    token,
    `/templates/${templateId}/batch-tests/run`,
    undefined,
    202,
  )
  return body.result
}

export async function getBatchTestHistoryViaApi(
  request: APIRequestContext,
  templateId: string,
  limit = 5,
): Promise<BatchTestRunSummary[]> {
  const token = await apiLogin(request)
  return authorizedGet<BatchTestRunSummary[]>(
    request,
    token,
    `/templates/${templateId}/batch-tests?limit=${limit}`,
  )
}

export async function getSubmitTestEligibilityViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<SubmitTestEligibilityResult> {
  const token = await apiLogin(request)
  return authorizedGet<SubmitTestEligibilityResult>(
    request,
    token,
    `/templates/${templateId}/batch-tests/submit-eligibility`,
  )
}

export async function touchTemplateContentForInvalidation(
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const token = await apiLogin(request)
  await authorizedPut(request, token, `/templates/${templateId}/variables/borrowerLegalName`, {
    variableKey: 'borrowerLegalName',
    variableType: 'TEXT',
    required: true,
    defaultValue: 'Meridian Borrower Ltd',
    description: `E2E invalidation touch ${Date.now()}`,
  })
}

export async function waitForPreviewConcurrencySlot(
  request: APIRequestContext,
  templateId: string,
  testDataSetId: string,
  timeoutMs = 180_000,
): Promise<void> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const attempt = await startAsyncPreviewWithStatus(request, templateId, testDataSetId)
    if (attempt.status === 202 || attempt.status === 200) {
      return
    }
    if (attempt.status === 429) {
      await new Promise((resolve) => setTimeout(resolve, 2_000))
      continue
    }
    throw new Error(`Unexpected preview start status ${attempt.status}: ${JSON.stringify(attempt.body)}`)
  }
  throw new Error('Timed out waiting for preview concurrency slot')
}

export async function openFolDevEditorTestingTab(
  page: Page,
  templateId: string,
  request?: APIRequestContext,
): Promise<void> {
  if (request) {
    const lines = await listTemplateVersionLines(request, templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    if (inFlight) {
      await page.goto(`/templates/${templateId}/dev/${inFlight.devVersionId}?workspaceTab=testing`)
      await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
      await expect(page.locator('.test-data-set-panel')).toBeVisible({ timeout: 30_000 })
      return
    }
  }

  await page.goto(`/templates/${templateId}`)
  await page
    .locator('.version-lines-card')
    .getByRole('button', { name: /view detail/i })
    .first()
    .click()
  await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })

  const workspace = page.locator('.workspace-tab-shell')
  await workspace.getByRole('tab', { name: /^template testing$/i }).click()
  await expect(page).toHaveURL(/workspaceTab=testing/)
  await expect(page.locator('.test-data-set-panel')).toBeVisible({ timeout: 30_000 })
}

export function previewProgressDialog(page: Page) {
  return page.locator('.el-dialog').filter({ hasText: /generating preview/i })
}

export function batchProgressDialog(page: Page) {
  return page.locator('.el-dialog').filter({ hasText: /full test in progress|full test complete/i })
}

export function submitForTestButton(page: Page) {
  return page
    .locator('.workspace-tab-shell__actions')
    .getByRole('button', { name: /submit for testing/i })
}

export async function hoverSubmitForTestTooltip(page: Page): Promise<void> {
  const button = submitForTestButton(page)
  await expect(button).toBeVisible({ timeout: 30_000 })

  const tooltipTrigger = page
    .locator('.workspace-tab-shell__actions .el-tooltip')
    .filter({ has: button })
    .locator('.el-tooltip__trigger')
    .first()

  if ((await tooltipTrigger.count()) > 0) {
    await tooltipTrigger.hover()
    return
  }

  await button.locator('xpath=ancestor::span[1]').hover()
}

export async function runFullTestFromUi(
  page: Page,
  request?: APIRequestContext,
  templateId?: string,
): Promise<void> {
  const fullTestButton = page
    .locator('.workspace-tab-shell__actions')
    .getByRole('button', { name: /^full test$/i })
  await expect(fullTestButton).toBeEnabled()

  const batchStart = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes('/batch-tests/run'),
    { timeout: 30_000 },
  )

  await fullTestButton.click()

  const confirmBox = page.locator('.el-message-box')
  await expect(confirmBox).toBeVisible()
  await confirmBox.getByRole('button', { name: /^confirm$/i }).click()

  const startResponse = await batchStart
  expect(startResponse.status()).toBe(202)
  const runId = ((await startResponse.json()) as { result: { runId: string } }).result.runId

  const dialog = batchProgressDialog(page)
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  await expect(dialog.getByText(/\d+ \/ \d+ complete/i)).toBeVisible({ timeout: 30_000 })

  if (request && templateId) {
    const deadline = Date.now() + 300_000
    while (Date.now() < deadline) {
      const history = await getBatchTestHistoryViaApi(request, templateId, 1)
      const latest = history[0]
      if (latest?.runId === runId && latest.status === 'COMPLETED') {
        break
      }
      await page.waitForTimeout(2_000)
    }
  }

  const uiSummaryVisible = await dialog
    .getByText(/full test complete|readiness checks passed|readiness checks not met/i)
    .first()
    .isVisible({ timeout: 30_000 })
    .then(() => true)
    .catch(() => false)

  if (!uiSummaryVisible) {
    if (!request || !templateId) {
      throw new Error('Batch test UI summary not visible and no API fallback was provided')
    }
    const history = await getBatchTestHistoryViaApi(request, templateId, 1)
    expect(history[0]?.runId).toBe(runId)
    expect(history[0]?.status).toBe('COMPLETED')
  }
}

export async function runPreviewFromFirstDataSetRow(page: Page): Promise<void> {
  const panel = page.locator('.test-data-set-panel')
  await panel.getByRole('button', { name: /^run preview$/i }).first().click()
  const dialog = previewProgressDialog(page)
  await expect(dialog).toBeVisible({ timeout: 15_000 })
}

export async function waitForPreviewDialogSuccess(page: Page): Promise<void> {
  const dialog = previewProgressDialog(page)
  // PreviewProgressDialog renders downloads as <el-button tag="a"> → accessible role "link".
  const terminal = await Promise.race([
    dialog
      .getByRole('link', { name: /^download docx$/i })
      .waitFor({ state: 'visible', timeout: 300_000 })
      .then(() => 'success' as const),
    dialog
      .locator('.preview-progress__error')
      .waitFor({ state: 'visible', timeout: 300_000 })
      .then(() => 'error' as const),
  ])
  if (terminal === 'error') {
    const message = (await dialog.locator('.preview-progress__error').innerText()).trim()
    throw new Error(`Preview reached error terminal state (expected success): ${message}`)
  }
  await expect(dialog.getByRole('link', { name: /^download pdf$/i })).toBeVisible()
  await expect(dialog.getByText(/expires in/i)).toBeVisible()
}
