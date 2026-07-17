/**
 * CE-U17 UIUX evidence — Binding editor shortcuts + command palette Actions
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/ce-u17-editor-shortcuts.md (BDD-CE-U17-EKS-012)
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { openDevBindingEditor } from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import {
  captureCeU17LocatorScreenshot,
  captureCeU17Screenshot,
  CE_U17_VIEWPORT,
  ensureCeU17EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`
const PALETTE_CHORD = `${process.platform === 'darwin' ? 'Meta' : 'Control'}+k`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openEditor(page: Page, request: Parameters<typeof openDevBindingEditor>[1], templateId: string) {
  await openDevBindingEditor(page, request, templateId)
  await dismissOnboardingTourIfPresent(page)
  await expect(page.getByTestId('binding-editor')).toBeVisible()
}

async function openPalette(page: Page) {
  await page.keyboard.press(PALETTE_CHORD)
  await expect(page.getByTestId('command-palette')).toBeVisible()
  await expect(page.getByTestId('command-palette-input')).toBeFocused()
}

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return {
      scrollWidth: doc.scrollWidth,
      clientWidth: doc.clientWidth,
    }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function expectNoCriticalAxeViolations(
  page: Page,
  label: string,
  includeSelector?: string,
): Promise<void> {
  let builder = new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
  if (includeSelector) {
    builder = builder.include(includeSelector)
    // Pre-existing CommandPaletteResults: role=listbox hosts h2 group titles (Pages/Actions).
    // Not introduced by CE-U17 action rows; tracked as Suggestion in UIUX manifest.
    builder = builder.disableRules(['aria-required-children'])
  } else {
    // Pre-existing EP style-picker combobox lacks accessible name (same as CE-U20/U21).
    builder = builder.exclude('[data-testid=style-picker]')
  }
  const results = await builder.analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

/** Fail if Save/Refresh action titles or shortcut subtitles clip inside the option box. */
async function assertPaletteActionTextNotClipped(page: Page): Promise<void> {
  const actions = [
    page.getByTestId('command-palette-action-save-binding'),
    page.getByTestId('command-palette-action-refresh-preview'),
  ]
  for (const action of actions) {
    await expect(action).toBeVisible()
    const clipped = await action.evaluate((el) => {
      const title = el.querySelector('.command-palette__option-title') as HTMLElement | null
      const subtitle = el.querySelector('.command-palette__option-subtitle') as HTMLElement | null
      const check = (node: HTMLElement | null) => {
        if (!node) {
          return true
        }
        return node.scrollWidth > node.clientWidth + 1 || node.scrollHeight > node.clientHeight + 1
      }
      return {
        titleClipped: check(title),
        subtitleClipped: check(subtitle),
        titleText: title?.textContent?.trim() ?? '',
        subtitleText: subtitle?.textContent?.trim() ?? '',
      }
    })
    expect(clipped.titleClipped, `title clipped: ${clipped.titleText}`).toBe(false)
    expect(clipped.subtitleClipped, `subtitle clipped: ${clipped.subtitleText}`).toBe(false)
  }
}

async function assertAuthorActionsVisible(page: Page): Promise<void> {
  await expect(page.getByTestId('command-palette-group-actions')).toBeVisible()
  const saveAction = page.getByTestId('command-palette-action-save-binding')
  const refreshAction = page.getByTestId('command-palette-action-refresh-preview')
  await expect(saveAction).toBeVisible()
  await expect(saveAction).toContainText(/save binding/i)
  await expect(saveAction).toContainText(/Ctrl\+S|⌘S/i)
  await expect(refreshAction).toBeVisible()
  await expect(refreshAction).toContainText(/refresh preview/i)
  await expect(refreshAction).toContainText(/Ctrl\+P|⌘P/i)
  await assertPaletteActionTextNotClipped(page)
}

