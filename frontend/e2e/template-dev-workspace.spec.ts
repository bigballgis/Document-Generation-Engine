import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { assertFolCatalogSeeded } from './helpers/fol-api'

async function openFolDevEditor(page: import('@playwright/test').Page, templateId: string) {
  await page.goto(`/templates/${templateId}`)
  await page
    .locator('.version-lines-card')
    .getByRole('button', { name: /view detail/i })
    .first()
    .click()
  await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
}

test.describe('template dev workspace tab shell', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('journey is read-only and actions live on workspace tab row only', async ({ page, request }) => {
    test.setTimeout(120_000)
    const fixture = await assertFolCatalogSeeded(request)
    await openFolDevEditor(page, fixture.templateId)

    await expect(page.locator('#dev-version-actions')).toHaveCount(0)
    await expect(page.locator('[data-template-journey-cta]')).toHaveCount(0)

    const workspace = page.locator('.workspace-tab-shell')
    const actions = workspace.locator('.workspace-tab-shell__actions')

    await expect(workspace.getByRole('tab', { name: /^template design$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(actions.getByRole('button')).toHaveCount(0)

    await workspace.getByRole('tab', { name: /^template testing$/i }).click()
    await expect(page).toHaveURL(/workspaceTab=testing/)
    await expect(page.locator('.testing-sub-tabs').getByRole('tab', { name: /test data sets/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(actions.getByRole('button', { name: /^full test$/i })).toBeVisible()
    await expect(actions.getByRole('button', { name: /submit for test/i })).toBeVisible()
    await expect(actions.getByRole('button', { name: /batch preview all/i })).toHaveCount(0)
    await expect(actions.getByRole('button', { name: /run preview \(selected\)/i })).toHaveCount(0)
    await expect(page.locator('.test-preview-workflow__actions')).toHaveCount(0)

    await workspace.getByRole('tab', { name: /^template approval$/i }).click()
    await expect(page).toHaveURL(/workspaceTab=approval/)
    await expect(page.locator('.approval-sub-tabs').getByRole('tab', { name: /submit for approval/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(page.getByRole('heading', { name: /^template approval$/i })).toBeVisible({
      timeout: 15_000,
    })
  })

  test('design sub-tabs exclude test preview and have no action rail', async ({ page, request }) => {
    test.setTimeout(90_000)
    const fixture = await assertFolCatalogSeeded(request)
    await openFolDevEditor(page, fixture.templateId)

    const designSubTabs = page.locator('.design-sub-tabs')
    await expect(designSubTabs.getByRole('tab', { name: /^variables$/i })).toBeVisible()
    await expect(designSubTabs.getByRole('tab', { name: /^clause references$/i })).toBeVisible()
    await expect(designSubTabs.getByRole('tab', { name: /^bindings$/i })).toBeVisible()
    await expect(designSubTabs.getByRole('tab', { name: /test & preview/i })).toHaveCount(0)

    await page.locator('.workspace-tab-shell').getByRole('tab', { name: /^template testing$/i }).click()
    const testingSubTabs = page.locator('.testing-sub-tabs')
    await expect(testingSubTabs.getByRole('tab', { name: /test data sets/i })).toBeVisible()
    await expect(testingSubTabs.getByRole('tab', { name: /preview runs/i })).toBeVisible()
    await expect(testingSubTabs.getByRole('tab', { name: /^coverage$/i })).toBeVisible()
    await expect(testingSubTabs.getByRole('tab', { name: /change diff/i })).toBeVisible()

    await page.locator('.workspace-tab-shell').getByRole('tab', { name: /^template approval$/i }).click()
    const approvalSubTabs = page.locator('.approval-sub-tabs')
    await expect(approvalSubTabs.getByRole('tab', { name: /submit for approval/i })).toBeVisible()
    await expect(approvalSubTabs.getByRole('tab', { name: /publish readiness/i })).toBeVisible()
    await expect(approvalSubTabs.getByRole('tab', { name: /risk prompts/i })).toBeVisible()
    await expect(approvalSubTabs.getByRole('tab', { name: /^maintenance$/i })).toBeVisible()
  })

  test('submit for test stays disabled until full test gate passes', async ({ page, request }) => {
    test.setTimeout(90_000)
    const fixture = await assertFolCatalogSeeded(request)
    await openFolDevEditor(page, fixture.templateId)

    await page.locator('.workspace-tab-shell').getByRole('tab', { name: /^template testing$/i }).click()
    const submitButton = page
      .locator('.workspace-tab-shell__actions')
      .getByRole('button', { name: /submit for test/i })
    await expect(submitButton).toBeDisabled()
  })
})
