import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Locator, type Page } from '@playwright/test'

import { DEMO_MASTER_NAME, E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  demoMasterDetailPath,
  REPLACEMENT_DOCX_PATH,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'

/**
 * LR-A3 master DOCX upload validation UX (BDD-LRP-A3-UPLOAD-001 A4 / A7).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-A3-master-docx-upload-validation.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080 (stage 5 DEPLOY_OK).
 */

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'LRP-A3-upload-validation',
)

const CLIENT_MAX_BYTES = 50 * 1024 * 1024
const READABLE_TOO_LARGE =
  /The file exceeds the 50 MB upload limit\. Reduce the file size and try again\./i
const READABLE_DOCX_ONLY = /Only \.docx letterhead files are accepted\./i
const RAW_NGINX_HTML = /Request Entity Too Large|nginx\/|<html[\s>]/i
const REPLACE_SUCCESS = /letterhead file replaced|master file replaced/i

const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)

/**
 * Element Plus Upload reads `file.size` from the selected File. Inject a small
 * payload with a forged size so we do not ship a real 50MB+ artifact through CDP
 * (Playwright rejects in-memory buffers larger than 50MB).
 */
async function setForgedSizeDocx(dialog: Locator, sizeBytes: number, name: string): Promise<void> {
  const input = dialog.locator('input[type="file"]')
  await input.evaluate(
    (el, args) => {
      const inputEl = el as HTMLInputElement
      const file = new File([new Uint8Array(64)], args.name, {
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      })
      Object.defineProperty(file, 'size', { value: args.sizeBytes })
      const transfer = new DataTransfer()
      transfer.items.add(file)
      inputEl.files = transfer.files
      inputEl.dispatchEvent(new Event('change', { bubbles: true }))
    },
    { sizeBytes, name },
  )
}

function replaceDialog(page: Page): Locator {
  return page.locator('.el-dialog').filter({ hasText: /update letterhead docx/i })
}

async function openReplaceDialog(
  page: Page,
  hubPath: string,
): Promise<Locator> {
  await loginAs(page, E2E_GROUP_ADMIN)
  await page.goto(hubPath)

  await expect(page).toHaveURL(/\/masters\/[^/?]+$/)
  await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible({
    timeout: 15_000,
  })
  await expect(page.getByText(/unable to load letterhead/i)).not.toBeVisible()

  await page.getByRole('button', { name: /^update letterhead docx$/i }).click()
  const dialog = replaceDialog(page)
  await expect(dialog).toBeVisible()
  await expect(dialog.getByRole('button', { name: /^replace file$/i })).toBeVisible()
  return dialog
}

async function captureEvidence(page: Page, name: string): Promise<void> {
  fs.mkdirSync(EVIDENCE_DIR, { recursive: true })
  await page.screenshot({
    path: path.join(EVIDENCE_DIR, `${name}.png`),
    fullPage: true,
  })
}

