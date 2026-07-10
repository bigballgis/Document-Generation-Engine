import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import { prepareRetailTemplateInTesting, type TestingTemplateFixture } from './helpers/collaboration-api'
import { confirmTestFailAfterTesterOpen } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { fetchTemplateDetail } from './helpers/submit-approval-gate-api'
import { dashboardTaskRow, filterDashboardTasksByItem } from './helpers/ui'
import {
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const TASK_ID = 'CDP-E2E-T03' as const

test.describe('CDP-E2E-T03 UIUX evidence — tester fail decision @1920 (BDD-CDP-TEST-002)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: TestingTemplateFixture

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-CDP-T03-UX-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CDP T03 UX ${Date.now().toString(36).toUpperCase()}`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture fail decision dialog with reason + remediation fields', async ({ page, request }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await switchBrand(page, 'REDBC')
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await filterDashboardTasksByItem(page, fixture.name)
    const testRow = await dashboardTaskRow(page, fixture.name)
    await expect(testRow).toBeVisible({ timeout: 30_000 })
    await testRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

    const lifecyclePanel = page.locator('#template-lifecycle-panel')
    const devWorkspace = page.locator('#dev-workspace')
    await expect
      .poll(async () => {
        if (await lifecyclePanel.isVisible().catch(() => false)) {
          return 'hub'
        }
        if (await devWorkspace.isVisible().catch(() => false)) {
          return 'dev'
        }
        return 'pending'
      }, { timeout: 30_000 })
      .not.toBe('pending')

    if (await lifecyclePanel.isVisible().catch(() => false)) {
      await page
        .locator('.workspace-tab-shell__actions, #template-lifecycle-panel')
        .getByRole('button', { name: /^record test failure$/i })
        .first()
        .click()
    } else {
      await page.locator('.workspace-tab-shell').getByRole('tab', { name: /^template testing$/i }).click()
      await page
        .locator('.workspace-tab-shell__actions')
        .getByRole('button', { name: /^record test failure$/i })
        .click()
    }

    const dialog = page.getByRole('dialog', { name: /record test failure/i })
    await expect(dialog).toBeVisible()
    await dialog.getByRole('combobox', { name: /reason category/i }).click()
    await page.getByRole('option', { name: /binding or layout placeholder issue/i }).click()
    await dialog
      .getByRole('textbox', { name: /impact summary/i })
      .fill('Header binding invalid — capture for UIUX evidence.')
    await dialog.getByRole('textbox', { name: /remediation checklist code/i }).fill('ANCHOR_INTEGRITY')
    await captureCdpE2eDecisionScreenshot(page, TASK_ID, '01-fail-decision-dialog-redbc-1920x1080.png')
    await dialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).not.toBeVisible()

    await confirmTestFailAfterTesterOpen(page)
    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('DRAFT')
  })
})
