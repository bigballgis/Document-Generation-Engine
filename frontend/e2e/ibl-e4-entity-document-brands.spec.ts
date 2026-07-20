/**
 * IBL-E4 / #131 — DocumentBrand + LegalEntity catalog UI journeys (functional).
 * BDD: docs/behavior/ibl-e4-entity-document-brands.md
 *   - BDD-IBL-E4-001 create document brand
 *   - BDD-IBL-E4-002 / 013 bind entity via brand picker
 *   - BDD-IBL-E4-003 re-bind document brand
 *   - BDD-IBL-E4-011 / allow-list UI (template overview)
 *   - BDD-IBL-E4-012 UI chrome orthogonal to document brand
 *   - BDD-IBL-E4-014 non-admin cannot mutate catalogs
 */
import { expect, test, type Page } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import { prepareRetailTemplateInTesting } from './helpers/collaboration-api'
import {
  createDocumentBrandViaApi,
  getLegalEntityViaApi,
  uniqueE4Code,
} from './helpers/ibl-e4-document-brand-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { selectElementPlusOption } from './helpers/ui'
import { dismissOnboardingTourIfPresent, switchBrand } from './helpers/uiux-evidence'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

const stamp = Date.now().toString(36).toUpperCase()
const brandA = `E2E-HK-${stamp}`
const brandB = `E2E-UK-${stamp}`
const entityCode = `E2E-LE-${stamp}`

function catalogRow(page: Page, tableTestId: string, code: string) {
  return page.getByTestId(tableTestId).locator('.el-table__body tr').filter({ hasText: code })
}

async function selectScopedGroup(
  page: Page,
  select: ReturnType<Page['getByTestId']>,
  groupCode: string,
) {
  const current = (await select.innerText()).trim()
  if (current.includes(groupCode)) {
    return
  }
  await select.click()
  const dropdown = page.locator('.el-select-dropdown:visible')
  await expect(dropdown).toBeVisible()
  await dropdown.getByRole('option', { name: groupCode, exact: true }).click()
  await expect(page.locator('.el-select-dropdown:visible')).toHaveCount(0)
  await expect(select).toContainText(groupCode)
}

