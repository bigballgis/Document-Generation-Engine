import { expect, test, type APIRequestContext } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  DEMO_FULL_FLOW_EXTERNAL_ID,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import {
  createDemoFullFlowDraftTemplate,
  DEMO_FULL_FLOW_RELEASE_VERSION,
  ensureDemoFullFlowAtStage,
  ensureDemoFullFlowPublished,
  fetchDemoFullFlowApiPolicy,
  type DemoFullFlowFixture,
} from './helpers/content-modules-api'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { managementNav } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

async function assertStackReady(request: APIRequestContext) {
  await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1` })
}

async function fetchFullFlowStatus(request: APIRequestContext, templateId: string) {
  const authorLogin = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: E2E_TEMPLATE_AUTHOR,
  })
  const token = ((await authorLogin.json()) as { result: { accessToken: string } }).result.accessToken
  const detail = await request.get(`${E2E_API_BASE_URL}/templates/${templateId}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  return (await detail.json()) as {
    result: { lifecycleStatus: string; approvalSubState?: string | null }
  }
}

test.describe('Demo full lifecycle through API management (BDD)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: DemoFullFlowFixture

  test.beforeAll(async ({ request }) => {
    await assertStackReady(request)
    await assertDemoCatalogSeeded(request)
    fixture = await createDemoFullFlowDraftTemplate(request)
  })

  test('stage 1 — draft template with bindings is ready for testing', async ({ request }) => {
    const detail = await fetchFullFlowStatus(request, fixture.templateId)
    if (detail.result.lifecycleStatus !== 'DRAFT') {
      test.skip(true, `Full-flow template already at ${detail.result.lifecycleStatus}`)
    }
    expect(detail.result.lifecycleStatus).toBe('DRAFT')
  })

  test('stage 2 — author submits for testing and template enters TESTING', async ({ request }) => {
    const before = await fetchFullFlowStatus(request, fixture.templateId)
    if (['APPROVAL', 'PENDING_RELEASE', 'PUBLISHED'].includes(before.result.lifecycleStatus)) {
      test.skip(true, `Full-flow template already at ${before.result.lifecycleStatus}`)
    }
    await ensureDemoFullFlowAtStage(request, 'TESTING')
    const detail = await fetchFullFlowStatus(request, fixture.templateId)
    expect(detail.result.lifecycleStatus).toBe('TESTING')
  })

  test('stage 3 — tester passes and author submits for approval', async ({ request }) => {
    const before = await fetchFullFlowStatus(request, fixture.templateId)
    if (['PENDING_RELEASE', 'PUBLISHED'].includes(before.result.lifecycleStatus)) {
      test.skip(true, `Full-flow template already at ${before.result.lifecycleStatus}`)
    }
    if (
      before.result.lifecycleStatus === 'APPROVAL' &&
      before.result.approvalSubState === 'PENDING_DECISION'
    ) {
      return
    }
    await ensureDemoFullFlowAtStage(request, 'APPROVAL_PENDING_DECISION')
    const detail = await fetchFullFlowStatus(request, fixture.templateId)
    expect(detail.result.lifecycleStatus).toBe('APPROVAL')
    expect(detail.result.approvalSubState).toBe('PENDING_DECISION')
  })

  test('stage 4 — approver approves and template enters PENDING_RELEASE', async ({ request }) => {
    const before = await fetchFullFlowStatus(request, fixture.templateId)
    if (before.result.lifecycleStatus === 'PUBLISHED') {
      test.skip(true, 'Full-flow template already published')
    }
    await ensureDemoFullFlowAtStage(request, 'PENDING_RELEASE')
    const detail = await fetchFullFlowStatus(request, fixture.templateId)
    expect(detail.result.lifecycleStatus).toBe('PENDING_RELEASE')
  })

  test('stage 5 — group admin configures API policy and publishes release', async ({ request }) => {
    fixture = await ensureDemoFullFlowPublished(request)
    const detail = await fetchFullFlowStatus(request, fixture.templateId)
    expect(detail.result.lifecycleStatus).toBe('PUBLISHED')

    const policy = await fetchDemoFullFlowApiPolicy(request, fixture.templateId)
    expect(policy.defaultRouteReleaseVersion).toBe(DEMO_FULL_FLOW_RELEASE_VERSION)
    expect(policy.allowedAdGroups).toContain('RETAIL_API')
    expect(policy.outputFormats).toContain('DOCX')
    expect(policy.policyVersion).toBeGreaterThanOrEqual(1)
  })

  test('stage 6 — published template appears on external services overview', async ({ page, request }) => {
    fixture = await ensureDemoFullFlowPublished(request)
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard')
    await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()
    await expect(page).toHaveURL(/\/api\/policies/)

    await expect(page.locator('.page-header h1')).toHaveText(/external services overview|对外服务概览/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByRole('row', { name: new RegExp(DEMO_FULL_FLOW_EXTERNAL_ID, 'i') })).toBeVisible({
      timeout: 30_000,
    })
  })

  test('stage 7 — group admin opens hub external access from overview', async ({ page, request }) => {
    fixture = await ensureDemoFullFlowPublished(request)
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard')
    await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()
    await page.getByRole('row', { name: new RegExp(DEMO_FULL_FLOW_EXTERNAL_ID, 'i') }).click()
    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId}\\?tab=apiAccess`),
      { timeout: 15_000 },
    )

    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByText(/authorized ad groups|授权 ad 组/i)).toBeVisible()
    await expect(page.getByText(/default route|默认路由/i)).toBeVisible()
    await expect(page.getByText(/advanced settings|高级设置/i)).toBeVisible()

    await page.getByText(/advanced settings|高级设置/i).click()
    await expect(page.getByRole('button', { name: /save output settings|保存输出设置/i })).toBeVisible()
  })

  test('stage 8 — legacy api policy URL redirects to hub tab', async ({ page, request }) => {
    fixture = await ensureDemoFullFlowPublished(request)
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto(`/api/policies/${fixture.templateId}?domain=OUTPUT_POLICY`)
    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId}\\?tab=apiAccess`),
      { timeout: 15_000 },
    )

    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.getByText(/RETAIL_API/)).toBeVisible()
    await expect(page.locator('.domain-nav')).toHaveCount(0)
  })

  test('stage 9 — left nav external services entry reaches published template catalog', async ({ page, request }) => {
    fixture = await ensureDemoFullFlowPublished(request)
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard')

    await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()
    await expect(page).toHaveURL(/\/api\/policies/)
    await expect(page.getByRole('row', { name: new RegExp(DEMO_FULL_FLOW_EXTERNAL_ID, 'i') })).toBeVisible()
  })
})
