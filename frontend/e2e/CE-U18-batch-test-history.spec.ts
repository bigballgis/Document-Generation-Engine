/**
 * CE-U18 — Batch test history drill-down + async-only full-test path
 * BDD: docs/behavior/ce-u18-batch-test-history.md (BDD-CE-U18-BTH-001…006)
 */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import {
  batchProgressDialog,
  createTestDataSet,
  getBatchTestHistoryViaApi,
  openFolDevEditorTestingTab,
  runBatchTestViaApi,
  runFullTestFromUi,
  type BatchTestRunSummary,
} from './helpers/template-testing-api'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

const MISSING_DATA_SET_ID = 'E2E-CE-U18-MISSING-DATA-SET'

async function waitForCompletedHistoryWithSamples(
  request: APIRequestContext,
  templateId: string,
  minSamples = 2,
  timeoutMs = 300_000,
): Promise<BatchTestRunSummary> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const history = await getBatchTestHistoryViaApi(request, templateId)
    const match = history.find(
      (row) =>
        row.status === 'COMPLETED' &&
        !row.invalidatedAt &&
        Array.isArray(row.sampleResults) &&
        row.sampleResults.length >= minSamples,
    )
    if (match) {
      return match
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000))
  }
  throw new Error(
    `Timed out waiting for COMPLETED batch-test history with ≥${minSamples} sampleResults`,
  )
}

async function ensureCompletedHistoryWithSamples(
  request: APIRequestContext,
  templateId: string,
): Promise<BatchTestRunSummary> {
  const history = await getBatchTestHistoryViaApi(request, templateId)
  const top = history[0]
  if (
    top?.status === 'COMPLETED' &&
    !top.invalidatedAt &&
    Array.isArray(top.sampleResults) &&
    top.sampleResults.length >= 2
  ) {
    return top
  }
  await runBatchTestViaApi(request, templateId)
  return waitForCompletedHistoryWithSamples(request, templateId)
}

async function openPreviewRunsHistory(page: Page, templateId: string, request: APIRequestContext) {
  await openFolDevEditorTestingTab(page, templateId, request)
  await page.locator('.testing-sub-tabs').getByRole('tab', { name: /preview runs/i }).click()
  const history = page.locator('.batch-test-history')
  await expect(history).toBeVisible({ timeout: 30_000 })
  await expect(history.locator('.batch-test-history__table tbody tr').first()).toBeVisible({
    timeout: 30_000,
  })
  return history
}

async function expandFirstHistoryRow(history: ReturnType<Page['locator']>) {
  const expandIcon = history.locator('.el-table__expand-icon').first()
  await expect(expandIcon).toBeVisible({ timeout: 15_000 })
  const expanded = history.locator('.el-table__expand-icon--expanded')
  if ((await expanded.count()) === 0) {
    await expandIcon.click()
  }
  await expect(history.getByTestId('batch-history-sample-results')).toBeVisible({ timeout: 15_000 })
}

