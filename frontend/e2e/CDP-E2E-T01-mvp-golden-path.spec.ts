import { expect, test } from '@playwright/test'

import {
  CDP_MVP_GOLDEN_NAME,
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
import { listCollaborationWorkItems } from './helpers/collaboration-api'
import { fetchTemplateDetail } from './helpers/submit-approval-gate-api'
import {
  approveTemplateFromDevWorkspace,
  confirmGoLiveFromDevWorkspace,
  confirmTestPassFromDevWorkspace,
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

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('CDP-E2E-T01 MVP golden path — browser only (BDD-CDP-MVP-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 600_000 })

  let fixture: CdpMvpGoldenFixture

  test.beforeAll(async ({ request }) => {
    let backendReady = false
    let frontendReady = false
    try {
      backendReady = (await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })).ok()
    } catch {
      backendReady = false
    }
    try {
      frontendReady = (await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })).ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )

    await assertDemoCatalogSeeded(request)
    fixture = await prepareCdpMvpGoldenDraft(request)
  })

  test('full lifecycle chain without API lifecycle helpers', async ({ page, request }) => {
    test.skip(
      fixture.lifecycleStatus !== 'DRAFT',
      `CDP-MVP-GOLDEN already at ${fixture.lifecycleStatus}; reset Docker volume or use a fresh DB to rerun the golden path.`,
    )

    // Step 1 — AUTHOR: full test + submit for testing (dev workspace / testing tab)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    await runFullTestFromUi(page, request, fixture.templateId)
    const batchDialog = batchProgressDialog(page)
    if (await batchDialog.isVisible()) {
      await batchDialog.getByRole('button', { name: /^close$/i }).click()
    }
    await submitForTestingFromDevWorkspace(page)

    let detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('TESTING')

    const testItemsBefore = await listCollaborationWorkItems(request, E2E_TEMPLATE_TESTER, {
      queue: 'TEST',
    })
    expect(testItemsBefore.some((item) => item.templateId === fixture.templateId)).toBeTruthy()

    // Step 2 — TESTER: dashboard TEST queue → confirm test pass
    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard#tasks-section')
    await filterDashboardTasksByItem(page, CDP_MVP_GOLDEN_NAME)
    const testRow = await dashboardTaskRow(page, CDP_MVP_GOLDEN_NAME)
    await expect(testRow).toBeVisible({ timeout: 30_000 })
    await testRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/\/dev\//)
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'testing')
    await confirmTestPassFromDevWorkspace(page)

    detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('APPROVAL')
    expect(detail.approvalSubState).toBe('PENDING_SUBMIT')

    const testItemsAfter = await listCollaborationWorkItems(request, E2E_TEMPLATE_TESTER, {
      queue: 'TEST',
    })
    expect(testItemsAfter.some((item) => item.templateId === fixture.templateId)).toBeFalsy()

    // Step 3 — AUTHOR: submit for approval with evidence checklist
    await reLoginAs(page, loginAs, E2E_TEMPLATE_AUTHOR)
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await submitForApprovalFromDevWorkspace(page)

    detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('APPROVAL')
    expect(detail.approvalSubState).toBe('PENDING_DECISION')

    const approvalItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_APPROVER, {
      queue: 'APPROVAL',
    })
    expect(approvalItems.some((item) => item.templateId === fixture.templateId)).toBeTruthy()

    // Step 4 — APPROVER: approve
    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await approveTemplateFromDevWorkspace(page)

    detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')

    // Step 5 — GROUP_ADMIN: confirm go-live
    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await confirmGoLiveFromDevWorkspace(page)

    detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PUBLISHED')

    // Step 6 — GROUP_ADMIN: save at least one API policy domain on hub tab
    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await saveApiRetentionPolicyFromHubTab(page)

    await expect(page.getByText(/policy version|策略版本/i).first()).toBeVisible()
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
  })
})
