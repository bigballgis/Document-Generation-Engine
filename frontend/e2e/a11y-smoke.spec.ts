import { expect, test } from '@playwright/test'

import { E2E_ADMIN, E2E_TEMPLATE_AUTHOR, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import { assertFolCatalogSeeded } from './helpers/fol-api'
import { prepareTemplatePendingSubmitReady } from './helpers/submit-approval-gate-api'
import { openContentModulesList } from './helpers/ui'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

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

    await expect(page.getByRole('heading', { level: 1, name: /^content modules$/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /new content module/i })).toBeVisible()
  })

  test('tester dashboard task hub exposes primary h1 after login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard#tasks-section')

    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.locator('#tasks-section').getByRole('heading', { name: /^my to-dos$/i })).toBeVisible()
  })

  test('templates list exposes primary h1 after login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/templates')

    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
  })

  test('dashboard timeout config panel exposes heading for global admin', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
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

    const lifecyclePanel = page.locator('#template-lifecycle-panel')
    await expect(lifecyclePanel).toBeVisible({ timeout: 30_000 })
    await expect(lifecyclePanel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(
      lifecyclePanel
        .locator('.submit-gate-card')
        .getByRole('heading', { name: /^submission readiness checks$/i }),
    ).toBeVisible()
  })

  test('template testing workspace exposes primary h2 after author login', async ({ page, request }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    const fixture = await assertFolCatalogSeeded(request)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(`/templates/${fixture.templateId}`)
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
})
