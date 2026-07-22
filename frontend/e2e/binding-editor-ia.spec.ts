/**
 * Binding editor IA + auto referenceKey — Task Master #155 + #156
 *
 * BDD SoT: docs/behavior/binding-editor-ia.md (BDD-BEI-001…020)
 * Focus: layout chrome (rail / Save primary / Refresh secondary / compact toolbar)
 *        + Add clause reference auto-key dialog journeys.
 * Normalize/suffix helpers are covered by Vitest (BDD-BEI-020 unit half).
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/binding-editor-ia.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { normalizeModuleCodeToReferenceKey } from '@/utils/referenceKeyFromModuleCode'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  createApprovedContentModule,
  uniqueModuleCode,
  upsertTemplateContentModuleReference,
} from './helpers/content-modules-api'
import { openDevBindingEditor } from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { resolveInFlightDevVersionId } from './helpers/template-version-lines-api'
import { selectElementPlusOption } from './helpers/ui'
import { dismissOnboardingTourIfPresent } from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function openClauseReferencesPanel(
  page: Page,
  request: APIRequestContext,
  templateId: string,
): Promise<void> {
  const devVersionId = await resolveInFlightDevVersionId(request, templateId)
  await page.goto(
    `/templates/${templateId}/dev/${devVersionId}?workspaceTab=design&designTab=contentModules`,
  )
  await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
  await page.locator('.design-sub-tabs').getByRole('tab', { name: /^clause references$/i }).click()
  await expect(page.locator('.clause-authoring-panel')).toBeVisible({ timeout: 30_000 })
}

async function openAddClauseReferenceDialog(page: Page) {
  await page.locator('.clause-authoring-panel').getByRole('button', { name: /^add reference$/i }).click()
  const dialog = page.getByTestId('clause-reference-dialog')
  await expect(dialog).toBeVisible()
  return dialog
}

async function selectModuleInReferenceDialog(page: Page, moduleCode: string): Promise<void> {
  await page.getByTestId('clause-reference-module-select').click()
  await selectElementPlusOption(page, new RegExp(moduleCode, 'i'))
}

async function selectVersionInReferenceDialog(page: Page, semanticVersion: string): Promise<void> {
  await page.getByTestId('clause-reference-version-select').click()
  await selectElementPlusOption(page, semanticVersion)
}

test.describe('Binding editor IA (BDD-BEI layout + auto referenceKey)', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  // ── A — Binding editor layout (#155) ─────────────────────────────────────

  test('BDD-BEI-001/002/009 — sticky action rail; Save primary; Back secondary; EN chrome', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDevBindingEditor(page, request, fixture.templateId)

    const rail = page.getByTestId('binding-editor-action-rail')
    await expect(rail).toBeVisible()
    await expect(page.getByTestId('binding-editor-back')).toBeVisible()
    await expect(page.getByTestId('binding-editor-save')).toBeVisible()
    await expect(page.getByTestId('binding-editor-anchor-title')).toContainText('HEADER')

    const railPosition = await rail.evaluate((el) => getComputedStyle(el).position)
    expect(railPosition).toBe('sticky')

    const save = page.getByTestId('binding-editor-save')
    await expect(save).toHaveClass(/el-button--primary/)
    await expect(page.getByTestId('binding-editor-back')).not.toHaveClass(/el-button--primary/)

    await expect(page.getByTestId('binding-editor-back')).toHaveText(/^Back$/i)
    await expect(save).toHaveText(/^Save$/i)

    // Rail is outside the side-by-side content columns
    await expect(page.getByTestId('authoring-side-by-side-layout').getByTestId('binding-editor-action-rail')).toHaveCount(0)
  })

  test('BDD-BEI-003/004/005/006 — visibility collapsed; compact toolbar; sticky preview; Refresh secondary', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDevBindingEditor(page, request, fixture.templateId)

    await expect(page.getByTestId('binding-editor-content-type')).toBeVisible()

    const visibility = page.getByTestId('binding-editor-visibility-advanced')
    await expect(visibility).toBeVisible()
    await expect(visibility.locator('.el-collapse-item')).not.toHaveClass(/is-active/)
    await expect(page.getByTestId('enable-visibility-checkbox')).toBeHidden()

    await visibility.getByRole('button', { name: /visibility|advanced/i }).click()
    await expect(page.getByTestId('enable-visibility-checkbox')).toBeVisible()

    const toolbar = page.getByTestId('structured-editor-toolbar')
    await expect(toolbar).toBeVisible()
    await expect(toolbar).toHaveClass(/toolbar--compact/)

    const previewSlot = page.getByTestId('authoring-preview-pane-slot')
    await expect(page.getByTestId('authoring-preview-pane')).toBeVisible()
    const previewPosition = await previewSlot.evaluate((el) => getComputedStyle(el).position)
    expect(previewPosition).toBe('sticky')

    const refresh = page.getByTestId('authoring-preview-refresh')
    await expect(refresh).toBeVisible()
    await expect(refresh).not.toHaveClass(/el-button--primary/)
    await expect(page.getByTestId('binding-editor-save')).toHaveClass(/el-button--primary/)
  })

  test('BDD-BEI-008 — nested Design sub-tabs have no Back/Save/Refresh CTAs', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(
      `/templates/${fixture.templateId}/dev/${devVersionId}?workspaceTab=design&designTab=bindings`,
    )
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })

    const subTabs = page.locator('.design-sub-tabs')
    await expect(subTabs).toBeVisible()
    await expect(subTabs.getByRole('button', { name: /^back$/i })).toHaveCount(0)
    await expect(subTabs.getByRole('button', { name: /^save$/i })).toHaveCount(0)
    await expect(subTabs.getByRole('button', { name: /refresh/i })).toHaveCount(0)
    await expect(page.getByTestId('binding-editor-action-rail')).toHaveCount(0)
  })

  test('BDD-BEI-010 — narrow viewport keeps usable action rail', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.setViewportSize({ width: 375, height: 812 })
    await openDevBindingEditor(page, request, fixture.templateId, 'HEADER', {
      expectPreviewPane: false,
    })

    await expect(page.getByTestId('binding-editor-action-rail')).toBeVisible()
    await expect(page.getByTestId('binding-editor-save')).toBeVisible()
    await expect(page.getByTestId('binding-editor-back')).toBeVisible()
    await expect(page.getByTestId('authoring-side-by-side-layout')).toHaveClass(
      /authoring-side-by-side--stacked/,
    )
  })

  // ── B — Auto referenceKey (#156) ─────────────────────────────────────────

  test('BDD-BEI-012 — Add dialog auto-fills UPPER_SNAKE key; Advanced collapsed', async ({
    page,
    request,
  }) => {
    const moduleCode = uniqueModuleCode('E2E-LOAN-DISCLOSURE')
    const expectedKey = normalizeModuleCodeToReferenceKey(moduleCode)
    const module = await createApprovedContentModule(request, {
      moduleCode,
      name: `E2E BEI Auto Key ${moduleCode}`,
    })
    const fixture = await prepareDraftTemplateWithCleanBinding(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openClauseReferencesPanel(page, request, fixture.templateId)
    const dialog = await openAddClauseReferenceDialog(page)

    await expect(dialog.getByTestId('clause-reference-key-input')).toHaveValue('')
    await expect(dialog.getByTestId('clause-reference-advanced').locator('.el-collapse-item')).not.toHaveClass(
      /is-active/,
    )

    await selectModuleInReferenceDialog(page, module.moduleCode)

    await expect(dialog.getByTestId('clause-reference-key-input')).toHaveValue(expectedKey)
    await expect(dialog.getByTestId('clause-reference-key-auto-hint')).toBeVisible()
    await expect(dialog.getByTestId('clause-reference-advanced').locator('.el-collapse-item')).not.toHaveClass(
      /is-active/,
    )
    await expect(dialog.getByTestId('clause-reference-key-input')).toBeDisabled()
  })

  test('BDD-BEI-013/014 — conflict suffix _2 then _3', async ({ page, request }) => {
    const moduleCode = uniqueModuleCode('E2E-BEI-CONFLICT')
    const baseKey = normalizeModuleCodeToReferenceKey(moduleCode)
    const module = await createApprovedContentModule(request, {
      moduleCode,
      name: `E2E BEI Conflict ${moduleCode}`,
    })
    const fixture = await prepareDraftTemplateWithCleanBinding(request)

    await upsertTemplateContentModuleReference(
      request,
      fixture.templateId,
      baseKey,
      module.moduleId,
      module.semanticVersion,
    )
    await upsertTemplateContentModuleReference(
      request,
      fixture.templateId,
      `${baseKey}_2`,
      module.moduleId,
      module.semanticVersion,
    )

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openClauseReferencesPanel(page, request, fixture.templateId)
    const dialog = await openAddClauseReferenceDialog(page)
    await selectModuleInReferenceDialog(page, module.moduleCode)

    await expect(dialog.getByTestId('clause-reference-key-input')).toHaveValue(`${baseKey}_3`)
  })

  test('BDD-BEI-015/016 — Advanced override persists across module change; reset restores auto', async ({
    page,
    request,
  }) => {
    const moduleCodeA = uniqueModuleCode('E2E-BEI-OVERRIDE-A')
    const moduleCodeB = uniqueModuleCode('E2E-BEI-OVERRIDE-B')
    const moduleA = await createApprovedContentModule(request, {
      moduleCode: moduleCodeA,
      name: `E2E BEI Override A ${moduleCodeA}`,
    })
    const moduleB = await createApprovedContentModule(request, {
      moduleCode: moduleCodeB,
      name: `E2E BEI Override B ${moduleCodeB}`,
    })
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const customKey = `MY_CUSTOM_REF_${Date.now().toString(36).toUpperCase()}`

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openClauseReferencesPanel(page, request, fixture.templateId)
    const dialog = await openAddClauseReferenceDialog(page)

    await selectModuleInReferenceDialog(page, moduleA.moduleCode)
    await expect(dialog.getByTestId('clause-reference-key-input')).toHaveValue(
      normalizeModuleCodeToReferenceKey(moduleCodeA),
    )

    await dialog.getByTestId('clause-reference-advanced').getByRole('button', { name: /^advanced$/i }).click()
    const override = dialog.getByTestId('clause-reference-key-override')
    await expect(override).toBeVisible()
    await override.fill(customKey)

    await selectModuleInReferenceDialog(page, moduleB.moduleCode)
    await expect(dialog.getByTestId('clause-reference-key-input')).toHaveValue(customKey)
    await expect(override).toHaveValue(customKey)

    await selectVersionInReferenceDialog(page, moduleB.semanticVersion)
    await dialog.getByTestId('clause-reference-save').click()
    await expect(page.locator('.el-message').getByText(/clause reference saved/i)).toBeVisible()
    await expect(page.locator('.clause-authoring-panel')).toContainText(customKey)

    // Reset path on a fresh Add dialog
    const dialog2 = await openAddClauseReferenceDialog(page)
    await selectModuleInReferenceDialog(page, moduleA.moduleCode)
    await dialog2.getByTestId('clause-reference-advanced').getByRole('button', { name: /^advanced$/i }).click()
    await dialog2.getByTestId('clause-reference-key-override').fill('TEMP_OVERRIDE_KEY')
    await dialog2.getByTestId('clause-reference-key-reset').click()
    await expect(dialog2.getByTestId('clause-reference-key-input')).toHaveValue(
      normalizeModuleCodeToReferenceKey(moduleCodeA),
    )
  })

  test('BDD-BEI-017 — Edit existing locks referenceKey', async ({ page, request }) => {
    const moduleCode = uniqueModuleCode('E2E-BEI-EDIT-LOCK')
    const referenceKey = normalizeModuleCodeToReferenceKey(moduleCode)
    const module = await createApprovedContentModule(request, {
      moduleCode,
      name: `E2E BEI Edit Lock ${moduleCode}`,
    })
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await upsertTemplateContentModuleReference(
      request,
      fixture.templateId,
      referenceKey,
      module.moduleId,
      module.semanticVersion,
    )

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openClauseReferencesPanel(page, request, fixture.templateId)

    const row = page.locator('.clause-authoring-panel .el-table__row').filter({ hasText: referenceKey })
    await expect(row).toBeVisible({ timeout: 30_000 })
    await row.getByRole('button', { name: /^edit pin$/i }).click()

    const dialog = page.getByTestId('clause-reference-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByTestId('clause-reference-key-input')).toBeDisabled()
    await expect(dialog.getByTestId('clause-reference-key-input')).toHaveValue(referenceKey)
    await expect(dialog.getByTestId('clause-reference-key-locked-hint')).toBeVisible()
    await expect(dialog.getByTestId('clause-reference-advanced')).toHaveCount(0)
  })

  test('BDD-BEI-019 — Save blocked when version missing after module select', async ({
    page,
    request,
  }) => {
    const moduleCode = uniqueModuleCode('E2E-BEI-VALIDATION')
    const module = await createApprovedContentModule(request, {
      moduleCode,
      name: `E2E BEI Validation ${moduleCode}`,
    })
    const fixture = await prepareDraftTemplateWithCleanBinding(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openClauseReferencesPanel(page, request, fixture.templateId)
    const dialog = await openAddClauseReferenceDialog(page)
    await selectModuleInReferenceDialog(page, module.moduleCode)
    await expect(dialog.getByTestId('clause-reference-key-input')).not.toHaveValue('')

    await dialog.getByTestId('clause-reference-save').click()
    await expect(page.locator('.el-message').getByText(/required|select/i)).toBeVisible()
    await expect(dialog).toBeVisible()
  })
})