test.describe('LR-A3 master DOCX upload validation (A4 / A7)', () => {
  test.describe.configure({ mode: 'serial' })

  let hubPath = ''

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL })
    await restoreDemoMasterToApproved(request)
    hubPath = await demoMasterDetailPath(request)
  })

  test.afterEach(async ({ request }) => {
    await restoreDemoMasterToApproved(request)
  })

  test('A7: replace dialog rejects oversized file with readable error and blocks submit', async ({
    page,
  }) => {
    const dialog = await openReplaceDialog(page, hubPath)

    await setForgedSizeDocx(dialog, CLIENT_MAX_BYTES + 1, 'huge.docx')

    await expect(dialog.locator('.upload-error')).toBeVisible()
    await expect(dialog.locator('.upload-error')).toHaveText(READABLE_TOO_LARGE)
    await expect(dialog.locator('.upload-error')).not.toHaveText(RAW_NGINX_HTML)
    await expect(dialog.getByRole('button', { name: /^replace file$/i })).toBeDisabled()

    await captureEvidence(page, 'A7-oversized-precheck')
  })

  test('A7: replace dialog rejects non-.docx with readable error and blocks submit', async ({
    page,
  }) => {
    const dialog = await openReplaceDialog(page, hubPath)

    await dialog.locator('input[type="file"]').setInputFiles({
      name: 'notes.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.4 e2e-fake'),
    })

    await expect(dialog.locator('.upload-error')).toBeVisible()
    await expect(dialog.locator('.upload-error')).toHaveText(READABLE_DOCX_ONLY)
    await expect(dialog.getByRole('button', { name: /^replace file$/i })).toBeDisabled()

    await captureEvidence(page, 'A7-non-docx-precheck')
  })

  test('A4: nginx-style HTML 413 maps to readable ElMessage (no raw HTML)', async ({ page }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    const nginxHtml =
      '<html><head><title>413 Request Entity Too Large</title></head>' +
      '<body>413 Request Entity Too Large<br/>nginx/1.25.3</body></html>'

    await page.route('**/api/management/v1/masters/*/file', async (route) => {
      if (route.request().method() !== 'PUT') {
        await route.continue()
        return
      }
      await route.fulfill({
        status: 413,
        contentType: 'text/html',
        body: nginxHtml,
      })
    })

    const dialog = await openReplaceDialog(page, hubPath)

    await dialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await expect(dialog.getByRole('button', { name: /^replace file$/i })).toBeEnabled()
    await dialog.getByRole('button', { name: /^replace file$/i }).click()

    const message = page.locator('.el-message').filter({ hasText: READABLE_TOO_LARGE })
    await expect(message).toBeVisible()
    await expect(message).not.toContainText(/Request Entity Too Large/i)
    await expect(message).not.toContainText(/nginx\//i)
    await expect(message).not.toContainText(/<html/i)
    await expect(page.getByText(REPLACE_SUCCESS)).not.toBeVisible()
    await expect(dialog).toBeVisible()

    await captureEvidence(page, 'A4-nginx-413-readable')
  })

  test('A4: Spring envelope docxTooLarge surfaces readable translated message', async ({
    page,
  }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    await page.route('**/api/management/v1/masters/*/file', async (route) => {
      if (route.request().method() !== 'PUT') {
        await route.continue()
        return
      }
      await route.fulfill({
        status: 413,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-lrp-a3-413', timestamp: new Date().toISOString() },
          result: null,
          error: {
            code: 'MASTER_VALIDATION_FAILED',
            category: 'VALIDATION',
            retryable: false,
            messageKey: 'api.error.master.docxTooLarge',
            message: 'The uploaded DOCX exceeds the maximum allowed size.',
          },
        }),
      })
    })

    const dialog = await openReplaceDialog(page, hubPath)

    await dialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await dialog.getByRole('button', { name: /^replace file$/i }).click()

    const message = page.locator('.el-message').filter({
      hasText: /The uploaded DOCX exceeds the maximum allowed size\./i,
    })
    await expect(message).toBeVisible()
    await expect(message).not.toContainText(RAW_NGINX_HTML)
    await expect(page.getByText(REPLACE_SUCCESS)).not.toBeVisible()

    await captureEvidence(page, 'A4-spring-envelope-413')
  })

  test('smoke: replace dialog accepts valid fixture under size limit (precheck pass)', async ({
    page,
  }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    const dialog = await openReplaceDialog(page, hubPath)

    await dialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)

    await expect(dialog.locator('.upload-error')).toHaveCount(0)
    await expect(dialog.getByRole('button', { name: /^replace file$/i })).toBeEnabled()
    await expect(
      dialog.locator('.el-upload-list__item-file-name').filter({
        hasText: /retail-letterhead-replacement\.docx/i,
      }).first(),
    ).toBeVisible()

    await captureEvidence(page, 'smoke-valid-precheck')
  })
})
