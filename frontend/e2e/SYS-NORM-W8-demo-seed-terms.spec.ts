/**
 * SYS-NORM Wave 8 / TM #152 — Demo/验收 seed · honest empty · L1 Letterhead/母版
 *
 * BDD SoT: docs/behavior/sys-norm-demo-seed-terms.md
 *   BDD-SYS-NORM-W8-001 — Asset library honest empty
 *   BDD-SYS-NORM-W8-005 — Legal hold empty (manage)
 *   BDD-SYS-NORM-W8-007 / 008 — L1 EN Letterhead / ZH 母版
 *   BDD-SYS-NORM-W8-012 — Master revision empty design summary
 *   BDD-SYS-NORM-W8-013 — Role journey timeline honest empty guidance
 *
 * Acceptance stack (Stage 5/6): FRONTEND_PORT=4173 + backend :8080
 * Seed OFF expected: DOCGEN_SEED_DEMO_ASSET_LIBRARY=false
 *
 * Run:
 *   pnpm -C frontend exec playwright test e2e/SYS-NORM-W8-demo-seed-terms.spec.ts `
 *     --config playwright.docker.config.ts
 */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import {
  E2E_ADMIN,
  E2E_DOCUMENT_AUTHOR,
  E2E_LEGAL_REVIEWER,
  loginAs,
} from './helpers/auth'
import {
  disableAllActiveLibraryAssetsViaApi,
  listLibraryAssetsViaApi,
} from './helpers/library-assets-api'
import {
  listLegalHoldsViaApi,
  releaseLegalHoldViaApi,
} from './helpers/legal-holds-api'
import { createDraftMasterForHubSubmit } from './helpers/masters-api'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import { selectElementPlusOption } from './helpers/ui'
import { dismissOnboardingTourIfPresent, switchLocale } from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function releaseAllActiveLegalHolds(request: APIRequestContext): Promise<number> {
  let released = 0
  for (let guard = 0; guard < 20; guard += 1) {
    const page = await listLegalHoldsViaApi(request, { status: 'ACTIVE', size: 50 })
    if (page.content.length === 0) {
      break
    }
    for (const hold of page.content) {
      await releaseLegalHoldViaApi(request, hold.id)
      released += 1
    }
  }
  return released
}

async function openAssetLibrary(page: Page) {
  await page.goto('/library/assets')
  await expect(page).not.toHaveURL(/\/forbidden/)
  await expect(page.getByRole('heading', { level: 1, name: /^asset library$/i })).toBeVisible({
    timeout: 20_000,
  })
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  await expect(page.getByText(/unable to load the asset library/i)).toHaveCount(0)
}

async function openLegalHolds(page: Page) {
  await page.goto('/governance/legal-holds')
  await expect(page).not.toHaveURL(/\/forbidden/)
  await expect(page.getByRole('heading', { level: 1, name: /^legal holds$/i })).toBeVisible({
    timeout: 20_000,
  })
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  await expect(page.getByText(/unable to load legal holds/i)).toHaveCount(0)
}

/** When RELEASED leftovers remain, narrow to ACTIVE so catalog empty is observable. */
async function ensureLegalHoldCatalogEmptyView(page: Page) {
  const honestEmpty = page.getByTestId('legal-hold-honest-empty')
  if (await honestEmpty.isVisible().catch(() => false)) {
    return
  }
  const statusFilter = page.getByTestId('legal-hold-status-filter')
  await expect(statusFilter).toBeVisible({ timeout: 10_000 })
  await statusFilter.click()
  await selectElementPlusOption(page, /^active$/i)
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  await expect(honestEmpty).toBeVisible({ timeout: 20_000 })
}

