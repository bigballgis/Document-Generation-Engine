/**
 * CE-G05 UIUX evidence — template annual review + clause FULL_TEXT / where-used
 * Dual-brand REDBC/GREENBC @1440×900 (Stage 7).
 * BDD surfaces: BDD-CE-G05-010/017/018 visual/UX.
 *
 * Acceptance stack: Docker FE :4173 + API :8080
 *
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/CE-G05-annual-review-fts-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  createApprovedModuleWithBodyPhrase,
  markTemplateAnnualReviewDueToday,
  preparePublishedAnnualReviewTemplate,
} from './helpers/ce-g05-annual-review-api'
import { preparePublishedTemplateReferencingModule } from './helpers/content-modules-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  expectDashboardPartitionHeading,
  openContentModulesList,
  selectElementPlusOption,
} from './helpers/ui'
import {
  captureCeG05LocatorScreenshot,
  captureCeG05Screenshot,
  CE_G05_VIEWPORT,
  ensureCeG05EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const UUID_LIKE =
  /[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/i

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openDashboardTasks(page: Page) {
  await page.goto('/dashboard#tasks-section')
  await dismissOnboardingTourIfPresent(page)
  const tasks = page.locator('#tasks-section')
  await expect(tasks).toBeVisible({ timeout: 30_000 })
  await expect(tasks.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
  return tasks
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

async function assertPrimaryBrandColor(page: Page, brand: 'REDBC' | 'GREENBC'): Promise<void> {
  const expected = brand === 'REDBC' ? '#db0011' : '#00847f'
  const primary = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim().toLowerCase(),
  )
  expect(primary, `expected --brand-primary ${expected} for ${brand}`).toBe(expected)
}

async function expectNoCriticalAxeViolations(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

test.describe('CE-G05 annual review + FTS UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeG05EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Acceptance stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
  })

  test('01–08 dual-brand: annual review queue/overview + FTS/where-used', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_G05_VIEWPORT)

    const annualFixture = await preparePublishedAnnualReviewTemplate(request)
    await markTemplateAnnualReviewDueToday(request, annualFixture.templateId)

    const stamp = Date.now().toString(36).toUpperCase()
    const phrase = `uiux-fts-journey-${stamp}`
    const module = await createApprovedModuleWithBodyPhrase(request, {
      phrase,
      name: `E2E G05 UIUX FTS ${stamp}`,
    })
    const published = await preparePublishedTemplateReferencingModule(request, module, {
      externalIdPrefix: 'E2E-G05-UIUX',
      referenceKey: 'E2E_G05_UIUX_REF',
      name: `E2E G05 UIUX FTS tpl ${stamp}`,
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)

    // --- REDBC: Annual review due partition ---
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    await assertPrimaryBrandColor(page, 'REDBC')

    await openDashboardTasks(page)
    const annualPartition = page.locator('[data-partition-id="template-annual-review"]')
    await expect(annualPartition).toBeVisible({ timeout: 30_000 })
    await expectDashboardPartitionHeading(page, /annual review due/i)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-G05 annual review partition REDBC')

    await annualPartition.getByRole('button', { name: /^Filter Item$/i }).click()
    const filterInput = page.locator('.table-column-filter-popover input:visible')
    await expect(filterInput).toBeVisible()
    await filterInput.fill(annualFixture.name)

    const annualRow = annualPartition
      .locator('.el-table__row.app-data-table__activatable-row')
      .filter({ hasText: annualFixture.name })
      .first()
    await expect(annualRow).toBeVisible({ timeout: 30_000 })

    const partitionText = await annualPartition.innerText()
    expect(partitionText, 'raw UUID must not appear in annual-review partition').not.toMatch(
      UUID_LIKE,
    )

    await captureCeG05Screenshot(page, '01-annual-review-due-partition-redbc-1440x900.png')
    await captureCeG05LocatorScreenshot(
      annualPartition,
      '01b-annual-review-partition-crop-redbc-1440x900.png',
    )
    await captureCeG05LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )
    await captureCeG05LocatorScreenshot(annualRow, '01d-annual-review-row-crop-redbc.png')

    await annualRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(new RegExp(`/templates/${annualFixture.templateId}`), {
      timeout: 30_000,
    })
    await expect(page.getByTestId('template-overview-summary')).toBeVisible({ timeout: 20_000 })
    await expect(page.getByTestId('template-annual-review-due-value')).toBeVisible()
    await expect(page.getByTestId('template-annual-review-complete')).toBeVisible()
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-G05 template overview annual review REDBC')

    await captureCeG05Screenshot(page, '02-template-overview-annual-review-redbc-1440x900.png')
    await captureCeG05LocatorScreenshot(
      page.getByTestId('template-overview-summary'),
      '02b-overview-summary-crop-redbc.png',
    )
    await captureCeG05LocatorScreenshot(
      page.getByTestId('template-annual-review-complete'),
      '02c-complete-review-cta-crop-redbc.png',
    )

    // --- REDBC: Content module FULL_TEXT + where-used ---
    await openContentModulesList(page)
    await expect(page.getByTestId('content-module-search-mode')).toBeVisible()
    await page.getByTestId('content-module-search-mode').click()
    await selectElementPlusOption(page, /full text \(body\)/i)

    const listResponse = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes('/content-modules') &&
        response.url().includes('searchMode=FULL_TEXT') &&
        response.url().includes('search=') &&
        response.ok(),
      { timeout: 30_000 },
    )
    const search = page.getByTestId('catalog-filter-search')
    await search.fill(phrase)
    await listResponse
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const nameLink = page.getByRole('link', { name: module.name }).first()
    await expect(nameLink).toBeVisible({ timeout: 30_000 })

    const catalogText = await page.locator('.app-page-layout, main').first().innerText()
    expect(catalogText, 'raw UUID must not appear as primary catalog text').not.toMatch(UUID_LIKE)

    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-G05 content modules FULL_TEXT REDBC')

    await captureCeG05Screenshot(page, '03-content-modules-full-text-redbc-1440x900.png')
    await captureCeG05LocatorScreenshot(
      page.getByTestId('content-module-search-mode'),
      '03b-search-mode-full-text-crop-redbc.png',
    )

    await nameLink.click()
    await expect(page).toHaveURL(new RegExp(`/content-modules/${module.moduleId}`), {
      timeout: 20_000,
    })
    await page.getByRole('tab', { name: /^where used$/i }).click()
    await expect(page.getByTestId('content-module-where-used')).toBeVisible()
    await expect(page.getByTestId('content-module-where-used-table')).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByText(published.externalId, { exact: false })).toBeVisible()

    const whereUsedText = await page.getByTestId('content-module-where-used').innerText()
    expect(whereUsedText, 'raw UUID must not appear in where-used panel').not.toMatch(UUID_LIKE)
    await expect(
      page.getByTestId('content-module-where-used-table').getByRole('link').first(),
    ).toBeVisible()

    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-G05 where-used tab REDBC')

    await captureCeG05Screenshot(page, '04-where-used-tab-redbc-1440x900.png')
    await captureCeG05LocatorScreenshot(
      page.getByTestId('content-module-where-used'),
      '04b-where-used-panel-crop-redbc.png',
    )

    // --- GREENBC spot-check ---
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')

    const logoSrc = await page
      .locator('.shell-header .header-brand img, .shell-header .brand-logo img')
      .first()
      .getAttribute('src')
    expect(logoSrc ?? '', 'brand logo src should reference greenbc after switch').toMatch(
      /greenbc/i,
    )

    await openDashboardTasks(page)
    const greenPartition = page.locator('[data-partition-id="template-annual-review"]')
    await expect(greenPartition).toBeVisible({ timeout: 30_000 })
    await expectDashboardPartitionHeading(page, /annual review due/i)
    await assertNoViewportOverflow(page)

    await captureCeG05Screenshot(page, '05-annual-review-due-partition-greenbc-1440x900.png')
    await captureCeG05LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '05b-brand-header-greenbc-crop.png',
    )
    await captureCeG05LocatorScreenshot(
      greenPartition,
      '05c-annual-review-partition-crop-greenbc-1440x900.png',
    )

    await openContentModulesList(page)
    await page.getByTestId('content-module-search-mode').click()
    await selectElementPlusOption(page, /full text \(body\)/i)
    const greenListResponse = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes('/content-modules') &&
        response.url().includes('searchMode=FULL_TEXT') &&
        response.url().includes('search=') &&
        response.ok(),
      { timeout: 30_000 },
    )
    await page.getByTestId('catalog-filter-search').fill(phrase)
    await greenListResponse
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByRole('link', { name: module.name }).first()).toBeVisible({
      timeout: 30_000,
    })
    await assertNoViewportOverflow(page)

    await captureCeG05Screenshot(page, '06-content-modules-full-text-greenbc-1440x900.png')

    await page.getByRole('link', { name: module.name }).first().click()
    await expect(page).toHaveURL(new RegExp(`/content-modules/${module.moduleId}`), {
      timeout: 20_000,
    })
    await page.getByRole('tab', { name: /^where used$/i }).click()
    await expect(page.getByTestId('content-module-where-used-table')).toBeVisible({
      timeout: 20_000,
    })
    await assertNoViewportOverflow(page)

    await captureCeG05Screenshot(page, '07-where-used-tab-greenbc-1440x900.png')
    await captureCeG05LocatorScreenshot(
      page.getByTestId('content-module-where-used'),
      '07b-where-used-panel-crop-greenbc.png',
    )

    await page.goto(`/templates/${annualFixture.templateId}?tab=overview`)
    await expect(page.getByTestId('template-annual-review-complete')).toBeVisible({
      timeout: 20_000,
    })
    await assertNoViewportOverflow(page)
    await captureCeG05Screenshot(page, '08-template-overview-annual-review-greenbc-1440x900.png')
    await captureCeG05LocatorScreenshot(
      page.getByTestId('template-annual-review-complete'),
      '08b-complete-review-cta-crop-greenbc.png',
    )
  })
})
