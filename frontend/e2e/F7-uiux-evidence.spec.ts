import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  assertDockerStackReady,
  dirtyGuardDialog,
  mutateBindingStructure,
  openDevBindingEditor,
  triggerRouteLeaveViaNav,
} from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  prepareDraftTemplateWithCleanBinding,
  type StructuredAuthoringFixture,
} from './helpers/structured-authoring-api'
import {
  captureF7LocatorScreenshot,
  captureF7Screenshot,
  ensureF7EvidenceDirs,
  F7_NARROW_VIEWPORT,
  F7_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

async function captureF7BrandHeader(page: import('@playwright/test').Page, filename: string) {
  return captureF7LocatorScreenshot(page.locator('.shell-header .header-brand'), filename)
}

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('CORE-FORTRESS F7 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let fixture: StructuredAuthoringFixture

  test.beforeAll(async ({ request }) => {
    ensureF7EvidenceDirs()
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )
    fixture = await prepareDraftTemplateWithCleanBinding(request)
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(F7_VIEWPORT)
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('capture side-by-side, dirty guard, stale, brands, and narrow stack', async ({
    page,
    request,
  }) => {
    await switchBrand(page, 'REDBC')
    await openDevBindingEditor(page, request, fixture.templateId)

    const layout = page.getByTestId('authoring-side-by-side-layout')
    await expect(layout).toBeVisible()
    await expect(page.getByTestId('authoring-editor-pane')).toBeVisible()
    await expect(page.getByTestId('authoring-preview-pane')).toBeVisible()
    await expect(layout).not.toHaveClass(/authoring-side-by-side--stacked/)

    await captureF7LocatorScreenshot(
      layout,
      '01-side-by-side-empty-preview-redbc-1440x900.png',
    )
    await captureF7BrandHeader(page, '02-brand-header-redbc-1440x900.png')

    const boundary = page.getByTestId('authoring-preview-boundary')
    await expect(boundary).toContainText(/guidance only/i)
    await captureF7LocatorScreenshot(
      page.getByTestId('authoring-preview-pane'),
      '03-preview-pane-boundary-empty-redbc-1440x900.png',
    )

    // Dirty guard dialog (Stay / Discard / Save)
    await mutateBindingStructure(page)
    await triggerRouteLeaveViaNav(page)
    const dialog = dirtyGuardDialog(page)
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('heading', { name: /unsaved changes/i })).toBeVisible()
    await expect(dialog.getByTestId('dirty-guard-stay')).toBeVisible()
    await expect(dialog.getByTestId('dirty-guard-discard')).toBeVisible()
    await expect(dialog.getByTestId('dirty-guard-save')).toBeVisible()
    await captureF7Screenshot(page, '04-dirty-guard-dialog-redbc-1440x900.png')
    await dialog.getByTestId('dirty-guard-stay').click()
    await expect(dialog).not.toBeVisible()

    // Stale badge after preview exists + structure mutation
    const refreshButton = page.getByTestId('authoring-preview-refresh')
    const initialRefresh = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-generate') &&
        response.ok(),
      { timeout: 120_000 },
    )
    await refreshButton.click()
    await initialRefresh
    await expect(page.getByTestId('authoring-preview-empty')).toHaveCount(0, { timeout: 60_000 })

    await mutateBindingStructure(page)
    await expect(page.getByTestId('authoring-preview-stale-badge')).toBeVisible()
    await captureF7LocatorScreenshot(
      page.getByTestId('authoring-preview-pane'),
      '05-preview-stale-badge-redbc-1440x900.png',
    )
    await captureF7LocatorScreenshot(
      layout,
      '06-side-by-side-stale-redbc-1440x900.png',
    )

    // GREENBC dual-brand
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await captureF7BrandHeader(page, '07-brand-header-greenbc-1440x900.png')
    await captureF7LocatorScreenshot(
      layout,
      '08-side-by-side-stale-greenbc-1440x900.png',
    )
    await captureF7LocatorScreenshot(
      page.getByTestId('authoring-preview-pane'),
      '09-preview-stale-badge-greenbc-1440x900.png',
    )

    await triggerRouteLeaveViaNav(page)
    await expect(dialog).toBeVisible()
    await captureF7Screenshot(page, '10-dirty-guard-dialog-greenbc-1440x900.png')
    await dialog.getByTestId('dirty-guard-stay').click()
    await expect(dialog).not.toBeVisible()

    // Narrow 375px stacked layout — collapse sidebar so content is visible at 375px
    await switchBrand(page, 'REDBC')
    await page.keyboard.press('Escape')
    const collapseSidebar = page.getByRole('button', { name: /collapse sidebar/i })
    if (await collapseSidebar.isVisible()) {
      await collapseSidebar.click()
    }
    await page.setViewportSize(F7_NARROW_VIEWPORT)
    await expect(layout).toHaveClass(/authoring-side-by-side--stacked/, { timeout: 10_000 })
    const previewToggle = page.getByTestId('authoring-preview-toggle')
    await expect(previewToggle).toBeVisible()
    await previewToggle.scrollIntoViewIfNeeded()
    await captureF7Screenshot(page, '11-side-by-side-stacked-redbc-375x812.png')
    await captureF7LocatorScreenshot(previewToggle, '12-side-by-side-stacked-toggle-redbc-375x812.png')

    await previewToggle.click()
    await expect(page.getByTestId('authoring-preview-boundary')).not.toBeVisible()
    await previewToggle.scrollIntoViewIfNeeded()
    await captureF7Screenshot(page, '13-side-by-side-preview-collapsed-redbc-375x812.png')

    await previewToggle.click()
    await expect(page.getByTestId('authoring-preview-boundary')).toBeVisible()

    await page.setViewportSize(F7_VIEWPORT)
    await switchBrand(page, 'GREENBC')
    await page.keyboard.press('Escape')
    await page.setViewportSize(F7_NARROW_VIEWPORT)
    await expect(layout).toHaveClass(/authoring-side-by-side--stacked/, { timeout: 10_000 })
    await previewToggle.scrollIntoViewIfNeeded()
    await captureF7Screenshot(page, '14-side-by-side-stacked-greenbc-375x812.png')

    // A11y spot checks on key F7 controls (desktop)
    await page.setViewportSize(F7_VIEWPORT)
    await expect(layout).not.toHaveClass(/authoring-side-by-side--stacked/)
    await expect(page.getByTestId('authoring-preview-refresh')).toBeVisible()
    await page.getByTestId('authoring-preview-refresh').focus()
    await expect(page.getByTestId('authoring-preview-refresh')).toBeFocused()

    await mutateBindingStructure(page)
    await triggerRouteLeaveViaNav(page)
    await expect(dialog).toBeVisible()
    await expect(dialog.getByTestId('dirty-guard-stay')).toBeVisible()
    await dialog.getByTestId('dirty-guard-stay').focus()
    await expect(dialog.getByTestId('dirty-guard-stay')).toBeFocused()
    await captureF7Screenshot(page, '15-dirty-guard-focus-stay-greenbc-1440x900.png')
    await dialog.getByTestId('dirty-guard-discard').click()
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })
  })
})
