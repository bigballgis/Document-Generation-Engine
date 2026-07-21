/**
 * SYS-NORM Wave 7 / TM #151 UIUX evidence — Templates Import dry-run dialog
 * (Check dependencies, dependency report, gated Import) @1440×900 dual-brand.
 *
 * BDD SoT: docs/behavior/sys-norm-promotion-pack.md (PP-016 / PP-017 visual surfaces)
 *
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/SYS-NORM-W7-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

import { expect, test, type Locator, type Page } from '@playwright/test'

import {
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  loginAs,
} from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  exportPromotionZipViaApi,
  mutatePromotionZipForStagingImport,
  preparePublishedTemplate,
  stripMasterDocxFromPromotionZip,
  type PublishedTemplateFixture,
  type StagingPromotionZip,
} from './helpers/template-export-import-api'
import {
  captureSysNormW7LocatorScreenshot,
  captureSysNormW7Screenshot,
  dismissOnboardingTourIfPresent,
  ensureSysNormW7EvidenceDirs,
  switchBrand,
  SYS_NORM_W7_VIEWPORT,
  type BrandPreset,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

function importDialog(page: Page): Locator {
  return page.locator('.el-dialog').filter({ hasText: /import template bundle/i })
}

async function openImportDialog(page: Page): Promise<Locator> {
  await page.goto('/templates')
  await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
  await dismissOnboardingTourIfPresent(page)
  await page.getByRole('button', { name: /import template/i }).click()
  const dialog = importDialog(page)
  await expect(dialog).toBeVisible()
  return dialog
}

async function ensureDemoMasterSelected(page: Page, dialog: Locator): Promise<void> {
  const masterSelect = dialog
    .locator('.el-form-item')
    .filter({ hasText: /target letterhead/i })
    .locator('.el-select')
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

async function assertPrimaryBrandColor(page: Page, brand: BrandPreset): Promise<void> {
  const expected = brand === 'REDBC' ? '#db0011' : '#00847f'
  const primary = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim().toLowerCase(),
  )
  expect(primary, `expected --brand-primary ${expected} for ${brand}`).toBe(expected)
}

/** Normalize any CSS color (rgb / oklab / hex) to sRGB channels via canvas. */
async function readNormalizedRgb(
  locator: Locator,
): Promise<{ raw: string; r: number; g: number; b: number }> {
  return locator.evaluate((el) => {
    const raw = getComputedStyle(el).backgroundColor
    const canvas = document.createElement('canvas')
    canvas.width = 1
    canvas.height = 1
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) {
      return { raw, r: -1, g: -1, b: -1 }
    }
    ctx.clearRect(0, 0, 1, 1)
    ctx.fillStyle = raw
    ctx.fillRect(0, 0, 1, 1)
    const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data
    return { raw, r, g, b }
  })
}

async function assertEnabledPrimaryUsesBrand(dialog: Locator): Promise<void> {
  const importButton = dialog.getByRole('button', { name: /^import template$/i })
  await expect(importButton).toBeEnabled()
  const { raw, r, g, b } = await readNormalizedRgb(importButton)
  // Enabled primary must not use Element Plus default blue.
  expect(raw).not.toMatch(/rgb\(64,\s*158,\s*255\)/)
  expect(raw).not.toMatch(/rgb\(160,\s*207,\s*255\)/)
  expect(r >= 0, `could not normalize background ${raw}`).toBe(true)
  // EP default primary #409EFF ≈ rgb(64, 158, 255)
  expect(!(r < 100 && g > 140 && b > 220), `bg ${raw} looks like EP default blue`).toBe(true)
  const isRedFamily = r > 150 && r > g && r > b
  const isTealFamily = g > 80 && b > 80 && r < 80
  expect(isRedFamily || isTealFamily, `bg ${raw} → rgb(${r},${g},${b}) not red/teal brand family`).toBe(
    true,
  )
}

/** Critical regression: footer actions must stay inside the 1440×900 viewport. */
async function assertImportFooterInViewport(dialog: Locator, page: Page): Promise<void> {
  const footer = dialog.locator('[data-testid="template-import-dialog-footer"]')
  await expect(footer).toBeVisible()
  await expect(footer.getByRole('button', { name: /^import template$/i })).toBeVisible()
  const box = await footer.boundingBox()
  expect(box, 'footer bounding box missing').toBeTruthy()
  const viewport = page.viewportSize()
  expect(viewport, 'viewport missing').toBeTruthy()
  expect(box!.y, 'footer top above viewport').toBeGreaterThanOrEqual(0)
  expect(box!.y + box!.height, 'footer clipped below viewport').toBeLessThanOrEqual(
    viewport!.height + 1,
  )
}

