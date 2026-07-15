import fs from 'node:fs'

import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  E2E_MASTER_DESIGNER,
  loginAs,
} from './helpers/auth'
import {
  E2E_API_BASE_URL,
  REPLACEMENT_DOCX_FILENAME,
  REPLACEMENT_DOCX_PATH,
  assertDemoCatalogSeeded,
  demoMasterDetailPath,
  findMasterByName,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'
import { reLoginAs } from './helpers/ui'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)

const CHANGE_SUMMARY = 'CDP-E2E-T06 master designer replace and submit for review'

async function openLetterheadReviewTab(page: Page) {
  await page.getByRole('tab', { name: /letterhead review/i }).click()
  await expect(page).toHaveURL(/workspaceTab=approval/)
}

function workspacePrimaryAction(page: Page, name: RegExp) {
  return page.locator('.workspace-tab-shell__actions').getByRole('button', { name })
}

test.describe('CDP-E2E-T06 Master designer upload-to-approve (BDD-CDP-MASTER-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 360_000 })

  let hubPath = ''
  let revisionDetailPath = ''

  test.beforeAll(async ({ request }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

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

  test('BDD-CDP-MASTER-001 — designer replace → anchor check → submit; group admin Approves', async ({
    page,
    request,
  }) => {
    // --- MASTER_DESIGNER: replace DOCX via UI, verify anchors, submit review ---
    await loginAs(page, E2E_MASTER_DESIGNER)
    await page.goto(hubPath)
    await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)

    await expect(page.getByText(/^approved$/i).first()).toBeVisible()
    await page.getByRole('button', { name: /update letterhead docx/i }).click()
    const replaceDialog = page.locator('.el-dialog').filter({ hasText: /update letterhead docx/i })
    await expect(replaceDialog).toBeVisible()

    await replaceDialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await replaceDialog.getByRole('button', { name: /^continue$/i }).click()
    await expect(replaceDialog.getByTestId('master-replace-impact-confirm')).toBeVisible()
    await replaceDialog.getByRole('button', { name: /confirm replace|确认替换/i }).click()

    await expect(
      page.locator('.el-message').getByText(/letterhead file replaced|master file replaced/i),
    ).toBeVisible({ timeout: 60_000 })
    await expect(page).toHaveURL(/\/masters\/[^/]+\/revisions\/[^/?]+/, { timeout: 30_000 })
    revisionDetailPath = new URL(page.url()).pathname

    await expect(page.getByRole('heading', { level: 1, name: /revision/i })).toBeVisible()
    await expect(page.getByText(/^draft$/i).first()).toBeVisible()
    await expect(page.getByText(REPLACEMENT_DOCX_FILENAME)).toBeVisible()

    // Anchor / layout-placeholder catalog on design tab (default)
    await expect(page.getByText(/^anchor catalog$/i)).toBeVisible()
    await expect(
      page.locator('.detail-grid .el-table__body').getByRole('cell', { name: 'HEADER' }).first(),
    ).toBeVisible()
    await expect(page.locator('.workflow-banner').getByText(/submit letterhead for review/i)).toBeVisible()

    await openLetterheadReviewTab(page)
    await workspacePrimaryAction(page, /submit for review/i).click()
    const submitDialog = page.locator('.el-dialog').filter({ hasText: /submit letterhead for review/i })
    await expect(submitDialog).toBeVisible()
    await submitDialog.locator('textarea').fill(CHANGE_SUMMARY)
    await submitDialog.getByRole('button', { name: /^submit$/i }).click()

    await expect(
      page.locator('.el-message').getByText(/letterhead submitted for review/i),
    ).toBeVisible()
    await expect(page.getByText(/^pending review$/i).first()).toBeVisible()

    // --- GROUP_ADMIN: Approve in browser (no API skip) ---
    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await page.goto(revisionDetailPath)
    await expect(page.getByRole('heading', { level: 1, name: /revision/i })).toBeVisible()
    await expect(page.getByText(/^pending review$/i).first()).toBeVisible()

    await openLetterheadReviewTab(page)
    await workspacePrimaryAction(page, /^approve$/i).click()
    const approveDialog = page.locator('.el-dialog').filter({ hasText: /approve letterhead/i })
    await expect(approveDialog).toBeVisible()
    await approveDialog.locator('textarea').fill('CDP-E2E-T06 group admin approval')
    await approveDialog.getByRole('button', { name: /^approve$/i }).click()

    await expect(page.locator('.el-message').getByText(/letterhead approved/i)).toBeVisible()
    await expect(page.getByText(/^approved$/i).first()).toBeVisible()

    // Eligible for binding: master package status APPROVED
    const loginResponse = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
      data: E2E_GROUP_ADMIN,
    })
    expect(loginResponse.ok()).toBeTruthy()
    const token = ((await loginResponse.json()) as { result: { accessToken: string } }).result
      .accessToken
    const master = await findMasterByName(request, token, DEMO_MASTER_NAME)
    expect(master?.status).toBe('APPROVED')
  })
})
