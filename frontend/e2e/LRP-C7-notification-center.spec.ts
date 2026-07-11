/**
 * LR-C7 — In-app notification center (bell + unread) — BDD-LRP-C7-001…017 subset.
 *
 * Canonical run (after stage 5 DEPLOY_OK):
 *   pnpm -C frontend exec playwright test e2e/LRP-C7-notification-center.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080.
 *
 * Scenario map (this file):
 *   BDD-001       — bell + badge ≥1; dropdown lists seeded OPEN TEST item
 *   BDD-002       — click item → mark-read + /dashboard?queue=TEST#tasks-section
 *   BDD-003       — open dropdown alone does not clear unread
 *   BDD-004       — Mark all as read clears badge
 *   BDD-009       — AUDIT_ADMIN (no collaboration capability) → no bell
 *
 * Note: shell mount can race the first unread-count poll against auth header readiness.
 * Specs re-hydrate unread via Page Visibility (C7-C8) before asserting the badge.
 */
import { expect, test, type Page } from '@playwright/test'

import {
  E2E_AUDIT_ADMIN,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  prepareRetailTemplateInTesting,
  requireOpenTestWorkItemForTemplate,
  type CollaborationWorkItemFixture,
  type TestingTemplateFixture,
} from './helpers/collaboration-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

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

function isMarkReadPost(url: string, workItemId: string): boolean {
  try {
    const pathname = new URL(url).pathname.replace(/\/$/, '')
    return pathname === `/api/management/v1/collaboration-notifications/${workItemId}/read`
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

async function seedOpenTestItem(
  request: Parameters<typeof prepareRetailTemplateInTesting>[0],
): Promise<{ template: TestingTemplateFixture; workItem: CollaborationWorkItemFixture }> {
  const template = await prepareRetailTemplateInTesting(request, {
    externalId: `E2E-C7-${Date.now().toString(36).toUpperCase()}`,
    name: `E2E C7 Notify ${Date.now().toString(36)}`,
  })
  const workItem = await requireOpenTestWorkItemForTemplate(request, template)
  return { template, workItem }
}

/**
 * After login the first unread poll can complete before the bearer token is attached,
 * leaving unreadCount at 0. C7-C8 visibility resume forces an immediate refresh.
 */
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
  const label = (await badge.innerText()).trim()
  const numeric = label === '99+' ? 99 : Number.parseInt(label, 10)
  expect(numeric).toBeGreaterThanOrEqual(1)
  return { bell, badge, label }
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

async function expectDropdownClosed(page: Page) {
  // Vue may omit aria-expanded when false; treat anything other than "true" as closed.
  await expect
    .poll(async () => page.getByTestId('notification-bell').getAttribute('aria-expanded'), {
      timeout: 10_000,
    })
    .not.toBe('true')
  await expect(page.getByTestId('notification-dropdown')).toBeHidden({ timeout: 10_000 })
}

async function closeNotificationDropdown(page: Page) {
  // Element Plus popover: outside click (C7-T7). Escape is unreliable with teleported content.
  await page.locator('#main-content, main, .shell-content').first().click({ position: { x: 24, y: 24 } })
  await expectDropdownClosed(page)
}

test.describe('LRP-C7 notification center', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test.beforeEach(async ({ page }) => {
    page.setDefaultTimeout(20_000)
  })

  test('BDD-001: OPEN TEST item shows unread badge and appears in dropdown', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    const { template, workItem } = await seedOpenTestItem(request)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard')

    await waitForBellWithBadge(page)
    await openNotificationDropdown(page)

    const item = page.getByTestId('notification-item').filter({ hasText: template.name })
    await expect(item).toBeVisible({ timeout: 15_000 })
    await expect(item).toContainText(/test|testing/i)
    await expect(item).toContainText(template.name)
    expect(workItem.queue).toBe('TEST')
  })

  test('BDD-002: click item marks read and deep-links to TEST task hub', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    const { template, workItem } = await seedOpenTestItem(request)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/templates')

    const { badge, label: badgeBefore } = await waitForBellWithBadge(page)
    await openNotificationDropdown(page)

    const item = page.getByTestId('notification-item').filter({ hasText: template.name })
    await expect(item).toBeVisible({ timeout: 15_000 })

    const markReadPromise = page.waitForResponse(
      (res) =>
        res.request().method() === 'POST' &&
        isMarkReadPost(res.url(), workItem.workItemId) &&
        res.ok(),
      { timeout: 20_000 },
    )
    await item.click()
    await markReadPromise

    await expect(page).toHaveURL(/\/dashboard\?queue=TEST(?:&|#|$)/, { timeout: 15_000 })
    await expect(page).toHaveURL(/#tasks-section/)
    await expect(page.locator('#tasks-section')).toBeVisible()
    await expect(page.locator('[data-partition-id="queue-TEST"]')).toBeVisible({ timeout: 15_000 })
    await expectDropdownClosed(page)

    await expect
      .poll(
        async () => {
          const count = await page.getByTestId('notification-badge').count()
          if (count === 0) {
            return 'gone'
          }
          return (await page.getByTestId('notification-badge').innerText()).trim()
        },
        { timeout: 15_000 },
      )
      .not.toBe(badgeBefore)

    if ((await badge.count()) > 0 && /^\d+$/.test(badgeBefore)) {
      const after = (await badge.innerText()).trim()
      if (/^\d+$/.test(after)) {
        expect(Number.parseInt(after, 10)).toBeLessThan(Number.parseInt(badgeBefore, 10))
      }
    }
  })

  test('BDD-003: opening dropdown alone does not clear unread', async ({ page, request }) => {
    test.setTimeout(180_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    await seedOpenTestItem(request)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard')

    const { badge, label: badgeBefore } = await waitForBellWithBadge(page)

    const markReadPosts: string[] = []
    page.on('request', (req) => {
      if (req.method() !== 'POST') {
        return
      }
      const url = req.url()
      if (isMarkAllPost(url) || /\/collaboration-notifications\/[^/]+\/read(?:\?|$)/.test(url)) {
        markReadPosts.push(url)
      }
    })

    await openNotificationDropdown(page)
    await expect(page.getByTestId('notification-item').first()).toBeVisible()
    await closeNotificationDropdown(page)

    expect(markReadPosts).toHaveLength(0)
    await expect(badge).toBeVisible()
    await expect(badge).toHaveText(badgeBefore)
  })

  test('BDD-004: Mark all as read clears badge', async ({ page, request }) => {
    test.setTimeout(180_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    await seedOpenTestItem(request)
    await seedOpenTestItem(request)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard')

    await waitForBellWithBadge(page)
    await openNotificationDropdown(page)

    const markAll = page.getByTestId('notification-mark-all')
    await expect(markAll).toBeVisible()

    const markAllPromise = page.waitForResponse(
      (res) => res.request().method() === 'POST' && isMarkAllPost(res.url()) && res.ok(),
      { timeout: 20_000 },
    )
    await markAll.click()
    await markAllPromise

    await expect(page.getByTestId('notification-badge')).toHaveCount(0, { timeout: 15_000 })
    await expect(page.getByTestId('notification-mark-all')).toHaveCount(0)
    await expect(page.getByTestId('notification-list-error')).toHaveCount(0)
  })

  test('BDD-009: user without collaboration capability has no bell', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)

    await loginAs(page, E2E_AUDIT_ADMIN)
    await page.goto('/audit')

    await expect(page.getByTestId('notification-bell')).toHaveCount(0)
  })
})
