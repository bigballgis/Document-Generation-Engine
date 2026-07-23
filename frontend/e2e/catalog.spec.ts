/**
 * Catalog smoke against keep-set Live demos (TM #164).
 * DEMO-RETAIL-LETTER / Demo Retail Letterhead were purged — use CORP-FOL-OFFER.
 */
import { expect, test } from '@playwright/test'
import {
  FOL_GROUP_CODE,
  FOL_MASTER_NAME,
  FOL_TEMPLATE_EXTERNAL_ID,
  loginAsGlobalAdmin,
} from './helpers/auth'

test.describe('document catalogs (demo keep-set)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsGlobalAdmin(page)
  })

  test('master catalog loads without error and shows keep-set FOL master', async ({ page }) => {
    await page.goto('/masters')

    await expect(page.getByText(/unable to load letterheads/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { level: 1, name: /^letterhead templates$/i })).toBeVisible()
    await expect(page.locator('.el-table').getByText(FOL_MASTER_NAME).first()).toBeVisible()
    await expect(page.locator('.el-table').getByText(FOL_GROUP_CODE, { exact: true }).first()).toBeVisible()
  })

  test('template catalog loads and shows keep-set FOL template', async ({ page }) => {
    await page.goto('/templates')

    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { level: 1, name: /^templates$/i })).toBeVisible()
    await expect(page.locator('.el-table').getByText(FOL_TEMPLATE_EXTERNAL_ID).first()).toBeVisible()
  })
})
