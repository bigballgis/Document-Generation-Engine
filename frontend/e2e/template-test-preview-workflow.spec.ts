import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { assertFolCatalogSeeded } from './helpers/fol-api'

test.describe('template test & preview workflow (dev editor)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('draft dev editor routes test actions to Test & Preview tab', async ({ page, request }) => {
    test.setTimeout(120_000)
    const fixture = await assertFolCatalogSeeded(request)

    await page.goto(`/templates/${fixture.templateId}`)
    await page
      .locator('.version-lines-card')
      .getByRole('button', { name: /view detail/i })
      .first()
      .click()

    await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
    await expect(page.locator('#dev-version-actions')).toBeVisible({ timeout: 30_000 })

    const devActions = page.locator('#dev-version-actions')
    await expect(devActions.getByRole('button', { name: /open test & preview/i })).toBeVisible()
    await expect(devActions.getByRole('button', { name: /^test generate$/i })).toHaveCount(0)
    await expect(devActions.getByRole('button', { name: /submit for test/i })).toHaveCount(0)

    await page.getByRole('tab', { name: /^template design$/i }).click()
    const authoringTabs = page.locator('.authoring-sub-tabs')
    await authoringTabs.getByRole('tab', { name: /test & preview/i }).click()

    const workflow = page.locator('.test-preview-workflow')
    await expect(workflow).toBeVisible({ timeout: 15_000 })
    await expect(workflow.getByRole('button', { name: /run preview \(selected\)/i })).toBeVisible()
    await expect(workflow.getByRole('button', { name: /batch preview all/i })).toBeVisible()
    await expect(workflow.getByRole('button', { name: /submit for test/i })).toBeVisible()

    const dataSetPanel = page.locator('.test-data-set-panel')
    await expect(dataSetPanel).toBeVisible()
    await expect(dataSetPanel.getByRole('button', { name: /^run preview$/i }).first()).toBeVisible()

    await expect(page.locator('.test-preview-workflow .context-help-trigger')).toBeVisible()
    await expect(page.locator('.test-data-set-panel .context-help-trigger')).toBeVisible()
  })

  test('header shortcut opens Test & Preview sub-tab', async ({ page, request }) => {
    test.setTimeout(90_000)
    const fixture = await assertFolCatalogSeeded(request)

    await page.goto(`/templates/${fixture.templateId}`)
    await page
      .locator('.version-lines-card')
      .getByRole('button', { name: /view detail/i })
      .first()
      .click()

    await expect(page).toHaveURL(/\/dev\//, { timeout: 15_000 })
    await expect(page.locator('#dev-version-actions')).toBeVisible({ timeout: 30_000 })

    await page.locator('#dev-version-actions').getByRole('button', { name: /open test & preview/i }).click()
    await expect(page).toHaveURL(/authoringTab=testPreview/)
    await expect(page.locator('.test-preview-workflow')).toBeVisible({ timeout: 15_000 })
  })
})
