import fs from 'node:fs'

import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_MASTER_DESIGNER, loginAs } from './helpers/auth'
import {
  E2E_API_BASE_URL,
  REPLACEMENT_DOCX_PATH,
  assertDemoCatalogSeeded,
  demoMasterDetailPath,
} from './helpers/masters-api'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)

test.describe('CE-K05 master impact real (MIR-008/009)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let hubPath = ''

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    hubPath = await demoMasterDetailPath(request)
  })

  test('MIR-009 — impact panel empty vs name links stay honest', async ({ page }) => {
    await loginAs(page, E2E_MASTER_DESIGNER)
    await page.goto(hubPath)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

    const panel = page.getByTestId('master-impact-panel')
    await expect(panel).toBeVisible()
    const empty = panel.getByTestId('master-impact-empty')
    const list = panel.getByTestId('master-impact-template-list')
    if ((await list.count()) > 0) {
      await expect(empty).toHaveCount(0)
      await expect(panel.getByTestId('master-impact-template-link').first()).toBeVisible()
    } else {
      await expect(empty).toBeVisible()
    }
  })

  test('MIR-008 — replace confirm shows impact and cancel does not replace', async ({ page }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    await loginAs(page, E2E_MASTER_DESIGNER)
    await page.goto(hubPath)
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
    const beforeUrl = page.url()

    await page.getByRole('button', { name: /update letterhead docx/i }).click()
    const dialog = page.getByTestId('master-replace-file-dialog')
    await expect(dialog).toBeVisible()
    await dialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await dialog.getByRole('button', { name: /^continue$/i }).click()
    await expect(dialog.getByTestId('master-replace-impact-confirm')).toBeVisible()
    await dialog.getByTestId('master-replace-cancel').click()
    await expect(dialog).toBeHidden()
    await expect(page).toHaveURL(beforeUrl)
    await expect(
      page.locator('.el-message').getByText(/letterhead file replaced|master file replaced/i),
    ).toHaveCount(0)
  })
})
