import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { assertDockerStackReady, openDevBindingEditor } from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  CLEAN_STRUCTURED_CONTENT_JSON,
  prepareDraftTemplateWithCleanBinding,
} from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'

/**
 * LR-C3 — Structure-level undo/redo (BDD-LRP-C3-001…014).
 *
 * Canonical run (after stage 5 DEPLOY_OK):
 *   pnpm -C frontend exec playwright test e2e/LRP-C3-undo-redo.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080.
 */

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'LRP-C3-undo-redo',
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

async function expectStructureEquals(page: Page, expectedJson: string): Promise<void> {
  await expect
    .poll(async () => {
      const current = await editorJsonPreview(page)
      try {
        return JSON.stringify(JSON.parse(current))
      } catch {
        return current
      }
    })
    .toBe(JSON.stringify(JSON.parse(expectedJson)))
}

function insertBlockButton(page: Page, label: RegExp) {
  return page.getByTestId('insert-block-node').filter({ hasText: label })
}

async function insertBlock(page: Page, label: RegExp): Promise<void> {
  await insertBlockButton(page, label).click()
}

async function focusEditor(page: Page): Promise<void> {
  const editor = page.getByTestId('controlled-structured-content-editor')
  await editor.evaluate((el) => (el as HTMLElement).focus())
}

