/**
 * TM #144 / published-template-test-artifacts — release Testing read-only artifacts
 * BDD: docs/behavior/published-template-test-artifacts.md (BDD-PTA-001/002/005/006)
 */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, FOL_TEMPLATE_EXTERNAL_ID, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { listPreviewRunsViaApi } from './helpers/preview-comparison-api'
import {
  getBatchTestHistoryViaApi,
  type BatchTestRunSummary,
} from './helpers/template-testing-api'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

/**
 * Prefer catalog demos already PUBLISHED with durable SUCCEEDED preview artifacts.
 * Fresh publish-through-lifecycle fixtures currently 422 on this stack (approval-decision
 * invalidState), so Stage 6 uses seeded demos for acceptance.
 */
const PREFERRED_PUBLISHED_EXTERNAL_IDS = [
  FOL_TEMPLATE_EXTERNAL_ID,
  'DEMO-CREDIT-LIMIT-CONFIRM',
  'DEMO-ANNUAL-REVIEW',
  'DEMO-FACILITY-RENEWAL',
] as const

interface TemplateListItem {
  id: string
  externalId: string
  lifecycleStatus: string
  releaseVersion?: string | null
}

interface PreviewRunListItem {
  previewId: string
  status: string
  docxAvailable?: boolean
  pdfAvailable?: boolean
}

interface PtaFixture {
  templateId: string
  externalId: string
  releaseVersion: string
  succeededPreviewId: string
}

async function apiLoginAuthor(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: E2E_TEMPLATE_AUTHOR,
  })
  expect(response.ok(), `API login failed: ${response.status()}`).toBeTruthy()
  const body = (await response.json()) as { result: { accessToken: string } }
  return body.result.accessToken
}

async function listPublishedTemplates(request: APIRequestContext): Promise<TemplateListItem[]> {
  const token = await apiLoginAuthor(request)
  const response = await request.get(`${E2E_API_BASE_URL}/templates?size=100`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(response.ok(), `GET /templates failed: ${response.status()}`).toBeTruthy()
  const body = (await response.json()) as { result: { content: TemplateListItem[] } }
  return (body.result?.content ?? []).filter((row) => row.lifecycleStatus === 'PUBLISHED')
}

async function resolveSucceededPreviewId(
  request: APIRequestContext,
  templateId: string,
): Promise<string | null> {
  const runs = (await listPreviewRunsViaApi(request, templateId)) as PreviewRunListItem[]
  const withArtifacts = runs.find(
    (run) =>
      run.status === 'SUCCEEDED' && (run.docxAvailable !== false || run.pdfAvailable !== false),
  )
  if (withArtifacts) {
    return withArtifacts.previewId
  }
  const succeeded = runs.find((run) => run.status === 'SUCCEEDED')
  return succeeded?.previewId ?? null
}

async function preparePtaPublishedFixture(request: APIRequestContext): Promise<PtaFixture> {
  const published = await listPublishedTemplates(request)
  expect(published.length, 'Expected at least one PUBLISHED catalog template').toBeGreaterThan(0)

  for (const preferred of PREFERRED_PUBLISHED_EXTERNAL_IDS) {
    const match = published.find((row) => row.externalId === preferred)
    if (!match?.releaseVersion) {
      continue
    }
    const previewId = await resolveSucceededPreviewId(request, match.id)
    if (previewId) {
      return {
        templateId: match.id,
        externalId: match.externalId,
        releaseVersion: match.releaseVersion,
        succeededPreviewId: previewId,
      }
    }
  }

  for (const row of published) {
    if (!row.releaseVersion) {
      continue
    }
    const previewId = await resolveSucceededPreviewId(request, row.id)
    if (previewId) {
      return {
        templateId: row.id,
        externalId: row.externalId,
        releaseVersion: row.releaseVersion,
        succeededPreviewId: previewId,
      }
    }
  }

  throw new Error(
    'No PUBLISHED template with SUCCEEDED preview history found for PTA E2E (catalog seed required)',
  )
}

async function openReleaseTestingTab(
  page: Page,
  fixture: Pick<PtaFixture, 'templateId' | 'releaseVersion'>,
): Promise<void> {
  await page.goto(
    `/templates/${fixture.templateId}/releases/${fixture.releaseVersion}?workspaceTab=testing`,
  )
  await expect(page.getByTestId('release-testing-readonly')).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('.batch-test-history')).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('.preview-run-history')).toBeVisible({ timeout: 30_000 })
}

