import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { demoTemplateDetailPath } from './helpers/content-modules-api'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T06a template detail tabs', () => {
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

  test('hub workflow status tab redirects to dev editor workflow actions', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const detailPath = await demoTemplateDetailPath(request)
    await page.goto(`${detailPath}?tab=overview`, { waitUntil: 'domcontentloaded' })

    const tabs = page.locator('.secondary-tabs')
    await expect(tabs.getByRole('tab', { name: /^overview$/i })).toHaveAttribute('aria-selected', 'true', {
      timeout: 15_000,
    })

    await tabs.getByRole('tab', { name: /^workflow status$/i }).click()
    await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
    await expect(page).toHaveURL(/tab=authoring/)
    await expect(page.locator('#dev-version-actions')).toBeVisible({ timeout: 15_000 })
  })

  test('hub shows version lines as primary surface', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const detailPath = await demoTemplateDetailPath(request)
    await page.goto(detailPath, { waitUntil: 'domcontentloaded' })

    await expect(page.locator('.version-lines-card')).toBeVisible()
    await expect(page.getByText(/version lines/i)).toBeVisible()
  })

  test('lifecycle deep-link redirects to dev editor workflow actions', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const detailPath = await demoTemplateDetailPath(request)
    await page.goto(`${detailPath}?focus=lifecycle`, { waitUntil: 'domcontentloaded' })

    await expect(page).toHaveURL(/\/dev\//)
    await expect(page).toHaveURL(/tab=authoring/)
    await expect(page.locator('#dev-version-actions')).toBeVisible({ timeout: 15_000 })

    const tabs = page.locator('.detail-tabs')
    await expect(tabs.getByRole('tab', { name: /^template design$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(tabs.getByRole('tab', { name: /^workflow status$/i })).toHaveCount(0)
  })
})
