import { expect, test } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

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
import { switchBrand } from './helpers/uiux-evidence'

/**
 * LRP-E1 Stage 7 light UIUX evidence — PreviewProgressDialog during SSE Scenario A.
 * No product UI changes in this slice; reuses CD-E2E-T08 / P12 visual bar.
 */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const VIEWPORT = { width: 1440, height: 900 } as const
const SPEC_DIR = path.dirname(fileURLToPath(import.meta.url))
const SCREENSHOT_DIR = path.join(SPEC_DIR, 'evidence', 'LRP-E1', 'screenshots')

test.describe('LRP-E1 UIUX evidence — preview progress dialog @1440 (light)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: CdpMvpGoldenFixture

  test.beforeAll(async ({ request }) => {
    mkdirSync(SCREENSHOT_DIR, { recursive: true })
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareCdpMvpGoldenDraft(request)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(VIEWPORT)
  })

  test('capture preview progress success dialog after Scenario A journey', async ({
    page,
    request,
  }) => {
    const dataSets = await listTestDataSets(request, fixture.templateId)
    expect(dataSets.length).toBeGreaterThan(0)
    await waitForPreviewConcurrencySlot(request, fixture.templateId, dataSets[0]!.testDataSetId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    // Dismiss LR-C8 onboarding tour if present (blocks brand switcher / header clicks).
    const skipTour = page.getByTestId('onboarding-tour-skip')
    if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await skipTour.click()
      await expect(skipTour).toHaveCount(0)
    }
    await switchBrand(page, 'REDBC')
    await openFolDevEditorTestingTab(page, fixture.templateId, request)

    await runPreviewFromFirstDataSetRow(page)
    const dialog = previewProgressDialog(page)

    // Mid-journey: progress label visible (SSE incremental path).
    await expect(
      dialog.getByText(/queued|generating docx|converting to pdf|uploading/i).first(),
    ).toBeVisible({ timeout: 30_000 })
    await dialog.screenshot({
      path: path.join(SCREENSHOT_DIR, '01-preview-progress-in-flight-redbc-1440x900.png'),
    })

    await waitForPreviewDialogSuccess(page)
    await expect(dialog.getByRole('link', { name: /^download docx$/i })).toBeVisible()
    await expect(dialog.getByRole('link', { name: /^download pdf$/i })).toBeVisible()

    await dialog.screenshot({
      path: path.join(SCREENSHOT_DIR, '02-preview-progress-success-redbc-1440x900.png'),
    })
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, '03-preview-success-workspace-redbc-1440x900.png'),
      fullPage: false,
    })
  })
})
