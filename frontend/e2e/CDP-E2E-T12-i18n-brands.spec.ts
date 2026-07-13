import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { managementNav } from './helpers/nav'
import {
  type BrandPreset,
  switchBrand,
  switchLocale,
} from './helpers/uiux-evidence'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

const BRAND_PRIMARY: Record<BrandPreset, string> = {
  REDBC: '#DB0011',
  GREENBC: '#00847F',
}

/**
 * Key golden surfaces for CD-E2E-T12 (aligned with T01 / BDD §5 recommended set).
 * Distinct routes — no lifecycle fixture required for locale/brand smoke.
 */
const KEY_SURFACES = [
  {
    id: 'dashboard-test-queue',
    path: '/dashboard?queue=TEST#tasks-section',
    /** Page H1 + nav item (zh-CN). */
    chineseMarkers: [/我的任务/, /待我测试|全部任务|工作流中的模板/],
  },
  {
    id: 'templates-catalog',
    path: '/templates',
    chineseMarkers: [/^模板$/, /按名称浏览模板包|暂无模板包|新建模板包/],
  },
  {
    id: 'external-services-hub',
    path: '/api/policies',
    chineseMarkers: [/对外服务概览/, /对外服务/],
  },
] as const

async function assertZhCnShell(page: Page): Promise<void> {
  await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN')
  await expect(managementNav(page)).toBeVisible()
  await expect(managementNav(page).getByRole('button', { name: /^我的任务$/ })).toBeVisible()
  await expect(managementNav(page).getByRole('button', { name: /^模板$/ })).toBeVisible()
}

async function openSurfaceAndAssertChinese(
  page: Page,
  surface: (typeof KEY_SURFACES)[number],
): Promise<void> {
  await page.goto(surface.path)
  await expect(page).not.toHaveURL(/\/forbidden/, { timeout: 15_000 })
  await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN')
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const main = page.locator('main.shell-content')
  await expect(main).toBeVisible()

  let matched = 0
  for (const marker of surface.chineseMarkers) {
    const hit = main.getByText(marker).first()
    if (await hit.isVisible().catch(() => false)) {
      matched += 1
    }
  }
  // At least one surface-specific Chinese marker (nav/title/CTA class).
  expect(
    matched,
    `Expected zh-CN copy on surface ${surface.id} (${surface.path}); markers=${surface.chineseMarkers}`,
  ).toBeGreaterThanOrEqual(1)

  // Shell nav remains Chinese (not whole-page English fallback).
  await expect(managementNav(page).getByRole('button', { name: /^我的任务$/ })).toBeVisible()
}

async function assertBrandApplied(page: Page, brand: BrandPreset): Promise<void> {
  await expect(page.locator('html')).toHaveAttribute('data-brand', brand)
  const primary = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim().toUpperCase(),
  )
  expect(primary).toBe(BRAND_PRIMARY[brand].toUpperCase())
  await expect(page.locator('.shell-header .header-brand')).toBeVisible()
  await expect(page.locator('.shell-header .header-brand img, .shell-header .header-brand svg').first()).toBeVisible()
}

/**
 * CD-E2E-T12 — zh-CN locale + REDBC/GREENBC dual-brand smoke on key golden surfaces.
 * BDD: docs/behavior/zh-cn-dual-brand-golden-screenshots.md (BDD-CDP-I18N-001…002)
 */
test.describe('CDP-E2E-T12 zh-CN + dual-brand smoke (BDD-CDP-I18N-001…002)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + backend :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 })
  })

  test('BDD-CDP-I18N-001 — zh-CN on ≥3 key golden surfaces; html lang=zh-CN', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)
    await expect(page.locator('.locale-switcher')).toBeVisible()

    await switchLocale(page, 'zh-CN')
    await assertZhCnShell(page)

    expect(KEY_SURFACES.length).toBeGreaterThanOrEqual(3)
    for (const surface of KEY_SURFACES) {
      await openSurfaceAndAssertChinese(page, surface)
    }

    // IA intact: shell + content still present after locale switch + navigation.
    await expect(page.locator('.shell-header')).toBeVisible()
    await expect(page.locator('main.shell-content')).toBeVisible()
  })

  test('BDD-CDP-I18N-002 — REDBC + GREENBC data-brand and primary color @1920', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)
    await switchLocale(page, 'zh-CN')
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await expect(page.getByRole('heading', { level: 1, name: /我的任务/ })).toBeVisible({
      timeout: 30_000,
    })

    await expect(page.locator('.brand-switcher')).toBeVisible()

    await switchBrand(page, 'REDBC')
    await assertBrandApplied(page, 'REDBC')
    // Layout/IA unchanged after brand switch.
    await expect(page.getByRole('heading', { level: 1, name: /我的任务/ })).toBeVisible()
    await expect(managementNav(page)).toBeVisible()

    await switchBrand(page, 'GREENBC')
    await assertBrandApplied(page, 'GREENBC')
    await expect(page.getByRole('heading', { level: 1, name: /我的任务/ })).toBeVisible()
    await expect(managementNav(page)).toBeVisible()

    // Evidence set requirement: both brands exercised in this run.
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
  })
})
