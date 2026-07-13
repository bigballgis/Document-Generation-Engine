/**
 * LR-C9 — Unified list states (LoadErrorPanel + role-aware empty CTAs).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C9-list-states.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080 (stage 5 DEPLOY_OK).
 */
import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs, loginAsGlobalAdmin } from './helpers/auth'
import { requireDockerStack } from './helpers/stack-readiness'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('LRP-C9 unified list states', () => {
  test.beforeEach(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL })
  })

  test('LR-C9-A: templates list load failure shows LoadErrorPanel; retry recovers', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)

    let failOnce = true
    await page.route('**/api/management/v1/templates**', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.continue()
        return
      }
      if (failOnce) {
        failOnce = false
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            metadata: { traceId: 'e2e-lrp-c9-templates' },
            error: {
              code: 'INTERNAL_ERROR',
              category: 'SYSTEM',
              retryable: true,
              message: 'Unable to load templates.',
              messageKey: 'templates.error.loadList',
            },
          }),
        })
        return
      }
      // Deterministic recovery payload (author catalog may be empty on live API).
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-lrp-c9-templates-retry' },
          result: {
            content: [
              {
                id: '00000000-0000-4000-8000-0000000000c9',
                externalId: 'E2E-LRP-C9-RETRY',
                groupCode: 'RETAIL',
                name: 'E2E LR-C9 retry recovery',
                lifecycleStatus: 'DRAFT',
                releaseVersion: null,
                releaseVersionCount: 0,
                masterId: '00000000-0000-4000-8000-0000000000m1',
                updatedBy: '10000002',
                updatedAt: '2026-07-10T12:00:00Z',
              },
            ],
            page: 0,
            size: 20,
            totalElements: 1,
            totalPages: 1,
          },
        }),
      })
    })

    await page.goto('/templates')

    await expect(page.getByText(/unable to load templates/i)).toBeVisible({ timeout: 20_000 })
    await expect(page.getByRole('button', { name: /^retry$/i })).toBeVisible()
    await expect(page.locator('.el-result')).toBeVisible()

    await page.getByRole('button', { name: /^retry$/i }).click()

    await expect(page.getByText(/unable to load templates/i)).toHaveCount(0, { timeout: 20_000 })
    await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText('E2E LR-C9 retry recovery')).toBeVisible()
  })

  test('LR-C9-B: empty templates catalog shows create CTA for authors', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)

    await page.route('**/api/management/v1/templates**', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.continue()
        return
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-lrp-c9-templates-empty' },
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

    await page.goto('/templates')

    await expect(page.getByText(/no template packages yet/i)).toBeVisible({ timeout: 20_000 })
    await expect(
      page.locator('[data-testid="empty-state-actions"]').getByRole('button', {
        name: /new template package/i,
      }),
    ).toBeVisible()
  })

  test('LR-C9-A: groups list load failure shows LoadErrorPanel with retry', async ({ page }) => {
    await loginAsGlobalAdmin(page)

    await page.route('**/api/management/v1/groups**', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.continue()
        return
      }
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-lrp-c9-groups' },
          error: {
            code: 'INTERNAL_ERROR',
            category: 'SYSTEM',
            retryable: true,
            message: 'Unable to load groups.',
            messageKey: 'identity.error.loadGroups',
          },
        }),
      })
    })

    await page.goto('/entitlement/groups')

    await expect(page.getByText(/unable to load groups/i)).toBeVisible({ timeout: 20_000 })
    await expect(page.getByRole('button', { name: /^retry$/i })).toBeVisible()
    await expect(page.locator('.el-result')).toBeVisible()
  })
})
