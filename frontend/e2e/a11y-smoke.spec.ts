import { expect, test } from '@playwright/test'

test.describe('management shell accessibility smoke', () => {
  test('login page exposes primary heading and form controls', async ({ page }) => {
    await page.goto('/login')

    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible()
    await expect(page.getByPlaceholder('10000001')).toBeVisible()
    await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible()
  })
})
