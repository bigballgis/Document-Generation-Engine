import { expect, test, type APIRequestContext, type Page, type Request } from '@playwright/test'

import { isDockerStackReady } from './helpers/stack-readiness'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { managementNav } from './helpers/nav'
import { switchLocale } from './helpers/uiux-evidence'

/**
 * LR-B6 session renewal / reminder browser journeys (BDD-LRP-SESSION-001
 * SCEN-UX-01 + SCEN-UX-02, docs/behavior/session-renewal-revocation.md §8.6/§8.7).
 *
 * The two parts need different backend TTLs, controlled OUTSIDE this spec by
 * restarting docgen-backend with env overrides (docker-compose.prod.yml passes
 * them through):
 *
 *   Part A (silent renewal):  JWT_ACCESS_TOKEN_TTL=PT2M   (absolute TTL default PT8H)
 *   Part B (near-limit banner): SESSION_ABSOLUTE_TTL=PT9M (token TTL default PT30M,
 *                               login expiry clamps to the 9-minute deadline)
 *
 * Select the active part per run:
 *   RENEWAL_PART=A pnpm -C frontend exec playwright test LRP-B6-session-renewal.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *   RENEWAL_PART=B ... (after restarting the backend with the Part B TTL)
 *
 * Each part additionally probes the live backend TTLs via an API login and
 * skips with a diagnostic when the environment does not match, so a mismatched
 * restart can never produce a false green.
 */

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

const RENEWAL_PART = process.env.RENEWAL_PART ?? ''

/** Mirrors useSessionRenewal.ts constants (renew < 5min left, remind < 10min left). */
const RENEWAL_WINDOW_MS = 5 * 60_000
const REMINDER_WINDOW_MS = 10 * 60_000

/** en / zh-CN L1 copy locked by the behavior spec §12.3. */
const REMINDER_COPY = {
  en: {
    title: 'Session ending soon',
    message:
      'Your sign-in session is about to reach its time limit. Please save your work, then sign in again to continue.',
    action: 'Sign in again',
  },
  'zh-CN': {
    title: '会话即将结束',
    message: '您的登录会话即将到达时长上限。请先保存当前工作，然后重新登录以继续使用。',
    action: '重新登录',
  },
} as const

async function stackReady(request: APIRequestContext): Promise<boolean> {
  return isDockerStackReady(request, { frontendBaseUrl: FRONTEND_BASE_URL })
}

interface SessionWindows {
  accessTokenMsLeft: number
  absoluteMsLeft: number
}

/** Logs in over the API and measures the server-issued renewal windows. */
async function measureSessionWindows(request: APIRequestContext): Promise<SessionWindows> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: { username: E2E_ADMIN.username, password: E2E_ADMIN.password },
  })
  expect(response.ok(), `TTL probe login failed with HTTP ${response.status()}`).toBeTruthy()
  const body = (await response.json()) as {
    result?: {
      accessTokenExpiresAt?: string
      sessionAbsoluteDeadline?: string
      session?: { expiresAt?: string; absoluteSessionExpiresAt?: string }
    }
  }
  const now = Date.now()
  const expiresAtRaw = body.result?.accessTokenExpiresAt ?? body.result?.session?.expiresAt
  const absoluteRaw =
    body.result?.sessionAbsoluteDeadline ?? body.result?.session?.absoluteSessionExpiresAt
  expect(expiresAtRaw, 'login result missing access token expiry').toBeTruthy()
  expect(absoluteRaw, 'login result missing absolute session deadline').toBeTruthy()
  return {
    accessTokenMsLeft: Date.parse(expiresAtRaw!) - now,
    absoluteMsLeft: Date.parse(absoluteRaw!) - now,
  }
}

function trackRenewRequests(page: Page): Request[] {
  const seen: Request[] = []
  page.on('request', (request) => {
    if (request.method() === 'POST' && request.url().includes('/auth/renew')) {
      seen.push(request)
    }
  })
  return seen
}

