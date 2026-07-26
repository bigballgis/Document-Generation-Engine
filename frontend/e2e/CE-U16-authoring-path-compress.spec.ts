/**
 * CE-U16 — Authoring path compression (design default Bindings + create micro-wizard)
 * BDD: docs/behavior/ce-u16-authoring-path-compress.md (BDD-CE-U16-APC)
 *
 * Create→wizard landing is exercised via the post-create URL contract
 * (`authoringGuide=1&authoringGuideStep=master`) that `handleCreated` /
 * `buildPostCreateAuthoringPath` emit. Full Create-dialog UI pick of masters is
 * blocked for TEMPLATE_AUTHOR today (create dialog does not fetchAllMasters;
 * dashboard skips masters when actor lacks master-management). Tracked as FE
 * follow-up — Import dialog already fetches on open.
 */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import {
  DEMO_MASTER_NAME,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

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
}

function authoringGuide(page: Page) {
  return page.getByTestId('authoring-path-guide')
}

function guideStep(page: Page, stepId: string) {
  return page.getByTestId(`authoring-path-guide-step-${stepId}`)
}

async function expectBindingsPanelActive(page: Page): Promise<void> {
  await expect(page).toHaveURL(/designTab=bindings|workspaceTab=design/)
  const bindingsTab = page.getByRole('tab', { name: /^bindings$/i })
  await expect(bindingsTab).toBeVisible({ timeout: 30_000 })
  await expect(bindingsTab).toHaveAttribute('aria-selected', 'true')
  await expect(page.locator('.bindings-panel')).toBeVisible()
  await expect(page.getByRole('tab', { name: /^variables$/i })).toHaveAttribute(
    'aria-selected',
    'false',
  )
}

async function expectVariablesPanelActive(page: Page): Promise<void> {
  await expect(page).toHaveURL(/designTab=variables/)
  const variablesTab = page.getByRole('tab', { name: /^variables$/i })
  await expect(variablesTab).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByTestId('variable-tree-panel')).toBeVisible()
  await expect(page.getByRole('tab', { name: /^bindings$/i })).toHaveAttribute(
    'aria-selected',
    'false',
  )
}

/** Post-create Authoring path entry (same query as buildPostCreateAuthoringPath). */
async function openPostCreateAuthoringPath(
  page: Page,
  templateId: string,
  devVersionId: string,
): Promise<void> {
  await openDevWorkspace(
    page,
    templateId,
    devVersionId,
    'workspaceTab=design&authoringGuide=1&authoringGuideStep=master',
  )
}

/**
 * CE-U16 functional journeys (APC-001 / 002 / 003 / 004 / 005 / 007 + U15 coexistence).
 */
