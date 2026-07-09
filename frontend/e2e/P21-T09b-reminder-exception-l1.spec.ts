import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_ADMIN, E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { prepareTemplateInTesting } from './helpers/collaboration-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { FORBIDDEN_L1_PATTERN } from './helpers/nav'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T09b reminder timing + confirm on behalf L1 copy', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start backend and frontend before running E2E.` })
  })

  test('reminder timing panel avoids IT jargon on primary surface', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    const timeoutPanel = page.locator('.timeout-config-card')
    await expect(timeoutPanel.getByRole('heading', { name: /reminder timing/i })).toBeVisible()

    const panelText = (await timeoutPanel.innerText()).toLowerCase()
    expect(panelText).not.toMatch(/\bescalation\b/)
    expect(panelText).not.toMatch(FORBIDDEN_L1_PATTERN)
    await expect(timeoutPanel.getByText(/testing reminder after/i)).toBeVisible()
    await expect(timeoutPanel.getByRole('button', { name: /save reminder timing/i })).toBeVisible()
  })

  test('GROUP admin sees confirm-on-behalf in test pass decision dialog', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplateInTesting(request)

    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto(`/templates/${template.templateId}?tab=lifecycle`)

    await expect(page.locator('#template-lifecycle-panel')).toBeVisible({ timeout: 15_000 })
    await page.getByRole('button', { name: /^pass test$/i }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText('Confirm on behalf', { exact: true })).toBeVisible()
    await expect(dialog.getByText(/record exception intervention/i)).toHaveCount(0)
  })

  test('GLOBAL admin sees confirm-on-behalf in test pass decision dialog', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplateInTesting(request)

    await loginAs(page, E2E_ADMIN)
    await page.goto(`/templates/${template.templateId}?tab=lifecycle`)

    await expect(page.locator('#template-lifecycle-panel')).toBeVisible({ timeout: 15_000 })
    await page.getByRole('button', { name: /^pass test$/i }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText('Confirm on behalf', { exact: true })).toBeVisible()
    await expect(dialog.getByText(/activity log/i)).toBeVisible()
  })
})
