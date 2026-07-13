/**
 * LR-C5 UIUX evidence — catalog list pagination / filter toolbar surfaces.
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C5-uiux-evidence.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Screenshots: frontend/e2e/evidence/LRP-C5/screenshots/
 * Manifest:    frontend/e2e/evidence/LRP-C5-uiux-manifest.md
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureLrpC5LocatorScreenshot,
  captureLrpC5Screenshot,
  ensureLrpC5EvidenceDirs,
  LRP_C5_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function expectPoppersClosed(page: Page): Promise<void> {
  await page.keyboard.press('Escape')
  await expect(page.locator('.el-popper:visible')).toHaveCount(0)
}

async function waitForCatalogChrome(page: Page): Promise<void> {
  await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('.catalog-filter-toolbar')).toBeVisible()
  await expect(page.locator('.list-pagination, .el-pagination').first()).toBeVisible()
}

async function paginationBar(page: Page) {
  return page.locator('.list-pagination, .el-pagination').first()
}

test.describe('LRP-C5 catalog pagination UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensureLrpC5EvidenceDirs()
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(LRP_C5_VIEWPORT)
  })

  test('templates / masters / content-modules — dual brand pagination + filters', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)
    await switchBrand(page, 'REDBC')
    await expectPoppersClosed(page)

    // --- Templates catalog (primary LR-C5 surface) ---
    await page.goto('/templates')
    await waitForCatalogChrome(page)

    await captureLrpC5Screenshot(page, '01-templates-page0-redbc-en-1440x900.png')
    await captureLrpC5LocatorScreenshot(
      page.locator('.catalog-filter-toolbar'),
      '02-templates-filter-toolbar-redbc-en.png',
    )
    const templatesPager = await paginationBar(page)
    await templatesPager.scrollIntoViewIfNeeded()
    await captureLrpC5LocatorScreenshot(templatesPager, '03-templates-pagination-redbc-en.png')
    await captureLrpC5LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04-brand-header-redbc-en.png',
    )

    // Focus on Next pager control
    const nextBtn = page.locator('.list-pagination button.btn-next, .el-pagination button.btn-next').first()
    await nextBtn.focus()
    await expect(nextBtn).toBeFocused()
    await captureLrpC5LocatorScreenshot(templatesPager, '05-templates-next-focus-redbc-en.png')

    // Page 2 — pager active state
    await nextBtn.click()
    await expect
      .poll(async () => {
        const active = page.locator('.list-pagination li.is-active, .el-pagination li.is-active')
        return (await active.first().innerText()).trim()
      })
      .not.toBe('1')
    await waitForCatalogChrome(page)
    await captureLrpC5Screenshot(page, '06-templates-page1-redbc-en-1440x900.png')
    await captureLrpC5LocatorScreenshot(
      await paginationBar(page),
      '07-templates-pagination-page1-redbc-en.png',
    )

    // Filter applied (group CORP) — toolbar + pager remain coherent
    await page.locator('.list-pagination button.btn-prev, .el-pagination button.btn-prev').first().click()
    await expect
      .poll(async () => {
        const active = page.locator('.list-pagination li.is-active, .el-pagination li.is-active')
        return (await active.first().innerText()).trim()
      })
      .toBe('1')
    const groupInput = page.locator('.catalog-filter-toolbar__control input').first()
    await groupInput.fill('CORP')
    await expect
      .poll(async () => {
        const codes = await page.locator('.el-table__body-wrapper tbody tr td:nth-child(1)').allInnerTexts()
        return codes.length > 0 && codes.every((c) => c.trim() === 'CORP')
      })
      .toBe(true)
    await captureLrpC5Screenshot(page, '08-templates-filtered-corp-redbc-en-1440x900.png')
    await captureLrpC5LocatorScreenshot(
      page.locator('.catalog-filter-toolbar'),
      '09-templates-filter-chips-redbc-en.png',
    )

    // GREENBC — templates
    await switchBrand(page, 'GREENBC')
    await expectPoppersClosed(page)
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await waitForCatalogChrome(page)
    await captureLrpC5Screenshot(page, '10-templates-filtered-corp-greenbc-en-1440x900.png')
    await captureLrpC5LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '11-brand-header-greenbc-en.png',
    )
    await captureLrpC5LocatorScreenshot(
      await paginationBar(page),
      '12-templates-pagination-greenbc-en.png',
    )

    // Clear filter for clean masters/modules shots
    await page.locator('.catalog-filter-toolbar').getByRole('button', { name: /clear/i }).click().catch(async () => {
      await groupInput.fill('')
    })

    // --- Masters catalog ---
    await switchBrand(page, 'REDBC')
    await expectPoppersClosed(page)
    await page.goto('/masters')
    await waitForCatalogChrome(page)
    await captureLrpC5Screenshot(page, '13-masters-page0-redbc-en-1440x900.png')
    await captureLrpC5LocatorScreenshot(
      await paginationBar(page),
      '14-masters-pagination-redbc-en.png',
    )

    await switchBrand(page, 'GREENBC')
    await expectPoppersClosed(page)
    await waitForCatalogChrome(page)
    await captureLrpC5Screenshot(page, '15-masters-page0-greenbc-en-1440x900.png')
    await captureLrpC5LocatorScreenshot(
      await paginationBar(page),
      '16-masters-pagination-greenbc-en.png',
    )

    // --- Content modules catalog ---
    await switchBrand(page, 'REDBC')
    await expectPoppersClosed(page)
    await page.goto('/content-modules')
    await waitForCatalogChrome(page)
    await captureLrpC5Screenshot(page, '17-content-modules-page0-redbc-en-1440x900.png')
    await captureLrpC5LocatorScreenshot(
      page.locator('.catalog-filter-toolbar'),
      '18-content-modules-filter-toolbar-redbc-en.png',
    )
    await captureLrpC5LocatorScreenshot(
      await paginationBar(page),
      '19-content-modules-pagination-redbc-en.png',
    )

    await switchBrand(page, 'GREENBC')
    await expectPoppersClosed(page)
    await waitForCatalogChrome(page)
    await captureLrpC5Screenshot(page, '20-content-modules-page0-greenbc-en-1440x900.png')
    await captureLrpC5LocatorScreenshot(
      await paginationBar(page),
      '21-content-modules-pagination-greenbc-en.png',
    )
  })
})