test.describe('CE-U16 authoring path compress (BDD-CE-U16-APC)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('BDD-CE-U16-APC-001 — Design without designTab defaults to Bindings', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDevWorkspace(page, fixture.templateId, devVersionId, 'workspaceTab=design')

    await expect(page.getByTestId('lifecycle-stepper')).toBeVisible({ timeout: 30_000 })
    await expect(authoringGuide(page)).toHaveCount(0)
    await expectBindingsPanelActive(page)
  })

  test('BDD-CE-U16-APC-002 — Explicit designTab wins over bindings default', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDevWorkspace(
      page,
      fixture.templateId,
      devVersionId,
      'workspaceTab=design&designTab=variables',
    )
    await expectVariablesPanelActive(page)

    await openDevWorkspace(
      page,
      fixture.templateId,
      devVersionId,
      'workspaceTab=design&designTab=contentModules',
    )
    await expect(page).toHaveURL(/designTab=contentModules/)
    const clauseTab = page.getByRole('tab', { name: /^clause references$/i })
    await expect(clauseTab).toHaveAttribute('aria-selected', 'true')
    await expect(page.getByRole('tab', { name: /^bindings$/i })).toHaveAttribute(
      'aria-selected',
      'false',
    )
    await expect(page.getByRole('tab', { name: /^variables$/i })).toHaveAttribute(
      'aria-selected',
      'false',
    )
  })

  test('BDD-CE-U16-APC-003/004/006 — Authoring path Master→Bindings→Variables→Preview; no lifecycle CTAs', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openPostCreateAuthoringPath(page, fixture.templateId, devVersionId)

    await expect(page).toHaveURL(/authoringGuide=1/)
    await expect(page).toHaveURL(/authoringGuideStep=master/)

    // U15 coexistence — lifecycle stepper remains and is distinct from authoring path.
    await expect(page.getByTestId('lifecycle-stepper')).toBeVisible({ timeout: 30_000 })
    await expect(authoringGuide(page)).toBeVisible()
    await expect(guideStep(page, 'master')).toHaveAttribute('aria-current', 'step')
    await expect(page.getByTestId('authoring-path-master-panel')).toBeVisible()
    await expect(page.getByTestId('authoring-path-master-identity')).toContainText(DEMO_MASTER_NAME)
    await expect(page.getByTestId('authoring-path-master-anchors')).toBeVisible()

    // APC-006 — guide has no lifecycle workflow CTAs.
    await expect(authoringGuide(page).getByRole('button', { name: /^submit for testing$/i })).toHaveCount(0)
    await expect(authoringGuide(page).getByRole('button', { name: /^approve$/i })).toHaveCount(0)
    await expect(authoringGuide(page).getByRole('button', { name: /^confirm go-live$/i })).toHaveCount(0)
    await expect(authoringGuide(page).getByRole('button', { name: /^publish$/i })).toHaveCount(0)

    // APC-004 — Bindings
    await guideStep(page, 'bindings').click()
    await expect(page).toHaveURL(/authoringGuideStep=bindings/)
    await expect(page).toHaveURL(/designTab=bindings/)
    await expect(guideStep(page, 'bindings')).toHaveAttribute('aria-current', 'step')
    await expectBindingsPanelActive(page)
    await expect(page.getByTestId('lifecycle-stepper')).toBeVisible()

    // APC-004 — Variables
    await guideStep(page, 'variables').click()
    await expect(page).toHaveURL(/authoringGuideStep=variables/)
    await expectVariablesPanelActive(page)

    // APC-004 — Preview → testing / previewRuns
    await guideStep(page, 'preview').click()
    await expect(page).toHaveURL(/authoringGuideStep=preview/)
    await expect(page).toHaveURL(/workspaceTab=testing/)
    await expect(page).toHaveURL(/testingTab=previewRuns/)
    await expect(guideStep(page, 'preview')).toHaveAttribute('aria-current', 'step')
    await expect(page.getByRole('tab', { name: /^preview runs$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
  })

  test('BDD-CE-U16-APC-005 — Skip guide hides Authoring path without blocking workspace', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openPostCreateAuthoringPath(page, fixture.templateId, devVersionId)

    await expect(authoringGuide(page)).toBeVisible()
    await page.getByTestId('authoring-path-guide-dismiss').click()
    await expect(authoringGuide(page)).toHaveCount(0)
    await expect(page).not.toHaveURL(/authoringGuide=1/)

    // Workspace Tab Shell returns; top-level tab label is "Template design".
    await expect(page.getByRole('tab', { name: /^template design$/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /^template testing$/i })).toBeVisible()
    await expectBindingsPanelActive(page)
    await expect(page.getByTestId('lifecycle-stepper')).toBeVisible()
  })

  test('BDD-CE-U16-APC-007 — Daily open without authoringGuide does not force wizard', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDevWorkspace(page, fixture.templateId, devVersionId, 'workspaceTab=design')

    await expect(authoringGuide(page)).toHaveCount(0)
    await expect(page.getByTestId('authoring-path-master-panel')).toHaveCount(0)
    await expectBindingsPanelActive(page)
    await expect(page.getByTestId('lifecycle-stepper')).toBeVisible()
  })
})