test.describe('SYS-NORM Wave 7 Import dry-run UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let publishedFixture: PublishedTemplateFixture
  let staging: StagingPromotionZip
  let readyZipPath: string
  let blockingZipPath: string

  test.beforeAll(async ({ request }) => {
    ensureSysNormW7EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })

    publishedFixture = await preparePublishedTemplate(request, {
      name: `E2E SYS-NORM-W7 UIUX ${Date.now()}`,
    })
    const promotionZip = await exportPromotionZipViaApi(request, publishedFixture.templateId)
    staging = mutatePromotionZipForStagingImport(promotionZip, { injectSyntheticAsset: true })
    const blockingZip = stripMasterDocxFromPromotionZip(staging.zipBytes)

    const tmp = os.tmpdir()
    readyZipPath = path.join(tmp, `sys-norm-w7-uiux-ready-${Date.now()}.zip`)
    blockingZipPath = path.join(tmp, `sys-norm-w7-uiux-blocking-${Date.now()}.zip`)
    fs.writeFileSync(readyZipPath, staging.zipBytes)
    fs.writeFileSync(blockingZipPath, blockingZip)
  })

  test.afterAll(() => {
    for (const filePath of [readyZipPath, blockingZipPath]) {
      if (filePath && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath)
      }
    }
  })

  for (const brand of ['REDBC', 'GREENBC'] as const) {
    test(`empty Import dialog — ${brand} @1440×900`, async ({ page }) => {
      await page.setViewportSize(SYS_NORM_W7_VIEWPORT)
      await loginAs(page, E2E_GROUP_ADMIN)
      await dismissOnboardingTourIfPresent(page)
      await switchBrand(page, brand)
      await assertPrimaryBrandColor(page, brand)

      const dialog = await openImportDialog(page)
      await expect(dialog.getByRole('heading', { name: /import template bundle/i })).toBeVisible()
      await expect(dialog.getByRole('button', { name: /check dependencies/i })).toBeDisabled()
      await expect(dialog.getByRole('button', { name: /^import template$/i })).toBeDisabled()
      await assertNoViewportOverflow(page)

      await captureSysNormW7Screenshot(page, `01-import-empty-${brand.toLowerCase()}-1440x900.png`)
      await captureSysNormW7LocatorScreenshot(
        dialog,
        `01b-import-empty-dialog-${brand.toLowerCase()}-crop.png`,
      )
      await captureSysNormW7LocatorScreenshot(
        page.locator('.shell-header .header-brand'),
        `01c-brand-header-${brand.toLowerCase()}-crop.png`,
      )
    })
  }

  test('ready dry-run report + gated Import enabled — REDBC', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W7_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    const dialog = await openImportDialog(page)
    await dialog.locator('input[type="file"]').setInputFiles(readyZipPath)
    await expect(dialog.getByText(staging.externalId)).toBeVisible()
    await ensureDemoMasterSelected(page, dialog)

    await dialog.getByRole('button', { name: /check dependencies/i }).click()
    await expect(dialog.getByRole('heading', { name: /dependency report/i })).toBeVisible({
      timeout: 60_000,
    })
    await expect(dialog.getByText(/ready to import/i)).toBeVisible()
    await expect(dialog.locator('.dependency-report')).toBeVisible()
    await assertEnabledPrimaryUsesBrand(dialog)
    await assertImportFooterInViewport(dialog, page)
    await assertNoViewportOverflow(page)

    await captureSysNormW7Screenshot(page, '02-import-ready-report-redbc-1440x900.png')
    await captureSysNormW7LocatorScreenshot(dialog, '02b-import-ready-report-dialog-redbc-crop.png')
  })

  test('ready dry-run report — GREENBC', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W7_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'GREENBC')
    await assertPrimaryBrandColor(page, 'GREENBC')

    const dialog = await openImportDialog(page)
    await dialog.locator('input[type="file"]').setInputFiles(readyZipPath)
    await expect(dialog.getByText(staging.externalId)).toBeVisible()
    await ensureDemoMasterSelected(page, dialog)

    await dialog.getByRole('button', { name: /check dependencies/i }).click()
    await expect(dialog.getByText(/ready to import/i)).toBeVisible({ timeout: 60_000 })
    await assertEnabledPrimaryUsesBrand(dialog)
    await assertImportFooterInViewport(dialog, page)

    await captureSysNormW7Screenshot(page, '03-import-ready-report-greenbc-1440x900.png')
    await captureSysNormW7LocatorScreenshot(dialog, '03b-import-ready-report-dialog-greenbc-crop.png')
  })

  test('blocking dry-run keeps Import disabled — REDBC', async ({ page }) => {
    await page.setViewportSize(SYS_NORM_W7_VIEWPORT)
    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    const dialog = await openImportDialog(page)
    await dialog.locator('input[type="file"]').setInputFiles(blockingZipPath)
    await expect(dialog.getByText(staging.externalId)).toBeVisible()
    await ensureDemoMasterSelected(page, dialog)

    await dialog.getByRole('button', { name: /check dependencies/i }).click()
    await expect(dialog.getByText(/not ready to import/i)).toBeVisible({ timeout: 60_000 })
    await expect(dialog.getByRole('button', { name: /^import template$/i })).toBeDisabled()
    await assertImportFooterInViewport(dialog, page)
    await assertNoViewportOverflow(page)

    await captureSysNormW7Screenshot(page, '04-import-blocking-report-redbc-1440x900.png')
    await captureSysNormW7LocatorScreenshot(
      dialog,
      '04b-import-blocking-report-dialog-redbc-crop.png',
    )
  })
})
