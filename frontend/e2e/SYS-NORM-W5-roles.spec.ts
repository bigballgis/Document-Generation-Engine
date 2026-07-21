/**
 * SYS-NORM Wave 5 / TM #149 — Six-role compression (ADR-0070)
 *
 * BDD SoT: docs/behavior/sys-norm-roles.md
 *   BDD-SYS-NORM-ROLE-001…018 (UI + remapped-seed journeys below;
 *   migration-only 001/006–009/017–018 covered by BE/docs gates)
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/SYS-NORM-W5-roles.spec.ts `
 *     --config playwright.docker.config.ts
 *
 * Coverage map:
 *   ROLE-010/016 — DOCUMENT_AUTHOR seeds (10000003/05) login + capabilities
 *   ROLE-011/016 — ex-approver 10000007 → GROUP_ADMIN session
 *   ROLE-004/016 — LEGAL_REVIEWER / AUDIT_ADMIN untouched
 *   ROLE-012/013 — Users role picker: six roles only; interim Document author label
 *   ROLE-014     — Journey maps for author / group admin / tester (no retired codes)
 *   ROLE-003/005 — SoD + fail-closed assignment API (decideTests 403; ROLE_NOT_ASSIGNABLE 422)
 *   ROLE-015     — Remapped GROUP_ADMIN can Approve; pure DOCUMENT_AUTHOR cannot
 */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'
import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  E2E_ADMIN,
  E2E_AUDIT_ADMIN,
  E2E_DOCUMENT_AUTHOR,
  E2E_LEGAL_REVIEWER,
  E2E_MASTER_DESIGNER,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_TESTER,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import { prepareTemplateInTesting } from './helpers/collaboration-api'
import { openDevEditorWorkspaceTab } from './helpers/lifecycle-ui'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  fetchTemplateDetail,
  prepareTemplatePendingApprovalDecision,
} from './helpers/submit-approval-gate-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import { reLoginAs } from './helpers/ui'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const EVIDENCE_DIR = path.join(__dirname, 'evidence', 'SYS-NORM-W5')

const SIX_ROLES = [
  'GLOBAL_ADMIN',
  'GROUP_ADMIN',
  'DOCUMENT_AUTHOR',
  'TEMPLATE_TESTER',
  'LEGAL_REVIEWER',
  'AUDIT_ADMIN',
] as const

const RETIRED_ROLES = ['TEMPLATE_APPROVER', 'MASTER_DESIGNER', 'TEMPLATE_AUTHOR'] as const

const SIX_ROLE_LABELS = [
  /global administrator/i,
  /group administrator/i,
  /document author/i,
  /template tester/i,
  /legal reviewer/i,
  /audit administrator/i,
] as const

const RETIRED_LABELS = [
  /template approver/i,
  /master designer/i,
  /^template author$/i,
] as const

interface LoginSession {
  roles: string[]
  capabilities: Record<string, boolean>
}

async function apiLoginSession(
  request: APIRequestContext,
  credentials: { username: string; password: string },
): Promise<LoginSession> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: credentials,
  })
  expect(response.ok(), `login ${credentials.username}`).toBeTruthy()
  const body = (await response.json()) as {
    result: { session: LoginSession }
  }
  return body.result.session
}

async function apiLoginToken(
  request: APIRequestContext,
  credentials: { username: string; password: string },
): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: credentials,
  })
  expect(response.ok(), `token login ${credentials.username}`).toBeTruthy()
  const body = (await response.json()) as { result: { accessToken: string } }
  return body.result.accessToken
}

async function captureEvidence(page: Page, filename: string) {
  mkdirSync(EVIDENCE_DIR, { recursive: true })
  await page.screenshot({ path: path.join(EVIDENCE_DIR, filename), fullPage: true })
}

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openUsersAdmin(page: Page) {
  await page.goto('/entitlement/users')
  await dismissOnboardingTourIfPresent(page)
  await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible({
    timeout: 20_000,
  })
  await expect(page.getByText(/unable to load users/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 45_000 })
}

