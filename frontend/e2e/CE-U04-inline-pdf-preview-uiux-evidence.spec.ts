import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, ensureDemoRetailMasterApproved } from './helpers/masters-api'
import { openDevBindingEditor } from './helpers/core-fortress-f7'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import {
  captureCdpE2eDecisionLocatorScreenshot,
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`
const TASK_ID = 'CE-U04' as const

async function dismissOnboardingTourIfPresent(page: Page) {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

test.describe('CE-U04 UIUX evidence — inline PDF preview @1920', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await ensureDemoRetailMasterApproved(request)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture side-by-side inline PDF viewer (REDBC + GREENBC @1920)', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await openDevBindingEditor(page, request, fixture.templateId)

      const refreshButton = page.getByTestId('authoring-preview-refresh')
      await refreshButton.click()
      await page.waitForResponse(
        (response) =>
          response.request().method() === 'GET' &&
          /\/previews\/[^/]+\/artifacts\/pdf$/.test(response.url()) &&
          response.ok(),
        { timeout: 120_000 },
      )

      const inlinePdf = page.getByTestId('preview-inline-pdf-section')
      const viewer = inlinePdf.getByTestId('inline-pdf-preview-viewer')
      await expect(viewer).toBeVisible({ timeout: 60_000 })
      await expect(inlinePdf.getByTestId('inline-pdf-preview-canvas')).toBeVisible({ timeout: 120_000 })

      const suffix = brand === 'REDBC' ? 'redbc' : 'greenbc'
      await captureCdpE2eDecisionScreenshot(
        page,
        TASK_ID,
        `01-side-by-side-inline-pdf-${suffix}-1920x1080.png`,
      )
      await captureCdpE2eDecisionLocatorScreenshot(
        viewer,
        TASK_ID,
        `02-inline-pdf-viewer-crop-${suffix}-1920x1080.png`,
      )
    }
  })
})