test.describe('LRP-B6 Part A — silent sliding renewal keeps editing intact (SCEN-UX-01)', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    test.skip(
      RENEWAL_PART !== 'A',
      'Part A runs only with RENEWAL_PART=A while docgen-backend uses JWT_ACCESS_TOKEN_TTL=PT2M',
    )
    test.skip(
      !(await stackReady(request)),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )
    const windows = await measureSessionWindows(request)
    test.skip(
      !(windows.accessTokenMsLeft > 0 && windows.accessTokenMsLeft < RENEWAL_WINDOW_MS),
      `Backend must run with JWT_ACCESS_TOKEN_TTL=PT2M so a fresh token is already inside the renewal window ` +
        `(measured token life ${Math.round(windows.accessTokenMsLeft / 1000)}s)`,
    )
    test.skip(
      windows.absoluteMsLeft < REMINDER_WINDOW_MS,
      `Absolute session TTL must stay at the PT8H default for Part A ` +
        `(measured deadline in ${Math.round(windows.absoluteMsLeft / 1000)}s)`,
    )
  })

  test('renew returns 200 mid-editing without navigation, input loss, or error toast', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)

    // Navigate SPA-style via the sidebar (a full page.goto reload could race
    // the shell-mount renewal that a 2-minute token triggers immediately: the
    // reload can drop the renew response after the server already rotated the
    // jti, stranding the tab on a revoked token — SCEN-CONCURRENT-01 race).
    await managementNav(page).getByRole('button', { name: /^templates$/i }).click()
    await expect(page).toHaveURL(/\/templates/)

    // A page with a real text input: the template catalog search box.
    const searchInput = page.locator('.catalog-filter-toolbar__search input')
    await expect(searchInput).toBeVisible({ timeout: 30_000 })

    const draftText = `E2E renewal draft ${Date.now().toString(36).toUpperCase()}`
    await searchInput.click()
    await searchInput.fill(draftText)
    await expect(searchInput).toHaveValue(draftText)

    const urlBefore = page.url()
    const tokenBefore = await page.evaluate(() => localStorage.getItem('docgen.accessToken'))
    expect(tokenBefore).toBeTruthy()

    // Only renew requests issued AFTER the user started editing count as the
    // silent renewal under test (the scheduler also renews at shell mount).
    const renewRequests = trackRenewRequests(page)
    const waitStartedAt = Date.now()

    // Tick is 60s; keep pointer activity fresh (< 5min window) while waiting
    // up to 120s. Mouse moves must not touch the search input.
    while (renewRequests.length === 0 && Date.now() - waitStartedAt < 120_000) {
      await page.waitForTimeout(3_000)
      const step = Math.floor((Date.now() - waitStartedAt) / 3_000)
      await page.mouse.move(420 + (step % 4) * 15, 420 + (step % 3) * 10)
    }
    expect(
      renewRequests.length,
      'no POST /auth/renew was issued within 120s of active editing',
    ).toBeGreaterThan(0)

    const renewResponse = await renewRequests[0].response()
    expect(renewResponse, 'renew request never received a response').not.toBeNull()
    expect(renewResponse!.status()).toBe(200)
    const renewSecondsAfterEditing = Math.round((Date.now() - waitStartedAt) / 1000)
    const renewBody = (await renewResponse!.json()) as {
      result?: { accessToken?: string; session?: { absoluteSessionExpiresAt?: string } }
    }
    expect(renewBody.result?.accessToken).toBeTruthy()
    expect(renewBody.result?.session?.absoluteSessionExpiresAt).toBeTruthy()
    console.log(
      `[LRP-B6 Part A] renew 200 observed ${renewSecondsAfterEditing}s after editing started`,
    )

    // SCEN-UX-01: fully silent — same URL, no login redirect, editing state
    // byte-identical, no error toast.
    expect(page.url()).toBe(urlBefore)
    await expect(page).not.toHaveURL(/\/login/)
    await expect(searchInput).toHaveValue(draftText)
    await expect(page.locator('.el-message--error')).toHaveCount(0)

    // The stored token was swapped in place (observable renewal evidence).
    await expect
      .poll(() => page.evaluate(() => localStorage.getItem('docgen.accessToken')), {
        timeout: 10_000,
      })
      .not.toBe(tokenBefore)

    // A follow-up protected call succeeds: the renewed token is live (the old
    // jti is revoked server-side, so any 200 here proves the swap worked).
    await searchInput.fill('')
    const demoRow = page.locator('.el-table__row').filter({ hasText: 'DEMO-RETAIL-LETTER' })
    await expect(demoRow).toBeVisible({ timeout: 15_000 })
    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        /\/api\/management\/v1\/templates\/[0-9a-f-]{36}/i.test(response.url()),
      { timeout: 30_000 },
    )
    await demoRow.click()
    const detailResponse = await detailResponsePromise
    expect(detailResponse.status()).toBe(200)
    await expect(page).toHaveURL(/\/templates\/[0-9a-f-]{36}/i)
  })
})

