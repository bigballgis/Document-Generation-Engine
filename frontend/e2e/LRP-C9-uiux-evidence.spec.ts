/**
 * LR-C9 UIUX evidence — LoadErrorPanel + role-aware empty CTAs on catalog lists.
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C9-uiux-evidence.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Screenshots: frontend/e2e/evidence/LRP-C9/screenshots/
 * Manifest:    frontend/e2e/evidence/LRP-C9-uiux-manifest.md
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs, loginAsGlobalAdmin } from './helpers/auth'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureLrpC9LocatorScreenshot,
  captureLrpC9Screenshot,
  ensureLrpC9EvidenceDirs,
  LRP_C9_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function expectPoppersClosed(page: Page): Promise<void> {
  await page.keyboard.press('Escape')
  await expect(page.locator('.el-popper:visible')).toHaveCount(0)
}

async function routeTemplatesFail(page: Page): Promise<void> {
  await page.route('**/api/management/v1/templates**', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.continue()
      return
    }
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({
        metadata: { traceId: 'uiux-lrp-c9-templates' },
        error: {
          code: 'INTERNAL_ERROR',
          category: 'SYSTEM',
          retryable: true,
          message: 'Unable to load templates.',
          messageKey: 'templates.error.loadList',
        },
      }),
    })
  })
}

async function routeTemplatesEmpty(page: Page): Promise<void> {
  await page.route('**/api/management/v1/templates**', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.continue()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        metadata: { traceId: 'uiux-lrp-c9-templates-empty' },
        result: {
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        },
      }),
    })
  })
}

async function routeGroupsFail(page: Page): Promise<void> {
  await page.route('**/api/management/v1/groups**', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.continue()
      return
    }
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({
        metadata: { traceId: 'uiux-lrp-c9-groups' },
        error: {
          code: 'INTERNAL_ERROR',
          category: 'SYSTEM',
          retryable: true,
          message: 'Unable to load groups.',
          messageKey: 'identity.error.loadGroups',
        },
      }),
    })
  })
}

test.describe('LRP-C9 LoadErrorPanel / empty CTA UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    ensureLrpC9EvidenceDirs()
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(LRP_C9_VIEWPORT)
  })

  test('capture templates error, empty CTA, groups error — REDBC + GREENBC', async ({ page }) => {
    // --- Templates LoadErrorPanel (author) ---
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await switchBrand(page, 'REDBC')
    await expectPoppersClosed(page)
    await routeTemplatesFail(page)
    await page.goto('/templates')

    const templatesError = page.locator('.el-result')
    await expect(page.getByText(/unable to load templates/i)).toBeVisible({ timeout: 20_000 })
    await expect(page.getByRole('button', { name: /^retry$/i })).toBeVisible()
    await expect(page.getByText(/this error is retryable/i)).toBeVisible()
    await expect(templatesError).toBeVisible()

    await captureLrpC9Screenshot(page, '01-templates-load-error-redbc-en-1440x900.png')
    await captureLrpC9LocatorScreenshot(
      templatesError,
      '02-templates-load-error-panel-redbc-en.png',
    )

    await switchBrand(page, 'GREENBC')
    await expectPoppersClosed(page)
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await captureLrpC9Screenshot(page, '03-templates-load-error-greenbc-en-1440x900.png')
    await captureLrpC9LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04-brand-header-greenbc-templates-error.png',
    )

    // Focus-visible on Retry
    await switchBrand(page, 'REDBC')
    await expectPoppersClosed(page)
    const retryBtn = page.getByRole('button', { name: /^retry$/i })
    await retryBtn.focus()
    await expect(retryBtn).toBeFocused()
    await captureLrpC9LocatorScreenshot(templatesError, '05-templates-retry-focus-redbc-en.png')

    await page.unroute('**/api/management/v1/templates**')

    // --- Templates empty + role-aware CTA ---
    await routeTemplatesEmpty(page)
    await page.goto('/templates')
    await expect(page.getByText(/no template packages yet/i)).toBeVisible({ timeout: 20_000 })
    const emptyActions = page.locator('[data-testid="empty-state-actions"]')
    await expect(
      emptyActions.getByRole('button', { name: /new template package/i }),
    ).toBeVisible()

    await captureLrpC9Screenshot(page, '06-templates-empty-cta-redbc-en-1440x900.png')
    await captureLrpC9LocatorScreenshot(
      page.locator('.el-empty').first(),
      '07-templates-empty-panel-redbc-en.png',
    )

    await switchBrand(page, 'GREENBC')
    await expectPoppersClosed(page)
    await captureLrpC9Screenshot(page, '08-templates-empty-cta-greenbc-en-1440x900.png')

    await page.unroute('**/api/management/v1/templates**')

    // --- Groups LoadErrorPanel (global admin) ---
    await page.context().clearCookies()
    await page.goto('/login', { waitUntil: 'domcontentloaded' })
    await page.evaluate(() => {
      localStorage.clear()
      sessionStorage.clear()
    })
    await loginAsGlobalAdmin(page)
    await switchBrand(page, 'REDBC')
    await expectPoppersClosed(page)
    await routeGroupsFail(page)
    await page.goto('/entitlement/groups')

    const groupsError = page.locator('.el-result')
    await expect(page.getByText(/unable to load groups/i)).toBeVisible({ timeout: 20_000 })
    await expect(page.getByRole('button', { name: /^retry$/i })).toBeVisible()
    await expect(groupsError).toBeVisible()

    await captureLrpC9Screenshot(page, '09-groups-load-error-redbc-en-1440x900.png')
    await captureLrpC9LocatorScreenshot(groupsError, '10-groups-load-error-panel-redbc-en.png')

    await switchBrand(page, 'GREENBC')
    await expectPoppersClosed(page)
    await captureLrpC9Screenshot(page, '11-groups-load-error-greenbc-en-1440x900.png')
    await captureLrpC9LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '12-brand-header-greenbc-groups-error.png',
    )
  })
})
