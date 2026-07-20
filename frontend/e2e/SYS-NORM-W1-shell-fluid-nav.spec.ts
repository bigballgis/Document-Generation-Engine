/**
 * SYS-NORM Wave 1 / #144 — shell fluid + Security nav trim + EditMore + EntityLink.
 *
 * BDD SoT: docs/behavior/sys-norm-shell-fluid-nav.md
 *   BDD-SYS-NORM-W1-001..014 (functional journeys)
 *   BDD-SYS-NORM-W1-015/016 — docs/i18n contract (not asserted here)
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/SYS-NORM-W1-shell-fluid-nav.spec.ts `
 *     --config playwright.docker.config.ts
 */
import { expect, test, type Locator, type Page } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'

const ACTIVITY_LOG = /^activity log$/i
const LEGAL_HOLDS = /^legal holds$/i
const DOCUMENT_BRANDS = /^document brands$/i
const LEGAL_ENTITIES = /^legal entities$/i
const TEMPLATES = /^templates$/i
const LETTERHEAD_TEMPLATES = /^letterhead templates$/i
const USERS = /^users$/i
const GROUPS = /^groups$/i
const STANDARD_CLAUSES = /^standard clauses$/i
const ASSET_LIBRARY = /^asset library$/i

function navItemButton(nav: Locator, label: RegExp): Locator {
  return nav.getByRole('button', { name: label })
}

async function expectNavItemHasIcon(nav: Locator, label: RegExp) {
  const item = navItemButton(nav, label)
  await expect(item).toBeVisible({ timeout: 20_000 })
  await expect(item.locator('.el-icon')).toHaveCount(1)
  await expect(item.locator('.el-icon')).toBeVisible()
}

async function expectFluidLayout(page: Page) {
  const layout = page.locator('.app-page-layout').first()
  await expect(layout).toBeVisible({ timeout: 20_000 })
  await expect(layout).toHaveClass(/app-page-layout--fluid/)
  await expect(layout.locator('.app-page-layout__inner')).toHaveCount(0)
}

/**
 * On a wide viewport, fluid content should use shell width beyond the retired 1440px cap.
 */
