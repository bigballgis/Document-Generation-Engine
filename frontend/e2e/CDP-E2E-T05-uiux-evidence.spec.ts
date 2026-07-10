import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  confirmGoLiveAfterTeamLeadOpen,
  openGoLiveSummaryAfterTeamLeadOpen,
} from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  fetchTemplateDetail,
  prepareTemplatePendingRelease,
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
const TASK_ID = 'CDP-E2E-T05' as const

test.describe('CDP-E2E-T05 UIUX evidence — team lead publish / go-live @1920 (BDD-CDP-PUB-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: PendingSubmitTemplateFixture

  test.beforeAll(async ({ request }) => {
    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-CDP-T05-UX-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CDP T05 UX ${Date.now().toString(36).toUpperCase()}`,
    })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture PENDING_RELEASE queue + go-live summary dialog (REDBC + GREENBC queue)', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await switchBrand(page, 'REDBC')
    await page.goto('/dashboard?queue=PENDING_RELEASE#tasks-section')
    await filterDashboardTasksByItem(page, fixture.name)
    const pendingRow = await dashboardTaskRow(page, fixture.name)
    await expect(pendingRow).toBeVisible({ timeout: 30_000 })
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-dashboard-pending-release-queue-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('#tasks-section')).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '02-dashboard-pending-release-queue-greenbc-1920x1080.png',
    )

    await switchBrand(page, 'REDBC')
    await pendingRow.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/tab=lifecycle|\/dev\//, { timeout: 15_000 })

    const dialog = await openGoLiveSummaryAfterTeamLeadOpen(page)
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '03-go-live-summary-dialog-redbc-1920x1080.png',
    )
    await dialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).not.toBeVisible()

    await confirmGoLiveAfterTeamLeadOpen(page)
    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PUBLISHED')

    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    await expect(page.getByTestId('route-summary-panel')).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '04-external-access-callable-redbc-1920x1080.png',
    )
  })
})