test.describe('LRP-B6 Part B — near-limit reminder with i18n and sign-in hand-off (SCEN-UX-02)', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    test.skip(
      RENEWAL_PART !== 'B',
      'Part B runs only with RENEWAL_PART=B while docgen-backend uses SESSION_ABSOLUTE_TTL=PT9M',
    )
    test.skip(
      !(await stackReady(request)),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )
    const windows = await measureSessionWindows(request)
    test.skip(
      !(windows.absoluteMsLeft > 0 && windows.absoluteMsLeft < REMINDER_WINDOW_MS),
      `Backend must run with SESSION_ABSOLUTE_TTL=PT9M so a fresh session is already inside the reminder window ` +
        `(measured deadline in ${Math.round(windows.absoluteMsLeft / 1000)}s)`,
    )
  })

  test('banner shows spec copy in en + zh-CN, suppresses renewal, and routes to login with redirect', async ({
    page,
  }) => {
    const renewRequests = trackRenewRequests(page)

    await loginAs(page, E2E_ADMIN)

    // Banner appears immediately: deadline (9min) is already inside the
    // 10-minute reminder window at first evaluation.
    const reminder = page.locator('.session-limit-reminder')
    await expect(reminder).toBeVisible({ timeout: 15_000 })
    await expect(reminder).toHaveAttribute('role', 'alert')

    const en = REMINDER_COPY.en
    await expect(reminder.getByText(en.title, { exact: true })).toBeVisible()
    await expect(reminder.getByText(en.message, { exact: true })).toBeVisible()
    await expect(reminder.getByRole('button', { name: en.action })).toBeVisible()

    // Reminder suppresses silent renewal: observe > one 60s scheduler tick
    // with fresh user activity and assert no POST /auth/renew fires.
    const observationStartedAt = Date.now()
    let step = 0
    while (Date.now() - observationStartedAt < 75_000) {
      await page.waitForTimeout(5_000)
      step += 1
      await page.mouse.move(500 + (step % 5) * 12, 380 + (step % 3) * 14)
    }
    expect(
      renewRequests.map((request) => request.url()),
      'renewal must stay suspended while the reminder is visible',
    ).toHaveLength(0)

    // Non-blocking: still signed in, banner persists.
    await expect(reminder).toBeVisible()
    await expect(page).not.toHaveURL(/\/login/)

    // zh-CN copy after the shell locale switch.
    await switchLocale(page, 'zh-CN')
    const zh = REMINDER_COPY['zh-CN']
    await expect(reminder.getByText(zh.title, { exact: true })).toBeVisible()
    await expect(reminder.getByText(zh.message, { exact: true })).toBeVisible()
    await expect(reminder.getByRole('button', { name: zh.action })).toBeVisible()

    // Action button hands off to the sign-in-again flow, preserving the
    // return destination (COR-F03 redirect semantics).
    const pathBeforeAction = new URL(page.url()).pathname
    await reminder.getByRole('button', { name: zh.action }).click()
    await expect(page).toHaveURL(/\/login\?/, { timeout: 15_000 })
    const redirectParam = new URL(page.url()).searchParams.get('redirect')
    expect(redirectParam, 'login URL must carry the redirect query').toBeTruthy()
    expect(redirectParam).toContain(pathBeforeAction)
    await expect(page.getByPlaceholder('10000001')).toBeVisible()
  })
})
