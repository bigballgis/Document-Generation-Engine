import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  prepareCdpMvpGoldenDraft,
  type CdpMvpGoldenFixture,
} from './helpers/cdp-mvp-golden-api'
import {
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import { fetchTemplateDetail } from './helpers/submit-approval-gate-api'
import {
  approveTemplateFromDevWorkspace,
  confirmGoLiveFromDevWorkspace,
  confirmTestPassAfterTesterOpen,
  openDevEditorWorkspaceTab,
  saveApiRetentionPolicyFromHubTab,
  submitForApprovalFromDevWorkspace,
  submitForTestingFromDevWorkspace,
} from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { dashboardTaskRow, filterDashboardTasksByItem, reLoginAs } from './helpers/ui'
import {
  batchProgressDialog,
  openFolDevEditorTestingTab,
  runFullTestFromUi,
} from './helpers/template-testing-api'
import {
  captureCdpE2eT01LocatorScreenshot,
  captureCdpE2eT01Screenshot,
  CDP_E2E_T01_VIEWPORT,
  ensureCdpE2eT01EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function captureBrandHeader(page: Page, filename: string): Promise<string> {
  return captureCdpE2eT01LocatorScreenshot(page.locator('.shell-header .header-brand'), filename)
}

async function openConfirmTestPassDialog(page: Page): Promise<void> {
  await expect(page).not.toHaveURL(/\/forbidden/, { timeout: 15_000 })

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
    const passButton = page
      .locator('.workspace-tab-shell__actions, #template-lifecycle-panel')
      .getByRole('button', { name: /^confirm test pass$/i })
      .first()
    await expect(passButton).toBeVisible({ timeout: 15_000 })
    await passButton.click()
  } else {
    await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
    await expect(devWorkspace).toBeVisible({ timeout: 30_000 })
    const workspace = page.locator('.workspace-tab-shell')
    await expect(workspace).toBeVisible({ timeout: 30_000 })
    await workspace.getByRole('tab', { name: /^template testing$/i }).click()
    await workspace
      .locator('.workspace-tab-shell__actions')
      .getByRole('button', { name: /^confirm test pass$/i })
      .click()
  }

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText(/confirm test pass/i)).toBeVisible()
}

async function completeOpenConfirmTestPassDialog(page: Page): Promise<void> {
  const dialog = page.getByRole('dialog')
  await dialog.getByText(/I reviewed fidelity warnings/i).click()
  await dialog.getByText(/I reviewed the coverage summary/i).click()
  await dialog.getByText(/I reviewed the structured preview comparison/i).click()

  const decisionResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes('/lifecycle/test-decision'),
    { timeout: 30_000 },
  )
  await dialog.getByRole('button', { name: /^submit decision$/i }).click()
  const decisionResponse = await decisionResponsePromise
  expect(decisionResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/test decision recorded/i)).toBeVisible({
    timeout: 15_000,
  })
}

