import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import {
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  listCollaborationWorkItems,
  prepareRetailTemplateInTesting,
  requireOpenTestWorkItemForTemplate,
} from './helpers/collaboration-api'
import { confirmTestPassAfterTesterOpen } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  fetchTemplateDetail,
  prepareTemplatePendingApprovalDecision,
  prepareTemplatePendingRelease,
} from './helpers/submit-approval-gate-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  dashboardTaskRow,
  expectDashboardPartitionHeading,
  filterDashboardTasksByItem,
} from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

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

/**
 * CE-U14 — Dashboard lifecycle to-dos deep links (BDD-CE-U14-DLT).
 * BDD: docs/behavior/ce-u14-dashboard-lifecycle-todos.md
 */
test.describe('CE-U14 dashboard lifecycle todos (BDD-CE-U14-DLT)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('BDD-CE-U14-DLT-001 — TEST queue todo deep-links to testing decision surface', async ({
    page,
    request,
  }) => {
    const fixture = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-CE-U14-DLT001-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U14 DLT-001 Test ${Date.now().toString(36).toUpperCase()}`,
    })
    const workItem = await requireOpenTestWorkItemForTemplate(request, fixture)
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await openDashboardQueueTasks(page, 'TEST')

    // Tabbed dashboard keeps h1 "My tasks"; queue label is the selected Tasks tab.
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /waiting on my testing/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expectDashboardPartitionHeading(page, /in testing/i)
    await expect(page.locator('[data-partition-id="queue-TEST"]')).toBeVisible()

    await filterDashboardTasksByItem(page, fixture.name)
    const row = await dashboardTaskRow(page, fixture.name)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await expect(row.getByText(fixture.name, { exact: true })).toBeVisible()

    await row.getByRole('button', { name: /^open$/i }).click()

    await expect(page).toHaveURL(
      new RegExp(
        `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}\\?.*workspaceTab=testing`,
      ),
      { timeout: 30_000 },
    )
    await expect(page).toHaveURL(/testingTab=previewRuns/)
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByRole('tab', { name: /^template testing$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )

    // CE-U14 single-path: Pass/Fail visible without manually switching from approval → testing.
    await expect(workspaceActions(page).getByRole('button', { name: /^confirm test pass$/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect(workspaceActions(page).getByRole('button', { name: /^record test failure$/i })).toBeVisible()

    const apiItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_TESTER, { queue: 'TEST' })
    expect(apiItems.some((item) => item.workItemId === workItem.workItemId)).toBeTruthy()
  })

  test('BDD-CE-U14-DLT-002 — APPROVAL queue todo deep-links to submitApproval surface', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingApprovalDecision(request, {
      externalId: `E2E-CE-U14-DLT002-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U14 DLT-002 Appr ${Date.now().toString(36).toUpperCase()}`,
    })
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()

    const approvalItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_APPROVER, {
      queue: 'APPROVAL',
    })
    expect(approvalItems.some((item) => item.templateId === fixture.templateId)).toBeTruthy()

    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await openDashboardQueueTasks(page, 'APPROVAL')

    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /waiting on my approval/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
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
    await expect(page.getByRole('tab', { name: /^template approval$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect(workspaceActions(page).getByRole('button', { name: /^reject$/i })).toBeVisible()
  })

  test('BDD-CE-U14-DLT-003 — PENDING_RELEASE queue todo deep-links to publishReadiness', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-CE-U14-DLT003-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U14 DLT-003 Pub ${Date.now().toString(36).toUpperCase()}`,
    })
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()

    await loginAs(page, E2E_GROUP_ADMIN)
    await openDashboardQueueTasks(page, 'PENDING_RELEASE')

    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /waiting to confirm go-live/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
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
    await expect(page).toHaveURL(/approvalTab=publishReadiness/)
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(workspaceActions(page).getByRole('button', { name: /^confirm go-live$/i })).toBeVisible({
      timeout: 60_000,
    })
  })

  test('BDD-CE-U14-DLT-004 — fail-closed: author sees no lifecycle decision queue partitions', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto('/dashboard#tasks-section')
    await dismissOnboardingTourIfPresent(page)

    const tasks = page.locator('#tasks-section')
    await expect(tasks).toBeVisible({ timeout: 30_000 })
    await expect(tasks.locator('.el-skeleton')).toHaveCount(0)

    await expect(page.locator('[data-partition-id="queue-TEST"]')).toHaveCount(0)
    await expect(page.locator('[data-partition-id="queue-APPROVAL"]')).toHaveCount(0)
    await expect(page.locator('[data-partition-id="queue-PENDING_RELEASE"]')).toHaveCount(0)

    // Direct queue deep-link must not invent a TEST partition for authors.
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await dismissOnboardingTourIfPresent(page)
    await expect(page.locator('#tasks-section')).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('[data-partition-id="queue-TEST"] .el-table__row')).toHaveCount(0)
  })

  test('BDD-CE-U14-DLT-005 — pass decision resolves TEST todo from Tasks', async ({
    page,
    request,
  }) => {
    const fixture = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-CE-U14-DLT005-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U14 DLT-005 Resolve ${Date.now().toString(36).toUpperCase()}`,
    })
    const workItem = await requireOpenTestWorkItemForTemplate(request, fixture)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await openDashboardQueueTasks(page, 'TEST')
    await filterDashboardTasksByItem(page, fixture.name)
    const row = await dashboardTaskRow(page, fixture.name)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await row.getByRole('button', { name: /^open$/i }).click()

    await expect(page).toHaveURL(/workspaceTab=testing/, { timeout: 30_000 })
    await confirmTestPassAfterTesterOpen(page)

    const detail = await fetchTemplateDetail(request, fixture.templateId)
    expect(detail.lifecycleStatus).toBe('APPROVAL')

    const testItemsAfter = await listCollaborationWorkItems(request, E2E_TEMPLATE_TESTER, {
      queue: 'TEST',
    })
    expect(testItemsAfter.some((item) => item.workItemId === workItem.workItemId)).toBeFalsy()

    await openDashboardQueueTasks(page, 'TEST')
    await filterDashboardTasksByItem(page, fixture.name)
    await expect(page.locator('#tasks-section').getByText(fixture.name, { exact: true })).toHaveCount(0)
  })

  test('BDD-CE-U14-DLT-006 — behavior entry lands on TEST Tasks filter', async ({ page, request }) => {
    const fixture = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-CE-U14-DLT006-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U14 DLT-006 Nav ${Date.now().toString(36).toUpperCase()}`,
    })

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await openDashboardQueueTasks(page, 'TEST')

    await expect(page).toHaveURL(/\/dashboard\?queue=TEST/)
    await expect(page.getByRole('tab', { name: /waiting on my testing/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect
      .poll(async () => {
        return page.getByRole('tab', { name: /waiting on my testing/i }).getAttribute('aria-selected')
      }, { timeout: 15_000 })
      .toBe('true')
    await expect(page.locator('[data-partition-id="queue-TEST"]')).toBeVisible()
    await expect(page.locator('[data-partition-id^="queue-"]')).toHaveCount(1)
    await expectDashboardPartitionHeading(page, /in testing/i)

    await filterDashboardTasksByItem(page, fixture.name)
    const row = await dashboardTaskRow(page, fixture.name)
    await expect(row).toBeVisible({ timeout: 30_000 })
  })
})
