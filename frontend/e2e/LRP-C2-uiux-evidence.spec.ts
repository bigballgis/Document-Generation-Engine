import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import {
  assertDockerStackReady,
  dirtyGuardDialog,
  mutateBindingStructure,
  openDevBindingEditor,
} from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  captureLrpC2LocatorScreenshot,
  captureLrpC2Screenshot,
  ensureLrpC2EvidenceDirs,
  LRP_C2_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

/**
 * LR-C2 — UIUX evidence for structured editor local draft recovery banner.
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C2-uiux-evidence.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 */

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const DRAFT_KEY_PREFIX = 'docgen.structuredDraft.v1:'

function recoveryBanner(page: Page) {
  return page.getByTestId('structured-draft-recovery-banner')
}

async function resolveDevVersionId(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight) {
    throw new Error(`No in-flight dev version for template ${templateId}`)
  }
  return inFlight.devVersionId
}

function draftStorageKey(userId: string, templateId: string, devVersionId: string): string {
  return `${DRAFT_KEY_PREFIX}${userId}:${templateId}:${devVersionId}`
}

async function waitForLocalDraft(page: Page, key: string): Promise<void> {
  await expect
    .poll(
      async () =>
        page.evaluate((storageKey) => {
          const raw = localStorage.getItem(storageKey)
          if (!raw) {
            return null
          }
          try {
            const parsed = JSON.parse(raw) as {
              structureJson?: string
              draftUpdatedAt?: string
            }
            if (
              typeof parsed.structureJson !== 'string' ||
              typeof parsed.draftUpdatedAt !== 'string'
            ) {
              return null
            }
            return parsed.draftUpdatedAt
          } catch {
            return null
          }
        }, key),
      { timeout: 15_000, intervals: [200, 400, 500] },
    )
    .not.toBeNull()
}

async function reloadAcceptingUnload(page: Page): Promise<void> {
  page.once('dialog', async (dialog) => {
    await dialog.accept()
  })
  await page.reload({ waitUntil: 'domcontentloaded' })
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
}

async function reopenHeaderBindingEditor(page: Page): Promise<void> {
  const row = page.locator('.bindings-panel .el-table__row').filter({ hasText: 'HEADER' })
  await expect(row).toBeVisible({ timeout: 30_000 })
  await row.getByRole('button', { name: /^edit$/i }).click()
  await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
}

async function prepareBannerContext(
  request: APIRequestContext,
  page: Page,
  options?: { skipLogin?: boolean },
): Promise<{ templateId: string; storageKey: string }> {
  const fixture = await prepareDraftTemplateWithCleanBinding(request)
  const devVersionId = await resolveDevVersionId(request, fixture.templateId)
  const storageKey = draftStorageKey(E2E_ADMIN.username, fixture.templateId, devVersionId)
  if (!options?.skipLogin) {
    await loginAs(page, E2E_ADMIN)
  }
  await page.evaluate((key) => localStorage.removeItem(key), storageKey)
  return { templateId: fixture.templateId, storageKey }
}

async function reachRecoveryBanner(page: Page, request: APIRequestContext, templateId: string, storageKey: string) {
  await openDevBindingEditor(page, request, templateId)
  await mutateBindingStructure(page)
  await waitForLocalDraft(page, storageKey)
  await reloadAcceptingUnload(page)
  await reopenHeaderBindingEditor(page)
  const banner = recoveryBanner(page)
  await expect(banner).toBeVisible({ timeout: 15_000 })
  await banner.scrollIntoViewIfNeeded()
  return banner
}

