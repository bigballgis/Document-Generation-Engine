/**
 * SYS-NORM Wave 6 / #150 — D1 DocumentBrand / LegalEntity hard retirement (ADR-0071).
 *
 * BDD SoT: docs/behavior/sys-norm-d1-brands.md
 *   BDD-SYS-NORM-D1-001 / 003 / 006 / 007 / 016 / 017 (+ nav continuity from W1)
 *
 * Functional journeys only — UIUX polish is stage 7.
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/SYS-NORM-W6-d1-brands.spec.ts `
 *     --config playwright.docker.config.ts
 */
import { expect, test, type APIRequestContext, type Locator, type Page } from '@playwright/test'

import {
  DEMO_MASTER_NAME,
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_ADMIN,
  E2E_DOCUMENT_AUTHOR,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { findTemplateByExternalId } from './helpers/content-modules-api'
import {
  expectDocumentBrandListRetired,
  expectLegalEntityListRetired,
} from './helpers/ibl-e4-document-brand-api'
import { demoMasterDetailPath } from './helpers/masters-api'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import { dismissOnboardingTourIfPresent, switchBrand } from './helpers/uiux-evidence'

const ACTIVITY_LOG = /^activity log$/i
const LEGAL_HOLDS = /^legal holds$/i
const DOCUMENT_BRANDS = /^document brands$/i
const LEGAL_ENTITIES = /^legal entities$/i
const LETTERHEAD_TEMPLATES = /^letterhead templates$/i

const RETIRED_BRANDS_TITLE = /document brands catalog retired/i
const RETIRED_ENTITIES_TITLE = /legal entities catalog retired/i

function navItemButton(nav: Locator, label: RegExp): Locator {
  return nav.getByRole('button', { name: label })
}

async function requireDemoRetailTemplate(
  request: APIRequestContext,
): Promise<{ templateId: string }> {
  const template = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!template) {
    throw new Error(
      `Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" was not found. Ensure DOCGEN_SEED_DEMO_CATALOG=true.`,
    )
  }
  return { templateId: template.id }
}

async function expectHonestRetiredSurface(
  page: Page,
  path: '/governance/document-brands' | '/governance/legal-entities',
  title: RegExp,
) {
  await page.goto(path)
  // Not soft-hidden Forbidden — honest gone/retired product messaging.
  await expect(page).not.toHaveURL(/\/forbidden/, { timeout: 15_000 })
  await expect(page.getByTestId('surface-retired-view')).toBeVisible({ timeout: 20_000 })
  await expect(page.getByText(title)).toBeVisible()
  // Catalog product UI must not be served.
  await expect(page.getByTestId('document-brand-create-open')).toHaveCount(0)
  await expect(page.getByTestId('legal-entity-create-open')).toHaveCount(0)
  await expect(page.getByTestId('document-brand-table')).toHaveCount(0)
  await expect(page.getByTestId('legal-entity-table')).toHaveCount(0)
}

