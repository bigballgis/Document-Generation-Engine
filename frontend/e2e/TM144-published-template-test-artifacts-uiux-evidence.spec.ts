/**
 * TM #144 / published-template-test-artifacts — UIUX evidence (Stage 7)
 * PUBLISHED release Testing read-only: summary + batch history + preview history.
 * Dual-brand REDBC/GREENBC @1920×1080.
 *
 * BDD: docs/behavior/published-template-test-artifacts.md
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts `
 *     e2e/TM144-published-template-test-artifacts-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, FOL_TEMPLATE_EXTERNAL_ID, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { listPreviewRunsViaApi } from './helpers/preview-comparison-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  getBatchTestHistoryViaApi,
  type BatchTestRunSummary,
} from './helpers/template-testing-api'
import {
  captureTm144PtaLocatorScreenshot,
  captureTm144PtaScreenshot,
  dismissOnboardingTourIfPresent,
  ensureTm144PtaEvidenceDirs,
  switchBrand,
  TM144_PTA_VIEWPORT,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

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
    'No PUBLISHED template with SUCCEEDED preview history found for PTA UIUX evidence',
  )
}

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

async function expandFirstBatchHistoryRow(page: Page) {
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

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return { scrollWidth: doc.scrollWidth, clientWidth: doc.clientWidth }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function assertPrimaryBrandColor(page: Page, brand: 'REDBC' | 'GREENBC'): Promise<void> {
  const expected = brand === 'REDBC' ? '#db0011' : '#00847f'
  const primary = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim().toLowerCase(),
  )
  expect(primary, `expected --brand-primary ${expected} for ${brand}`).toBe(expected)
}

async function expectNoCriticalAxeViolations(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

async function assertPublishedTestingNoAuthoring(page: Page): Promise<void> {
  const testing = page.getByTestId('release-testing-readonly')
  await expect(testing.getByRole('button', { name: /^run preview$/i })).toHaveCount(0)
  await expect(testing.getByRole('button', { name: /^full test$/i })).toHaveCount(0)
  await expect(testing.getByRole('button', { name: /run full test/i })).toHaveCount(0)
  await expect(page.locator('.test-data-set-panel')).toHaveCount(0)
  await expect(page.locator('#dev-workspace')).toHaveCount(0)
  // Do NOT assert showAuthoringSection true for PUBLISHED (Stage 6 residual).
}

async function assertSelectedRowHighlightVisible(page: Page): Promise<void> {
  const selected = page.locator(
    '.preview-run-history .el-table__body-wrapper tbody tr.el-table__row.is-selected',
  )
  await expect(selected).toBeVisible({ timeout: 15_000 })
  const bg = await selected.locator('td').first().evaluate((el) => {
    return getComputedStyle(el).backgroundColor
  })
  // Must not be fully transparent / identical to default white-only (RGB all 255 or rgba 0)
  expect(bg, 'selected row background').not.toMatch(/^rgba?\(\s*0\s*,\s*0\s*,\s*0\s*,\s*0\s*\)$/)
  const match = bg.match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/i)
  expect(match, `parseable backgroundColor: ${bg}`).toBeTruthy()
  const r = Number(match![1])
  const g = Number(match![2])
  const b = Number(match![3])
  // Brand accent soft should tint away from pure white
  const isNearWhite = r > 250 && g > 250 && b > 250
  expect(isNearWhite, `selected highlight too weak (bg=${bg})`).toBe(false)
}

async function assertToastDoesNotObscureDownloads(page: Page): Promise<void> {
  const toast = page.locator('.el-message').filter({
    hasText:
      /data sets are not editable on published release detail\. use preview run history below to review and download prior test artifacts/i,
  })
  await expect(toast).toBeVisible({ timeout: 10_000 })

  const downloadDocx = page
    .locator('.preview-run-history')
    .getByRole('button', { name: /^download docx$/i })
    .first()
  await expect(downloadDocx).toBeVisible()

  const toastBox = await toast.boundingBox()
  const buttonBox = await downloadDocx.boundingBox()
  expect(toastBox && buttonBox, 'toast and download button boxes').toBeTruthy()

  const overlapX =
    toastBox!.x < buttonBox!.x + buttonBox!.width && toastBox!.x + toastBox!.width > buttonBox!.x
  const overlapY =
    toastBox!.y < buttonBox!.y + buttonBox!.height && toastBox!.y + toastBox!.height > buttonBox!.y
  expect(overlapX && overlapY, 'toast must not obscure Download DOCX').toBe(false)
}

test.describe('TM #144 published-template-test-artifacts UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  let fixture: PtaFixture

  test.beforeAll(async ({ request }) => {
    ensureTm144PtaEvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await preparePtaPublishedFixture(request)
  })

  test('01–02 dual-brand: release Testing read-only surface (summary + histories)', async ({
    page,
  }) => {
    await page.setViewportSize(TM144_PTA_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await assertPrimaryBrandColor(page, 'REDBC')

    await openReleaseTestingTab(page, fixture)

    await expect(
      page.getByText(
        /this published release completed the testing workflow before go-live\. snapshot data below is read-only/i,
      ),
    ).toBeVisible()
    await expect(page.getByRole('heading', { name: /^test run history$/i })).toBeVisible()
    await expect(page.getByRole('heading', { name: /^preview run history$/i })).toBeVisible()
    await assertPublishedTestingNoAuthoring(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'TM144 REDBC release Testing')

    await captureTm144PtaScreenshot(page, '01-release-testing-readonly-redbc-1920x1080.png')
    await captureTm144PtaLocatorScreenshot(
      page.getByTestId('release-testing-readonly'),
      '01b-release-testing-panel-crop-redbc-1920x1080.png',
    )
    await captureTm144PtaLocatorScreenshot(
      page.locator('.batch-test-history'),
      '01c-batch-history-crop-redbc-1920x1080.png',
    )
    await captureTm144PtaLocatorScreenshot(
      page.locator('.preview-run-history'),
      '01d-preview-history-crop-redbc-1920x1080.png',
    )
    await captureTm144PtaLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01e-brand-header-redbc-crop.png',
    )

    await switchBrand(page, 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')
    await expect(page.getByTestId('release-testing-readonly')).toBeVisible()
    await assertPublishedTestingNoAuthoring(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'TM144 GREENBC release Testing')

    await captureTm144PtaScreenshot(page, '02-release-testing-readonly-greenbc-1920x1080.png')
    await captureTm144PtaLocatorScreenshot(
      page.getByTestId('release-testing-readonly'),
      '02b-release-testing-panel-crop-greenbc-1920x1080.png',
    )
    await captureTm144PtaLocatorScreenshot(
      page.locator('.batch-test-history'),
      '02c-batch-history-crop-greenbc-1920x1080.png',
    )
    await captureTm144PtaLocatorScreenshot(
      page.locator('.preview-run-history'),
      '02d-preview-history-crop-greenbc-1920x1080.png',
    )
    await captureTm144PtaLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02e-brand-header-greenbc-crop.png',
    )
  })

  test('03–04 dual-brand: Open preview selection highlight + DOCX/PDF affordances', async ({
    page,
  }) => {
    await page.setViewportSize(TM144_PTA_VIEWPORT)
    await withBatchHistorySamples(page, fixture.templateId, {
      previewId: fixture.succeededPreviewId,
    })

    try {
      await loginAs(page, E2E_TEMPLATE_AUTHOR)
      await dismissOnboardingTourIfPresent(page)
      await switchBrand(page, 'REDBC')

      await openReleaseTestingTab(page, fixture)
      const samples = await expandFirstBatchHistoryRow(page)
      const openPreview = samples.getByTestId('batch-history-open-preview').first()
      await expect(openPreview).toBeVisible({ timeout: 15_000 })
      await openPreview.click()

      await assertSelectedRowHighlightVisible(page)
      const selected = page.locator(
        '.preview-run-history .el-table__body-wrapper tbody tr.el-table__row.is-selected',
      )
      await expect(selected.getByRole('button', { name: /^download docx$/i })).toBeEnabled()
      await expect(selected.getByRole('button', { name: /^download pdf$/i })).toBeEnabled()
      await assertPublishedTestingNoAuthoring(page)
      await assertNoViewportOverflow(page)
      await expectNoCriticalAxeViolations(page, 'TM144 REDBC after Open preview')

      await captureTm144PtaScreenshot(page, '03-open-preview-selected-redbc-1920x1080.png')
      await captureTm144PtaLocatorScreenshot(
        page.locator('.preview-run-history'),
        '03b-preview-history-selected-crop-redbc-1920x1080.png',
      )
      await captureTm144PtaLocatorScreenshot(
        selected,
        '03c-selected-row-crop-redbc-1920x1080.png',
      )

      await switchBrand(page, 'GREENBC')
      await assertPrimaryBrandColor(page, 'GREENBC')
      // Selection may reset on remount; re-open preview if needed
      const selectedGreen = page.locator(
        '.preview-run-history .el-table__body-wrapper tbody tr.el-table__row.is-selected',
      )
      if ((await selectedGreen.count()) === 0) {
        const samplesGreen = await expandFirstBatchHistoryRow(page)
        await samplesGreen.getByTestId('batch-history-open-preview').first().click()
      }
      await assertSelectedRowHighlightVisible(page)
      await assertNoViewportOverflow(page)
      await expectNoCriticalAxeViolations(page, 'TM144 GREENBC after Open preview')

      await captureTm144PtaScreenshot(page, '04-open-preview-selected-greenbc-1920x1080.png')
      await captureTm144PtaLocatorScreenshot(
        page.locator('.preview-run-history'),
        '04b-preview-history-selected-crop-greenbc-1920x1080.png',
      )
      await captureTm144PtaLocatorScreenshot(
        page.locator(
          '.preview-run-history .el-table__body-wrapper tbody tr.el-table__row.is-selected',
        ),
        '04c-selected-row-crop-greenbc-1920x1080.png',
      )
      await captureTm144PtaLocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        '04d-brand-header-greenbc-crop.png',
      )
    } finally {
      await page.unroute(`**/templates/${fixture.templateId}/batch-tests**`)
    }
  })

  test('05–06 dual-brand: Open data set toast does not obscure downloads', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(TM144_PTA_VIEWPORT)

    const history = await getBatchTestHistoryViaApi(request, fixture.templateId)
    const hasSamples = history.some(
      (row) => Array.isArray(row.sampleResults) && row.sampleResults.length > 0,
    )
    if (!hasSamples) {
      await withBatchHistorySamples(page, fixture.templateId, {
        previewId: fixture.succeededPreviewId,
      })
    } else {
      await withBatchHistorySamples(page, fixture.templateId, {
        previewId: fixture.succeededPreviewId,
      })
    }

    try {
      await loginAs(page, E2E_TEMPLATE_AUTHOR)
      await dismissOnboardingTourIfPresent(page)
      await switchBrand(page, 'REDBC')

      await openReleaseTestingTab(page, fixture)
      // Ensure a SUCCEEDED preview row is selected so downloads are in view
      const previewHistory = page.locator('.preview-run-history')
      const succeededRow = previewHistory
        .locator('.el-table__body-wrapper tbody tr.el-table__row')
        .filter({ hasText: /succeeded/i })
        .first()
      await expect(succeededRow).toBeVisible({ timeout: 30_000 })
      await succeededRow.getByRole('button', { name: /^details$/i }).click()
      await expect(succeededRow).toHaveClass(/is-selected/)

      const samples = await expandFirstBatchHistoryRow(page)
      await samples.getByTestId('batch-history-open-data-set').first().click()

      await assertToastDoesNotObscureDownloads(page)
      // Capture while toast is still visible (el-message auto-dismisses).
      const toastRed = page.locator('.el-message').filter({
        hasText: /data sets are not editable on published release detail/i,
      })
      await captureTm144PtaLocatorScreenshot(toastRed, '05b-toast-crop-redbc.png')
      await captureTm144PtaScreenshot(page, '05-open-data-set-toast-redbc-1920x1080.png')
      await captureTm144PtaLocatorScreenshot(
        previewHistory,
        '05c-preview-downloads-with-toast-crop-redbc-1920x1080.png',
      )

      await assertPublishedTestingNoAuthoring(page)
      await expect(page.locator('.test-data-set-panel')).toHaveCount(0)
      await assertNoViewportOverflow(page)
      await expectNoCriticalAxeViolations(page, 'TM144 REDBC open data set toast')

      await switchBrand(page, 'GREENBC')
      await assertPrimaryBrandColor(page, 'GREENBC')

      // Re-trigger toast under GREENBC (prior message may have dismissed).
      const samplesGreen = await expandFirstBatchHistoryRow(page)
      await samplesGreen.getByTestId('batch-history-open-data-set').first().click()
      await assertToastDoesNotObscureDownloads(page)
      const toastGreen = page.locator('.el-message').filter({
        hasText: /data sets are not editable on published release detail/i,
      })
      await captureTm144PtaLocatorScreenshot(toastGreen, '06b-toast-crop-greenbc.png')
      await captureTm144PtaScreenshot(page, '06-open-data-set-toast-greenbc-1920x1080.png')
      await captureTm144PtaLocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        '06c-brand-header-greenbc-crop.png',
      )

      await assertPublishedTestingNoAuthoring(page)
      await assertNoViewportOverflow(page)
      await expectNoCriticalAxeViolations(page, 'TM144 GREENBC open data set toast')
    } finally {
      await page.unroute(`**/templates/${fixture.templateId}/batch-tests**`).catch(() => undefined)
    }
  })
})
