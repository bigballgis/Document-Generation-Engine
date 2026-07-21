import { expect, type Page } from '@playwright/test'

import folCatalogManifest from '../fixtures/fol-catalog-manifest.json' with { type: 'json' }
import { managementNav } from './nav'

export const DEMO_MASTER_NAME = 'Demo Retail Letterhead'
export const DEMO_TEMPLATE_EXTERNAL_ID = 'DEMO-RETAIL-LETTER'
/** Idempotent full-lifecycle demo template (draft → test → approval → publish → API policy). */
export const DEMO_FULL_FLOW_EXTERNAL_ID = 'DEMO-FULL-FLOW-LETTER'
export const DEMO_FULL_FLOW_NAME = 'Demo Full-Flow Retail Letter'
export const DEMO_GROUP_CODE = 'RETAIL'

export const FOL_MASTER_NAME = 'Meridian Wholesale FOL Master'
export const FOL_TEMPLATE_EXTERNAL_ID = 'CORP-FOL-OFFER'
export const FOL_CATALOG_MARKER = folCatalogManifest.catalogMarker
export const FOL_GROUP_CODE = 'CORP'
export const FOL_EXPECTED_ANCHOR_COUNT = folCatalogManifest.expectedAnchorCount
export const FOL_CLAUSE_CODES = folCatalogManifest.clauseCodes as readonly string[]

/** GLOBAL_ADMIN — unchanged by ADR-0070. */
export const E2E_ADMIN = {
  username: '10000001',
  password: 'ChangeMe123!',
}

/** GROUP_ADMIN — unchanged; also use for compliance decide (ex-approver absorbed). */
export const E2E_GROUP_ADMIN = {
  username: '10000002',
  password: 'ChangeMe123!',
}

/**
 * Seed 10000003 — role code DOCUMENT_AUTHOR after V75 (was TEMPLATE_AUTHOR).
 * Constant name kept for existing E2E imports.
 */
export const E2E_TEMPLATE_AUTHOR = {
  username: '10000003',
  password: 'ChangeMe123!',
}

/** Prefer this alias in new specs (ADR-0070). */
export const E2E_DOCUMENT_AUTHOR = E2E_TEMPLATE_AUTHOR

/**
 * Seed 10000005 — role code DOCUMENT_AUTHOR after V75 (was MASTER_DESIGNER).
 * Constant name kept for existing E2E imports.
 */
export const E2E_MASTER_DESIGNER = {
  username: '10000005',
  password: 'ChangeMe123!',
}

/** TEMPLATE_TESTER — retained SoD role (ADR-0070). */
export const E2E_TEMPLATE_TESTER = {
  username: '10000006',
  password: 'ChangeMe123!',
}

/**
 * Seed 10000007 — role code GROUP_ADMIN after V75 (was TEMPLATE_APPROVER).
 * Constant name kept for existing E2E imports.
 */
export const E2E_TEMPLATE_APPROVER = {
  username: '10000007',
  password: 'ChangeMe123!',
}

/** CORP group scope only — DOCUMENT_AUTHOR after V75 (was TEMPLATE_AUTHOR). */
export const E2E_CORP_TEMPLATE_AUTHOR = {
  username: '10000008',
  password: 'ChangeMe123!',
}

/** RETAIL LEGAL_REVIEWER — IBL-E3 / V71 seed (…109). */
export const E2E_LEGAL_REVIEWER = {
  username: '10000009',
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
