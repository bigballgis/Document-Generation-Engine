/**
 * Binding editor IA / Task Master #155+#156 — Stage 7 UIUX evidence.
 *
 * Surfaces: binding editor sticky rail + side-by-side OA layout; Design nested
 * sub-tabs (no CTA); Add clause reference dialog auto-key. Dual-brand @1920
 * (BEI-C15) plus 1440 desktop-first and 375 narrow stack.
 *
 * BDD SoT: docs/behavior/binding-editor-ia.md (BDD-BEI-001…010, 012, 020)
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts `
 *     e2e/binding-editor-ia-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  createApprovedContentModule,
  uniqueModuleCode,
} from './helpers/content-modules-api'
import { openDevBindingEditor } from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { resolveInFlightDevVersionId } from './helpers/template-version-lines-api'
import { selectElementPlusOption } from './helpers/ui'
import {
  BINDING_EDITOR_IA_DESKTOP_VIEWPORT,
  BINDING_EDITOR_IA_NARROW_VIEWPORT,
  BINDING_EDITOR_IA_VIEWPORT,
  captureBindingEditorIaLocatorScreenshot,
  captureBindingEditorIaScreenshot,
  dismissOnboardingTourIfPresent,
  ensureBindingEditorIaEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return { scrollWidth: doc.scrollWidth, clientWidth: doc.clientWidth }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function assertPrimaryBrandColor(page: Page, brand: 'REDBC' | 'GREENBC'): Promise<void> {
  const expected = brand === 'REDBC' ? '#db0011' : '#00847f'
  const primary = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim().toLowerCase(),
  )
  expect(primary, `expected --brand-primary ${expected} for ${brand}`).toBe(expected)
}

test.describe('Binding editor IA UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  test.beforeAll(async ({ request }) => {
    ensureBindingEditorIaEvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
  })

  test('01–02 Binding editor OA layout dual-brand @1920', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await page.setViewportSize(BINDING_EDITOR_IA_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await openDevBindingEditor(page, request, fixture.templateId)

    const rail = page.getByTestId('binding-editor-action-rail')
    const layout = page.getByTestId('authoring-side-by-side-layout')
    const toolbar = page.getByTestId('structured-editor-toolbar')
    const preview = page.getByTestId('authoring-preview-pane')
    const previewSlot = page.getByTestId('authoring-preview-pane-slot')

    await expect(rail).toBeVisible()
    await expect(layout).toBeVisible()
    await expect(layout).not.toHaveClass(/authoring-side-by-side--stacked/)
    await expect(page.getByTestId('binding-editor-content-type')).toBeVisible()
    await expect(page.getByTestId('binding-editor-visibility-advanced')).toBeVisible()
    await expect(toolbar).toBeVisible()
    await expect(toolbar).toHaveClass(/toolbar--compact/)
    await expect(preview).toBeVisible()

    const railPosition = await rail.evaluate((el) => getComputedStyle(el).position)
    expect(railPosition).toBe('sticky')
    const previewPosition = await previewSlot.evaluate((el) => getComputedStyle(el).position)
    expect(previewPosition).toBe('sticky')

    await expect(page.getByTestId('binding-editor-save')).toHaveClass(/el-button--primary/)
    await expect(page.getByTestId('binding-editor-back')).not.toHaveClass(/el-button--primary/)
    await expect(page.getByTestId('authoring-preview-refresh')).not.toHaveClass(/el-button--primary/)
    await expect(page.getByTestId('binding-editor-back')).toHaveText(/^Back$/i)
    await expect(page.getByTestId('binding-editor-save')).toHaveText(/^Save$/i)

    // Nested card density: binding editor root should not stack multiple el-cards
    const nestedCards = page.getByTestId('binding-editor').locator('.el-card')
    const cardCount = await nestedCards.count()
    expect(cardCount, `unexpected nested el-card count=${cardCount}`).toBeLessThanOrEqual(2)

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)
      await assertNoViewportOverflow(page)

      const suffix = brand.toLowerCase()
      await captureBindingEditorIaScreenshot(
        page,
        `01-binding-editor-oa-layout-${suffix}-1920x1080.png`,
      )
      await captureBindingEditorIaLocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `01b-brand-header-${suffix}-crop.png`,
      )
      await captureBindingEditorIaLocatorScreenshot(
        rail,
        `01c-action-rail-${suffix}-crop.png`,
      )
      await captureBindingEditorIaLocatorScreenshot(
        layout,
        `01d-side-by-side-${suffix}-crop.png`,
      )
      await captureBindingEditorIaLocatorScreenshot(
        toolbar,
        `01e-compact-toolbar-${suffix}-crop.png`,
      )
      await captureBindingEditorIaLocatorScreenshot(
        preview,
        `01f-preview-pane-${suffix}-crop.png`,
      )
    }
  })

  test('03 Binding editor @1440 desktop-first (OA standard)', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await page.setViewportSize(BINDING_EDITOR_IA_DESKTOP_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await openDevBindingEditor(page, request, fixture.templateId)

    await expect(page.getByTestId('binding-editor-action-rail')).toBeVisible()
    await expect(page.getByTestId('authoring-side-by-side-layout')).not.toHaveClass(
      /authoring-side-by-side--stacked/,
    )
    await assertNoViewportOverflow(page)

    await switchBrand(page, 'REDBC')
    await captureBindingEditorIaScreenshot(page, '02-binding-editor-oa-layout-redbc-1440x900.png')
    await switchBrand(page, 'GREENBC')
    await captureBindingEditorIaScreenshot(page, '02-binding-editor-oa-layout-greenbc-1440x900.png')
  })

  test('04 Design nested sub-tabs — no workflow CTAs (WorkspaceTabShell)', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await page.setViewportSize(BINDING_EDITOR_IA_VIEWPORT)
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

    await switchBrand(page, 'REDBC')
    await assertNoViewportOverflow(page)
    await captureBindingEditorIaScreenshot(page, '03-design-nested-tabs-no-cta-redbc-1920x1080.png')
    await captureBindingEditorIaLocatorScreenshot(
      subTabs,
      '03b-design-sub-tabs-redbc-crop.png',
    )
    await switchBrand(page, 'GREENBC')
    await captureBindingEditorIaScreenshot(page, '03-design-nested-tabs-no-cta-greenbc-1920x1080.png')
  })

  test('05 Add clause reference dialog — auto-key chrome dual-brand', async ({ page, request }) => {
    const moduleCode = uniqueModuleCode('E2E-BEI-UIUX-LOAN')
    const module = await createApprovedContentModule(request, {
      moduleCode,
      name: `E2E BEI UIUX ${moduleCode}`,
    })
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await page.setViewportSize(BINDING_EDITOR_IA_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(
      `/templates/${fixture.templateId}/dev/${devVersionId}?workspaceTab=design&designTab=contentModules`,
    )
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await page.locator('.design-sub-tabs').getByRole('tab', { name: /^clause references$/i }).click()
    await expect(page.locator('.clause-authoring-panel')).toBeVisible({ timeout: 30_000 })

    for (const brand of ['REDBC', 'GREENBC'] as const) {
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)

      await page
        .locator('.clause-authoring-panel')
        .getByRole('button', { name: /^add reference$/i })
        .click()
      const dialog = page.getByTestId('clause-reference-dialog')
      await expect(dialog).toBeVisible()

      await page.getByTestId('clause-reference-module-select').click()
      await selectElementPlusOption(page, new RegExp(module.moduleCode, 'i'))
      await expect(dialog.getByTestId('clause-reference-key-input')).not.toHaveValue('')
      await expect(dialog.getByTestId('clause-reference-key-auto-hint')).toBeVisible()
      await expect(
        dialog.getByTestId('clause-reference-advanced').locator('.el-collapse-item'),
      ).not.toHaveClass(/is-active/)

      const suffix = brand.toLowerCase()
      await captureBindingEditorIaScreenshot(
        page,
        `04-add-clause-reference-dialog-${suffix}-1920x1080.png`,
      )
      await captureBindingEditorIaLocatorScreenshot(
        dialog,
        `04b-add-clause-reference-dialog-${suffix}-crop.png`,
      )

      await dialog.getByTestId('clause-reference-cancel').click()
      await expect(dialog).not.toBeVisible({ timeout: 10_000 })
    }
  })

  test('06 Narrow viewport — stacked preview keeps usable rail', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await page.setViewportSize(BINDING_EDITOR_IA_NARROW_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)

    const collapseSidebar = page.getByRole('button', { name: /collapse sidebar/i })
    if (await collapseSidebar.isVisible().catch(() => false)) {
      await collapseSidebar.click()
    }

    await openDevBindingEditor(page, request, fixture.templateId, 'HEADER', {
      expectPreviewPane: false,
    })

    await expect(page.getByTestId('binding-editor-action-rail')).toBeVisible()
    await expect(page.getByTestId('binding-editor-save')).toBeVisible()
    await expect(page.getByTestId('binding-editor-back')).toBeVisible()
    await expect(page.getByTestId('authoring-side-by-side-layout')).toHaveClass(
      /authoring-side-by-side--stacked/,
    )

    await switchBrand(page, 'REDBC')
    await captureBindingEditorIaScreenshot(page, '05-binding-editor-stacked-redbc-375x812.png')
    await captureBindingEditorIaLocatorScreenshot(
      page.getByTestId('binding-editor-action-rail'),
      '05b-action-rail-stacked-redbc-crop.png',
    )
    await switchBrand(page, 'GREENBC')
    await captureBindingEditorIaScreenshot(page, '05-binding-editor-stacked-greenbc-375x812.png')
  })
})
