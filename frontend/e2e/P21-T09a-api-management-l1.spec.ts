import { expect, test } from '@playwright/test'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { FORBIDDEN_L1_PATTERN, managementNav } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T09a API management L1 copy (§2.3)', () => {
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

  test('api management home avoids forbidden L1 tokens on primary surface', async ({ page }) => {
    await page.goto('/dashboard')
    await managementNav(page).getByRole('button', { name: /^api management$/i }).click()

    await expect(page.locator('.page-header h1')).toHaveText(/manage api access/i)
    await expect(page.getByText(/^API policy management$/i)).toHaveCount(0)
    await expect(page.getByRole('button', { name: /^credentials$/i })).toHaveCount(0)

    const header = page.locator('.page-header').first()
    await expect(header).toBeVisible()
    const headerText = (await header.innerText()).toLowerCase()
    expect(headerText).not.toMatch(FORBIDDEN_L1_PATTERN)
  })

  test('left nav api management item uses business label', async ({ page }) => {
    await page.goto('/dashboard')

    const nav = managementNav(page)
    await expect(nav.getByRole('button', { name: /^api management$/i })).toBeVisible()
    const navText = (await nav.innerText()).toLowerCase()
    expect(navText).not.toMatch(FORBIDDEN_L1_PATTERN)
  })
})
