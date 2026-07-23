/**
 * PQH N19–N20 / TM #161 — Where-used + MasterImpact EntityLink journeys.
 *
 * BDD SoT: docs/behavior/pqh-n19-n20-entitylink.md
 *   BDD-PQH-N19N20-001…011 — unit-covered (EntityLinkCell / gating); E2E locks journeys
 *   BDD-PQH-N19N20-012 — where-used template + groupCode EntityLink navigation
 *   BDD-PQH-N19N20-013 — MasterImpact EntityLink navigation + fail-closed spot-check
 *   BDD-PQH-N19N20-014 — vetoes / i18n (docs gate; out of E2E)
 *
 * Durability: demo seed template (DEMO-RETAIL-LETTER) + route-mocked where-used / impact
 * payloads (publish-lifecycle fixture seeding is brittle on this stack).
 *
 * Acceptance stack (Stage 5/6): FRONTEND_PORT=4173 + backend :8080
 *
 * Run:
 *   pnpm -C frontend exec playwright test e2e/PQH-N19N20-entitylink.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type APIRequestContext, type Page, type Route } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  DEMO_GROUP_CODE,
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { findTemplateByExternalId } from './helpers/content-modules-api'
import { assertDemoCatalogSeeded, demoMasterDetailPath, E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { dismissOnboardingTourIfPresent } from './helpers/uiux-evidence'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const EVIDENCE_DIR = path.join(__dirname, 'evidence', 'PQH-N19N20')
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const GROUPS_PATH = '/entitlement/groups'
const IDENTITY_ROUTE = 'route.identity-administration'
const TEMPLATE_ROUTE = 'route.template-management'

interface DemoTemplateRef {
  id: string
  externalId: string
  name: string
  groupCode: string
}

interface SeedContext {
  template: DemoTemplateRef
  moduleId: string
  masterHubPath: string
  masterId: string
}

let seed: SeedContext

async function captureEvidence(page: Page, filename: string) {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  await page.screenshot({ path: path.join(EVIDENCE_DIR, filename), fullPage: true })
}

async function resolveRetailModuleId(request: APIRequestContext): Promise<string> {
  const login = await request.post(`${E2E_API_BASE_URL}/auth/login`, { data: E2E_ADMIN })
  expect(login.ok()).toBeTruthy()
  const token = ((await login.json()) as { result: { accessToken: string } }).result.accessToken
  const list = await request.get(
    `${E2E_API_BASE_URL}/content-modules?page=0&size=5&groupCode=${DEMO_GROUP_CODE}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  expect(list.ok()).toBeTruthy()
  const body = (await list.json()) as {
    result: { content: Array<{ moduleId: string }> }
  }
  const moduleId = body.result.content[0]?.moduleId
  if (!moduleId) {
    throw new Error(`No ${DEMO_GROUP_CODE} content module available for where-used host page`)
  }
  return moduleId
}

/** Strip selected visibleRoutes from login + session refresh (fail-closed EntityLink). */
async function loginAsAdminWithoutRoutes(page: Page, routesToStrip: string[]) {
  const strip = new Set(routesToStrip)
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
      (routeKey) => !strip.has(routeKey),
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
    body.result.visibleRoutes = body.result.visibleRoutes.filter((routeKey) => !strip.has(routeKey))
    await route.fulfill({
      status: response.status(),
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(body),
    })
  })
  await loginAs(page, E2E_ADMIN)
}

async function fulfillWhereUsed(
  route: Route,
  moduleId: string,
  row: DemoTemplateRef,
  overrides: Partial<DemoTemplateRef> = {},
) {
  const url = new URL(route.request().url())
  if (
    route.request().method() !== 'GET' ||
    !url.pathname.includes(`/content-modules/${moduleId}/where-used`)
  ) {
    await route.continue()
    return
  }
  const payload = { ...row, ...overrides }
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      metadata: { auditId: 'AUD-E2E-N19N20', traceId: 'trace-e2e-n19n20-wu' },
      result: {
        content: [
          {
            id: payload.id,
            name: payload.name,
            externalId: payload.externalId,
            groupCode: payload.groupCode,
            lifecycleStatus: 'PUBLISHED',
            pinnedSemanticVersion: '1.0.0',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    }),
  })
}

