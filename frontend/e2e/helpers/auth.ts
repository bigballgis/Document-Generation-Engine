import { expect, type Page } from '@playwright/test'

import { managementNav } from './nav'

export const DEMO_MASTER_NAME = 'Demo Retail Letterhead'
export const DEMO_TEMPLATE_EXTERNAL_ID = 'DEMO-RETAIL-LETTER'
export const DEMO_GROUP_CODE = 'RETAIL'

export const FOL_MASTER_NAME = 'Meridian Wholesale FOL Master'
export const FOL_TEMPLATE_EXTERNAL_ID = 'CORP-FOL-OFFER'
export const FOL_CATALOG_MARKER = 'fol-exec-demo-v3'
export const FOL_GROUP_CODE = 'CORP'
export const FOL_EXPECTED_ANCHOR_COUNT = 40
export const FOL_CLAUSE_CODES = [
  'MOD-FOL-SEC-01',
  'MOD-FOL-SEC-02',
  'MOD-FOL-SEC-03',
  'MOD-FOL-SEC-04',
  'MOD-FOL-SEC-05',
  'MOD-FOL-SEC-06',
  'MOD-FOL-SEC-07',
  'MOD-FOL-SEC-08',
  'MOD-FOL-SEC-09',
  'MOD-FOL-SEC-10',
  'MOD-FOL-SEC-11',
  'MOD-FOL-SEC-12',
  'MOD-FOL-SEC-13',
  'MOD-FOL-SEC-14',
  'MOD-FOL-SEC-15',
  'MOD-FOL-SEC-16',
  'MOD-FOL-SEC-17',
  'MOD-FOL-SEC-18',
  'MOD-FOL-SEC-19',
  'MOD-FOL-SEC-20',
  'MOD-FOL-SEC-21',
  'MOD-FOL-SEC-22',
  'MOD-FOL-SEC-23',
  'MOD-FOL-SEC-24',
  'MOD-FOL-SEC-25',
  'MOD-FOL-SEC-26',
  'MOD-FOL-SEC-27',
  'MOD-FOL-SEC-28',
  'MOD-FOL-SEC-29',
  'MOD-FOL-SEC-30',
  'MOD-FOL-SCH-01',
  'MOD-FOL-SCH-02',
  'MOD-FOL-SCH-03',
  'MOD-FOL-SCH-04',
  'MOD-FOL-SCH-05',
  'MOD-FOL-SCH-06',
] as const

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

/** CORP group scope only — for cross-group isolation E2E (BDD S6). */
export const E2E_CORP_TEMPLATE_AUTHOR = {
  username: '10000008',
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

export async function loginAsAuditAdmin(page: Page) {
  await loginAs(page, E2E_AUDIT_ADMIN)
}
