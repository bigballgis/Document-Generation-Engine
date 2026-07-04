import { expect, test } from '@playwright/test'

import {
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

test.describe('SOR-T03 identity and forbidden journey gaps', () => {
  test('forbidden page shows guidance, reference affordance, and home action', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/audit')

    await expect(page).toHaveURL(/\/forbidden/)
    await expect(page.getByRole('heading', { name: /access denied/i })).toBeVisible()
    await expect(page.getByText(/you do not have permission to view this page/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /back to home/i })).toBeVisible()
    await expect(page.getByText(/reference/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /copy reference/i })).toBeVisible()
  })

  test('global admin can open user management and create-user dialog', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/entitlement/users')

    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /create user/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /employee id/i })).toBeVisible()
    await page.getByRole('button', { name: /create user/i }).click()
    await expect(page.getByRole('dialog', { name: /create user/i })).toBeVisible()
  })

  test('group admin can open group management within authorized scope', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/entitlement/groups')

    await expect(page.getByRole('heading', { name: /group management/i })).toBeVisible()
    await expect(page.getByText(/unable to load groups/i)).not.toBeVisible()
  })

  test('global admin can open reset-password dialog from user row actions', async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/entitlement/users')
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible()

    const firstRow = page.locator('.el-table__body-wrapper tbody tr').first()
    await expect(firstRow).toBeVisible()
    await firstRow.getByRole('button', { name: /^more$/i }).click()
    await page.getByRole('menuitem', { name: /reset password/i }).click()
    await expect(page.getByRole('dialog', { name: /reset password/i })).toBeVisible()
    await expect(page.getByLabel(/new password/i)).toBeVisible()
  })
})