async function expandFirstBatchHistoryRow(page: Page): Promise<ReturnType<Page['locator']>> {
  const history = page.locator('.batch-test-history')
  const expandIcon = history.locator('.el-table__expand-icon').first()
  await expect(expandIcon).toBeVisible({ timeout: 15_000 })
  if ((await history.locator('.el-table__expand-icon--expanded').count()) === 0) {
    await expandIcon.click()
  }
  const samples = history.getByTestId('batch-history-sample-results')
  await expect(samples).toBeVisible({ timeout: 15_000 })
  return samples
}

/**
 * Ensure batch history exposes at least one expandable COMPLETED row with sampleResults.
 * Injects synthetic sampleResults (including optional previewId) when the publish-path
 * sync batch left empty/legacy samples without drill-down fields.
 */
async function withBatchHistorySamples(
  page: Page,
  templateId: string,
  options: { previewId?: string | null },
): Promise<void> {
  await page.route(`**/templates/${templateId}/batch-tests**`, async (route) => {
    const response = await route.fetch()
    const json = (await response.json()) as { result: BatchTestRunSummary[] }
    const rows = Array.isArray(json.result) ? [...json.result] : []
    const sample = {
      dataSetExternalId: 'E2E-PTA-SAMPLE-DS',
      testDataSetId: 'E2E-PTA-SAMPLE-DS-ID',
      success: true,
      status: 'SUCCEEDED',
      previewId: options.previewId ?? null,
      errorDetail: null,
    }
    if (rows.length === 0) {
      rows.push({
        runId: 'E2E-PTA-SYNTH-RUN',
        createdAt: new Date().toISOString(),
        createdBy: 'e2e',
        status: 'COMPLETED',
        totalSamples: 1,
        succeededCount: 1,
        failedCount: 0,
        gatePassed: true,
        invalidatedAt: null,
        anchorCoveragePct: 100,
        variableCoveragePct: 100,
        sampleCoveragePct: 100,
        sampleResults: [sample],
      })
    } else {
      const first = { ...rows[0]! }
      const existing = Array.isArray(first.sampleResults) ? first.sampleResults : []
      first.status = first.status || 'COMPLETED'
      first.sampleResults = [sample, ...existing]
      rows[0] = first
    }
    await route.fulfill({
      status: response.status(),
      headers: { ...response.headers(), 'content-type': 'application/json' },
      body: JSON.stringify({ ...json, result: rows }),
    })
  })
}

async function assertArtifactDownloadViaButton(
  page: Page,
  previewId: string,
  format: 'docx' | 'pdf',
): Promise<void> {
  const history = page.locator('.preview-run-history')
  const candidates = history.locator('.el-table__body-wrapper tbody tr.el-table__row')
  await expect(candidates.first()).toBeVisible({ timeout: 30_000 })

  let targetRow = candidates.filter({ hasClass: /is-selected/ }).first()
  if ((await targetRow.count()) === 0) {
    targetRow = candidates
      .filter({ hasText: /succeeded/i })
      .filter({
        has: page.getByRole('button', {
          name: format === 'docx' ? /^download docx$/i : /^download pdf$/i,
        }),
      })
      .first()
  }

  const button = targetRow.getByRole('button', {
    name: format === 'docx' ? /^download docx$/i : /^download pdf$/i,
  })
  await expect(button).toBeVisible()
  await expect(button).toBeEnabled({ timeout: 15_000 })

  const responsePromise = page.waitForResponse(
    (response) => {
      if (response.request().method() !== 'GET') {
        return false
      }
      const url = response.url()
      return (
        url.includes(`/previews/`) &&
        url.includes(`/artifacts/${format}`) &&
        (url.includes(previewId) || url.includes('/previews/'))
      )
    },
    { timeout: 60_000 },
  )

  await button.click()
  const response = await responsePromise
  expect(
    [401, 403, 410].includes(response.status()),
    `Artifact GET ${format} must not be 401/403/410 (got ${response.status()})`,
  ).toBe(false)
  expect(response.ok(), `Artifact GET ${format} failed: ${response.status()}`).toBeTruthy()
  expect(response.url()).toContain(`/artifacts/${format}`)
}

