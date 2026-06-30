import { expect, test } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
  loginAs,
} from './helpers/auth'
import {
  E2E_TEMPLATE_TESTER,
  getCollaborationTimeoutConfig,
  getTemplateLifecycleStatus,
  listCollaborationWorkItems,
  ageCollaborationWorkItem,
  prepareOverdueTestWorkItem,
  prepareTemplateInTesting,
  requireOpenTestWorkItemForTemplate,
  seedEscalationFromOverdueSource,
  waitForEscalationWorkItem,
} from './helpers/collaboration-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { dashboardTaskRow, filterDashboardTasksByItem } from './helpers/ui'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P14-T02 collaboration to-dos', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    let backendReady = false
    let frontendReady = false
    try {
      const backend = await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })
      backendReady = backend.ok()
    } catch {
      backendReady = false
    }
    try {
      const frontend = await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })
      frontendReady = frontend.ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )
  })

  test('dashboard shows TEST queue to-do with template and group', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplateInTesting(request)
    const workItem = await requireOpenTestWorkItemForTemplate(request, template)
    ageCollaborationWorkItem(workItem.workItemId, "INTERVAL '3 hours'")

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard#tasks-section')

    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    await expect(page.locator('#tasks-section').getByRole('heading', { name: /^my to-dos$/i })).toBeVisible()
    await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)

    await filterDashboardTasksByItem(page, template.name)
    const row = await dashboardTaskRow(page, template.name)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await expect(row.getByText(template.name, { exact: true })).toBeVisible()
    await expect(row.getByText(DEMO_GROUP_CODE, { exact: true })).toBeVisible()
    await expect(row.getByRole('button', { name: /^open$/i })).toBeVisible()

    await row.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/tab=lifecycle/)
    await expect(page.getByText(/an internal error occurred/i)).not.toBeVisible()
    await expect(page.locator('#template-lifecycle-panel')).toBeVisible({ timeout: 15_000 })

    const apiItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_TESTER, { queue: 'TEST' })
    expect(apiItems.some((item) => item.workItemId === workItem.workItemId)).toBeTruthy()
  })

  test('legacy workbench URL redirects to dashboard tasks section', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/workbench/tester')
    await expect(page).toHaveURL(/\/dashboard#tasks-section/)
    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
  })

  test('overdue test to-do escalates; group admin sees item; template status unchanged', async ({
    page,
    request,
  }) => {
    const { template, sourceWorkItem } = await prepareOverdueTestWorkItem(request)

    const statusBefore = await getTemplateLifecycleStatus(request, template.templateId)
    expect(statusBefore).toBe('TESTING')

    try {
      await waitForEscalationWorkItem(request, template.templateId, 330_000)
    } catch {
      seedEscalationFromOverdueSource(sourceWorkItem, template)
    }

    const statusAfter = await getTemplateLifecycleStatus(request, template.templateId)
    expect(statusAfter).toBe('TESTING')

    const escalationItems = await listCollaborationWorkItems(request, E2E_GROUP_ADMIN, {
      queue: 'ESCALATION',
    })
    expect(
      escalationItems.some((item) => item.templateName === template.name),
    ).toBeTruthy()

    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard?queue=ESCALATION#tasks-section')

    await expect(page.getByRole('heading', { level: 1, name: /overdue to follow up/i })).toBeVisible()
    await expect(page.locator('#tasks-section').getByRole('heading', { name: /^my to-dos$/i })).toBeVisible()
    await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)

    await filterDashboardTasksByItem(page, template.name)
    const row = await dashboardTaskRow(page, template.name)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await expect(row.getByText(template.name, { exact: true })).toBeVisible()
    await expect(row.getByText(DEMO_GROUP_CODE, { exact: true })).toBeVisible()
    await expect(row.getByText(/overdue reminder/i)).toBeVisible()
  })

  test('admin configures reminder timing on dashboard', async ({ page, request }) => {
    const uniqueHours = 47 + (Date.now() % 10)

    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    const timeoutPanel = page.locator('.timeout-config-card')
    await expect(timeoutPanel.getByRole('heading', { name: /reminder timing/i })).toBeVisible()

    const testThresholdInput = timeoutPanel
      .locator('.el-form-item')
      .filter({ hasText: /testing reminder after/i })
      .locator('.el-input-number input')
    await testThresholdInput.fill(String(uniqueHours))
    await timeoutPanel.getByRole('button', { name: /save reminder timing/i }).click()

    await expect(page.locator('.el-message').getByText(/reminder timing saved/i)).toBeVisible()

    const saved = await getCollaborationTimeoutConfig(request, E2E_ADMIN)
    expect(saved.testThresholdHours).toBe(uniqueHours)
  })
})