test.describe('SYS-NORM Wave 8 — demo seed terms / honest empty / L1 Letterhead', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-SYS-NORM-W8-001: Asset library shows honest empty + Upload CTA when seed off', async ({
    page,
    request,
  }) => {
    await disableAllActiveLibraryAssetsViaApi(request)
    const active = await listLibraryAssetsViaApi(request, { status: 'ACTIVE' })
    expect(active, 'ACTIVE managed assets must be zero for honest-empty acceptance').toHaveLength(0)

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    await openAssetLibrary(page)

    const honestEmpty = page.getByTestId('asset-library-honest-empty')
    await expect(honestEmpty).toBeVisible({ timeout: 20_000 })
    await expect(honestEmpty.getByText(/no assets yet/i)).toBeVisible()
    await expect(
      honestEmpty.getByText(/no managed library assets are registered yet/i),
    ).toBeVisible()
    await expect(page.getByTestId('asset-library-table')).toHaveCount(0)
    await expect(page.getByTestId('asset-library-upload-open-empty')).toBeVisible()
    await expect(page.getByTestId('empty-state-actions')).toBeVisible()
  })

  test('BDD-SYS-NORM-W8-005: Legal holds empty catalog shows honest empty + Create CTA', async ({
    page,
    request,
  }) => {
    await releaseAllActiveLegalHolds(request)

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    await openLegalHolds(page)
    await ensureLegalHoldCatalogEmptyView(page)

    const honestEmpty = page.getByTestId('legal-hold-honest-empty')
    await expect(honestEmpty).toBeVisible()
    await expect(honestEmpty.getByText(/no legal holds yet/i)).toBeVisible()
    await expect(
      honestEmpty.getByText(/create a legal hold to protect invocation or audit records/i),
    ).toBeVisible()
    await expect(page.getByTestId('legal-hold-table')).toHaveCount(0)
    await expect(page.getByTestId('legal-hold-create-open-empty')).toBeVisible()
    await expect(page.getByTestId('empty-state-actions')).toBeVisible()
  })

  test('BDD-SYS-NORM-W8-007/008: L1 nav + list use Letterhead (en) and 母版 (zh-CN)', async ({
    page,
  }) => {
    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })

    const nav = managementNav(page)
    await expect(nav.getByRole('button', { name: /^letterhead templates$/i })).toBeVisible()
    await expect(nav.getByRole('button', { name: /^master documents$/i })).toHaveCount(0)

    await page.goto('/masters')
    await expect(page.getByRole('heading', { level: 1, name: /^letterhead templates$/i })).toBeVisible(
      { timeout: 20_000 },
    )
    await expect(page.getByText(/unable to load letterheads/i)).not.toBeVisible()
    const enMain = (await page.locator('main.shell-content').innerText()).toLowerCase()
    expect(enMain).not.toMatch(/\bmaster documents\b/)

    await switchLocale(page, 'zh-CN')
    await expect(page.locator('html')).toHaveAttribute('lang', 'zh-CN')
    await expect(nav.getByRole('button', { name: /母版/ })).toBeVisible({ timeout: 15_000 })
    await expect(nav.getByRole('button', { name: /^主文档$/ })).toHaveCount(0)

    await page.goto('/masters')
    await expect(page.getByRole('heading', { level: 1, name: /母版/ })).toBeVisible({
      timeout: 20_000,
    })
    const zhMain = await page.locator('main.shell-content').innerText()
    expect(zhMain).toMatch(/母版/)
    expect(zhMain).not.toMatch(/主文档/)
  })

  test('BDD-SYS-NORM-W8-012: Letterhead revision design tab shows honest empty summary', async ({
    page,
    request,
  }) => {
    const draft = await createDraftMasterForHubSubmit(request, {
      name: `E2E-SYS-NORM-W8-Design ${Date.now()}`,
    })

    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })

    await page.goto(`${draft.currentRevisionPath}?workspaceTab=design`)
    await expect(page).toHaveURL(/\/masters\/[^/]+\/revisions\/[^/?]+/, { timeout: 20_000 })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const honestEmpty = page.getByTestId('master-revision-design-honest-empty')
    await expect(honestEmpty).toBeVisible({ timeout: 20_000 })
    await expect(honestEmpty.getByText(/no design change summary yet/i)).toBeVisible()
    await expect(
      honestEmpty.getByText(/this revision has no change summary/i),
    ).toBeVisible()
  })

  test('BDD-SYS-NORM-W8-013: Role journey timeline shows empty guidance (not silent blank)', async ({
    page,
  }) => {
    // Seeded LEGAL queues are often non-empty on the shared acceptance stack; force an empty
    // work set so currentStepIndex=null surfaces *.empty.guidance inline (N21 / W8-C7).
    await loginAs(page, E2E_LEGAL_REVIEWER)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })

    await page.route('**/api/management/v1/collaboration-work-items**', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.continue()
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-sys-norm-w8-empty-collab' },
          result: [],
        }),
      })
    })
    await page.route('**/api/management/v1/templates**', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.continue()
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-sys-norm-w8-empty-templates' },
          result: {
            content: [],
            page: 0,
            size: 20,
            totalElements: 0,
            totalPages: 0,
          },
        }),
      })
    })

    await page.goto('/dashboard?tab=workflow')
    const journeySection = page.locator('#journey-section')
    await expect(journeySection).toBeVisible({ timeout: 20_000 })

    const honestEmpty = journeySection.getByTestId('journey-timeline-honest-empty')
    if (await honestEmpty.isVisible().catch(() => false)) {
      await expect(honestEmpty).toContainText(/\S/)
      return
    }

    const guidance = journeySection.locator('[data-journey-guidance]')
    await expect(guidance).toBeVisible({ timeout: 15_000 })
    await expect(guidance).toContainText(/\S/)
    await expect(journeySection.getByRole('button', { name: /^show help$/i })).toHaveCount(0)
  })
})
