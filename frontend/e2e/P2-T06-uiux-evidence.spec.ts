import fs from 'node:fs'

import { expect, test } from '@playwright/test'

import {
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  loginAs,
} from './helpers/auth'
import {
  DEMO_SEED_DOCX_FILENAME,
  E2E_API_BASE_URL,
  prepareDemoMasterWithReplaceHistory,
  REPLACEMENT_DOCX_PATH,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'
import {
  captureP2T06Screenshot,
  ensureP2T06EvidenceDirs,
  P2_T06_VIEWPORT,
  switchBrand,
  switchLocale,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)

async function openMasterHub(page: import('@playwright/test').Page, hubPath: string) {
  await page.goto(hubPath)
  await expect(page.locator('.master-package-hub-page')).toBeVisible()
  await expect(page.getByText(/unable to load letterheads|无法加载母版/i)).not.toBeVisible()
  await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible()
  await expect(page.locator('.revision-lines-card')).toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
}

async function assertHubRevisionLinesTable(page: import('@playwright/test').Page) {
  const revisionLinesCard = page.locator('.revision-lines-card')
  await expect(revisionLinesCard).toBeVisible()
  await expect(revisionLinesCard.locator('.el-table__header')).toBeVisible()
  await expect(
    revisionLinesCard.getByRole('columnheader', { name: /^line$|修订线/i }),
  ).toBeVisible()
  await expect(revisionLinesCard.locator('.el-table__body-wrapper tbody tr').first()).toBeVisible()
  expect(await revisionLinesCard.locator('.el-table__body-wrapper tbody tr').count()).toBeGreaterThanOrEqual(2)
}

async function assertHistoricalRevisionDetail(page: import('@playwright/test').Page) {
  await expect(page.locator('.master-revision-detail-page')).toBeVisible()
  await expect(page.locator('.historical-hint')).toBeVisible()
  await expect(page.locator('.header-actions').getByText(/^historical$|历史/i)).toBeVisible()
  await expect(page.locator('.meta')).toContainText(DEMO_SEED_DOCX_FILENAME)
  await expect(
    page.locator('.header-actions').getByRole('button', { name: /submit for review|提交审核/i }),
  ).toHaveCount(0)
}

test.describe('P2-T06 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let hubPath = ''
  let historicalRevisionPath = ''

  test.beforeAll(async ({ request }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    ensureP2T06EvidenceDirs()

    let backendReady = false
    let frontendReady = false
    try {
      backendReady = (await request.get('http://127.0.0.1:8080/healthz', { timeout: 5_000 })).ok()
    } catch {
      backendReady = false
    }
    try {
      frontendReady = (await request.get(FRONTEND_BASE_URL, { timeout: 5_000 })).ok()
    } catch {
      frontendReady = false
    }
    test.skip(
      !(backendReady && frontendReady),
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )

    const seeded = await prepareDemoMasterWithReplaceHistory(request)
    hubPath = seeded.hubPath
    historicalRevisionPath = seeded.historicalRevisionPath
  })

  test.afterAll(async ({ request }) => {
    if (hasReplacementFixture) {
      await restoreDemoMasterToApproved(request)
    }
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P2_T06_VIEWPORT)
  })

  test('capture multi-revision hub, historical detail, dual-brand, and zh-CN evidence', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)

    await openMasterHub(page, hubPath)
    await assertHubRevisionLinesTable(page)
    await captureP2T06Screenshot(page, '01-master-hub-revision-lines-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await assertHubRevisionLinesTable(page)
    await captureP2T06Screenshot(page, '02-master-hub-revision-lines-greenbc-1440x900.png')

    await switchBrand(page, 'REDBC')
    await page.goto(historicalRevisionPath)
    await assertHistoricalRevisionDetail(page)
    await captureP2T06Screenshot(page, '03-historical-revision-detail-redbc-1440x900.png')

    await switchBrand(page, 'GREENBC')
    await assertHistoricalRevisionDetail(page)
    await captureP2T06Screenshot(page, '04-historical-revision-detail-greenbc-1440x900.png')

    await switchLocale(page, 'zh-CN')
    await openMasterHub(page, hubPath)
    await assertHubRevisionLinesTable(page)
    await expect(page.locator('.revision-lines-card .card-header span')).toContainText(/修订线/)
    await captureP2T06Screenshot(page, '05-master-hub-revision-lines-zhcn-1440x900.png')

    await page.goto(historicalRevisionPath)
    await assertHistoricalRevisionDetail(page)
    await expect(page.getByText(/此为历史修订线/)).toBeVisible()
    await captureP2T06Screenshot(page, '06-historical-revision-detail-zhcn-1440x900.png')
  })
})
