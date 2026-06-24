import { expect, test } from '@playwright/test'
import {
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'

test.describe('role journey smoke (COR-E02)', () => {
  test('global admin login lands on dashboard', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await page.goto('/dashboard')

    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    await expect(page.getByText(/unable to load your task list/i)).not.toBeVisible()
  })

  test('template author is redirected to forbidden for audit console', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/audit')

    await expect(page).toHaveURL(/\/forbidden/)
    await expect(page.getByText(/access denied/i)).toBeVisible()
  })

  test('group admin can open identity user management', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/entitlement/users')

    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible()
    await expect(page.getByText(/unable to load users/i)).not.toBeVisible()
  })

  test('template author can open demo template lifecycle detail', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/templates')

    await expect(page.getByText(DEMO_TEMPLATE_EXTERNAL_ID)).toBeVisible()
    await page.getByRole('row', { name: new RegExp(DEMO_TEMPLATE_EXTERNAL_ID) }).click()
    await expect(page).toHaveURL(/\/templates\/[^/?]+/)

    await page.getByRole('tab', { name: /overview/i }).click()
    await expect(page.getByText(/draft/i).first()).toBeVisible()
    await expect(page.getByRole('heading', { name: /lifecycle actions/i })).toBeVisible()
  })
})
