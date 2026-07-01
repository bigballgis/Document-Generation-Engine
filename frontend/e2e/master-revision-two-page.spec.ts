import fs from 'node:fs'

import { expect, test } from '@playwright/test'

import {
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  E2E_MASTER_DESIGNER,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import {
  DEMO_SEED_DOCX_FILENAME,
  REPLACEMENT_DOCX_FILENAME,
  REPLACEMENT_DOCX_PATH,
  demoMasterRevisionDetailPath,
  prepareDemoMasterWithReplaceHistory,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'

const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)

async function openDemoMasterHub(page: import('@playwright/test').Page) {
  await page.goto('/masters')

  await expect(page.getByText(/unable to load master documents/i)).not.toBeVisible()
  await expect(page.getByRole('heading', { name: /^masters$/i })).toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
  await expect(page.getByText(DEMO_MASTER_NAME)).toBeVisible()

  await page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: DEMO_MASTER_NAME }).click()
  await expect(page).toHaveURL(/\/masters\/[^/?]+$/)

  await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible()
  await expect(page.getByText(`Group: ${DEMO_GROUP_CODE}`)).toBeVisible()
}

async function assertHubRevisionLinesTable(page: import('@playwright/test').Page) {
  const revisionLinesCard = page.locator('.revision-lines-card')
  await expect(revisionLinesCard.getByText(/^revision lines$/i)).toBeVisible()
  await expect(revisionLinesCard.locator('.el-table__header')).toBeVisible()
  await expect(revisionLinesCard.getByRole('columnheader', { name: /^line$/i })).toBeVisible()
  await expect(revisionLinesCard.getByRole('columnheader', { name: /^status$/i })).toBeVisible()
  await expect(revisionLinesCard.getByRole('columnheader', { name: /^source file$/i })).toBeVisible()
  await expect(revisionLinesCard.getByRole('columnheader', { name: /^anchors$/i })).toBeVisible()
  await expect(revisionLinesCard.locator('.el-table__body-wrapper tbody tr').first()).toBeVisible()

  await expect(page.locator('.master-package-hub-page .detail-grid')).toHaveCount(0)
  await expect(page.locator('.master-package-hub-page .history-card')).toHaveCount(0)
  await expect(page.locator('.master-package-hub-page').getByText(/^anchor catalog$/i)).toHaveCount(0)
  await expect(page.locator('.master-package-hub-page').getByText(/^review history$/i)).toHaveCount(0)
}

async function openCurrentRevisionFromHub(page: import('@playwright/test').Page) {
  const revisionRow = page
    .locator('.revision-lines-card .el-table__body-wrapper tbody tr')
    .filter({ hasText: /current revision|current/i })
    .first()
  await revisionRow.click()
  await expect(page).toHaveURL(/\/masters\/[^/]+\/revisions\/[^/?]+$/)
}

async function assertRevisionDetailSections(page: import('@playwright/test').Page) {
  await expect(page.locator('.master-revision-detail-page')).toBeVisible()
  await expect(page.getByText(/^revision overview$/i)).toBeVisible()
  await expect(page.getByText(/^anchor catalog$/i)).toBeVisible()
  await expect(page.getByText(/^review history$/i)).toBeVisible()
  await expect(page.locator('.detail-grid .el-table__body-wrapper tbody tr').first()).toBeVisible()
}

function breadcrumb(page: import('@playwright/test').Page) {
  return page.locator('nav.app-breadcrumb')
}

function revisionLineRows(page: import('@playwright/test').Page) {
  return page.locator('.revision-lines-card .el-table__body-wrapper tbody tr')
}

function currentRevisionRow(page: import('@playwright/test').Page) {
  return revisionLineRows(page).filter({
    has: page.locator('.line-tag').filter({ hasText: /^current$/i }),
  })
}

function approvedHistoricalSeedRow(page: import('@playwright/test').Page) {
  return page.locator('.revision-lines-card').getByRole('row', {
    name: new RegExp(
      `historical.*approved.*${DEMO_SEED_DOCX_FILENAME.replace('.', '\\.')}`,
      'i',
    ),
  })
}

