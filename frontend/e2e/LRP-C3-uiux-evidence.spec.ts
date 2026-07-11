import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { assertDockerStackReady, openDevBindingEditor } from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  captureLrpC3LocatorScreenshot,
  captureLrpC3Screenshot,
  ensureLrpC3EvidenceDirs,
  LRP_C3_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

/**
 * LR-C3 — UIUX evidence for structure-level undo/redo History toolbar.
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/LRP-C3-uiux-evidence.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 */

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

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

function insertBlockButton(page: Page, label: RegExp) {
  return page.getByTestId('insert-block-node').filter({ hasText: label })
}

async function insertBlock(page: Page, label: RegExp): Promise<void> {
  await insertBlockButton(page, label).click()
}

async function prepareEditor(
  request: APIRequestContext,
  page: Page,
  options?: { skipLogin?: boolean },
): Promise<{ templateId: string }> {
  const fixture = await prepareDraftTemplateWithCleanBinding(request)
  await resolveDevVersionId(request, fixture.templateId)
  if (!options?.skipLogin) {
    await loginAs(page, E2E_ADMIN)
  }
  await openDevBindingEditor(page, request, fixture.templateId)
  const editor = page.getByTestId('controlled-structured-content-editor')
  await expect(editor).toBeVisible({ timeout: 30_000 })
  await editor.scrollIntoViewIfNeeded()
  return { templateId: fixture.templateId }
}

async function captureHistoryStates(
  page: Page,
  brand: 'REDBC' | 'GREENBC',
  startIndex: number,
): Promise<void> {
  const brandSlug = brand.toLowerCase()
  const undoBtn = page.getByTestId('structured-editor-undo')
  const redoBtn = page.getByTestId('structured-editor-redo')
  const toolbar = page.locator('.toolbar').first()
  const editor = page.getByTestId('controlled-structured-content-editor')

  await expect(undoBtn).toBeDisabled()
  await expect(redoBtn).toBeDisabled()
  await expect(undoBtn).toHaveAttribute('aria-label', /undo/i)
  await expect(redoBtn).toHaveAttribute('title', /redo structure change/i)

  await toolbar.scrollIntoViewIfNeeded()
  await captureLrpC3Screenshot(
    page,
    `${String(startIndex).padStart(2, '0')}-history-disabled-${brandSlug}-en-1440x900.png`,
  )
  await captureLrpC3LocatorScreenshot(
    toolbar,
    `${String(startIndex + 1).padStart(2, '0')}-history-disabled-closeup-${brandSlug}-en.png`,
  )
  await captureLrpC3LocatorScreenshot(
    page.locator('.shell-header .header-brand'),
    `${String(startIndex + 2).padStart(2, '0')}-brand-header-${brandSlug}-en.png`,
  )

  await insertBlock(page, /^heading$/i)
  await insertBlock(page, /^paragraph$/i)
  await expect(undoBtn).toBeEnabled()
  await expect(redoBtn).toBeDisabled()
  await toolbar.scrollIntoViewIfNeeded()
  await captureLrpC3LocatorScreenshot(
    toolbar,
    `${String(startIndex + 3).padStart(2, '0')}-undo-enabled-redo-disabled-closeup-${brandSlug}-en.png`,
  )

  await undoBtn.click()
  await expect(redoBtn).toBeEnabled()
  await expect(undoBtn).toBeEnabled()
  await toolbar.scrollIntoViewIfNeeded()
  await captureLrpC3LocatorScreenshot(
    toolbar,
    `${String(startIndex + 4).padStart(2, '0')}-undo-redo-enabled-closeup-${brandSlug}-en.png`,
  )

  await undoBtn.focus()
  await captureLrpC3LocatorScreenshot(
    toolbar,
    `${String(startIndex + 5).padStart(2, '0')}-undo-focus-${brandSlug}-en.png`,
  )

  await editor.scrollIntoViewIfNeeded()
  await captureLrpC3Screenshot(
    page,
    `${String(startIndex + 6).padStart(2, '0')}-editor-history-${brandSlug}-en-1440x900.png`,
  )
}

test.describe('LRP-C3 undo/redo History toolbar UIUX evidence', () => {
  test.describe.configure({ timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensureLrpC3EvidenceDirs()
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    )
  })

  test('dual-brand History toolbar enabled/disabled + focus', async ({ page, request }) => {
    await page.setViewportSize(LRP_C3_VIEWPORT)
    await prepareEditor(request, page)
    await switchBrand(page, 'REDBC')
    await captureHistoryStates(page, 'REDBC', 1)

    // Fresh fixture for GREENBC so History starts empty again (reuse session)
    await prepareEditor(request, page, { skipLogin: true })
    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await captureHistoryStates(page, 'GREENBC', 8)
  })
})
