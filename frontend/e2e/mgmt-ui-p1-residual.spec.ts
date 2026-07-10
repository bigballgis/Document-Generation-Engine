/**
 * mgmt-ui-p1 / P1-5 — residual D1–D4 + P1 depth journeys on Docker :4173.
 * BDD: docs/requirements/mgmt-ui-defects-behavior-spec.md v1.2.0
 */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAsGlobalAdmin } from './helpers/auth'
import { preparePublishedTemplateWithLockedReference } from './helpers/content-modules-api'

async function resolvePublishedRelease(
  request: APIRequestContext,
): Promise<{ templateId: string; releaseVersion: string; externalId: string }> {
  // DEMO-RETAIL-LETTER may remain DRAFT on some stacks; seed a published E2E- fixture instead.
  const fixture = await preparePublishedTemplateWithLockedReference(request)
  return {
    templateId: fixture.templateId,
    releaseVersion: '1.0.0',
    externalId: fixture.externalId,
  }
}

async function expectShellLayoutFill(page: Page) {
  const shellRoot = page.locator('.shell-page-root')
  await expect(shellRoot).toBeVisible()

  const layout = page.locator('.app-page-layout').first()
  await expect(layout).toBeVisible()
  await expect(layout).toHaveClass(/app-page-layout--panel/)

  const shellBox = await shellRoot.boundingBox()
  const layoutBox = await layout.boundingBox()
  expect(shellBox, 'shell-page-root should have geometry').toBeTruthy()
  expect(layoutBox, 'app-page-layout should have geometry').toBeTruthy()
  if (shellBox && layoutBox) {
    // Panel should occupy nearly the full shell content width (no large right gray gutter).
    expect(layoutBox.width / shellBox.width).toBeGreaterThan(0.85)
  }
}

