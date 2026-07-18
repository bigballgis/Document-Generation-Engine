/**
 * PRR-D01c / #136 UIUX evidence — Dashboard Overview after summary API
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/prod-dashboard-summary-api.md (BDD-PRR-D01C / D01C-S4)
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  capturePrrD01cLocatorScreenshot,
  capturePrrD01cScreenshot,
  ensurePrrD01cEvidenceDirs,
  PRR_D01C_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
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

async function expectNoCriticalAxeViolations(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

async function openDashboardOverview(page: Page): Promise<void> {
  await page.goto('/dashboard')
  await dismissOnboardingTourIfPresent(page)
  await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible({
    timeout: 30_000,
  })
  const overviewTab = page.getByRole('tab', { name: /^overview$/i })
  if (await overviewTab.isVisible().catch(() => false)) {
    await overviewTab.click()
    await expect(overviewTab).toHaveAttribute('aria-selected', 'true')
  }
  const stats = page.locator('.dashboard-stats')
  await expect(stats).toBeVisible({ timeout: 30_000 })
  await expect(stats.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
  await expect(page.getByText(/unable to load dashboard summary/i)).not.toBeVisible()
  await expect(
    page.getByRole('heading', { level: 2, name: /catalog & workflow snapshot/i }),
  ).toBeVisible()
}

test.describe('PRR-D01C Dashboard Overview summary UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensurePrrD01cEvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('01–02 dual-brand Overview stats cards after summary API', async ({ page }) => {
    await page.setViewportSize(PRR_D01C_VIEWPORT)

    const summaryResponses: string[] = []
    page.on('response', (response) => {
      if (response.url().includes('/api/management/v1/dashboard/summary') && response.ok()) {
        summaryResponses.push(response.url())
      }
    })

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openDashboardOverview(page)
    expect(
      summaryResponses.length,
      'GET /dashboard/summary should succeed on Overview',
    ).toBeGreaterThan(0)

    await expect(page.locator('.stats-grid .stat-card').first()).toBeVisible()
    await expect(page.locator('.stat-count').first()).toBeVisible()
    await expect
      .poll(async () =>
        page.locator('.stat-count').first().evaluate((el) => getComputedStyle(el).color),
      )
      .toBe('rgb(219, 0, 17)')

    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'PRR-D01C Overview REDBC')

    await capturePrrD01cScreenshot(page, '01-overview-summary-redbc-1920x1080.png')
    await capturePrrD01cLocatorScreenshot(
      page.locator('.dashboard-stats'),
      '01b-stats-section-crop-redbc-1920x1080.png',
    )
    await capturePrrD01cLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )

    const redLogo = page
      .locator('.shell-header .header-brand img, .shell-header .header-brand svg')
      .first()
    await expect(redLogo).toBeVisible()

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.locator('.dashboard-stats')).toBeVisible()
    await expect(page.locator('.stats-grid .stat-card').first()).toBeVisible()
    await expect
      .poll(async () =>
        page.locator('.stat-count').first().evaluate((el) => getComputedStyle(el).color),
      )
      .toBe('rgb(0, 132, 127)')

    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'PRR-D01C Overview GREENBC')

    await capturePrrD01cScreenshot(page, '02-overview-summary-greenbc-1920x1080.png')
    await capturePrrD01cLocatorScreenshot(
      page.locator('.dashboard-stats'),
      '02b-stats-section-crop-greenbc-1920x1080.png',
    )
    await capturePrrD01cLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02c-brand-header-greenbc-crop.png',
    )

    await expect(page.getByRole('heading', { level: 1, name: /^my tasks$/i })).toBeVisible()
    await expect(
      page.getByRole('heading', { level: 2, name: /catalog & workflow snapshot/i }),
    ).toBeVisible()
    await expect(page.getByText(/authorized groups/i).first()).toBeVisible()
  })
})

