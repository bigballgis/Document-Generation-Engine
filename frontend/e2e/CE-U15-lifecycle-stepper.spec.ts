import { expect, test, type APIRequestContext, type Page, type Route } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import {
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import { prepareRetailTemplateInTesting } from './helpers/collaboration-api'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  prepareTemplatePendingApprovalDecision,
  prepareTemplatePendingRelease,
} from './helpers/submit-approval-gate-api'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import { reLoginAs } from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

type PublishGateItem = {
  checkCode: string
  ready: boolean
  blocker: boolean
  messageKey?: string
  summary?: string
}

type PublishGateEnvelope = {
  metadata?: unknown
  result: {
    ready: boolean
    items: PublishGateItem[]
  }
  error?: unknown
}

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function resolveInFlightDevVersionId(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  expect(inFlight?.devVersionId, `IN_FLIGHT devVersion for ${templateId}`).toBeTruthy()
  return inFlight!.devVersionId
}

async function openDevWorkspace(
  page: Page,
  templateId: string,
  devVersionId: string,
  query = '',
): Promise<void> {
  const suffix = query.startsWith('?') || query.length === 0 ? query : `?${query}`
  await page.goto(`/templates/${templateId}/dev/${devVersionId}${suffix}`)
  await dismissOnboardingTourIfPresent(page)
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
  await expect(page.getByTestId('lifecycle-stepper')).toBeVisible({ timeout: 30_000 })
}

function stepper(page: Page) {
  return page.getByTestId('lifecycle-stepper')
}

function step(page: Page, stepId: string) {
  return page.getByTestId(`lifecycle-stepper-step-${stepId}`)
}

function workspaceActions(page: Page) {
  return page.locator('.workspace-tab-shell__actions')
}

/**
 * Force selected gate checks to pending so Go fix controls are visible.
 * Gate evaluation algorithms are out of CE-U15 scope (U15 non-goal); this isolates navigation.
 */
async function forcePublishGatePending(
  page: Page,
  pendingCodes: string[],
): Promise<void> {
  await page.route('**/api/management/v1/templates/*/publish-gate**', async (route: Route) => {
    const response = await route.fetch()
    const body = (await response.json()) as PublishGateEnvelope
    if (!body?.result?.items) {
      await route.fulfill({ response })
      return
    }
    const pending = new Set(pendingCodes)
    const items = body.result.items.map((item) =>
      pending.has(item.checkCode)
        ? {
            ...item,
            ready: false,
            blocker: item.blocker ?? true,
            messageKey: item.messageKey ?? `api.publishGate.${item.checkCode}.pending`,
            summary: item.summary ?? `${item.checkCode} pending (E2E CE-U15)`,
          }
        : item,
    )
    for (const code of pendingCodes) {
      if (!items.some((item) => item.checkCode === code)) {
        items.push({
          checkCode: code,
          ready: false,
          blocker: true,
          messageKey: `api.publishGate.${code}.pending`,
          summary: `${code} pending (E2E CE-U15)`,
        })
      }
    }
    await route.fulfill({
      status: response.status(),
      contentType: 'application/json',
      body: JSON.stringify({
        ...body,
        result: {
          ...body.result,
          ready: false,
          items,
        },
      }),
    })
  })
}

/**
 * CE-U15 — Lifecycle Stepper + publish-gate Go fix deep links (BDD-CE-U15-LSS).
 * BDD: docs/behavior/ce-u15-lifecycle-stepper.md
 */
