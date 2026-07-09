import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  ensureDemoFullFlowPublished,
  fetchRecentManagementInvocations,
} from './helpers/content-modules-api'
import { managementNav } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P12 API package access hub (BDD S6 L1)', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.` })
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
  })

  test('external access tab shows L1 retention controls without not-configured empty state', async ({
    page,
    request,
  }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)

    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.getByText(/API access not configured|尚未配置对外接入/i)).toHaveCount(0)
    await expect(
      page.getByText(/save generated documents on the server|在服务端保存生成文档/i),
    ).toBeVisible()
    await expect(page.getByText(/invocation record retention|调用记录保留/i)).toBeVisible()
    await expect(page.getByRole('heading', { name: /recent invocations|最近调用/i })).toBeVisible()
  })

  test('BDD S4 — advanced policy domains collapsed by default on hub tab', async ({
    page,
    request,
  }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)

    await expect(page.getByText(/advanced settings|高级设置/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /save output settings|保存输出设置/i })).toHaveCount(0)
    await page.getByText(/advanced settings|高级设置/i).click()
    await expect(page.getByRole('button', { name: /save output settings|保存输出设置/i })).toBeVisible()
    await expect(page.getByText(/platform defaults|平台默认/i).first()).toBeVisible()
  })

  test('BDD S8 — retention preset options visible on L1 surface', async ({ page, request }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)

    const recordSelect = page.locator('.retention-select').first()
    await expect(recordSelect).toBeVisible()
    await recordSelect.click()
    await expect(page.getByRole('option', { name: /365 days|365 天/i })).toBeVisible()
    await expect(page.getByRole('option', { name: /90 days|90 天/i })).toBeVisible()
  })

  test('recent invocations panel shows empty state instead of coming-soon placeholder', async ({
    page,
    request,
  }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)

    await expect(page.getByText(/invocation summary coming soon|调用摘要即将提供/i)).toHaveCount(0)
    const emptyState = page.getByText(/no recent invocations|暂无最近调用/i)
    const invocationTable = page.locator('.invocation-table')
    await expect(emptyState.or(invocationTable)).toBeVisible()
  })

  test('api services overview avoids legacy catalog primary surface', async ({ page }) => {
    await page.goto('/dashboard')
    await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()
    await expect(page.locator('.page-header h1')).toHaveText(/external services overview|对外服务概览/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByText(/^API policy management$/i)).toHaveCount(0)
  })

  test('BDD S6 — management recent invocations API returns summary without parameters', async ({
    request,
  }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    const rows = await fetchRecentManagementInvocations(request, fixture.templateId, 10)
    expect(Array.isArray(rows)).toBe(true)
    for (const row of rows) {
      expect(row).not.toHaveProperty('parameters')
      expect(row).not.toHaveProperty('variableValues')
      expect(Object.keys(row).sort()).toEqual(
        [
          'accessAccountSummary',
          'createdAt',
          'invocationId',
          'invocationKind',
          'requestId',
          'resolvedReleaseVersion',
          'routeType',
          'status',
        ].sort(),
      )
    }
  })
})