async function waitForLocalDraft(
  page: Page,
  key: string,
): Promise<Record<string, unknown>> {
  await expect
    .poll(
      async () =>
        page.evaluate((storageKey) => {
          const raw = localStorage.getItem(storageKey)
          if (!raw) {
            return null
          }
          try {
            return JSON.parse(raw) as Record<string, unknown>
          } catch {
            return null
          }
        }, key),
      { timeout: 15_000, intervals: [200, 400, 500] },
    )
    .not.toBeNull()

  const payload = await page.evaluate((storageKey) => {
    const raw = localStorage.getItem(storageKey)
    return raw ? (JSON.parse(raw) as Record<string, unknown>) : null
  }, key)
  if (!payload) {
    throw new Error(`Expected local draft at ${key}`)
  }
  return payload
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

function recoveryBanner(page: Page) {
  return page.getByTestId('structured-draft-recovery-banner')
}

async function captureEvidence(page: Page, filename: string): Promise<void> {
  await page.screenshot({
    path: path.join(EVIDENCE_DIR, filename),
    fullPage: false,
  })
}

async function prepareIsolatedContext(
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

test.describe('LR-C3 structure-level undo/redo', () => {
  test.describe.configure({ timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    )
  })

  // BDD-LRP-C3-005 — empty stack toolbar disabled (+ shortcut no-op)
  test('BDD-LRP-C3-005 empty history: Undo/Redo disabled; shortcut no-op', async ({
    page,
    request,
  }) => {
    const { templateId } = await prepareIsolatedContext(request, page)
    await openDevBindingEditor(page, request, templateId)

    const undoBtn = page.getByTestId('structured-editor-undo')
    const redoBtn = page.getByTestId('structured-editor-redo')
    await expect(undoBtn).toBeDisabled()
    await expect(redoBtn).toBeDisabled()

    const before = await editorJsonPreview(page)
    expect(JSON.parse(before)).toEqual(JSON.parse(CLEAN_STRUCTURED_CONTENT_JSON))

    await focusEditor(page)
    await page.keyboard.press('Control+z')
    await page.keyboard.press('Control+y')
    await expectStructureEquals(page, before)
    await expect(undoBtn).toBeDisabled()
    await expect(redoBtn).toBeDisabled()
    await captureEvidence(page, '01-empty-toolbar-disabled.png')
  })

  // BDD-LRP-C3-001 + 003 — three edits → undo×2 → structure = post-E1; redo×1 restores E2
  test('BDD-LRP-C3-001/003 three edits → undo×2 → redo×1 restores', async ({
    page,
    request,
  }) => {
    const { templateId } = await prepareIsolatedContext(request, page)
    await openDevBindingEditor(page, request, templateId)

    const undoBtn = page.getByTestId('structured-editor-undo')
    const redoBtn = page.getByTestId('structured-editor-redo')
    await expect(undoBtn).toBeDisabled()
    await expect(redoBtn).toBeDisabled()

    await insertBlock(page, /^heading$/i)
    const afterE1 = await editorJsonPreview(page)
    expect(afterE1).toContain('"type":"sectionHeading"')
    await expect(undoBtn).toBeEnabled()
    await expect(redoBtn).toBeDisabled()

    await insertBlock(page, /^paragraph$/i)
    const afterE2 = await editorJsonPreview(page)
    expect(afterE2).toContain('"type":"paragraph"')

    await insertBlock(page, /^list$/i)
    const afterE3 = await editorJsonPreview(page)
    expect(afterE3).toContain('"type":"list"')
    await expect(undoBtn).toBeEnabled()
    await expect(redoBtn).toBeDisabled()

    await focusEditor(page)
    await page.keyboard.press('Control+z')
    await page.keyboard.press('Control+z')

    await expectStructureEquals(page, afterE1)
    await expect(redoBtn).toBeEnabled()
    await expect(undoBtn).toBeEnabled()

    await page.keyboard.press('Control+y')
    await expectStructureEquals(page, afterE2)
    await expect(redoBtn).toBeEnabled()
    await expect(undoBtn).toBeEnabled()

    await redoBtn.click()
    await expectStructureEquals(page, afterE3)
    await expect(redoBtn).toBeDisabled()
    await captureEvidence(page, '02-after-undo-redo-cycle.png')
  })

  // BDD-LRP-C3-004 — new edit after undo truncates redo branch
  test('BDD-LRP-C3-004 new edit after undo clears redo branch', async ({ page, request }) => {
    const { templateId } = await prepareIsolatedContext(request, page)
    await openDevBindingEditor(page, request, templateId)

    const undoBtn = page.getByTestId('structured-editor-undo')
    const redoBtn = page.getByTestId('structured-editor-redo')

    await insertBlock(page, /^heading$/i)
    await insertBlock(page, /^list$/i)
    const afterTwo = await editorJsonPreview(page)
    expect(afterTwo).toContain('"type":"list"')

    await undoBtn.click()
    await expect(redoBtn).toBeEnabled()
    const afterUndo = await editorJsonPreview(page)
    expect(afterUndo).not.toContain('"type":"list"')

    await insertBlock(page, /^paragraph$/i)
    await expect(redoBtn).toBeDisabled()

    await focusEditor(page)
    await page.keyboard.press('Control+y')
    const afterStaleRedo = await editorJsonPreview(page)
    expect(afterStaleRedo).not.toContain('"type":"list"')
    expect(afterStaleRedo).toContain('"type":"paragraph"')
    await captureEvidence(page, '03-branch-truncation.png')
  })

  // BDD-LRP-C3-008 — draft blob never contains undo/history fields
  test('BDD-LRP-C3-008 local draft payload has no undo stack fields', async ({
    page,
    request,
  }) => {
    const { templateId, storageKey } = await prepareIsolatedContext(request, page)
    await openDevBindingEditor(page, request, templateId)

    await insertBlock(page, /^heading$/i)
    await insertBlock(page, /^paragraph$/i)

    const payload = await waitForLocalDraft(page, storageKey)
    expect(payload).toHaveProperty('schemaVersion')
    expect(payload).toHaveProperty('structureJson')
    expect(payload).toHaveProperty('draftUpdatedAt')
    expect(payload).not.toHaveProperty('undoStack')
    expect(payload).not.toHaveProperty('redoStack')
    expect(payload).not.toHaveProperty('history')
    expect(payload).not.toHaveProperty('past')
    expect(payload).not.toHaveProperty('future')
    expect(JSON.stringify(payload).toLowerCase()).not.toMatch(/undostack|redostack|"history"/)
  })

  // BDD-LRP-C3-009 — Save success → remount has empty history (clear-on-save)
  test('BDD-LRP-C3-009 save success clears history on remount', async ({ page, request }) => {
    const { templateId } = await prepareIsolatedContext(request, page)
    await openDevBindingEditor(page, request, templateId)

    const undoBtn = page.getByTestId('structured-editor-undo')
    const redoBtn = page.getByTestId('structured-editor-redo')

    await insertBlock(page, /^heading$/i)
    await insertBlock(page, /^list$/i)
    await expect(undoBtn).toBeEnabled()

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

    await reopenHeaderBindingEditor(page)
    await expect(page.getByTestId('structured-editor-undo')).toBeDisabled()
    await expect(page.getByTestId('structured-editor-redo')).toBeDisabled()
    await captureEvidence(page, '04-after-save-empty-history.png')
  })

  // BDD-LRP-C3-010 — Restore draft resets history
  test('BDD-LRP-C3-010 Restore draft resets undo/redo history', async ({ page, request }) => {
    const { templateId, storageKey } = await prepareIsolatedContext(request, page)
    await openDevBindingEditor(page, request, templateId)

    await insertBlock(page, /^heading$/i)
    const draft = await waitForLocalDraft(page, storageKey)
    const draftStructure = String(draft.structureJson)

    await reloadAcceptingUnload(page)
    await reopenHeaderBindingEditor(page)

    const banner = recoveryBanner(page)
    await expect(banner).toBeVisible({ timeout: 15_000 })

    // Build in-session history while banner is visible, then Restore must clear it.
    await insertBlock(page, /^list$/i)
    await expect(page.getByTestId('structured-editor-undo')).toBeEnabled()

    await banner.getByTestId('structured-draft-recovery-banner-restore').click()
    await expect(banner).toHaveCount(0)

    await expectStructureEquals(page, draftStructure)
    await expect(page.getByTestId('structured-editor-undo')).toBeDisabled()
    await expect(page.getByTestId('structured-editor-redo')).toBeDisabled()
    await captureEvidence(page, '05-after-restore-history-cleared.png')
  })

  // BDD-LRP-C3-011 — Discard draft resets history
  test('BDD-LRP-C3-011 Discard draft resets undo/redo history', async ({ page, request }) => {
    const { templateId, storageKey } = await prepareIsolatedContext(request, page)
    await openDevBindingEditor(page, request, templateId)

    await insertBlock(page, /^heading$/i)
    await waitForLocalDraft(page, storageKey)

    await reloadAcceptingUnload(page)
    await reopenHeaderBindingEditor(page)

    const banner = recoveryBanner(page)
    await expect(banner).toBeVisible({ timeout: 15_000 })

    // Build in-session history under the banner; Discard must clear stacks (C3-C10).
    await insertBlock(page, /^list$/i)
    await expect(page.getByTestId('structured-editor-undo')).toBeEnabled()

    await banner.getByTestId('structured-draft-recovery-banner-discard').click()
    await expect(banner).toHaveCount(0)

    await expect(page.getByTestId('structured-editor-undo')).toBeDisabled()
    await expect(page.getByTestId('structured-editor-redo')).toBeDisabled()
    await captureEvidence(page, '06-after-discard-history-cleared.png')
  })
})
