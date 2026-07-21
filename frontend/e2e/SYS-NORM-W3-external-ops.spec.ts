/**
 * SYS-NORM Wave 3 / #147 — External services ops
 * (dashboard + invocations + package settings).
 *
 * BDD SoT: docs/behavior/sys-norm-external-ops.md
 *   BDD-SYS-NORM-W3-001…018 (critical journeys below; W3-017/018 docs/process)
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/SYS-NORM-W3-external-ops.spec.ts `
 *     --config playwright.docker.config.ts
 *
 * Coverage map:
 *   W3-001       — dashboard readiness + ops summary cards
 *   W3-002       — alert → package settings (not hub apiAccess)
 *   W3-003       — ops empty/error honesty (empty sample OR cards present)
 *   W3-004/005   — invocations separate page + filters
 *   W3-006/007   — detail summary-only OR honest empty
 *   W3-008/009   — settings complete (no interim banner); L1 panels
 *   W3-010       — unknown panel fail-closed
 *   W3-011       — legacy redirects → settings
 *   W3-012       — nav: overview + invocation records (+ icons)
 *   W3-013       — fail-closed without canManageApiPolicy
 *   W3-014       — GROUP_ADMIN out-of-scope settings denied (FOL/CORP)
 *   W3-015       — published ≠ runtime-callable warning on settings
 *   W3-016       — dashboard/invocations deep-link to settings (no catalog)
 */
import { expect, test, type APIRequestContext, type Locator, type Page } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import {
  ensureDemoFullFlowPublished,
  findTemplateByExternalId,
} from './helpers/content-modules-api'
import { requireFolTemplate } from './helpers/fol-api'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  prepareTemplatePendingRelease,
  type PendingSubmitTemplateFixture,
} from './helpers/submit-approval-gate-api'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const EVIDENCE_DIR = path.join(__dirname, 'evidence', 'SYS-NORM-W3')

const OVERVIEW_NAV = /^external services overview$/i
const INVOCATIONS_NAV = /^invocation records$/i

function settingsUrlRe(templateId: string): RegExp {
  return new RegExp(
    `/api/packages/${templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/settings`,
  )
}

async function expectFluidLayout(page: Page) {
  const layout = page.locator('.app-page-layout').first()
  await expect(layout).toBeVisible({ timeout: 20_000 })
  await expect(layout).toHaveClass(/app-page-layout--fluid/)
  await expect(layout.locator('.app-page-layout__inner')).toHaveCount(0)
}

async function expectNavItemHasIcon(nav: Locator, label: RegExp) {
  const item = nav.getByRole('button', { name: label })
  await expect(item).toBeVisible({ timeout: 20_000 })
  await expect(item.locator('.el-icon')).toHaveCount(1)
  await expect(item.locator('.el-icon')).toBeVisible()
}

async function openExternalServicesOverview(page: Page) {
  await page.goto('/dashboard')
  await managementNav(page).getByRole('button', { name: OVERVIEW_NAV }).click()
  await expect(page).toHaveURL(/\/api\/policies\/?$/, { timeout: 20_000 })
  await expect(page.locator('.page-header h1')).toHaveText(
    /external services overview|对外服务概览/i,
  )
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })
}

async function openInvocationRecordsViaNav(page: Page) {
  await page.goto('/dashboard')
  await managementNav(page).getByRole('button', { name: INVOCATIONS_NAV }).click()
  await expect(page).toHaveURL(/\/api\/invocations/, { timeout: 20_000 })
  await expect(page.locator('.page-header h1')).toHaveText(/invocation records|调用记录/i)
}

async function requireDemoRetailTemplate(
  request: APIRequestContext,
): Promise<{ templateId: string }> {
  const template = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!template) {
    throw new Error(
      `Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" was not found. Ensure DOCGEN_SEED_DEMO_CATALOG=true.`,
    )
  }
  return { templateId: template.id }
}

async function captureEvidence(page: Page, filename: string) {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  await page.screenshot({ path: path.join(EVIDENCE_DIR, filename), fullPage: true })
}