test.describe('CE-U15 lifecycle stepper + Go fix (BDD-CE-U15-LSS)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('BDD-CE-U15-LSS-001 — DRAFT dev workspace shows Lifecycle Stepper on Draft', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDevWorkspace(page, fixture.templateId, devVersionId)

    await expect(stepper(page)).toBeVisible()
    await expect(stepper(page)).toHaveAttribute('data-terminal', 'false')
    await expect(step(page, 'draft')).toHaveAttribute('aria-current', 'step')
    await expect(step(page, 'draft')).toContainText(/draft/i)
    await expect(step(page, 'testing')).not.toHaveAttribute('aria-current', 'step')
    await expect(step(page, 'testing')).toHaveClass(/is-upcoming/)
    await expect(step(page, 'readyForApproval')).toHaveClass(/is-upcoming/)
    await expect(step(page, 'pendingApproval')).toHaveClass(/is-upcoming/)
    await expect(step(page, 'pendingRelease')).toHaveClass(/is-upcoming/)
    await expect(step(page, 'published')).toHaveClass(/is-upcoming/)

    // Stepper is orientation-only — no workflow CTAs on the stepper itself (U15-D3 / LSS-001).
    await expect(stepper(page).getByRole('button', { name: /^submit for testing$/i })).toHaveCount(0)
    await expect(stepper(page).getByRole('button', { name: /^approve$/i })).toHaveCount(0)
    await expect(stepper(page).getByRole('button', { name: /^confirm go-live$/i })).toHaveCount(0)
    await expect(stepper(page).getByRole('button', { name: /^publish$/i })).toHaveCount(0)
  })

  test('BDD-CE-U15-LSS-002 — Stepper advances for TESTING / PENDING_DECISION / PENDING_RELEASE', async ({
    page,
    request,
  }) => {
    const testing = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-CE-U15-LSS002-T-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U15 LSS-002 Testing ${Date.now().toString(36).toUpperCase()}`,
    })
    const testingDevId = await resolveInFlightDevVersionId(request, testing.templateId)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await openDevWorkspace(page, testing.templateId, testingDevId, 'workspaceTab=testing')

    await expect(step(page, 'testing')).toHaveAttribute('aria-current', 'step')
    await expect(step(page, 'draft')).toHaveClass(/is-completed/)
    await expect(step(page, 'readyForApproval')).toHaveClass(/is-upcoming/)

    const pendingDecision = await prepareTemplatePendingApprovalDecision(request, {
      externalId: `E2E-CE-U15-LSS002-A-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U15 LSS-002 Appr ${Date.now().toString(36).toUpperCase()}`,
    })
    const decisionDevId = await resolveInFlightDevVersionId(request, pendingDecision.templateId)

    // Approver (not tester) can open PENDING_DECISION approval workspace.
    await reLoginAs(page, loginAs, E2E_TEMPLATE_APPROVER)
    await openDevWorkspace(
      page,
      pendingDecision.templateId,
      decisionDevId,
      'workspaceTab=approval&approvalTab=submitApproval',
    )
    await expect(step(page, 'pendingApproval')).toHaveAttribute('aria-current', 'step')
    await expect(step(page, 'testing')).toHaveClass(/is-completed/)

    const pendingRelease = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-CE-U15-LSS002-P-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U15 LSS-002 Pub ${Date.now().toString(36).toUpperCase()}`,
    })
    const releaseDevId = await resolveInFlightDevVersionId(request, pendingRelease.templateId)

    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await openDevWorkspace(
      page,
      pendingRelease.templateId,
      releaseDevId,
      'workspaceTab=approval&approvalTab=publishReadiness',
    )
    await expect(step(page, 'pendingRelease')).toHaveAttribute('aria-current', 'step')
    await expect(step(page, 'pendingApproval')).toHaveClass(/is-completed/)
  })

  test('BDD-CE-U15-LSS-004 — Go fix for pending ANCHOR_INTEGRITY lands on design/bindings', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-CE-U15-LSS004-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U15 LSS-004 GoFix ${Date.now().toString(36).toUpperCase()}`,
    })
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await forcePublishGatePending(page, ['ANCHOR_INTEGRITY'])

    await loginAs(page, E2E_GROUP_ADMIN)
    await openDevWorkspace(
      page,
      fixture.templateId,
      devVersionId,
      'workspaceTab=approval&approvalTab=publishReadiness',
    )

    await expect(step(page, 'pendingRelease')).toHaveAttribute('aria-current', 'step')
    await expect(workspaceActions(page).getByRole('button', { name: /^confirm go-live$/i })).toBeVisible({
      timeout: 60_000,
    })

    const goFix = page.getByTestId('publish-gate-go-fix-ANCHOR_INTEGRITY')
    await expect(goFix).toBeVisible({ timeout: 30_000 })
    await expect(goFix).toHaveText(/^Go fix$/i)
    await goFix.click()

    await expect(page).toHaveURL(/workspaceTab=design/, { timeout: 15_000 })
    await expect(page).toHaveURL(/designTab=bindings/)
    await expect(page.getByRole('tab', { name: /^template design$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    // Bindings surface reachable without manual tab hunting.
    await expect(
      page.getByRole('tab', { name: /bindings/i }).or(page.locator('[data-design-tab="bindings"]')),
    ).toBeVisible({ timeout: 15_000 })
  })

  test('BDD-CE-U15-LSS-006 — Go fix mapping sample (COVERAGE_THRESHOLDS → testing/coverage)', async ({
    page,
    request,
  }) => {
    const fixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-CE-U15-LSS006-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U15 LSS-006 Map ${Date.now().toString(36).toUpperCase()}`,
    })
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await forcePublishGatePending(page, [
      'COVERAGE_THRESHOLDS',
      'FIDELITY_WARNINGS_VIEWED',
      'CONTENT_MODULE_EFFECTIVE_EXPIRED',
    ])

    await loginAs(page, E2E_GROUP_ADMIN)
    await openDevWorkspace(
      page,
      fixture.templateId,
      devVersionId,
      'workspaceTab=approval&approvalTab=publishReadiness',
    )

    const coverageGoFix = page.getByTestId('publish-gate-go-fix-COVERAGE_THRESHOLDS')
    await expect(coverageGoFix).toBeVisible({ timeout: 30_000 })
    await coverageGoFix.click()
    await expect(page).toHaveURL(/workspaceTab=testing/, { timeout: 15_000 })
    await expect(page).toHaveURL(/testingTab=coverage/)

    await page.goto(
      `/templates/${fixture.templateId}/dev/${devVersionId}?workspaceTab=approval&approvalTab=publishReadiness`,
    )
    await expect(page.getByTestId('lifecycle-stepper')).toBeVisible({ timeout: 30_000 })

    await page.getByTestId('publish-gate-go-fix-FIDELITY_WARNINGS_VIEWED').click()
    await expect(page).toHaveURL(/workspaceTab=testing/, { timeout: 15_000 })
    await expect(page).toHaveURL(/testingTab=previewRuns/)

    await page.goto(
      `/templates/${fixture.templateId}/dev/${devVersionId}?workspaceTab=approval&approvalTab=publishReadiness`,
    )
    await expect(page.getByTestId('lifecycle-stepper')).toBeVisible({ timeout: 30_000 })

    await page.getByTestId('publish-gate-go-fix-CONTENT_MODULE_EFFECTIVE_EXPIRED').click()
    await expect(page).toHaveURL(/workspaceTab=design/, { timeout: 15_000 })
    await expect(page).toHaveURL(/designTab=contentModules/)
  })

  test('BDD-CE-U15-LSS-010 — U14 dashboard Tasks must not show Lifecycle Stepper', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await dismissOnboardingTourIfPresent(page)

    const tasks = page.locator('#tasks-section')
    await expect(tasks).toBeVisible({ timeout: 30_000 })
    await expect(tasks.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })

    await expect(page.getByTestId('lifecycle-stepper')).toHaveCount(0)
    await expect(page.locator('[data-ce-u15-stepper]')).toHaveCount(0)
  })
})
