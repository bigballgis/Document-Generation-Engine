import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  captureLrpB6LocatorScreenshot,
  captureLrpB6Screenshot,
  ensureLrpB6EvidenceDirs,
  LRP_B6_VIEWPORT,
  switchBrand,
  switchLocale,
} from './helpers/uiux-evidence'

/**
 * LR-B6 session-limit reminder UIUX evidence (SCEN-UX-02).
 *
 * Requires the Part B TTL environment: docgen-backend restarted with
 * SESSION_ABSOLUTE_TTL=PT9M so the banner is visible right after login
 * (see LRP-B6-session-renewal.spec.ts header for the restart runbook).
 * Screenshots land in e2e/evidence/LRP-B6/screenshots/.
 */

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

const REMINDER_WINDOW_MS = 10 * 60_000

async function stackReady(request: APIRequestContext): Promise<boolean> {
  try {
    const backend = await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })
    const frontend = await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })
    return backend.ok() && frontend.ok()
  } catch {
    return false
  }
}

async function absoluteDeadlineMsLeft(request: APIRequestContext): Promise<number> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: { username: E2E_ADMIN.username, password: E2E_ADMIN.password },
  })
  expect(response.ok(), `TTL probe login failed with HTTP ${response.status()}`).toBeTruthy()
  const body = (await response.json()) as {
    result?: {
      sessionAbsoluteDeadline?: string
      session?: { absoluteSessionExpiresAt?: string }
    }
  }
  const absoluteRaw =
    body.result?.sessionAbsoluteDeadline ?? body.result?.session?.absoluteSessionExpiresAt
  expect(absoluteRaw, 'login result missing absolute session deadline').toBeTruthy()
  return Date.parse(absoluteRaw!) - Date.now()
}

/** Brand switcher option labels are localized; match either locale's label. */
async function switchBrandToRedbcLocalized(page: Page): Promise<void> {
  await page.locator('.brand-switcher').click()
  await page.getByRole('option', { name: /^(red bank|红色银行)$/i }).click()
  await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
}

/** Waits out the switcher dropdown close animation so captures stay clean. */
async function expectPoppersClosed(page: Page): Promise<void> {
  await page.keyboard.press('Escape')
  await expect(page.locator('.el-popper:visible')).toHaveCount(0)
}

test.describe('LRP-B6 session-limit reminder UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    ensureLrpB6EvidenceDirs()
    test.skip(
      !(await stackReady(request)),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )
    const msLeft = await absoluteDeadlineMsLeft(request)
    test.skip(
      !(msLeft > 0 && msLeft < REMINDER_WINDOW_MS),
      `Backend must run with SESSION_ABSOLUTE_TTL=PT9M so the reminder shows right after login ` +
        `(measured deadline in ${Math.round(msLeft / 1000)}s)`,
    )
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(LRP_B6_VIEWPORT)
  })

  test('capture reminder banner across REDBC/GREENBC and en/zh-CN at 1440x900', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)

    const reminder = page.locator('.session-limit-reminder')
    await expect(reminder).toBeVisible({ timeout: 15_000 })
    await expect(reminder.getByText('Session ending soon', { exact: true })).toBeVisible()

    // Captures must show a settled page, not skeleton placeholders.
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    await expectPoppersClosed(page)
    await captureLrpB6Screenshot(page, '01-session-reminder-redbc-en-1440x900.png')
    await captureLrpB6LocatorScreenshot(reminder, '02-session-reminder-banner-redbc-en.png')

    // Action button keyboard :focus-visible state (a11y evidence): real Tab
    // presses so the focus-visible heuristic applies, then a banner close-up
    // with the focus ring.
    const actionButton = reminder.getByRole('button', { name: 'Sign in again' })
    let actionFocused = false
    for (let i = 0; i < 30 && !actionFocused; i += 1) {
      await page.keyboard.press('Tab')
      actionFocused = await actionButton.evaluate((el) => el === document.activeElement)
    }
    expect(actionFocused, 'Tab order must reach the reminder action button').toBeTruthy()
    await captureLrpB6LocatorScreenshot(
      reminder,
      '07-session-reminder-action-focus-visible-redbc-en.png',
    )
    await actionButton.evaluate((el) => (el as HTMLElement).blur())

    await switchBrand(page, 'GREENBC')
    await expect(reminder).toBeVisible()
    await expectPoppersClosed(page)
    await captureLrpB6Screenshot(page, '03-session-reminder-greenbc-en-1440x900.png')
    await captureLrpB6LocatorScreenshot(reminder, '06-session-reminder-banner-greenbc-en.png')

    await switchLocale(page, 'zh-CN')
    await expect(reminder.getByText('会话即将结束', { exact: true })).toBeVisible()
    await expectPoppersClosed(page)
    await captureLrpB6Screenshot(page, '04-session-reminder-greenbc-zhcn-1440x900.png')

    await switchBrandToRedbcLocalized(page)
    await expect(reminder).toBeVisible()
    await expectPoppersClosed(page)
    await captureLrpB6Screenshot(page, '05-session-reminder-redbc-zhcn-1440x900.png')
  })
})
