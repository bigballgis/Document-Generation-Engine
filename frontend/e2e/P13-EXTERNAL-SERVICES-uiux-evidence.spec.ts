import { expect, test } from '@playwright/test'

import { isBackendReady } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  createTemplateApiCredential,
  ensureDemoFullFlowPublished,
  runtimeGenerateDefault,
} from './helpers/content-modules-api'
import { managementNav } from './helpers/nav'
import {
  captureP13ExternalServicesLocatorScreenshot,
  captureP13ExternalServicesScreenshot,
  ensureP13ExternalServicesEvidenceDirs,
  P13_EXTERNAL_SERVICES_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

async function resolveReachableFrontendBaseUrl(
  request: import('@playwright/test').APIRequestContext,
): Promise<string | null> {
  const candidates = [
    process.env.E2E_BASE_URL,
    'http://127.0.0.1:4173',
    'http://127.0.0.1:5173',
  ].filter((value, index, array): value is string => Boolean(value) && array.indexOf(value) === index)

  for (const baseUrl of candidates) {
    try {
      if ((await request.get(baseUrl, { timeout: 5_000 })).ok()) {
        return baseUrl
      }
    } catch {
      // try next candidate
    }
  }
  return null
}

test.describe('P13 External services UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let templateId = ''
  let templateExternalId = ''

  test.beforeAll(async ({ request }) => {
    ensureP13ExternalServicesEvidenceDirs()
    const backendReady = await isBackendReady(request)
    const frontendBaseUrl = await resolveReachableFrontendBaseUrl(request)
    test.skip(
      !(backendReady && frontendBaseUrl),
      `Stack required (${frontendBaseUrl ?? 'frontend unreachable'} + :8080).`,
    )

    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId
    templateExternalId = fixture.externalId

    const credential = await createTemplateApiCredential(request, templateId)
    const generateResult = await runtimeGenerateDefault(
      request,
      templateExternalId,
      credential,
      `e2e-p13-uiux-invocation-${Date.now()}`,
    )
    expect(generateResult.status).toBe(200)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P13_EXTERNAL_SERVICES_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
  })

  test('01 — hub route summary + L1 (REDBC)', async ({ page }) => {
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const routeSummary = page.getByTestId('route-summary-panel')
    await expect(routeSummary).toBeVisible()
    await captureP13ExternalServicesLocatorScreenshot(
      routeSummary,
      '01-hub-route-summary-redbc-1440x900.png',
    )
    await captureP13ExternalServicesScreenshot(page, '02-hub-external-access-full-redbc-1440x900.png')
  })

  test('02 — hub external access L1 (GREENBC)', async ({ page }) => {
    await page.goto('/dashboard')
    await switchBrand(page, 'GREENBC')
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await captureP13ExternalServicesScreenshot(page, '03-hub-external-access-l1-greenbc-1440x900.png')
  })

  test('03 — external services overview alerts (REDBC)', async ({ page }) => {
    await page.goto('/dashboard')
    await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()
    await expect(page.locator('.alerts-card')).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await captureP13ExternalServicesLocatorScreenshot(
      page.locator('.alerts-card'),
      '04-overview-alerts-redbc-1440x900.png',
    )
  })

  test('04 — invocation history panel (REDBC)', async ({ page }) => {
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const invocationsPanel = page.locator('.section-card').filter({
      has: page.getByRole('heading', { name: /invocation history|调用历史/i }),
    })
    await invocationsPanel.scrollIntoViewIfNeeded()
    await expect(invocationsPanel).toBeVisible()
    await captureP13ExternalServicesLocatorScreenshot(
      invocationsPanel,
      '05-hub-invocation-history-redbc-1440x900.png',
    )
  })

  test('05 — overview alerts (GREENBC)', async ({ page }) => {
    await page.goto('/dashboard')
    await switchBrand(page, 'GREENBC')
    await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()
    await expect(page.locator('.alerts-card')).toBeVisible()
    await captureP13ExternalServicesLocatorScreenshot(
      page.locator('.alerts-card'),
      '06-overview-alerts-greenbc-1440x900.png',
    )
  })

  test('06 — invocation summary drawer (REDBC)', async ({ page }) => {
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const invocationsPanel = page.locator('.section-card').filter({
      has: page.getByRole('heading', { name: /invocation history|调用历史/i }),
    })
    await invocationsPanel.scrollIntoViewIfNeeded()
    await expect(invocationsPanel.locator('.invocation-table tbody tr').first()).toBeVisible({
      timeout: 30_000,
    })

    await invocationsPanel.locator('.invocation-table tbody tr').first().click()
    const drawer = page.getByTestId('invocation-summary-drawer')
    await expect(drawer).toBeVisible()
    await expect(drawer.locator('.summary-list')).toBeVisible({ timeout: 15_000 })
    await captureP13ExternalServicesLocatorScreenshot(
      drawer,
      '07-hub-invocation-drawer-redbc-1440x900.png',
    )
  })
})
