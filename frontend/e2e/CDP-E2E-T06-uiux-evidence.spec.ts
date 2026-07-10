import fs from 'node:fs'

import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  E2E_MASTER_DESIGNER,
  loginAs,
} from './helpers/auth'
import {
  E2E_API_BASE_URL,
  REPLACEMENT_DOCX_PATH,
  assertDemoCatalogSeeded,
  demoMasterDetailPath,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'
import { reLoginAs } from './helpers/ui'
import {
  captureCdpE2eDecisionScreenshot,
  CDP_E2E_CD2_DECISION_VIEWPORT,
  ensureCdpE2eDecisionEvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const TASK_ID = 'CDP-E2E-T06' as const
const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)
const CHANGE_SUMMARY = 'CDP-E2E-T06 UIUX evidence submit summary'

test.describe('CDP-E2E-T06 UIUX evidence — master designer lifecycle @1920 (BDD-CDP-MASTER-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let hubPath = ''
  let revisionDetailPath = ''

  test.beforeAll(async ({ request }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    ensureCdpE2eDecisionEvidenceDirs(TASK_ID)
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    await restoreDemoMasterToApproved(request, { force: true })
    hubPath = await demoMasterDetailPath(request)
  })

  test.afterAll(async ({ request }) => {
    if (hasReplacementFixture) {
      await restoreDemoMasterToApproved(request)
    }
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(CDP_E2E_CD2_DECISION_VIEWPORT)
  })

  test('capture draft anchors + submit + pending approve dual-brand (REDBC + GREENBC)', async ({
    page,
  }) => {
    await loginAs(page, E2E_MASTER_DESIGNER)
    await switchBrand(page, 'REDBC')
    await page.goto(hubPath)
    await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)

    await page.getByRole('button', { name: /update letterhead docx/i }).click()
    const replaceDialog = page.locator('.el-dialog').filter({ hasText: /update letterhead docx/i })
    await replaceDialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await replaceDialog.getByRole('button', { name: /^replace file$/i }).click()
    await expect(page).toHaveURL(/\/masters\/[^/]+\/revisions\/[^/?]+/, { timeout: 60_000 })
    revisionDetailPath = new URL(page.url()).pathname

    await expect(page.getByText(/^anchor catalog$/i)).toBeVisible()
    await expect(
      page.locator('.detail-grid .el-table__body').getByRole('cell', { name: 'HEADER' }).first(),
    ).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '01-revision-draft-anchor-catalog-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.getByText(/^anchor catalog$/i)).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '02-revision-draft-anchor-catalog-greenbc-1920x1080.png',
    )

    await switchBrand(page, 'REDBC')
    await page.getByRole('tab', { name: /letterhead review/i }).click()
    await page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /submit for review/i }).click()
    const submitDialog = page.locator('.el-dialog').filter({ hasText: /submit letterhead for review/i })
    await expect(submitDialog).toBeVisible()
    await submitDialog.locator('textarea').fill(CHANGE_SUMMARY)
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '03-submit-review-dialog-redbc-1920x1080.png',
    )
    await submitDialog.getByRole('button', { name: /^submit$/i }).click()
    await expect(page.getByText(/^pending review$/i).first()).toBeVisible()

    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await switchBrand(page, 'REDBC')
    await page.goto(`${revisionDetailPath}?workspaceTab=approval`)
    await expect(page.getByText(/^pending review$/i).first()).toBeVisible()
    await expect(
      page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /^approve$/i }),
    ).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '04-pending-review-approve-rail-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(
      page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /^approve$/i }),
    ).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '05-pending-review-approve-rail-greenbc-1920x1080.png',
    )

    await switchBrand(page, 'REDBC')
    await page.locator('.workspace-tab-shell__actions').getByRole('button', { name: /^approve$/i }).click()
    const approveDialog = page.locator('.el-dialog').filter({ hasText: /approve letterhead/i })
    await expect(approveDialog).toBeVisible()
    await approveDialog.getByRole('button', { name: /^approve$/i }).click()
    await expect(page.getByText(/^approved$/i).first()).toBeVisible()
    await captureCdpE2eDecisionScreenshot(
      page,
      TASK_ID,
      '06-revision-approved-redbc-1920x1080.png',
    )
  })
})