async function openCreateUserRoleDropdown(page: Page) {
  // Header + empty-state may both expose Create user; prefer the panel header action.
  const headerCreate = page.locator('.panel-header').getByRole('button', { name: /^create user$/i })
  if (await headerCreate.isVisible().catch(() => false)) {
    await headerCreate.click()
  } else {
    await page.getByRole('button', { name: /^create user$/i }).first().click()
  }
  const dialog = page.getByRole('dialog', { name: /^create user$/i })
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  // Click the EP select wrapper — the inner combobox input is covered by the placeholder layer.
  const rolesSelect = dialog
    .locator('.el-form-item')
    .filter({ hasText: /\broles\b/i })
    .locator('.el-select')
    .first()
  await rolesSelect.click()
  const dropdown = page.locator('.el-select-dropdown:visible')
  await expect(dropdown.locator('.el-select-dropdown__item').first()).toBeVisible({
    timeout: 10_000,
  })
  return { dialog, dropdown }
}

async function openDashboardWorkflowJourney(page: Page) {
  await page.goto('/dashboard?tab=workflow')
  await dismissOnboardingTourIfPresent(page)
  const journey = page.locator('#journey-section')
  await expect(journey).toBeVisible({ timeout: 20_000 })
  return journey
}

function workspaceActions(page: Page) {
  return page.locator('.workspace-tab-shell__actions')
}

