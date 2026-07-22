/**
 * SYS-NORM N18 + P-Q1 / TM #157 + #158 — Legal-hold Created by EntityLink + DOCUMENT_AUTHOR L1
 *
 * BDD SoT: docs/behavior/sys-norm-n18-role-l1.md
 *   BDD-N18-L1-001…007 — Legal holds Created by EntityLink / gating / navigation
 *   BDD-N18-L1-008…010 / 012 — DOCUMENT_AUTHOR L1 EN/ZH (no interim); role ID unchanged
 *   BDD-N18-L1-011 — vetoes held (docs/plan gate; out of E2E)
 *
 * Acceptance stack (Stage 5/6): FRONTEND_PORT=4173 + backend :8080
 *
 * Run:
 *   pnpm -C frontend exec playwright test e2e/SYS-NORM-N18-role-l1.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type APIRequestContext, type Page, type Route } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  E2E_ADMIN,
  E2E_DOCUMENT_AUTHOR,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import {
  createLegalHoldViaApi,
  ensureLegalHoldTemplateFixture,
  releaseLegalHoldViaApi,
  type LegalHoldView,
} from './helpers/legal-holds-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { dismissOnboardingTourIfPresent, switchLocale } from './helpers/uiux-evidence'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const EVIDENCE_DIR = path.join(__dirname, 'evidence', 'SYS-NORM-N18')
const LEGAL_HOLDS_PATH = '/governance/legal-holds'
const USERS_PATH = '/entitlement/users'
const IDENTITY_ROUTE = 'route.identity-administration'
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function captureEvidence(page: Page, filename: string) {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  await page.screenshot({ path: path.join(EVIDENCE_DIR, filename), fullPage: true })
}

async function openLegalHoldsPage(page: Page) {
  await page.goto(LEGAL_HOLDS_PATH)
  await expect(page).not.toHaveURL(/\/forbidden/)
  await expect(page.getByRole('heading', { level: 1, name: /^legal holds$/i })).toBeVisible({
    timeout: 20_000,
  })
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  await expect(page.getByText(/unable to load legal holds/i)).toHaveCount(0)
}

async function seedTemplateWindowHold(
  request: APIRequestContext,
  reason: string,
): Promise<LegalHoldView> {
  const template = await ensureLegalHoldTemplateFixture(request)
  const from = new Date().toISOString()
  return createLegalHoldViaApi(request, {
    scopeType: 'TEMPLATE_WINDOW',
    reason,
    templateId: template.templateId,
    templateExternalId: template.externalId,
    effectiveFrom: from,
  })
}

async function fulfillLegalHoldsList(route: Route, holds: LegalHoldView[]) {
  const requestUrl = new URL(route.request().url())
  if (route.request().method() !== 'GET' || requestUrl.pathname.includes('/release')) {
    await route.continue()
    return
  }
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      metadata: { auditId: 'AUD-E2E-N18', traceId: 'trace-e2e-n18' },
      result: {
        content: holds,
        page: 0,
        size: holds.length || 20,
        totalElements: holds.length,
        totalPages: holds.length > 0 ? 1 : 0,
      },
    }),
  })
}

function sampleHold(overrides: Partial<LegalHoldView> = {}): LegalHoldView {
  return {
    id: 'e2e-n18-hold-1',
    holdExternalId: 'E2E-N18-HOLD-1',
    scopeType: 'TEMPLATE_WINDOW',
    status: 'ACTIVE',
    reason: 'E2E N18 mock hold',
    templateId: 'tpl-1',
    templateExternalId: 'E2E-N18-TPL',
    effectiveFrom: new Date().toISOString(),
    effectiveTo: null,
    invocationExternalIds: [],
    invocationCount: 0,
    createdAt: new Date().toISOString(),
    createdByUsername: '10000001',
    createdByDisplayName: null,
    releasedAt: null,
    releasedByUsername: null,
    ...overrides,
  }
}

/** Strip identity-administration from login (+ session refresh) so Created by is plain text. */
async function loginAsAdminWithoutIdentityRoute(page: Page) {
  await page.route('**/api/management/v1/auth/login', async (route) => {
    const response = await route.fetch()
    const body = (await response.json()) as {
      metadata: unknown
      result: {
        accessToken: string
        session: { visibleRoutes: string[] }
      }
    }
    body.result.session.visibleRoutes = body.result.session.visibleRoutes.filter(
      (routeKey) => routeKey !== IDENTITY_ROUTE,
    )
    await route.fulfill({
      status: response.status(),
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(body),
    })
  })
  await page.route('**/api/management/v1/auth/session', async (route) => {
    const response = await route.fetch()
    const body = (await response.json()) as {
      metadata: unknown
      result: { visibleRoutes: string[] }
    }
    body.result.visibleRoutes = body.result.visibleRoutes.filter(
      (routeKey) => routeKey !== IDENTITY_ROUTE,
    )
    await route.fulfill({
      status: response.status(),
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(body),
    })
  })
  await loginAs(page, E2E_ADMIN)
}

