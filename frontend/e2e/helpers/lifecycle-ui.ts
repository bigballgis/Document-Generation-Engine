import { expect, type APIRequestContext, type Page } from '@playwright/test'

import { listTemplateVersionLines } from './template-version-lines-api'

async function devEditorUrl(
  request: APIRequestContext,
  templateId: string,
  workspaceTab: 'testing' | 'approval',
): Promise<string> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight) {
    throw new Error(`No in-flight dev version for template ${templateId}`)
  }
  return `/templates/${templateId}/dev/${inFlight.devVersionId}?workspaceTab=${workspaceTab}`
}

export async function openDevEditorWorkspaceTab(
  page: Page,
  templateId: string,
  request: APIRequestContext,
  workspaceTab: 'testing' | 'approval',
): Promise<void> {
  await page.goto(await devEditorUrl(request, templateId, workspaceTab))
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('.workspace-tab-shell')).toBeVisible({ timeout: 30_000 })
}

function workspaceActions(page: Page) {
  return page.locator('.workspace-tab-shell__actions')
}

export async function submitForTestingFromDevWorkspace(page: Page, comment = 'CDP E2E submit for testing') {
  const workspace = page.locator('.workspace-tab-shell')
  const submitButton = workspaceActions(page).getByRole('button', { name: /^submit for testing$/i })

  const eligibilityRefresh = page.waitForResponse(
    (response) => response.url().includes('/batch-tests/submit-eligibility') && response.ok(),
    { timeout: 60_000 },
  )
  await workspace.getByRole('tab', { name: /^template approval$/i }).click()
  await workspace.getByRole('tab', { name: /^template testing$/i }).click()
  await eligibilityRefresh.catch(() => undefined)

  await expect(page.locator('.test-data-set-panel tbody tr').first()).toBeVisible({ timeout: 30_000 })

  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes('/lifecycle/submit-test'),
    { timeout: 30_000 },
  )

  const enabled = await submitButton.isEnabled().catch(() => false)
  if (!enabled) {
    await submitButton.click({ force: true })
  } else {
    await submitButton.click()
  }
  const dialog = page.locator('.el-dialog').filter({ hasText: /submit for testing/i })
  await expect(dialog).toBeVisible()
  await dialog.locator('textarea').fill(comment)
  await dialog.getByRole('button', { name: /^submit for testing$/i }).click()

  const submitResponse = await submitResponsePromise
  expect(submitResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/template submitted for testing/i)).toBeVisible({
    timeout: 15_000,
  })
}

export async function confirmTestPassFromDevWorkspace(page: Page) {
  await workspaceActions(page).getByRole('button', { name: /^confirm test pass$/i }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText(/confirm test pass/i)).toBeVisible()
  await dialog.getByText(/I reviewed fidelity warnings/i).click()
  await dialog.getByText(/I reviewed the coverage summary/i).click()
  await dialog.getByText(/I reviewed the structured preview comparison/i).click()

  const decisionResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes('/lifecycle/test-decision'),
    { timeout: 30_000 },
  )

  await dialog.getByRole('button', { name: /^submit decision$/i }).click()
  const decisionResponse = await decisionResponsePromise
  expect(decisionResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/test decision recorded/i)).toBeVisible({
    timeout: 15_000,
  })
}

export async function submitForApprovalFromDevWorkspace(page: Page) {
  const submitButton = workspaceActions(page).getByRole('button', { name: /^submit for approval$/i })
  await expect(submitButton).toBeEnabled({ timeout: 60_000 })

  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes('/lifecycle/submit-approval'),
    { timeout: 30_000 },
  )

  await submitButton.click()
  const dialog = page.locator('.el-dialog').filter({ hasText: /review before submit for approval/i })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: /^confirm submit for approval$/i }).click()

  const submitResponse = await submitResponsePromise
  expect(submitResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/submitted for approval/i)).toBeVisible({
    timeout: 15_000,
  })
}

export async function approveTemplateFromDevWorkspace(
  page: Page,
  rationale = 'CDP E2E approval rationale — evidence reviewed and ready for release.',
) {
  await workspaceActions(page).getByRole('button', { name: /^approve$/i }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText(/confirm approval/i)).toBeVisible()
  await dialog.getByRole('textbox', { name: /approval rationale/i }).fill(rationale)
  await dialog.getByText(/I reviewed key evidence/i).click()

  const decisionResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes('/lifecycle/approval-decision'),
    { timeout: 30_000 },
  )

  await dialog.getByRole('button', { name: /^submit decision$/i }).click()
  const decisionResponse = await decisionResponsePromise
  expect(decisionResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/approval decision recorded/i)).toBeVisible({
    timeout: 15_000,
  })
}

export async function confirmGoLiveFromDevWorkspace(page: Page) {
  const publishButton = workspaceActions(page).getByRole('button', { name: /^confirm go-live$/i })
  await expect(publishButton).toBeEnabled({ timeout: 60_000 })

  const publishResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes('/lifecycle/publish'),
    { timeout: 30_000 },
  )

  await publishButton.click()
  const dialog = page.locator('.el-dialog').filter({ hasText: /go-live summary/i })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: /^confirm go-live$/i }).click()

  const publishResponse = await publishResponsePromise
  expect(publishResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/template is now live/i)).toBeVisible({
    timeout: 15_000,
  })
}

export async function saveApiRetentionPolicyFromHubTab(page: Page) {
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const retentionSelect = page.locator('.retention-select').first()
  await expect(retentionSelect).toBeVisible()
  await retentionSelect.click()
  await page.getByRole('option', { name: /90 days/i }).click()

  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/api/policy/invocation-retention'),
    { timeout: 30_000 },
  )

  await page.getByRole('button', { name: /^save retention$/i }).click()
  const confirmBox = page.locator('.el-message-box')
  await expect(confirmBox).toBeVisible()
  await confirmBox.getByRole('button', { name: /^confirm$/i }).click()

  const saveResponse = await saveResponsePromise
  expect(saveResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/retention settings saved|access setting saved/i)).toBeVisible({
    timeout: 15_000,
  })
}

/** @deprecated Hub deep-link redirects to dev editor; use openDevEditorWorkspaceTab. */
export async function openTemplateLifecycleTab(page: Page, templateId: string, request: APIRequestContext) {
  await openDevEditorWorkspaceTab(page, templateId, request, 'approval')
}

export async function submitForTestingFromLifecycleTab(page: Page, comment?: string) {
  await submitForTestingFromDevWorkspace(page, comment)
}

export async function confirmTestPassFromLifecycleTab(page: Page) {
  await confirmTestPassFromDevWorkspace(page)
}

export async function submitForApprovalFromLifecycleTab(page: Page) {
  await submitForApprovalFromDevWorkspace(page)
}

export async function approveTemplateFromLifecycleTab(page: Page, rationale?: string) {
  await approveTemplateFromDevWorkspace(page, rationale)
}

export async function confirmGoLiveFromLifecycleTab(page: Page) {
  await confirmGoLiveFromDevWorkspace(page)
}
