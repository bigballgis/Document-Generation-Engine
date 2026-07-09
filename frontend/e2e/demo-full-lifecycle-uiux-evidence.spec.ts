import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  DEMO_FULL_FLOW_EXTERNAL_ID,
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
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + :8080).` })

    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(DEMO_FULL_FLOW_VIEWPORT)
  })

  test('capture external services overview and hub api access tab (REDBC)', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard')
    await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()
    await expect(page.locator('.page-header h1')).toHaveText(/external services overview|对外服务概览/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const catalogTable = page.locator('.app-data-table')
    await expect(catalogTable.getByRole('row', { name: new RegExp(DEMO_FULL_FLOW_EXTERNAL_ID, 'i') })).toBeVisible()
    await captureDemoFullFlowScreenshot(page, '01-api-management-home-redbc-1440x900.png')

    await page.getByRole('row', { name: new RegExp(DEMO_FULL_FLOW_EXTERNAL_ID, 'i') }).click()
    await expect(page).toHaveURL(new RegExp(`/templates/${templateId}\\?tab=apiAccess`))
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.locator('.api-access-layout')).toBeVisible()
    await captureDemoFullFlowLocatorScreenshot(
      page.locator('.api-access-layout'),
      '02-api-policy-detail-domains-redbc-1440x900.png',
    )
  })

  test('capture template hub API access tab (GREENBC)', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const policyCard = page.locator('.api-access-layout').first()
    await expect(policyCard).toBeVisible()

    await switchBrand(page, 'GREENBC')
    await expect(policyCard).toBeVisible()
    await captureDemoFullFlowLocatorScreenshot(
      policyCard,
      '03-template-api-access-tab-greenbc-1440x900.png',
    )
  })
})
