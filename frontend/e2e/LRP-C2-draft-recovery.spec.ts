import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import {
  assertDockerStackReady,
  dirtyGuardDialog,
  mutateBindingStructure,
  openDevBindingEditor,
  triggerRouteLeaveViaNav,
} from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  CLEAN_STRUCTURED_CONTENT_JSON,
  prepareDraftTemplateWithCleanBinding,
} from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'

/**
 * LR-C2 — Structured editor local draft recovery (BDD-LRP-C2-001…003 + 006).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C2-draft-recovery.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080 (stage 5 DEPLOY_OK).
 */

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'LRP-C2-draft-recovery',
)

const DRAFT_KEY_PREFIX = 'docgen.structuredDraft.v1:'

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

async function waitForLocalDraft(
  page: Page,
  key: string,
): Promise<{ structureJson: string; draftUpdatedAt: string }> {
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
            if (typeof parsed.structureJson !== 'string' || typeof parsed.draftUpdatedAt !== 'string') {
              return null
            }
            return {
              structureJson: parsed.structureJson,
              draftUpdatedAt: parsed.draftUpdatedAt,
            }
          } catch {
            return null
          }
        }, key),
      { timeout: 15_000, intervals: [200, 400, 500] },
    )
    .not.toBeNull()

  const draft = await page.evaluate((storageKey) => {
    const raw = localStorage.getItem(storageKey)
    if (!raw) {
      return null
    }
    return JSON.parse(raw) as { structureJson: string; draftUpdatedAt: string }
  }, key)

  if (!draft) {
    throw new Error(`Expected local draft at ${key}`)
  }
  return draft
}

async function readLocalDraft(
  page: Page,
  key: string,
): Promise<{ structureJson: string; draftUpdatedAt: string } | null> {
  return page.evaluate((storageKey) => {
    const raw = localStorage.getItem(storageKey)
    if (!raw) {
      return null
    }
    try {
      return JSON.parse(raw) as { structureJson: string; draftUpdatedAt: string }
    } catch {
      return null
    }
  }, key)
}

async function editorJsonPreview(page: Page): Promise<string> {
  const details = page.locator('details.json-preview')
  await expect(details).toBeAttached()
  await details.evaluate((el) => {
    ;(el as HTMLDetailsElement).open = true
  })
  const preview = details.locator('pre')
  await expect(preview).toBeVisible()
  const text = await preview.textContent()
  expect(text).toBeTruthy()
  return text ?? ''
}