test.describe('LRP-C2 structured draft recovery UIUX evidence', () => {
  test.describe.configure({ timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensureLrpC2EvidenceDirs()
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    )
  })

  test('dual-brand recovery banner + after-save + dirty-guard remount', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(LRP_C2_VIEWPORT)
    const { templateId, storageKey } = await prepareBannerContext(request, page)
    await switchBrand(page, 'REDBC')

    const banner = await reachRecoveryBanner(page, request, templateId, storageKey)

    await expect(banner.getByText(/unsaved local draft found/i)).toBeVisible()
    await expect(banner.getByText(/draft saved:/i)).toBeVisible()
    await expect(banner.getByTestId('structured-draft-recovery-banner-restore')).toBeVisible()
    await expect(banner.getByTestId('structured-draft-recovery-banner-discard')).toBeVisible()

    await captureLrpC2Screenshot(page, '01-recovery-banner-redbc-en-1440x900.png')
    await captureLrpC2LocatorScreenshot(banner, '02-recovery-banner-closeup-redbc-en.png')
    await captureLrpC2LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03-brand-header-redbc-en.png',
    )

    // Focus ring on Restore (primary)
    const restore = banner.getByTestId('structured-draft-recovery-banner-restore')
    await restore.focus()
    await captureLrpC2LocatorScreenshot(banner, '04-restore-focus-redbc-en.png')

    // GREENBC dual-brand — same banner surface
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(banner).toBeVisible()
    await banner.scrollIntoViewIfNeeded()
    await captureLrpC2Screenshot(page, '05-recovery-banner-greenbc-en-1440x900.png')
    await captureLrpC2LocatorScreenshot(banner, '06-recovery-banner-closeup-greenbc-en.png')
    await captureLrpC2LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '07-brand-header-greenbc-en.png',
    )
    await restore.focus()
    await captureLrpC2LocatorScreenshot(banner, '08-restore-focus-greenbc-en.png')

    // After Restore + Save → remount shows no banner
    await switchBrand(page, 'REDBC')
    await restore.click()
    await expect(banner).toHaveCount(0)

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${templateId}/bindings/HEADER`),
      { timeout: 60_000 },
    )
    await page.getByTestId('binding-editor-save').click()
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()
    await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0, {
      timeout: 15_000,
    })

    await reopenHeaderBindingEditor(page)
    await expect(recoveryBanner(page)).toHaveCount(0)
    await page.getByTestId('controlled-structured-content-editor').scrollIntoViewIfNeeded()
    await captureLrpC2Screenshot(page, '09-after-save-no-banner-redbc-en-1440x900.png')
    await captureLrpC2LocatorScreenshot(
      page.getByTestId('controlled-structured-content-editor'),
      '10-editor-no-banner-closeup-redbc-en.png',
    )

    // Dirty-guard Discard retains localStorage → banner returns
    const { templateId: templateId2, storageKey: storageKey2 } = await prepareBannerContext(
      request,
      page,
      { skipLogin: true },
    )
    await switchBrand(page, 'REDBC')
    await openDevBindingEditor(page, request, templateId2)
    await mutateBindingStructure(page)
    await waitForLocalDraft(page, storageKey2)

    await page.getByRole('button', { name: /^back$/i }).click()
    const dialog = dirtyGuardDialog(page)
    await expect(dialog).toBeVisible()
    await dialog.getByTestId('dirty-guard-discard').click()
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })
    await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0)

    await reopenHeaderBindingEditor(page)
    const remountBanner = recoveryBanner(page)
    await expect(remountBanner).toBeVisible({ timeout: 15_000 })
    await remountBanner.scrollIntoViewIfNeeded()
    await captureLrpC2Screenshot(page, '11-banner-after-dirty-guard-discard-redbc-en-1440x900.png')
    await captureLrpC2LocatorScreenshot(
      remountBanner,
      '12-banner-after-dirty-guard-closeup-redbc-en.png',
    )

    // GREENBC remount banner (same retained draft)
    await switchBrand(page, 'GREENBC')
    await expect(remountBanner).toBeVisible()
    await remountBanner.scrollIntoViewIfNeeded()
    await captureLrpC2Screenshot(page, '13-banner-after-dirty-guard-discard-greenbc-en-1440x900.png')
    await captureLrpC2LocatorScreenshot(
      remountBanner,
      '14-banner-after-dirty-guard-closeup-greenbc-en.png',
    )

    await remountBanner.getByTestId('structured-draft-recovery-banner-discard').click()
    await expect(remountBanner).toHaveCount(0)
  })
})