test.describe('CDP-E2E-T01 UIUX evidence — golden path @1920 (BDD-CDP-MVP-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 600_000 })

  let fixture: CdpMvpGoldenFixture

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eT01EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareCdpMvpGoldenDraft(request)
    if (fixture.lifecycleStatus !== 'DRAFT') {
      throw new Error(
        `prepareCdpMvpGoldenDraft must yield DRAFT (got ${fixture.lifecycleStatus} for ${fixture.externalId})`,
      )
    }
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_T01_VIEWPORT)
  })

  test('capture golden-path surfaces REDBC + GREENBC', async ({ page, request }) => {
    // --- AUTHOR: full test + submit for testing ---
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await switchBrand(page, 'REDBC')
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    await captureCdpE2eT01Screenshot(page, '01-author-testing-workspace-redbc-1920x1080.png')
    await runFullTestFromUi(page, request, fixture.templateId)
    const batchDialog = batchProgressDialog(page)
    if (await batchDialog.isVisible()) {
      await batchDialog.getByRole('button', { name: /^close$/i }).click()
    }
    await submitForTestingFromDevWorkspace(page)
    let detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('TESTING')

    // --- TESTER: dashboard TEST queue (REDBC + GREENBC) ---
    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await switchBrand(page, 'REDBC')
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await filterDashboardTasksByItem(page, fixture.name)
    const testRow = await dashboardTaskRow(page, fixture.name)
    await expect(testRow).toBeVisible({ timeout: 30_000 })
    await captureCdpE2eT01Screenshot(page, '02-dashboard-test-queue-redbc-1920x1080.png')
    await captureBrandHeader(page, '03-brand-header-redbc-1920x1080.png')

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('#tasks-section')).toBeVisible()
    await captureCdpE2eT01Screenshot(page, '04-dashboard-test-queue-greenbc-1920x1080.png')
    await captureBrandHeader(page, '05-brand-header-greenbc-1920x1080.png')

    await switchBrand(page, 'REDBC')
    await testRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

    // Testing decision dialog (open → capture → complete)
    await openConfirmTestPassDialog(page)
    await captureCdpE2eT01Screenshot(page, '06-testing-decision-dialog-redbc-1920x1080.png')
    await completeOpenConfirmTestPassDialog(page)

    detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('APPROVAL')
    expect(detail.approvalSubState).toBe('PENDING_SUBMIT')

    // --- AUTHOR: submit for approval ---
    await reLoginAs(page, loginAs, E2E_TEMPLATE_AUTHOR)
    await switchBrand(page, 'REDBC')
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await captureCdpE2eT01Screenshot(page, '07-approval-workspace-pending-submit-redbc-1920x1080.png')
    await submitForApprovalFromDevWorkspace(page)

    detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('APPROVAL')
    expect(detail.approvalSubState).toBe('PENDING_DECISION')

    // --- APPROVER: approve (dialog capture) ---
    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await switchBrand(page, 'REDBC')
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await captureCdpE2eT01Screenshot(page, '08-approval-workspace-pending-decision-redbc-1920x1080.png')

    await page
      .locator('.workspace-tab-shell__actions')
      .getByRole('button', { name: /^approve$/i })
      .click()
    const approveDialog = page.getByRole('dialog')
    await expect(approveDialog.getByText(/confirm approval/i)).toBeVisible()
    await captureCdpE2eT01Screenshot(page, '09-approval-decision-dialog-redbc-1920x1080.png')
    await approveDialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(approveDialog).not.toBeVisible()

    await switchBrand(page, 'GREENBC')
    await captureCdpE2eT01Screenshot(
      page,
      '10-approval-workspace-pending-decision-greenbc-1920x1080.png',
    )
    await switchBrand(page, 'REDBC')
    await approveTemplateFromDevWorkspace(page)

    detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')

    // --- GROUP_ADMIN: go-live ---
    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await switchBrand(page, 'REDBC')
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await captureCdpE2eT01Screenshot(page, '11-publish-go-live-workspace-redbc-1920x1080.png')

    const publishButton = page
      .locator('.workspace-tab-shell__actions')
      .getByRole('button', { name: /^confirm go-live$/i })
    await expect(publishButton).toBeEnabled({ timeout: 60_000 })
    await publishButton.click()
    const goLiveDialog = page.locator('.el-dialog').filter({ hasText: /go-live summary/i })
    await expect(goLiveDialog).toBeVisible()
    await captureCdpE2eT01Screenshot(page, '12-go-live-summary-dialog-redbc-1920x1080.png')
    await goLiveDialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(goLiveDialog).not.toBeVisible()
    await confirmGoLiveFromDevWorkspace(page)

    detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PUBLISHED')

    // --- GROUP_ADMIN: API policy (REDBC + GREENBC) ---
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
    await expect(page.locator('#policy-domain-INVOCATION_RETENTION')).toBeVisible()
    await captureCdpE2eT01Screenshot(page, '13-api-policy-hub-redbc-1920x1080.png')

    await saveApiRetentionPolicyFromHubTab(page)
    await expect(page.getByTestId('retention-save-success')).toBeVisible()
    await captureCdpE2eT01Screenshot(page, '14-api-policy-retention-saved-redbc-1920x1080.png')

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('.api-access-layout').first()).toBeVisible()
    await captureCdpE2eT01Screenshot(page, '15-api-policy-hub-greenbc-1920x1080.png')
  })
})
