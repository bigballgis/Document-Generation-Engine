/**
 * SYS-NORM Wave 1 / #144 UIUX evidence — fluid shell, Security nav trim,
 * Users/Groups EditMore, EntityLink; dual-brand REDBC/GREENBC @1440×900 (+1800 fluid).
 *
 * BDD SoT: docs/behavior/sys-norm-shell-fluid-nav.md (W1-001..014 visual surfaces)
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/SYS-NORM-W1-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Locator, type Page } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_ADMIN,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureSysNormW1LocatorScreenshot,
  captureSysNormW1Screenshot,
  dismissOnboardingTourIfPresent,
  ensureSysNormW1EvidenceDirs,
  switchBrand,
  SYS_NORM_W1_VIEWPORT,
  SYS_NORM_W1_WIDE_VIEWPORT,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const ACTIVITY_LOG = /^activity log$/i
const LEGAL_HOLDS = /^legal holds$/i
const DOCUMENT_BRANDS = /^document brands$/i
const LEGAL_ENTITIES = /^legal entities$/i

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

async function expectFluidLayout(page: Page) {
  const layout = page.locator('.app-page-layout').first()
  await expect(layout).toBeVisible({ timeout: 20_000 })
  await expect(layout).toHaveClass(/app-page-layout--fluid/)
  await expect(layout.locator('.app-page-layout__inner')).toHaveCount(0)
}

async function expectFluidBeyondContainedCap(page: Page) {
  await page.setViewportSize(SYS_NORM_W1_WIDE_VIEWPORT)
  const shellRoot = page.locator('.shell-page-root')
  const layout = page.locator('.app-page-layout').first()
  await expect(shellRoot).toBeVisible()
  await expect(layout).toBeVisible()
  const shellBox = await shellRoot.boundingBox()
  const layoutBox = await layout.boundingBox()
  expect(shellBox, 'shell-page-root geometry').toBeTruthy()
  expect(layoutBox, 'app-page-layout geometry').toBeTruthy()
  if (shellBox && layoutBox) {
    expect(layoutBox.width / shellBox.width).toBeGreaterThan(0.85)
    expect(layoutBox.width).toBeGreaterThan(1440)
  }
}

async function expectNavItemHasRenderedIcon(item: Locator) {
  await expect(item).toBeVisible({ timeout: 20_000 })
  const icon = item.locator('.el-icon').first()
  await expect(icon).toBeVisible()
  const box = await icon.boundingBox()
  expect(box, 'icon bounding box').toBeTruthy()
  expect(box!.width).toBeGreaterThanOrEqual(12)
  expect(box!.height).toBeGreaterThanOrEqual(12)
  await expect(icon.locator('svg')).toHaveCount(1)
}

async function searchTemplatesCatalog(page: Page, query: string) {
  await page.goto('/templates')
  await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible({
    timeout: 20_000,
  })
  const search = page.getByPlaceholder(/search/i)
  await expect(search).toBeVisible({ timeout: 15_000 })
  await search.fill(query)
  await expect(catalogRowByExternalId(page, query)).toBeVisible({ timeout: 30_000 })
}

function catalogRowByExternalId(page: Page, externalId: string): Locator {
  return page.locator('.el-table__body tr').filter({ hasText: externalId })
}

async function assertEditMoreAlignment(actions: Locator) {
  await expect(actions).toBeVisible()
  const edit = actions.getByRole('button', { name: /^edit$/i })
  const more = actions.getByRole('button', { name: /^more$/i })
  await expect(edit).toBeVisible()
  await expect(more).toBeVisible()
  const editBox = await edit.boundingBox()
  const moreBox = await more.boundingBox()
  expect(editBox && moreBox).toBeTruthy()
  expect(Math.abs(editBox!.y - moreBox!.y), 'Edit/More vertical misalignment').toBeLessThanOrEqual(2)
  expect(moreBox!.x, 'More should be to the right of Edit').toBeGreaterThan(editBox!.x)
  const gap = moreBox!.x - (editBox!.x + editBox!.width)
  expect(gap, 'Edit/More gap').toBeGreaterThanOrEqual(0)
  expect(gap, 'Edit/More excessive gap').toBeLessThanOrEqual(24)
}

async function expectNoCriticalAxe(page: Page, include: string, label: string) {
  const results = await new AxeBuilder({ page })
    .include(include)
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((v) => v.impact === 'critical')
  expect(critical, `${label} critical axe`).toEqual([])
}

test.describe('SYS-NORM Wave 1 UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensureSysNormW1EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080).`,
    })
  })

  test('01–04 Security nav trim + icons dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W1_VIEWPORT)
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      const nav = managementNav(page)
      await expect(nav.getByRole('heading', { name: /security/i })).toBeVisible()
      await expect(navItemButton(nav, ACTIVITY_LOG)).toBeVisible()
      await expect(navItemButton(nav, LEGAL_HOLDS)).toBeVisible()
      await expect(navItemButton(nav, DOCUMENT_BRANDS)).toHaveCount(0)
      await expect(navItemButton(nav, LEGAL_ENTITIES)).toHaveCount(0)
      await expectNavItemHasRenderedIcon(navItemButton(nav, ACTIVITY_LOG))
      await expectNavItemHasRenderedIcon(navItemButton(nav, LEGAL_HOLDS))
      await assertNoViewportOverflow(page)
      await expectNoCriticalAxe(page, 'aside.shell-nav', `${brand} shell-nav`)

      const suffix = brand.toLowerCase()
      await captureSysNormW1Screenshot(page, `01-security-nav-shell-${suffix}-1440x900.png`)
      await captureSysNormW1LocatorScreenshot(
        page.locator('aside.shell-nav'),
        `01b-security-nav-crop-${suffix}.png`,
      )
      await captureSysNormW1LocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `01c-brand-header-${suffix}-crop.png`,
      )
    }
  })

  test('05–08 catalog + detail fluid (1440 + 1800) dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W1_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    await switchBrand(page, 'REDBC')
    await searchTemplatesCatalog(page, DEMO_TEMPLATE_EXTERNAL_ID)
    await expectFluidLayout(page)
    await assertNoViewportOverflow(page)
    await captureSysNormW1Screenshot(page, '05-templates-catalog-fluid-redbc-1440x900.png')

    await expectFluidBeyondContainedCap(page)
    await assertNoViewportOverflow(page)
    await captureSysNormW1Screenshot(page, '05b-templates-catalog-fluid-redbc-1800x900.png')

    await catalogRowByExternalId(page, DEMO_TEMPLATE_EXTERNAL_ID).click()
    await expect(page).toHaveURL(/\/templates\/[^/?]+/, { timeout: 20_000 })
    await expectFluidLayout(page)
    await expectFluidBeyondContainedCap(page)
    await assertNoViewportOverflow(page)
    await captureSysNormW1Screenshot(page, '06-template-detail-fluid-redbc-1800x900.png')
    await page.setViewportSize(SYS_NORM_W1_VIEWPORT)
    await expectFluidLayout(page)
    await captureSysNormW1Screenshot(page, '06b-template-detail-fluid-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')
    await page.goto('/templates')
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible({
      timeout: 20_000,
    })
    await expectFluidLayout(page)
    await captureSysNormW1Screenshot(page, '07-templates-catalog-fluid-greenbc-1440x900.png')
    await expectFluidBeyondContainedCap(page)
    await captureSysNormW1Screenshot(page, '07b-templates-catalog-fluid-greenbc-1800x900.png')

    await page.setViewportSize(SYS_NORM_W1_VIEWPORT)
    await searchTemplatesCatalog(page, DEMO_TEMPLATE_EXTERNAL_ID)
    await catalogRowByExternalId(page, DEMO_TEMPLATE_EXTERNAL_ID).click()
    await expect(page).toHaveURL(/\/templates\/[^/?]+/, { timeout: 20_000 })
    await expectFluidLayout(page)
    await expectFluidBeyondContainedCap(page)
    await captureSysNormW1Screenshot(page, '08-template-detail-fluid-greenbc-1800x900.png')
    await captureSysNormW1LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '08b-brand-header-greenbc-crop.png',
    )
  })

  test('09–12 Users/Groups EditMore + EntityLink dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W1_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    await switchBrand(page, 'REDBC')
    await page.goto('/entitlement/users')
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
      timeout: 20_000,
    })
    const userActions = page.getByTestId('table-edit-more-actions').first()
    await assertEditMoreAlignment(userActions)
    const userGroupLink = page.locator('.authorized-groups a.entity-link-cell__link').first()
    await expect(userGroupLink).toBeVisible({ timeout: 30_000 })
    await expect(userGroupLink).toHaveAttribute('href', /\/entitlement\/groups/)
    // No raw UUID as primary entity text in group link
    await expect(userGroupLink).not.toHaveText(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    )
    await assertNoViewportOverflow(page)
    await captureSysNormW1Screenshot(page, '09-users-editmore-entitylink-redbc-1440x900.png')
    await captureSysNormW1LocatorScreenshot(
      userActions,
      '09b-users-editmore-crop-redbc.png',
    )
    await userGroupLink.scrollIntoViewIfNeeded()
    await expect(userGroupLink).toBeVisible()
    await captureSysNormW1LocatorScreenshot(
      userGroupLink,
      '09c-users-entitylink-crop-redbc.png',
    )

    await userActions.getByRole('button', { name: /^more$/i }).click()
    await expect(page.getByRole('menuitem', { name: /enable|disable/i })).toBeVisible()
    await captureSysNormW1Screenshot(page, '09d-users-more-menu-redbc-1440x900.png')
    await page.keyboard.press('Escape')

    await page.goto('/entitlement/groups')
    await expect(page.getByRole('heading', { name: /group management/i })).toBeVisible({
      timeout: 20_000,
    })
    const groupActions = page.getByTestId('table-edit-more-actions').first()
    await assertEditMoreAlignment(groupActions)
    await captureSysNormW1Screenshot(page, '10-groups-editmore-redbc-1440x900.png')
    await captureSysNormW1LocatorScreenshot(
      groupActions,
      '10b-groups-editmore-crop-redbc.png',
    )

    await switchBrand(page, 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')
    await page.goto('/entitlement/users')
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
      timeout: 20_000,
    })
    await assertEditMoreAlignment(page.getByTestId('table-edit-more-actions').first())
    await captureSysNormW1Screenshot(page, '11-users-editmore-entitylink-greenbc-1440x900.png')

    await page.goto('/entitlement/groups')
    await expect(page.getByRole('heading', { name: /group management/i })).toBeVisible({
      timeout: 20_000,
    })
    await assertEditMoreAlignment(page.getByTestId('table-edit-more-actions').first())
    await captureSysNormW1Screenshot(page, '12-groups-editmore-greenbc-1440x900.png')
    await captureSysNormW1LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '12b-brand-header-greenbc-crop.png',
    )
  })

  test('13–14 EntityLink on templates catalog dual-brand', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W1_VIEWPORT)
    await loginAsGlobalAdmin(page)
    await dismissOnboardingTourIfPresent(page)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await searchTemplatesCatalog(page, DEMO_TEMPLATE_EXTERNAL_ID)
      const row = catalogRowByExternalId(page, DEMO_TEMPLATE_EXTERNAL_ID)
      const groupLink = row.getByRole('link', { name: DEMO_GROUP_CODE, exact: true })
      await expect(groupLink).toBeVisible()
      await expect(groupLink).toHaveAttribute('href', /\/entitlement\/groups/)
      const linkBox = await groupLink.boundingBox()
      expect(linkBox, 'EntityLink geometry').toBeTruthy()
      expect(linkBox!.width).toBeGreaterThan(0)
      expect(linkBox!.height).toBeGreaterThan(0)
      await assertNoViewportOverflow(page)
      const suffix = brand.toLowerCase()
      await captureSysNormW1Screenshot(page, `13-templates-entitylink-${suffix}-1440x900.png`)
      await captureSysNormW1LocatorScreenshot(row, `13b-templates-row-entitylink-${suffix}.png`)
    }
  })
})
