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
  await completeConfirmTestPassDialog(page)
}

/**
 * Tester/approver/team-lead dashboard Open destination after CE-U14.
 * Primary path: `/dev/...?workspaceTab=…` with decision actions already on the action rail.
 * Legacy fallback: hub `#template-lifecycle-panel` (queue-aware redirect may still land here briefly).
 */
async function waitForTesterOrApproverOpenDestination(page: Page): Promise<'hub' | 'dev'> {
  await expect(page).not.toHaveURL(/\/forbidden/, { timeout: 15_000 })

  const lifecyclePanel = page.locator('#template-lifecycle-panel')
  const devWorkspace = page.locator('#dev-workspace')
  // Wait for either destination after Open (redirect can be async).
  await expect
    .poll(async () => {
      if (await lifecyclePanel.isVisible().catch(() => false)) {
        return 'hub'
      }
      if (await devWorkspace.isVisible().catch(() => false)) {
        return 'dev'
      }
      return 'pending'
    }, { timeout: 30_000 })
    .not.toBe('pending')

  if (await lifecyclePanel.isVisible().catch(() => false)) {
    return 'hub'
  }
  await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
  await expect(devWorkspace).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('.workspace-tab-shell')).toBeVisible({ timeout: 30_000 })
  return 'dev'
}

/**
 * CE-U14: decision buttons must be on the action rail after Open — no extra tab click.
 * Hub fallback still accepts lifecycle-panel actions.
 */
async function clickWorkspaceDecisionButton(page: Page, buttonName: RegExp, destination: 'hub' | 'dev') {
  if (destination === 'hub') {
    const button = page
      .locator('.workspace-tab-shell__actions, #template-lifecycle-panel')
      .getByRole('button', { name: buttonName })
      .first()
    await expect(button).toBeVisible({ timeout: 15_000 })
    await button.click()
    return
  }

  const button = workspaceActions(page).getByRole('button', { name: buttonName })
  await expect(button).toBeVisible({ timeout: 15_000 })
  await button.click()
}

/**
 * Tester dashboard Open → open Confirm test pass dialog (does not submit).
 */
export async function openConfirmTestPassDialogAfterTesterOpen(page: Page) {
  const destination = await waitForTesterOrApproverOpenDestination(page)
  await clickWorkspaceDecisionButton(page, /^confirm test pass$/i, destination)

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText(/confirm test pass/i)).toBeVisible()
  return dialog
}

export async function confirmTestPassAfterTesterOpen(page: Page) {
  await openConfirmTestPassDialogAfterTesterOpen(page)
  await completeConfirmTestPassDialog(page)
}

/**
 * Tester dashboard Open → Record test failure → structured fail form (reason + impact + remediation).
 */
export async function confirmTestFailAfterTesterOpen(
  page: Page,
  options?: {
    reasonCategoryLabel?: RegExp
    impactSummary?: string
    remediationChecklistCode?: string
  },
) {
  const destination = await waitForTesterOrApproverOpenDestination(page)
  await clickWorkspaceDecisionButton(page, /^record test failure$/i, destination)
  await completeConfirmTestFailDialog(page, options)
}

async function completeConfirmTestPassDialog(page: Page) {
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText(/confirm test pass/i)).toBeVisible()
  await dialog.getByTestId('confirm-fidelity-viewed').click()
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

async function completeConfirmTestFailDialog(
  page: Page,
  options?: {
    reasonCategoryLabel?: RegExp
    impactSummary?: string
    remediationChecklistCode?: string
  },
) {
  const reasonCategoryLabel = options?.reasonCategoryLabel ?? /binding or layout placeholder issue/i
  const impactSummary =
    options?.impactSummary ??
    'Header binding invalid — author must fix layout placeholder and re-run full test.'
  const remediationChecklistCode = options?.remediationChecklistCode ?? 'ANCHOR_INTEGRITY'

  const dialog = page.getByRole('dialog', { name: /record test failure/i })
  await expect(dialog).toBeVisible()

  await dialog.getByRole('combobox', { name: /reason category/i }).click()
  await page.getByRole('option', { name: reasonCategoryLabel }).click()

  await dialog.getByRole('textbox', { name: /impact summary/i }).fill(impactSummary)
  await dialog
    .getByRole('textbox', { name: /remediation checklist code/i })
    .fill(remediationChecklistCode)

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

/**
 * Approver dashboard Open → open Confirm approval dialog (does not submit).
 */
export async function openApproveDialogAfterApproverOpen(page: Page) {
  const destination = await waitForTesterOrApproverOpenDestination(page)
  await clickWorkspaceDecisionButton(page, /^approve$/i, destination)

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText(/confirm approval/i)).toBeVisible()
  return dialog
}

