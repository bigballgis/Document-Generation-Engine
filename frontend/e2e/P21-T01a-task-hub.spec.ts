import { expect, test } from '@playwright/test'

import { E2E_GROUP_ADMIN, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import {
  ageCollaborationWorkItem,
  prepareOverdueTestWorkItem,
  prepareTemplateInTesting,
  requireOpenTestWorkItemForTemplate,
  seedEscalationFromOverdueSource,
} from './helpers/collaboration-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  dashboardTaskRow,
  expectDashboardPartitionHeading,
  filterDashboardTasksByItem,
} from './helpers/ui'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T01a task hub deepening (§12.3)', () => {
  test.describe.configure({ mode: 'serial' })

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
      `Stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start backend on :8080 and pnpm dev on :5173 (or docker on :4173).`,
    )
  })

  test('TEST deep link shows queue title, single partition, restored columns, and Open action', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplateInTesting(request)
    const workItem = await requireOpenTestWorkItemForTemplate(request, template)
    ageCollaborationWorkItem(workItem.workItemId, "INTERVAL '2 hours'")

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')

    await expect(page.getByRole('heading', { level: 1, name: /waiting on my testing/i })).toBeVisible()
    await expectDashboardPartitionHeading(page, /in testing/i)
    await expect(page.locator('[data-partition-id^="queue-"]')).toHaveCount(1)

    await filterDashboardTasksByItem(page, template.name)
    const row = await dashboardTaskRow(page, template.name)
    await expect(row.locator('.summary-cell')).toContainText(/template submitted for testing/i)
    await expect(row.getByText('2h', { exact: true })).toBeVisible()
    await expect(row.getByText(workItem.submitterUserId, { exact: true })).toBeVisible()

    await row.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(/\/templates\/[^/?]+\?tab=lifecycle/)
  })

  test('invalid queue falls back to unfiltered hub title', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=NOT_A_QUEUE#tasks-section')

    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
  })

  test('master-review filter shows master title and skips collaboration partitions', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard?filter=master-review#tasks-section')

    await expect(page.getByRole('heading', { level: 1, name: /masters to review/i })).toBeVisible()
    await expect(page.locator('[data-partition-id^="queue-"]')).toHaveCount(0)
  })

  test('ESCALATION queue shows template-escalation row with overdue badge', async ({ page, request }) => {
    const { template, sourceWorkItem } = await prepareOverdueTestWorkItem(request)
    seedEscalationFromOverdueSource(sourceWorkItem, template)

    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard?queue=ESCALATION#tasks-section')

    await expect(page.getByRole('heading', { level: 1, name: /overdue to follow up/i })).toBeVisible()
    await expectDashboardPartitionHeading(page, /overdue follow-up/i)
    await expect(page.locator('[data-partition-id^="queue-"]')).toHaveCount(1)

    await filterDashboardTasksByItem(page, template.name)
    const row = await dashboardTaskRow(page, template.name)
    await expect(row.getByText(/overdue to follow up/i)).toBeVisible()
    await expect(row.getByText(/overdue reminder/i)).toBeVisible()
    await expect(row.getByText(/exceeded the configured timeout threshold/i)).toBeVisible()
  })

  test('collaboration load failure shows segmented error without hiding dashboard shell', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.route('**/collaboration-work-items**', (route) =>
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-collab-fail' },
          error: {
            code: 'INTERNAL_ERROR',
            category: 'SYSTEM',
            retryable: true,
            message: 'Unable to load collaboration to-do items.',
            messageKey: 'collaboration.workItems.error.load',
          },
        }),
      }),
    )

    await page.goto('/dashboard#tasks-section')

    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByText(/unable to load collaboration to-do items/i)).toBeVisible()
    await expect(page.locator('#tasks-section')).toBeVisible()
    await expect(page).not.toHaveURL(/\/forbidden/)
  })
})
