import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  prepareTemplatePendingSubmitBlocked,
  prepareTemplatePendingSubmitReady,
} from './helpers/submit-approval-gate-api'
import {
  captureP12AudB10LocatorScreenshot,
  captureP12AudB10Screenshot,
  ensureP12AudB10EvidenceDirs,
  P12_AUD_B10_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

async function openTemplateLifecycleTab(page: import('@playwright/test').Page, templateId: string) {
  await page.goto(`/templates/${templateId}?tab=lifecycle`)
  const lifecyclePanel = page.locator('#template-lifecycle-panel')
  await expect(lifecyclePanel).toBeVisible({ timeout: 30_000 })
  await expect(lifecyclePanel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  return lifecyclePanel
}

function lifecycleSubmitButton(page: import('@playwright/test').Page) {
  return page
    .locator('#template-lifecycle-panel')
    .getByRole('button', { name: /^submit for approval$/i })
}

function submitSummaryDialog(page: import('@playwright/test').Page) {
  return page.locator('.el-dialog').filter({ hasText: /review before submit for approval/i })
}

test.describe('P12-AUD-B10 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    ensureP12AudB10EvidenceDirs()

    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1` })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P12_AUD_B10_VIEWPORT)
  })

  test('capture submit gate lifecycle, summary dialog, author journey CTA, and dual-brand evidence', async ({
    page,
    request,
  }) => {
    const readyTemplate = await prepareTemplatePendingSubmitReady(request)
    const blockedTemplate = await prepareTemplatePendingSubmitBlocked(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)

    await page.goto(`/templates/${readyTemplate.templateId}`)
    const authorJourney = page.locator('[data-journey-timeline]')
    await expect(authorJourney).toBeVisible()
    await expect(page.locator('[data-template-journey-cta]')).toBeEnabled()
    await captureP12AudB10LocatorScreenshot(
      authorJourney,
      '01-author-journey-submit-cta-redbc-1440x900.png',
    )

    const readyLifecyclePanel = await openTemplateLifecycleTab(page, readyTemplate.templateId)
    const readySubmitGateCard = readyLifecyclePanel.locator('.submit-gate-card')
    await expect(readySubmitGateCard).toBeVisible()
    await expect(
      readySubmitGateCard.getByRole('heading', { name: /^submission readiness checks$/i }),
    ).toBeVisible()
    await expect(
      readySubmitGateCard.getByText(/before submitting the template for approval/i),
    ).toBeVisible()
    await expect(lifecycleSubmitButton(page)).toBeEnabled()
    await captureP12AudB10Screenshot(page, '02-lifecycle-submit-gate-ready-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await captureP12AudB10Screenshot(page, '03-lifecycle-submit-gate-ready-greenbc-1440x900.png')

    await page.goto(`/templates/${blockedTemplate.templateId}?tab=lifecycle`)
    const blockedLifecyclePanel = page.locator('#template-lifecycle-panel')
    await expect(blockedLifecyclePanel).toBeVisible({ timeout: 30_000 })
    await expect(blockedLifecyclePanel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const blockedSubmitGateCard = blockedLifecyclePanel.locator('.submit-gate-card')
    await expect(blockedSubmitGateCard.getByText(/no batch test run recorded/i)).toBeVisible()
    await expect(lifecycleSubmitButton(page)).toBeDisabled()
    await captureP12AudB10Screenshot(page, '04-lifecycle-submit-gate-blocked-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await captureP12AudB10Screenshot(page, '05-lifecycle-submit-gate-blocked-greenbc-1440x900.png')

    await page.goto(`/templates/${readyTemplate.templateId}?tab=lifecycle`)
    await expect(lifecycleSubmitButton(page)).toBeEnabled()
    await lifecycleSubmitButton(page).click()
    const dialog = submitSummaryDialog(page)
    await expect(dialog).toBeVisible()
    await expect(dialog.getByText(/review the submission checklist/i)).toBeVisible()
    await captureP12AudB10LocatorScreenshot(
      dialog,
      '06-submit-summary-dialog-ready-redbc-1440x900.png',
    )
    await dialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).not.toBeVisible()
  })
})
