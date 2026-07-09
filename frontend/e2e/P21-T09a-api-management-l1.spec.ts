import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { FORBIDDEN_L1_PATTERN, managementNav } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T09a API management L1 copy (§2.3)', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.` })
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
