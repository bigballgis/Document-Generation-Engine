import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_ADMIN, loginAs, loginAsAuditAdmin } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  captureUxEntityDisplayLocatorScreenshot,
  captureUxEntityDisplayScreenshot,
  ensureUxEntityDisplayEvidenceDirs,
  switchBrand,
  UX_ENTITY_DISPLAY_VIEWPORT,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

const UUID_RE =
  /\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b/i

async function expectFluidPageLayout(page: import('@playwright/test').Page) {
  const layout = page.locator('.app-page-layout').first()
  await expect(layout).toBeVisible()
  const maxWidth = await layout.evaluate((el) => window.getComputedStyle(el).maxWidth)
  expect(['none', '100%', '']).toContain(maxWidth)
}

async function expectNoRawUuidInEntityCells(page: import('@playwright/test').Page) {
  const labels = page.locator('.entity-link-cell__text, .entity-link-cell__link')
  await expect(labels.first()).toBeVisible({ timeout: 30_000 })
  const count = await labels.count()
  for (let i = 0; i < count; i += 1) {
    const text = (await labels.nth(i).textContent())?.trim() ?? ''
    expect(text.length).toBeGreaterThan(0)
    expect(UUID_RE.test(text)).toBe(false)
  }
}

test.describe('UX-ENTITY-DISPLAY UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    ensureUxEntityDisplayEvidenceDirs()

    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1` })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(UX_ENTITY_DISPLAY_VIEWPORT)
  })

  test('capture activity log and template list entity display evidence', async ({ page }) => {
    await loginAsAuditAdmin(page)
    await page.goto('/audit')
    await expect(page.getByRole('heading', { level: 1, name: /^activity log$/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    await expectFluidPageLayout(page)

    const filtersCard = page.locator('.filters-card')
    await expect(filtersCard.getByText(/^event type$/i)).toBeVisible()
    const eventTypeSelect = filtersCard.locator('.el-select').first()
    await expect(eventTypeSelect).toBeVisible()
    await eventTypeSelect.click()
    const eventTypeDropdown = page.locator('.el-select-dropdown').filter({ visible: true })
    await expect(eventTypeDropdown.locator('.el-select-dropdown__item').first()).toBeVisible({
      timeout: 10_000,
    })
    await page.keyboard.press('Escape')

    await expectNoRawUuidInEntityCells(page)
    await expect(page.locator('.entity-link-cell__text').first()).toBeVisible()

    await captureUxEntityDisplayLocatorScreenshot(
      filtersCard,
      '01-activity-log-filters-redbc-1440x900.png',
    )
    await captureUxEntityDisplayScreenshot(page, '02-activity-log-table-redbc-1440x900.png')

    await page.evaluate(() => localStorage.clear())
    await page.context().clearCookies()
    await loginAs(page, E2E_ADMIN)
    await page.goto('/templates')
    await expect(page.getByRole('heading', { level: 1, name: /^templates$/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()

    await expectFluidPageLayout(page)
    await expectNoRawUuidInEntityCells(page)

    const templateLink = page.locator('.entity-link-cell__link').first()
    await expect(templateLink).toBeVisible()

    await captureUxEntityDisplayScreenshot(page, '03-templates-list-redbc-1440x900.png')
    await expect(page.locator('.entity-link-cell__subtitle').first()).toBeVisible()

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await captureUxEntityDisplayLocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04-brand-header-greenbc-1440x900.png',
    )
    await captureUxEntityDisplayScreenshot(page, '05-templates-list-greenbc-1440x900.png')
  })
})