/**
 * Approver dashboard Open → Approve with rationale + key evidence confirmation.
 * When a fidelity-viewed checkbox is present (CD-E2E-T10), it is checked as well.
 */
export async function approveTemplateAfterApproverOpen(
  page: Page,
  rationale = 'CDP E2E approval rationale — evidence reviewed and ready for release.',
) {
  const dialog = await openApproveDialogAfterApproverOpen(page)
  await dialog.getByRole('textbox', { name: /approval rationale/i }).fill(rationale)
  const fidelityConfirm = dialog.getByTestId('confirm-fidelity-viewed')
  if ((await fidelityConfirm.count()) > 0) {
    await fidelityConfirm.click()
  }
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
  const fidelityConfirm = dialog.getByTestId('confirm-fidelity-viewed')
  if ((await fidelityConfirm.count()) > 0) {
    await fidelityConfirm.click()
  }
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
  const fidelityConfirm = dialog.getByTestId('confirm-fidelity-viewed')
  if ((await fidelityConfirm.count()) > 0) {
    await fidelityConfirm.click()
  }
  await dialog.getByRole('button', { name: /^confirm go-live$/i }).click()

  const publishResponse = await publishResponsePromise
  expect(publishResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/template is now live/i)).toBeVisible({
    timeout: 15_000,
  })
}

/**
 * Team-lead dashboard Open → open Go-live summary dialog (does not confirm).
 * Asserts fidelity/coverage validation summaries are visible in the dialog.
 */
export async function openGoLiveSummaryAfterTeamLeadOpen(page: Page) {
  const destination = await waitForTesterOrApproverOpenDestination(page)

  if (destination === 'hub') {
    const publishButton = page
      .locator('.workspace-tab-shell__actions, #template-lifecycle-panel')
      .getByRole('button', { name: /^confirm go-live$/i })
      .first()
    await expect(publishButton).toBeEnabled({ timeout: 60_000 })
    await publishButton.click()
  } else {
    // CE-U14: PENDING_RELEASE Open lands on publishReadiness with Confirm go-live visible.
    const publishButton = workspaceActions(page).getByRole('button', { name: /^confirm go-live$/i })
    await expect(publishButton).toBeEnabled({ timeout: 60_000 })
    await publishButton.click()
  }

  const dialog = page.locator('.el-dialog').filter({ hasText: /go-live summary/i })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByText(/validation summaries/i)).toBeVisible()
  await expect(dialog.getByText(/coverage/i).first()).toBeVisible()
  await expect(dialog.getByText('Release version', { exact: true })).toBeVisible()
  return dialog
}

/**
 * Team-lead dashboard Open → Go-live summary → Confirm go-live (browser publish).
 */
export async function confirmGoLiveAfterTeamLeadOpen(page: Page) {
  const dialog = await openGoLiveSummaryAfterTeamLeadOpen(page)
  const fidelityConfirm = dialog.getByTestId('confirm-fidelity-viewed')
  if ((await fidelityConfirm.count()) > 0) {
    await fidelityConfirm.click()
  }

  const publishResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().includes('/lifecycle/publish'),
    { timeout: 30_000 },
  )

  await dialog.getByRole('button', { name: /^confirm go-live$/i }).click()

  const publishResponse = await publishResponsePromise
  expect(publishResponse.ok()).toBeTruthy()
  await expect(page.locator('.el-message').getByText(/template is now live/i)).toBeVisible({
    timeout: 15_000,
  })
}

