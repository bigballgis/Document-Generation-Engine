import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_APPROVER, loginAs } from './helpers/auth'
import { listCollaborationWorkItems } from './helpers/collaboration-api'
import { approveTemplateAfterApproverOpen } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  fetchTemplateDetail,
  prepareTemplatePendingApprovalDecision,
  type PendingSubmitTemplateFixture,
} from './helpers/submit-approval-gate-api'
import { dashboardTaskRow, filterDashboardTasksByItem } from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('CDP-E2E-T04 Approver approve decision (BDD-CDP-APPR-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: PendingSubmitTemplateFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    // API setup only (TESTING → pass → submit-approval) — Approve clicks are browser-only below.
    fixture = await prepareTemplatePendingApprovalDecision(request, {
      externalId: `E2E-CDP-T04-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CDP Approver Approve ${Date.now().toString(36).toUpperCase()}`,
    })

    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('APPROVAL')
    expect(detail.approvalSubState).toBe('PENDING_DECISION')

    const approvalItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_APPROVER, {
      queue: 'APPROVAL',
    })
    expect(approvalItems.some((item) => item.templateId === fixture.templateId)).toBeTruthy()
  })

  test('approver Approve with evidence confirmation reaches PENDING_RELEASE and clears APPROVAL queue', async ({
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

    await approveTemplateAfterApproverOpen(page)

    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')

    const approvalItemsAfter = await listCollaborationWorkItems(request, E2E_TEMPLATE_APPROVER, {
      queue: 'APPROVAL',
    })
    expect(approvalItemsAfter.some((item) => item.templateId === fixture.templateId)).toBeFalsy()
  })
})
