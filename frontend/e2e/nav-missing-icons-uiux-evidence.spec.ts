/**
 * nav-missing-icons UIUX evidence — shell sidebar icons
 * Asset library (FolderOpened) + Legal holds (Lock); dual-brand @1440×900.
 *
 * BDD SoT: docs/behavior/nav-missing-icons.md
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/nav-missing-icons-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Locator, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureNavMissingIconsLocatorScreenshot,
  captureNavMissingIconsScreenshot,
  dismissOnboardingTourIfPresent,
  ensureNavMissingIconsEvidenceDirs,
  NAV_MISSING_ICONS_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const ASSET_LIBRARY_LABEL = /^asset library$/i
const LEGAL_HOLDS_LABEL = /^legal holds$/i
const TEMPLATES_LABEL = /^templates$/i
const ACTIVITY_LOG_LABEL = /^activity log$/i

function shellAside(page: Page): Locator {
  return page.locator('aside.shell-nav')
}

function navItemButton(nav: Locator, label: RegExp): Locator {
  return nav.getByRole('button', { name: label })
}

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return { scrollWidth: doc.scrollWidth, clientWidth: doc.clientWidth }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function assertPrimaryBrandColor(page: Page, brand: 'REDBC' | 'GREENBC'): Promise<void> {
  const expected = brand === 'REDBC' ? '#db0011' : '#00847f'
  const primary = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim().toLowerCase(),
  )
  expect(primary, `expected --brand-primary ${expected} for ${brand}`).toBe(expected)
}

async function expectNavItemHasRenderedIcon(item: Locator) {
  await expect(item).toBeVisible({ timeout: 20_000 })
  const icon = item.locator('.el-icon').first()
  await expect(icon).toBeVisible()
  const box = await icon.boundingBox()
  expect(box, 'icon bounding box').toBeTruthy()
  expect(box!.width, 'icon width').toBeGreaterThanOrEqual(12)
  expect(box!.height, 'icon height').toBeGreaterThanOrEqual(12)
  // Glyph present: Element Plus icons render an SVG path
  await expect(icon.locator('svg')).toHaveCount(1)
  const pathCount = await icon.locator('svg path').count()
  expect(pathCount, 'svg path glyphs').toBeGreaterThan(0)
}

async function assertExpandedIconAlignment(nav: Locator) {
  const asset = navItemButton(nav, ASSET_LIBRARY_LABEL)
  const templates = navItemButton(nav, TEMPLATES_LABEL)
  const legal = navItemButton(nav, LEGAL_HOLDS_LABEL)
  const activity = navItemButton(nav, ACTIVITY_LOG_LABEL)

  for (const item of [asset, templates, legal, activity]) {
    await expectNavItemHasRenderedIcon(item)
  }

  const [assetBox, templatesBox, legalBox, activityBox] = await Promise.all([
    asset.boundingBox(),
    templates.boundingBox(),
    legal.boundingBox(),
    activity.boundingBox(),
  ])
  expect(assetBox && templatesBox && legalBox && activityBox).toBeTruthy()

  // Same left edge rhythm as sibling nav rows (expanded)
  const lefts = [assetBox!, templatesBox!, legalBox!, activityBox!].map((b) => b.x)
  const minLeft = Math.min(...lefts)
  const maxLeft = Math.max(...lefts)
  expect(maxLeft - minLeft, 'nav item left-edge drift').toBeLessThanOrEqual(2)

  const heights = [assetBox!, templatesBox!, legalBox!, activityBox!].map((b) => b.height)
  const minH = Math.min(...heights)
  const maxH = Math.max(...heights)
  expect(maxH - minH, 'nav item height density drift').toBeLessThanOrEqual(4)

  const [assetIcon, templatesIcon] = await Promise.all([
    asset.locator('.el-icon').first().boundingBox(),
    templates.locator('.el-icon').first().boundingBox(),
  ])
  expect(assetIcon && templatesIcon).toBeTruthy()
  expect(Math.abs(assetIcon!.x - templatesIcon!.x), 'icon column x drift').toBeLessThanOrEqual(2)
  expect(Math.abs(assetIcon!.width - templatesIcon!.width), 'icon size drift').toBeLessThanOrEqual(2)
}

async function expectNoCriticalAxeOnNav(page: Page, label: string) {
  const aside = shellAside(page)
  const results = await new AxeBuilder({ page })
    .include('aside.shell-nav')
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((v) => v.impact === 'critical')
  expect(critical, `${label} critical axe on shell-nav`).toEqual([])
  await expect(aside).toBeVisible()
}

async function ensureExpanded(page: Page) {
  const aside = shellAside(page)
  if (await aside.evaluate((el) => el.classList.contains('shell-nav--collapsed'))) {
    await page.locator('.collapse-btn').click()
  }
  await expect(aside).not.toHaveClass(/shell-nav--collapsed/)
}

async function collapseNav(page: Page) {
  const aside = shellAside(page)
  if (!(await aside.evaluate((el) => el.classList.contains('shell-nav--collapsed')))) {
    await page.locator('.collapse-btn').click()
  }
  await expect(aside).toHaveClass(/shell-nav--collapsed/)
}

async function captureBrandPair(page: Page, brand: 'REDBC' | 'GREENBC') {
  const suffix = brand.toLowerCase()
  const nav = managementNav(page)
  const aside = shellAside(page)

  await ensureExpanded(page)
  await assertExpandedIconAlignment(nav)
  await assertNoViewportOverflow(page)
  await assertPrimaryBrandColor(page, brand)
  await expectNoCriticalAxeOnNav(page, `${brand} expanded`)

  await captureNavMissingIconsScreenshot(page, `01-shell-nav-expanded-${suffix}-1440x900.png`)
  await captureNavMissingIconsLocatorScreenshot(
    aside,
    `01b-shell-nav-aside-expanded-${suffix}.png`,
  )
  await captureNavMissingIconsLocatorScreenshot(
    page.locator('.shell-header .header-brand'),
    `01c-brand-header-${suffix}-crop.png`,
  )

  // Focus crop: Content group (Asset library) + Security (Legal holds)
  const asset = navItemButton(nav, ASSET_LIBRARY_LABEL)
  const legal = navItemButton(nav, LEGAL_HOLDS_LABEL)
  await captureNavMissingIconsLocatorScreenshot(
    asset,
    `02-asset-library-nav-item-expanded-${suffix}.png`,
  )
  await captureNavMissingIconsLocatorScreenshot(
    legal,
    `02b-legal-holds-nav-item-expanded-${suffix}.png`,
  )

  await collapseNav(page)
  // Collapsed: aria-label buttons still expose names
  const assetCollapsed = navItemButton(nav, ASSET_LIBRARY_LABEL)
  const legalCollapsed = navItemButton(nav, LEGAL_HOLDS_LABEL)
  await expect(assetCollapsed).toHaveClass(/nav-item--icon-only/)
  await expect(legalCollapsed).toHaveClass(/nav-item--icon-only/)
  await expectNavItemHasRenderedIcon(assetCollapsed)
  await expectNavItemHasRenderedIcon(legalCollapsed)
  await assertNoViewportOverflow(page)
  await expectNoCriticalAxeOnNav(page, `${brand} collapsed`)

  await captureNavMissingIconsScreenshot(page, `03-shell-nav-collapsed-${suffix}-1440x900.png`)
  await captureNavMissingIconsLocatorScreenshot(
    aside,
    `03b-shell-nav-aside-collapsed-${suffix}.png`,
  )
  await captureNavMissingIconsLocatorScreenshot(
    assetCollapsed,
    `03c-asset-library-icon-only-${suffix}.png`,
  )
  await captureNavMissingIconsLocatorScreenshot(
    legalCollapsed,
    `03d-legal-holds-icon-only-${suffix}.png`,
  )

  // Restore expanded for subsequent brand switch
  await ensureExpanded(page)
}

test.describe('nav-missing-icons UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    ensureNavMissingIconsEvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Acceptance stack required (${FRONTEND_BASE_URL} + :8080).`,
    })
  })

  test('dual-brand expanded + collapsed shell nav icons', async ({ page }) => {
    await page.setViewportSize(NAV_MISSING_ICONS_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    const nav = managementNav(page)
    await expect(nav).toBeVisible()

    await switchBrand(page, 'REDBC')
    await captureBrandPair(page, 'REDBC')

    await switchBrand(page, 'GREENBC')
    await captureBrandPair(page, 'GREENBC')
  })
})
