/**
 * SYS-NORM Wave 7 / TM #151 — Import dry-run UI (promotion pack)
 *
 * BDD SoT: docs/behavior/sys-norm-promotion-pack.md
 *   BDD-SYS-NORM-PP-016 — UI dry-run report rendering
 *   BDD-SYS-NORM-PP-017 — Import gated on readyToCommit; clear on input change
 *   BDD-SYS-NORM-PP-018 — UI commit success → DRAFT observable
 *   BDD-SYS-NORM-PP-015 — Authorization fail-closed (TEMPLATE_TESTER)
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/SYS-NORM-W7-promotion-import-dry-run.spec.ts `
 *     --config playwright.docker.config.ts
 */
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

import { expect, test, type Locator, type Page } from '@playwright/test'

import {
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  assertNoSecretsInSerializedBundle,
  exportPromotionZipViaApi,
  mutatePromotionZipForStagingImport,
  preparePublishedTemplate,
  stripMasterDocxFromPromotionZip,
  type PublishedTemplateFixture,
  type StagingPromotionZip,
} from './helpers/template-export-import-api'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

function importDialog(page: Page): Locator {
  return page.locator('.el-dialog').filter({ hasText: /import template bundle/i })
}

async function openImportDialog(page: Page): Promise<Locator> {
  await page.goto('/templates')
  await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
  await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
  await page.getByRole('button', { name: /import template/i }).click()
  const dialog = importDialog(page)
  await expect(dialog).toBeVisible()
  return dialog
}

async function uploadZip(dialog: Locator, zipPath: string): Promise<void> {
  await dialog.locator('input[type="file"]').setInputFiles(zipPath)
}

async function ensureDemoMasterSelected(page: Page, dialog: Locator): Promise<void> {
  const masterSelect = dialog.locator('.el-form-item').filter({ hasText: /target letterhead/i }).locator('.el-select')
  await expect(masterSelect).toBeVisible()
  await expect(masterSelect).not.toHaveClass(/is-disabled/)
  const selected = await masterSelect.innerText()
  if (!new RegExp(DEMO_MASTER_NAME, 'i').test(selected)) {
    await masterSelect.click()
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: new RegExp(DEMO_MASTER_NAME, 'i') })
      .first()
    await expect(option).toBeVisible()
    await option.click()
  }
  await expect(masterSelect).toContainText(new RegExp(DEMO_MASTER_NAME, 'i'))
}