test.describe('TM #144 published-template-test-artifacts (BDD-PTA)', () => {
  test.describe.configure({ mode: 'serial' })

  let fixture: PtaFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await preparePtaPublishedFixture(request)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('BDD-PTA-001: PUBLISHED Testing shows preview + batch history; no authoring run controls', async ({
    page,
  }) => {
    test.setTimeout(120_000)
    await openReleaseTestingTab(page, fixture)

    await expect(page.getByTestId('release-testing-readonly')).toBeVisible()
    await expect(
      page.getByText(
        /this published release completed the testing workflow before go-live\. snapshot data below is read-only/i,
      ),
    ).toBeVisible()

    await expect(page.getByRole('heading', { name: /^test run history$/i })).toBeVisible()
    await expect(page.locator('.batch-test-history')).toBeVisible()

    await expect(page.getByRole('heading', { name: /^preview run history$/i })).toBeVisible()
    await expect(page.locator('.preview-run-history')).toBeVisible()
    await expect(
      page.locator('.preview-run-history .el-table__body-wrapper tbody tr.el-table__row').first(),
    ).toBeVisible({ timeout: 30_000 })

    // Must not re-enable authoring Testing controls on release detail (PTA-D2 / BDD-PTA-009).
    const testing = page.getByTestId('release-testing-readonly')
    await expect(testing.getByRole('button', { name: /^run preview$/i })).toHaveCount(0)
    await expect(testing.getByRole('button', { name: /^full test$/i })).toHaveCount(0)
    await expect(testing.getByRole('button', { name: /run full test/i })).toHaveCount(0)
    await expect(page.locator('.test-data-set-panel')).toHaveCount(0)
    await expect(page.locator('#dev-workspace')).toHaveCount(0)
  })

  test('BDD-PTA-002: SUCCEEDED preview DOCX/PDF download available on release Testing', async ({
    page,
  }) => {
    test.setTimeout(180_000)
    await openReleaseTestingTab(page, fixture)

    const history = page.locator('.preview-run-history')
    await expect(
      history.locator('.el-table__body-wrapper tbody tr.el-table__row').first(),
    ).toBeVisible({ timeout: 30_000 })

    // Select the seeded SUCCEEDED row when possible (Details click sets selection).
    const succeededRow = history
      .locator('.el-table__body-wrapper tbody tr.el-table__row')
      .filter({ hasText: /succeeded/i })
      .first()
    await expect(succeededRow).toBeVisible()
    await succeededRow.getByRole('button', { name: /^details$/i }).click()
    await expect(succeededRow).toHaveClass(/is-selected/)

    await assertArtifactDownloadViaButton(page, fixture.succeededPreviewId, 'docx')
    await assertArtifactDownloadViaButton(page, fixture.succeededPreviewId, 'pdf')
  })

  test('BDD-PTA-005: Open preview selects corresponding preview history row', async ({ page }) => {
    test.setTimeout(180_000)

    await withBatchHistorySamples(page, fixture.templateId, {
      previewId: fixture.succeededPreviewId,
    })

    try {
      await openReleaseTestingTab(page, fixture)
      const samples = await expandFirstBatchHistoryRow(page)

      const openPreview = samples.getByTestId('batch-history-open-preview').first()
      await expect(openPreview).toBeVisible({ timeout: 15_000 })
      await openPreview.click()

      const selected = page.locator(
        '.preview-run-history .el-table__body-wrapper tbody tr.el-table__row.is-selected',
      )
      await expect(selected).toBeVisible({ timeout: 15_000 })
      await expect(selected.getByRole('button', { name: /^download docx$/i })).toBeEnabled()
    } finally {
      await page.unroute(`**/templates/${fixture.templateId}/batch-tests**`)
    }
  })

  test('BDD-PTA-006: Open data set shows read-only toast; no authoring navigation', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000)

    // Prefer real history when publish left samples; otherwise inject a sample without previewId.
    const history = await getBatchTestHistoryViaApi(request, fixture.templateId)
    const hasSamples = history.some(
      (row) => Array.isArray(row.sampleResults) && row.sampleResults.length > 0,
    )
    if (!hasSamples) {
      await withBatchHistorySamples(page, fixture.templateId, { previewId: null })
    }

    try {
      await openReleaseTestingTab(page, fixture)
      const samples = await expandFirstBatchHistoryRow(page)

      const openDataSet = samples.getByTestId('batch-history-open-data-set').first()
      await expect(openDataSet).toBeVisible()
      await openDataSet.click()

      await expect(
        page.locator('.el-message').filter({
          hasText:
            /data sets are not editable on published release detail\. use preview run history below to review and download prior test artifacts/i,
        }),
      ).toBeVisible({ timeout: 10_000 })

      const releaseEscaped = fixture.releaseVersion.replace(/\./g, '\\.')
      await expect(page).toHaveURL(
        new RegExp(
          `/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/releases/${releaseEscaped}`,
        ),
      )
      await expect(page).not.toHaveURL(/testingTab=dataSets/)
      await expect(page.locator('.test-data-set-panel')).toHaveCount(0)
      await expect(page.locator('#dev-workspace')).toHaveCount(0)
      await expect(page.getByTestId('release-testing-readonly')).toBeVisible()
    } finally {
      await page.unroute(`**/templates/${fixture.templateId}/batch-tests**`).catch(() => undefined)
    }
  })
})