async function expectFluidBeyondContainedCap(page: Page) {
  await page.setViewportSize({ width: 1800, height: 900 })
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

test.describe('SYS-NORM Wave 1 — shell fluid + nav + EntityLink / Actions', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      skipMessage:
        'Stack required (:4173 + :8080). Start with .\\scripts\\docker-deploy-queue.ps1',
    })
  })

  test('BDD-SYS-NORM-W1-003/005/006: Security nav trimmed; remaining items have icons', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)

    const nav = managementNav(page)
    await expect(nav).toBeVisible()

    await expect(nav.getByRole('heading', { name: /security/i })).toBeVisible()
    await expect(navItemButton(nav, ACTIVITY_LOG)).toBeVisible()
    await expect(navItemButton(nav, LEGAL_HOLDS)).toBeVisible()
    await expect(navItemButton(nav, DOCUMENT_BRANDS)).toHaveCount(0)
    await expect(navItemButton(nav, LEGAL_ENTITIES)).toHaveCount(0)

    await expectNavItemHasIcon(nav, ACTIVITY_LOG)
    await expectNavItemHasIcon(nav, LEGAL_HOLDS)
    await expectNavItemHasIcon(nav, USERS)
    await expectNavItemHasIcon(nav, GROUPS)
    await expectNavItemHasIcon(nav, LETTERHEAD_TEMPLATES)
    await expectNavItemHasIcon(nav, TEMPLATES)
    await expectNavItemHasIcon(nav, STANDARD_CLAUSES)
    await expectNavItemHasIcon(nav, ASSET_LIBRARY)
  })

  test('BDD-SYS-NORM-W1-004: legacy brand/entity routes still resolve outside nav', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)

    const nav = managementNav(page)
    await expect(navItemButton(nav, DOCUMENT_BRANDS)).toHaveCount(0)
    await expect(navItemButton(nav, LEGAL_ENTITIES)).toHaveCount(0)

    await page.goto('/governance/document-brands')
    await expect(page).not.toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByRole('heading', { name: DOCUMENT_BRANDS })).toBeVisible({
      timeout: 30_000,
    })

    await page.goto('/governance/legal-entities')
    await expect(page).not.toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByRole('heading', { name: LEGAL_ENTITIES })).toBeVisible({
      timeout: 30_000,
    })
  })

  test('BDD-SYS-NORM-W1-001/002: catalog + detail workspaces are fluid (no 1440 inner)', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)

    await searchTemplatesCatalog(page, DEMO_TEMPLATE_EXTERNAL_ID)
    await expectFluidLayout(page)
    await expectFluidBeyondContainedCap(page)

    await catalogRowByExternalId(page, DEMO_TEMPLATE_EXTERNAL_ID).click()
    await expect(page).toHaveURL(/\/templates\/[^/?]+/, { timeout: 20_000 })
    await expectFluidLayout(page)
    await expectFluidBeyondContainedCap(page)

    await page.goto('/masters')
    await expect(page.locator('.app-page-layout').first()).toBeVisible({ timeout: 20_000 })
    await expectFluidLayout(page)
    await expectFluidBeyondContainedCap(page)
  })

  test('BDD-SYS-NORM-W1-007/008: Users and Groups share Edit/More; domain commands remain', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)

    await page.goto('/entitlement/users')
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByText(/unable to load users/i)).not.toBeVisible()
    await expect(page.getByTestId('table-edit-more-actions').first()).toBeVisible({
      timeout: 30_000,
    })

    const userActions = page.getByTestId('table-edit-more-actions').first()
    await expect(userActions.getByRole('button', { name: /^edit$/i })).toBeVisible()
    await expect(userActions.getByRole('button', { name: /^more$/i })).toBeVisible()
    await userActions.getByRole('button', { name: /^more$/i }).click()
    await expect(page.getByRole('menuitem', { name: /enable|disable/i })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: /reset password/i })).toBeVisible()
    await page.keyboard.press('Escape')

    await page.goto('/entitlement/groups')
    await expect(page.getByRole('heading', { name: /group management/i })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByText(/unable to load groups/i)).not.toBeVisible()
    await expect(page.getByTestId('table-edit-more-actions').first()).toBeVisible({
      timeout: 30_000,
    })

    const groupActions = page.getByTestId('table-edit-more-actions').first()
    await expect(groupActions.getByRole('button', { name: /^edit$/i })).toBeVisible()
    await expect(groupActions.getByRole('button', { name: /^more$/i })).toBeVisible()
    await groupActions.getByRole('button', { name: /^more$/i }).click()
    await expect(page.getByRole('menuitem', { name: /enable|disable/i })).toBeVisible()
    await page.keyboard.press('Escape')
  })

  test('BDD-SYS-NORM-W1-011/012/013/014: EntityLink groupCode when identity visible', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)

    await searchTemplatesCatalog(page, DEMO_TEMPLATE_EXTERNAL_ID)
    const templateRow = catalogRowByExternalId(page, DEMO_TEMPLATE_EXTERNAL_ID)
    const templateGroupLink = templateRow.getByRole('link', { name: DEMO_GROUP_CODE, exact: true })
    await expect(templateGroupLink).toBeVisible()
    await expect(templateGroupLink).toHaveAttribute('href', /\/entitlement\/groups/)

    await page.goto('/masters')
    await expect(page.locator('.el-table__body tr').first()).toBeVisible({ timeout: 30_000 })
    const masterGroupLink = page
      .getByRole('link', { name: /^(RETAIL|CORP)$/ })
      .first()
    await expect(masterGroupLink).toBeVisible({ timeout: 20_000 })
    await expect(masterGroupLink).toHaveAttribute('href', /\/entitlement\/groups/)

    await page.goto('/content-modules')
    await expect(page.locator('.app-page-layout').first()).toBeVisible({ timeout: 20_000 })
    const moduleGroupLink = page.getByRole('link', { name: /^(RETAIL|CORP)$/ }).first()
    if ((await moduleGroupLink.count()) > 0) {
      await expect(moduleGroupLink).toHaveAttribute('href', /\/entitlement\/groups/)
    } else {
      test.info().annotations.push({
        type: 'note',
        description: 'No RETAIL/CORP group EntityLink on content-modules; catalog may be empty',
      })
    }

    await page.goto('/entitlement/users')
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
      timeout: 20_000,
    })
    const userGroupLink = page.locator('.authorized-groups a.entity-link-cell__link').first()
    await expect(userGroupLink).toBeVisible({ timeout: 30_000 })
    await expect(userGroupLink).toHaveAttribute('href', /\/entitlement\/groups/)
  })

  test('BDD-SYS-NORM-W1-011: template groupCode is plain text without identity route', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)

    const nav = managementNav(page)
    await expect(navItemButton(nav, USERS)).toHaveCount(0)
    await expect(navItemButton(nav, GROUPS)).toHaveCount(0)

    await searchTemplatesCatalog(page, DEMO_TEMPLATE_EXTERNAL_ID)
    const row = catalogRowByExternalId(page, DEMO_TEMPLATE_EXTERNAL_ID)
    await expect(row.getByRole('link', { name: DEMO_GROUP_CODE, exact: true })).toHaveCount(0)
    await expect(row.locator('td').first().locator('.entity-link-cell__text')).toHaveText(
      DEMO_GROUP_CODE,
    )
  })

  test('BDD-SYS-NORM-W1-009/010: task hub EntityLink when rows exist', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await page.goto('/dashboard')
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByText(/unable to load your task list/i)).not.toBeVisible()

    const taskRows = page.locator('.el-table__body tr')
    const rowCount = await taskRows.count()
    if (rowCount === 0) {
      test.info().annotations.push({
        type: 'note',
        description:
          'No task-hub rows on this stack; BDD-009/010 covered by unit tests + catalog EntityLink E2E',
      })
      return
    }

    const firstRow = taskRows.first()
    const itemCell = firstRow.locator('.entity-link-cell').first()
    await expect(itemCell).toBeVisible()
    const itemLink = itemCell.locator('a.entity-link-cell__link')
    const itemText = itemCell.locator('.entity-link-cell__text')
    expect((await itemLink.count()) + (await itemText.count())).toBeGreaterThan(0)

    const groupCell = firstRow.locator('.entity-link-cell').nth(1)
    if ((await groupCell.count()) > 0) {
      const groupLink = groupCell.locator('a.entity-link-cell__link')
      if ((await groupLink.count()) > 0) {
        await expect(groupLink).toHaveAttribute('href', /\/entitlement\/groups/)
      }
    }
  })
})
