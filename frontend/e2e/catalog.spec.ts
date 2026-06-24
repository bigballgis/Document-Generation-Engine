import { expect, test } from '@playwright/test'
import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  DEMO_TEMPLATE_EXTERNAL_ID,
  loginAsGlobalAdmin,
} from './helpers/auth'

test.describe('version catalog (demo seed)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsGlobalAdmin(page)
  })

  test('master catalog loads without error and shows demo master', async ({ page }) => {
    await page.goto('/masters')

    await expect(page.getByText(/unable to load master documents/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { name: /master version catalog/i })).toBeVisible()
    await expect(page.getByText(DEMO_MASTER_NAME)).toBeVisible()
    await expect(page.getByRole('heading', { name: `Group: ${DEMO_GROUP_CODE}` })).toBeVisible()
  })

  test('template catalog loads and shows demo template', async ({ page }) => {
    await page.goto('/templates')

    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { name: /template version catalog/i })).toBeVisible()
    await expect(page.getByText(DEMO_TEMPLATE_EXTERNAL_ID)).toBeVisible()
  })
})
