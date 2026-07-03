import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { assertFolCatalogSeeded } from './helpers/fol-api'

test.describe('template test & preview workflow (dev editor)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('draft dev editor exposes test actions on Template testing tab row', async ({ page, request }) => {
    test.setTimeout(120_000)
    const fixture = await assertFolCatalogSeeded(request)

    await page.goto(`/templates/${fixture.templateId}`)
    await page
      .locator('.version-lines-card')
      .getByRole('button', { name: /view detail/i })
      .first()
      .click()

    await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('#dev-version-actions')).toHaveCount(0)

    const workspace = page.locator('.workspace-tab-shell')
    await workspace.getByRole('tab', { name: /^template testing$/i }).click()
    await expect(page).toHaveURL(/workspaceTab=testing/)

    const actions = workspace.locator('.workspace-tab-shell__actions')
    await expect(actions.getByRole('button', { name: /^full test$/i })).toBeVisible()
    await expect(actions.getByRole('button', { name: /submit for test/i })).toBeVisible()
    await expect(actions.getByRole('button', { name: /batch preview all/i })).toHaveCount(0)
    await expect(actions.getByRole('button', { name: /run preview \(selected\)/i })).toHaveCount(0)

    const dataSetPanel = page.locator('.test-data-set-panel')
    await expect(dataSetPanel).toBeVisible()
    await expect(dataSetPanel.getByRole('button', { name: /^run preview$/i }).first()).toBeVisible()

    await expect(page.locator('.test-data-set-panel .context-help-trigger')).toBeVisible()

    await page.locator('.testing-sub-tabs').getByRole('tab', { name: /preview runs/i }).click()
    await expect(page.locator('.batch-test-history')).toBeVisible()
  })

  test('legacy authoringTab=testPreview deep-link opens Template testing tab', async ({ page, request }) => {
    test.setTimeout(90_000)
    const fixture = await assertFolCatalogSeeded(request)

    await page.goto(`/templates/${fixture.templateId}`)
    await page
      .locator('.version-lines-card')
      .getByRole('button', { name: /view detail/i })
      .first()
      .click()

    await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
    const devUrl = page.url()
    await page.goto(`${devUrl}${devUrl.includes('?') ? '&' : '?'}tab=authoring&authoringTab=testPreview`)

    await expect(page.locator('.workspace-tab-shell').getByRole('tab', { name: /^template testing$/i })).toHaveAttribute(
      'aria-selected',
      'true',
      { timeout: 15_000 },
    )
    await expect(page.locator('.test-data-set-panel')).toBeVisible({ timeout: 15_000 })
  })
})
