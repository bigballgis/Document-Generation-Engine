import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { prepareDraftTemplateWithOutdatedClauseReference } from './helpers/content-modules-api'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

test.describe('CE-U07 clause outdated bump UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
  })

  test('capture out-of-date badge and bump affordances @1920 REDBC', async ({ page, request }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
    await loginAs(page, E2E_TEMPLATE_AUTHOR)

    const fixture = await prepareDraftTemplateWithOutdatedClauseReference(request)
    await page.goto(
      `/templates/${fixture.templateId}/dev/${fixture.inFlightDevVersionId}?workspaceTab=design&designTab=contentModules`,
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await page.locator('.design-sub-tabs').getByRole('tab', { name: /^clause references$/i }).click()
    await expect(page.getByTestId('clause-reference-outdated-badge')).toBeVisible({
      timeout: 30_000,
    })
    await page.screenshot({
      path: 'e2e/evidence/CE-U07/screenshots/01-clause-outdated-badge-redbc-1920x1080.png',
      fullPage: true,
    })
  })
})
