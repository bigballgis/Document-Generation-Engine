/**
 * CE-U06 UIUX evidence — MasterAnchorPositionOverview (document-order + label edit)
 * Dual-brand REDBC/GREENBC @1440×900 (Stage 7; desktop-first).
 * BDD: docs/behavior/ce-u06-master-anchor-context.md (MAC-001…007 surfaces)
 */
import { expect, test, type Page } from '@playwright/test'

import { E2E_MASTER_DESIGNER, loginAs } from './helpers/auth'
import {
  createDraftMasterForHubSubmit,
  createPendingReviewMasterForDecide,
  E2E_API_BASE_URL,
  prepareDemoMasterWithReplaceHistory,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  captureCeU06LocatorScreenshot,
  captureCeU06Screenshot,
  CE_U06_VIEWPORT,
  ensureCeU06EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

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

async function openRevisionDesignWorkspace(page: Page, revisionPath: string): Promise<void> {
  await page.goto(revisionPath)
  await dismissOnboardingTourIfPresent(page)
  await expect(page.locator('.el-skeleton')).toHaveCount(0, { timeout: 30_000 })

  const designTab = page.getByRole('tab', { name: /letterhead design|信头设计/i })
  await expect(designTab).toBeVisible({ timeout: 30_000 })
  await designTab.click()

  await expect(page.getByTestId('master-anchor-position-overview')).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.getByText(/DOCX overview — anchor positions|DOCX 概览/i)).toBeVisible()
}

test.describe('CE-U06 master anchor context UIUX evidence @1440 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeU06EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
  })

  test('01–04 writable draft: ordered table, selection, edit dialog (REDBC/GREENBC)', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U06_VIEWPORT)
    const fixture = await createDraftMasterForHubSubmit(request, {
      name: `E2E-CE-U06-UIUX-Draft ${Date.now()}`,
    })

    await loginAs(page, E2E_MASTER_DESIGNER)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openRevisionDesignWorkspace(page, fixture.currentRevisionPath)

    const overview = page.getByTestId('master-anchor-position-overview')
    const table = page.getByTestId('master-anchor-position-list')
    await expect(table).toBeVisible()
    await expect(overview.getByText(/document-order anchor list|按文档顺序/i)).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /position|位置/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /anchor id|锚点/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /display label|显示标签/i })).toBeVisible()

    const editButtons = page.locator('[data-testid^="master-anchor-edit-label-"]')
    await expect(editButtons.first()).toBeVisible()
    await expect(editButtons.first()).toHaveText(/edit label|编辑标签/i)

    const firstRow = table.locator('.el-table__body-wrapper tbody tr').first()
    await firstRow.click()
    await expect(firstRow).toHaveClass(/master-anchor-row--selected/)

    await assertNoViewportOverflow(page)
    await captureCeU06Screenshot(page, '01-anchor-overview-writable-redbc-1440x900.png')
    await captureCeU06LocatorScreenshot(overview, '01b-anchor-overview-crop-redbc-1440x900.png')
    await captureCeU06LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )

    // Capture current label from first row before first open (MAC-003 seed check)
    const firstRowLabelCell = firstRow.locator('td').nth(2)
    const expectedLabel = (await firstRowLabelCell.innerText()).trim()
    expect(expectedLabel.length, 'row display label must be non-empty for seed check').toBeGreaterThan(0)

    await editButtons.first().click()
    const dialog = page.getByTestId('master-anchor-display-label-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByText(/edit display label|编辑显示标签/i)).toBeVisible()
    const labelInput = page.getByTestId('master-anchor-display-label-input')
    await expect(labelInput).toBeVisible()
    // BDD-CE-U06-MAC-003: first open must seed current displayLabel (prior Critical)
    await expect(labelInput).toHaveValue(expectedLabel)
    await expect(page.getByTestId('master-anchor-display-label-error')).toHaveCount(0)
    await expect(page.getByTestId('master-anchor-display-label-save')).toBeEnabled()
    await captureCeU06Screenshot(page, '02-edit-display-label-dialog-redbc-1440x900.png')
    await captureCeU06LocatorScreenshot(dialog, '02b-edit-dialog-crop-redbc-1440x900.png')
    await dialog.getByRole('button', { name: /^cancel$|^取消$/i }).click()
    await expect(dialog).toHaveCount(0)

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(overview).toBeVisible()
    await expect(editButtons.first()).toBeVisible()
    await assertNoViewportOverflow(page)
    await captureCeU06Screenshot(page, '03-anchor-overview-writable-greenbc-1440x900.png')
    await captureCeU06LocatorScreenshot(overview, '03b-anchor-overview-crop-greenbc-1440x900.png')
    await captureCeU06LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03c-brand-header-greenbc-crop.png',
    )

    await editButtons.first().click()
    await expect(dialog).toBeVisible()
    await captureCeU06Screenshot(page, '04-edit-display-label-dialog-greenbc-1440x900.png')
    await captureCeU06LocatorScreenshot(dialog, '04b-edit-dialog-crop-greenbc-1440x900.png')
    await dialog.getByRole('button', { name: /^cancel$|^取消$/i }).click()
  })

  test('05–06 PENDING_REVIEW + historical: edit controls hidden (REDBC)', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U06_VIEWPORT)

    const pending = await createPendingReviewMasterForDecide(request, {
      name: `E2E-CE-U06-UIUX-Pending ${Date.now()}`,
    })

    await loginAs(page, E2E_MASTER_DESIGNER)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')

    await openRevisionDesignWorkspace(page, pending.currentRevisionPath)
    await expect(page.getByTestId('master-anchor-position-list')).toBeVisible()
    await expect(page.locator('[data-testid^="master-anchor-edit-label-"]')).toHaveCount(0)
    await assertNoViewportOverflow(page)
    await captureCeU06Screenshot(page, '05-anchor-overview-pending-review-readonly-redbc-1440x900.png')
    await captureCeU06LocatorScreenshot(
      page.getByTestId('master-anchor-position-overview'),
      '05b-pending-review-overview-crop-redbc-1440x900.png',
    )

    const history = await prepareDemoMasterWithReplaceHistory(request)
    await openRevisionDesignWorkspace(page, history.historicalRevisionPath)
    await expect(page.locator('.historical-hint')).toBeVisible()
    await expect(page.getByTestId('master-anchor-position-overview')).toBeVisible()
    await expect(page.locator('[data-testid^="master-anchor-edit-label-"]')).toHaveCount(0)
    await assertNoViewportOverflow(page)
    await captureCeU06Screenshot(page, '06-anchor-overview-historical-readonly-redbc-1440x900.png')
    await captureCeU06LocatorScreenshot(
      page.getByTestId('master-anchor-position-overview'),
      '06b-historical-overview-crop-redbc-1440x900.png',
    )

    await restoreDemoMasterToApproved(request)
  })
})
