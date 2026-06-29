import { expect, type Page } from '@playwright/test'

import { managementNav } from './nav'

export const DEMO_MASTER_NAME = 'Demo Retail Letterhead'
export const DEMO_TEMPLATE_EXTERNAL_ID = 'DEMO-RETAIL-LETTER'
export const DEMO_GROUP_CODE = 'RETAIL'

export const E2E_ADMIN = {
  username: '10000001',
  password: 'ChangeMe123!',
}

export const E2E_GROUP_ADMIN = {
  username: '10000002',
  password: 'ChangeMe123!',
}

export const E2E_TEMPLATE_AUTHOR = {
  username: '10000003',
  password: 'ChangeMe123!',
}

export const E2E_MASTER_DESIGNER = {
  username: '10000005',
  password: 'ChangeMe123!',
}

export const E2E_TEMPLATE_TESTER = {
  username: '10000006',
  password: 'ChangeMe123!',
}

export const E2E_TEMPLATE_APPROVER = {
  username: '10000007',
  password: 'ChangeMe123!',
}

export const E2E_AUDIT_ADMIN = {
  username: '10000004',
  password: 'ChangeMe123!',
}

export async function loginAs(page: Page, credentials: { username: string; password: string }) {
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await expect(page.getByPlaceholder('10000001')).toBeVisible()
  await page.getByPlaceholder('10000001').fill(credentials.username)
  await page.locator('input[type="password"]').fill(credentials.password)
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
  await expect(page).not.toHaveURL(/\/forbidden/)
  await expect(managementNav(page)).toBeVisible()
}

export async function loginAsGlobalAdmin(page: Page) {
  await loginAs(page, E2E_ADMIN)
}
