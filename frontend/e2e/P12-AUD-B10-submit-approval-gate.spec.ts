import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_TEMPLATE_APPROVER, E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { listCollaborationWorkItems } from './helpers/collaboration-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  fetchTemplateDetail,
  prepareTemplatePendingSubmitBlocked,
  prepareTemplatePendingSubmitReady,
} from './helpers/submit-approval-gate-api'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

async function openTemplateLifecycleTab(page: import('@playwright/test').Page, templateId: string) {
  await page.goto(`/templates/${templateId}?tab=lifecycle`)
  const lifecyclePanel = page.locator('#template-lifecycle-panel')
  await expect(lifecyclePanel).toBeVisible({ timeout: 30_000 })
  await expect(lifecyclePanel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  return lifecyclePanel
}

function lifecycleSubmitButton(page: import('@playwright/test').Page) {
  return page
    .locator('#template-lifecycle-panel')
    .getByRole('button', { name: /^submit for approval$/i })
}

function submitSummaryDialog(page: import('@playwright/test').Page) {
  return page.locator('.el-dialog').filter({ hasText: /review before submit for approval/i })
}

test.describe('P12-AUD-B10 submit-for-approval evidence checklist gate (§5.8)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1` })
  })

  test('pass path: green checklist → summary dialog → confirm → PENDING_DECISION + APPROVAL work item', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplatePendingSubmitReady(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const lifecyclePanel = await openTemplateLifecycleTab(page, template.templateId)

    const submitGateCard = lifecyclePanel.locator('.submit-gate-card')
    await expect(submitGateCard).toBeVisible()
    await expect(submitGateCard.getByRole('heading', { name: /^submission readiness checks$/i })).toBeVisible()
    await expect(submitGateCard.getByText(/batch test results are available/i)).toBeVisible()
    await expect(submitGateCard.getByText(/^informational$/i).first()).toBeVisible()

    const submitButton = lifecycleSubmitButton(page)
    await expect(submitButton).toBeEnabled()

    await lifecyclePanel.getByRole('textbox', { name: /optional comment for the activity log/i }).fill(
      'E2E submit-for-approval gate pass path',
    )

    const submitResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/lifecycle/submit-approval'),
      { timeout: 30_000 },
    )

    await submitButton.click()
    const dialog = submitSummaryDialog(page)
    await expect(dialog).toBeVisible()
    await expect(dialog.getByText(/review the submission checklist/i)).toBeVisible()

    const confirmButton = dialog.getByRole('button', { name: /^confirm submit for approval$/i })
    await expect(confirmButton).toBeEnabled()
    await confirmButton.click()

    const submitResponse = await submitResponsePromise
    expect(submitResponse.ok()).toBeTruthy()

    await expect(page.locator('.el-message').getByText(/template submitted for approval/i)).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.getByText(/^awaiting approval$/i).first()).toBeVisible()

    const detail = await fetchTemplateDetail(request, template.templateId)
    expect(detail.lifecycleStatus).toBe('APPROVAL')
    expect(detail.approvalSubState).toBe('PENDING_DECISION')

    const approvalItems = await listCollaborationWorkItems(request, E2E_TEMPLATE_APPROVER, {
      queue: 'APPROVAL',
    })
    expect(approvalItems.some((item) => item.templateId === template.templateId)).toBeTruthy()
  })

  test('blocker path: hard blocker keeps submit disabled and no submit-approval API call', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplatePendingSubmitBlocked(request)

    let submitApprovalPosts = 0
    page.on('request', (req) => {
      if (req.method() === 'POST' && req.url().includes('/lifecycle/submit-approval')) {
        submitApprovalPosts += 1
      }
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const lifecyclePanel = await openTemplateLifecycleTab(page, template.templateId)

    const submitGateCard = lifecyclePanel.locator('.submit-gate-card')
    await expect(submitGateCard).toBeVisible()
    await expect(submitGateCard.getByText(/no batch test run recorded/i)).toBeVisible()
    await expect(submitGateCard.getByText(/^pending$/i).first()).toBeVisible()
    await expect(submitGateCard.getByText(/resolve blockers before submitting|fix pending items/i)).not.toBeVisible()

    const submitButton = lifecycleSubmitButton(page)
    await expect(submitButton).toBeDisabled()

    await expect(submitSummaryDialog(page)).toHaveCount(0)
    expect(submitApprovalPosts).toBe(0)

    const detail = await fetchTemplateDetail(request, template.templateId)
    expect(detail.lifecycleStatus).toBe('APPROVAL')
    expect(detail.approvalSubState).toBe('PENDING_SUBMIT')
  })

  test('lifecycle tab shows submit-phase checklist for PENDING_SUBMIT templates', async ({
    page,
    request,
  }) => {
    const template = await prepareTemplatePendingSubmitReady(request, {
      externalId: undefined,
      name: `E2E Submit Gate Checklist ${Date.now().toString(36).toUpperCase()}`,
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    const lifecyclePanel = await openTemplateLifecycleTab(page, template.templateId)

    const submitGateCard = lifecyclePanel.locator('.submit-gate-card')
    await expect(submitGateCard).toBeVisible()
    await expect(submitGateCard.getByRole('heading', { name: /^submission readiness checks$/i })).toBeVisible()
    await expect(submitGateCard.getByText(/layout placeholder bindings are valid/i)).toBeVisible()
    await expect(submitGateCard.getByText(/variable schema is configured/i)).toBeVisible()
    await expect(submitGateCard.getByText(/successful preview artifacts exist/i)).toBeVisible()
    await expect(submitGateCard.getByText(/^informational$/i).first()).toBeVisible()
  })
})
