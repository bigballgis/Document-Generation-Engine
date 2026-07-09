import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { ensureDemoFullFlowPublished } from './helpers/content-modules-api'
import { managementNav } from './helpers/nav'
import {
  captureP12ApiPackageAccessLocatorScreenshot,
  captureP12ApiPackageAccessScreenshot,
  ensureP12ApiPackageAccessEvidenceDirs,
  P12_API_PACKAGE_ACCESS_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P12 API package access UIUX evidence (T12)', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  let templateId = ''

  test.beforeAll(async ({ request }) => {
    ensureP12ApiPackageAccessEvidenceDirs()
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + :8080).` })

    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P12_API_PACKAGE_ACCESS_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
  })

  test('capture hub external access L1 (REDBC)', async ({ page }) => {
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const tabPanel = page.getByRole('tabpanel', { name: /external access|对外接入/i })
    await captureP12ApiPackageAccessLocatorScreenshot(
      tabPanel,
      '01-hub-external-access-l1-redbc-1440x900.png',
    )
    await captureP12ApiPackageAccessScreenshot(page, '02-hub-external-access-full-redbc-1440x900.png')
  })

  test('capture hub external access L1 (GREENBC)', async ({ page }) => {
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const tabPanel = page.getByRole('tabpanel', { name: /external access|对外接入/i })
    await captureP12ApiPackageAccessLocatorScreenshot(
      tabPanel,
      '03-hub-external-access-l1-greenbc-1440x900.png',
    )
  })

  test('capture external services overview (REDBC)', async ({ page }) => {
    await page.goto('/dashboard')
    await managementNav(page).getByRole('button', { name: /^external services overview$/i }).click()
    await expect(page.locator('.page-header h1')).toHaveText(/external services overview|对外服务概览/i)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await captureP12ApiPackageAccessScreenshot(
      page,
      '04-api-services-overview-redbc-1440x900.png',
    )
  })
})
