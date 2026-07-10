import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { managementNav } from './helpers/nav'
import {
  captureCdpE2eDecisionLocatorScreenshot,
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
  switchLocale,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`
const TASK_ID = 'CDP-E2E-T12' as const

async function captureBrandHeader(page: Page, filename: string): Promise<string> {
  return captureCdpE2eDecisionLocatorScreenshot(
    page.locator('.shell-header .header-brand'),
    TASK_ID,
    filename,
  )
}

/**
 * CD-E2E-T12 UIUX evidence scaffold — zh-CN × REDBC/GREENBC @1920 on key golden surfaces
 * (BDD-CDP-I18N-001…002). Functional assertions live in CDP-E2E-T12-i18n-brands.spec.ts;
 * this spec captures screenshot artifacts for e2e-uiux-reviewer / T12 manifest.
 */
test.describe('CDP-E2E-T12 UIUX evidence — zh-CN dual-brand @1920 (BDD-CDP-I18N-001…002)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + backend :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture ≥3 zh-CN key surfaces + REDBC/GREENBC @1920', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)
    await switchLocale(page, 'zh-CN')
    await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN')
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    // --- Surface 1: Dashboard TEST queue (zh-CN / REDBC) ---
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await expect(page.getByRole('heading', { level: 1, name: /我的任务/ })).toBeVisible({
      timeout: 30_000,
    })
    await expect(managementNav(page).getByRole('button', { name: /^我的任务$/ })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-dashboard-test-queue-zhcn-redbc-1920x1080.png',
    )
    await captureBrandHeader(page, '02-brand-header-zhcn-redbc-1920x1080.png')

    // --- Surface 2: Templates catalog (zh-CN / REDBC) ---
    await page.goto('/templates')
    await expect(page.getByRole('heading', { level: 1, name: /^模板$/ })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '03-templates-catalog-zhcn-redbc-1920x1080.png',
    )

    // --- Surface 3: External services hub (zh-CN / REDBC) ---
    await page.goto('/api/policies')
    await expect(page.getByRole('heading', { level: 1, name: /对外服务概览/ })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '04-external-services-hub-zhcn-redbc-1920x1080.png',
    )

    // --- Dual-brand: GREENBC on dashboard (zh-CN retained) ---
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await expect(page.getByRole('heading', { level: 1, name: /我的任务/ })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN')
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '05-dashboard-test-queue-zhcn-greenbc-1920x1080.png',
    )
    await captureBrandHeader(page, '06-brand-header-zhcn-greenbc-1920x1080.png')

    // --- GREENBC on templates (second brand × second surface) ---
    await page.goto('/templates')
    await expect(page.getByRole('heading', { level: 1, name: /^模板$/ })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '07-templates-catalog-zhcn-greenbc-1920x1080.png',
    )
  })
})