const CREATE_USER_BUTTON = /^(create user|创建用户)$/i
const CREATE_USER_DIALOG = /^(create user|创建用户)$/i

async function openCreateUserRoleDropdown(page: Page) {
  const headerCreate = page.locator('.panel-header').getByRole('button', { name: CREATE_USER_BUTTON })
  if (await headerCreate.isVisible().catch(() => false)) {
    await headerCreate.click()
  } else {
    await page.getByRole('button', { name: CREATE_USER_BUTTON }).first().click()
  }
  const dialog = page.getByRole('dialog', { name: CREATE_USER_DIALOG })
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  const rolesSelect = dialog
    .locator('.el-form-item')
    .filter({ hasText: /roles|角色/i })
    .locator('.el-select')
    .first()
  await rolesSelect.click()
  const dropdown = page.locator('.el-select-dropdown:visible')
  await expect(dropdown.locator('.el-select-dropdown__item').first()).toBeVisible({
    timeout: 10_000,
  })
  return { dialog, dropdown }
}

test.describe('SYS-NORM N18 — Legal-hold EntityLink + DOCUMENT_AUTHOR L1', () => {
  test.describe.configure({ timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    mkdirSync(EVIDENCE_DIR, { recursive: true })
  })

  test('BDD-N18-L1-001/003/004/007: Created by EntityLink links to users when identity permitted', async ({
    page,
    request,
  }) => {
    const reason = `E2E N18 EntityLink ${Date.now()}`
    const hold = await seedTemplateWindowHold(request, reason)

    try {
      await loginAsGlobalAdmin(page)
      await dismissOnboardingTourIfPresent(page)
      await openLegalHoldsPage(page)

      const table = page.getByTestId('legal-hold-table')
      await expect(table).toBeVisible({ timeout: 20_000 })
      const row = table.locator('.el-table__row').filter({ hasText: reason }).first()
      await expect(row).toBeVisible({ timeout: 20_000 })

      const createdBy = row.getByTestId('legal-hold-created-by')
      await expect(createdBy).toBeVisible()
      await expect(createdBy).toHaveClass(/entity-link-cell/)

      // API enrichment optional — without createdByDisplayName label falls back to username (003).
      const expectedLabel = hold.createdByDisplayName?.trim() || hold.createdByUsername
      await expect(createdBy).toContainText(expectedLabel)

      const link = createdBy.locator('a.entity-link-cell__link')
      await expect(link).toBeVisible()
      await expect(link).toHaveAttribute('href', new RegExp(`${USERS_PATH}\\?q=${hold.createdByUsername}`))

      await captureEvidence(page, 'N18-L1-001-004-created-by-entity-link.png')

      await link.click()
      await expect(page).toHaveURL(new RegExp(`${USERS_PATH.replace(/\//g, '\\/')}\\?q=${hold.createdByUsername}`), {
        timeout: 15_000,
      })
      await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
        timeout: 20_000,
      })
      // Actor remains discoverable on Users surface (username present in catalog).
      await expect(page.getByText(hold.createdByUsername, { exact: true }).first()).toBeVisible({
        timeout: 20_000,
      })

      await captureEvidence(page, 'N18-L1-007-users-catalog-after-created-by.png')
    } finally {
      await releaseLegalHoldViaApi(request, hold.id).catch(() => undefined)
    }
  })

  test('BDD-N18-L1-002: display name preferred as Created by label', async ({ page }) => {
    await page.route('**/api/management/v1/legal-holds**', (route) =>
      fulfillLegalHoldsList(
        route,
        [
          sampleHold({
            createdByUsername: '10000001',
            createdByDisplayName: 'Alice Author',
            reason: 'E2E N18 display-name',
          }),
        ],
      ),
    )

    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await openLegalHoldsPage(page)

    const createdBy = page.getByTestId('legal-hold-created-by').first()
    await expect(createdBy).toBeVisible()
    await expect(createdBy).toContainText('Alice Author')
    await expect(createdBy).not.toContainText('10000001')
    await expect(createdBy.locator('a.entity-link-cell__link')).toBeVisible()

    await captureEvidence(page, 'N18-L1-002-display-name-label.png')
  })

  test('BDD-N18-L1-005: Created by is plain text when identity administration denied', async ({
    page,
  }) => {
    await page.route('**/api/management/v1/legal-holds**', (route) =>
      fulfillLegalHoldsList(route, [
        sampleHold({
          createdByUsername: '10000001',
          reason: 'E2E N18 fail-closed link',
        }),
      ]),
    )

    await loginAsAdminWithoutIdentityRoute(page)
    await dismissOnboardingTourIfPresent(page)
    await openLegalHoldsPage(page)

    const createdBy = page.getByTestId('legal-hold-created-by').first()
    await expect(createdBy).toBeVisible()
    await expect(createdBy).toContainText('10000001')
    await expect(createdBy.locator('a.entity-link-cell__link')).toHaveCount(0)
    await expect(createdBy.locator('.entity-link-cell__text')).toBeVisible()

    await captureEvidence(page, 'N18-L1-005-created-by-plain-text.png')
  })

  test('BDD-N18-L1-006: empty Created by is em dash and not a link', async ({ page }) => {
    await page.route('**/api/management/v1/legal-holds**', (route) =>
      fulfillLegalHoldsList(route, [
        sampleHold({
          createdByUsername: '   ',
          createdByDisplayName: null,
          reason: 'E2E N18 empty actor',
        }),
      ]),
    )

    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await openLegalHoldsPage(page)

    const createdBy = page.getByTestId('legal-hold-created-by').first()
    await expect(createdBy).toBeVisible()
    await expect(createdBy).toHaveText('—')
    await expect(createdBy.locator('a.entity-link-cell__link')).toHaveCount(0)

    await captureEvidence(page, 'N18-L1-006-empty-actor-em-dash.png')
  })

  test('BDD-N18-L1-008/009/010/012: DOCUMENT_AUTHOR L1 locked (EN/ZH, no interim, role ID)', async ({
    page,
    request,
  }) => {
    const loginResponse = await request.post('http://127.0.0.1:8080/api/management/v1/auth/login', {
      data: E2E_DOCUMENT_AUTHOR,
    })
    expect(loginResponse.ok()).toBeTruthy()
    const sessionBody = (await loginResponse.json()) as {
      result: { session: { roles: string[] } }
    }
    expect(sessionBody.result.session.roles).toEqual(['DOCUMENT_AUTHOR'])

    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await page.goto(USERS_PATH)
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })

    const { dialog, dropdown } = await openCreateUserRoleDropdown(page)
    const enOption = dropdown.getByRole('option', { name: /^document author$/i })
    await expect(enOption).toBeVisible()
    const enText = (await enOption.innerText()).trim()
    expect(enText).toBe('Document author')
    expect(enText.toLowerCase()).not.toContain('interim')
    await expect(dropdown.getByRole('option', { name: /DOCUMENT_AUTHOR/ })).toHaveCount(0)

    await captureEvidence(page, 'N18-L1-008-role-picker-document-author-en.png')
    await page.keyboard.press('Escape')
    await dialog.getByRole('button', { name: /cancel|取消/i }).click()

    await switchLocale(page, 'zh-CN')
    await page.goto(USERS_PATH)
    await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN')
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })

    const zhPicker = await openCreateUserRoleDropdown(page)
    const zhOption = zhPicker.dropdown.getByRole('option', { name: /^文档作者$/ })
    await expect(zhOption).toBeVisible()
    const zhText = (await zhOption.innerText()).trim()
    expect(zhText).toBe('文档作者')
    expect(zhText).not.toContain('interim')
    expect(zhText).not.toContain('（interim）')

    await captureEvidence(page, 'N18-L1-009-role-picker-document-author-zh.png')
    await page.keyboard.press('Escape')
    await zhPicker.dialog.getByRole('button', { name: /cancel|取消/i }).click()
  })
})