test.describe('SYS-NORM Wave 5 — Six-role compression functional journeys', () => {
  test.describe.configure({ timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      skipMessage:
        'Stack required (:4173 + :8080). Start with .\\scripts\\docker-deploy-queue.ps1',
    })
    mkdirSync(EVIDENCE_DIR, { recursive: true })
  })

  test('BDD-SYS-NORM-ROLE-010/016: DOCUMENT_AUTHOR seeds login with authoring union, no decide*', async ({
    page,
    request,
  }) => {
    for (const seed of [E2E_DOCUMENT_AUTHOR, E2E_MASTER_DESIGNER]) {
      const session = await apiLoginSession(request, seed)
      expect(session.roles).toEqual(['DOCUMENT_AUTHOR'])
      expect(session.roles).not.toEqual(expect.arrayContaining([...RETIRED_ROLES]))
      expect(session.capabilities.authorTemplates).toBe(true)
      expect(session.capabilities.manageMasters).toBe(true)
      expect(session.capabilities.authorContentModules).toBe(true)
      expect(session.capabilities.decideTests).toBe(false)
      expect(session.capabilities.decideApprovals).toBe(false)
    }

    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    await page.goto('/dashboard')
    await dismissOnboardingTourIfPresent(page)
    await expect(managementNav(page)).toBeVisible()
    await expect(page).not.toHaveURL(/\/forbidden/)
    await captureEvidence(page, 'TM149-ROLE-010-document-author-dashboard.png')
  })

  test('BDD-SYS-NORM-ROLE-011/016: ex-approver seed is GROUP_ADMIN with decideApprovals', async ({
    page,
    request,
  }) => {
    const session = await apiLoginSession(request, E2E_TEMPLATE_APPROVER)
    expect(session.roles).toEqual(['GROUP_ADMIN'])
    expect(session.roles).not.toContain('TEMPLATE_APPROVER')
    expect(session.capabilities.decideApprovals).toBe(true)
    expect(session.capabilities.decideTests).toBe(true)

    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await page.goto('/entitlement/users')
    await dismissOnboardingTourIfPresent(page)
    await expect(page.getByRole('heading', { name: /user management/i })).toBeVisible()
    await captureEvidence(page, 'TM149-ROLE-011-ex-approver-group-admin-users.png')
  })

  test('BDD-SYS-NORM-ROLE-004/016: LEGAL_REVIEWER and AUDIT_ADMIN seeds unchanged', async ({
    request,
  }) => {
    const legal = await apiLoginSession(request, E2E_LEGAL_REVIEWER)
    expect(legal.roles).toEqual(['LEGAL_REVIEWER'])
    expect(legal.capabilities.decideLegalApprovals).toBe(true)
    expect(legal.capabilities.decideApprovals).toBe(false)

    const audit = await apiLoginSession(request, E2E_AUDIT_ADMIN)
    expect(audit.roles).toEqual(['AUDIT_ADMIN'])
    expect(audit.capabilities.readAudit).toBe(true)
  })

  test('BDD-SYS-NORM-ROLE-012/013: role picker exposes six roles only with interim Document author label', async ({
    page,
  }) => {
    await loginAsGlobalAdmin(page)
    await openUsersAdmin(page)

    const { dialog, dropdown } = await openCreateUserRoleDropdown(page)
    const options = dropdown.locator('.el-select-dropdown__item')
    await expect(options).toHaveCount(SIX_ROLES.length)

    for (const label of SIX_ROLE_LABELS) {
      await expect(dropdown.getByRole('option', { name: label })).toBeVisible()
    }
    for (const retired of RETIRED_LABELS) {
      await expect(dropdown.getByRole('option', { name: retired })).toHaveCount(0)
    }

    const optionTexts = (await options.allTextContents()).map((t) => t.trim()).join('\n')
    for (const code of RETIRED_ROLES) {
      expect(optionTexts).not.toContain(code)
    }
    await expect(dropdown.getByRole('option', { name: /document author/i })).toBeVisible()

    await captureEvidence(page, 'TM149-ROLE-012-role-picker-six-roles.png')
    await page.keyboard.press('Escape')
    await dialog.getByRole('button', { name: /cancel/i }).click()
  })

  test('BDD-SYS-NORM-ROLE-014: journey maps resolve for six-role catalog', async ({ page }) => {
    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    const authorJourney = await openDashboardWorkflowJourney(page)
    await expect(
      authorJourney.getByRole('heading', { name: /document authoring workflow/i }),
    ).toBeVisible()
    await expect(authorJourney.locator('[data-journey-step]')).toHaveCount(6)
    await captureEvidence(page, 'TM149-ROLE-014-document-author-journey.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    const adminJourney = await openDashboardWorkflowJourney(page)
    // Remapped ex-approver is GROUP_ADMIN → team-lead journey (not retired approver-only path).
    await expect(
      adminJourney.getByRole('heading', { name: /team-lead go-live workflow/i }),
    ).toBeVisible()
    await expect(adminJourney.locator('[data-journey-step]')).toHaveCount(4)
    await expect(adminJourney.getByText(/TEMPLATE_APPROVER|MASTER_DESIGNER|TEMPLATE_AUTHOR/)).toHaveCount(
      0,
    )
    await captureEvidence(page, 'TM149-ROLE-014-group-admin-journey.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await dismissOnboardingTourIfPresent(page)
    // Dashboard shell keeps H1 "My tasks"; queue surfaces as the selected tab.
    await expect(page.getByRole('tab', { name: /waiting on my testing/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page.locator('#tasks-section')).toBeVisible()
    const testerJourney = await openDashboardWorkflowJourney(page)
    await expect(testerJourney.locator('[data-journey-step]')).toHaveCount(3)
    await captureEvidence(page, 'TM149-ROLE-014-tester-journey.png')
  })

  test('BDD-SYS-NORM-ROLE-003/005: author decideTests fail-closed; retired roles ROLE_NOT_ASSIGNABLE', async ({
    request,
  }) => {
    const fixture = await prepareTemplateInTesting(request, {
      externalId: `E2E-SYS-NORM-W5-SOD-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E SYS-NORM-W5 SoD ${Date.now().toString(36).toUpperCase()}`,
    })

    const authorToken = await apiLoginToken(request, E2E_DOCUMENT_AUTHOR)
    const decideResponse = await request.post(
      `${E2E_API_BASE_URL}/templates/${fixture.templateId}/lifecycle/test-decision`,
      {
        headers: { Authorization: `Bearer ${authorToken}` },
        data: {
          decision: 'PASSED',
          commentSummary: 'E2E SYS-NORM-W5 author must not decideTests',
          fidelityViewedConfirmed: true,
          coverageViewedConfirmed: true,
          previewViewedConfirmed: true,
        },
      },
    )
    expect(decideResponse.status()).toBe(403)
    writeFileSync(
      path.join(EVIDENCE_DIR, 'TM149-ROLE-003-author-decideTests-403.json'),
      JSON.stringify(
        { status: decideResponse.status(), body: await decideResponse.json() },
        null,
        2,
      ),
    )

    const adminToken = await apiLoginToken(request, E2E_ADMIN)
    const probes: Array<{ role: string; status: number; code?: string }> = []
    let probeSeq = Number(String(Date.now()).slice(-6))
    for (const retired of RETIRED_ROLES) {
      probeSeq += 1
      // CreateUserRequest requires 8-digit employee id; uniqueness avoids USERNAME_TAKEN noise.
      const username = String(18000000 + (probeSeq % 1_000_000)).padStart(8, '0')
      const response = await request.post(`${E2E_API_BASE_URL}/users`, {
        headers: { Authorization: `Bearer ${adminToken}` },
        data: {
          username,
          displayName: `E2E Retired ${retired}`,
          email: `e2e-${retired.toLowerCase()}-${username}@example.com`,
          initialPassword: 'ChangeMe123!',
          roles: [retired],
          authorizedGroupCodes: ['RETAIL'],
        },
      })
      expect(response.status(), retired).toBe(422)
      const body = (await response.json()) as { error?: { code?: string; retryable?: boolean } }
      expect(body.error?.code).toBe('ROLE_NOT_ASSIGNABLE')
      expect(body.error?.retryable).toBe(false)
      probes.push({ role: retired, status: response.status(), code: body.error?.code })
    }
    writeFileSync(
      path.join(EVIDENCE_DIR, 'TM149-ROLE-005-retired-role-not-assignable.json'),
      JSON.stringify({ probes }, null, 2),
    )
  })

  test('BDD-SYS-NORM-ROLE-015: remapped GROUP_ADMIN can Approve; DOCUMENT_AUTHOR cannot', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingApprovalDecision(request, {
      externalId: `E2E-SYS-NORM-W5-APPR-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E SYS-NORM-W5 Approval ${Date.now().toString(36).toUpperCase()}`,
    })
    const before = await fetchTemplateDetail(request, fixture.templateId)
    expect(before.lifecycleStatus).toBe('APPROVAL')
    expect(before.approvalSubState).toBe('PENDING_DECISION')

    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()

    await loginAs(page, E2E_DOCUMENT_AUTHOR)
    await page.goto(
      `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}?workspaceTab=approval&approvalTab=submitApproval`,
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toHaveCount(0)
    await captureEvidence(page, 'TM149-ROLE-015-author-no-approve.png')

    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await openDevEditorWorkspaceTab(page, fixture.templateId, request, 'approval')
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toBeVisible({
      timeout: 15_000,
    })
    await captureEvidence(page, 'TM149-ROLE-015-group-admin-approve-cta.png')

    // Complete approve to prove decideApprovals path for remapped GROUP_ADMIN.
    await workspaceActions(page).getByRole('button', { name: /^approve$/i }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText(/confirm approval/i)).toBeVisible()
    await dialog
      .getByRole('textbox', { name: /approval rationale/i })
      .fill('E2E SYS-NORM-W5 remapped GROUP_ADMIN compliance approve.')
    const fidelityConfirm = dialog.getByTestId('confirm-fidelity-viewed')
    if ((await fidelityConfirm.count()) > 0) {
      await fidelityConfirm.click()
    }
    await dialog.getByText(/I reviewed key evidence/i).click()
    const decisionResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/lifecycle/approval-decision'),
      { timeout: 30_000 },
    )
    await dialog.getByRole('button', { name: /^submit decision$/i }).click()
    const decisionResponse = await decisionResponsePromise
    expect(decisionResponse.ok()).toBeTruthy()

    const after = await fetchTemplateDetail(request, fixture.templateId)
    expect(after.lifecycleStatus).toBe('PENDING_RELEASE')
    writeFileSync(
      path.join(EVIDENCE_DIR, 'TM149-ROLE-015-approval-result.json'),
      JSON.stringify(
        {
          templateId: fixture.templateId,
          before: { lifecycleStatus: before.lifecycleStatus, approvalSubState: before.approvalSubState },
          after: { lifecycleStatus: after.lifecycleStatus, approvalSubState: after.approvalSubState },
        },
        null,
        2,
      ),
    )
  })
})
