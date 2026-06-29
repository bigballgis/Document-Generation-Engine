import { expect, test } from '@playwright/test'

import { E2E_ADMIN, E2E_TEMPLATE_AUTHOR, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
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

  test('tester workbench exposes primary h1 after login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/workbench/tester')

    await expect(page.getByRole('heading', { level: 1, name: /tester workbench/i })).toBeVisible()
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
        .getByRole('heading', { name: /collaboration timeout thresholds/i }),
    ).toBeVisible()
  })
})
