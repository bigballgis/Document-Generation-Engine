/**
 * CE-U18 UIUX evidence — Batch test history sampleResults expand + Full test action rail
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/ce-u18-batch-test-history.md (BTH-001 / BTH-006 visual surfaces)
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import {
  createTestDataSet,
  getBatchTestHistoryViaApi,
  openFolDevEditorTestingTab,
  runBatchTestViaApi,
  type BatchTestRunSummary,
} from './helpers/template-testing-api'
import {
  captureCeU18LocatorScreenshot,
  captureCeU18Screenshot,
  CE_U18_VIEWPORT,
  ensureCeU18EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

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
  await dismissOnboardingTourIfPresent(page)
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

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return {
      scrollWidth: doc.scrollWidth,
      clientWidth: doc.clientWidth,
    }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function expectNoCriticalAxeViolations(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

function actionRail(page: Page) {
  return page.locator('.workspace-tab-shell__actions')
}

async function assertFullTestActionRail(page: Page): Promise<void> {
  const rail = actionRail(page)
  const fullTestButton = rail.getByRole('button', { name: /^full test$/i })
  await expect(fullTestButton).toBeVisible()
  await expect(fullTestButton).toBeEnabled()
  await expect(
    rail.getByRole('button', { name: /batch test generate|sync batch|run batch test/i }),
  ).toHaveCount(0)
}

test.describe('CE-U18 batch test history UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let templateId = ''

  test.beforeAll(async ({ request }) => {
    ensureCeU18EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)

    const draft = await prepareDraftTemplateWithCleanBinding(request)
    templateId = draft.templateId

    await createTestDataSet(request, templateId, {
      name: `E2E CE-U18 UIUX A ${Date.now()}`,
      required: true,
      variables: { customerName: 'Alice CE-U18 UIUX' },
      scenarioName: 'CE-U18 UIUX sample A',
    })
    await createTestDataSet(request, templateId, {
      name: `E2E CE-U18 UIUX B ${Date.now()}`,
      required: true,
      variables: { customerName: 'Bob CE-U18 UIUX' },
      scenarioName: 'CE-U18 UIUX sample B',
    })
  })

  test('01–04 dual-brand: expanded Sample results + Full test rail (BTH-001/006)', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U18_VIEWPORT)

    const completed = await ensureCompletedHistoryWithSamples(request, templateId)
    expect(completed.sampleResults!.length).toBeGreaterThanOrEqual(2)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    const history = await openPreviewRunsHistory(page, templateId, request)
    await expandFirstHistoryRow(history)

    const samples = history.getByTestId('batch-history-sample-results')
    await expect(samples.getByRole('heading', { name: /sample results/i })).toBeVisible()
    await expect(samples.getByTestId('batch-history-open-data-set').first()).toBeVisible()
    await expect(samples.getByText(/succeeded|failed/i).first()).toBeVisible()
    await assertFullTestActionRail(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U18 REDBC expanded sample results')

    await captureCeU18Screenshot(page, '01-batch-history-expanded-redbc-1920x1080.png')
    await captureCeU18LocatorScreenshot(
      history,
      '01b-batch-test-history-panel-crop-redbc-1920x1080.png',
    )
    await captureCeU18LocatorScreenshot(
      samples,
      '01c-sample-results-expand-crop-redbc-1920x1080.png',
    )
    await captureCeU18LocatorScreenshot(
      actionRail(page),
      '01d-full-test-action-rail-crop-redbc-1920x1080.png',
    )
    await captureCeU18LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01e-brand-header-redbc-crop.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')

    // Re-assert expand still visible after brand switch; re-expand if table re-rendered.
    const historyGreen = page.locator('.batch-test-history')
    await expect(historyGreen).toBeVisible()
    await expandFirstHistoryRow(historyGreen)
    const samplesGreen = historyGreen.getByTestId('batch-history-sample-results')
    await expect(samplesGreen.getByRole('heading', { name: /sample results/i })).toBeVisible()
    await assertFullTestActionRail(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U18 GREENBC expanded sample results')

    await captureCeU18Screenshot(page, '02-batch-history-expanded-greenbc-1920x1080.png')
    await captureCeU18LocatorScreenshot(
      historyGreen,
      '02b-batch-test-history-panel-crop-greenbc-1920x1080.png',
    )
    await captureCeU18LocatorScreenshot(
      samplesGreen,
      '02c-sample-results-expand-crop-greenbc-1920x1080.png',
    )
    await captureCeU18LocatorScreenshot(
      actionRail(page),
      '02d-full-test-action-rail-crop-greenbc-1920x1080.png',
    )
    await captureCeU18LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02e-brand-header-greenbc-crop.png',
    )
  })

  test('03–04 dual-brand: Sample results nested table density after Open data set path', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U18_VIEWPORT)

    await ensureCompletedHistoryWithSamples(request, templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    const history = await openPreviewRunsHistory(page, templateId, request)
    await expandFirstHistoryRow(history)

    const samples = history.getByTestId('batch-history-sample-results')
    await expect(samples.locator('.batch-test-history__samples-table')).toBeVisible()
    const rowCount = await samples.locator('.batch-test-history__samples-table tbody tr').count()
    expect(rowCount).toBeGreaterThanOrEqual(2)

    // Nested table scrolls internally when tall — panel itself must not overflow viewport.
    const samplesBox = await samples.boundingBox()
    expect(samplesBox, 'sample results bounding box').toBeTruthy()
    expect(samplesBox!.width).toBeLessThanOrEqual(CE_U18_VIEWPORT.width)

    await samples.getByTestId('batch-history-open-data-set').first().click()
    await expect(page).toHaveURL(/testingTab=dataSets/)
    await expect(page.locator('.testing-sub-tabs').getByRole('tab', { name: /data sets/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U18 REDBC after Open data set')

    await captureCeU18Screenshot(page, '03-open-data-set-data-sets-tab-redbc-1920x1080.png')
    await captureCeU18LocatorScreenshot(
      page.locator('.test-data-set-panel'),
      '03b-data-sets-panel-crop-redbc-1920x1080.png',
    )
    await captureCeU18LocatorScreenshot(
      actionRail(page),
      '03c-full-test-rail-after-open-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.locator('.test-data-set-panel')).toBeVisible()
    await assertFullTestActionRail(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U18 GREENBC after Open data set')

    await captureCeU18Screenshot(page, '04-open-data-set-data-sets-tab-greenbc-1920x1080.png')
    await captureCeU18LocatorScreenshot(
      page.locator('.test-data-set-panel'),
      '04b-data-sets-panel-crop-greenbc-1920x1080.png',
    )
    await captureCeU18LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04c-brand-header-greenbc-crop.png',
    )
  })
})
