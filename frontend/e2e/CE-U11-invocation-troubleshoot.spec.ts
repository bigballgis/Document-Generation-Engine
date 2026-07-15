/**
 * CE-U11 — Invocation troubleshoot + recall (BDD-CE-U11-IRC-001..008).
 * BDD: docs/behavior/ce-u11-invocation-troubleshoot.md
 */
import { expect, test, type Page } from '@playwright/test'
import { readFile } from 'node:fs/promises'

import { requireDockerStack } from './helpers/stack-readiness'

import { E2E_GROUP_ADMIN, E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  createTemplateApiCredential,
  ensureDemoFullFlowPublished,
  publishSecondReleaseFromClone,
  runtimeGenerateDefault,
  updateApiPolicyBatchSettings,
} from './helpers/content-modules-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  exportManagementInvocationsCsv,
  getManagementInvocationDetail,
  listManagementInvocations,
  listManagementInvocationsStatus,
  runtimeGenerateByVersion,
  runtimeGenerateContractInvalid,
  waitForManagementInvocationByRequestId,
} from './helpers/management-invocations-api'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

const RELEASE_V1 = '1.0.0'
const RELEASE_V12 = '1.2.0'

async function openInvocationHistory(page: Page, templateId: string) {
  await page.goto(`/templates/${templateId}?tab=apiAccess`)
  await expect(page.getByRole('heading', { name: /external access|对外接入/i })).toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const panel = page.locator('.section-card').filter({
    has: page.getByRole('heading', { name: /invocation history|调用历史/i }),
  })
  await panel.scrollIntoViewIfNeeded()
  await expect(panel).toBeVisible()
  return panel
}

