import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { demoTemplateDetailPath } from './helpers/content-modules-api'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('P21-T06b template detail states', () => {
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

  test('header and lifecycle tab render for template author', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const detailPath = await demoTemplateDetailPath(request)
    await page.goto(`${detailPath}?tab=lifecycle`, { waitUntil: 'domcontentloaded' })

    await expect(page.locator('.workspace-header__title')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /^template approval$/i })).toBeVisible({
      timeout: 15_000,
    })
  })
})
