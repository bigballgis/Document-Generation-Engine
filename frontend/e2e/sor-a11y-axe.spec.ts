import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

import { E2E_ADMIN, E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { openContentModulesList } from './helpers/ui'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

async function expectNoAxeViolations(page: import('@playwright/test').Page, label: string) {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  expect(results.violations, `${label} axe violations`).toEqual([])
}

test.describe('SOR-T02 axe accessibility depth', () => {
  test('login page passes axe scan', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible()
    await expectNoAxeViolations(page, 'login')
  })

  test('content modules list passes axe scan after author login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openContentModulesList(page)
    await expect(page.getByRole('heading', { level: 1, name: /^content modules$/i })).toBeVisible()
    await expectNoAxeViolations(page, 'content modules list')
  })

  test('templates list passes axe scan after author login', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/templates')
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expectNoAxeViolations(page, 'templates list')
  })

  test('forbidden page passes axe scan for denied author', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/audit')
    await expect(page).toHaveURL(/\/forbidden/)
    await expect(page.getByRole('heading', { name: /access denied/i })).toBeVisible()
    await expectNoAxeViolations(page, 'forbidden')
  })

  test('create-user dialog keeps keyboard focus inside dialog surface', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/entitlement/users')
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible()
    await page.getByRole('button', { name: /create user/i }).click()
    const dialog = page.getByRole('dialog', { name: /create user/i })
    await expect(dialog).toBeVisible()
    await page.keyboard.press('Tab')
    await page.keyboard.press('Tab')
    const focusInsideDialog = await dialog.evaluate((dialogEl) => {
      const active = document.activeElement
      return active instanceof Node && dialogEl.contains(active)
    })
    expect(focusInsideDialog).toBe(true)
  })
})
