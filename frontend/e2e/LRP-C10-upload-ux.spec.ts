import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Locator, type Page } from '@playwright/test'

import { DEMO_GROUP_CODE, DEMO_MASTER_NAME, E2E_GROUP_ADMIN, loginAs } from './helpers/auth'
import {
  demoMasterDetailPath,
  REPLACEMENT_DOCX_PATH,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { selectElementPlusOption } from './helpers/ui'

/**
 * LR-C10 — Master DOCX upload UX polish (progress, drag hint, inline errors).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C10-upload-ux.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080 (stage 5 DEPLOY_OK).
 * Rejection semantics remain LR-A3; this spec covers presentation only.
 */

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'LRP-C10-upload-ux',
)

const CLIENT_MAX_BYTES = 50 * 1024 * 1024
const READABLE_TOO_LARGE =
  /The file exceeds the 50 MB upload limit\. Reduce the file size and try again\./i
const READABLE_DOCX_ONLY = /Only \.docx letterhead files are accepted\./i
const DRAG_HINT = /Drop a \.docx file here, or click to choose/i
const LIMIT_HINT = /Maximum size:\s*50 MB/i
const RAW_ENVELOPE = /Request Entity Too Large|nginx\/|<html[\s>]|metadata|messageKey/i

const hasReplacementFixture = fs.existsSync(REPLACEMENT_DOCX_PATH)

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

function createDialog(page: Page): Locator {
  return page.locator('.el-dialog').filter({ hasText: /upload docx letterhead/i })
}

async function openReplaceDialog(page: Page, hubPath: string): Promise<Locator> {
  await loginAs(page, E2E_GROUP_ADMIN)
  await page.goto(hubPath)
  await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible({
    timeout: 15_000,
  })
  await page.getByRole('button', { name: /^update letterhead docx$/i }).click()
  const dialog = replaceDialog(page)
  await expect(dialog).toBeVisible()
  return dialog
}

async function openCreateDialog(page: Page): Promise<Locator> {
  await loginAs(page, E2E_GROUP_ADMIN)
  await page.goto('/masters')
  await expect(page.getByRole('heading', { name: /^letterhead templates$/i })).toBeVisible({
    timeout: 15_000,
  })
  await page.getByRole('button', { name: /new letterhead package/i }).click()
  const dialog = createDialog(page)
  await expect(dialog).toBeVisible()
  return dialog
}

async function captureEvidence(page: Page, name: string): Promise<void> {
  fs.mkdirSync(EVIDENCE_DIR, { recursive: true })
  await page.screenshot({
    path: path.join(EVIDENCE_DIR, `${name}.png`),
    fullPage: true,
  })
}

