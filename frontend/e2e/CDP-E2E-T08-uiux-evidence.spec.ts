import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  prepareCdpMvpGoldenDraft,
  type CdpMvpGoldenFixture,
} from './helpers/cdp-mvp-golden-api'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  listTestDataSets,
  openFolDevEditorTestingTab,
  previewProgressDialog,
  runPreviewFromFirstDataSetRow,
  waitForPreviewConcurrencySlot,
  waitForPreviewDialogSuccess,
} from './helpers/template-testing-api'
import {
  captureCdpE2eDecisionLocatorScreenshot,
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`
const TASK_ID = 'CDP-E2E-T08' as const

test.describe('CDP-E2E-T08 UIUX evidence — preview success dialog @1920 (BDD-CDP-PREV-003)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: CdpMvpGoldenFixture

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareCdpMvpGoldenDraft(request)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture testing tab + preview success dialog (closes P12 T13 success-frame gap)', async ({
    page,
    request,
  }) => {
    const dataSets = await listTestDataSets(request, fixture.templateId)
    expect(dataSets.length).toBeGreaterThan(0)
    await waitForPreviewConcurrencySlot(request, fixture.templateId, dataSets[0]!.testDataSetId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await switchBrand(page, 'REDBC')
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    await expect(page.locator('.test-data-set-panel')).toBeVisible()

    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-testing-tab-data-sets-redbc-1920x1080.png',
    )

    await runPreviewFromFirstDataSetRow(page)
    const dialog = previewProgressDialog(page)
    await waitForPreviewDialogSuccess(page)

    await expect(dialog.getByRole('link', { name: /^download docx$/i })).toBeVisible()
    await expect(dialog.getByRole('link', { name: /^download pdf$/i })).toBeVisible()
    await expect(dialog.getByText(/expires in/i)).toBeVisible()

    // Primary evidence: preview success frame with Download DOCX/PDF (P12 T13 gap closure).
    await captureCdpE2eDecisionLocatorScreenshot(
      dialog,
      TASK_ID,
      '02-preview-progress-success-redbc-1920x1080.png',
    )
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '03-preview-success-workspace-redbc-1920x1080.png',
    )
  })
})
