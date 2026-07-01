import { expect, test } from '@playwright/test'

import {
  E2E_TEMPLATE_AUTHOR,
  FOL_CLAUSE_CODES,
  FOL_EXPECTED_ANCHOR_COUNT,
  FOL_GROUP_CODE,
  FOL_MASTER_NAME,
  FOL_TEMPLATE_EXTERNAL_ID,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { assertFolCatalogSeeded } from './helpers/fol-api'

test.describe('corporate FOL catalog (demo seed)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsGlobalAdmin(page)
  })

  test('API seed includes FOL template, 40 anchor bindings, and 36 standard clauses', async ({ request }) => {
    const fixture = await assertFolCatalogSeeded(request)
    expect(fixture.externalId).toBe(FOL_TEMPLATE_EXTERNAL_ID)
    expect(fixture.groupCode).toBe(FOL_GROUP_CODE)
    expect(FOL_CLAUSE_CODES).toHaveLength(36)
    expect(FOL_EXPECTED_ANCHOR_COUNT).toBe(40)
  })

  test('master catalog shows FOL letterhead for CORP group', async ({ page }) => {
    await page.goto('/masters')

    await expect(page.getByText(/unable to load master documents/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { name: /^masters$/i })).toBeVisible()
    await expect(page.getByText(FOL_MASTER_NAME)).toBeVisible()
    await expect(page.locator('.el-table').getByText(FOL_GROUP_CODE, { exact: true }).first()).toBeVisible()
  })

  test('template catalog shows FOL offer letter template', async ({ page }) => {
    await page.goto('/templates')

    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByText(FOL_TEMPLATE_EXTERNAL_ID)).toBeVisible()
  })

  test('FOL template hub opens dev editor with variables and content module references', async ({
    page,
    request,
  }) => {
    test.setTimeout(90_000)
    const fixture = await assertFolCatalogSeeded(request)

    await page.goto(`/templates/${fixture.templateId}`)

    await expect(page.getByText(/unable to load template/i)).not.toBeVisible()
    await expect(page.getByText(`External ID: ${FOL_TEMPLATE_EXTERNAL_ID}`)).toBeVisible()
    await expect(page.getByText(/version lines/i)).toBeVisible()

    await page
      .locator('.version-lines-card')
      .getByRole('button', { name: /view detail/i })
      .first()
      .click()

    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/dev/`),
      { timeout: 15_000 },
    )
    await expect(page.locator('.detail-tabs')).toBeVisible({ timeout: 30_000 })

    await page.getByRole('tab', { name: /^template design$/i }).click()
    const authoringTabs = page.locator('.authoring-sub-tabs')
    await expect(authoringTabs.getByRole('tab', { name: /^variables$/i })).toBeVisible({ timeout: 30_000 })

    const variableSearch = page.locator('.variable-tree-panel .search-input input')
    await variableSearch.fill('borrowerLegalName')
    await expect(page.locator('.variable-tree').getByText('LegalName')).toBeVisible({ timeout: 15_000 })

    await authoringTabs.getByRole('tab', { name: /^bindings$/i }).click()
    const anchorFilter = page.locator('.bindings-panel').getByPlaceholder(/filter/i).first()
    await anchorFilter.fill('FOL_SEC_01')
    await expect(
      page.locator('.bindings-panel .el-table').getByText('FOL_SEC_01', { exact: true }),
    ).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('corporate FOL content modules (author scope)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('content module catalog lists FOL standard clauses', async ({ page, request }) => {
    await assertFolCatalogSeeded(request)

    await page.goto('/content-modules')

    await expect(page.getByText(/unable to load content modules/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { name: /^standard clauses$/i })).toBeVisible()

    const groupFilter = page.locator('.group-filter-item').getByRole('combobox')
    await groupFilter.click()
    await page.locator('.el-select-dropdown__item').filter({ hasText: FOL_GROUP_CODE }).first().click()

    await expect(page.locator('.el-table').getByText('MOD-FOL-SEC-01', { exact: true }).first()).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.locator('.el-table__body-wrapper tbody tr')).not.toHaveCount(0)
  })
})
