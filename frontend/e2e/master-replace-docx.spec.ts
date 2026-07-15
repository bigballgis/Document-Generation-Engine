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

    await expect(page.getByText(/unable to load letterheads/i)).not.toBeVisible()
    await expect(
      page.getByRole('heading', { name: /^(masters|letterhead templates)$/i }),
    ).toBeVisible()
    await expect(page.locator('.el-skeleton')).toHaveCount(0)
    await expect(page.getByText(DEMO_MASTER_NAME)).toBeVisible()

    await page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: DEMO_MASTER_NAME }).click()
    await expect(page).toHaveURL(/\/masters\/[^/?]+$/)

    await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible()
    await expect(page.getByText(/^approved$/i).first()).toBeVisible()
    await expect(page.getByRole('button', { name: /update (master|letterhead) docx/i })).toBeVisible()

    await page.getByRole('button', { name: /update (master|letterhead) docx/i }).click()
    const dialog = page.getByTestId('master-replace-file-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByText(/update (master|letterhead) docx/i)).toBeVisible()

    await dialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await dialog.getByTestId('master-replace-continue').click()
    await expect(dialog.getByTestId('master-replace-impact-confirm')).toBeVisible()
    await dialog.getByTestId('master-replace-confirm').click()

    await expect(
      page.locator('.el-message').getByText(/(master|letterhead) file replaced/i),
    ).toBeVisible()
    await expect(page).toHaveURL(/\/masters\/[^/]+\/revisions\/[^/?]+$/)
    await expect(page.getByText(REPLACEMENT_DOCX_FILENAME)).toBeVisible()
    await expect(page.getByText(/^draft$/i).first()).toBeVisible()

    await expect(
      page.locator('.detail-grid .el-table__body, .el-table__body').getByRole('cell', { name: 'HEADER' }).first(),
    ).toBeVisible()
    await expect(page.getByText(/submit (master|letterhead) for review/i).first()).toBeVisible()
    await expect(page.getByRole('button', { name: /submit for review/i })).toBeVisible()
  })
})
