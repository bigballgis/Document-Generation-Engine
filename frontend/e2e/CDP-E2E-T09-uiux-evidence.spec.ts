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
import {
  captureCdpE2eDecisionLocatorScreenshot,
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`
const TASK_ID = 'CDP-E2E-T09' as const

test.describe('CDP-E2E-T09 UIUX evidence — preview comparison panel @1920 (BDD-CDP-CMP-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: PreviewComparisonFixture

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareSucceededPreviewWithComparison(request)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture Preview runs comparison panel + warningCode filter (REDBC @1920)', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await switchBrand(page, 'REDBC')
    await openTestingPreviewRunsTab(page, fixture.templateId, request)
    await openSucceededPreviewDetails(page)

    const panel = page.locator('.preview-panel')
    await expect(
      panel.getByRole('heading', { name: /structured preview comparison/i }),
    ).toBeVisible()
    await expect(panel.getByTestId('fidelity-warning-list')).toBeVisible()

    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-preview-runs-comparison-panel-redbc-1920x1080.png',
    )
    await captureCdpE2eDecisionLocatorScreenshot(
      panel,
      TASK_ID,
      '02-preview-comparison-panel-detail-redbc-1920x1080.png',
    )

    await page.getByTestId('filter-warning-code').fill(fixture.warningCode.split('_')[0] ?? 'IMAGE')
    await expect(panel.getByTestId('fidelity-warning-list').locator('.el-table__row').first()).toBeVisible()

    await captureCdpE2eDecisionLocatorScreenshot(
      panel.getByTestId('fidelity-warning-list'),
      TASK_ID,
      '03-fidelity-warning-code-filter-redbc-1920x1080.png',
    )
  })
})