test.describe('SYS-NORM Wave 7 promotion import dry-run UI', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let publishedFixture: PublishedTemplateFixture
  let staging: StagingPromotionZip
  let readyZipPath: string
  let blockingZipPath: string

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })

    publishedFixture = await preparePublishedTemplate(request, {
      name: `E2E SYS-NORM-W7 Promotion ${Date.now()}`,
    })

    const promotionZip = await exportPromotionZipViaApi(request, publishedFixture.templateId)
    staging = mutatePromotionZipForStagingImport(promotionZip, { injectSyntheticAsset: true })
    const blockingZip = stripMasterDocxFromPromotionZip(staging.zipBytes)

    const tmp = os.tmpdir()
    readyZipPath = path.join(tmp, `sys-norm-w7-ready-${Date.now()}.zip`)
    blockingZipPath = path.join(tmp, `sys-norm-w7-blocking-${Date.now()}.zip`)
    fs.writeFileSync(readyZipPath, staging.zipBytes)
    fs.writeFileSync(blockingZipPath, blockingZip)

    assertNoSecretsInSerializedBundle(staging.zipBytes.toString('utf8'))
  })

  test.afterAll(() => {
    for (const filePath of [readyZipPath, blockingZipPath]) {
      if (filePath && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath)
      }
    }
  })

  test('PP-016/017 — Check dependencies renders report; Import gated; clears on policy change', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    const dialog = await openImportDialog(page)

    const importButton = dialog.getByRole('button', { name: /^import template$/i })
    const checkButton = dialog.getByRole('button', { name: /check dependencies/i })
    await expect(importButton).toBeDisabled()

    await uploadZip(dialog, readyZipPath)
    await expect(dialog.getByText(staging.externalId)).toBeVisible()
    await expect(dialog.getByText(staging.templateId)).toBeVisible()
    await ensureDemoMasterSelected(page, dialog)
    await expect(importButton).toBeDisabled()

    await checkButton.click()
    await expect(dialog.getByRole('heading', { name: /dependency report/i })).toBeVisible({
      timeout: 60_000,
    })
    await expect(dialog.getByText(/ready to import/i)).toBeVisible()
    await expect(dialog.getByText(/blocking:\s*\d+/i)).toBeVisible()
    await expect(dialog.getByText(/warnings:\s*\d+/i)).toBeVisible()
    await expect(dialog.getByText(/info:\s*\d+/i)).toBeVisible()
    await expect(dialog.getByText('MASTER_PIN').first()).toBeVisible()
    await expect(dialog.getByText('ASSET_BINARY').first()).toBeVisible()
    await expect(dialog.getByText(/WILL_MATERIALIZE/i).first()).toBeVisible()
    if (staging.injectedAssetKey) {
      await expect(dialog.getByText(staging.injectedAssetKey).first()).toBeVisible()
    }
    // CLAUSE_NESTING appears when the pack carries a nesting graph (seed-dependent).
    const nestingCount = await dialog.getByText('CLAUSE_NESTING').count()
    if (nestingCount > 0) {
      await expect(dialog.getByText('CLAUSE_NESTING').first()).toBeVisible()
    }

    const reportText = await dialog.locator('.dependency-report').innerText()
    assertNoSecretsInSerializedBundle(reportText)
    await expect(importButton).toBeEnabled()

    // Element Plus radios hide the native input; click the visible label/control.
    await dialog.locator('.el-radio').filter({ hasText: /keep template id/i }).click()
    await expect(dialog.locator('.dependency-report')).toHaveCount(0)
    await expect(importButton).toBeDisabled()

    await dialog
      .locator('.el-radio')
      .filter({ hasText: /reject import when template id already exists/i })
      .click()
    await expect(importButton).toBeDisabled()
    await checkButton.click()
    await expect(dialog.getByText(/ready to import/i)).toBeVisible({ timeout: 60_000 })
    await expect(importButton).toBeEnabled()
  })

  test('PP-017 — blocking dry-run keeps Import disabled', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    const dialog = await openImportDialog(page)
    const importButton = dialog.getByRole('button', { name: /^import template$/i })

    await uploadZip(dialog, blockingZipPath)
    await expect(dialog.getByText(staging.externalId)).toBeVisible()
    await ensureDemoMasterSelected(page, dialog)
    await dialog.getByRole('button', { name: /check dependencies/i }).click()

    await expect(dialog.getByRole('heading', { name: /dependency report/i })).toBeVisible({
      timeout: 60_000,
    })
    await expect(dialog.getByText(/not ready to import/i)).toBeVisible()
    await expect(dialog.getByText(/MASTER_DOCX_ABSENT|MASTER_PIN/i).first()).toBeVisible()
    await expect(importButton).toBeDisabled()
  })

  test('PP-018 — successful Import lands DRAFT (not published)', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    const dialog = await openImportDialog(page)

    await uploadZip(dialog, readyZipPath)
    await expect(dialog.getByText(staging.externalId)).toBeVisible()
    await ensureDemoMasterSelected(page, dialog)
    await dialog.getByRole('button', { name: /check dependencies/i }).click()
    await expect(dialog.getByText(/ready to import/i)).toBeVisible({ timeout: 60_000 })

    const importButton = dialog.getByRole('button', { name: /^import template$/i })
    await expect(importButton).toBeEnabled()
    await importButton.click()

    await expect(page.locator('.el-message').getByText(/imported successfully/i)).toBeVisible({
      timeout: 60_000,
    })
    await expect(page).toHaveURL(
      new RegExp(`/templates/${staging.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`),
    )
    // Package hub labels DRAFT as "Drafting" (en); must not claim published.
    await expect(page.getByText(/^drafting$/i).first()).toBeVisible()
    await expect(page.getByText(/^published$/i)).toHaveCount(0)
    await expect(page.getByText(staging.name).first()).toBeVisible()
  })

  test('PP-015 — TEMPLATE_TESTER has no Import affordance', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/templates')
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /import template/i })).toHaveCount(0)
  })
})