test.describe('CE-U18 batch test history drill-down + async-only path (BTH)', () => {
  test.describe.configure({ mode: 'serial' })

  let templateId = ''

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    const draft = await prepareDraftTemplateWithCleanBinding(request)
    templateId = draft.templateId

    await createTestDataSet(request, templateId, {
      name: `E2E CE-U18 sample A ${Date.now()}`,
      required: true,
      variables: { customerName: 'Alice CE-U18' },
      scenarioName: 'CE-U18 sample A',
    })
    await createTestDataSet(request, templateId, {
      name: `E2E CE-U18 sample B ${Date.now()}`,
      required: true,
      variables: { customerName: 'Bob CE-U18' },
      scenarioName: 'CE-U18 sample B',
    })
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('BDD-CE-U18-BTH-001/002: expand sampleResults and Open data set selects dataSets', async ({
    page,
    request,
  }) => {
    test.setTimeout(360_000)
    const completed = await ensureCompletedHistoryWithSamples(request, templateId)
    expect(completed.sampleResults!.length).toBeGreaterThanOrEqual(2)

    const history = await openPreviewRunsHistory(page, templateId, request)
    await expandFirstHistoryRow(history)

    const samples = history.getByTestId('batch-history-sample-results')
    await expect(samples.getByRole('heading', { name: /sample results/i })).toBeVisible()

    for (const sample of completed.sampleResults!.slice(0, 2)) {
      const id = sample.dataSetExternalId ?? sample.testDataSetId
      expect(id).toBeTruthy()
      await expect(samples.getByText(id!, { exact: true }).first()).toBeVisible()
    }
    await expect(samples.getByText(/succeeded|failed/i).first()).toBeVisible()

    const openBtn = samples.getByTestId('batch-history-open-data-set').first()
    await expect(openBtn).toBeVisible()
    await openBtn.click()

    await expect(page).toHaveURL(/workspaceTab=testing/)
    await expect(page).toHaveURL(/testingTab=dataSets/)
    await expect(page.locator('.testing-sub-tabs').getByRole('tab', { name: /data sets/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page.locator('.test-data-set-panel tr.is-selected-row').first()).toBeVisible({
      timeout: 15_000,
    })
  })

  test('BDD-CE-U18-BTH-003: Open data set shows non-blocking feedback when unmatched', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000)
    await ensureCompletedHistoryWithSamples(request, templateId)

    await page.route(`**/templates/${templateId}/batch-tests**`, async (route) => {
      const response = await route.fetch()
      const json = (await response.json()) as {
        result: BatchTestRunSummary[]
      }
      const rows = Array.isArray(json.result) ? json.result : []
      if (rows.length > 0) {
        const first = rows[0]!
        first.sampleResults = [
          {
            dataSetExternalId: MISSING_DATA_SET_ID,
            success: false,
            errorDetail: 'E2E synthetic unmatched sample',
          },
          ...(first.sampleResults ?? []),
        ]
      }
      await route.fulfill({
        status: response.status(),
        headers: { ...response.headers(), 'content-type': 'application/json' },
        body: JSON.stringify(json),
      })
    })

    try {
      const history = await openPreviewRunsHistory(page, templateId, request)
      await expandFirstHistoryRow(history)

      const samples = history.getByTestId('batch-history-sample-results')
      await expect(samples.getByText(MISSING_DATA_SET_ID, { exact: true })).toBeVisible()
      await expect(samples.getByText(/failed/i).first()).toBeVisible()
      await expect(samples.getByText(/E2E synthetic unmatched sample/i)).toBeVisible()

      await samples.getByTestId('batch-history-open-data-set').first().click()

      await expect(page).toHaveURL(/testingTab=dataSets/)
      await expect(
        page.locator('.el-message--warning').filter({ hasText: /could not find data set/i }),
      ).toBeVisible({
        timeout: 10_000,
      })
      await expect(page.locator('#dev-workspace')).toBeVisible()
    } finally {
      await page.unroute(`**/templates/${templateId}/batch-tests**`)
    }
  })

  test('BDD-CE-U18-BTH-004/005/006: Run full test is async-only; completed does not call sync batch', async ({
    page,
    request,
  }) => {
    test.setTimeout(360_000)

    const syncBatchPosts: string[] = []
    const asyncBatchPosts: string[] = []
    page.on('request', (req) => {
      if (req.method() !== 'POST') {
        return
      }
      const url = req.url()
      if (url.includes('/previews/batch-test')) {
        syncBatchPosts.push(url)
      }
      if (url.includes('/batch-tests/run')) {
        asyncBatchPosts.push(url)
      }
    })

    await openFolDevEditorTestingTab(page, templateId, request)

    const actionRail = page.locator('.workspace-tab-shell__actions')
    const fullTestButton = actionRail.getByRole('button', { name: /^full test$/i })
    await expect(fullTestButton).toBeVisible()
    await expect(fullTestButton).toBeEnabled()

    // BTH-006 — no legacy sync batch entry in the Testing action rail.
    await expect(
      actionRail.getByRole('button', { name: /batch test generate|sync batch|run batch test/i }),
    ).toHaveCount(0)
    await expect(page.getByRole('button', { name: /batch test generate/i })).toHaveCount(0)

    await runFullTestFromUi(page, request, templateId)

    // BTH-004 — only async run was posted; BTH-005 — completion path never called sync batch.
    expect(asyncBatchPosts.length).toBeGreaterThanOrEqual(1)
    expect(syncBatchPosts).toEqual([])
    await expect(page.locator('.el-message').filter({ hasText: /batch test complete|批量测试完成/i })).toHaveCount(
      0,
    )

    const dialog = batchProgressDialog(page)
    if (await dialog.isVisible().catch(() => false)) {
      const closeBtn = dialog.getByRole('button', { name: /^close$/i })
      if (await closeBtn.isVisible().catch(() => false)) {
        await closeBtn.click()
        await expect(dialog).toBeHidden({ timeout: 15_000 })
      }
    }
    expect(syncBatchPosts).toEqual([])

    // History refresh after async completion (BTH-005) — wait for API then force panel reload.
    await expect
      .poll(async () => (await getBatchTestHistoryViaApi(request, templateId, 1)).length, {
        timeout: 60_000,
      })
      .toBeGreaterThan(0)

    await page.locator('.testing-sub-tabs').getByRole('tab', { name: /preview runs/i }).click()
    const history = page.locator('.batch-test-history')
    await expect(history).toBeVisible({ timeout: 30_000 })
    const refresh = history.getByRole('button', { name: /^refresh$/i })
    if (await refresh.isVisible().catch(() => false)) {
      await refresh.click()
    }
    await expect(history.locator('.batch-test-history__table tbody tr').first()).toBeVisible({
      timeout: 30_000,
    })
    expect(syncBatchPosts).toEqual([])
  })
})