test.describe('mgmt-ui-p1 residual D1–D4 + P1 (BDD-MGMT-UI)', () => {
  test.describe('D4 + P1-1 login validation', () => {
    // Serialize to avoid concurrent admin logins flaking under Docker rate/session pressure.
    test.describe.configure({ mode: 'serial' })

    test('D4: filled employee id signs in without false username required', async ({ page }) => {
      await page.goto('/login', { waitUntil: 'domcontentloaded' })
      await page.getByPlaceholder('10000001').fill(E2E_ADMIN.username)
      await page.locator('input[type="password"]').fill(E2E_ADMIN.password)
      await page.getByRole('button', { name: /sign in/i }).click()

      await expect(page.getByText(/username is required/i)).toHaveCount(0)
      await expect(page.getByText(/employee id is required/i)).toHaveCount(0)
      await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })
    })

    test('P1-1-A: password with edge whitespace trims before auth (no false required)', async ({
      page,
    }) => {
      await page.goto('/login', { waitUntil: 'domcontentloaded' })
      await page.getByPlaceholder('10000001').fill(E2E_ADMIN.username)
      // type() preserves edge spaces more reliably than fill() for password fields.
      await page.locator('input[type="password"]').click()
      await page.locator('input[type="password"]').press('Control+A')
      await page.locator('input[type="password"]').type(`  ${E2E_ADMIN.password}  `, { delay: 15 })

      const loginRequest = page.waitForRequest(
        (req) =>
          req.method() === 'POST' &&
          /\/api\/management\/v1\/auth\/login/.test(req.url()),
      )
      await page.getByRole('button', { name: /sign in/i }).click()

      const request = await loginRequest
      const payload = request.postDataJSON() as { username?: string; password?: string }
      expect(payload.password).toBe(E2E_ADMIN.password)
      expect(payload.password).not.toMatch(/^\s|\s$/)

      await expect(page.getByTestId('login-password-required')).toHaveCount(0)
      // Trim is the BDD observable; session establish may race under load — accept either
      // successful navigation or a non-validation auth error (not password-required).
      await expect
        .poll(async () => {
          if (!page.url().includes('/login')) {
            return 'navigated'
          }
          const alert = page.locator('.login-alert')
          if ((await alert.count()) > 0 && (await alert.isVisible())) {
            return 'auth-error'
          }
          return 'pending'
        }, { timeout: 20_000 })
        .not.toBe('pending')
    })

    test('P1-1-B: whitespace-only password shows required without auth call', async ({ page }) => {
      await page.goto('/login', { waitUntil: 'domcontentloaded' })
      await page.getByPlaceholder('10000001').fill(E2E_ADMIN.username)
      await page.locator('input[type="password"]').fill('   ')

      let authCalls = 0
      await page.route('**/api/management/v1/auth/login', async (route) => {
        authCalls += 1
        await route.continue()
      })

      await page.getByRole('button', { name: /sign in/i }).click()

      await expect(page.getByTestId('login-password-required')).toBeVisible()
      await expect(page.getByTestId('login-password-required')).toContainText(/password is required/i)
      expect(authCalls).toBe(0)
      await expect(page).toHaveURL(/\/login/)
    })
  })

  test.describe('D1 layout fill', () => {
    test.beforeEach(async ({ page }) => {
      await loginAsGlobalAdmin(page)
    })

    test('dashboard / users / api policies use full-width panel surface', async ({ page }) => {
      for (const path of ['/dashboard', '/entitlement/users', '/api/policies'] as const) {
        await page.goto(path)
        await expect(page.locator('.shell-page-root')).toBeVisible()
        await expectShellLayoutFill(page)
      }
    })

    test('published release detail uses panel surface fill', async ({ page, request }) => {
      test.setTimeout(120_000)
      const release = await resolvePublishedRelease(request)
      await page.goto(`/templates/${release.templateId}/releases/${release.releaseVersion}`)
      await expect(page.getByText(/published release snapshot — read-only/i)).toBeVisible({
        timeout: 30_000,
      })
      await expectShellLayoutFill(page)
    })
  })

  test.describe('D2 + P1-3 release governance surfaces', () => {
    test.describe.configure({ mode: 'serial' })

    let sharedRelease: { templateId: string; releaseVersion: string; externalId: string }

    test.beforeEach(async ({ page }) => {
      await loginAsGlobalAdmin(page)
    })

    test('D2: basics / testing / approval show real read-only content', async ({
      page,
      request,
    }) => {
      test.setTimeout(180_000)
      sharedRelease = await resolvePublishedRelease(request)
      const release = sharedRelease
      await page.goto(`/templates/${release.templateId}/releases/${release.releaseVersion}`)

      await expect(page.getByText(/published release snapshot — read-only/i)).toBeVisible({
        timeout: 30_000,
      })

      const tabs = page.locator('.workspace-tab-shell')
      await expect(tabs.getByRole('tab', { name: /^basics$/i })).toHaveAttribute(
        'aria-selected',
        'true',
      )
      await expect(page.getByText(release.externalId, { exact: true })).toBeVisible()
      await expect(page.getByText(release.releaseVersion, { exact: true }).first()).toBeVisible()

      await tabs.getByRole('tab', { name: /^testing$/i }).click()
      await expect(
        page.getByText(/this published release completed the testing workflow before go-live/i),
      ).toBeVisible()
      await expect(page.getByRole('heading', { name: /^test run history$/i })).toBeVisible()
      await expect(page.locator('.batch-test-history')).toBeVisible()

      await tabs.getByRole('tab', { name: /^approval$/i }).click()
      await expect(
        page.getByText(/approval and go-live decisions are frozen in this published snapshot/i),
      ).toBeVisible()
      await expect(page.getByRole('heading', { name: /workflow audit trail/i })).toBeVisible()
      await expect(page.getByRole('button', { name: /submit for approval|publish/i })).toHaveCount(0)
    })

    test('P1-3-A: Approval shows release-scoped publish-gate checklist (2xx success)', async ({
      page,
    }) => {
      test.setTimeout(90_000)
      expect(sharedRelease, 'D2 seed must run first in this serial suite').toBeTruthy()
      const release = sharedRelease
      const releaseGatePath = new RegExp(
        `/api/management/v1/templates/${release.templateId}/releases/${release.releaseVersion}/publish-gate`,
      )

      const gateResponsePromise = page.waitForResponse(
        (res) => res.request().method() === 'GET' && releaseGatePath.test(res.url()),
      )

      await page.goto(
        `/templates/${release.templateId}/releases/${release.releaseVersion}?workspaceTab=approval`,
      )

      await expect(page.getByText(/current pre-release checks evaluation/i)).toBeVisible({
        timeout: 30_000,
      })
      // Option B labeling: live evaluation of this published release version (not DEV-line gate).
      await expect(
        page.getByText(/live evaluation of this published release version/i),
      ).toBeVisible()
      await expect(page.getByText(/not a historical publish-time snapshot/i)).toBeVisible()

      // No publish/submit actions on read-only release Approval (P1-3-A boundary).
      await expect(page.getByRole('button', { name: /publish|submit for approval/i })).toHaveCount(0)

      const gateResponse = await gateResponsePromise
      expect(
        gateResponse.ok(),
        `release publish-gate must be 2xx for published fixture (got ${gateResponse.status()})`,
      ).toBe(true)

      // Known published release must never surface "template not found" (Option A DEV-gate failure).
      await expect(page.getByText(/template was not found|template not found/i)).toHaveCount(0)
      await expect(page.locator('.publish-gate-readonly .el-result')).toHaveCount(0)

      const gateBody = (await gateResponse.json()) as {
        result?: { items?: Array<{ checkCode?: string; summary?: string }> }
      }
      const items = gateBody.result?.items ?? []
      if (items.length === 0) {
        // P1-3-C: honest empty — not fabricated all-green.
        await expect(page.getByText(/no pre-release checks returned/i)).toBeVisible()
        await expect(page.locator('.publish-gate-readonly .gate-list li')).toHaveCount(0)
      } else {
        // P1-3-A: prefer real checklist rows for published fixture.
        const gateItems = page.locator('.publish-gate-readonly .gate-list li')
        await expect(gateItems.first()).toBeVisible()
        await expect(gateItems).toHaveCount(items.length)
      }
    })

    test('P1-3-B: forced publish-gate failure shows LoadErrorPanel + retry only', async ({
      page,
    }) => {
      test.setTimeout(90_000)
      expect(sharedRelease, 'D2 seed must run first in this serial suite').toBeTruthy()
      const release = sharedRelease

      await page.route(
        `**/api/management/v1/templates/${release.templateId}/releases/${release.releaseVersion}/publish-gate**`,
        async (route) => {
          await route.fulfill({
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify({
              metadata: { traceId: 'e2e-mgmt-ui-p1-publish-gate' },
              error: {
                code: 'INTERNAL_ERROR',
                category: 'SYSTEM',
                retryable: true,
                message: 'Unable to load publish gate.',
                messageKey: 'templates.error.loadPublishGate',
              },
            }),
          })
        },
      )

      await page.goto(
        `/templates/${release.templateId}/releases/${release.releaseVersion}?workspaceTab=approval`,
      )

      await expect(page.getByText(/current pre-release checks evaluation/i)).toBeVisible({
        timeout: 30_000,
      })
      await expect(page.locator('.publish-gate-readonly .el-result')).toBeVisible({
        timeout: 15_000,
      })
      await expect(page.getByRole('button', { name: /^retry$/i })).toBeVisible()
      await expect(page.locator('.publish-gate-readonly .gate-list li')).toHaveCount(0)
      // Forced failure must not invent checklist rows.
      await expect(page.getByText(/no pre-release checks returned/i)).toHaveCount(0)
    })
  })

  test.describe('D3 API policy home', () => {
    test.beforeEach(async ({ page }) => {
      await loginAsGlobalAdmin(page)
    })

    test('alerts-first home renders table or empty CTA (no published-packages catalog)', async ({
      page,
    }) => {
      await page.goto('/api/policies')

      await expect(page.getByRole('heading', { name: /external services overview/i })).toBeVisible()
      await expect(page.getByRole('heading', { name: /^attention items$/i })).toBeVisible()
      await expect(page.getByText(/published packages/i)).toHaveCount(0)

      const errorPanel = page.getByText(/unable to load external access alerts/i)
      const emptyTitle = page.getByText(/no attention items/i)
      const alertsTable = page.locator('.alerts-card .el-table')

      await expect(errorPanel.or(emptyTitle).or(alertsTable.first())).toBeVisible({
        timeout: 20_000,
      })

      if (await emptyTitle.isVisible()) {
        await expect(
          page.getByRole('button', { name: /browse templates/i }).first(),
        ).toBeVisible()
      }
    })

    test('alerts load failure shows LoadErrorPanel with retry', async ({ page }) => {
      await page.route('**/api/management/v1/api-access/alerts**', async (route) => {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            metadata: { traceId: 'e2e-mgmt-ui-p1-alerts' },
            error: {
              code: 'INTERNAL_ERROR',
              category: 'SYSTEM',
              retryable: true,
              message: 'Unable to load external access alerts.',
              messageKey: 'apiPolicy.home.alerts.loadFailed',
            },
          }),
        })
      })

      await page.goto('/api/policies')

      await expect(page.getByText(/unable to load external access alerts/i)).toBeVisible({
        timeout: 15_000,
      })
      await expect(page.getByRole('button', { name: /^retry$/i })).toBeVisible()
      await expect(page.locator('.alerts-card .el-table')).toHaveCount(0)
    })
  })

  test.describe('P1-2 Groups LoadErrorPanel', () => {
    test('smoke: Groups page loads table without dual el-alert error chrome', async ({ page }) => {
      await loginAsGlobalAdmin(page)
      await page.goto('/entitlement/groups')

      await expect(page.getByRole('heading', { name: /group management/i })).toBeVisible()
      await expect(page.getByText(/unable to load groups/i)).toHaveCount(0)
      await expect(page.locator('.el-table').first()).toBeVisible({ timeout: 20_000 })
    })

    test('list load failure shows LoadErrorPanel + retry (not primary el-alert)', async ({
      page,
    }) => {
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
            metadata: { traceId: 'e2e-mgmt-ui-p1-groups' },
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
      // Primary failure surface is el-result (LoadErrorPanel), not a standalone el-alert banner.
      await expect(page.locator('.el-result')).toBeVisible()
      await expect(page.locator('.el-alert--error')).toHaveCount(0)
    })
  })
})