export async function saveApiRetentionPolicyFromHubTab(page: Page) {
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const retentionSection = page.locator('#policy-domain-INVOCATION_RETENTION')
  await expect(retentionSection).toBeVisible({ timeout: 30_000 })

  const retentionSelect = retentionSection.locator('.retention-select').first()
  await expect(retentionSelect).toBeVisible()
  const currentLabel = ((await retentionSelect.textContent()) ?? '').trim()

  // Pick a different preset so the form becomes dirty and Save enables.
  const candidates = [/30 days/i, /90 days/i, /180 days/i, /365 days/i]
  const nextOption = candidates.find((pattern) => !pattern.test(currentLabel)) ?? /180 days/i

  await retentionSelect.click()
  await page.getByRole('option', { name: nextOption }).click()

  const saveButton = retentionSection.getByTestId('retention-save-button')
  await expect(saveButton).toBeEnabled({ timeout: 15_000 })

  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' &&
      response.url().includes('/api/policy/invocation-retention'),
    { timeout: 30_000 },
  )

  await saveButton.click()
  await expect(page.locator('.el-message-box')).toBeVisible({ timeout: 10_000 })
  await confirmPolicyChangeDialog(page)

  const saveResponse = await saveResponsePromise
  expect(saveResponse.ok()).toBeTruthy()
  await expect(page.getByTestId('retention-save-success')).toBeVisible({
    timeout: 15_000,
  })
}

export async function expandApiPolicyAdvancedSettings(page: Page) {
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
  const advancedToggle = page.getByRole('button', { name: /advanced settings|高级设置/i })
  await expect(advancedToggle).toBeVisible()
  const saveOutputButton = page.getByRole('button', { name: /save output settings|保存输出设置/i })
  if ((await saveOutputButton.count()) === 0) {
    await advancedToggle.click()
  }
  await expect(saveOutputButton).toBeVisible()
}

export async function editOutputPolicyCandidate(
  page: Page,
  currentFormats: string[],
  currentModes: string[],
) {
  const advancedRegion = page.getByRole('region', { name: /advanced settings|高级设置/i })
  const modes = currentModes ?? []
  const formatSelect = advancedRegion.getByRole('combobox').nth(0)
  const modeSelect = advancedRegion.getByRole('combobox').nth(1)

  if (!currentFormats.includes('PDF')) {
    await formatSelect.click({ force: true })
    await page.getByRole('option', { name: 'PDF' }).click()
    await page.keyboard.press('Escape')
    return
  }
  if (!modes.includes('INLINE')) {
    await modeSelect.click({ force: true })
    await page.getByRole('option', { name: 'INLINE' }).click()
    await page.keyboard.press('Escape')
    return
  }
  if (!modes.includes('SYNC_DOWNLOAD_URL')) {
    await modeSelect.click({ force: true })
    await page.getByRole('option', { name: 'SYNC_DOWNLOAD_URL' }).click()
    await page.keyboard.press('Escape')
    return
  }
  await modeSelect.click({ force: true })
  await page.getByRole('option', { name: 'INLINE' }).click()
  await page.keyboard.press('Escape')
}

async function confirmPolicyChangeDialog(page: Page) {
  const messageBox = page.locator('.el-message-box')
  await expect(messageBox).toBeVisible()
  await messageBox.getByRole('button', { name: /^(ok|confirm)$/i }).click()
}

export async function saveApiOutputPolicyFromHubTab(page: Page) {
  await expandApiPolicyAdvancedSettings(page)

  const previewPromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/api/policy/impact-preview'),
    { timeout: 45_000 },
  )
  const savePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' && response.url().includes('/api/policy/output'),
    { timeout: 60_000 },
  )

  await page.getByRole('button', { name: /save output settings|保存输出设置/i }).click()

  const previewResponse = await previewPromise
  expect(previewResponse.ok()).toBeTruthy()

  await expect(page.locator('.el-message-box')).toBeVisible({ timeout: 10_000 })
  await confirmPolicyChangeDialog(page)

  const saveResponse = await savePromise
  expect(saveResponse.ok()).toBeTruthy()

  await expect(
    page.locator('.el-message').getByText(/access setting saved|访问设置已保存/i),
  ).toBeVisible({ timeout: 15_000 })
}

