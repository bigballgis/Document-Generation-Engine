import { expect, test } from '@playwright/test'

import {
  DEMO_FULL_FLOW_EXTERNAL_ID,
  DEMO_FULL_FLOW_NAME,
  E2E_GROUP_ADMIN,
  loginAs,
} from './helpers/auth'
import { ensureDemoFullFlowPublished } from './helpers/content-modules-api'
import { managementNav } from './helpers/nav'
import {
  captureDemoFullFlowLocatorScreenshot,
  captureDemoFullFlowScreenshot,
  DEMO_FULL_FLOW_VIEWPORT,
  ensureDemoFullFlowEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('Demo full lifecycle UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let templateId = ''

  test.beforeAll(async ({ request }) => {
    ensureDemoFullFlowEvidenceDirs()
    let backendReady = false
    let frontendReady = false
    try {
      backendReady = (await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })).ok()
    } catch {
      backendReady = false
    }
    try {
      frontendReady = (await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })).ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Stack required (${FRONTEND_BASE_URL} + :8080).`,
    )

    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(DEMO_FULL_FLOW_VIEWPORT)
  })

  test('capture API management home and policy detail (REDBC)', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard')
    await managementNav(page).getByRole('button', { name: /^api management$/i }).click()
    await expect(page.locator('.page-header h1')).toHaveText(/manage api access/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const catalogTable = page.locator('.app-data-table')
    await expect(catalogTable.getByRole('row', { name: new RegExp(DEMO_FULL_FLOW_EXTERNAL_ID, 'i') })).toBeVisible()
    await captureDemoFullFlowScreenshot(page, '01-api-management-home-redbc-1440x900.png')

    await page.getByRole('row', { name: new RegExp(DEMO_FULL_FLOW_EXTERNAL_ID, 'i') }).click()
    await expect(page).toHaveURL(new RegExp(`/api/policies/${templateId}`))
    await expect(page.locator('.page-header h1')).toHaveText(DEMO_FULL_FLOW_NAME)
    await expect(page.locator('.domain-nav')).toBeVisible()
    await captureDemoFullFlowLocatorScreenshot(
      page.locator('.domain-layout'),
      '02-api-policy-detail-domains-redbc-1440x900.png',
    )
  })

  test('capture template hub API access tab (GREENBC)', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const policyCard = page.locator('.section-card').filter({ has: page.locator('.policy-summary') })
    await expect(policyCard).toBeVisible()

    await switchBrand(page, 'GREENBC')
    await expect(policyCard).toBeVisible()
    await captureDemoFullFlowLocatorScreenshot(
      policyCard,
      '03-template-api-access-tab-greenbc-1440x900.png',
    )
  })
})
