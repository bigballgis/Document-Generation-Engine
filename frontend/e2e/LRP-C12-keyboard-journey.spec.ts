import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

/**
 * LR-C12 keyboard journey:
 * login → shell skip-link → catalog table row → Enter opens detail (same as click).
 */
test.describe('LRP-C12 keyboard a11y journey', () => {
  test('skip-link then Enter on catalog row opens template detail', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/templates')
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)

    const skipLink = page.getByRole('link', { name: /skip to main content/i })
    await expect(skipLink).toBeAttached()

    // First Tab from document body should land on the skip-link (first focusable in shell).
    await page.locator('body').focus()
    await page.keyboard.press('Tab')
    await expect(skipLink).toBeFocused()

    await page.keyboard.press('Enter')
    await expect(page.locator('#main-content')).toBeFocused()

    const row = page.locator('tbody tr.app-data-table__activatable-row').first()
    await expect(row).toBeVisible({ timeout: 30_000 })
    await expect(row).toHaveAttribute('tabindex', '0')

    await row.focus()
    await expect(row).toBeFocused()
    await page.keyboard.press('Enter')

    await expect(page).toHaveURL(/\/templates\/[^/?]+/, { timeout: 15_000 })
    await expect(page.getByText(/unable to load/i)).not.toBeVisible()
  })
})
