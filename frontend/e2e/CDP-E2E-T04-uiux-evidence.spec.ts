import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_APPROVER, loginAs } from './helpers/auth'
import { approveTemplateAfterApproverOpen } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  fetchTemplateDetail,
  prepareTemplatePendingApprovalDecision,
  type PendingSubmitTemplateFixture,
} from './helpers/submit-approval-gate-api'
import { dashboardTaskRow, filterDashboardTasksByItem } from './helpers/ui'
import {
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const TASK_ID = 'CDP-E2E-T04' as const

test.describe('CDP-E2E-T04 UIUX evidence — approver approve decision @1920 (BDD-CDP-APPR-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: PendingSubmitTemplateFixture

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareTemplatePendingApprovalDecision(request, {
      externalId: `E2E-CDP-T04-UX-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CDP T04 UX ${Date.now().toString(36).toUpperCase()}`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture APPROVAL queue + approve decision dialog (REDBC + GREENBC queue)', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await switchBrand(page, 'REDBC')
    await page.goto('/dashboard?queue=APPROVAL#tasks-section')
    await filterDashboardTasksByItem(page, fixture.name)
    const approvalRow = await dashboardTaskRow(page, fixture.name)
    await expect(approvalRow).toBeVisible({ timeout: 30_000 })
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-dashboard-approval-queue-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('#tasks-section')).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '02-dashboard-approval-queue-greenbc-1920x1080.png',
    )

    await switchBrand(page, 'REDBC')
    await approvalRow.getByRole('button', { name: /^open$/i }).click()
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
        .getByRole('button', { name: /^approve$/i })
        .first()
        .click()
    } else {
      await page.locator('.workspace-tab-shell').getByRole('tab', { name: /^template approval$/i }).click()
      await page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /^approve$/i }).click()
    }

    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText(/confirm approval/i)).toBeVisible()
    await dialog
      .getByRole('textbox', { name: /approval rationale/i })
      .fill('UIUX evidence capture — key evidence reviewed.')
    await dialog.getByText(/I reviewed key evidence/i).click()
    await captureCdpE2eDecisionScreenshot(page, TASK_ID, '03-approve-decision-dialog-redbc-1920x1080.png')
    await dialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).not.toBeVisible()

    await approveTemplateAfterApproverOpen(page)
    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')
  })
})
