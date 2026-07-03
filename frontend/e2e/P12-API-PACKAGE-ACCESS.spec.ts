import { expect, test } from '@playwright/test'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { ensureDemoFullFlowPublished } from './helpers/content-modules-api'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P12 API package access hub (BDD S6 L1)', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeAll(async ({ request }) => {
    let backendReady = false
    let frontendReady = false
    try {
      const backend = await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })
      backendReady = backend.ok()
    } catch {
      backendReady = false
    }
    try {
      const frontend = await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })
      frontendReady = frontend.ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.`,
    )
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
    await expect(page.getByText(/save generated documents|保存生成文档/i)).toBeVisible()
    await expect(page.getByText(/invocation record retention|调用记录保留/i)).toBeVisible()
    await expect(page.getByText(/recent invocations|最近调用/i)).toBeVisible()
  })

  test('api services overview avoids legacy catalog primary surface', async ({ page }) => {
    await page.goto('/api/policies')
    await expect(page.getByText(/external services overview|对外服务概览/i)).toBeVisible()
    await expect(page.getByText(/^API policy management$/i)).toHaveCount(0)
  })
})
