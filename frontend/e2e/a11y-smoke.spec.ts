import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

import {
  E2E_ADMIN,
  E2E_MASTER_DESIGNER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import { prepareTemplatePendingSubmitReady } from './helpers/submit-approval-gate-api'
import { openContentModulesList } from './helpers/ui'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

/**
 * LR-C12 acceptance: zero *critical* axe violations on covered views.
 * Brand/Element Plus color-contrast often lands as `serious` — out of scope for this smoke.
 */
async function expectNoCriticalAxeViolations(
  page: import('@playwright/test').Page,
  label: string,
) {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

test.describe('management shell accessibility smoke', () => {
  test('login page exposes primary heading and form controls', async ({ page }) => {
    await page.goto('/login')

    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible()
    await expect(page.getByPlaceholder('10000001')).toBeVisible()
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible()
  })

  test('content modules list exposes primary h1 after login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openContentModulesList(page)

    await expect(page.getByRole('heading', { level: 1, name: /^standard clauses$/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /new content module/i })).toBeVisible()
  })

  test('tester dashboard task hub exposes primary h1 after login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')

    // Page title stays "My tasks"; queue deep-link selects the testing tab + tasks section.
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /waiting on my testing/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page.locator('#tasks-section')).toBeVisible()
  })

  test('templates list exposes primary h1 after login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/templates')

    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
  })

  test('System settings Reminder timing exposes heading for global admin', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    await expect(page.locator('.timeout-config-card')).toHaveCount(0)

    await page.goto('/system/settings/reminder-timing')
    await expect(page.getByRole('heading', { level: 1, name: /reminder timing/i })).toBeVisible()
    await expect(
      page
        .locator('.timeout-config-card')
        .getByRole('heading', { name: /reminder timing/i }),
    ).toBeVisible()
  })

  test('template lifecycle submit gate exposes checklist heading after author login', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    const template = await prepareTemplatePendingSubmitReady(request)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(`/templates/${template.templateId}?tab=lifecycle`)

    // Package `?tab=lifecycle` may open the approval workspace surface; assert the gate heading.
    await expect(
      page.getByRole('heading', { name: /^submission readiness checks$/i }),
    ).toBeVisible({ timeout: 30_000 })
  })

  test('template testing workspace exposes primary h2 after author login', async ({ page, request }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    // Avoid FOL seed dependency — use the same submit-ready fixture as the lifecycle smoke.
    const template = await prepareTemplatePendingSubmitReady(request)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(`/templates/${template.templateId}`)
    await page
      .locator('.version-lines-card')
      .getByRole('button', { name: /view detail/i })
      .first()
      .click()
    await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
    await page.locator('.workspace-tab-shell').getByRole('tab', { name: /^template testing$/i }).click()
    await expect(page.getByRole('heading', { level: 2, name: /^template testing$/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.locator('.test-data-set-panel')).toBeVisible()
  })

  // LR-C12: axe coverage for ≥2 additional views (incl. table-heavy masters catalog)
  test('masters list (table-heavy) has zero critical axe violations', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_MASTER_DESIGNER)
    await page.goto('/masters')
    await expect(page.getByRole('heading', { name: /letterhead templates/i })).toBeVisible()
    await expect(page.getByText(/unable to load/i)).not.toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expectNoCriticalAxeViolations(page, 'masters list')
  })

  test('dashboard has zero critical axe violations after author login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expectNoCriticalAxeViolations(page, 'dashboard')
  })
})