test.describe('LR-C10 master DOCX upload UX', () => {
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

  test('LR-C10-A: create dialog shows drag hint, 50 MB limit, and progress during upload', async ({
    page,
  }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    let releaseUpload: (() => void) | undefined
    const uploadGate = new Promise<void>((resolve) => {
      releaseUpload = resolve
    })

    await page.route('**/api/management/v1/masters', async (route) => {
      if (route.request().method() !== 'POST') {
        await route.continue()
        return
      }
      await uploadGate
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-lrp-c10-create' },
          result: {
            id: '00000000-0000-4000-8000-00000000c10a',
            groupCode: 'RETAIL',
            name: 'E2E LR-C10 create',
            description: null,
            status: 'DRAFT',
            originalFilename: 'retail-letterhead-replacement.docx',
            changeSummary: null,
            anchors: [],
            reviewHistory: [],
            createdAt: '2026-07-11T00:00:00Z',
            updatedAt: '2026-07-11T00:00:00Z',
          },
        }),
      })
    })

    const dialog = await openCreateDialog(page)
    await expect(dialog.getByText(DRAG_HINT)).toBeVisible()
    await expect(dialog.getByText(LIMIT_HINT)).toBeVisible()
    await expect(dialog.locator('.el-upload-dragger')).toBeVisible()

    // GROUP_ADMIN has multiple authorized groups — group is not auto-locked.
    await dialog.getByRole('combobox', { name: /group/i }).click()
    await selectElementPlusOption(page, new RegExp(`^${DEMO_GROUP_CODE}$`, 'i'))
    await dialog
      .locator('.el-form-item')
      .filter({ hasText: /letterhead name/i })
      .locator('input')
      .fill('E2E LR-C10 create')
    await dialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await expect(dialog.getByRole('button', { name: /^upload$/i })).toBeEnabled()
    await dialog.getByRole('button', { name: /^upload$/i }).click()

    await expect(dialog.getByTestId('master-upload-progress')).toBeVisible({ timeout: 10_000 })
    await captureEvidence(page, 'C10-A-create-progress')

    releaseUpload?.()
    await expect(page.getByText(/letterhead uploaded successfully/i)).toBeVisible({
      timeout: 20_000,
    })
  })

  test('LR-C10-A: replace dialog shows progress during transfer', async ({ page }) => {
    test.skip(
      !hasReplacementFixture,
      'Replacement DOCX fixture missing — run: mvn -f backend/pom.xml -Dtest=E2eDocxFixtureGeneratorTest test',
    )

    let releaseUpload: (() => void) | undefined
    const uploadGate = new Promise<void>((resolve) => {
      releaseUpload = resolve
    })

    await page.route('**/api/management/v1/masters/*/file', async (route) => {
      if (route.request().method() !== 'PUT') {
        await route.continue()
        return
      }
      await uploadGate
      await route.continue()
    })

    const dialog = await openReplaceDialog(page, hubPath)
    await expect(dialog.getByText(DRAG_HINT)).toBeVisible()
    await expect(dialog.getByText(LIMIT_HINT)).toBeVisible()

    await dialog.locator('input[type="file"]').setInputFiles(REPLACEMENT_DOCX_PATH)
    await dialog.getByRole('button', { name: /^continue$/i }).click()
    await dialog.getByRole('button', { name: /confirm replace|确认替换/i }).click()

    await expect(dialog.getByTestId('master-upload-progress')).toBeVisible({ timeout: 10_000 })
    await captureEvidence(page, 'C10-A-replace-progress')

    releaseUpload?.()
  })

  test('LR-C10-B: oversized / non-docx show inline translated errors; retry clears', async ({
    page,
  }) => {
    const dialog = await openReplaceDialog(page, hubPath)

    await setForgedSizeDocx(dialog, CLIENT_MAX_BYTES + 1, 'huge.docx')
    await expect(dialog.locator('.upload-error')).toBeVisible()
    await expect(dialog.locator('.upload-error')).toHaveText(READABLE_TOO_LARGE)
    await expect(dialog.locator('.upload-error')).not.toHaveText(RAW_ENVELOPE)
    await expect(dialog.getByRole('button', { name: /^continue$/i })).toBeDisabled()

    await dialog.locator('input[type="file"]').setInputFiles({
      name: 'notes.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.4 e2e-fake'),
    })
    await expect(dialog.locator('.upload-error')).toHaveText(READABLE_DOCX_ONLY)
    await expect(dialog.getByRole('button', { name: /^continue$/i })).toBeDisabled()

    await captureEvidence(page, 'C10-B-client-precheck')
  })

  test('LR-C10-B: server docxTooLarge shows inline translated error (no raw envelope)', async ({
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
          metadata: { traceId: 'e2e-lrp-c10-413', timestamp: new Date().toISOString() },
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
    await dialog.getByRole('button', { name: /^continue$/i }).click()
    await dialog.getByRole('button', { name: /confirm replace|确认替换/i }).click()

    await expect(dialog.locator('.upload-error')).toBeVisible({ timeout: 10_000 })
    await expect(dialog.locator('.upload-error')).toHaveText(
      /The uploaded DOCX exceeds the maximum allowed size\./i,
    )
    await expect(dialog.locator('.upload-error')).not.toHaveText(RAW_ENVELOPE)
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('button', { name: /confirm replace|确认替换/i })).toBeEnabled()

    await captureEvidence(page, 'C10-B-server-inline')
  })
})
