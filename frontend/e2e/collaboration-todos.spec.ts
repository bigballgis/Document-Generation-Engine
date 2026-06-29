import { expect, test } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
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

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

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

  test('tester workbench shows TEST queue to-do with template, group, submitter, age', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplateInTesting(request)
    const workItem = await requireOpenTestWorkItemForTemplate(request, template)
    ageCollaborationWorkItem(workItem.workItemId, "INTERVAL '3 hours'")

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/workbench/tester')

    await expect(page.getByRole('heading', { name: /tester workbench/i })).toBeVisible()
    await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)

    const row = page.getByRole('row', { name: new RegExp(template.name) })
    await expect(row).toBeVisible()
    await expect(row.getByText(template.name, { exact: true })).toBeVisible()
    await expect(row.getByText(DEMO_GROUP_CODE, { exact: true })).toBeVisible()
    await expect(row.getByText(E2E_TEMPLATE_AUTHOR.username, { exact: true })).toBeVisible()
    await expect(row.getByText(/\d+h|\d+d/)).toBeVisible()

    const apiItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_TESTER, { queue: 'TEST' })
    expect(apiItems.some((item) => item.workItemId === workItem.workItemId)).toBeTruthy()
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
    await page.goto('/workbench/escalation')

    await expect(page.getByRole('heading', { name: /escalation workbench/i })).toBeVisible()
    await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)

    const row = page.getByRole('row', { name: new RegExp(template.name) })
    await expect(row).toBeVisible()
    await expect(row.getByText(template.name, { exact: true })).toBeVisible()
    await expect(row.getByText(DEMO_GROUP_CODE, { exact: true })).toBeVisible()
    await expect(row.getByText(/exceeded.*threshold/i)).toBeVisible()
  })

  test('admin configures collaboration timeout thresholds on dashboard', async ({ page, request }) => {
    const uniqueHours = 47 + (Date.now() % 10)

    await loginAs(page, E2E_ADMIN)
    await page.goto('/dashboard')

    await expect(page.getByRole('heading', { name: /my tasks/i })).toBeVisible()
    const timeoutPanel = page.locator('.timeout-config-card')
    await expect(timeoutPanel.getByRole('heading', { name: /collaboration timeout thresholds/i })).toBeVisible()

    const testThresholdInput = timeoutPanel
      .locator('.el-form-item')
      .filter({ hasText: /testing threshold/i })
      .locator('.el-input-number input')
    await testThresholdInput.fill(String(uniqueHours))
    await timeoutPanel.getByRole('button', { name: /save thresholds/i }).click()

    await expect(page.locator('.el-message').getByText(/timeout thresholds saved/i)).toBeVisible()

    const saved = await getCollaborationTimeoutConfig(request, E2E_ADMIN)
    expect(saved.testThresholdHours).toBe(uniqueHours)
  })
})
