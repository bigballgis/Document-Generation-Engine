/**
 * CE-K05 UIUX evidence — master hub impact links, replace Confirm, revision diff
 * REDBC primary @1440×900 (Stage 7). GREENBC optional note if slow.
 * BDD: docs/behavior/ce-k05-master-impact-real.md (MIR-008/009 + revision compare)
 */
import fs from 'node:fs'

import { expect, test, type Page } from '@playwright/test'

import { E2E_MASTER_DESIGNER, loginAs } from './helpers/auth'
import {
  E2E_API_BASE_URL,
  REPLACEMENT_DOCX_PATH,
  assertDemoCatalogSeeded,
  demoMasterDetailPath,
} from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  CE_K05_VIEWPORT,
  captureCeK05LocatorScreenshot,
  captureCeK05Screenshot,
  ensureCeK05EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
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

test.describe('CE-K05 master impact real UIUX evidence @1440 REDBC', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let hubPath = ''

  test.beforeAll(async ({ request }) => {
    ensureCeK05EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)
    hubPath = await demoMasterDetailPath(request)
  })

  test('01–04 REDBC: impact panel + replace confirm + revision diff if reachable', async ({
    page,
  }) => {
    await page.setViewportSize(CE_K05_VIEWPORT)

    await loginAs(page, E2E_MASTER_DESIGNER)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await page.goto(hubPath)
    await expect(page.getByRole('heading', { name: /demo retail letterhead/i })).toBeVisible({
      timeout: 30_000,
    })

    const panel = page.getByTestId('master-impact-panel')
    await expect(panel).toBeVisible()
    const empty = panel.getByTestId('master-impact-empty')
    const list = panel.getByTestId('master-impact-template-list')
    await expect(empty.or(list)).toBeVisible({ timeout: 30_000 })

    await assertNoViewportOverflow(page)
    await captureCeK05Screenshot(page, '01-master-hub-impact-panel-redbc-1440x900.png')
    await captureCeK05LocatorScreenshot(panel, '01b-impact-panel-crop-redbc-1440x900.png')
    await captureCeK05LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )

    if ((await list.count()) > 0) {
      const firstLink = panel.getByTestId('master-impact-template-link').first()
      await expect(firstLink).toBeVisible()
      const linkText = (await firstLink.innerText()).trim()
      expect(linkText.length, 'impact link must show a human name, not empty').toBeGreaterThan(0)
      expect(linkText, 'impact link must not be a raw UUID').not.toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
      )
      await captureCeK05LocatorScreenshot(
        firstLink,
        '01d-impact-template-link-crop-redbc.png',
      )
    }

    if (hasReplacementFixture) {
      await page.getByRole('button', { name: /update letterhead docx/i }).click()
      const dialog = page.getByTestId('master-replace-file-dialog')
      await expect(dialog).toBeVisible()
      await dialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
      await dialog.getByTestId('master-replace-continue').click()
      const confirm = dialog.getByTestId('master-replace-impact-confirm')
      await expect(confirm).toBeVisible({ timeout: 30_000 })
      await expect(dialog.getByTestId('master-replace-confirm')).toBeVisible()
      await assertNoViewportOverflow(page)
      await captureCeK05Screenshot(page, '02-replace-confirm-dialog-redbc-1440x900.png')
      await captureCeK05LocatorScreenshot(dialog, '02b-replace-confirm-crop-redbc-1440x900.png')

      await dialog.getByTestId('master-replace-cancel').click()
      await expect(dialog).toBeHidden()
    }

    const compareBtn = page.getByTestId('master-revision-compare')
    if (await compareBtn.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await compareBtn.click()
      const diffDialog = page.getByTestId('master-revision-diff-dialog')
      await expect(diffDialog).toBeVisible({ timeout: 30_000 })
      await expect(
        diffDialog
          .getByTestId('master-revision-baseline-hash')
          .or(diffDialog.locator('.diff-error')),
      ).toBeVisible({ timeout: 30_000 })
      await captureCeK05Screenshot(page, '03-revision-diff-dialog-redbc-1440x900.png')
      await captureCeK05LocatorScreenshot(
        diffDialog,
        '03b-revision-diff-crop-redbc-1440x900.png',
      )
      await page.keyboard.press('Escape')
      await expect(diffDialog).toBeHidden({ timeout: 10_000 })
    } else {
      await captureCeK05Screenshot(page, '03-revision-compare-unavailable-redbc-1440x900.png')
    }

    // Fast GREENBC brand spot-check (header + impact crop only).
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await captureCeK05LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04-brand-header-greenbc-crop.png',
    )
    await captureCeK05LocatorScreenshot(
      page.getByTestId('master-impact-panel'),
      '04b-impact-panel-crop-greenbc-1440x900.png',
    )
  })
})
