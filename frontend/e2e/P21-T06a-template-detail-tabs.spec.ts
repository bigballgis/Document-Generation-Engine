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

  test('overview tab query syncs when switching tabs', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const detailPath = await demoTemplateDetailPath(request)
    await page.goto(`${detailPath}?tab=overview`, { waitUntil: 'domcontentloaded' })

    const tabs = page.locator('.detail-tabs')
    await expect(tabs.getByRole('tab', { name: /^overview$/i })).toHaveAttribute('aria-selected', 'true', {
      timeout: 15_000,
    })

    await tabs.getByRole('tab', { name: /^published versions$/i }).click()
    await expect(page).toHaveURL(/tab=releaseVersions/)
    await expect(tabs.getByRole('tab', { name: /^published versions$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
  })

  test('lifecycle deep-link normalizes and allows switching away', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const detailPath = await demoTemplateDetailPath(request)
    await page.goto(`${detailPath}?focus=lifecycle`, { waitUntil: 'domcontentloaded' })

    const tabs = page.locator('.detail-tabs')
    await expect(tabs.getByRole('tab', { name: /^workflow status$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page).toHaveURL(/tab=lifecycle/)
    await expect(page).not.toHaveURL(/focus=lifecycle/)

    await tabs.getByRole('tab', { name: /^overview$/i }).click()
    await expect(page).toHaveURL(/tab=overview/)
    await expect(tabs.getByRole('tab', { name: /^overview$/i })).toHaveAttribute('aria-selected', 'true')
  })
})
