import { expect, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR } from './auth'
import { E2E_API_BASE_URL } from './masters-api'
import {
  prepareDraftTemplateWithImageScalingBinding,
  type StructuredAuthoringFixture,
} from './structured-authoring-api'
import { createTestDataSet, startAsyncPreviewWithStatus } from './template-testing-api'
import { listTemplateVersionLines } from './template-version-lines-api'

interface ApiEnvelope<T> {
  result: T
}

export interface PreviewComparisonItemDto {
  locationType: string
  locationRef: string
  severity: string
  diffCode: string
  summary: string
}

export interface PreviewRecordDto {
  previewId: string
  status: string
  fidelityWarnings: Array<{
    code: string
    messageKey: string
    location?: string | null
    artifact?: string | null
    viewed?: boolean
  }>
  previewComparison: {
    totalDiffCount: number
    blockerCount: number
    warningCount: number
    items: PreviewComparisonItemDto[]
  } | null
}

export interface PreviewComparisonFixture extends StructuredAuthoringFixture {
  testDataSetId: string
  previewId: string
  warningCode: string
  preview: PreviewRecordDto
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

export async function getPreviewViaApi(
  request: APIRequestContext,
  templateId: string,
  previewId: string,
): Promise<PreviewRecordDto> {
  const token = await apiLogin(request)
  return authorizedGet<PreviewRecordDto>(
    request,
    token,
    `/templates/${templateId}/previews/${previewId}`,
  )
}

async function tryGetPreviewViaApi(
  request: APIRequestContext,
  templateId: string,
  previewId: string,
): Promise<PreviewRecordDto | null> {
  const token = await apiLogin(request)
  const response = await request.get(
    `${E2E_API_BASE_URL}/templates/${templateId}/previews/${previewId}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  if (response.status() === 404) {
    return null
  }
  if (!response.ok()) {
    throw new Error(
      `GET /templates/${templateId}/previews/${previewId} failed (${response.status()}): ${await response.text()}`,
    )
  }
  const body = (await response.json()) as ApiEnvelope<PreviewRecordDto>
  return body.result
}

export async function listPreviewRunsViaApi(
  request: APIRequestContext,
  templateId: string,
): Promise<Array<{ previewId: string; status: string; fidelityWarningCount: number }>> {
  const token = await apiLogin(request)
  return authorizedGet(request, token, `/templates/${templateId}/previews`)
}

/**
 * Start final-path async preview and poll until SUCCEEDED/FAILED.
 * Handles 429 concurrency by retrying the start request.
 * Treats early 404 as "record not persisted yet" (async executor race).
 */
export async function runAsyncPreviewUntilTerminal(
  request: APIRequestContext,
  templateId: string,
  testDataSetId: string,
  timeoutMs = 300_000,
): Promise<PreviewRecordDto> {
  const deadline = Date.now() + timeoutMs
  let previewId: string | undefined

  while (Date.now() < deadline) {
    const attempt = await startAsyncPreviewWithStatus(request, templateId, testDataSetId)
    if (attempt.status === 429) {
      await new Promise((resolve) => setTimeout(resolve, 2_000))
      continue
    }
    if (attempt.status === 202 || attempt.status === 200) {
      const started = attempt.body as ApiEnvelope<{ previewId: string }>
      previewId = started.result.previewId
      break
    }
    throw new Error(
      `Unexpected preview start status ${attempt.status}: ${JSON.stringify(attempt.body)}`,
    )
  }

  if (!previewId) {
    throw new Error('Timed out waiting for preview concurrency slot')
  }

  while (Date.now() < deadline) {
    const preview = await tryGetPreviewViaApi(request, templateId, previewId)
    if (preview && (preview.status === 'SUCCEEDED' || preview.status === 'FAILED')) {
      return preview
    }
    // Fallback: list runs in case start ID and persisted ID diverge on older stacks.
    if (!preview) {
      const runs = await listPreviewRunsViaApi(request, templateId)
      const match = runs.find(
        (run) =>
          run.previewId === previewId
          || (run.status === 'SUCCEEDED' && run.fidelityWarningCount > 0),
      )
      if (match && (match.status === 'SUCCEEDED' || match.status === 'FAILED')) {
        const listed = await tryGetPreviewViaApi(request, templateId, match.previewId)
        if (listed && (listed.status === 'SUCCEEDED' || listed.status === 'FAILED')) {
          return listed
        }
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000))
  }

  throw new Error(`Timed out waiting for preview ${previewId} to reach a terminal status`)
}

/**
 * Seed DRAFT template with IMAGE_SCALING binding + test data set + SUCCEEDED final-path preview
 * that includes fidelityWarnings (and derived previewComparison items).
 */
export async function prepareSucceededPreviewWithComparison(
  request: APIRequestContext,
): Promise<PreviewComparisonFixture> {
  const fixture = await prepareDraftTemplateWithImageScalingBinding(request)
  const dataSet = await createTestDataSet(request, fixture.templateId, {
    name: `E2E-CDP-T09-COMPARE-DS-${Date.now().toString(36).toUpperCase()}`,
    required: true,
    variables: { customerName: 'E2E Compare Customer' },
  })

  const preview = await runAsyncPreviewUntilTerminal(
    request,
    fixture.templateId,
    dataSet.testDataSetId,
  )
  if (preview.status !== 'SUCCEEDED') {
    throw new Error(
      `Expected SUCCEEDED preview for comparison fixture, got ${preview.status} (${preview.previewId})`,
    )
  }
  if (!preview.fidelityWarnings?.length) {
    throw new Error(
      `Expected ≥1 fidelityWarnings on preview ${preview.previewId}, got ${JSON.stringify(preview.fidelityWarnings)}`,
    )
  }
  if (!preview.previewComparison) {
    throw new Error(`Expected previewComparison on preview ${preview.previewId}`)
  }

  const warningCode = preview.fidelityWarnings[0]!.code
  return {
    ...fixture,
    testDataSetId: dataSet.testDataSetId,
    previewId: preview.previewId,
    warningCode,
    preview,
  }
}

/** Open Template testing → Preview runs sub-tab (dev workspace). */
export async function openTestingPreviewRunsTab(
  page: Page,
  templateId: string,
  request: APIRequestContext,
): Promise<void> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (inFlight) {
    await page.goto(
      `/templates/${templateId}/dev/${inFlight.devVersionId}?workspaceTab=testing&testingTab=previewRuns`,
    )
  } else {
    await page.goto(`/templates/${templateId}?workspaceTab=testing&testingTab=previewRuns`)
  }

  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
  await expect(page.getByRole('tab', { name: /^preview runs$/i })).toBeVisible({ timeout: 30_000 })
  // Ensure Preview runs is active (URL may already select it).
  await page.getByRole('tab', { name: /^preview runs$/i }).click()
  await expect(page.locator('.preview-run-history')).toBeVisible({ timeout: 30_000 })
}

/** Select a SUCCEEDED preview run via Details and wait for TemplatePreviewPanel. */
export async function openSucceededPreviewDetails(page: Page): Promise<void> {
  const history = page.locator('.preview-run-history')
  await expect(history.getByRole('button', { name: /^details$/i }).first()).toBeVisible({
    timeout: 30_000,
  })
  await history.getByRole('button', { name: /^details$/i }).first().click()
  await expect(page.locator('.preview-panel')).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('.preview-panel')).toContainText(/SUCCEEDED/i)
}
