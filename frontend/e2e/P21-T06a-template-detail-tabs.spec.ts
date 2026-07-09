import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { assertFolCatalogSeeded } from './helpers/fol-api'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T06a template detail tabs', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start backend and frontend before running E2E.` })
  })

  test('hub workflow status tab redirects to dev editor approval workspace', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const fixture = await assertFolCatalogSeeded(request)
    await page.goto(`/templates/${fixture.templateId}?tab=overview`, { waitUntil: 'domcontentloaded' })

    const tabs = page.locator('.secondary-tabs')
    await expect(tabs.getByRole('tab', { name: /^overview$/i })).toHaveAttribute('aria-selected', 'true', {
      timeout: 15_000,
    })

    await tabs.getByRole('tab', { name: /^workflow status$/i }).click()
    await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
    await expect(page).toHaveURL(/workspaceTab=approval/)
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('#dev-version-actions')).toHaveCount(0)
  })

  test('hub shows version lines as primary surface', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const fixture = await assertFolCatalogSeeded(request)
    await page.goto(`/templates/${fixture.templateId}`, { waitUntil: 'domcontentloaded' })

    await expect(page.locator('.version-lines-card')).toBeVisible()
    await expect(page.getByText(/version lines/i)).toBeVisible()
  })

  test('lifecycle deep-link redirects to dev editor approval workspace', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const fixture = await assertFolCatalogSeeded(request)
    await page.goto(`/templates/${fixture.templateId}?focus=lifecycle`, { waitUntil: 'domcontentloaded' })

    await expect(page).toHaveURL(/\/dev\//)
    await expect(page).toHaveURL(/workspaceTab=approval/)
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('#dev-version-actions')).toHaveCount(0)

    await expect(
      page.locator('.workspace-tab-shell').getByRole('tab', { name: /^template approval$/i }),
    ).toHaveAttribute('aria-selected', 'true')
  })
})
