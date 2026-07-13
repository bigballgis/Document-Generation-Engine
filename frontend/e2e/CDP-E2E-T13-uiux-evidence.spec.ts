import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  createIsolatedTemplatePendingRelease,
  publishSecondReleaseFromClone,
  publishTemplateRelease,
} from './helpers/content-modules-api'
import {
  captureCdpE2eDecisionLocatorScreenshot,
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`
const TASK_ID = 'CDP-E2E-T13' as const

/**
 * CD-E2E-T13 UIUX evidence scaffold — Hub route summary (S1) + default-route impact
 * preview / after-change (S3) @1920 REDBC. Functional assertions live in
 * CDP-E2E-T13-api-package-materialize.spec.ts; this spec captures screenshot artifacts
 * for e2e-uiux-reviewer.
 */
test.describe('CDP-E2E-T13 UIUX evidence — API package materialize Hub @1920 (BDD S1/S3)', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let templateId = ''

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + backend :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture S1 Hub route summary + S3 default-route impact preview (REDBC @1920)', async ({
    page,
    request,
  }) => {
    const fixture = await createIsolatedTemplatePendingRelease(request)
    templateId = fixture.templateId

    await publishTemplateRelease(request, templateId, '1.0.0')
    await publishSecondReleaseFromClone(request, templateId, '1.0.0', '2.0.0')

    await loginAs(page, E2E_GROUP_ADMIN)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    // --- S1 surface: Hub API Access route summary (default still 1.0.0 after second publish) ---
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const routeSummary = page.getByTestId('route-summary-panel')
    await expect(routeSummary).toBeVisible()
    await expect(routeSummary.locator('.path-value')).toContainText(/\/generate/i)

    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-hub-api-access-route-summary-redbc-1920x1080.png',
    )
    await captureCdpE2eDecisionLocatorScreenshot(
      routeSummary,
      TASK_ID,
      '02-route-summary-panel-detail-redbc-1920x1080.png',
    )

    // --- S3 surface: default route change → impact preview dialog → after save ---
    const routeSection = page.locator('#policy-domain-DEFAULT_ROUTE_TARGET')
    await routeSection.scrollIntoViewIfNeeded()
    const routeInput = routeSection.locator('input').first()
    await expect(routeInput).toBeVisible()
    await routeInput.fill('2.0.0')

    const previewPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/api/policy/impact-preview'),
      { timeout: 45_000 },
    )
    const savePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' && response.url().includes('/api/policy/default-route'),
      { timeout: 60_000 },
    )

    await routeSection.getByRole('button', { name: /save default route|保存默认路由/i }).click()
    expect((await previewPromise).ok()).toBeTruthy()

    const impactBox = page.locator('.el-message-box')
    await expect(impactBox).toBeVisible({ timeout: 10_000 })
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '03-default-route-impact-preview-redbc-1920x1080.png',
    )
    await captureCdpE2eDecisionLocatorScreenshot(
      impactBox,
      TASK_ID,
      '04-default-route-impact-preview-dialog-redbc-1920x1080.png',
    )

    await impactBox.getByRole('button', { name: /^(ok|confirm)$/i }).click()
    expect((await savePromise).ok()).toBeTruthy()
    await expect(
      page.locator('.el-message').getByText(/access setting saved|访问设置已保存/i),
    ).toBeVisible({ timeout: 15_000 })

    await page.reload()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.locator('#policy-domain-DEFAULT_ROUTE_TARGET')).toContainText('2.0.0')
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '05-default-route-after-change-2-0-0-redbc-1920x1080.png',
    )
  })
})