function recoveryBanner(page: Page) {
  return page.getByTestId('structured-draft-recovery-banner')
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

async function captureEvidence(page: Page, filename: string): Promise<void> {
  await page.screenshot({
    path: path.join(EVIDENCE_DIR, filename),
    fullPage: false,
  })
}

async function prepareIsolatedDraftContext(
  request: APIRequestContext,
  page: Page,
): Promise<{ templateId: string; storageKey: string }> {
  const fixture = await prepareDraftTemplateWithCleanBinding(request)
  const devVersionId = await resolveDevVersionId(request, fixture.templateId)
  const storageKey = draftStorageKey(E2E_ADMIN.username, fixture.templateId, devVersionId)
  await loginAs(page, E2E_ADMIN)
  await page.evaluate((key) => localStorage.removeItem(key), storageKey)
  return { templateId: fixture.templateId, storageKey }
}

test.describe('LRP-C2 structured editor local draft recovery', () => {
  test.describe.configure({ timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    )
  })

  // BDD-LRP-C2-001 — unsaved → reload → banner + timestamp; Restore exact structure
  test('reload shows recovery banner with timestamp and Restore applies exact draft JSON', async ({
    page,
    request,
  }) => {
    const { templateId, storageKey } = await prepareIsolatedDraftContext(request, page)

    await openDevBindingEditor(page, request, templateId)
    const baselineJson = await editorJsonPreview(page)
    expect(JSON.parse(baselineJson)).toEqual(JSON.parse(CLEAN_STRUCTURED_CONTENT_JSON))

    await mutateBindingStructure(page)
    const draft = await waitForLocalDraft(page, storageKey)
    expect(draft.structureJson).not.toEqual(CLEAN_STRUCTURED_CONTENT_JSON)
    expect(draft.draftUpdatedAt).toMatch(/^\d{4}-\d{2}-\d{2}T/)

    const editedPreview = await editorJsonPreview(page)
    expect(JSON.parse(editedPreview)).toEqual(JSON.parse(draft.structureJson))

    await reloadAcceptingUnload(page)
    await reopenHeaderBindingEditor(page)

    const banner = recoveryBanner(page)
    await expect(banner).toBeVisible({ timeout: 15_000 })
    await expect(banner.getByText(/unsaved local draft found/i)).toBeVisible()
    await expect(banner.getByText(/draft saved:/i)).toBeVisible()
    await expect(banner.getByTestId('structured-draft-recovery-banner-restore')).toBeVisible()
    await expect(banner.getByTestId('structured-draft-recovery-banner-discard')).toBeVisible()
    await captureEvidence(page, '01-recovery-banner-after-reload.png')

    await banner.getByTestId('structured-draft-recovery-banner-restore').click()
    await expect(banner).toHaveCount(0)

    const restoredPreview = await editorJsonPreview(page)
    expect(JSON.parse(restoredPreview)).toEqual(JSON.parse(draft.structureJson))
    expect(await readLocalDraft(page, storageKey)).not.toBeNull()
  })

  // BDD-LRP-C2-002 — Restore then Save → remount no banner + clear-on-save
  test('Restore then Save clears draft so remount shows no banner', async ({ page, request }) => {
    const { templateId, storageKey } = await prepareIsolatedDraftContext(request, page)

    await openDevBindingEditor(page, request, templateId)
    await mutateBindingStructure(page)
    const draft = await waitForLocalDraft(page, storageKey)

    await reloadAcceptingUnload(page)
    await reopenHeaderBindingEditor(page)

    const banner = recoveryBanner(page)
    await expect(banner).toBeVisible({ timeout: 15_000 })
    await banner.getByTestId('structured-draft-recovery-banner-restore').click()
    await expect(banner).toHaveCount(0)

    await expect
      .poll(async () => (await readLocalDraft(page, storageKey))?.structureJson ?? null, {
        timeout: 5_000,
      })
      .toEqual(draft.structureJson)

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${templateId}/bindings/HEADER`),
      { timeout: 60_000 },
    )
    await page.locator('.binding-editor__toolbar').getByRole('button', { name: /^save$/i }).click()
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()
    await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0, {
      timeout: 15_000,
    })

    await expect
      .poll(async () => readLocalDraft(page, storageKey), { timeout: 5_000 })
      .toBeNull()

    await reopenHeaderBindingEditor(page)
    await expect(recoveryBanner(page)).toHaveCount(0)
    const remountPreview = await editorJsonPreview(page)
    expect(JSON.parse(remountPreview)).toEqual(JSON.parse(draft.structureJson))
    await captureEvidence(page, '02-after-save-no-banner.png')
    expect(await readLocalDraft(page, storageKey)).toBeNull()
  })

  // BDD-LRP-C2-003 — Discard banner → clear draft, keep server structure
  test('banner Discard clears draft and keeps server structure', async ({ page, request }) => {
    const { templateId, storageKey } = await prepareIsolatedDraftContext(request, page)

    await openDevBindingEditor(page, request, templateId)
    await mutateBindingStructure(page)
    await waitForLocalDraft(page, storageKey)

    await reloadAcceptingUnload(page)
    await reopenHeaderBindingEditor(page)

    const banner = recoveryBanner(page)
    await expect(banner).toBeVisible({ timeout: 15_000 })
    await banner.getByTestId('structured-draft-recovery-banner-discard').click()
    await expect(banner).toHaveCount(0)
    expect(await readLocalDraft(page, storageKey)).toBeNull()

    const preview = await editorJsonPreview(page)
    expect(JSON.parse(preview)).toEqual(JSON.parse(CLEAN_STRUCTURED_CONTENT_JSON))
    await captureEvidence(page, '03-after-banner-discard.png')

    await triggerRouteLeaveViaNav(page)
    await expect(dirtyGuardDialog(page)).toHaveCount(0)
  })

  // BDD-LRP-C2-006 — dirty-guard Discard must NOT clear localStorage
  test('dirty-guard Discard leaves localStorage draft for later recovery', async ({
    page,
    request,
  }) => {
    const { templateId, storageKey } = await prepareIsolatedDraftContext(request, page)

    await openDevBindingEditor(page, request, templateId)
    await mutateBindingStructure(page)
    const draft = await waitForLocalDraft(page, storageKey)

    await page.getByRole('button', { name: /^back$/i }).click()
    const dialog = dirtyGuardDialog(page)
    await expect(dialog).toBeVisible()
    await dialog.getByTestId('dirty-guard-discard').click()
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })
    await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0)

    const retained = await readLocalDraft(page, storageKey)
    expect(retained).not.toBeNull()
    expect(retained?.structureJson).toEqual(draft.structureJson)

    await reopenHeaderBindingEditor(page)
    const banner = recoveryBanner(page)
    await expect(banner).toBeVisible({ timeout: 15_000 })
    await expect(banner.getByTestId('structured-draft-recovery-banner-restore')).toBeVisible()
    await captureEvidence(page, '04-banner-after-dirty-guard-discard.png')

    await banner.getByTestId('structured-draft-recovery-banner-discard').click()
    await expect(banner).toHaveCount(0)
    expect(await readLocalDraft(page, storageKey)).toBeNull()
    const preview = await editorJsonPreview(page)
    expect(JSON.parse(preview)).toEqual(JSON.parse(CLEAN_STRUCTURED_CONTENT_JSON))
  })
})
