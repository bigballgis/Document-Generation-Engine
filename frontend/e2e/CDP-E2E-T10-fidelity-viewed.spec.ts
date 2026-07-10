import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  prepareRetailTemplateInTesting,
  type TestingTemplateFixture,
} from './helpers/collaboration-api'
import {
  openApproveDialogAfterApproverOpen,
  openConfirmTestPassDialogAfterTesterOpen,
  openGoLiveSummaryAfterTeamLeadOpen,
} from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  fetchTemplateDetail,
  prepareTemplatePendingApprovalDecision,
  prepareTemplatePendingRelease,
  type PendingSubmitTemplateFixture,
} from './helpers/submit-approval-gate-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import { dashboardTaskRow, filterDashboardTasksByItem } from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

const FIDELITY_VIEWED_LABEL = /I reviewed fidelity warnings/i
const FIDELITY_VIEWED_TEST_ID = 'confirm-fidelity-viewed'

/**
 * CD-E2E-T10 — Fidelity «viewed» confirmation fail-closed + smoke.
 * BDD: docs/behavior/fidelity-viewed-confirmation-journey.md (BDD-CDP-FID-001…004)
 */
test.describe('CDP-E2E-T10 Fidelity viewed confirmation (BDD-CDP-FID-001…004)', () => {
  // Independent serial suites so Approve/Publish product gaps still produce evidence.
  test.describe('Pass test (FID-001 / FID-004)', () => {
    test.describe.configure({ mode: 'serial', timeout: 420_000 })

    let fixture: TestingTemplateFixture

    test.beforeAll(async ({ request }) => {
      await requireDockerStack(request, {
        frontendBaseUrl: FRONTEND_BASE_URL,
        skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
      })
      await assertDemoCatalogSeeded(request)

      fixture = await prepareRetailTemplateInTesting(request, {
        externalId: `E2E-CDP-T10-PASS-${Date.now().toString(36).toUpperCase()}`,
        name: `E2E CDP T10 Pass Fidelity ${Date.now().toString(36).toUpperCase()}`,
      })

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('TESTING')
    })

    test('BDD-CDP-FID-001 — Pass blocked without fidelity viewed checkbox', async ({
      page,
      request,
    }) => {
      await loginAs(page, E2E_TEMPLATE_TESTER)
      await page.goto('/dashboard?queue=TEST#tasks-section')
      await filterDashboardTasksByItem(page, fixture.name)
      const testRow = await dashboardTaskRow(page, fixture.name)
      await expect(testRow).toBeVisible({ timeout: 30_000 })
      await testRow.getByRole('button', { name: /^open$/i }).click()
      await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

      const dialog = await openConfirmTestPassDialogAfterTesterOpen(page)
      const fidelityCheckbox = dialog.getByTestId(FIDELITY_VIEWED_TEST_ID)
      await expect(fidelityCheckbox).toBeVisible()
      await expect(fidelityCheckbox.getByText(FIDELITY_VIEWED_LABEL)).toBeVisible()

      // Other required confirmations only — leave fidelity unchecked.
      await dialog.getByText(/I reviewed the coverage summary/i).click()
      await dialog.getByText(/I reviewed the structured preview comparison/i).click()

      const submit = dialog.getByRole('button', { name: /^submit decision$/i })
      await expect(submit).toBeDisabled()

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('TESTING')
      expect(detail.approvalSubState ?? null).not.toBe('PENDING_SUBMIT')
    })

    test('BDD-CDP-FID-004 — Pass succeeds after fidelity viewed confirmation', async ({
      page,
      request,
    }) => {
      await loginAs(page, E2E_TEMPLATE_TESTER)
      await page.goto('/dashboard?queue=TEST#tasks-section')
      await filterDashboardTasksByItem(page, fixture.name)
      const testRow = await dashboardTaskRow(page, fixture.name)
      await expect(testRow).toBeVisible({ timeout: 30_000 })
      await testRow.getByRole('button', { name: /^open$/i }).click()
      await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

      const dialog = await openConfirmTestPassDialogAfterTesterOpen(page)
      await dialog.getByTestId(FIDELITY_VIEWED_TEST_ID).click()
      await dialog.getByText(/I reviewed the coverage summary/i).click()
      await dialog.getByText(/I reviewed the structured preview comparison/i).click()

      const decisionResponsePromise = page.waitForResponse(
        (response) =>
          response.request().method() === 'POST' &&
          response.url().includes('/lifecycle/test-decision'),
        { timeout: 30_000 },
      )
      await dialog.getByRole('button', { name: /^submit decision$/i }).click()
      const decisionResponse = await decisionResponsePromise
      expect(decisionResponse.ok()).toBeTruthy()
      await expect(page.locator('.el-message').getByText(/test decision recorded/i)).toBeVisible({
        timeout: 15_000,
      })

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('APPROVAL')
      expect(detail.approvalSubState).toBe('PENDING_SUBMIT')
    })
  })

  test.describe('Approve (FID-002 / FID-004)', () => {
    test.describe.configure({ mode: 'serial', timeout: 420_000 })

    let fixture: PendingSubmitTemplateFixture

    test.beforeAll(async ({ request }) => {
      await requireDockerStack(request, {
        frontendBaseUrl: FRONTEND_BASE_URL,
        skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
      })
      await assertDemoCatalogSeeded(request)

      fixture = await prepareTemplatePendingApprovalDecision(request, {
        externalId: `E2E-CDP-T10-APPR-${Date.now().toString(36).toUpperCase()}`,
        name: `E2E CDP T10 Approve Fidelity ${Date.now().toString(36).toUpperCase()}`,
      })

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('APPROVAL')
      expect(detail.approvalSubState).toBe('PENDING_DECISION')
    })

    test('BDD-CDP-FID-002 — Approve blocked without fidelity confirmation', async ({
      page,
      request,
    }) => {
      await loginAs(page, E2E_TEMPLATE_APPROVER)
      await page.goto('/dashboard?queue=APPROVAL#tasks-section')
      await filterDashboardTasksByItem(page, fixture.name)
      const approvalRow = await dashboardTaskRow(page, fixture.name)
      await expect(approvalRow).toBeVisible({ timeout: 30_000 })
      await approvalRow.getByRole('button', { name: /^open$/i }).click()
      await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

      const dialog = await openApproveDialogAfterApproverOpen(page)
      // Domain requires Approval Summary to record fidelity-viewed confirmation
      // (dedicated checkbox or equivalent that explicitly covers fidelity warning summary).
      const fidelityCheckbox = dialog.getByTestId(FIDELITY_VIEWED_TEST_ID)
      await expect(fidelityCheckbox).toBeVisible()
      await expect(fidelityCheckbox.getByText(FIDELITY_VIEWED_LABEL)).toBeVisible()

      await dialog
        .getByRole('textbox', { name: /approval rationale/i })
        .fill('CDP T10 approve without fidelity — must stay blocked.')
      await dialog.getByText(/I reviewed key evidence/i).click()

      const submit = dialog.getByRole('button', { name: /^submit decision$/i })
      await expect(submit).toBeDisabled()

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('APPROVAL')
      expect(detail.approvalSubState).toBe('PENDING_DECISION')
    })

    test('BDD-CDP-FID-004 — Approve succeeds after fidelity confirmation', async ({
      page,
      request,
    }) => {
      await loginAs(page, E2E_TEMPLATE_APPROVER)
      await page.goto('/dashboard?queue=APPROVAL#tasks-section')
      await filterDashboardTasksByItem(page, fixture.name)
      const approvalRow = await dashboardTaskRow(page, fixture.name)
      await expect(approvalRow).toBeVisible({ timeout: 30_000 })
      await approvalRow.getByRole('button', { name: /^open$/i }).click()
      await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

      const dialog = await openApproveDialogAfterApproverOpen(page)
      await dialog
        .getByRole('textbox', { name: /approval rationale/i })
        .fill('CDP T10 approve with fidelity viewed confirmation.')
      await dialog.getByTestId(FIDELITY_VIEWED_TEST_ID).click()
      await dialog.getByText(/I reviewed key evidence/i).click()

      const decisionResponsePromise = page.waitForResponse(
        (response) =>
          response.request().method() === 'POST' &&
          response.url().includes('/lifecycle/approval-decision'),
        { timeout: 30_000 },
      )
      await dialog.getByRole('button', { name: /^submit decision$/i }).click()
      const decisionResponse = await decisionResponsePromise
      expect(decisionResponse.ok()).toBeTruthy()
      await expect(
        page.locator('.el-message').getByText(/approval decision recorded/i),
      ).toBeVisible({ timeout: 15_000 })

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')
    })
  })

  test.describe('Publish (FID-003 / FID-004)', () => {
    test.describe.configure({ mode: 'serial', timeout: 420_000 })

    let fixture: PendingSubmitTemplateFixture

    test.beforeAll(async ({ request }) => {
      await requireDockerStack(request, {
        frontendBaseUrl: FRONTEND_BASE_URL,
        skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
      })
      await assertDemoCatalogSeeded(request)

      fixture = await prepareTemplatePendingRelease(request, {
        externalId: `E2E-CDP-T10-PUB-${Date.now().toString(36).toUpperCase()}`,
        name: `E2E CDP T10 Publish Fidelity ${Date.now().toString(36).toUpperCase()}`,
      })

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')
    })

    test('BDD-CDP-FID-003 — Publish blocked without fidelity viewed', async ({
      page,
      request,
    }) => {
      const linesBefore = await listTemplateVersionLines(request, fixture.templateId)
      const publishedBefore = linesBefore.filter((line) => line.lineKind === 'PUBLISHED').length

      await loginAs(page, E2E_GROUP_ADMIN)
      await page.goto('/dashboard?queue=PENDING_RELEASE#tasks-section')
      await filterDashboardTasksByItem(page, fixture.name)
      const pendingRow = await dashboardTaskRow(page, fixture.name)
      await expect(pendingRow).toBeVisible({ timeout: 30_000 })
      await pendingRow.getByRole('button', { name: /^open$/i }).click()
      await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

      const dialog = await openGoLiveSummaryAfterTeamLeadOpen(page)
      // Release Summary must record fidelity warning summary viewed confirmation
      // (explicit checkbox or gate checklist item that is not yet ready).
      const fidelityCheckbox = dialog.getByTestId(FIDELITY_VIEWED_TEST_ID)
      await expect(fidelityCheckbox).toBeVisible()
      await expect(fidelityCheckbox.getByText(FIDELITY_VIEWED_LABEL)).toBeVisible()

      const confirm = dialog.getByRole('button', { name: /^confirm go-live$/i })
      // Unconfirmed fidelity must keep Confirm disabled (or equivalent fail-closed).
      await expect(confirm).toBeDisabled()

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')

      const linesAfter = await listTemplateVersionLines(request, fixture.templateId)
      const publishedAfter = linesAfter.filter((line) => line.lineKind === 'PUBLISHED').length
      expect(publishedAfter).toBe(publishedBefore)
    })

    test('BDD-CDP-FID-004 — Publish succeeds after fidelity viewed confirmation', async ({
      page,
      request,
    }) => {
      await loginAs(page, E2E_GROUP_ADMIN)
      await page.goto('/dashboard?queue=PENDING_RELEASE#tasks-section')
      await filterDashboardTasksByItem(page, fixture.name)
      const pendingRow = await dashboardTaskRow(page, fixture.name)
      await expect(pendingRow).toBeVisible({ timeout: 30_000 })
      await pendingRow.getByRole('button', { name: /^open$/i }).click()
      await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

      const dialog = await openGoLiveSummaryAfterTeamLeadOpen(page)
      const fidelityCheckbox = dialog.getByTestId(FIDELITY_VIEWED_TEST_ID)
      await expect(fidelityCheckbox).toBeVisible()
      await expect(fidelityCheckbox.getByText(FIDELITY_VIEWED_LABEL)).toBeVisible()
      await fidelityCheckbox.click()

      const publishResponsePromise = page.waitForResponse(
        (response) =>
          response.request().method() === 'POST' && response.url().includes('/lifecycle/publish'),
        { timeout: 30_000 },
      )
      await dialog.getByRole('button', { name: /^confirm go-live$/i }).click()
      const publishResponse = await publishResponsePromise
      expect(publishResponse.ok()).toBeTruthy()
      await expect(page.locator('.el-message').getByText(/template is now live/i)).toBeVisible({
        timeout: 15_000,
      })

      const detail = await fetchTemplateDetail(request, fixture.templateId)
      expect(detail.lifecycleStatus).toBe('PUBLISHED')
    })
  })
})