test.describe('CE-U17 editor shortcuts UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let templateId = ''

  test.beforeAll(async ({ request }) => {
    ensureCeU17EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)
    const draft = await prepareDraftTemplateWithCleanBinding(request)
    templateId = draft.templateId
  })

  test('01–02 dual-brand: binding editor + palette Actions (EKS-012)', async ({ page, request }) => {
    await page.setViewportSize(CE_U17_VIEWPORT)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openEditor(page, request, templateId)
    await expect(page.getByTestId('authoring-side-by-side-layout')).toBeVisible()
    await expect(page.getByTestId('authoring-preview-pane')).toBeVisible()
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U17 binding editor REDBC')

    await captureCeU17Screenshot(page, '01-binding-editor-redbc-1920x1080.png')
    await captureCeU17LocatorScreenshot(
      page.getByTestId('binding-editor'),
      '01b-binding-editor-crop-redbc-1920x1080.png',
    )
    await captureCeU17LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )

    await page.getByTestId('controlled-structured-content-editor').click()
    await openPalette(page)
    await assertAuthorActionsVisible(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U17 palette Actions REDBC', '[data-testid=command-palette]')

    await captureCeU17Screenshot(page, '01d-palette-actions-redbc-1920x1080.png')
    await captureCeU17LocatorScreenshot(
      page.getByTestId('command-palette'),
      '01e-command-palette-crop-redbc-1920x1080.png',
    )
    await captureCeU17LocatorScreenshot(
      page.getByTestId('command-palette-group-actions'),
      '01f-actions-group-crop-redbc-1920x1080.png',
    )

    await page.keyboard.press('Escape')
    await expect(page.getByTestId('command-palette')).toHaveCount(0)

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.getByTestId('binding-editor')).toBeVisible()
    await assertNoViewportOverflow(page)

    await captureCeU17Screenshot(page, '02-binding-editor-greenbc-1920x1080.png')
    await captureCeU17LocatorScreenshot(
      page.getByTestId('binding-editor'),
      '02b-binding-editor-crop-greenbc-1920x1080.png',
    )
    await captureCeU17LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02c-brand-header-greenbc-crop.png',
    )

    await page.getByTestId('controlled-structured-content-editor').click()
    await openPalette(page)
    await assertAuthorActionsVisible(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U17 palette Actions GREENBC', '[data-testid=command-palette]')

    await captureCeU17Screenshot(page, '02d-palette-actions-greenbc-1920x1080.png')
    await captureCeU17LocatorScreenshot(
      page.getByTestId('command-palette'),
      '02e-command-palette-crop-greenbc-1920x1080.png',
    )
    await captureCeU17LocatorScreenshot(
      page.getByTestId('command-palette-group-actions'),
      '02f-actions-group-crop-greenbc-1920x1080.png',
    )

    // Focus ring on first action (keyboard polish)
    await page.keyboard.press('ArrowDown')
    const saveAction = page.getByTestId('command-palette-action-save-binding')
    await expect(saveAction).toHaveClass(/command-palette__option--active/)
    await captureCeU17LocatorScreenshot(
      page.getByTestId('command-palette'),
      '02g-palette-action-highlight-greenbc-1920x1080.png',
    )

    await page.keyboard.press('Escape')
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
  })

  test('03 bindings list: no author Actions outside edit surface', async ({ page, request }) => {
    await page.setViewportSize(CE_U17_VIEWPORT)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    // Stay on bindings list (not anchor edit)
    const { listTemplateVersionLines } = await import('./helpers/template-version-lines-api')
    const lines = await listTemplateVersionLines(request, templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()
    await page.goto(
      `/templates/${templateId}/dev/${inFlight!.devVersionId}?workspaceTab=design&designTab=bindings`,
    )
    await dismissOnboardingTourIfPresent(page)
    await expect(page.locator('.bindings-panel')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('binding-editor')).toHaveCount(0)

    await openPalette(page)
    await expect(page.getByTestId('command-palette-group-actions')).toHaveCount(0)
    await expect(page.getByTestId('command-palette-action-save-binding')).toHaveCount(0)
    await expect(page.getByTestId('command-palette-action-refresh-preview')).toHaveCount(0)
    await assertNoViewportOverflow(page)

    await captureCeU17Screenshot(page, '03-bindings-list-palette-no-actions-redbc-1920x1080.png')
    await captureCeU17LocatorScreenshot(
      page.getByTestId('command-palette'),
      '03b-palette-no-actions-crop-redbc-1920x1080.png',
    )

    await page.keyboard.press('Escape')
  })
})
