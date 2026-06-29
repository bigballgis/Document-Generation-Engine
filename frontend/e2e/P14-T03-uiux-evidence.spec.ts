import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

import { expect, test } from '@playwright/test'

import { E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  buildImportJsonFileContent,
  exportTemplateJsonViaApi,
  mutateBundleForStagingImport,
  preparePublishedTemplate,
  type PublishedTemplateFixture,
} from './helpers/template-export-import-api'
import {
  captureP14T03LocatorScreenshot,
  captureP14T03Screenshot,
  ensureP14T03EvidenceDirs,
  P14_T03_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('P14-T03 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let publishedFixture: PublishedTemplateFixture
  let stagingImportPath: string

  test.beforeAll(async ({ request }) => {
    ensureP14T03EvidenceDirs()

    let backendReady = false
    let frontendReady = false
    try {
      const backend = await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })
      backendReady = backend.ok()
    } catch {
      backendReady = false
    }
    try {
      const frontend = await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })
      frontendReady = frontend.ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )

    publishedFixture = await preparePublishedTemplate(request, {
      name: `E2E P14-T03 UIUX ${Date.now()}`,
    })

    const exported = await exportTemplateJsonViaApi(request, publishedFixture.templateId)
    const stagingBundle = mutateBundleForStagingImport(exported)
    stagingImportPath = path.join(
      os.tmpdir(),
      `p14-t03-uiux-staging-import-${Date.now()}.json`,
    )
    fs.writeFileSync(stagingImportPath, buildImportJsonFileContent(stagingBundle), 'utf8')
  })

  test.afterAll(() => {
    if (stagingImportPath && fs.existsSync(stagingImportPath)) {
      fs.unlinkSync(stagingImportPath)
    }
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P14_T03_VIEWPORT)
  })

  async function openImportDialogWithBundle(page: import('@playwright/test').Page) {
    await page.getByRole('button', { name: /import template/i }).click()
    const importDialog = page.locator('.el-dialog').filter({ hasText: /import template bundle/i })
    await expect(importDialog).toBeVisible()
    await importDialog.locator('input[type="file"]').setInputFiles(stagingImportPath)
    await expect(importDialog.getByText(/bundle summary/i)).toBeVisible()
    return importDialog
  }

  test('capture template export/import surfaces and dual-brand evidence', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)

    await page.goto(`/templates/${publishedFixture.templateId}`)
    await expect(page.getByRole('heading', { level: 1, name: publishedFixture.name })).toBeVisible()
    await expect(page.getByText(/^published$/i).first()).toBeVisible()

    const exportButton = page.getByRole('button', { name: /export bundle/i })
    await expect(exportButton).toBeVisible()
    await captureP14T03Screenshot(page, '01-template-detail-export-redbc-1440x900.png')

    await exportButton.click()
    await expect(page.getByRole('menuitem', { name: /download json/i })).toBeVisible()
    await expect(page.getByRole('menuitem', { name: /download zip/i })).toBeVisible()
    await captureP14T03Screenshot(page, '02-template-detail-export-menu-redbc-1440x900.png')
    await page.keyboard.press('Escape')

    await switchBrand(page, 'GREENBC')
    await expect(exportButton).toBeVisible()
    await captureP14T03Screenshot(page, '03-template-detail-export-greenbc-1440x900.png')

    await captureP14T03LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '10-brand-header-greenbc-1440x900.png',
    )
    await switchBrand(page, 'REDBC')
    await captureP14T03LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '11-brand-header-redbc-1440x900.png',
    )

    await page.goto('/templates')
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /import template/i })).toBeVisible()
    await captureP14T03Screenshot(page, '04-templates-list-import-redbc-1440x900.png')

    await page.getByRole('button', { name: /import template/i }).click()
    const emptyImportDialog = page.locator('.el-dialog').filter({ hasText: /import template bundle/i })
    await expect(emptyImportDialog).toBeVisible()
    await captureP14T03LocatorScreenshot(
      emptyImportDialog,
      '05-import-dialog-empty-redbc-1440x900.png',
    )

    await emptyImportDialog.locator('input[type="file"]').setInputFiles(stagingImportPath)
    await expect(emptyImportDialog.getByText(/bundle summary/i)).toBeVisible()
    await expect(emptyImportDialog.getByRole('button', { name: /^import template$/i })).toBeVisible()
    await captureP14T03LocatorScreenshot(
      emptyImportDialog,
      '06-import-dialog-bundle-loaded-redbc-1440x900.png',
    )

    await page.goto('/templates')
    await switchBrand(page, 'GREENBC')
    const greenImportDialog = await openImportDialogWithBundle(page)
    await captureP14T03LocatorScreenshot(
      greenImportDialog,
      '07-import-dialog-bundle-loaded-greenbc-1440x900.png',
    )
  })
})
