/**
 * CE-U11 UIUX evidence — invocation release filter + Export CSV + failed error envelope
 * Dual-brand REDBC/GREENBC @1440×900 (Stage 7; P13 pattern).
 * BDD: docs/behavior/ce-u11-invocation-troubleshoot.md (IRC-003/004/007 surfaces)
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  createTemplateApiCredential,
  ensureDemoFullFlowPublished,
  publishSecondReleaseFromClone,
  updateApiPolicyBatchSettings,
} from './helpers/content-modules-api'
import {
  runtimeGenerateByVersion,
  runtimeGenerateContractInvalid,
  waitForManagementInvocationByRequestId,
} from './helpers/management-invocations-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  captureCeU11LocatorScreenshot,
  captureCeU11Screenshot,
  CE_U11_VIEWPORT,
  ensureCeU11EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const RELEASE_V1 = '1.0.0'
const RELEASE_V12 = '1.2.0'

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openInvocationHistory(page: Page, templateId: string) {
  await page.goto(`/templates/${templateId}?tab=apiAccess`)
  await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const panel = page.locator('.section-card').filter({
    has: page.getByRole('heading', { name: /invocation history|调用历史/i }),
  })
  await panel.scrollIntoViewIfNeeded()
  await expect(panel).toBeVisible()
  return panel
}

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return {
      scrollWidth: doc.scrollWidth,
      clientWidth: doc.clientWidth,
    }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

test.describe('CE-U11 invocation troubleshoot UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let templateId = ''
  let failV12RequestId = ''

  test.beforeAll(async ({ request }) => {
    ensureCeU11EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })

    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId
    const externalId = fixture.externalId

    const lines = await listTemplateVersionLines(request, templateId)
    const hasV12 = lines.some(
      (line) => line.lineKind === 'PUBLISHED' && line.releaseVersion === RELEASE_V12,
    )
    if (!hasV12) {
      await publishSecondReleaseFromClone(request, templateId, RELEASE_V1, RELEASE_V12)
    }

    await updateApiPolicyBatchSettings(request, templateId, false, 10)
    const credential = await createTemplateApiCredential(request, templateId)
    const stamp = Date.now().toString(36)

    const successV12 = await runtimeGenerateByVersion(
      request,
      externalId,
      credential,
      RELEASE_V12,
      `ce-u11-uiux-ok-v12-${stamp}`,
    )
    expect(successV12.status).toBe(200)

    const failV12 = await runtimeGenerateContractInvalid(
      request,
      externalId,
      credential,
      RELEASE_V12,
      `ce-u11-uiux-fail-v12-${stamp}`,
    )
    expect(failV12.status).toBeGreaterThanOrEqual(400)
    failV12RequestId = failV12.requestId

    await waitForManagementInvocationByRequestId(request, templateId, successV12.requestId)
    const failRow = await waitForManagementInvocationByRequestId(
      request,
      templateId,
      failV12RequestId,
    )
    expect(failRow.status).toBe('FAILED')
  })

  test('01–04 dual-brand: filters + export + failed error envelope', async ({ page }) => {
    await page.setViewportSize(CE_U11_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    const panel = await openInvocationHistory(page, templateId)
    await expect(panel.getByTestId('invocation-release-version-filter')).toBeVisible()
    await expect(panel.getByText(/release version/i).first()).toBeVisible()
    await expect(panel.getByTestId('invocation-export-csv')).toBeVisible()
    await expect(panel.getByTestId('invocation-export-csv')).toContainText(/export csv/i)

    await panel.getByTestId('invocation-release-version-filter').locator('input').fill(RELEASE_V12)
    await panel.getByTestId('invocation-apply-filters').click()
    await expect(panel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(panel.locator('.invocation-table tbody tr').first()).toBeVisible({
      timeout: 30_000,
    })
    await assertNoViewportOverflow(page)

    await captureCeU11Screenshot(page, '01-invocation-history-filters-redbc-1440x900.png')
    await captureCeU11LocatorScreenshot(
      panel,
      '01b-invocation-panel-crop-redbc-1440x900.png',
    )
    await captureCeU11LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )
    await captureCeU11LocatorScreenshot(
      panel.locator('.filters-actions'),
      '01d-export-csv-actions-crop-redbc.png',
    )

    const failRow = panel.locator('.invocation-table tbody tr').filter({ hasText: failV12RequestId })
    await expect(failRow).toBeVisible({ timeout: 30_000 })
    await failRow.click()

    const drawer = page.getByTestId('invocation-summary-drawer')
    await expect(drawer).toBeVisible()
    const envelope = drawer.getByTestId('invocation-error-envelope')
    await expect(envelope).toBeVisible({ timeout: 15_000 })
    await expect(drawer.getByTestId('invocation-error-code')).toHaveText('REQUEST_BODY_INVALID')
    await expect(envelope).toContainText(/category|类别/i)
    await expect(envelope).toContainText(/message key|消息键/i)
    await expect(envelope).toContainText(/retryable|可重试/i)
    await expect(drawer.getByText(/parameters|variables/i)).toHaveCount(0)
    await assertNoViewportOverflow(page)

    await captureCeU11LocatorScreenshot(drawer, '02-failed-error-envelope-drawer-redbc-1440x900.png')
    await captureCeU11LocatorScreenshot(
      envelope,
      '02b-error-envelope-crop-redbc-1440x900.png',
    )

    // el-drawer close control is icon-only; Escape is reliable (CE-K05 / P14 pattern).
    await page.keyboard.press('Escape')
    await expect(drawer).toBeHidden({ timeout: 10_000 })

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')

    const greenPanel = await openInvocationHistory(page, templateId)
    await expect(greenPanel.getByTestId('invocation-release-version-filter')).toBeVisible()
    await expect(greenPanel.getByTestId('invocation-export-csv')).toBeVisible()
    await greenPanel
      .getByTestId('invocation-release-version-filter')
      .locator('input')
      .fill(RELEASE_V12)
    await greenPanel.getByTestId('invocation-apply-filters').click()
    await expect(greenPanel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(greenPanel.locator('.invocation-table tbody tr').first()).toBeVisible({
      timeout: 30_000,
    })

    await captureCeU11Screenshot(page, '03-invocation-history-filters-greenbc-1440x900.png')
    await captureCeU11LocatorScreenshot(
      greenPanel,
      '03b-invocation-panel-crop-greenbc-1440x900.png',
    )
    await captureCeU11LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03c-brand-header-greenbc-crop.png',
    )

    const greenFailRow = greenPanel
      .locator('.invocation-table tbody tr')
      .filter({ hasText: failV12RequestId })
    await expect(greenFailRow).toBeVisible({ timeout: 30_000 })
    await greenFailRow.click()

    const greenDrawer = page.getByTestId('invocation-summary-drawer')
    await expect(greenDrawer).toBeVisible()
    const greenEnvelope = greenDrawer.getByTestId('invocation-error-envelope')
    await expect(greenEnvelope).toBeVisible({ timeout: 15_000 })
    await expect(greenDrawer.getByTestId('invocation-error-code')).toHaveText(
      'REQUEST_BODY_INVALID',
    )
    await expect(greenDrawer.getByText(/parameters|variables/i)).toHaveCount(0)

    await captureCeU11LocatorScreenshot(
      greenDrawer,
      '04-failed-error-envelope-drawer-greenbc-1440x900.png',
    )
    await captureCeU11LocatorScreenshot(
      greenEnvelope,
      '04b-error-envelope-crop-greenbc-1440x900.png',
    )
  })
})
