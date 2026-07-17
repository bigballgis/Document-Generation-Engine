import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import {
  assertDockerStackReady,
  dirtyGuardDialog,
  mutateBindingStructure,
  openDevBindingEditor,
} from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  CLEAN_STRUCTURED_CONTENT_JSON,
  getBindingUpdatedAtViaApi,
  prepareDraftTemplateWithCleanBinding,
  prepareDualAnchorFolDraftTemplate,
  upsertBindingViaApi,
} from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'

/**
 * CE-U21 — Per-anchor localDraft keys + binding save 409 conflict UX.
 * BDD: docs/behavior/ce-u21-draft-anchor-concurrency.md (BDD-CE-U21-DAC-001…012)
 *
 * Canonical run (after stage 5 DEPLOY_OK):
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test e2e/CE-U21-draft-anchor-concurrency.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'CE-U21-draft-anchor-concurrency',
)

const DRAFT_KEY_PREFIX = 'docgen.structuredDraft.v1:'

const CONCURRENT_STRUCTURED_CONTENT_JSON = JSON.stringify({
  schemaVersion: '1.0',
  nodes: [
    {
      type: 'paragraph',
      children: [{ type: 'textRun', value: 'Concurrent session saved content' }],
    },
  ],
})

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

function draftStorageKey(
  userId: string,
  templateId: string,
  devVersionId: string,
  anchorId?: string,
): string {
  const base = `${DRAFT_KEY_PREFIX}${userId}:${templateId}:${devVersionId}`
  return anchorId ? `${base}:${anchorId}` : base
}

async function waitForLocalDraft(
  page: Page,
  key: string,
): Promise<{ structureJson: string; draftUpdatedAt: string; anchorId?: string | null }> {
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
              anchorId?: string | null
            }
            if (typeof parsed.structureJson !== 'string' || typeof parsed.draftUpdatedAt !== 'string') {
              return null
            }
            return {
              structureJson: parsed.structureJson,
              draftUpdatedAt: parsed.draftUpdatedAt,
              anchorId: parsed.anchorId,
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
    return JSON.parse(raw) as {
      structureJson: string
      draftUpdatedAt: string
      anchorId?: string | null
    }
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

function bindingVersionConflictDialog(page: Page) {
  return page.locator('.el-message-box').filter({ hasText: /binding updated elsewhere/i })
}

async function reopenBindingEditor(page: Page, anchorId: string): Promise<void> {
  const row = page.locator('.bindings-panel .el-table__row').filter({ hasText: anchorId })
  await expect(row).toBeVisible({ timeout: 30_000 })
  await row.getByRole('button', { name: /^edit$/i }).click()
  await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
}

async function leaveEditorKeepingLocalDraft(page: Page): Promise<void> {
  await page.getByRole('button', { name: /^back$/i }).click()
  const dialog = dirtyGuardDialog(page)
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  await dialog.getByTestId('dirty-guard-discard').click()
  await expect(dialog).not.toBeVisible({ timeout: 15_000 })
  await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0)
}

async function captureEvidence(page: Page, filename: string): Promise<void> {
  await page.screenshot({
    path: path.join(EVIDENCE_DIR, filename),
    fullPage: false,
  })
}

async function clearDraftKeys(page: Page, keys: string[]): Promise<void> {
  await page.evaluate((storageKeys) => {
    for (const key of storageKeys) {
      localStorage.removeItem(key)
    }
  }, keys)
}

test.describe('CE-U21 draft-anchor concurrency (BDD-CE-U21-DAC)', () => {
  test.describe.configure({ timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    )
  })

  // BDD-CE-U21-DAC-001 + 002 — per-anchor keys + cross-anchor isolation
  test('DAC-001/002 — per-anchor draft keys isolate across anchors', async ({ page, request }) => {
    const fixture = await prepareDualAnchorFolDraftTemplate(request)
    const devVersionId = await resolveDevVersionId(request, fixture.templateId)
    const keyA = draftStorageKey(
      E2E_ADMIN.username,
      fixture.templateId,
      devVersionId,
      fixture.anchorA,
    )
    const keyB = draftStorageKey(
      E2E_ADMIN.username,
      fixture.templateId,
      devVersionId,
      fixture.anchorB,
    )
    const legacyTriple = draftStorageKey(E2E_ADMIN.username, fixture.templateId, devVersionId)

    await loginAs(page, E2E_ADMIN)
    await clearDraftKeys(page, [keyA, keyB, legacyTriple])

    await openDevBindingEditor(page, request, fixture.templateId, fixture.anchorA, {
      expectPreviewPane: false,
    })
    await mutateBindingStructure(page)
    const draftA = await waitForLocalDraft(page, keyA)
    expect(draftA.structureJson).not.toEqual(CLEAN_STRUCTURED_CONTENT_JSON)
    expect(await readLocalDraft(page, legacyTriple)).toBeNull()

    await leaveEditorKeepingLocalDraft(page)

    await reopenBindingEditor(page, fixture.anchorB)
    await expect(recoveryBanner(page)).toHaveCount(0)
    await mutateBindingStructure(page)
    await mutateBindingStructure(page)
    const draftB = await waitForLocalDraft(page, keyB)
    expect(draftB.structureJson).not.toEqual(draftA.structureJson)

    const retainedA = await readLocalDraft(page, keyA)
    expect(retainedA).not.toBeNull()
    expect(retainedA?.structureJson).toEqual(draftA.structureJson)
    expect(await readLocalDraft(page, legacyTriple)).toBeNull()

    await leaveEditorKeepingLocalDraft(page)
    await reopenBindingEditor(page, fixture.anchorA)

    const banner = recoveryBanner(page)
    await expect(banner).toBeVisible({ timeout: 15_000 })
    await banner.getByTestId('structured-draft-recovery-banner-restore').click()
    await expect(banner).toHaveCount(0)
    const restored = await editorJsonPreview(page)
    expect(JSON.parse(restored)).toEqual(JSON.parse(draftA.structureJson))
    await captureEvidence(page, '01-per-anchor-isolation-restore-A.png')
  })

  // BDD-CE-U21-DAC-005 — success save clears current anchor draft only
  test('DAC-005 — successful Save clears current-anchor draft only', async ({ page, request }) => {
    const fixture = await prepareDualAnchorFolDraftTemplate(request)
    const devVersionId = await resolveDevVersionId(request, fixture.templateId)
    const keyA = draftStorageKey(
      E2E_ADMIN.username,
      fixture.templateId,
      devVersionId,
      fixture.anchorA,
    )
    const keyB = draftStorageKey(
      E2E_ADMIN.username,
      fixture.templateId,
      devVersionId,
      fixture.anchorB,
    )

    await loginAs(page, E2E_ADMIN)
    await clearDraftKeys(page, [keyA, keyB])

    await openDevBindingEditor(page, request, fixture.templateId, fixture.anchorB, {
      expectPreviewPane: false,
    })
    await mutateBindingStructure(page)
    const draftB = await waitForLocalDraft(page, keyB)
    await leaveEditorKeepingLocalDraft(page)

    await reopenBindingEditor(page, fixture.anchorA)
    await mutateBindingStructure(page)
    await waitForLocalDraft(page, keyA)

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${fixture.templateId}/bindings/${fixture.anchorA}`),
      { timeout: 60_000 },
    )
    await page.locator('.binding-editor__toolbar').getByRole('button', { name: /^save$/i }).click()
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()
    await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0, {
      timeout: 15_000,
    })

    await expect.poll(async () => readLocalDraft(page, keyA), { timeout: 5_000 }).toBeNull()
    const retainedB = await readLocalDraft(page, keyB)
    expect(retainedB).not.toBeNull()
    expect(retainedB?.structureJson).toEqual(draftB.structureJson)
    await captureEvidence(page, '02-save-clears-anchor-A-only.png')
  })

  // BDD-CE-U21-DAC-007 — stale token → 409 + Reload / Keep editing; draft retained
  test('DAC-007 — stale Save shows conflict UX and keeps local draft', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveDevVersionId(request, fixture.templateId)
    const keyHeader = draftStorageKey(E2E_ADMIN.username, fixture.templateId, devVersionId, 'HEADER')

    await loginAs(page, E2E_ADMIN)
    await clearDraftKeys(page, [keyHeader])

    await openDevBindingEditor(page, request, fixture.templateId, 'HEADER')
    await mutateBindingStructure(page)
    const draft = await waitForLocalDraft(page, keyHeader)

    const serverToken = await getBindingUpdatedAtViaApi(request, fixture.templateId, 'HEADER')
    await upsertBindingViaApi(
      request,
      fixture.templateId,
      'HEADER',
      CONCURRENT_STRUCTURED_CONTENT_JSON,
      { expectedUpdatedAt: serverToken, credentials: E2E_ADMIN },
    )

    const conflictResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${fixture.templateId}/bindings/HEADER`) &&
        response.status() === 409,
      { timeout: 60_000 },
    )
    await page.locator('.binding-editor__toolbar').getByRole('button', { name: /^save$/i }).click()
    const conflictResponse = await conflictResponsePromise
    const envelope = (await conflictResponse.json()) as {
      error?: { code?: string; messageKey?: string; category?: string }
    }
    expect(envelope.error?.code).toBe('BINDING_VERSION_CONFLICT')
    expect(envelope.error?.messageKey).toBe('api.error.template.bindingVersionConflict')
    expect(envelope.error?.category).toBe('CONFLICT')

    const dialog = bindingVersionConflictDialog(page)
    await expect(dialog).toBeVisible({ timeout: 15_000 })
    await expect(dialog.getByText(/binding updated elsewhere/i)).toBeVisible()
    await expect(dialog.getByRole('button', { name: /^reload$/i })).toBeVisible()
    await expect(dialog.getByRole('button', { name: /^keep editing$/i })).toBeVisible()
    await expect(page.getByText(/publish version conflict/i)).toHaveCount(0)
    await captureEvidence(page, '03-binding-version-conflict-dialog.png')

    await dialog.getByRole('button', { name: /^keep editing$/i }).click()
    await expect(dialog).toHaveCount(0)

    const retained = await readLocalDraft(page, keyHeader)
    expect(retained).not.toBeNull()
    expect(retained?.structureJson).toEqual(draft.structureJson)
    await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
  })

  // BDD-CE-U21-DAC-008 + 012 — Reload then Save succeeds; draft cleared
  test('DAC-008/012 — Reload after conflict then Save succeeds and clears draft', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveDevVersionId(request, fixture.templateId)
    const keyHeader = draftStorageKey(E2E_ADMIN.username, fixture.templateId, devVersionId, 'HEADER')

    await loginAs(page, E2E_ADMIN)
    await clearDraftKeys(page, [keyHeader])

    await openDevBindingEditor(page, request, fixture.templateId, 'HEADER')
    await mutateBindingStructure(page)
    await waitForLocalDraft(page, keyHeader)

    const serverToken = await getBindingUpdatedAtViaApi(request, fixture.templateId, 'HEADER')
    await upsertBindingViaApi(
      request,
      fixture.templateId,
      'HEADER',
      CONCURRENT_STRUCTURED_CONTENT_JSON,
      { expectedUpdatedAt: serverToken, credentials: E2E_ADMIN },
    )

    const conflictResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${fixture.templateId}/bindings/HEADER`) &&
        response.status() === 409,
      { timeout: 60_000 },
    )
    await page.locator('.binding-editor__toolbar').getByRole('button', { name: /^save$/i }).click()
    await conflictResponsePromise

    const dialog = bindingVersionConflictDialog(page)
    await expect(dialog).toBeVisible({ timeout: 15_000 })
    await dialog.getByRole('button', { name: /^reload$/i }).click()
    await expect(dialog).toHaveCount(0)

    await expect
      .poll(async () => {
        try {
          return JSON.stringify(JSON.parse(await editorJsonPreview(page)))
        } catch {
          return ''
        }
      }, { timeout: 15_000 })
      .toBe(JSON.stringify(JSON.parse(CONCURRENT_STRUCTURED_CONTENT_JSON)))

    // Reload clears per-anchor draft (and may suppress further draft writes until remount).
    expect(await readLocalDraft(page, keyHeader)).toBeNull()

    await mutateBindingStructure(page)
    const editedAfterReload = await editorJsonPreview(page)
    expect(JSON.parse(editedAfterReload)).not.toEqual(
      JSON.parse(CONCURRENT_STRUCTURED_CONTENT_JSON),
    )

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${fixture.templateId}/bindings/HEADER`) &&
        response.ok(),
      { timeout: 60_000 },
    )
    await page.locator('.binding-editor__toolbar').getByRole('button', { name: /^save$/i }).click()
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()
    await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0, {
      timeout: 15_000,
    })

    await expect.poll(async () => readLocalDraft(page, keyHeader), { timeout: 5_000 }).toBeNull()
    await captureEvidence(page, '04-reload-then-save-success.png')
  })
})