test.describe('SYS-NORM Wave 6 — D1 DocumentBrand / LegalEntity retirement', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      skipMessage:
        'Stack required (:4173 + :8080). Start with .\\scripts\\docker-deploy-queue.ps1',
    })
  })

  test('BDD-SYS-NORM-D1-001/nav: Security has Legal holds; no Document brands / Legal entities', async ({
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
    await expect(navItemButton(nav, LETTERHEAD_TEMPLATES)).toBeVisible()
  })

  test('BDD-SYS-NORM-D1-006: legacy brand/entity bookmarks show honest retired surface', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)

    await expectHonestRetiredSurface(page, '/governance/document-brands', RETIRED_BRANDS_TITLE)
    await expect(page.getByTestId('surface-retired-letterhead-link')).toBeVisible()
    await expect(page.getByTestId('surface-retired-letterhead-link')).toHaveAttribute(
      'href',
      /\/masters/,
    )

    await expectHonestRetiredSurface(page, '/governance/legal-entities', RETIRED_ENTITIES_TITLE)
    await expect(page.getByTestId('surface-retired-legal-holds-link')).toBeVisible()
    await expect(page.getByTestId('surface-retired-legal-holds-link')).toHaveAttribute(
      'href',
      /\/governance\/legal-holds/,
    )
  })

  test('BDD-SYS-NORM-D1-003: shell REDBC/GREENBC theme switch remains orthogonal', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')

    // Theme switch must not resurrect DocumentBrand MDM catalog.
    await page.goto('/governance/document-brands')
    await expect(page.getByTestId('surface-retired-view')).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText(RETIRED_BRANDS_TITLE)).toBeVisible()

    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')
    await expect(page.getByTestId('surface-retired-view')).toBeVisible()
  })

  test('BDD-SYS-NORM-D1-007: template overview has no DocumentBrand allow-list UI', async ({
    page,
    request,
  }) => {
    const fixture = await requireDemoRetailTemplate(request)
    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })

    // Wave 2 hub: overview is Properties drawer (legacy ?tab=overview still opens it).
    await page.goto(`/templates/${fixture.templateId}?tab=overview`)
    const drawer = page.getByTestId('template-properties-drawer')
    await expect(drawer).toBeVisible({ timeout: 20_000 })
    await expect(drawer.getByTestId('template-overview-summary')).toBeVisible()

    await expect(drawer.getByTestId('template-overview-document-brand-allow-list')).toHaveCount(0)
    await expect(
      drawer.getByTestId('template-overview-document-brand-allow-list-edit'),
    ).toHaveCount(0)
    await expect(drawer.getByTestId('template-document-brand-allow-list-select')).toHaveCount(0)
    await expect(drawer.getByText(/any active document brand/i)).toHaveCount(0)
    await expect(drawer.getByText(/document brand allow-list/i)).toHaveCount(0)
  })

  test('BDD-SYS-NORM-D1-016: Legal holds remain reachable and usable', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })

    const nav = managementNav(page)
    await expect(navItemButton(nav, LEGAL_HOLDS)).toBeVisible()

    // Deep-link proves product surface (nav click can be blocked by onboarding tour mask).
    await page.goto('/governance/legal-holds')
    await expect(page).toHaveURL(/\/governance\/legal-holds/, { timeout: 15_000 })
    await expect(page).not.toHaveURL(/\/forbidden/)
    await expect(page.getByRole('heading', { level: 1, name: LEGAL_HOLDS })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByText(/unable to load legal holds/i)).toHaveCount(0)

    // Core create affordance (empty or header) remains — not folded into brand retirement.
    const emptyCreate = page.getByTestId('legal-hold-create-open-empty')
    const headerCreate = page.getByTestId('legal-hold-create-open')
    const createVisible =
      (await emptyCreate.isVisible().catch(() => false)) ||
      (await headerCreate.isVisible().catch(() => false))
    expect(createVisible, 'Legal holds create control should remain available').toBe(true)
  })

  test('BDD-SYS-NORM-D1-017: Letterhead (master) remains logo/seal governance path', async ({
    page,
    request,
  }) => {
    const masterPath = await demoMasterDetailPath(request)
    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })

    // Retired brands page points operators to Letterhead hub (not DocumentBrand MDM).
    await page.goto('/governance/document-brands')
    await expect(page.getByTestId('surface-retired-letterhead-link')).toBeVisible({
      timeout: 20_000,
    })
    await page.getByTestId('surface-retired-letterhead-link').click()
    await expect(page).toHaveURL(/\/masters/, { timeout: 15_000 })
    await expect(page).not.toHaveURL(/document-brands|legal-entities/)
    await expect(
      page.getByRole('heading', { name: /^(masters|letterhead templates)$/i }),
    ).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText(/unable to load letterheads/i)).not.toBeVisible()

    // Demo letterhead detail remains a product workspace (smoke).
    await page.goto(masterPath)
    await expect(page).toHaveURL(/\/masters\/[^/?]+$/, { timeout: 20_000 })
    await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page).not.toHaveURL(/document-brands/)
  })

  test('BDD-SYS-NORM-D1-009/010: management brand/entity list APIs fail-closed', async ({
    request,
  }) => {
    await expectDocumentBrandListRetired(request)
    await expectLegalEntityListRetired(request)
  })
})
