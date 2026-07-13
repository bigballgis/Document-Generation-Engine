import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  openSucceededPreviewDetails,
  openTestingPreviewRunsTab,
  prepareSucceededPreviewWithComparison,
  type PreviewComparisonFixture,
} from './helpers/preview-comparison-api'

/** Docker acceptance UI (override with E2E_BASE_URL / FRONTEND_PORT). */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

test.describe('CDP-E2E-T09 Preview vs final comparison (BDD-CDP-CMP-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: PreviewComparisonFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    // API seed only — UI journey opens Preview runs → View details (no ensureDemoFullFlowAtStage).
    fixture = await prepareSucceededPreviewWithComparison(request)
    expect(fixture.preview.status).toBe('SUCCEEDED')
    expect(fixture.preview.fidelityWarnings.length).toBeGreaterThan(0)
    expect(fixture.preview.previewComparison).toBeTruthy()
  })

  test('BDD-CDP-CMP-001 — comparison panel + location columns + warningCode filter', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await openTestingPreviewRunsTab(page, fixture.templateId, request)
    await openSucceededPreviewDetails(page)

    const panel = page.locator('.preview-panel')
    await expect(panel.getByText(/comparison summary/i)).toBeVisible()
    await expect(
      panel.getByRole('heading', { name: /structured preview comparison/i }),
    ).toBeVisible()

    const comparisonItems = fixture.preview.previewComparison?.items ?? []
    if (comparisonItems.length > 0) {
      const table = panel.locator('.comparison-table')
      await expect(table).toBeVisible()
      const first = comparisonItems[0]!
      await expect(table).toContainText(first.locationRef)
      // locationType is i18n-labeled (e.g. Component / Anchor) or raw enum fallback
      await expect(table.locator('.el-table__row').first()).toBeVisible()
      await expect(table).toContainText(/component|anchor|page|section|COMPONENT|ANCHOR|PAGE|SECTION/i)
    } else {
      await expect(panel.getByText(/no structured comparison differences recorded/i)).toBeVisible()
    }

    const warningList = panel.getByTestId('fidelity-warning-list')
    await expect(warningList).toBeVisible()
    await expect(warningList).toContainText(fixture.warningCode)
    await expect(page.getByTestId('filter-warning-code')).toBeVisible()

    // Match known code → only matching rows remain
    const matchToken = fixture.warningCode.includes('_')
      ? fixture.warningCode.split('_')[0]!
      : fixture.warningCode.slice(0, Math.min(6, fixture.warningCode.length))
    await page.getByTestId('filter-warning-code').fill(matchToken)
    await expect(warningList.locator('.el-table__row').first()).toBeVisible()
    await expect(warningList).toContainText(fixture.warningCode)

    // Non-matching code → empty filter state
    await page.getByTestId('filter-warning-code').fill('UNRESOLVED_VARIABLE_NO_MATCH_E2E')
    await expect(page.getByText(/no warnings match the current filters/i)).toBeVisible()
  })
})
