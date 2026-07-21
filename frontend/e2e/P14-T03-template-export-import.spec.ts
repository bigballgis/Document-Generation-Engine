import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  assertNoSecretsInSerializedBundle,
  buildImportJsonFileContent,
  exportTemplateJsonViaApi,
  extractSingleFileZipJson,
  mutateBundleForStagingImport,
  preparePublishedTemplate,
  type PublishedTemplateFixture,
} from './helpers/template-export-import-api'
import { reLoginAs } from './helpers/ui'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('P14-T03 template export / import', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let publishedFixture: PublishedTemplateFixture
  let stagingImportPath: string
  let stagingExternalId: string
  let stagingTemplateId: string

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1` })

    publishedFixture = await preparePublishedTemplate(request, {
      name: `E2E P14-T03 Export ${Date.now()}`,
    })

    const exported = await exportTemplateJsonViaApi(request, publishedFixture.templateId)
    const stagingBundle = mutateBundleForStagingImport(exported)
    stagingExternalId = stagingBundle.bundle.metadata.externalId
    stagingTemplateId = stagingBundle.bundle.metadata.templateId

    stagingImportPath = path.join(
      os.tmpdir(),
      `p14-t03-staging-import-${Date.now()}.json`,
    )
    fs.writeFileSync(stagingImportPath, buildImportJsonFileContent(stagingBundle), 'utf8')
  })

  test.afterAll(() => {
    if (stagingImportPath && fs.existsSync(stagingImportPath)) {
      fs.unlinkSync(stagingImportPath)
    }
  })

  test('GROUP admin exports published template as JSON/ZIP without secrets', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto(`/templates/${publishedFixture.templateId}`)
    await expect(page.getByText(publishedFixture.name).first()).toBeVisible()
    await expect(page.getByText(/^published$/i).first()).toBeVisible()

    const exportButton = page.getByRole('button', { name: /export bundle/i })
    await expect(exportButton).toBeVisible()

    const jsonDownloadPromise = page.waitForEvent('download')
    await exportButton.click()
    await page.getByRole('menuitem', { name: /download json/i }).click()
    const jsonDownload = await jsonDownloadPromise
    expect(jsonDownload.suggestedFilename()).toMatch(/\.json$/i)

    const jsonPath = await jsonDownload.path()
    if (!jsonPath) {
      throw new Error('JSON export download path was not available')
    }
    const jsonContent = fs.readFileSync(jsonPath, 'utf8')
    assertNoSecretsInSerializedBundle(jsonContent)
    expect(jsonContent).toContain('template-export-bundle-v1-json')
    expect(jsonContent).toContain(publishedFixture.externalId)
    await expect(page.locator('.el-message').getByText(/export downloaded/i)).toBeVisible()

    const zipDownloadPromise = page.waitForEvent('download')
    await exportButton.click()
    await page.getByRole('menuitem', { name: /download zip/i }).click()
    const zipDownload = await zipDownloadPromise
    expect(zipDownload.suggestedFilename()).toMatch(/\.zip$/i)

    const zipPath = await zipDownload.path()
    if (!zipPath) {
      throw new Error('ZIP export download path was not available')
    }
    const zipJson = extractSingleFileZipJson(fs.readFileSync(zipPath))
    assertNoSecretsInSerializedBundle(zipJson)
    expect(zipJson).toContain('template-export-bundle-v1-json')
    expect(zipJson).toContain(publishedFixture.externalId)
  })

  test('import bundle lands template in DRAFT after dry-run gate', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/templates')
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()

    await page.getByRole('button', { name: /import template/i }).click()
    const importDialog = page.locator('.el-dialog').filter({ hasText: /import template bundle/i })
    await expect(importDialog).toBeVisible()

    await importDialog.locator('input[type="file"]').setInputFiles(stagingImportPath)
    await expect(importDialog.getByText(stagingExternalId)).toBeVisible()
    await expect(importDialog.getByText(stagingTemplateId)).toBeVisible()

    // Wave 7 dry-run gate: Import stays disabled until Check dependencies is ready.
    const importButton = importDialog.getByRole('button', { name: /^import template$/i })
    await expect(importButton).toBeDisabled()
    await importDialog.getByRole('button', { name: /check dependencies/i }).click()
    await expect(importDialog.getByText(/ready to import/i)).toBeVisible({ timeout: 60_000 })
    await expect(importButton).toBeEnabled()
    await importButton.click()
    await expect(page.locator('.el-message').getByText(/imported successfully/i)).toBeVisible()
    await expect(page).toHaveURL(new RegExp(`/templates/${stagingTemplateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`))
    await expect(page.getByText(/^drafting$/i).first()).toBeVisible()

    await reLoginAs(page, loginAs, E2E_TEMPLATE_AUTHOR)
    await page.goto(`/templates/${stagingTemplateId}`)
    await expect(page.getByText(/staging import/i).first()).toBeVisible()
    await expect(page.getByText(/^drafting$/i).first()).toBeVisible()

    await reLoginAs(page, loginAs, E2E_TEMPLATE_TESTER)
    await page.goto(`/templates/${stagingTemplateId}`)
    await expect(page.getByText(/staging import/i).first()).toBeVisible()
    await expect(page.getByText(/^drafting$/i).first()).toBeVisible()
  })
})