async function fulfillMasterImpact(
  route: Route,
  payload: {
    masterId: string
    templateId: string
    name: string
    externalId: string
  },
) {
  if (route.request().method() !== 'GET' || !route.request().url().includes('/impact-analysis')) {
    await route.continue()
    return
  }
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      metadata: { auditId: 'AUD-E2E-N19N20', traceId: 'trace-e2e-n19n20-mi' },
      result: {
        masterId: payload.masterId,
        referencedTemplateIds: [payload.templateId],
        referencedTemplates: [
          {
            templateId: payload.templateId,
            name: payload.name,
            externalId: payload.externalId,
            lifecycleStatus: 'PUBLISHED',
          },
        ],
        retestRequired: true,
      },
    }),
  })
}

async function openContentModuleWhereUsed(page: Page, moduleId: string) {
  await page.goto(`/content-modules/${moduleId}`)
  await expect(page).not.toHaveURL(/\/forbidden/)
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })
  await page.getByRole('tab', { name: /^where used$/i }).click()
  await expect(page.getByTestId('content-module-where-used')).toBeVisible({ timeout: 20_000 })
  await expect(page.getByTestId('content-module-where-used-table')).toBeVisible({
    timeout: 30_000,
  })
}

test.describe('PQH N19–N20 — Where-used + MasterImpact EntityLink', () => {
  test.describe.configure({ timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    const demo = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
    if (!demo) {
      throw new Error(`Demo template ${DEMO_TEMPLATE_EXTERNAL_ID} required for N19/N20 E2E`)
    }
    // TemplateSummary may omit name — fetch detail for durable label
    const login = await request.post(`${E2E_API_BASE_URL}/auth/login`, { data: E2E_ADMIN })
    const token = ((await login.json()) as { result: { accessToken: string } }).result.accessToken
    const detailRes = await request.get(`${E2E_API_BASE_URL}/templates/${demo.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(detailRes.ok()).toBeTruthy()
    const detail = ((await detailRes.json()) as { result: { name?: string } }).result

    const moduleId = await resolveRetailModuleId(request)
    const masterHubPath = await demoMasterDetailPath(request)

    seed = {
      template: {
        id: demo.id,
        externalId: demo.externalId,
        name: detail.name?.trim() || 'Demo Retail Letter',
        groupCode: demo.groupCode || DEMO_GROUP_CODE,
      },
      moduleId,
      masterHubPath,
      masterId: masterHubPath.replace(/^\/masters\//, ''),
    }
    mkdirSync(EVIDENCE_DIR, { recursive: true })
  })

  test('BDD-PQH-N19N20-012: where-used template + groupCode EntityLink navigate when authorized', async ({
    page,
  }) => {
    await page.route('**/api/management/v1/content-modules/**/where-used**', (route) =>
      fulfillWhereUsed(route, seed.moduleId, seed.template),
    )

    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await openContentModuleWhereUsed(page, seed.moduleId)

    const table = page.getByTestId('content-module-where-used-table')
    const tableRow = table.locator('.el-table__row').filter({ hasText: seed.template.name }).first()
    await expect(tableRow).toBeVisible({ timeout: 20_000 })

    const nameCell = tableRow.getByTestId('where-used-template-name')
    await expect(nameCell).toBeVisible()
    await expect(nameCell).toHaveClass(/entity-link-cell/)
    await expect(nameCell).toContainText(seed.template.name)
    await expect(nameCell.locator('.entity-link-cell__subtitle')).toHaveText(
      seed.template.externalId,
    )
    const templateLink = nameCell.locator('a.entity-link-cell__link')
    await expect(templateLink).toBeVisible()
    await expect(templateLink).toHaveAttribute(
      'href',
      new RegExp(`/templates/${seed.template.id}`),
    )

    const groupCell = tableRow.getByTestId('where-used-group-code')
    await expect(groupCell).toBeVisible()
    await expect(groupCell).toHaveClass(/entity-link-cell/)
    await expect(groupCell).toContainText(seed.template.groupCode)
    const groupLink = groupCell.locator('a.entity-link-cell__link')
    await expect(groupLink).toBeVisible()
    await expect(groupLink).toHaveAttribute(
      'href',
      new RegExp(
        `${GROUPS_PATH.replace(/\//g, '\\/')}\\?q=${encodeURIComponent(seed.template.groupCode)}`,
      ),
    )

    await captureEvidence(page, 'PQH-N19N20-012-where-used-entity-links.png')

    await templateLink.click()
    await expect(page).toHaveURL(new RegExp(`/templates/${seed.template.id}`), { timeout: 20_000 })
    await expect(page.getByText(seed.template.name, { exact: false }).first()).toBeVisible({
      timeout: 20_000,
    })
    await captureEvidence(page, 'PQH-N19N20-012-template-detail-after-name-link.png')

    await openContentModuleWhereUsed(page, seed.moduleId)
    await page
      .getByTestId('content-module-where-used-table')
      .locator('.el-table__row')
      .filter({ hasText: seed.template.name })
      .first()
      .getByTestId('where-used-group-code')
      .locator('a.entity-link-cell__link')
      .click()
    await expect(page).toHaveURL(
      new RegExp(
        `${GROUPS_PATH.replace(/\//g, '\\/')}\\?q=${encodeURIComponent(seed.template.groupCode)}`,
      ),
      { timeout: 15_000 },
    )
    await expect(page.getByRole('heading', { name: /group management/i })).toBeVisible({
      timeout: 20_000,
    })
    await captureEvidence(page, 'PQH-N19N20-012-groups-catalog-after-group-link.png')
  })

  test('BDD-PQH-N19N20-007/012: where-used groupCode plain text without identity route', async ({
    page,
  }) => {
    await page.route('**/api/management/v1/content-modules/**/where-used**', (route) =>
      fulfillWhereUsed(route, seed.moduleId, seed.template),
    )

    // DOCUMENT_AUTHOR: template management yes, identity administration no
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await openContentModuleWhereUsed(page, seed.moduleId)

    const tableRow = page
      .getByTestId('content-module-where-used-table')
      .locator('.el-table__row')
      .filter({ hasText: seed.template.name })
      .first()
    await expect(tableRow).toBeVisible({ timeout: 20_000 })

    const groupCell = tableRow.getByTestId('where-used-group-code')
    await expect(groupCell).toHaveClass(/entity-link-cell/)
    await expect(groupCell.locator('a.entity-link-cell__link')).toHaveCount(0)
    await expect(groupCell.locator('.entity-link-cell__text')).toHaveText(seed.template.groupCode)

    const nameCell = tableRow.getByTestId('where-used-template-name')
    await expect(nameCell.locator('a.entity-link-cell__link')).toBeVisible()

    await captureEvidence(page, 'PQH-N19N20-007-where-used-group-plain-text.png')
  })

  test('BDD-PQH-N19N20-004: where-used template plain text when template management denied', async ({
    page,
  }) => {
    await page.route('**/api/management/v1/content-modules/**/where-used**', (route) =>
      fulfillWhereUsed(route, seed.moduleId, seed.template),
    )

    await loginAsAdminWithoutRoutes(page, [TEMPLATE_ROUTE])
    await dismissOnboardingTourIfPresent(page)
    await openContentModuleWhereUsed(page, seed.moduleId)

    const tableRow = page
      .getByTestId('content-module-where-used-table')
      .locator('.el-table__row')
      .filter({ hasText: seed.template.name })
      .first()
    await expect(tableRow).toBeVisible({ timeout: 20_000 })

    const nameCell = tableRow.getByTestId('where-used-template-name')
    await expect(nameCell).toContainText(seed.template.name)
    await expect(nameCell.locator('a.entity-link-cell__link')).toHaveCount(0)
    await expect(nameCell.locator('.entity-link-cell__text')).toBeVisible()

    const groupCell = tableRow.getByTestId('where-used-group-code')
    await expect(groupCell.locator('a.entity-link-cell__link')).toBeVisible()

    await captureEvidence(page, 'PQH-N19N20-004-where-used-template-plain-text.png')
  })

  test('BDD-PQH-N19N20-007: where-used groupCode wildcard never links', async ({ page }) => {
    await page.route('**/api/management/v1/content-modules/**/where-used**', (route) =>
      fulfillWhereUsed(route, seed.moduleId, seed.template, { groupCode: '*' }),
    )

    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await openContentModuleWhereUsed(page, seed.moduleId)

    const groupCell = page
      .getByTestId('content-module-where-used-table')
      .getByTestId('where-used-group-code')
      .first()
    await expect(groupCell.locator('.entity-link-cell__text')).toHaveText('*')
    await expect(groupCell.locator('a.entity-link-cell__link')).toHaveCount(0)

    await captureEvidence(page, 'PQH-N19N20-007-where-used-group-wildcard-plain.png')
  })

  test('BDD-PQH-N19N20-013: MasterImpact EntityLink navigates when authorized', async ({ page }) => {
    const impactLabel = seed.template.name

    await page.route('**/api/management/v1/masters/**/impact-analysis**', (route) =>
      fulfillMasterImpact(route, {
        masterId: seed.masterId,
        templateId: seed.template.id,
        name: impactLabel,
        externalId: seed.template.externalId,
      }),
    )

    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await page.goto(seed.masterHubPath)
    await expect(page).not.toHaveURL(/\/forbidden/)

    const panel = page.getByTestId('master-impact-panel')
    await expect(panel).toBeVisible({ timeout: 30_000 })
    await expect(panel.getByTestId('master-impact-template-list')).toBeVisible({ timeout: 20_000 })

    const cell = panel.getByTestId('master-impact-template-cell').first()
    await expect(cell).toBeVisible()
    await expect(cell).toHaveClass(/entity-link-cell/)
    await expect(cell).toContainText(impactLabel)
    await expect(cell.locator('.entity-link-cell__subtitle')).toHaveText(seed.template.externalId)

    const link = cell.locator('a.entity-link-cell__link')
    await expect(link).toBeVisible()
    await expect(link).toHaveAttribute('href', new RegExp(`/templates/${seed.template.id}`))
    await expect(panel.locator('li a.entity-link-cell__link')).toHaveCount(1)
    await expect(panel.locator('li > a')).toHaveCount(0)

    await captureEvidence(page, 'PQH-N19N20-013-master-impact-entity-link.png')

    await link.click()
    await expect(page).toHaveURL(new RegExp(`/templates/${seed.template.id}`), { timeout: 20_000 })
    await captureEvidence(page, 'PQH-N19N20-013-template-detail-after-impact-link.png')
  })

  test('BDD-PQH-N19N20-010/013: MasterImpact plain text when template management denied', async ({
    page,
  }) => {
    await page.route('**/api/management/v1/masters/**/impact-analysis**', (route) =>
      fulfillMasterImpact(route, {
        masterId: seed.masterId,
        templateId: seed.template.id,
        name: seed.template.name,
        externalId: seed.template.externalId,
      }),
    )

    await loginAsAdminWithoutRoutes(page, [TEMPLATE_ROUTE, IDENTITY_ROUTE])
    await dismissOnboardingTourIfPresent(page)
    await page.goto(seed.masterHubPath)
    await expect(page).not.toHaveURL(/\/forbidden/)

    const panel = page.getByTestId('master-impact-panel')
    await expect(panel.getByTestId('master-impact-template-list')).toBeVisible({ timeout: 20_000 })

    const cell = panel.getByTestId('master-impact-template-cell').first()
    await expect(cell).toContainText(seed.template.name)
    await expect(cell.locator('a.entity-link-cell__link')).toHaveCount(0)
    await expect(cell.locator('.entity-link-cell__text')).toBeVisible()

    await captureEvidence(page, 'PQH-N19N20-013-master-impact-fail-closed.png')
  })

  test('BDD-PQH-N19N20-005/006: where-used groupCode EntityLink targets groups catalog (RETAIL)', async ({
    page,
  }) => {
    expect(seed.template.groupCode).toBe(DEMO_GROUP_CODE)

    await page.route('**/api/management/v1/content-modules/**/where-used**', (route) =>
      fulfillWhereUsed(route, seed.moduleId, seed.template),
    )

    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)
    await openContentModuleWhereUsed(page, seed.moduleId)

    const groupLink = page
      .getByTestId('content-module-where-used-table')
      .getByTestId('where-used-group-code')
      .locator('a.entity-link-cell__link')
      .filter({ hasText: DEMO_GROUP_CODE })
      .first()
    await expect(groupLink).toBeVisible({ timeout: 20_000 })
    await expect(groupLink).toHaveAttribute('href', new RegExp(`q=${DEMO_GROUP_CODE}`))

    await captureEvidence(page, 'PQH-N19N20-005-006-where-used-group-retail-link.png')
  })
})
