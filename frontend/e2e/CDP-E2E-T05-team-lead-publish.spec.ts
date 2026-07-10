import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { fetchManagementCallerContract } from './helpers/content-modules-api'
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
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import { dashboardTaskRow, filterDashboardTasksByItem } from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('CDP-E2E-T05 Team lead publish / go-live (BDD-CDP-PUB-001/002)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: PendingSubmitTemplateFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    // API setup only (through APPROVAL → PENDING_RELEASE) — go-live clicks are browser-only below.
    fixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-CDP-T05-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CDP Team Lead Publish ${Date.now().toString(36).toUpperCase()}`,
    })

    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')
  })

  test('BDD-CDP-PUB-002 — dismiss go-live summary leaves PENDING_RELEASE unchanged', async ({
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
    await dialog.getByRole('button', { name: /^cancel$/i }).click()
    await expect(dialog).not.toBeVisible()

    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PENDING_RELEASE')

    const linesAfter = await listTemplateVersionLines(request, fixture.templateId)
    const publishedAfter = linesAfter.filter((line) => line.lineKind === 'PUBLISHED').length
    expect(publishedAfter).toBe(publishedBefore)
  })

  test('BDD-CDP-PUB-001 — confirm go-live publishes release and External access shows callable', async ({
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

    await confirmGoLiveAfterTeamLeadOpen(page)

    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('PUBLISHED')

    const lines = await listTemplateVersionLines(request, fixture.templateId)
    expect(lines.some((line) => line.lineKind === 'PUBLISHED' && line.releaseVersion === '1.0.0')).toBeTruthy()

    const contract = await fetchManagementCallerContract(request, fixture.templateId)
    expect(
      contract.callableVersions.some(
        (version) =>
          version.releaseVersion === '1.0.0' && /\/versions\/1\.0\.0/i.test(version.explicitVersionUrl),
      ),
    ).toBeTruthy()

    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const routeSummary = page.getByTestId('route-summary-panel')
    await expect(routeSummary).toBeVisible()
    await expect(routeSummary.locator('.path-value')).toContainText(/\/generate/i)
    await expect(routeSummary.getByText(/Default release 1\.0\.0/i)).toBeVisible()
    await expect(page.getByText(/api not configured|未配置 api/i)).toHaveCount(0)
  })
})
