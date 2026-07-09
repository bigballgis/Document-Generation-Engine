import { expect, test, type APIRequestContext } from '@playwright/test'

import { isBackendReady } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { ensureDemoFullFlowPublished } from './helpers/content-modules-api'
import { managementNav } from './helpers/nav'

async function resolveReachableFrontendBaseUrl(request: APIRequestContext): Promise<string | null> {
  const candidates = [
    process.env.E2E_BASE_URL,
    'http://127.0.0.1:4173',
    'http://127.0.0.1:5173',
  ].filter((value, index, array): value is string => Boolean(value) && array.indexOf(value) === index)

  for (const baseUrl of candidates) {
    try {
      if ((await request.get(baseUrl, { timeout: 5_000 })).ok()) {
        return baseUrl
      }
    } catch {
      // try next candidate
    }
  }
  return null
}

test.describe('P13 External services excellence — Phase 3', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeAll(async ({ request }) => {
    const backendReady = await isBackendReady(request)
    const frontendBaseUrl = await resolveReachableFrontendBaseUrl(request)
    test.skip(
      !(backendReady && frontendBaseUrl),
      `Stack required (frontend + backend :8080). Start with .\\scripts\\docker-deploy.ps1 or pnpm dev.`,
    )
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
  })

  test.describe('P13-ESO-B04 — route summary on hub External access', () => {
    test('route summary visible without expanding caller contract panel', async ({ page, request }) => {
      const fixture = await ensureDemoFullFlowPublished(request)
      await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)

      await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
      await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

      const routeSummary = page.getByTestId('route-summary-panel')
      await expect(routeSummary.getByRole('heading', { name: /^route summary$|路由摘要/i })).toBeVisible()
      await expect(routeSummary.locator('.summary-item').filter({ hasText: /package external id|包外部 id/i }).locator('dd')).toHaveText(fixture.externalId)
      await expect(routeSummary.locator('.path-value')).toContainText(/\/generate/i)

      const contractCollapse = page.locator('.contract-collapse .el-collapse-item').first()
      await expect(contractCollapse.getByText(/caller contract|调用方契约/i)).toBeVisible()
      await expect(contractCollapse).not.toHaveClass(/is-active/)
      await expect(contractCollapse.getByText(/runtime paths|运行时路径/i)).not.toBeVisible()
    })

    test('legacy /api/policies/:id redirects to hub External access tab', async ({ page, request }) => {
      const fixture = await ensureDemoFullFlowPublished(request)
      await page.goto(`/api/policies/${fixture.templateId}?domain=OUTPUT_POLICY`)

      await expect(page).toHaveURL(
        new RegExp(`/templates/${fixture.templateId}\\?tab=apiAccess`),
        { timeout: 15_000 },
      )
      await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
      await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
      const routeSummary = page.getByTestId('route-summary-panel')
      await expect(routeSummary.locator('.summary-item').filter({ hasText: /package external id|包外部 id/i }).locator('dd')).toHaveText(fixture.externalId)
      await expect(page.locator('.domain-nav')).toHaveCount(0)
    })
  })

  test.describe('P13-ESO-D05 — cross-package alerts on overview', () => {
    test('overview shows alerts section (not coming soon) for published fixture', async ({
      page,
      request,
    }) => {
      await ensureDemoFullFlowPublished(request)
      await page.goto('/dashboard')
      await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()

      await expect(page).toHaveURL(/\/api\/policies/)
      await expect(page.locator('.page-header h1')).toHaveText(/external services overview|对外服务概览/i)

      const alertsCard = page.locator('.alerts-card')
      await expect(alertsCard.getByRole('heading', { name: /attention items|待关注项/i })).toBeVisible()
      await expect(page.getByText(/aggregated alerts coming soon|汇总告警即将提供/i)).toHaveCount(0)
      await expect(alertsCard.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

      const issueColumn = alertsCard.getByRole('columnheader', { name: /^issue$|^问题$/i })
      const emptyState = alertsCard.getByText(/no attention items|暂无待关注项/i)
      await expect(issueColumn.or(emptyState)).toBeVisible({ timeout: 30_000 })
    })
  })
})
