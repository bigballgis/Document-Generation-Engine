/**
 * Shell sidebar nav icons — Asset Library & Legal Hold (slice nav-missing-icons).
 *
 * BDD SoT: docs/behavior/nav-missing-icons.md
 *   BDD-NAV-ICON-001 / 002 — icons present when visible
 *   BDD-NAV-ICON-003 — items hidden without routes (fail-closed visibility)
 *   BDD-NAV-ICON-004 — sibling icons unchanged
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/nav-missing-icons.spec.ts `
 *     --config playwright.docker.config.ts
 */
import { expect, test, type Locator, type Page } from '@playwright/test'

import { E2E_ADMIN, E2E_AUDIT_ADMIN, loginAs } from './helpers/auth'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'

const ASSET_LIBRARY_LABEL = /^asset library$/i
const LEGAL_HOLDS_LABEL = /^legal holds$/i
const TEMPLATES_LABEL = /^templates$/i
const ACTIVITY_LOG_LABEL = /^activity log$/i

function navItemButton(nav: Locator, label: RegExp): Locator {
  return nav.getByRole('button', { name: label })
}

async function expectNavItemHasIcon(nav: Locator, label: RegExp) {
  const item = navItemButton(nav, label)
  await expect(item).toBeVisible({ timeout: 20_000 })
  await expect(item.locator('.el-icon')).toHaveCount(1)
  await expect(item.locator('.el-icon')).toBeVisible()
}

/** AUDIT_ADMIN may land on a denied default route — do not require shell via loginAs. */
async function signInWithoutShellAssert(
  page: Page,
  credentials: { username: string; password: string },
) {
  await page.context().clearCookies()
  await page.goto('/login', { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.reload({ waitUntil: 'domcontentloaded' })
  await expect(page.getByPlaceholder('10000001')).toBeVisible({ timeout: 15_000 })
  await page.getByPlaceholder('10000001').fill(credentials.username)
  await page.locator('input[type="password"]').fill(credentials.password)
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
}

test.describe('nav-missing-icons — shell sidebar icon chrome', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      skipMessage:
        'Stack required (:4173 + :8080). Start with .\\scripts\\docker-deploy-queue.ps1',
    })
  })

  test('BDD-NAV-ICON-001/002/004: Asset library & Legal holds show icons; siblings keep icons', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)

    const nav = managementNav(page)
    await expect(nav).toBeVisible()

    // BDD-NAV-ICON-001
    await expectNavItemHasIcon(nav, ASSET_LIBRARY_LABEL)

    // BDD-NAV-ICON-002
    await expectNavItemHasIcon(nav, LEGAL_HOLDS_LABEL)

    // BDD-NAV-ICON-004 — already-mapped siblings still render icons
    await expectNavItemHasIcon(nav, TEMPLATES_LABEL)
    await expectNavItemHasIcon(nav, ACTIVITY_LOG_LABEL)
  })

  test('BDD-NAV-ICON-003: AUDIT_ADMIN does not see Asset library or Legal holds nav', async ({
    page,
  }) => {
    await signInWithoutShellAssert(page, E2E_AUDIT_ADMIN)
    await page.goto('/audit')
    await expect(page).not.toHaveURL(/\/forbidden/, { timeout: 15_000 })

    const nav = managementNav(page)
    await expect(nav).toBeVisible()
    await expect(navItemButton(nav, ACTIVITY_LOG_LABEL)).toBeVisible()

    await expect(navItemButton(nav, ASSET_LIBRARY_LABEL)).toHaveCount(0)
    await expect(navItemButton(nav, LEGAL_HOLDS_LABEL)).toHaveCount(0)
  })
})
