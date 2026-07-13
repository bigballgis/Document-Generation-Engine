import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  ensureDemoFullFlowPublished,
  fetchDemoFullFlowApiPolicy,
} from './helpers/content-modules-api'
import {
  attemptNonCallableDefaultRouteHardBlock,
  editOutputPolicyCandidate,
  expandApiPolicyAdvancedSettings,
} from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  captureCdpE2eDecisionLocatorScreenshot,
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const TASK_ID = 'CDP-E2E-T07' as const

test.describe('CDP-E2E-T07 UIUX evidence — API policy OUTPUT_POLICY + hard-block @1920', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let templateId = ''

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture OUTPUT_POLICY edit → impact confirm → save (BDD-CDP-APIPOL-001)', async ({
    page,
    request,
  }) => {
    const policyBefore = await fetchDemoFullFlowApiPolicy(request, templateId)

    await loginAs(page, E2E_GROUP_ADMIN)
    await switchBrand(page, 'REDBC')
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByRole('tab', { name: /external access|对外接入/i })).toBeVisible()

    await expandApiPolicyAdvancedSettings(page)
    await expect(page.getByRole('button', { name: /save output settings/i })).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-api-access-advanced-output-policy-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.getByRole('button', { name: /save output settings/i })).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '02-api-access-advanced-output-policy-greenbc-1920x1080.png',
    )

    await switchBrand(page, 'REDBC')
    await editOutputPolicyCandidate(
      page,
      policyBefore.outputFormats,
      policyBefore.outputModes ?? [],
    )

    const previewPromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/api/policy/impact-preview'),
      { timeout: 45_000 },
    )
    const savePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' && response.url().includes('/api/policy/output'),
      { timeout: 60_000 },
    )

    await page.getByRole('button', { name: /save output settings/i }).click()
    const previewResponse = await previewPromise
    expect(previewResponse.ok()).toBeTruthy()

    const messageBox = page.locator('.el-message-box')
    await expect(messageBox).toBeVisible({ timeout: 10_000 })
    await captureCdpE2eDecisionLocatorScreenshot(
      messageBox,
      TASK_ID,
      '03-output-policy-impact-confirm-redbc-1920x1080.png',
    )
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '04-output-policy-impact-confirm-workspace-redbc-1920x1080.png',
    )

    await messageBox.getByRole('button', { name: /^(ok|confirm)$/i }).click()
    const saveResponse = await savePromise
    expect(saveResponse.ok()).toBeTruthy()
    await expect(
      page.locator('.el-message').getByText(/access setting saved|访问设置已保存/i),
    ).toBeVisible({ timeout: 15_000 })

    const policyAfterSave = await fetchDemoFullFlowApiPolicy(request, templateId)
    expect(policyAfterSave.policyVersion).toBeGreaterThan(policyBefore.policyVersion)

    await page.reload()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(
      page.getByText(new RegExp(`settings version\\s+v${policyAfterSave.policyVersion}`, 'i')),
    ).toBeVisible()
    await expect(page.getByRole('button', { name: /^activity log$/i })).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '05-output-policy-saved-version-redbc-1920x1080.png',
    )
  })

  test('capture DEFAULT_ROUTE hard-block finding panel (BDD-CDP-APIPOL-002)', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await switchBrand(page, 'REDBC')
    await page.goto(`/templates/${templateId}?tab=apiAccess`)
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const { defaultRoutePutCount } = await attemptNonCallableDefaultRouteHardBlock(page)
    expect(defaultRoutePutCount).toBe(0)

    const routeSection = page.locator('#policy-domain-DEFAULT_ROUTE_TARGET')
    const hardBlock = routeSection.getByTestId('api-policy-hard-block-finding')
    await expect(hardBlock).toBeVisible()
    await expect(routeSection.getByTestId('api-policy-hard-block-reason')).not.toBeEmpty()
    await expect(routeSection.getByTestId('api-policy-hard-block-impact')).not.toBeEmpty()
    await expect(routeSection.getByTestId('api-policy-hard-block-advice')).not.toBeEmpty()
    await expect(routeSection.getByTestId('api-policy-hard-block-error-code')).toHaveText(
      'DEFAULT_ROUTE_TARGET_UNAVAILABLE',
    )

    await captureCdpE2eDecisionLocatorScreenshot(
      routeSection.getByTestId('api-policy-impact-preview-panel'),
      TASK_ID,
      '06-default-route-hard-block-finding-redbc-1920x1080.png',
    )
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '07-default-route-hard-block-workspace-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(hardBlock).toBeVisible()
    await captureCdpE2eDecisionLocatorScreenshot(
      routeSection.getByTestId('api-policy-impact-preview-panel'),
      TASK_ID,
      '08-default-route-hard-block-finding-greenbc-1920x1080.png',
    )
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '09-default-route-hard-block-workspace-greenbc-1920x1080.png',
    )

    const policyAfter = await fetchDemoFullFlowApiPolicy(request, templateId)
    expect(policyAfter.defaultRouteReleaseVersion).not.toBe('9.9.9-non-callable')
  })
})
