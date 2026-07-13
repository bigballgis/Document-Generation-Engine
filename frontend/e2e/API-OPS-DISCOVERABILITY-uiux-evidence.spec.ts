/**
 * API-OPS-DISCOVERABILITY UIUX evidence — overview summary + Hub External access warnings.
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/API-OPS-DISCOVERABILITY-uiux-evidence.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Screenshots: frontend/e2e/evidence/API-OPS-DISCOVERABILITY/screenshots/
 * Manifest:    frontend/e2e/evidence/API-OPS-DISCOVERABILITY-uiux-manifest.md
 */
import { expect, test } from '@playwright/test'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { fetchDemoFullFlowApiPolicy } from './helpers/content-modules-api'
import { openDevEditorWorkspaceTab } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  prepareTemplatePendingRelease,
  type PendingSubmitTemplateFixture,
} from './helpers/submit-approval-gate-api'
import {
  API_OPS_DISCOVERABILITY_VIEWPORT,
  captureApiOpsDiscoverabilityLocatorScreenshot,
  captureApiOpsDiscoverabilityScreenshot,
  ensureApiOpsDiscoverabilityEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('API-OPS-DISCOVERABILITY UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: PendingSubmitTemplateFixture

  test.beforeAll(async ({ request }) => {
    ensureApiOpsDiscoverabilityEvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)

    fixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-AOD-UX-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E API Ops UX ${Date.now().toString(36).toUpperCase()}`,
    })

    const policy = await fetchDemoFullFlowApiPolicy(request, fixture.templateId)
    expect(Array.isArray(policy.allowedAdGroups)).toBe(true)
    expect(policy.allowedAdGroups.length).toBe(0)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(API_OPS_DISCOVERABILITY_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    // Group Admin may see first-run onboarding; dismiss so brand switcher is clickable.
    const skipTour = page.getByTestId('onboarding-tour-skip')
    if (await skipTour.isVisible().catch(() => false)) {
      await skipTour.click()
      await expect(page.getByTestId('onboarding-tour-skip')).toHaveCount(0)
    }
  })

  test('capture overview + hub warnings — REDBC + GREENBC', async ({ page, request }) => {
    // --- Overview /api/policies (REDBC) ---
    await page.goto('/api/policies')
    await expect(page.locator('.page-header h1')).toHaveText(/external services overview/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const skipTour = page.getByTestId('onboarding-tour-skip')
    if (await skipTour.isVisible().catch(() => false)) {
      await skipTour.click()
    }
    await switchBrand(page, 'REDBC')
    await page.goto('/api/policies')
    await expect(page.locator('.page-header h1')).toHaveText(/external services overview/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByTestId('api-readiness-summary')).toBeVisible()
    await expect(page.getByTestId('summary-card-publishedInScope')).toBeVisible()
    await expect(page.getByTestId('summary-card-attention')).toBeVisible()
    await expect(page.getByTestId('summary-card-pendingReleaseNeedingSetup')).toBeVisible()
    await expect(page.locator('.alerts-card')).toBeVisible()
    await expect(page.getByRole('heading', { name: /^published packages$/i })).toHaveCount(0)
    await expect(page.locator('.alerts-card .el-pagination')).toHaveCount(0)

    await captureApiOpsDiscoverabilityScreenshot(page, '01-overview-summary-alerts-REDBC.png')
    await captureApiOpsDiscoverabilityLocatorScreenshot(
      page.getByTestId('api-readiness-summary'),
      '02-overview-summary-cards-REDBC.png',
    )
    await captureApiOpsDiscoverabilityLocatorScreenshot(
      page.locator('.alerts-card'),
      '03-overview-alerts-table-REDBC.png',
    )
    await captureApiOpsDiscoverabilityLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04-brand-header-REDBC.png',
    )

    // --- Hub External access tab + warnings (REDBC) ---
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.locator('.api-access-layout')).toBeVisible()

    const apiAccessTab = page.locator('.workspace-tab-shell').getByRole('tab', {
      name: /external access/i,
    })
    await expect(apiAccessTab).toBeVisible()
    await expect(apiAccessTab).toHaveAttribute('aria-selected', 'true')

    const accessWarning = page.getByTestId('ad-groups-not-configured-warning')
    await expect(accessWarning).toBeVisible()
    await expect(page.getByTestId('published-vs-callable-hint')).toBeVisible()

    await captureApiOpsDiscoverabilityScreenshot(page, '05-hub-external-access-warnings-REDBC.png')
    await captureApiOpsDiscoverabilityLocatorScreenshot(
      accessWarning,
      '06-hub-ad-groups-warning-REDBC.png',
    )
    await captureApiOpsDiscoverabilityLocatorScreenshot(
      page.getByTestId('published-vs-callable-hint'),
      '06b-hub-published-vs-callable-hint-REDBC.png',
    )

    // --- Publish readiness gate warning (REDBC) ---
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await page
      .locator('.approval-sub-tabs')
      .getByRole('tab', { name: /publish readiness/i })
      .click()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const gateWarning = page.getByTestId('publish-gate-ad-groups-warning')
    await expect(gateWarning).toBeVisible({ timeout: 30_000 })
    await captureApiOpsDiscoverabilityScreenshot(page, '07-publish-gate-ad-groups-warning-REDBC.png')
    await captureApiOpsDiscoverabilityLocatorScreenshot(
      gateWarning,
      '08-publish-gate-warning-crop-REDBC.png',
    )

    // --- Overview GREENBC ---
    await page.goto('/api/policies')
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const skipTourGreen = page.getByTestId('onboarding-tour-skip')
    if (await skipTourGreen.isVisible().catch(() => false)) {
      await skipTourGreen.click()
    }
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await page.goto('/api/policies')
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await captureApiOpsDiscoverabilityScreenshot(page, '09-overview-summary-alerts-GREENBC.png')
    await captureApiOpsDiscoverabilityLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '10-brand-header-GREENBC.png',
    )

    // --- Hub External access GREENBC ---
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByTestId('ad-groups-not-configured-warning')).toBeVisible()
    await captureApiOpsDiscoverabilityScreenshot(page, '11-hub-external-access-warnings-GREENBC.png')
  })
})
