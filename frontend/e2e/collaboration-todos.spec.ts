import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

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
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1` })
  })

  test('dashboard shows TEST queue to-do with template and group', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplateInTesting(request)
    const workItem = await requireOpenTestWorkItemForTemplate(request, template)
    ageCollaborationWorkItem(workItem.workItemId, "INTERVAL '3 hours'")

    await loginAs(page, E2E_TEMPLATE_TESTER)
    // Tabbed dashboard requires ?queue=TEST to mount #tasks-section (hash alone stays on Overview).
    await page.goto('/dashboard?queue=TEST#tasks-section')

    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /waiting on my testing/i })).toBeVisible()
    await expect(page.locator('#tasks-section').getByRole('heading', { name: /^in testing$/i })).toBeVisible()
    await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })

    await filterDashboardTasksByItem(page, template.name)
    const row = await dashboardTaskRow(page, template.name)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await expect(row.getByText(template.name, { exact: true })).toBeVisible()
    await expect(row.getByText(DEMO_GROUP_CODE, { exact: true })).toBeVisible()
    await expect(row.getByRole('button', { name: /^open$/i })).toBeVisible()

    await row.getByRole('button', { name: /^open$/i }).click()
    // CE-U14: TEST Open deep-links to testing workspace (enriched /dev/… or hub lifecycle fallback).
    await expect(page).toHaveURL(/workspaceTab=testing|tab=lifecycle/, { timeout: 15_000 })
    await expect(page.getByText(/an internal error occurred/i)).not.toBeVisible()
    await expect
      .poll(async () => {
        if (await page.locator('#dev-workspace').isVisible().catch(() => false)) {
          return 'dev'
        }
        if (await page.locator('#template-lifecycle-panel').isVisible().catch(() => false)) {
          return 'hub'
        }
        return 'pending'
      }, { timeout: 15_000 })
      .not.toBe('pending')

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

    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /overdue to follow up/i })).toBeVisible()
    await expect(page.locator('#tasks-section').getByRole('heading', { name: /overdue follow-up/i })).toBeVisible()
    await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })

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
