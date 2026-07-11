/**
 * LR-C7 UIUX evidence — notification center (bell + dropdown).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C7-uiux-evidence.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Screenshots: frontend/e2e/evidence/LRP-C7/screenshots/
 * Manifest:    frontend/e2e/evidence/LRP-C7-uiux-manifest.md
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import {
  prepareRetailTemplateInTesting,
  requireOpenTestWorkItemForTemplate,
} from './helpers/collaboration-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureLrpC7LocatorScreenshot,
  captureLrpC7Screenshot,
  ensureLrpC7EvidenceDirs,
  LRP_C7_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

function isUnreadCountGet(url: string): boolean {
  try {
    const pathname = new URL(url).pathname.replace(/\/$/, '')
    return pathname === '/api/management/v1/collaboration-notifications/unread-count'
  } catch {
    return false
  }
}

function isNotificationListGet(url: string): boolean {
  try {
    const pathname = new URL(url).pathname.replace(/\/$/, '')
    return pathname === '/api/management/v1/collaboration-notifications'
  } catch {
    return false
  }
}

function isMarkAllPost(url: string): boolean {
  try {
    const pathname = new URL(url).pathname.replace(/\/$/, '')
    return pathname === '/api/management/v1/collaboration-notifications/read-all'
  } catch {
    return false
  }
}

async function hydrateUnreadViaVisibility(page: Page) {
  const unreadPromise = page.waitForResponse(
    (res) => res.request().method() === 'GET' && isUnreadCountGet(res.url()) && res.ok(),
    { timeout: 20_000 },
  )
  await page.evaluate(() => {
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      get: () => 'hidden',
    })
    document.dispatchEvent(new Event('visibilitychange'))
  })
  await page.evaluate(() => {
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      get: () => 'visible',
    })
    document.dispatchEvent(new Event('visibilitychange'))
  })
  await unreadPromise
}

async function waitForBellWithBadge(page: Page) {
  const bell = page.getByTestId('notification-bell')
  await expect(bell).toBeVisible({ timeout: 20_000 })
  await hydrateUnreadViaVisibility(page)
  const badge = page.getByTestId('notification-badge')
  await expect(badge).toBeVisible({ timeout: 15_000 })
  return { bell, badge }
}

async function openNotificationDropdown(page: Page) {
  const listPromise = page.waitForResponse(
    (res) => res.request().method() === 'GET' && isNotificationListGet(res.url()) && res.ok(),
    { timeout: 20_000 },
  )
  await page.getByTestId('notification-bell').click()
  await listPromise
  await expect(page.getByTestId('notification-dropdown')).toBeVisible()
}

async function closeNotificationDropdown(page: Page) {
  await page.locator('#main-content, main, .shell-content').first().click({ position: { x: 24, y: 24 } })
  await expect
    .poll(async () => page.getByTestId('notification-bell').getAttribute('aria-expanded'), {
      timeout: 10_000,
    })
    .not.toBe('true')
  await expect(page.getByTestId('notification-dropdown')).toBeHidden({ timeout: 10_000 })
}

test.describe('LRP-C7 notification center UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    ensureLrpC7EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(LRP_C7_VIEWPORT)
    page.setDefaultTimeout(20_000)
  })

  test('notification bell — dual brand badge / dropdown / empty / focus', async ({
    page,
    request,
  }) => {
    const template = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-C7-UX-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E C7 UX Notify ${Date.now().toString(36)}`,
    })
    await requireOpenTestWorkItemForTemplate(request, template)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible({ timeout: 30_000 })

    // --- REDBC: shell + badge ---
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    const { bell } = await waitForBellWithBadge(page)

    // aria-haspopup may be stripped by Element Plus popover trigger wrapper — note in manifest.
    await expect(bell).toHaveAttribute('aria-label', /notification/i)

    await captureLrpC7Screenshot(page, '01-shell-bell-badge-redbc-en-1440x900.png')
    await captureLrpC7LocatorScreenshot(
      page.locator('.shell-header'),
      '02-header-bell-badge-redbc-en.png',
    )
    await captureLrpC7LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03-brand-header-redbc-en.png',
    )

    // Focus-visible on bell (keyboard)
    await page.keyboard.press('Tab')
    // Walk focus toward bell if needed — click then blur via keyboard re-focus
    await bell.focus()
    await expect(bell).toBeFocused()
    await captureLrpC7LocatorScreenshot(bell, '04-bell-focus-redbc-en.png')

    // --- REDBC: open dropdown with items ---
    await openNotificationDropdown(page)
    await expect(page.getByTestId('notification-item').first()).toBeVisible({ timeout: 15_000 })
    await expect(page.getByTestId('notification-dropdown')).toHaveAttribute('role', 'region')
    // Note: Element Plus popover trigger may omit aria-expanded on the reference button (see manifest).

    await captureLrpC7Screenshot(page, '05-dropdown-open-redbc-en-1440x900.png')
    await captureLrpC7LocatorScreenshot(
      page.getByTestId('notification-dropdown'),
      '06-dropdown-items-redbc-en.png',
    )

    // --- REDBC: empty state (list API may still return read items after mark-all;
    // force empty payload once so empty copy is reachable for evidence) ---
    const markAll = page.getByTestId('notification-mark-all')
    await expect(markAll).toBeVisible()
    const markAllPromise = page.waitForResponse(
      (res) => res.request().method() === 'POST' && isMarkAllPost(res.url()) && res.ok(),
      { timeout: 20_000 },
    )
    await markAll.click()
    await markAllPromise
    await expect(page.getByTestId('notification-badge')).toHaveCount(0, { timeout: 15_000 })

    await closeNotificationDropdown(page)

    await page.route('**/api/management/v1/collaboration-notifications**', async (route) => {
      const url = route.request().url()
      if (route.request().method() !== 'GET') {
        await route.continue()
        return
      }
      if (url.includes('unread-count')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            metadata: { requestId: 'e2e-c7-ux', timestamp: new Date().toISOString() },
            result: { unreadCount: 0 },
            error: null,
          }),
        })
        return
      }
      if (isNotificationListGet(url)) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            metadata: { requestId: 'e2e-c7-ux', timestamp: new Date().toISOString() },
            result: [],
            error: null,
          }),
        })
        return
      }
      await route.continue()
    })

    await openNotificationDropdown(page)
    await expect(page.getByTestId('notification-empty')).toBeVisible({ timeout: 15_000 })
    await captureLrpC7Screenshot(page, '07-dropdown-empty-redbc-en-1440x900.png')
    await captureLrpC7LocatorScreenshot(
      page.getByTestId('notification-dropdown'),
      '08-dropdown-empty-closeup-redbc-en.png',
    )
    await captureLrpC7LocatorScreenshot(
      page.locator('.shell-header'),
      '09-header-bell-zero-unread-redbc-en.png',
    )

    await closeNotificationDropdown(page)
    await page.unroute('**/api/management/v1/collaboration-notifications**')

    // Seed another item for GREENBC badge + open frames
    const templateG = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-C7-UXG-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E C7 UXG Notify ${Date.now().toString(36)}`,
    })
    await requireOpenTestWorkItemForTemplate(request, templateG)

    // --- GREENBC ---
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await hydrateUnreadViaVisibility(page)
    await expect(page.getByTestId('notification-badge')).toBeVisible({ timeout: 15_000 })

    await captureLrpC7Screenshot(page, '10-shell-bell-badge-greenbc-en-1440x900.png')
    await captureLrpC7LocatorScreenshot(
      page.locator('.shell-header'),
      '11-header-bell-badge-greenbc-en.png',
    )
    await captureLrpC7LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '12-brand-header-greenbc-en.png',
    )

    await openNotificationDropdown(page)
    await expect(page.getByTestId('notification-item').first()).toBeVisible({ timeout: 15_000 })
    await captureLrpC7Screenshot(page, '13-dropdown-open-greenbc-en-1440x900.png')
    await captureLrpC7LocatorScreenshot(
      page.getByTestId('notification-dropdown'),
      '14-dropdown-items-greenbc-en.png',
    )

    await closeNotificationDropdown(page)
  })
})
