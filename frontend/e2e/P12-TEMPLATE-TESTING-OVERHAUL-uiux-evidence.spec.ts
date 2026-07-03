import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { assertFolCatalogSeeded } from './helpers/fol-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  batchProgressDialog,
  createTestDataSet,
  deleteTestDataSet,
  hoverSubmitForTestTooltip,
  listTestDataSets,
  openFolDevEditorTestingTab,
  previewProgressDialog,
  runFullTestFromUi,
  runPreviewFromFirstDataSetRow,
  submitForTestButton,
  touchTemplateContentForInvalidation,
  waitForPreviewConcurrencySlot,
} from './helpers/template-testing-api'
import {
  captureP12TemplateTestingOverhaulLocatorScreenshot,
  captureP12TemplateTestingOverhaulScreenshot,
  ensureP12TemplateTestingOverhaulEvidenceDirs,
  P12_TEMPLATE_TESTING_OVERHAUL_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P12-TEMPLATE-TESTING-OVERHAUL UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let templateId = ''

  test.beforeAll(async ({ request }) => {
    ensureP12TemplateTestingOverhaulEvidenceDirs()

    let backendReady = false
    let frontendReady = false
    try {
      backendReady = (await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })).ok()
    } catch {
      backendReady = false
    }
    try {
      frontendReady = (await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })).ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )

    const fixture = await assertFolCatalogSeeded(request)
    templateId = fixture.templateId
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P12_TEMPLATE_TESTING_OVERHAUL_VIEWPORT)
  })

  test('capture testing tab, data sets, coverage, history, progress dialogs, and dual-brand evidence', async ({
    page,
    request,
  }) => {
    test.setTimeout(600_000)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await touchTemplateContentForInvalidation(request, templateId)
    await openFolDevEditorTestingTab(page, templateId, request)

    const workspace = page.locator('.workspace-tab-shell')
    await expect(workspace.getByRole('tab', { name: /^template testing$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page.locator('.test-data-set-panel')).toBeVisible()
    await captureP12TemplateTestingOverhaulScreenshot(
      page,
      '01-testing-tab-data-sets-redbc-1440x900.png',
    )

    await expect(submitForTestButton(page)).toBeDisabled()
    await hoverSubmitForTestTooltip(page)
    await captureP12TemplateTestingOverhaulLocatorScreenshot(
      workspace.locator('.workspace-tab-shell__actions'),
      '02-submit-for-test-disabled-redbc-1440x900.png',
    )

    await switchBrand(page, 'GREENBC')
    await captureP12TemplateTestingOverhaulScreenshot(
      page,
      '03-testing-tab-data-sets-greenbc-1440x900.png',
    )
    await switchBrand(page, 'REDBC')

    const baselineDataSets = await listTestDataSets(request, templateId)
    const baselineDataSetId = baselineDataSets[0]?.testDataSetId
    if (baselineDataSetId) {
      await waitForPreviewConcurrencySlot(request, templateId, baselineDataSetId)
    }

    await runPreviewFromFirstDataSetRow(page)
    const previewDialog = previewProgressDialog(page)
    await expect(
      previewDialog.getByText(/queued|generating docx|converting to pdf|uploading/i).first(),
    ).toBeVisible({ timeout: 30_000 })
    await captureP12TemplateTestingOverhaulLocatorScreenshot(
      previewDialog,
      '04-preview-progress-in-flight-redbc-1440x900.png',
    )

    const previewTerminal = await Promise.race([
      previewDialog
        .getByRole('button', { name: /^download docx$/i })
        .waitFor({ state: 'visible', timeout: 240_000 })
        .then(() => 'success' as const),
      previewDialog
        .locator('.preview-progress__error')
        .waitFor({ state: 'visible', timeout: 240_000 })
        .then(() => 'error' as const),
    ])

    if (previewTerminal === 'success') {
      await captureP12TemplateTestingOverhaulLocatorScreenshot(
        previewDialog,
        '05-preview-progress-success-redbc-1440x900.png',
      )
    } else {
      await captureP12TemplateTestingOverhaulLocatorScreenshot(
        previewDialog,
        '05-preview-progress-failed-retry-redbc-1440x900.png',
      )
    }
    await previewDialog.getByRole('button', { name: /^close$/i }).click()
    await expect(previewDialog).toBeHidden({ timeout: 15_000 })

    if (baselineDataSetId && previewTerminal === 'success') {
      await waitForPreviewConcurrencySlot(request, templateId, baselineDataSetId)
    }

    const failingSet = await createTestDataSet(request, templateId, {
      name: `E2E UIUX preview failure ${Date.now()}`,
      required: false,
      variables: {},
      scenarioName: 'E2E UIUX intentional preview failure',
    })

    try {
      if (previewTerminal === 'success') {
        const failingRow = page.locator('.test-data-set-panel .el-table__body-wrapper tbody tr').filter({
          hasText: failingSet.name,
        })
        await failingRow.getByRole('button', { name: /^run preview$/i }).click()
        await expect(previewDialog).toBeVisible({ timeout: 30_000 })
        const failureVisible = await previewDialog
          .locator('.preview-progress__error')
          .isVisible({ timeout: 300_000 })
          .catch(() => false)
        if (failureVisible) {
          await expect(previewDialog.getByTestId('retry-btn')).toBeVisible()
          await captureP12TemplateTestingOverhaulLocatorScreenshot(
            previewDialog,
            '06-preview-progress-failed-retry-redbc-1440x900.png',
          )
        }
        if (await previewDialog.isVisible()) {
          await previewDialog.getByRole('button', { name: /^close$/i }).click()
        }
      }
    } finally {
      await deleteTestDataSet(request, templateId, failingSet.testDataSetId)
    }

    await runFullTestFromUi(page, request, templateId)
    const batchDialog = batchProgressDialog(page)
    await captureP12TemplateTestingOverhaulLocatorScreenshot(
      batchDialog,
      '07-batch-test-progress-complete-redbc-1440x900.png',
    )
    await batchDialog.getByRole('button', { name: /^close$/i }).click()

    await page.locator('.testing-sub-tabs').getByRole('tab', { name: /^coverage$/i }).click()
    const coveragePanel = page.locator('.coverage-panel')
    await expect(coveragePanel).toBeVisible({ timeout: 30_000 })
    await expect(coveragePanel.locator('.coverage-table')).toBeVisible()
    await captureP12TemplateTestingOverhaulScreenshot(
      page,
      '08-coverage-panel-redbc-1440x900.png',
    )

    await switchBrand(page, 'GREENBC')
    await captureP12TemplateTestingOverhaulScreenshot(
      page,
      '09-coverage-panel-greenbc-1440x900.png',
    )
    await switchBrand(page, 'REDBC')

    await page.locator('.testing-sub-tabs').getByRole('tab', { name: /preview runs/i }).click()
    const history = page.locator('.batch-test-history')
    await expect(history).toBeVisible()
    await expect(history.locator('.batch-test-history__table tbody tr').first()).toBeVisible({
      timeout: 30_000,
    })
    await captureP12TemplateTestingOverhaulScreenshot(
      page,
      '10-batch-test-history-redbc-1440x900.png',
    )
  })
})