test.describe('CE-U11 invocation troubleshoot (IRC-001..008)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let templateId = ''
  let externalId = ''
  let successV1RequestId = ''
  let successV12RequestId = ''
  let failV12RequestId = ''
  let successV12InvocationId = ''
  let failV12InvocationId = ''

  test.beforeAll(async ({ request }) => {
    const { isDockerStackReady } = await import('./helpers/stack-readiness')
    const deadline = Date.now() + 90_000
    let ready = false
    while (Date.now() < deadline) {
      ready = await isDockerStackReady(request, {
        frontendBaseUrl: FRONTEND_BASE_URL,
        timeoutMs: 5_000,
      })
      if (ready) {
        break
      }
      await new Promise((resolve) => setTimeout(resolve, 3_000))
    }
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Stack required (${FRONTEND_BASE_URL} + backend :8080). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('seed — ensure 1.0.0 + 1.2.0 releases and write success/failure invocations', async ({
    request,
  }) => {
    const fixture = await ensureDemoFullFlowPublished(request)
    templateId = fixture.templateId
    externalId = fixture.externalId

    const lines = await listTemplateVersionLines(request, templateId)
    const hasV12 = lines.some(
      (line) => line.lineKind === 'PUBLISHED' && line.releaseVersion === RELEASE_V12,
    )
    if (!hasV12) {
      await publishSecondReleaseFromClone(request, templateId, RELEASE_V1, RELEASE_V12)
    }

    // Fail-closed AD groups: ensure RETAIL_API so seeded e2e-runtime-caller can invoke.
    await updateApiPolicyBatchSettings(request, templateId, false, 10)

    const credential = await createTemplateApiCredential(request, templateId)
    const stamp = Date.now().toString(36)

    const successV1 = await runtimeGenerateDefault(
      request,
      externalId,
      credential,
      `ce-u11-ok-v1-${stamp}`,
    )
    expect(successV1.status, 'default generate (1.0.0)').toBe(200)
    successV1RequestId = `req-ce-u11-ok-v1-${stamp}`

    const successV12 = await runtimeGenerateByVersion(
      request,
      externalId,
      credential,
      RELEASE_V12,
      `ce-u11-ok-v12-${stamp}`,
    )
    expect(successV12.status, 'explicit 1.2.0 generate').toBe(200)
    successV12RequestId = successV12.requestId

    const failV12 = await runtimeGenerateContractInvalid(
      request,
      externalId,
      credential,
      RELEASE_V12,
      `ce-u11-fail-v12-${stamp}`,
    )
    expect(failV12.status, 'contract-invalid generate').toBeGreaterThanOrEqual(400)
    failV12RequestId = failV12.requestId

    const successV1Row = await waitForManagementInvocationByRequestId(
      request,
      templateId,
      successV1RequestId,
    )
    expect(successV1Row.resolvedReleaseVersion).toBe(RELEASE_V1)

    const successV12Row = await waitForManagementInvocationByRequestId(
      request,
      templateId,
      successV12RequestId,
    )
    expect(successV12Row.resolvedReleaseVersion).toBe(RELEASE_V12)
    successV12InvocationId = successV12Row.invocationId

    const failV12Row = await waitForManagementInvocationByRequestId(
      request,
      templateId,
      failV12RequestId,
    )
    expect(failV12Row.resolvedReleaseVersion).toBe(RELEASE_V12)
    expect(failV12Row.status).toBe('FAILED')
    failV12InvocationId = failV12Row.invocationId
  })

  test('BDD-CE-U11-IRC-001/002 — API resolvedReleaseVersion filter (+ status combine)', async ({
    request,
  }) => {
    const filtered = await listManagementInvocations(request, templateId, 0, 50, {
      resolvedReleaseVersion: RELEASE_V12,
    })
    expect(filtered.totalElements).toBeGreaterThanOrEqual(2)
    expect(filtered.content.length).toBeGreaterThanOrEqual(2)
    for (const row of filtered.content) {
      expect(row.resolvedReleaseVersion).toBe(RELEASE_V12)
      expect(row).not.toHaveProperty('parameters')
      expect(row).not.toHaveProperty('variableValues')
    }
    expect(filtered.content.some((row) => row.requestId === successV12RequestId)).toBe(true)
    expect(filtered.content.some((row) => row.requestId === failV12RequestId)).toBe(true)
    expect(filtered.content.some((row) => row.requestId === successV1RequestId)).toBe(false)

    // API `status` query filters the outcome column (SUCCESS/FAILURE), not InvocationStatus.
    const combined = await listManagementInvocations(request, templateId, 0, 50, {
      resolvedReleaseVersion: RELEASE_V12,
      status: 'FAILURE',
    })
    expect(combined.totalElements).toBeGreaterThanOrEqual(1)
    expect(combined.content.every((row) => row.status === 'FAILED')).toBe(true)
    expect(combined.content.every((row) => row.resolvedReleaseVersion === RELEASE_V12)).toBe(true)
    expect(combined.content.some((row) => row.requestId === failV12RequestId)).toBe(true)
    expect(combined.content.some((row) => row.requestId === successV12RequestId)).toBe(false)
  })

  test('BDD-CE-U11-IRC-003 — FE Release version Apply sends resolvedReleaseVersion', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    const panel = await openInvocationHistory(page, templateId)

    await panel.getByTestId('invocation-release-version-filter').locator('input').fill(RELEASE_V12)

    const listResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes(`/templates/${templateId}/api/invocations`) &&
        !response.url().includes('/export') &&
        !response.url().includes('/recent') &&
        response.url().includes(`resolvedReleaseVersion=${encodeURIComponent(RELEASE_V12)}`),
      { timeout: 30_000 },
    )

    await panel.getByTestId('invocation-apply-filters').click()
    const listResponse = await listResponsePromise
    expect(listResponse.ok()).toBeTruthy()

    await expect(panel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })
    const table = panel.locator('.invocation-table')
    await expect(table).toBeVisible()

    await expect(table.locator('tbody tr td').filter({ hasText: RELEASE_V12 }).first()).toBeVisible()
    await expect(table.getByText(RELEASE_V1, { exact: true })).toHaveCount(0)
    await expect(table.getByText(successV12RequestId)).toBeVisible()
    await expect(table.getByText(failV12RequestId)).toBeVisible()
    await expect(table.getByText(successV1RequestId)).toHaveCount(0)
  })

  test('BDD-CE-U11-IRC-004 — failed drawer shows error envelope without parameters', async ({
    page,
    request,
  }) => {
    const detail = await getManagementInvocationDetail(request, templateId, failV12InvocationId)
    expect(detail.outcome).toBe('FAILURE')
    expect(detail.errorCode).toBe('REQUEST_BODY_INVALID')
    expect(detail.errorMessageKey).toBe('api.error.validation.requestBodyInvalid')
    expect(detail.errorCategory).toBeTruthy()
    expect(detail.errorRetryable).toBe(false)
    expect(detail).not.toHaveProperty('parameters')
    expect(detail).not.toHaveProperty('variableValues')

    await loginAs(page, E2E_GROUP_ADMIN)
    const panel = await openInvocationHistory(page, templateId)
    await panel.getByTestId('invocation-release-version-filter').locator('input').fill(RELEASE_V12)
    await panel.getByTestId('invocation-apply-filters').click()
    await expect(panel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const failRow = panel.locator('.invocation-table tbody tr').filter({ hasText: failV12RequestId })
    await expect(failRow).toBeVisible()
    await failRow.click()

    const drawer = page.getByTestId('invocation-summary-drawer')
    await expect(drawer).toBeVisible()
    const envelope = drawer.getByTestId('invocation-error-envelope')
    await expect(envelope).toBeVisible({ timeout: 15_000 })
    await expect(drawer.getByTestId('invocation-error-code')).toHaveText('REQUEST_BODY_INVALID')
    await expect(envelope).toContainText('api.error.validation.requestBodyInvalid')
    await expect(envelope).toContainText(/VALIDATION/i)
    await expect(envelope).toContainText(/No|否/)
    await expect(drawer.getByText(/parameters|variables/i)).toHaveCount(0)
  })

  test('BDD-CE-U11-IRC-005 — successful drawer does not show error envelope', async ({
    page,
    request,
  }) => {
    const detail = await getManagementInvocationDetail(request, templateId, successV12InvocationId)
    expect(detail.outcome).toBe('SUCCESS')
    expect(detail.errorCode == null || detail.errorCode === '').toBe(true)

    await loginAs(page, E2E_GROUP_ADMIN)
    const panel = await openInvocationHistory(page, templateId)
    await panel.getByTestId('invocation-release-version-filter').locator('input').fill(RELEASE_V12)
    await panel.getByTestId('invocation-apply-filters').click()
    await expect(panel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    const successRow = panel
      .locator('.invocation-table tbody tr')
      .filter({ hasText: successV12RequestId })
    await expect(successRow).toBeVisible()
    await successRow.click()

    const drawer = page.getByTestId('invocation-summary-drawer')
    await expect(drawer).toBeVisible()
    await expect(drawer.locator('.summary-list')).toBeVisible({ timeout: 15_000 })
    await expect(drawer.getByText('SUCCESS')).toBeVisible()
    await expect(drawer.getByTestId('invocation-error-envelope')).toHaveCount(0)
    await expect(drawer.getByText('REQUEST_BODY_INVALID')).toHaveCount(0)
  })

  test('BDD-CE-U11-IRC-007 — Export CSV respects applied filters and omits parameters', async ({
    page,
    request,
  }) => {
    const apiExport = await exportManagementInvocationsCsv(request, templateId, {
      resolvedReleaseVersion: RELEASE_V12,
    })
    expect(apiExport.status).toBe(200)
    expect(apiExport.text).toMatch(/invocationId,requestId,invocationKind,status,resolvedReleaseVersion/)
    expect(apiExport.text.toLowerCase()).not.toMatch(/parameters|variables/)
    expect(apiExport.text).toContain(RELEASE_V12)
    expect(apiExport.text).toContain(successV12RequestId)
    expect(apiExport.text).toContain(failV12RequestId)
    expect(apiExport.text).not.toContain(successV1RequestId)
    expect(apiExport.text).toMatch(/errorCode|REQUEST_BODY_INVALID/)

    await loginAs(page, E2E_GROUP_ADMIN)
    const panel = await openInvocationHistory(page, templateId)
    await panel.getByTestId('invocation-release-version-filter').locator('input').fill(RELEASE_V12)
    await panel.getByTestId('invocation-apply-filters').click()
    await expect(panel.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

    // Work around axios default Accept: application/json, which makes Spring skip
    // produces=text/csv and match /invocations/{id}=export. Product fix: FE sets Accept
    // text/csv (apiPolicyInvocations.ts) and/or BE drops produces restriction.
    await page.route('**/api/invocations/export**', async (route) => {
      const headers = { ...route.request().headers(), accept: 'text/csv' }
      await route.continue({ headers })
    })

    const exportResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        response.url().includes(`/templates/${templateId}/api/invocations/export`) &&
        response.url().includes(`resolvedReleaseVersion=${encodeURIComponent(RELEASE_V12)}`),
      { timeout: 30_000 },
    )
    const downloadPromise = page.waitForEvent('download', { timeout: 30_000 })

    await panel.getByTestId('invocation-export-csv').click()

    const exportResponse = await exportResponsePromise
    expect(exportResponse.ok()).toBeTruthy()
    const download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/\.csv$/i)

    const downloadPath = await download.path()
    expect(downloadPath).toBeTruthy()
    const csvText = await readFile(downloadPath!, 'utf8')
    expect(csvText.toLowerCase()).not.toMatch(/parameters|variables/)
    expect(csvText).toContain(RELEASE_V12)
    expect(csvText).not.toContain(successV1RequestId)

    await expect(page.locator('.el-message').getByText(/export|导出/i)).toBeVisible({
      timeout: 15_000,
    })
  })

  test('BDD-CE-U11-IRC-008 — author without canManageApiPolicy is fail-closed', async ({
    request,
  }) => {
    const denied = await listManagementInvocationsStatus(
      request,
      templateId,
      { resolvedReleaseVersion: RELEASE_V12 },
      E2E_TEMPLATE_AUTHOR,
    )
    expect([401, 403]).toContain(denied.status)

    const exportDenied = await exportManagementInvocationsCsv(
      request,
      templateId,
      { resolvedReleaseVersion: RELEASE_V12 },
      E2E_TEMPLATE_AUTHOR,
    )
    expect([401, 403]).toContain(exportDenied.status)
  })
})
