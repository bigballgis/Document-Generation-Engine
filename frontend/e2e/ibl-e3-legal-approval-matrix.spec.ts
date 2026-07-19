import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import {
  E2E_LEGAL_REVIEWER,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import { listCollaborationWorkItems } from './helpers/collaboration-api'
import { approveTemplateAfterApproverOpen } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  fetchTemplateDetail,
  prepareTemplatePendingComplianceDecision,
  prepareTemplatePendingLegalDecision,
  prepareTemplatePendingSubmitReady,
  setApprovalMatrixMode,
} from './helpers/submit-approval-gate-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  dashboardTaskRow,
  filterDashboardTasksByItem,
  reLoginAs,
  selectElementPlusOption,
} from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

const LEGAL_NAV = /waiting on my legal review/i
const APPROVAL_NAV = /waiting on my approval/i

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openDashboardQueueTasks(page: Page, queue: string) {
  await page.goto(`/dashboard?queue=${queue}#tasks-section`)
  await dismissOnboardingTourIfPresent(page)
  const tasks = page.locator('#tasks-section')
  await expect(tasks).toBeVisible({ timeout: 30_000 })
  await expect(tasks.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
  await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()
  return tasks
}

function workspaceActions(page: Page) {
  return page.locator('.workspace-tab-shell__actions')
}

test.describe('IBL-E3 legal→compliance approval matrix', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('BDD-IBL-E3-015 — author configures LEGAL_THEN_COMPLIANCE; Legal stage gates Approve CTAs', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingSubmitReady(request, {
      externalId: `E2E-IBL-E3-015-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E IBL-E3-015 Mode ${Date.now().toString(36).toUpperCase()}`,
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(`/templates/${fixture.templateId}`)
    await dismissOnboardingTourIfPresent(page)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible({ timeout: 30_000 })

    const modeEdit = page.getByTestId('template-overview-approval-matrix-edit')
    await expect(modeEdit).toBeVisible({ timeout: 15_000 })
    await modeEdit.getByTestId('approval-matrix-mode-select').click()
    await selectElementPlusOption(page, /legal then compliance/i)
    await modeEdit.getByTestId('template-overview-approval-matrix-save').click()
    await expect(page.locator('.el-message').getByText(/approval matrix mode updated/i)).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.getByTestId('template-overview-approval-matrix-mode')).toContainText(
      /legal then compliance/i,
    )

    const detailAfterMode = await fetchTemplateDetail(request, fixture.templateId)
    expect(detailAfterMode.approvalMatrixMode).toBe('LEGAL_THEN_COMPLIANCE')

    // Advance to LEGAL stage via API (UI submit exercised elsewhere).
    const authorLogin = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
      data: E2E_TEMPLATE_AUTHOR,
    })
    expect(authorLogin.ok()).toBeTruthy()
    const authorToken = ((await authorLogin.json()) as { result: { accessToken: string } }).result
      .accessToken
    const submitResponse = await request.post(
      `${E2E_API_BASE_URL}/templates/${fixture.templateId}/lifecycle/submit-approval`,
      {
        headers: { Authorization: `Bearer ${authorToken}` },
        data: { commentSummary: 'E2E IBL-E3-015 submit after UI mode config' },
      },
    )
    expect(submitResponse.ok()).toBeTruthy()

    const legalDetail = await fetchTemplateDetail(request, fixture.templateId)
    expect(legalDetail.lifecycleStatus).toBe('APPROVAL')
    expect(legalDetail.approvalSubState).toBe('PENDING_LEGAL_DECISION')

    // TEMPLATE_APPROVER must not see Approve/Reject on LEGAL stage.
    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()
    await page.goto(
      `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}?workspaceTab=approval&approvalTab=submitApproval`,
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('approval-stage-indicator')).toContainText(/legal review/i)
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toHaveCount(0)
    await expect(workspaceActions(page).getByRole('button', { name: /^reject$/i })).toHaveCount(0)

    // LEGAL_REVIEWER sees Legal stage CTA.
    await reLoginAs(page, loginAs, E2E_LEGAL_REVIEWER)
    await page.goto(
      `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}?workspaceTab=approval&approvalTab=submitApproval`,
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('approval-stage-indicator')).toContainText(/legal review/i)
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect(workspaceActions(page).getByRole('button', { name: /^reject$/i })).toBeVisible()
  })

  test('BDD-IBL-E3-016 — LEGAL_REVIEWER Waiting on my legal review queue deep-links to decision surface', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingLegalDecision(request, {
      externalId: `E2E-IBL-E3-016-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E IBL-E3-016 Queue ${Date.now().toString(36).toUpperCase()}`,
    })

    const legalItems = await listCollaborationWorkItems(request, E2E_LEGAL_REVIEWER, {
      queue: 'LEGAL',
    })
    expect(legalItems.some((item) => item.templateId === fixture.templateId)).toBeTruthy()

    // Approver sees APPROVAL queue tab, not LEGAL (dashboard tablist is the L1 surface).
    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await page.goto('/dashboard')
    await dismissOnboardingTourIfPresent(page)
    await expect(page.getByRole('tab', { name: APPROVAL_NAV })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('tab', { name: LEGAL_NAV })).toHaveCount(0)

    await reLoginAs(page, loginAs, E2E_LEGAL_REVIEWER)
    await page.goto('/dashboard')
    await dismissOnboardingTourIfPresent(page)
    await expect(page.getByRole('tab', { name: LEGAL_NAV })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('tab', { name: APPROVAL_NAV })).toHaveCount(0)

    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()

    await openDashboardQueueTasks(page, 'LEGAL')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: LEGAL_NAV })).toHaveAttribute('aria-selected', 'true')

    await filterDashboardTasksByItem(page, fixture.name)
    const row = await dashboardTaskRow(page, fixture.name)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await row.getByRole('button', { name: /^open$/i }).click()

    await expect(page).toHaveURL(
      new RegExp(
        `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}\\?.*workspaceTab=approval`,
      ),
      { timeout: 30_000 },
    )
    await expect(page).toHaveURL(/approvalTab=submitApproval/)
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('approval-stage-indicator')).toContainText(/legal review/i)
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toBeVisible({
      timeout: 15_000,
    })
  })

  test('BDD-IBL-E3-005/006 — LEGAL then COMPLIANCE Approve CTAs reach PENDING_RELEASE', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingLegalDecision(request, {
      externalId: `E2E-IBL-E3-STG-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E IBL-E3 Stages ${Date.now().toString(36).toUpperCase()}`,
    })

    await loginAs(page, E2E_LEGAL_REVIEWER)
    await openDashboardQueueTasks(page, 'LEGAL')
    await filterDashboardTasksByItem(page, fixture.name)
    const legalRow = await dashboardTaskRow(page, fixture.name)
    await expect(legalRow).toBeVisible({ timeout: 30_000 })
    await legalRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

    await approveTemplateAfterApproverOpen(
      page,
      'E2E IBL-E3 LEGAL approve — evidence reviewed for compliance handoff.',
    )

    const afterLegal = await fetchTemplateDetail(request, fixture.templateId)
    expect(afterLegal.lifecycleStatus).toBe('APPROVAL')
    expect(afterLegal.approvalSubState).toBe('PENDING_COMPLIANCE_DECISION')

    const legalItemsAfter = await listCollaborationWorkItems(request, E2E_LEGAL_REVIEWER, {
      queue: 'LEGAL',
    })
    expect(legalItemsAfter.some((item) => item.templateId === fixture.templateId)).toBeFalsy()

    const approvalItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_APPROVER, {
      queue: 'APPROVAL',
    })
    expect(approvalItems.some((item) => item.templateId === fixture.templateId)).toBeTruthy()

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await openDashboardQueueTasks(page, 'APPROVAL')
    await filterDashboardTasksByItem(page, fixture.name)
    const complianceRow = await dashboardTaskRow(page, fixture.name)
    await expect(complianceRow).toBeVisible({ timeout: 30_000 })
    await complianceRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })
    await expect(page.getByTestId('approval-stage-indicator')).toContainText(/compliance approval/i)
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toBeVisible({
      timeout: 15_000,
    })

    await approveTemplateAfterApproverOpen(
      page,
      'E2E IBL-E3 COMPLIANCE approve — ready for pending release.',
    )

    const afterCompliance = await fetchTemplateDetail(request, fixture.templateId)
    expect(afterCompliance.lifecycleStatus).toBe('PENDING_RELEASE')
  })

  test('BDD-IBL-E3-009 UI — LEGAL_REVIEWER-only surface; compliance fixture shows Compliance stage', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingComplianceDecision(request, {
      externalId: `E2E-IBL-E3-009-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E IBL-E3-009 Comp ${Date.now().toString(36).toUpperCase()}`,
    })

    // LEGAL_REVIEWER must not see Approve on COMPLIANCE stage.
    await loginAs(page, E2E_LEGAL_REVIEWER)
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()
    await page.goto(
      `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}?workspaceTab=approval&approvalTab=submitApproval`,
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('approval-stage-indicator')).toContainText(/compliance approval/i)
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toHaveCount(0)

    // TEMPLATE_APPROVER sees Compliance Approve.
    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await page.goto(
      `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}?workspaceTab=approval&approvalTab=submitApproval`,
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('approval-stage-indicator')).toContainText(/compliance approval/i)
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toBeVisible({
      timeout: 15_000,
    })

    // Mode locked after LEGAL entered — API regression for E3-C4 / BDD-IBL-E3-003.
    const lockProbe = await setApprovalMatrixMode(request, fixture.templateId, 'SINGLE_TRACK').catch(
      (error: Error) => error,
    )
    expect(String(lockProbe)).toMatch(/422|APPROVAL_MATRIX_MODE_LOCKED|failed \(422\)/i)
  })
})
