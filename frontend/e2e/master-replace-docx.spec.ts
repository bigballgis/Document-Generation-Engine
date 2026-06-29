import fs from 'node:fs'

import { expect, test } from '@playwright/test'

import {
  DEMO_MASTER_NAME,
  E2E_GROUP_ADMIN,
  loginAs,
} from './helpers/auth'
import {
  REPLACEMENT_DOCX_FILENAME,
  REPLACEMENT_DOCX_PATH,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'

const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)

test.describe('master DOCX replacement (demo retail letterhead)', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeAll(async ({ request }) => {
    await restoreDemoMasterToApproved(request)
  })

  test.afterEach(async ({ request }) => {
    await restoreDemoMasterToApproved(request)
  })

  test('group admin replaces approved demo master and returns workflow to draft', async ({
    page,
  }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/masters')

    await expect(page.getByText(/unable to load master documents/i)).not.toBeVisible()
    await expect(page.getByRole('heading', { name: /^masters$/i })).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.getByText(DEMO_MASTER_NAME)).toBeVisible()

    await page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: DEMO_MASTER_NAME }).click()
    await expect(page).toHaveURL(/\/masters\/[^/?]+$/)

    await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible()
    await expect(page.locator('.header-actions').getByText(/^approved$/i)).toBeVisible()

    await page.getByRole('button', { name: /update master docx/i }).click()
    await expect(page.locator('.el-dialog').getByText(/update master docx/i)).toBeVisible()

    await page.locator('.el-dialog input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await page.getByRole('button', { name: /^replace file$/i }).click()

    await expect(page.locator('.el-message').getByText(/master file replaced/i)).toBeVisible()
    await expect(page).toHaveURL(/\/masters\/[^/]+\/revisions\/[^/?]+$/)
    await expect(page.locator('.meta')).toContainText(REPLACEMENT_DOCX_FILENAME)
    await expect(page.locator('.header-actions').getByText(/^draft$/i)).toBeVisible()

    await expect(
      page.locator('.detail-grid .el-table__body').getByRole('cell', { name: 'HEADER' }).first(),
    ).toBeVisible()
    await expect(page.locator('.workflow-banner').getByText(/submit master for review/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /submit for review/i })).toBeVisible()
  })
})
