/**
 * LR-C6 UIUX evidence — global command palette (Ctrl+K / Cmd+K).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C6-uiux-evidence.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Screenshots: frontend/e2e/evidence/LRP-C6/screenshots/
 * Manifest:    frontend/e2e/evidence/LRP-C6-uiux-manifest.md
 */
import { expect, test, type Page } from '@playwright/test'

import { DEMO_TEMPLATE_EXTERNAL_ID, E2E_ADMIN, loginAs } from './helpers/auth'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureLrpC6LocatorScreenshot,
  captureLrpC6Screenshot,
  ensureLrpC6EvidenceDirs,
  LRP_C6_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
/** Handoff fragment: DEMO-RET (first 8 of DEMO-RETAIL-LETTER). */
const SEARCH_FRAGMENT = DEMO_TEMPLATE_EXTERNAL_ID.slice(0, 8)

async function openPalette(page: Page): Promise<void> {
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+KeyK' : 'Control+KeyK')
  await expect(page.getByTestId('command-palette')).toBeVisible()
  await expect(page.getByTestId('command-palette-input')).toBeFocused()
}

async function closePalette(page: Page): Promise<void> {
  if ((await page.getByTestId('command-palette').count()) === 0) {
    return
  }
  await page.keyboard.press('Escape')
  await expect(page.getByTestId('command-palette')).toHaveCount(0)
}

test.describe('LRP-C6 command palette UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensureLrpC6EvidenceDirs()
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(LRP_C6_VIEWPORT)
    page.setDefaultTimeout(20_000)
  })

  test('command palette — dual brand empty / search / no-match / keyboard', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible({ timeout: 30_000 })

    // --- REDBC: empty query (routes only) ---
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    await openPalette(page)
    await expect(page.getByTestId('command-palette-option').first()).toBeVisible()
    await expect(page.getByTestId('command-palette')).toHaveAttribute('role', 'dialog')
    await expect(page.getByTestId('command-palette')).toHaveAttribute('aria-modal', 'true')

    await captureLrpC6Screenshot(page, '01-palette-routes-empty-redbc-en-1440x900.png')
    await captureLrpC6LocatorScreenshot(
      page.getByTestId('command-palette'),
      '02-palette-dialog-routes-redbc-en.png',
    )
    await captureLrpC6LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03-brand-header-redbc-en.png',
    )

    // --- REDBC: search hit DEMO-RET ---
    await page.getByTestId('command-palette-input').fill(SEARCH_FRAGMENT)
    const hitOption = page
      .getByTestId('command-palette-option')
      .filter({ hasText: DEMO_TEMPLATE_EXTERNAL_ID })
      .first()
    await expect(hitOption).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('command-palette-group-loading')).toHaveCount(0)

    await captureLrpC6Screenshot(page, '04-palette-search-demo-ret-redbc-en-1440x900.png')
    await captureLrpC6LocatorScreenshot(
      page.getByTestId('command-palette'),
      '05-palette-dialog-search-hit-redbc-en.png',
    )

    // --- REDBC: keyboard highlight (ArrowDown → aria-selected) ---
    await page.keyboard.press('ArrowDown')
    const selected = page.locator('[data-testid="command-palette-option"][aria-selected="true"]')
    await expect(selected).toBeVisible()
    await captureLrpC6LocatorScreenshot(
      page.getByTestId('command-palette'),
      '06-palette-keyboard-highlight-redbc-en.png',
    )

    // --- REDBC: no-match empty ---
    await page.getByTestId('command-palette-input').fill('zz-no-match-xyz-9f3a')
    await expect(page.getByTestId('command-palette-no-match')).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('[data-testid="load-error-panel"]')).toHaveCount(0)
    await captureLrpC6Screenshot(page, '07-palette-no-match-redbc-en-1440x900.png')
    await captureLrpC6LocatorScreenshot(
      page.getByTestId('command-palette'),
      '08-palette-dialog-no-match-redbc-en.png',
    )

    await closePalette(page)

    // --- GREENBC: empty routes ---
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await openPalette(page)
    await expect(page.getByTestId('command-palette-option').first()).toBeVisible()

    await captureLrpC6Screenshot(page, '09-palette-routes-empty-greenbc-en-1440x900.png')
    await captureLrpC6LocatorScreenshot(
      page.getByTestId('command-palette'),
      '10-palette-dialog-routes-greenbc-en.png',
    )
    await captureLrpC6LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '11-brand-header-greenbc-en.png',
    )

    // --- GREENBC: search hit DEMO-RET (brand wash on active option) ---
    await page.getByTestId('command-palette-input').fill(SEARCH_FRAGMENT)
    await expect(
      page.getByTestId('command-palette-option').filter({ hasText: DEMO_TEMPLATE_EXTERNAL_ID }).first(),
    ).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('command-palette-group-loading')).toHaveCount(0)
    await captureLrpC6Screenshot(page, '12-palette-search-demo-ret-greenbc-en-1440x900.png')
    await captureLrpC6LocatorScreenshot(
      page.getByTestId('command-palette'),
      '13-palette-dialog-search-hit-greenbc-en.png',
    )

    await closePalette(page)
  })
})