async function openDocumentBrands(page: Page) {
  await page.goto('/governance/document-brands')
  await dismissOnboardingTourIfPresent(page)
  await expect(page.getByRole('heading', { name: /^document brands$/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.getByText(/unable to load document brands/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
  await selectScopedGroup(page, page.getByTestId('document-brand-group-filter'), DEMO_GROUP_CODE)
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
  // Seeded PLATFORM_DEFAULT proves the group catalog loaded.
  await expect(catalogRow(page, 'document-brand-table', 'PLATFORM_DEFAULT')).toBeVisible({
    timeout: 30_000,
  })
}

async function openLegalEntities(page: Page) {
  await page.goto('/governance/legal-entities')
  await dismissOnboardingTourIfPresent(page)
  await expect(page.getByRole('heading', { name: /^legal entities$/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.getByText(/unable to load legal entities/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
  await selectScopedGroup(page, page.getByTestId('legal-entity-group-filter'), DEMO_GROUP_CODE)
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
}

async function openBrandSwitcherOptions(page: Page) {
  await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
  const brandSwitcher = page.locator('.brand-switcher')
  await brandSwitcher.click()
  const dropdown = page.locator('.el-select-dropdown:visible, .el-dropdown-menu:visible').first()
  await expect(dropdown).toBeVisible({ timeout: 10_000 })
  return dropdown
}

test.describe('IBL-E4 entity document brands UI', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-IBL-E4-001 — admin creates DocumentBrand; not listed in UI chrome switcher', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openDocumentBrands(page)

    await expect(page.getByTestId('document-brand-create-open')).toBeVisible()
    await page.getByTestId('document-brand-create-open').click()

    const form = page.getByTestId('document-brand-form')
    await expect(form).toBeVisible()
    await selectScopedGroup(page, form.getByTestId('document-brand-group'), DEMO_GROUP_CODE)
    await form.getByTestId('document-brand-code').fill(brandA)
    await form.getByTestId('document-brand-display-name').fill(`HK Retail ${stamp}`)
    await form.getByTestId('document-brand-logo-ref').fill(`e2e/document-brands/${brandA}/logo`)
    await form.getByTestId('document-brand-letterhead-name').fill(`HK Retail Letterhead ${stamp}`)
    await expect(form.getByText(/does not change these bindings/i)).toBeVisible()
    await expect(page.getByTestId('document-brand-submit')).toBeEnabled()
    await page.getByTestId('document-brand-submit').click()

    await expect(page.locator('.el-message').getByText(/document brand created/i)).toBeVisible({
      timeout: 15_000,
    })
    await expect(catalogRow(page, 'document-brand-table', brandA)).toBeVisible({ timeout: 15_000 })
    await expect(catalogRow(page, 'document-brand-table', brandA)).toContainText(/active/i)

    // Document brand codes must not appear as UI BrandPreset options (E4-C1 / E4-C18).
    const chromeOptions = await openBrandSwitcherOptions(page)
    await expect(chromeOptions.getByRole('option', { name: /^(red bank|红色银行)$/i })).toBeVisible()
    await expect(chromeOptions.getByRole('option', { name: /^(green bank|绿色银行)$/i })).toBeVisible()
    await expect(chromeOptions.getByText(brandA)).toHaveCount(0)
    await page.keyboard.press('Escape')
  })

  test('BDD-IBL-E4-002 / 013 — LegalEntity create uses ACTIVE document brand picker', async ({
    page,
    request,
  }) => {
    // Second ACTIVE brand for re-bind + picker coverage (seed via API).
    await createDocumentBrandViaApi(request, {
      documentBrandCode: brandB,
      displayName: `UK Corp ${stamp}`,
    })

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openLegalEntities(page)

    await expect(page.getByTestId('legal-entity-default-panel')).toBeVisible()
    await page.getByTestId('legal-entity-create-open').click()

    const form = page.getByTestId('legal-entity-form')
    await expect(form).toBeVisible()
    await selectScopedGroup(page, form.getByTestId('legal-entity-group'), DEMO_GROUP_CODE)
    await form.getByTestId('legal-entity-code').fill(entityCode)
    await form.getByTestId('legal-entity-display-name').fill(`HK Entity ${stamp}`)

    const brandSelect = form.getByTestId('legal-entity-document-brand')
    await brandSelect.click()
    const dropdown = page.locator('.el-select-dropdown:visible')
    await expect(dropdown).toBeVisible()
    // Picker lists ACTIVE document brands only — not REDBC/GREENBC chrome codes.
    await expect(dropdown.getByText(brandA)).toBeVisible()
    await expect(dropdown.getByText(brandB)).toBeVisible()
    await expect(dropdown.getByText(/^REDBC$/)).toHaveCount(0)
    await expect(dropdown.getByText(/^GREENBC$/)).toHaveCount(0)
    await expect(form.getByText(/ui themes are not listed here/i)).toBeVisible()
    await selectElementPlusOption(page, new RegExp(brandA))

    await page.getByTestId('legal-entity-submit').click()
    await expect(page.locator('.el-message').getByText(/legal entity created/i)).toBeVisible({
      timeout: 15_000,
    })

    const row = catalogRow(page, 'legal-entity-table', entityCode)
    await expect(row).toBeVisible({ timeout: 15_000 })
    await expect(row).toContainText(brandA)

    const persisted = await getLegalEntityViaApi(request, entityCode)
    expect(persisted.documentBrandCode).toBe(brandA)
    expect(persisted.groupCode).toBe(DEMO_GROUP_CODE)
  })

  test('BDD-IBL-E4-003 — admin rebinds LegalEntity document brand', async ({ page, request }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openLegalEntities(page)

    const row = catalogRow(page, 'legal-entity-table', entityCode)
    await expect(row).toBeVisible({ timeout: 15_000 })
    await row.getByTestId('legal-entity-edit').click()

    const form = page.getByTestId('legal-entity-form')
    await expect(form).toBeVisible()
    await form.getByTestId('legal-entity-document-brand').click()
    await selectElementPlusOption(page, new RegExp(brandB))
    await page.getByTestId('legal-entity-submit').click()

    await expect(page.locator('.el-message').getByText(/legal entity updated/i)).toBeVisible({
      timeout: 15_000,
    })
    await expect(catalogRow(page, 'legal-entity-table', entityCode)).toContainText(brandB)

    const persisted = await getLegalEntityViaApi(request, entityCode)
    expect(persisted.documentBrandCode).toBe(brandB)
  })

  test('BDD-IBL-E4-012 — UI chrome REDBC/GREENBC orthogonal to entity→document brand bind', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')

    await openLegalEntities(page)
    const row = catalogRow(page, 'legal-entity-table', entityCode)
    await expect(row).toBeVisible({ timeout: 15_000 })
    await expect(row).toContainText(brandB)

    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    await expect(catalogRow(page, 'legal-entity-table', entityCode)).toContainText(brandB)

    const persisted = await getLegalEntityViaApi(request, entityCode)
    expect(persisted.documentBrandCode).toBe(brandB)
  })

  test('BDD-IBL-E4-011 — template author configures document brand allow-list', async ({
    page,
    request,
  }) => {
    const fixture = await prepareRetailTemplateInTesting(request, {
      externalId: uniqueE4Code('E2E-IBL-E4-AL'),
      name: `E2E IBL-E4 allow-list ${stamp}`,
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await page.goto(`/templates/${fixture.templateId}`)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible({ timeout: 30_000 })

    await expect(page.getByTestId('template-overview-document-brand-allow-list')).toBeVisible()
    await expect(page.getByTestId('template-overview-document-brand-allow-list')).toContainText(
      /any active document brand/i,
    )

    const edit = page.getByTestId('template-overview-document-brand-allow-list-edit')
    await expect(edit).toBeVisible({ timeout: 15_000 })
    await edit.getByTestId('template-document-brand-allow-list-select').click()
    const dropdown = page.locator('.el-select-dropdown:visible')
    await expect(dropdown.getByText(brandA)).toBeVisible({ timeout: 15_000 })
    await dropdown.getByRole('option', { name: new RegExp(brandA) }).click()
    // Keep dropdown open for multi-select — select PLATFORM_DEFAULT if present, else close.
    const platformOpt = dropdown.getByRole('option', { name: /PLATFORM_DEFAULT/i })
    if (await platformOpt.isVisible().catch(() => false)) {
      await platformOpt.click()
    }
    await page.keyboard.press('Escape')

    await edit.getByTestId('template-overview-document-brand-allow-list-save').click()
    await expect(
      page.locator('.el-message').getByText(/document brand allow-list updated/i),
    ).toBeVisible({ timeout: 15_000 })

    await expect(page.getByTestId('template-overview-document-brand-allow-list')).toContainText(
      brandA,
    )
    await expect(page.getByTestId('template-overview-document-brand-allow-list')).not.toContainText(
      /any active document brand/i,
    )
  })

  test('BDD-IBL-E4-014 — TEMPLATE_AUTHOR cannot open catalog write surfaces', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)

    await page.goto('/governance/document-brands')
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByTestId('document-brand-create-open')).toHaveCount(0)

    await page.goto('/governance/legal-entities')
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByTestId('legal-entity-create-open')).toHaveCount(0)

    // Admin retains create affordance (regression vs author deny). Forbidden shell may omit user menu.
    await page.context().clearCookies()
    await page.evaluate(() => {
      localStorage.clear()
      sessionStorage.clear()
    })
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await openDocumentBrands(page)
    await expect(page.getByTestId('document-brand-create-open')).toBeVisible()
  })
})