test.describe('SYS-NORM Wave 3 — External services ops functional journeys', () => {
  test.describe.configure({ timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      skipMessage:
        'Stack required (:4173 + :8080). Start with .\\scripts\\docker-deploy-queue.ps1',
    })
    mkdirSync(EVIDENCE_DIR, { recursive: true })
  })

  test('BDD-SYS-NORM-W3-001/003/016: dashboard readiness + ops cards; not a catalog', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await openExternalServicesOverview(page)
    await expectFluidLayout(page)

    const readiness = page.getByTestId('api-readiness-summary')
    await expect(readiness).toBeVisible()
    await expect(page.getByTestId('summary-card-publishedInScope')).toBeVisible()
    await expect(page.getByTestId('summary-card-attention')).toBeVisible()
    await expect(page.getByTestId('summary-card-pendingReleaseNeedingSetup')).toBeVisible()

    const ops = page.getByTestId('api-ops-summary')
    await expect(ops).toBeVisible()
    await expect(ops.getByRole('heading', { name: /recent invocation sample|最近调用样本/i })).toBeVisible()

    const opsCards = page.getByTestId(/^ops-card-/)
    const opsEmpty = ops.getByText(/no sampled invocations|暂无抽样调用/i)
    await expect(opsCards.first().or(opsEmpty)).toBeVisible({ timeout: 30_000 })

    if ((await opsCards.count()) > 0) {
      await expect(page.getByTestId('ops-card-performance')).toBeVisible()
      await expect(page.getByTestId('ops-card-failureRate')).toBeVisible()
      await expect(page.getByTestId('ops-card-artifacts')).toBeVisible()
      // No invented SLO / p95 budget labels required for Done.
      await expect(page.getByText(/\bp95\b|error budget|SLO\b/i)).toHaveCount(0)
    }

    await expect(page.getByRole('heading', { name: /^published packages$|^已发布包$/i })).toHaveCount(
      0,
    )
    await expect(page.locator('.alerts-card .el-pagination')).toHaveCount(0)

    await captureEvidence(page, 'SYS-NORM-W3-001-dashboard-ops.png')
  })

  test('BDD-SYS-NORM-W3-002: alert row opens package settings (not hub apiAccess)', async ({
    page,
    request,
  }) => {
    test.setTimeout(360_000)
    const fixture: PendingSubmitTemplateFixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-W3-ALERT-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E W3 Alert ${Date.now().toString(36).toUpperCase()}`,
    })

    await loginAs(page, E2E_GROUP_ADMIN)
    await openExternalServicesOverview(page)

    const alertsCard = page.locator('.alerts-card')
    await expect(alertsCard.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })

    const alertRow = alertsCard.locator('.el-table__row').filter({ hasText: fixture.externalId })
    await expect(alertRow).toBeVisible({ timeout: 45_000 })
    await expect(alertRow).toContainText(/missing authorized ad group|缺少已授权 ad 组/i)

    await alertRow.getByRole('button', { name: /open api settings|打开 api 设置|api settings/i }).click()
    await expect(page).toHaveURL(settingsUrlRe(fixture.templateId), { timeout: 20_000 })
    await expect(page).not.toHaveURL(/tab=apiAccess/)
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toHaveCount(0)

    await captureEvidence(page, 'SYS-NORM-W3-002-alert-to-settings.png')
  })

  test('BDD-SYS-NORM-W3-004/005/007: invocations page via nav; filters; empty or rows', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)

    // Hard navigation must hit nginx SPA try_files (HTML), not the /api/ JSON proxy.
    const hardNav = await page.goto('/api/invocations')
    expect(hardNav?.status()).toBe(200)
    expect(hardNav?.headers()['content-type'] ?? '').toMatch(/text\/html/i)
    await expect(page.locator('#app')).toBeVisible({ timeout: 20_000 })
    await expect(page.locator('.page-header h1')).toHaveText(/invocation records|调用记录/i)
    await expect(page.locator('body')).not.toHaveText(/^\s*\{[\s\S]*"result"\s*:/)

    await openInvocationRecordsViaNav(page)
    await expectFluidLayout(page)

    const filters = page.getByTestId('api-invocations-filters')
    await expect(filters).toBeVisible()
    await expect(page.getByTestId('api-invocations-filter-status')).toBeVisible()
    await expect(page.getByTestId('api-invocations-filter-package')).toBeVisible()
    await expect(page.getByTestId('api-invocations-filter-request-id')).toBeVisible()
    // Element Plus date-picker may not expose data-testid on a visible host — assert by label.
    await expect(filters.getByText(/^created after$|^创建于之后$/i)).toBeVisible()
    await expect(filters.getByText(/^created before$|^创建于之前$/i)).toBeVisible()
    await expect(filters.locator('.el-date-editor')).toHaveCount(2)
    await expect(page.getByTestId('api-invocations-table-card')).toBeVisible()

    // Distinct from dashboard + settings routes.
    await expect(page).not.toHaveURL(/\/api\/policies\/?$/)
    await expect(page).not.toHaveURL(/\/api\/packages\/.+\/settings/)

    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })
    const empty = page.getByText(/no invocation records|暂无调用记录/i)
    const row = page.locator('[data-testid="api-invocations-table-card"] .el-table__body tr').first()
    await expect(empty.or(row)).toBeVisible({ timeout: 30_000 })

    await captureEvidence(page, 'SYS-NORM-W3-004-invocations-page.png')
  })

  test('BDD-SYS-NORM-W3-006/016: invocation detail summary-only when rows exist', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await openInvocationRecordsViaNav(page)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })

    const rows = page.locator('[data-testid="api-invocations-table-card"] .el-table__body tr')
    const rowCount = await rows.count()
    test.skip(rowCount === 0, 'No in-scope invocation rows to open detail (honest empty covered in W3-004)')

    await rows.first().getByRole('button', { name: /open summary|打开摘要/i }).click()
    const drawer = page.getByTestId('invocation-summary-drawer')
    await expect(drawer).toBeVisible({ timeout: 20_000 })
    await expect(drawer.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(drawer.getByText(/variables|variableValues|parameters/i)).toHaveCount(0)
    await expect(drawer.locator('dt').filter({ hasText: /request id|请求 id/i })).toBeVisible()

    await captureEvidence(page, 'SYS-NORM-W3-006-invocation-detail.png')
  })

  test('BDD-SYS-NORM-W3-008/009/015: package settings complete edit surface', async ({
    page,
    request,
  }) => {
    const published = await ensureDemoFullFlowPublished(request)
    await loginAs(page, E2E_GROUP_ADMIN)

    await page.goto(`/api/packages/${published.templateId}/settings`)
    await expect(page).toHaveURL(settingsUrlRe(published.templateId), { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toHaveCount(0)
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    await expect(
      page.getByRole('heading', { name: /external access|对外接入|api settings/i }).first(),
    ).toBeVisible()
    await expect(page.getByText(/under construction|interim shell|临时壳/i)).toHaveCount(0)
    await expect(page.getByText(/advanced settings|高级设置/i)).toBeVisible()

    // Hub must not host a parallel External access tab (Wave 2 lock retained).
    await page.goto(`/templates/${published.templateId}`)
    await expect(page.getByTestId('template-package-hub')).toBeVisible({ timeout: 20_000 })
    await expect(page.getByRole('tab', { name: /^external access$/i })).toHaveCount(0)
    await expect(page.getByRole('tab', { name: /^api access$/i })).toHaveCount(0)

    // PENDING_RELEASE settings path (AOD P1 mapped to settings shell).
    const draft = await requireDemoRetailTemplate(request)
    await page.goto(`/api/packages/${draft.templateId}/settings`)
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toHaveCount(0)

    const accessWarning = page.getByTestId('ad-groups-not-configured-warning')
    const callableHint = page.getByTestId('published-vs-callable-hint')
    // DEMO draft / empty AD Group surfaces publishability vs runtime-callable honesty when applicable.
    if ((await accessWarning.count()) > 0) {
      await expect(accessWarning).toContainText(/not yet runtime-callable|尚不可运行时调用/i)
      await expect(callableHint).toBeVisible()
    }

    await captureEvidence(page, 'SYS-NORM-W3-008-package-settings.png')
  })

  test('BDD-SYS-NORM-W3-010: unknown panel fails closed to settings home', async ({
    page,
    request,
  }) => {
    const fixture = await requireDemoRetailTemplate(request)
    await loginAsGlobalAdmin(page)

    await page.goto(
      `/api/packages/${fixture.templateId}/settings?panel=not-a-real-panel-${Date.now()}`,
    )
    await expect(page).toHaveURL(settingsUrlRe(fixture.templateId), { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-unknown-panel')).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toHaveCount(0)

    await captureEvidence(page, 'SYS-NORM-W3-010-unknown-panel.png')
  })

  test('BDD-SYS-NORM-W3-011: hub / legacy redirects land on completed settings', async ({
    page,
    request,
  }) => {
    const fixture = await requireDemoRetailTemplate(request)
    await loginAsGlobalAdmin(page)
    const settingsRe = settingsUrlRe(fixture.templateId)

    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page).toHaveURL(settingsRe, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toHaveCount(0)

    await page.goto(`/templates/${fixture.templateId}#apiAccess`)
    await expect(page).toHaveURL(settingsRe, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })

    await page.goto(`/api/policies/${fixture.templateId}`)
    await expect(page).toHaveURL(settingsRe, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })

    await page.goto(`/templates/${fixture.templateId}`)
    await page.getByTestId('hub-api-settings-action').click()
    await expect(page).toHaveURL(settingsRe, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible({ timeout: 30_000 })

    await captureEvidence(page, 'SYS-NORM-W3-011-legacy-redirect-settings.png')
  })

  test('BDD-SYS-NORM-W3-012: External services nav has overview + invocations (not settings catalog)', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    const nav = managementNav(page)
    await expect(nav.getByRole('heading', { name: /external services|对外服务/i })).toBeVisible()
    await expect(nav.getByRole('button', { name: OVERVIEW_NAV })).toBeVisible()
    await expect(nav.getByRole('button', { name: INVOCATIONS_NAV })).toBeVisible()
    await expectNavItemHasIcon(nav, OVERVIEW_NAV)
    await expectNavItemHasIcon(nav, INVOCATIONS_NAV)

    // Package settings is deep-link only — not a third sidebar catalog.
    await expect(nav.getByRole('button', { name: /^api settings$|^package api settings$/i })).toHaveCount(
      0,
    )

    await captureEvidence(page, 'SYS-NORM-W3-012-nav-membership.png')
  })

  test('BDD-SYS-NORM-W3-013: template author fail-closed for ops routes', async ({
    page,
    request,
  }) => {
    const demo = await requireDemoRetailTemplate(request)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')

    const nav = managementNav(page)
    await expect(nav.getByRole('button', { name: OVERVIEW_NAV })).toHaveCount(0)
    await expect(nav.getByRole('button', { name: INVOCATIONS_NAV })).toHaveCount(0)

    // SPA deep-links served by nginx try_files (not /api/ management proxy).
    await page.goto('/api/policies')
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByText(/access denied/i)).toBeVisible()

    await page.goto(`/api/packages/${demo.templateId}/settings`)
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })

    // Hard refresh of /api/invocations must serve SPA HTML (nginx exception), then fail-closed.
    const invocationsHardNav = await page.goto('/api/invocations')
    expect(invocationsHardNav?.status()).toBe(200)
    expect(invocationsHardNav?.headers()['content-type'] ?? '').toMatch(/text\/html/i)
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByText(/access denied/i)).toBeVisible()

    await captureEvidence(page, 'SYS-NORM-W3-013-fail-closed.png')
  })

  test('BDD-SYS-NORM-W3-014: GROUP_ADMIN denied out-of-scope CORP package settings', async ({
    page,
    request,
  }) => {
    const fol = await requireFolTemplate(request)
    await loginAs(page, E2E_GROUP_ADMIN)

    await page.goto(`/api/packages/${fol.templateId}/settings`)

    await expect(async () => {
      const onForbidden = /\/forbidden/.test(page.url())
      const deniedVisible =
        (await page.getByText(/access denied|unable to load|forbidden|not found/i).count()) > 0
      const panelAbsent = (await page.getByTestId('api-package-settings-panel').count()) === 0
      expect(onForbidden || deniedVisible || panelAbsent).toBeTruthy()
    }).toPass({ timeout: 20_000 })

    // Must not show CORP editable L1 retention controls for RETAIL group admin.
    await expect(page.getByText(/invocation record retention|调用记录保留/i)).toHaveCount(0)

    await captureEvidence(page, 'SYS-NORM-W3-014-group-scope.png')
  })
})
