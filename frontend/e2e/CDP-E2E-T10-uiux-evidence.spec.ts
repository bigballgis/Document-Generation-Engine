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
  prepareTemplatePendingApprovalDecision,
  prepareTemplatePendingRelease,
  type PendingSubmitTemplateFixture,
} from './helpers/submit-approval-gate-api'
import { dashboardTaskRow, filterDashboardTasksByItem } from './helpers/ui'
import {
  captureCdpE2eDecisionLocatorScreenshot,
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`
const TASK_ID = 'CDP-E2E-T10' as const

const FIDELITY_VIEWED_LABEL = /I reviewed fidelity warnings/i
const FIDELITY_VIEWED_TEST_ID = 'confirm-fidelity-viewed'

/**
 * CD-E2E-T10 UIUX evidence — fidelity viewed confirmation on Pass / Approve / Publish
 * dialogs @1920 REDBC (BDD-CDP-FID-001…004). Follows T09 capture helpers + viewport.
 */
test.describe('CDP-E2E-T10 UIUX evidence — fidelity viewed confirmation @1920 (BDD-CDP-FID-001…004)', () => {
  test.describe('Pass dialog blocked (FID-001)', () => {
    test.describe.configure({ mode: 'serial', timeout: 420_000 })

    let fixture: TestingTemplateFixture

    test.beforeAll(async ({ request }) => {
      ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
      await requireDockerStack(request, {
        frontendBaseUrl: FRONTEND_BASE_URL,
        skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
      })
      await assertDemoCatalogSeeded(request)
      fixture = await prepareRetailTemplateInTesting(request, {
        externalId: `E2E-CDP-T10-UX-PASS-${Date.now().toString(36).toUpperCase()}`,
        name: `E2E CDP T10 UX Pass Fidelity ${Date.now().toString(36).toUpperCase()}`,
      })
    })

    test.beforeEach(async ({ page }) => {
      await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
    })

    test('capture Pass dialog fidelity checkbox + disabled submit (REDBC @1920)', async ({
      page,
    }) => {
      await loginAs(page, E2E_TEMPLATE_TESTER)
      await switchBrand(page, 'REDBC')
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

      await dialog.getByText(/I reviewed the coverage summary/i).click()
      await dialog.getByText(/I reviewed the structured preview comparison/i).click()

      const submit = dialog.getByRole('button', { name: /^submit decision$/i })
      await expect(submit).toBeDisabled()

      await captureCdpE2eDecisionScreenshot(
        page,
        TASK_ID,
        '01-pass-dialog-fidelity-unchecked-disabled-submit-redbc-1920x1080.png',
      )
      await captureCdpE2eDecisionLocatorScreenshot(
        dialog,
        TASK_ID,
        '02-pass-dialog-fidelity-checkbox-detail-redbc-1920x1080.png',
      )
    })
  })

  test.describe('Approve dialog blocked (FID-002)', () => {
    test.describe.configure({ mode: 'serial', timeout: 420_000 })

    let fixture: PendingSubmitTemplateFixture

    test.beforeAll(async ({ request }) => {
      ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
      await requireDockerStack(request, {
        frontendBaseUrl: FRONTEND_BASE_URL,
        skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
      })
      await assertDemoCatalogSeeded(request)
      fixture = await prepareTemplatePendingApprovalDecision(request, {
        externalId: `E2E-CDP-T10-UX-APPR-${Date.now().toString(36).toUpperCase()}`,
        name: `E2E CDP T10 UX Approve Fidelity ${Date.now().toString(36).toUpperCase()}`,
      })
    })

    test.beforeEach(async ({ page }) => {
      await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
    })

    test('capture Approve dialog fidelity checkbox + disabled submit (REDBC @1920)', async ({
      page,
    }) => {
      await loginAs(page, E2E_TEMPLATE_APPROVER)
      await switchBrand(page, 'REDBC')
      await page.goto('/dashboard?queue=APPROVAL#tasks-section')
      await filterDashboardTasksByItem(page, fixture.name)
      const approvalRow = await dashboardTaskRow(page, fixture.name)
      await expect(approvalRow).toBeVisible({ timeout: 30_000 })
      await approvalRow.getByRole('button', { name: /^open$/i }).click()
      await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

      const dialog = await openApproveDialogAfterApproverOpen(page)
      const fidelityCheckbox = dialog.getByTestId(FIDELITY_VIEWED_TEST_ID)
      await expect(fidelityCheckbox).toBeVisible()
      await expect(fidelityCheckbox.getByText(FIDELITY_VIEWED_LABEL)).toBeVisible()

      await dialog
        .getByRole('textbox', { name: /approval rationale/i })
        .fill('CDP T10 UX approve without fidelity — capture blocked submit.')
      await dialog.getByText(/I reviewed key evidence/i).click()

      const submit = dialog.getByRole('button', { name: /^submit decision$/i })
      await expect(submit).toBeDisabled()

      await captureCdpE2eDecisionScreenshot(
        page,
        TASK_ID,
        '03-approve-dialog-fidelity-unchecked-disabled-submit-redbc-1920x1080.png',
      )
      await captureCdpE2eDecisionLocatorScreenshot(
        dialog,
        TASK_ID,
        '04-approve-dialog-fidelity-checkbox-detail-redbc-1920x1080.png',
      )
    })
  })

  test.describe('Publish dialog blocked (FID-003)', () => {
    test.describe.configure({ mode: 'serial', timeout: 420_000 })

    let fixture: PendingSubmitTemplateFixture

    test.beforeAll(async ({ request }) => {
      ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
      await requireDockerStack(request, {
        frontendBaseUrl: FRONTEND_BASE_URL,
        skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
      })
      await assertDemoCatalogSeeded(request)
      fixture = await prepareTemplatePendingRelease(request, {
        externalId: `E2E-CDP-T10-UX-PUB-${Date.now().toString(36).toUpperCase()}`,
        name: `E2E CDP T10 UX Publish Fidelity ${Date.now().toString(36).toUpperCase()}`,
      })
    })

    test.beforeEach(async ({ page }) => {
      await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
    })

    test('capture Publish dialog fidelity checkbox + disabled confirm (REDBC @1920)', async ({
      page,
    }) => {
      await loginAs(page, E2E_GROUP_ADMIN)
      await switchBrand(page, 'REDBC')
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

      const confirm = dialog.getByRole('button', { name: /^confirm go-live$/i })
      await expect(confirm).toBeDisabled()

      await captureCdpE2eDecisionScreenshot(
        page,
        TASK_ID,
        '05-publish-dialog-fidelity-unchecked-disabled-confirm-redbc-1920x1080.png',
      )
      await captureCdpE2eDecisionLocatorScreenshot(
        dialog,
        TASK_ID,
        '06-publish-dialog-fidelity-checkbox-detail-redbc-1920x1080.png',
      )
    })
  })
})
