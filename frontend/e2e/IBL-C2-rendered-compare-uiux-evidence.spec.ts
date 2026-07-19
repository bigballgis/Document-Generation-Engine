/**
 * IBL-C2 UIUX evidence — side-by-side rendered PDF compare dialog (F18)
 * Dual-brand REDBC/GREENBC @1920 + narrow stack @900 (Stage 7).
 * BDD: docs/behavior/ibl-c2-rendered-compare-ui.md
 *
 * Canonical run (after stage 5 DEPLOY_OK):
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/IBL-C2-rendered-compare-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  prepareCdpMvpGoldenDraft,
  type CdpMvpGoldenFixture,
} from './helpers/cdp-mvp-golden-api'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  openTestingPreviewRunsTab,
  runAsyncPreviewUntilTerminal,
} from './helpers/preview-comparison-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { listTestDataSets } from './helpers/template-testing-api'
import {
  captureIblC2LocatorScreenshot,
  captureIblC2Screenshot,
  dismissOnboardingTourIfPresent,
  ensureIblC2EvidenceDirs,
  IBL_C2_NARROW_VIEWPORT,
  IBL_C2_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

function previewHistory(page: Page) {
  return page.locator('.preview-run-history')
}

function compareButton(page: Page) {
  return page.getByTestId('compare-rendered-outputs')
}

function renderedCompareDialog(page: Page) {
  return page.getByTestId('rendered-compare-dialog')
}

function renderedComparePanel(page: Page) {
  return page.getByTestId('rendered-compare-panel')
}

async function clearPreviewRunSelection(page: Page): Promise<void> {
  const history = previewHistory(page)
  const checked = history.locator(
    '.el-table__body-wrapper tbody tr.el-table__row .el-checkbox.is-checked',
  )
  while ((await checked.count()) > 0) {
    await checked.first().click()
  }
}

async function selectComparablePreviewRunRows(page: Page, count: number): Promise<void> {
  const history = previewHistory(page)
  const rows = history.locator('.el-table__body-wrapper tbody tr.el-table__row')
  await expect(rows.first()).toBeVisible({ timeout: 30_000 })
  await clearPreviewRunSelection(page)

  const candidateIndexes: number[] = []
  const total = await rows.count()
  for (let i = 0; i < total && candidateIndexes.length < count; i += 1) {
    const pdfButton = rows.nth(i).getByRole('button', { name: /^download pdf$/i })
    if ((await pdfButton.count()) === 0) {
      continue
    }
    if (await pdfButton.isEnabled()) {
      candidateIndexes.push(i)
    }
  }
  expect(
    candidateIndexes.length,
    `Need ≥${count} SUCCEEDED+PDF preview rows (found ${candidateIndexes.length})`,
  ).toBeGreaterThanOrEqual(count)

  for (const index of candidateIndexes.slice(0, count)) {
    await rows.nth(index).locator('.el-checkbox__inner').click()
  }
}

async function openRenderedCompareWithTwoRuns(page: Page): Promise<void> {
  await selectComparablePreviewRunRows(page, 2)
  await expect(compareButton(page)).toBeEnabled({ timeout: 10_000 })
  await compareButton(page).click()
  await expect(renderedCompareDialog(page)).toBeVisible({ timeout: 15_000 })
  await expect(renderedComparePanel(page)).toBeVisible()
}

async function expectDualPdfCanvases(page: Page): Promise<void> {
  const paneA = page.getByTestId('rendered-compare-pane-a')
  const paneB = page.getByTestId('rendered-compare-pane-b')
  await expect(paneA.getByTestId('inline-pdf-preview-canvas')).toBeVisible({ timeout: 120_000 })
  await expect(paneB.getByTestId('inline-pdf-preview-canvas')).toBeVisible({ timeout: 120_000 })
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

async function assertSideBySideLayout(page: Page): Promise<void> {
  const boxes = await page.evaluate(() => {
    const a = document.querySelector('[data-testid="rendered-compare-pane-a"]')
    const b = document.querySelector('[data-testid="rendered-compare-pane-b"]')
    if (!a || !b) {
      return null
    }
    const ra = a.getBoundingClientRect()
    const rb = b.getBoundingClientRect()
    return {
      a: { left: ra.left, top: ra.top, right: ra.right, bottom: ra.bottom, width: ra.width },
      b: { left: rb.left, top: rb.top, right: rb.right, bottom: rb.bottom, width: rb.width },
    }
  })
  expect(boxes, 'both compare panes must be in DOM').not.toBeNull()
  // Side-by-side: B starts to the right of A with meaningful horizontal overlap in Y.
  expect(boxes!.b.left).toBeGreaterThan(boxes!.a.left + boxes!.a.width * 0.4)
  const verticalOverlap =
    Math.min(boxes!.a.bottom, boxes!.b.bottom) - Math.max(boxes!.a.top, boxes!.b.top)
  expect(verticalOverlap).toBeGreaterThan(40)
}

async function assertStackedLayout(page: Page): Promise<void> {
  const boxes = await page.evaluate(() => {
    const a = document.querySelector('[data-testid="rendered-compare-pane-a"]')
    const b = document.querySelector('[data-testid="rendered-compare-pane-b"]')
    if (!a || !b) {
      return null
    }
    const ra = a.getBoundingClientRect()
    const rb = b.getBoundingClientRect()
    return {
      a: { left: ra.left, top: ra.top, right: ra.right, bottom: ra.bottom },
      b: { left: rb.left, top: rb.top, right: rb.right, bottom: rb.bottom },
    }
  })
  expect(boxes, 'both compare panes must be in DOM').not.toBeNull()
  // Stacked: B below A (grid 1fr at ≤960px).
  expect(boxes!.b.top).toBeGreaterThan(boxes!.a.top + 20)
}

async function expectNoCriticalAxeViolations(page: Page, include: string): Promise<void> {
  const results = await new AxeBuilder({ page }).include(include).analyze()
  const critical = results.violations.filter((v) => v.impact === 'critical')
  expect(critical, JSON.stringify(critical, null, 2)).toEqual([])
}

test.describe('IBL-C2 UIUX evidence — rendered compare @1920', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: CdpMvpGoldenFixture

  test.beforeAll(async ({ request }) => {
    ensureIblC2EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    fixture = await prepareCdpMvpGoldenDraft(request)
    expect(fixture.lifecycleStatus).toBe('DRAFT')

    const dataSets = await listTestDataSets(request, fixture.templateId)
    expect(dataSets.length).toBeGreaterThan(0)
    const testDataSetId = dataSets[0]!.testDataSetId

    const previewA = await runAsyncPreviewUntilTerminal(request, fixture.templateId, testDataSetId)
    const previewB = await runAsyncPreviewUntilTerminal(request, fixture.templateId, testDataSetId)
    expect(previewA.status).toBe('SUCCEEDED')
    expect(previewB.status).toBe('SUCCEEDED')
  })

  test('REDBC @1920 — side-by-side dual PDF panes + brand header', async ({ page, request }) => {
    await page.setViewportSize(IBL_C2_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    // Longer post-login wait — delayed LR-C8 tour otherwise races switchBrand.
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 15_000 })
    await switchBrand(page, 'REDBC')
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
    await openTestingPreviewRunsTab(page, fixture.templateId, request)
    await openRenderedCompareWithTwoRuns(page)
    await expectDualPdfCanvases(page)
    await assertSideBySideLayout(page)
    await assertNoViewportOverflow(page)

    await expect(renderedCompareDialog(page)).toContainText(/compare rendered outputs/i)
    await expect(page.getByTestId('rendered-compare-preview-id')).toHaveCount(2)

    await captureIblC2Screenshot(page, '01-side-by-side-compare-redbc-1920x1080.png')
    await captureIblC2LocatorScreenshot(
      renderedCompareDialog(page),
      '01b-compare-dialog-crop-redbc-1920x1080.png',
    )
    await captureIblC2LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )

    const primary = await page
      .getByTestId('rendered-compare-close')
      .evaluate((el) => getComputedStyle(el).backgroundColor)
    // REDBC primary ≈ #DB0011
    expect(primary).toMatch(/rgb\(\s*219,\s*0,\s*17\s*\)/)

    await expectNoCriticalAxeViolations(page, '[data-testid="rendered-compare-dialog"]')
  })

  test('GREENBC @1920 — dual-brand compare dialog', async ({ page, request }) => {
    await page.setViewportSize(IBL_C2_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 15_000 })
    await switchBrand(page, 'GREENBC')
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
    await openTestingPreviewRunsTab(page, fixture.templateId, request)
    await openRenderedCompareWithTwoRuns(page)
    await expectDualPdfCanvases(page)
    await assertSideBySideLayout(page)
    await assertNoViewportOverflow(page)

    await captureIblC2Screenshot(page, '02-side-by-side-compare-greenbc-1920x1080.png')
    await captureIblC2LocatorScreenshot(
      renderedCompareDialog(page),
      '02b-compare-dialog-crop-greenbc-1920x1080.png',
    )
    await captureIblC2LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02c-brand-header-greenbc-crop.png',
    )

    const primary = await page
      .getByTestId('rendered-compare-close')
      .evaluate((el) => getComputedStyle(el).backgroundColor)
    // GREENBC primary ≈ #00847F
    expect(primary).toMatch(/rgb\(\s*0,\s*132,\s*127\s*\)/)

    await expectNoCriticalAxeViolations(page, '[data-testid="rendered-compare-dialog"]')
  })

  test('Narrow @900 — stacked panes still show both PDFs (REDBC)', async ({ page, request }) => {
    await page.setViewportSize(IBL_C2_NARROW_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 15_000 })
    await switchBrand(page, 'REDBC')
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
    await openTestingPreviewRunsTab(page, fixture.templateId, request)
    await openRenderedCompareWithTwoRuns(page)
    await expectDualPdfCanvases(page)
    await assertStackedLayout(page)

    await captureIblC2Screenshot(page, '03-stacked-compare-redbc-900x900.png')
    await captureIblC2LocatorScreenshot(
      renderedComparePanel(page),
      '03b-stacked-panel-crop-redbc-900x900.png',
    )
  })
})
