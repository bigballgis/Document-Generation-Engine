import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_AUTHOR, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import { prepareRetailTemplateInTesting } from './helpers/collaboration-api'
import { assertDemoCatalogSeeded, E2E_API_BASE_URL } from './helpers/masters-api'
import {
  prepareTestingTemplateWithRiskPromptOverride,
  resolveDevEditorTestPreviewPath,
} from './helpers/risk-prompt-config-api'
import { reLoginAs } from './helpers/ui'
import {
  captureP12RiskPromptUxLocatorScreenshot,
  captureP12RiskPromptUxScreenshot,
  ensureP12RiskPromptUxEvidenceDirs,
  P12_RISK_PROMPT_UX_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P12-BDD-RISK-PROMPT-UX-001 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    ensureP12RiskPromptUxEvidenceDirs()

    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1` })

    try {
      await assertDemoCatalogSeeded(request)
    } catch (error) {
      test.skip(true, error instanceof Error ? error.message : String(error))
    }
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P12_RISK_PROMPT_UX_VIEWPORT)
  })

  test('capture list/create/detail/decision UIUX evidence (REDBC + GREENBC)', async ({
    page,
    request,
  }) => {
    await prepareRetailTemplateInTesting(request)
    const filteredFixture = await prepareTestingTemplateWithRiskPromptOverride(request, [
      'FIDELITY_WARNING',
      'OTHER',
    ])

    await loginAs(page, E2E_TEMPLATE_AUTHOR)

    await page.goto('/templates')
    await captureP12RiskPromptUxScreenshot(page, '01-template-list-no-risk-prompt-panel-redbc-1440x900.png')

    await page.getByRole('button', { name: /new template package/i }).click()
    const createDialog = page.getByRole('dialog', { name: /^create template$/i })
    await expect(createDialog).toBeVisible({ timeout: 15_000 })
    await captureP12RiskPromptUxLocatorScreenshot(
      createDialog,
      '02-create-dialog-collapsed-advanced-redbc-1440x900.png',
    )

    await createDialog.getByRole('button', { name: /test and approval return reasons \(optional\)/i }).click()
    await createDialog.getByText(/customize return reasons for this template/i).click()
    const riskPromptSection = createDialog.locator('.risk-prompt-panel')
    await expect(riskPromptSection).toBeVisible()
    await captureP12RiskPromptUxLocatorScreenshot(
      riskPromptSection,
      '03-create-dialog-expanded-risk-prompt-redbc-1440x900.png',
    )
    await captureP12RiskPromptUxLocatorScreenshot(
      riskPromptSection,
      '04-template-detail-risk-prompt-section-redbc-1440x900.png',
    )
    await createDialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(createDialog).not.toBeVisible()

    await switchBrand(page, 'GREENBC')
    await page.getByRole('button', { name: /new template package/i }).click()
    const createDialogGreen = page.getByRole('dialog', { name: /^create template$/i })
    await expect(createDialogGreen).toBeVisible({ timeout: 15_000 })
    await createDialogGreen
      .getByRole('button', { name: /test and approval return reasons \(optional\)/i })
      .click()
    await createDialogGreen.getByText(/customize return reasons for this template/i).click()
    const riskPromptSectionGreen = createDialogGreen.locator('.risk-prompt-panel')
    await expect(riskPromptSectionGreen).toBeVisible()
    await captureP12RiskPromptUxLocatorScreenshot(
      riskPromptSectionGreen,
      '05-template-detail-risk-prompt-section-greenbc-1440x900.png',
    )
    await createDialogGreen.getByRole('button', { name: /^cancel$/i }).click()
    await expect(createDialogGreen).not.toBeVisible()
    await switchBrand(page, 'REDBC')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await page.goto(await resolveDevEditorTestPreviewPath(request, filteredFixture))
    await expect(page).toHaveURL(/authoringTab=testPreview/)
    const workflow = page.locator('.test-preview-workflow')
    await expect(workflow).toBeVisible({ timeout: 30_000 })
    await workflow.getByRole('button', { name: /^record test failure$/i }).click()
    const decisionDialog = page.getByRole('dialog', { name: /record test failure/i })
    await expect(decisionDialog).toBeVisible()
    await captureP12RiskPromptUxLocatorScreenshot(
      decisionDialog,
      '06-decision-dialog-filtered-categories-redbc-1440x900.png',
    )
  })
})
