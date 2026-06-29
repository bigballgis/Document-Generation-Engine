import { expect, test } from '@playwright/test'

import {
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import {
  attachReferenceToDemoTemplate,
  createApprovedContentModule,
  createDraftContentModule,
} from './helpers/content-modules-api'
import {
  openContentModulesList,
  openDemoTemplateAuthoringTab,
  reLoginAs,
} from './helpers/ui'
import {
  captureBrandHeader,
  captureP14LocatorScreenshot,
  captureP14Screenshot,
  ensureP14EvidenceDirs,
  P14_T01_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

test.describe('P14-T01 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(() => {
    ensureP14EvidenceDirs()
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P14_T01_VIEWPORT)
  })

  test('capture content-modules, dialogs, template panel, and brand evidence', async ({
    page,
    request,
  }) => {
    const draftModule = await createDraftContentModule(request)
    const approvedModule = await createApprovedContentModule(request, {
      name: `E2E UIUX Lifecycle ${Date.now()}`,
    })
    const referenceKey = `E2E_UIUX_REF_${Date.now().toString(36).toUpperCase()}`
    await attachReferenceToDemoTemplate(request, approvedModule, referenceKey)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openContentModulesList(page)

    await captureP14Screenshot(page, '01-content-modules-list-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await captureP14Screenshot(page, '02-content-modules-list-greenbc-1440x900.png')

    await captureBrandHeader(page, '10-brand-header-greenbc-1440x900.png')
    await switchBrand(page, 'REDBC')
    await captureBrandHeader(page, '11-brand-header-redbc-1440x900.png')

    await page.getByRole('button', { name: /new content module/i }).click()
    const createDialog = page.locator('.el-dialog').filter({ hasText: /create content module/i })
    await expect(createDialog).toBeVisible()
    await captureP14LocatorScreenshot(createDialog, '03-create-content-module-dialog-1440x900.png')
    await createDialog.getByRole('button', { name: /cancel/i }).click()
    await expect(createDialog).toHaveCount(0)

    await page.goto(`/content-modules/${draftModule.moduleId}`)
    await expect(page.getByRole('heading', { level: 1, name: draftModule.name })).toBeVisible()
    await expect(page.getByText(/^draft$/i).first()).toBeVisible()
    await captureP14Screenshot(page, '04-content-module-detail-draft-1440x900.png')

    await openDemoTemplateAuthoringTab(page, DEMO_TEMPLATE_EXTERNAL_ID)
    const panel = page.locator('.content-module-references-panel')
    await expect(panel.getByRole('heading', { name: /content module references/i })).toBeVisible()
    await captureP14LocatorScreenshot(panel, '05-template-content-module-references-panel-1440x900.png')

    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await page.goto(`/content-modules/${approvedModule.moduleId}`)
    await expect(page.getByRole('heading', { level: 1, name: approvedModule.name })).toBeVisible()
    await expect(page.getByText(/^approved$/i).first()).toBeVisible()

    await page.getByRole('button', { name: /stop module/i }).click()
    const impactDialog = page.locator('.el-dialog').filter({ hasText: /lifecycle impact preview/i })
    await expect(impactDialog).toBeVisible()
    await expect(impactDialog.getByText(/referencing templates/i)).toBeVisible()
    await captureP14LocatorScreenshot(
      impactDialog,
      '06-lifecycle-impact-preview-dialog-1440x900.png',
    )
  })
})
