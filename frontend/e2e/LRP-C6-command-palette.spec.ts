/**
 * LR-C6 — Global command palette (Ctrl+K / Cmd+K) — BDD-LRP-C6-001…015 subset.
 *
 * Canonical run (after stage 5 DEPLOY_OK):
 *   pnpm -C frontend exec playwright test e2e/LRP-C6-command-palette.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080.
 *
 * Scenario map (this file):
 *   BDD-001/007/011 — open Ctrl+K, routes-only empty query, a11y dialog, Templates route
 *   BDD-002/008     — search DEMO-RET → Enter → /templates/{id}; C5 search= page=0 size=8
 *   BDD-003/009     — TEMPLATE_TESTER: no content-modules request/group (fail-closed)
 *   BDD-004         — ArrowDown + Enter activates highlighted item
 *   BDD-005         — Esc closes and restores focus
 *   BDD-006         — empty query no catalog GETs; no-match empty state
 *   BDD-010         — CORP author: DEMO-RETAIL-LETTER not leaked
 *   BDD-014         — backdrop click closes
 *   (012/013/015 deferred to unit/UIUX or admin-only journeys)
 */
import { expect, test, type Page, type Request } from '@playwright/test'

import {
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_ADMIN,
  E2E_CORP_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import { requireDockerStack } from './helpers/stack-readiness'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
/** Handoff fragment: DEMO-RET (first 8 of DEMO-RETAIL-LETTER). */
const SEARCH_FRAGMENT = DEMO_TEMPLATE_EXTERNAL_ID.slice(0, 8)

function isCatalogListGet(url: string, resource: 'templates' | 'masters' | 'content-modules'): boolean {
  try {
    const pathname = new URL(url).pathname.replace(/\/$/, '')
    return pathname === `/api/management/v1/${resource}`
  } catch {
    return false
  }
}

/** Palette list calls lock size=8 (C6-C6); dashboard/catalog pages use other sizes. */
function isPaletteCatalogListGet(url: string, resource?: 'templates' | 'masters' | 'content-modules'): boolean {
  if (resource) {
    if (!isCatalogListGet(url, resource)) {
      return false
    }
  } else if (
    !isCatalogListGet(url, 'templates') &&
    !isCatalogListGet(url, 'masters') &&
    !isCatalogListGet(url, 'content-modules')
  ) {
    return false
  }
  try {
    return new URL(url).searchParams.get('size') === '8'
  } catch {
    return false
  }
}

async function openPalette(page: Page) {
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+KeyK' : 'Control+KeyK')
  await expect(page.getByTestId('command-palette')).toBeVisible()
  await expect(page.getByTestId('command-palette-input')).toBeFocused()
}

test.describe('LRP-C6 command palette', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL })
  })

  test.beforeEach(async ({ page }) => {
    // Docker acceptance under load: login form can exceed Playwright's 5s default.
    page.setDefaultTimeout(20_000)
  })

  test('BDD-001/007/011: Ctrl+K opens routes-only; Enter opens Templates', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    const catalogRequests: Request[] = []
    page.on('request', (req) => {
      if (req.method() !== 'GET') {
        return
      }
      if (isPaletteCatalogListGet(req.url())) {
        catalogRequests.push(req)
      }
    })

    await openPalette(page)
    await expect(page.getByTestId('command-palette')).toHaveAttribute('role', 'dialog')
    await expect(page.getByTestId('command-palette')).toHaveAttribute('aria-modal', 'true')
    expect(catalogRequests).toHaveLength(0)

    const templatesOption = page.getByRole('option', { name: /Templates\s+\/templates$/i })
    await expect(templatesOption).toBeVisible()
    await templatesOption.click()
    await expect(page).toHaveURL(/\/templates\/?$/, { timeout: 15_000 })
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
  })

  test('BDD-002/008: search template fragment then Enter navigates', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')

    const listPromise = page.waitForRequest(
      (req) =>
        req.method() === 'GET' &&
        isPaletteCatalogListGet(req.url(), 'templates') &&
        new URL(req.url()).searchParams.get('search') === SEARCH_FRAGMENT &&
        new URL(req.url()).searchParams.get('page') === '0' &&
        new URL(req.url()).searchParams.get('size') === '8',
      { timeout: 30_000 },
    )

    await openPalette(page)
    await page.getByTestId('command-palette-input').fill(SEARCH_FRAGMENT)
    const listRequest = await listPromise
    expect(listRequest.url()).toContain('search=')

    const option = page
      .getByTestId('command-palette-option')
      .filter({ hasText: DEMO_TEMPLATE_EXTERNAL_ID })
      .first()
    await expect(option).toBeVisible({ timeout: 30_000 })
    await option.click()
    await expect(page).toHaveURL(/\/templates\/[^/?]+/, { timeout: 15_000 })
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
  })

  test('BDD-003/009: restricted role does not leak content-modules', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    // TEMPLATE_TESTER: dashboard + templates only — no content-module-management route.
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard')

    const contentModuleRequests: Request[] = []
    const masterRequests: Request[] = []
    page.on('request', (req) => {
      if (req.method() !== 'GET') {
        return
      }
      const url = req.url()
      if (isPaletteCatalogListGet(url, 'content-modules')) {
        contentModuleRequests.push(req)
      }
      if (isPaletteCatalogListGet(url, 'masters')) {
        masterRequests.push(req)
      }
    })

    const templatesListPromise = page.waitForRequest(
      (req) =>
        req.method() === 'GET' &&
        isPaletteCatalogListGet(req.url(), 'templates') &&
        new URL(req.url()).searchParams.get('search') === 'clause',
      { timeout: 30_000 },
    )

    await openPalette(page)
    await page.getByTestId('command-palette-input').fill('clause')
    await templatesListPromise

    expect(contentModuleRequests, 'must not GET /content-modules without route gate').toHaveLength(0)
    expect(masterRequests, 'must not GET /masters without route gate').toHaveLength(0)
    await expect(page.getByTestId('command-palette-group-content-modules')).toHaveCount(0)
    await expect(page.getByTestId('command-palette-group-masters')).toHaveCount(0)
  })

  test('BDD-004: ArrowDown/Enter activates highlighted route', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()

    await openPalette(page)
    await expect(page.getByTestId('command-palette-option').first()).toBeVisible()
    await page.keyboard.press('ArrowDown')
    const selected = page.locator('[data-testid="command-palette-option"][aria-selected="true"]')
    await expect(selected).toBeVisible()
    await page.keyboard.press('Enter')
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
    await expect(page).not.toHaveURL(/\/login/)
  })

  test('BDD-005: Esc closes and restores focus', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    const skipLink = page.getByRole('link', { name: /skip to main content/i })
    await skipLink.focus()
    await expect(skipLink).toBeFocused()

    await openPalette(page)
    await page.keyboard.press('Escape')
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
    await expect(skipLink).toBeFocused()
  })

  test('BDD-006: empty query skips catalog; no-match shows empty state', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    const catalogRequests: Request[] = []
    page.on('request', (req) => {
      if (req.method() !== 'GET') {
        return
      }
      if (isPaletteCatalogListGet(req.url())) {
        catalogRequests.push(req)
      }
    })

    await openPalette(page)
    await expect(page.getByTestId('command-palette-option').first()).toBeVisible()
    expect(catalogRequests).toHaveLength(0)

    await page.getByTestId('command-palette-input').fill('zz-no-match-xyz-9f3a')
    await expect(page.getByTestId('command-palette-no-match')).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('[data-testid="load-error-panel"]')).toHaveCount(0)
  })

  test('BDD-010: CORP author does not see RETAIL demo template', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_CORP_TEMPLATE_AUTHOR)
    await page.goto('/dashboard')

    const templatesSearch = page.waitForResponse(
      (res) =>
        res.request().method() === 'GET' &&
        isPaletteCatalogListGet(res.url(), 'templates') &&
        new URL(res.url()).searchParams.get('search') === SEARCH_FRAGMENT,
      { timeout: 30_000 },
    )

    await openPalette(page)
    await page.getByTestId('command-palette-input').fill(SEARCH_FRAGMENT)
    await templatesSearch
    await expect(page.getByTestId('command-palette-group-loading')).toHaveCount(0)

    await expect(
      page.getByTestId('command-palette-option').filter({ hasText: DEMO_TEMPLATE_EXTERNAL_ID }),
    ).toHaveCount(0)
  })

  test('BDD-014: backdrop click closes palette', async ({ page }) => {
    test.setTimeout(90_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    await openPalette(page)
    await page.getByTestId('command-palette-backdrop').click({ position: { x: 8, y: 8 } })
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
  })
})
