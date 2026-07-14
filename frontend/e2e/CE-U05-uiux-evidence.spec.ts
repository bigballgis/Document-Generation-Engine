import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
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
const TASK_ID = 'CE-U05' as const

async function dismissOnboardingTourIfPresent(page: Page) {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

test.describe('CE-U05 UIUX evidence — fidelity warning human copy + edit link @1920', () => {
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

  test('capture fidelity warning list human message + technical details (REDBC @1920)', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await openTestingPreviewRunsTab(page, fixture.templateId, request)
    await openSucceededPreviewDetails(page)

    const warningList = page.getByTestId('fidelity-warning-list')
    await expect(warningList).toBeVisible()
    await expect(page.getByTestId('fidelity-warning-human-message').first()).toBeVisible()

    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-fidelity-warnings-human-message-redbc-1920x1080.png',
    )
    await captureCdpE2eDecisionLocatorScreenshot(
      warningList,
      TASK_ID,
      '02-fidelity-warning-list-crop-redbc-1920x1080.png',
    )

    await page.getByTestId('fidelity-warning-technical-toggle').first().click()
    await expect(page.getByTestId('fidelity-warning-technical-details').first()).toBeVisible()
    await captureCdpE2eDecisionLocatorScreenshot(
      warningList,
      TASK_ID,
      '03-fidelity-warning-technical-expanded-redbc-1920x1080.png',
    )

    await expect(page.getByTestId('fidelity-warning-edit-binding').first()).toBeVisible()
    await captureCdpE2eDecisionLocatorScreenshot(
      page.getByTestId('fidelity-warning-edit-binding').first(),
      TASK_ID,
      '04-edit-binding-link-crop-redbc-1920x1080.png',
    )
  })
})
