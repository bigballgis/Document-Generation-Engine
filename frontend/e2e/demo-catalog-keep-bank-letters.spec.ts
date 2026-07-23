/**
 * TM #164 / demo-catalog-keep-bank-letters — keep-set catalog membership smoke.
 *
 * BDD: BDD-DEMO-KEEP-001 (keep-set remains Live),
 *      BDD-DEMO-KEEP-002 (purge-set absent from catalog).
 *
 * Prerequisite: Docker stack up; keep-set cleanup already applied (purge_absent=true).
 * Canonical run with demos:
 *   pnpm -C frontend exec playwright test e2e/demo-catalog-keep-bank-letters.spec.ts e2e/demo-runtime-generate.spec.ts --config playwright.docker.config.ts
 */
import { expect, test, type APIRequestContext } from '@playwright/test'

import { DEMO_PUBLISH_EXTERNAL_IDS } from '@/utils/demoRuntimeRegistry'

import {
  E2E_ADMIN,
  FOL_MASTER_NAME,
  FOL_TEMPLATE_EXTERNAL_ID,
  loginAsGlobalAdmin,
} from './helpers/auth'
import {
  buildCatalogQuery,
  collectCatalogPages,
  E2E_CATALOG_PAGE_SIZE,
  type CatalogPageView,
} from './helpers/catalog-query'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'

/** Sample of purged externalIds — must be absent after keep-set cleanup. */
const PURGE_SAMPLE_EXTERNAL_IDS = [
  'DEMO-FULL-FLOW-LETTER',
  'DEMO-RETAIL-LETTER',
  'DEMO-RETAIL-ACCOUNT-OPEN',
  'DEMO-MORTGAGE-APPROVAL',
  'DEMO-INSURANCE-ENDORSEMENT',
  'DEMO-KYC-CDD-NOTICE',
  'DEMO-ACCOUNT-CLOSURE',
  'DEMO-WEALTH-STATEMENT',
] as const

interface TemplateSummary {
  id: string
  externalId: string
  lifecycleStatus: string
}

interface ApiEnvelope<T> {
  result: T
}

async function apiLoginAdmin(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: E2E_ADMIN,
  })
  expect(response.ok(), `API login failed: ${response.status()}`).toBeTruthy()
  const body = (await response.json()) as ApiEnvelope<{ accessToken: string }>
  return body.result.accessToken
}

async function listAllTemplates(request: APIRequestContext): Promise<TemplateSummary[]> {
  const token = await apiLoginAdmin(request)
  return collectCatalogPages<TemplateSummary>(
    async (page, size) => {
      const response = await request.get(
        `${E2E_API_BASE_URL}/templates${buildCatalogQuery({ page, size })}`,
        { headers: { Authorization: `Bearer ${token}` } },
      )
      expect(response.ok(), `GET /templates failed: ${response.status()}`).toBeTruthy()
      const body = (await response.json()) as ApiEnvelope<CatalogPageView<TemplateSummary> | TemplateSummary[]>
      return body.result
    },
    { pageSize: E2E_CATALOG_PAGE_SIZE },
  )
}

test.describe('Demo catalog keep-set membership (BDD-DEMO-KEEP-001/002)', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      skipMessage: 'Docker stack :4173 + :8080 required for keep-set catalog smoke.',
    })
  })

  test('API catalog contains keep-set of 8 as PUBLISHED and purge sample absent', async ({
    request,
  }) => {
    expect(DEMO_PUBLISH_EXTERNAL_IDS).toHaveLength(8)

    const templates = await listAllTemplates(request)
    const byExternalId = new Map(templates.map((row) => [row.externalId, row]))

    for (const externalId of DEMO_PUBLISH_EXTERNAL_IDS) {
      const row = byExternalId.get(externalId)
      expect(row, `keep-set template missing: ${externalId}`).toBeTruthy()
      expect(row!.lifecycleStatus, `${externalId} must remain PUBLISHED`).toBe('PUBLISHED')
    }

    for (const externalId of PURGE_SAMPLE_EXTERNAL_IDS) {
      expect(
        byExternalId.has(externalId),
        `purge-set template still present: ${externalId}`,
      ).toBe(false)
    }

    // Demo keep-set rows only — ignore E2E-* / LOAD-TPL-* / other non-demo fixtures.
    const demoKeepRows = templates.filter((row) =>
      (DEMO_PUBLISH_EXTERNAL_IDS as readonly string[]).includes(row.externalId),
    )
    expect(demoKeepRows).toHaveLength(8)
  })

  test('UI template catalog lists a keep-set Live template (FOL)', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await page.goto('/templates')

    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { level: 1, name: /^templates$/i })).toBeVisible()

    const search = page.getByRole('textbox', { name: /search/i }).first()
    if (await search.isVisible()) {
      await search.fill(FOL_TEMPLATE_EXTERNAL_ID)
    }

    await expect(page.locator('.el-table').getByText(FOL_TEMPLATE_EXTERNAL_ID).first()).toBeVisible()
  })

  test('UI master catalog lists keep-set FOL letterhead', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await page.goto('/masters')

    await expect(page.getByText(/unable to load letterheads/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { level: 1, name: /^letterhead templates$/i })).toBeVisible()

    const search = page.getByRole('textbox', { name: /search/i }).first()
    if (await search.isVisible()) {
      await search.fill(FOL_MASTER_NAME)
    }

    await expect(page.locator('.el-table').getByText(FOL_MASTER_NAME).first()).toBeVisible()
  })
})