export async function saveDefaultRouteFromHubTab(page: Page, releaseVersion: string) {
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const routeSection = page.locator('#policy-domain-DEFAULT_ROUTE_TARGET')
  const routeInput = routeSection.locator('input').first()
  await expect(routeInput).toBeVisible()
  await routeInput.fill(releaseVersion)

  const previewPromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/api/policy/impact-preview'),
    { timeout: 45_000 },
  )
  const savePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'PUT' && response.url().includes('/api/policy/default-route'),
    { timeout: 60_000 },
  )

  await routeSection.getByRole('button', { name: /save default route|保存默认路由/i }).click()

  const previewResponse = await previewPromise
  expect(previewResponse.ok()).toBeTruthy()

  await expect(page.locator('.el-message-box')).toBeVisible({ timeout: 10_000 })
  await confirmPolicyChangeDialog(page)

  const saveResponse = await savePromise
  expect(saveResponse.ok()).toBeTruthy()

  await expect(
    page.locator('.el-message').getByText(/access setting saved|访问设置已保存/i),
  ).toBeVisible({ timeout: 15_000 })
}

/**
 * BDD-CDP-APIPOL-002 — set DEFAULT_ROUTE_TARGET to a non-callable release, run impact preview,
 * and assert hard-block UI (no PUT / save disabled). Does not assume a successful save.
 */
export async function attemptNonCallableDefaultRouteHardBlock(
  page: Page,
  releaseVersion = '9.9.9-non-callable',
): Promise<{ defaultRoutePutCount: number }> {
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const routeSection = page.locator('#policy-domain-DEFAULT_ROUTE_TARGET')
  await routeSection.scrollIntoViewIfNeeded()

  const routeInput = routeSection.locator('input').first()
  await expect(routeInput).toBeVisible()
  await routeInput.fill(releaseVersion)

  let defaultRoutePutCount = 0
  const onRequest = (req: { method: () => string; url: () => string }) => {
    if (req.method() === 'PUT' && req.url().includes('/api/policy/default-route')) {
      defaultRoutePutCount += 1
    }
  }
  page.on('request', onRequest)

  const saveButton = routeSection.getByTestId('default-route-save-button')
  await expect(saveButton).toBeEnabled()

  const previewPromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url().includes('/api/policy/impact-preview'),
    { timeout: 45_000 },
  )

  await saveButton.click()

  const previewResponse = await previewPromise
  expect(previewResponse.ok()).toBeTruthy()
  const previewBody = (await previewResponse.json()) as {
    result?: { blocking?: boolean; warnings?: string[] }
    blocking?: boolean
    warnings?: string[]
  }
  const preview =
    previewBody.result && typeof previewBody.result === 'object'
      ? previewBody.result
      : previewBody
  expect(preview.blocking).toBe(true)

  // Hard-block path must not open the warning confirm dialog used by soft warnings.
  await expect(page.locator('.el-message-box')).toHaveCount(0)

  const hardBlock = routeSection.getByTestId('api-policy-hard-block-finding')
  await expect(hardBlock).toBeVisible({ timeout: 15_000 })
  await expect(routeSection.getByTestId('api-policy-hard-block-reason')).not.toBeEmpty()
  await expect(routeSection.getByTestId('api-policy-hard-block-impact')).not.toBeEmpty()
  await expect(routeSection.getByTestId('api-policy-hard-block-advice')).not.toBeEmpty()
  await expect(routeSection.getByTestId('api-policy-hard-block-error-code')).toHaveText(
    'DEFAULT_ROUTE_TARGET_UNAVAILABLE',
  )

  await expect(saveButton).toBeDisabled()
  await expect(
    page
      .locator('.el-message')
      .getByText(/save is blocked until blocking impacts are resolved|保存被阻止|无法保存/i),
  ).toBeVisible({ timeout: 10_000 })

  // Allow any late PUT that would race the hard-block return path.
  await expect.poll(() => defaultRoutePutCount, { timeout: 2_000 }).toBe(0)

  page.off('request', onRequest)
  return { defaultRoutePutCount }
}

/** @deprecated Hub deep-link redirects to dev editor; use openDevEditorWorkspaceTab. */
export async function openTemplateLifecycleTab(page: Page, templateId: string, request: APIRequestContext) {
  await openDevEditorWorkspaceTab(page, templateId, request, 'approval')
}

export async function submitForTestingFromLifecycleTab(page: Page, comment?: string) {
  await submitForTestingFromDevWorkspace(page, comment)
}

export async function confirmTestPassFromLifecycleTab(page: Page) {
  await confirmTestPassAfterTesterOpen(page)
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
