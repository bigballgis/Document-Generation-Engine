import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { fetchDemoFullFlowApiPolicy } from './helpers/content-modules-api'
import { openDevEditorWorkspaceTab } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  prepareTemplatePendingRelease,
  type PendingSubmitTemplateFixture,
} from './helpers/submit-approval-gate-api'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

/**
 * Stage 6 functional E2E — slice `api-ops-discoverability` (Task Master #52).
 * BDD: docs/behavior/api-ops-discoverability.md (SCEN-AOD-01/03/06/07/09/13/14).
 */
test.describe('API-OPS-DISCOVERABILITY (BDD-API-OPS-DISCOVERABILITY-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: PendingSubmitTemplateFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    fixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-AOD-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E API Ops Discoverability ${Date.now().toString(36).toUpperCase()}`,
    })

    // C10 skeleton policy must exist at PENDING_RELEASE with empty AD groups (AOD-C1 / AOD-C6).
    const policy = await fetchDemoFullFlowApiPolicy(request, fixture.templateId)
    expect(Array.isArray(policy.allowedAdGroups)).toBe(true)
    expect(policy.allowedAdGroups.length).toBe(0)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
  })

  test('SCEN-AOD-01 — PENDING_RELEASE Hub registers External access tab', async ({ page }) => {
    await page.goto(`/templates/${fixture.templateId}`)
    await expect(page.locator('.workspace-tab-shell')).toBeVisible({ timeout: 30_000 })

    const apiAccessTab = page.locator('.workspace-tab-shell').getByRole('tab', {
      name: /external access|对外接入/i,
    })
    await expect(apiAccessTab).toBeVisible()

    await apiAccessTab.click()
    await expect(page).toHaveURL(new RegExp(`/templates/${fixture.templateId}\\?tab=apiAccess`))
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.locator('.api-access-layout')).toBeVisible()
  })

  test('SCEN-AOD-03 — /api/policies/:id deep link activates apiAccess (not overview fallback)', async ({
    page,
  }) => {
    await page.goto(`/api/policies/${fixture.templateId}`)

    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId}\\?tab=apiAccess`),
      { timeout: 15_000 },
    )
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const apiAccessTab = page.locator('.workspace-tab-shell').getByRole('tab', {
      name: /external access|对外接入/i,
    })
    await expect(apiAccessTab).toBeVisible()
    await expect(apiAccessTab).toHaveAttribute('aria-selected', 'true')

    await expect(page.locator('.api-access-layout')).toBeVisible()
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    // Must not fall back to overview-only empty hub without the access surface.
    await expect(page.getByTestId('route-summary-panel')).toBeVisible()
  })

  test('SCEN-AOD-06/07 — Overview shows three summary cards + alerts; no template catalog', async ({
    page,
  }) => {
    await page.goto('/api/policies')

    await expect(page.locator('.page-header h1')).toHaveText(
      /external services overview|对外服务概览/i,
    )
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const summary = page.getByTestId('api-readiness-summary')
    await expect(summary).toBeVisible()
    await expect(summary.getByRole('heading', { name: /api readiness summary|api 就绪摘要/i })).toBeVisible()

    await expect(page.getByTestId('summary-card-publishedInScope')).toBeVisible()
    await expect(page.getByTestId('summary-card-attention')).toBeVisible()
    await expect(page.getByTestId('summary-card-pendingReleaseNeedingSetup')).toBeVisible()

    // Numeric counts render (AOD-C4 minimum set); pending-setup should include our fixture (≥1).
    const pendingCard = page.getByTestId('summary-card-pendingReleaseNeedingSetup')
    await expect(pendingCard.locator('.summary-count')).toBeVisible()
    const pendingCountText = ((await pendingCard.locator('.summary-count').textContent()) ?? '').trim()
    expect(Number.parseInt(pendingCountText, 10)).toBeGreaterThanOrEqual(1)

    const alertsCard = page.locator('.alerts-card')
    await expect(alertsCard.getByRole('heading', { name: /attention items|待关注项/i })).toBeVisible()
    await expect(page.getByText(/aggregated alerts coming soon|汇总告警即将提供/i)).toHaveCount(0)

    // SCEN-ALERT-04 / AOD-C5: monitoring only — not a paginated template catalog.
    await expect(page.getByRole('heading', { name: /^published packages$|^已发布包$/i })).toHaveCount(0)
    await expect(alertsCard.locator('.el-pagination')).toHaveCount(0)
    await expect(page.locator('.summary-section + .el-pagination')).toHaveCount(0)
  })

  test('SCEN-AOD-09 — PENDING_RELEASE missing AD Group alert deep-links to Hub apiAccess', async ({
    page,
  }) => {
    await page.goto('/api/policies')
    await expect(page.locator('.alerts-card .el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const alertsCard = page.locator('.alerts-card')
    const alertRow = alertsCard.locator('.el-table__row').filter({ hasText: fixture.externalId })
    await expect(alertRow).toBeVisible({ timeout: 30_000 })
    await expect(alertRow).toContainText(/missing authorized ad group|缺少已授权 ad 组/i)

    await alertRow.getByRole('button', { name: /open external access|打开对外接入|external access|对外接入/i }).click()

    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId}\\?tab=apiAccess`),
      { timeout: 15_000 },
    )
    await expect(page.locator('.api-access-layout')).toBeVisible({ timeout: 30_000 })
  })

  test('SCEN-AOD-13/14 — AD groups warning testids + published ≠ runtime-callable copy', async ({
    page,
    request,
  }) => {
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.locator('.api-access-layout')).toBeVisible()

    const accessWarning = page.getByTestId('ad-groups-not-configured-warning')
    await expect(accessWarning).toBeVisible()
    await expect(accessWarning).toContainText(/not yet runtime-callable|尚不可运行时调用/i)
    await expect(accessWarning).toContainText(
      /published status alone does not make|仅发布状态并不能使|fail-closed|失败关闭/i,
    )

    const callableHint = page.getByTestId('published-vs-callable-hint')
    await expect(callableHint).toBeVisible()
    await expect(callableHint).toContainText(
      /published status is not the same as runtime-callable|已发布不等于运行时可调用/i,
    )

    // Publish-gate surface lives under Approval → Publish readiness sub-tab.
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await page
      .locator('.approval-sub-tabs')
      .getByRole('tab', { name: /publish readiness|发布就绪/i })
      .click()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const gateWarning = page.getByTestId('publish-gate-ad-groups-warning')
    await expect(gateWarning).toBeVisible({ timeout: 30_000 })
    await expect(gateWarning).toContainText(
      /authorized ad groups not configured|尚未配置授权 ad 组/i,
    )
  })
})
