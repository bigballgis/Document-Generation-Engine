/**
 * Historical IBL-E4 (#131) catalog journeys are superseded by ADR-0071 / SYS-NORM Wave 6.
 *
 * Product DocumentBrand / LegalEntity surfaces are hard-retired. Canonical user-journey
 * coverage lives in `SYS-NORM-W6-d1-brands.spec.ts` (BDD-SYS-NORM-D1-*).
 *
 * This file keeps a thin regression that the obsolete catalog UI is gone, so CI does not
 * resurrect create/bind/allow-list Playwright flows against retired APIs.
 *
 * Run:
 *   pnpm -C frontend exec playwright test e2e/ibl-e4-entity-document-brands.spec.ts `
 *     --config playwright.docker.config.ts
 */
import { expect, test } from '@playwright/test'

import { loginAsGlobalAdmin } from './helpers/auth'
import {
  expectDocumentBrandListRetired,
  expectLegalEntityListRetired,
} from './helpers/ibl-e4-document-brand-api'
import { requireDockerStack } from './helpers/stack-readiness'

test.describe('IBL-E4 entity document brands UI (superseded — Wave 6 hard retire)', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      skipMessage:
        'Stack required (:4173 + :8080). Start with .\\scripts\\docker-deploy-queue.ps1',
    })
  })

  test('legacy catalog routes serve honest retired surface (not IBL-E4 create UI)', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)

    await page.goto('/governance/document-brands')
    await expect(page).not.toHaveURL(/\/forbidden/)
    await expect(page.getByTestId('surface-retired-view')).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText(/document brands catalog retired/i)).toBeVisible()
    await expect(page.getByTestId('document-brand-create-open')).toHaveCount(0)

    await page.goto('/governance/legal-entities')
    await expect(page).not.toHaveURL(/\/forbidden/)
    await expect(page.getByTestId('surface-retired-view')).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText(/legal entities catalog retired/i)).toBeVisible()
    await expect(page.getByTestId('legal-entity-create-open')).toHaveCount(0)
  })

  test('retired management list APIs return stable SURFACE_RETIRED codes', async ({
    request,
  }) => {
    await expectDocumentBrandListRetired(request)
    await expectLegalEntityListRetired(request)
  })
})