test.describe('master revision two-page UX', () => {
  test.describe.configure({ mode: 'serial' })

test.describe('Phase A', () => {

  test.beforeAll(async ({ request }) => {
    await restoreDemoMasterToApproved(request)
  })

  test('group admin navigates hub → revision detail → back and breadcrumbs', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await openDemoMasterHub(page)
    await assertHubRevisionLinesTable(page)

    await expect(breadcrumb(page)).toBeVisible()
    await expect(breadcrumb(page)).toContainText(/letterhead templates/i)
    await expect(breadcrumb(page)).toContainText(/package/i)

    await openCurrentRevisionFromHub(page)
    await assertRevisionDetailSections(page)

    await expect(breadcrumb(page)).toContainText(/revision line/i)

    await page.getByRole('button', { name: /back to master package/i }).click()
    await expect(page).toHaveURL(/\/masters\/[^/?]+$/)
    await assertHubRevisionLinesTable(page)

    await openCurrentRevisionFromHub(page)
    await breadcrumb(page).getByRole('button', { name: /^package$/i }).click()
    await expect(page).toHaveURL(/\/masters\/[^/?]+$/)
    await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible()

    await openCurrentRevisionFromHub(page)
    await breadcrumb(page).getByRole('button', { name: /^letterhead templates$/i }).click()
    await expect(page).toHaveURL(/\/masters\/?$/)
    await expect(page.getByRole('heading', { name: /^masters$/i })).toBeVisible()
  })

  test('global admin can open revision detail via view detail action', async ({ page }) => {
    await loginAsGlobalAdmin(page)
    await openDemoMasterHub(page)
    await assertHubRevisionLinesTable(page)

    await page.getByRole('button', { name: /^view detail$/i }).first().click()
    await expect(page).toHaveURL(/\/masters\/[^/]+\/revisions\/[^/?]+$/)
    await assertRevisionDetailSections(page)
  })

  test('master designer can browse revision lines and detail', async ({ page }) => {
    await loginAs(page, E2E_MASTER_DESIGNER)
    await openDemoMasterHub(page)
    await assertHubRevisionLinesTable(page)

    await page.getByRole('button', { name: /^view detail$/i }).first().click()
    await expect(page).toHaveURL(/\/masters\/[^/]+\/revisions\/[^/?]+$/)
    await assertRevisionDetailSections(page)
    await expect(page.getByText(/approved/i).first()).toBeVisible()
  })

  test('direct revision URL loads detail for authorized user', async ({ page, request }) => {
    const detailPath = await demoMasterRevisionDetailPath(request)

    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto(detailPath)
    await assertRevisionDetailSections(page)
  })
})

test.describe('Phase B — revision history', () => {

  let historicalRevisionPath = ''

  test.beforeAll(async ({ request }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    const seeded = await prepareDemoMasterWithReplaceHistory(request)
    historicalRevisionPath = seeded.historicalRevisionPath
  })

  test.afterAll(async ({ request }) => {
    if (hasReplacementFixture) {
      await restoreDemoMasterToApproved(request)
    }
  })

  test('hub revision lines table shows current and historical rows after replace', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await openDemoMasterHub(page)
    await assertHubRevisionLinesTable(page)

    const revisionRows = revisionLineRows(page)
    expect(await revisionRows.count()).toBeGreaterThanOrEqual(2)

    const currentRow = currentRevisionRow(page).first()
    await expect(currentRow).toBeVisible()
    await expect(currentRow).toContainText(REPLACEMENT_DOCX_FILENAME)

    const historicalRow = approvedHistoricalSeedRow(page).first()
    await expect(historicalRow).toBeVisible()
    await expect(historicalRow).toContainText(DEMO_SEED_DOCX_FILENAME)
  })

  test('click historical row navigates to historical revision detail', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await openDemoMasterHub(page)
    await assertHubRevisionLinesTable(page)

    const historicalRow = approvedHistoricalSeedRow(page).first()
    await historicalRow.click()

    await expect(page).toHaveURL(new RegExp(`${historicalRevisionPath.replace(/\//g, '\\/')}$`))
    await assertRevisionDetailSections(page)

    await expect(page.locator('.historical-hint')).toContainText(/historical revision line/i)
    await expect(page.locator('.header-actions').getByText(/^historical$/i)).toBeVisible()
    await expect(page.locator('.meta')).toContainText(DEMO_SEED_DOCX_FILENAME)
    await expect(page.locator('.header-actions').getByRole('button', { name: /submit for review/i })).toHaveCount(0)
  })

  test('download historical DOCX from revision detail', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto(historicalRevisionPath)
    await assertRevisionDetailSections(page)
    await expect(page.locator('.meta')).toContainText(DEMO_SEED_DOCX_FILENAME)

    const downloadPromise = page.waitForEvent('download')
    await page.getByRole('button', { name: /^download docx$/i }).click()
    const download = await downloadPromise

    await expect(page.locator('.el-message').getByText(/master file downloaded/i)).toBeVisible()

    const suggestedFilename = download.suggestedFilename()
    expect(suggestedFilename.toLowerCase()).toMatch(/\.docx$/)
    expect(suggestedFilename).toContain('demo-retail-letterhead')
  })
})
})
