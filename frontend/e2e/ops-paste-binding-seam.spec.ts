import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import {
  assertDockerStackReady,
  openDevBindingEditor,
} from './helpers/core-fortress-f7'
import { assertDemoCatalogSeeded, E2E_API_BASE_URL } from './helpers/masters-api'
import {
  CLEAN_STRUCTURED_CONTENT_JSON,
  fetchPublishGateViaApi,
  prepareDraftTemplateWithCleanBinding,
  prepareDraftTemplateWithUnresolvedPasteResidue,
  upsertBindingViaApi,
  validateBindingsViaApi,
  type StructuredAuthoringFixture,
} from './helpers/structured-authoring-api'

/**
 * ops-paste-binding-seam — Paste cleaning ↔ binding / publish fail-closed (BDD-OPS-PASTE-BINDING-001).
 *
 * Canonical run:
 *   pnpm -C frontend exec playwright test e2e/ops-paste-binding-seam.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 *
 * Requires Docker acceptance stack at :4173 / :8080 (DEPLOY_OK).
 */

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'ops-paste-binding-seam',
)

const OBJECT_PASTE_HTML =
  '<p>Paragraph with embedded object</p><object data="embed.bin" type="application/octet-stream"></object>'
const ABSOLUTE_PASTE_HTML = '<p style="position:absolute;top:0">Absolutely positioned paragraph</p>'
const CLEAN_PASTE_HTML = '<p>Clean paragraph from E2E paste binding seam</p>'

async function pasteHtmlIntoEditor(page: Page, html: string) {
  // Controlled editor accepts paste via Import HTML file input (not clipboard events).
  await page.evaluate(async (sourceHtml) => {
    const input = document.querySelector(
      '[data-testid="controlled-structured-content-editor"] input[type="file"]',
    ) as HTMLInputElement | null
    if (!input) {
      throw new Error('Structured editor Import HTML file input was not found')
    }
    const file = new File([sourceHtml], 'e2e-paste.html', { type: 'text/html' })
    const transfer = new DataTransfer()
    transfer.items.add(file)
    input.files = transfer.files
    input.dispatchEvent(new Event('change', { bubbles: true }))
  }, html)
}

async function captureEvidence(page: Page, filename: string): Promise<void> {
  await page.screenshot({
    path: path.join(EVIDENCE_DIR, filename),
    fullPage: false,
  })
}

function pasteDialog(page: Page) {
  return page.getByTestId('paste-cleaning-summary-dialog')
}

function headerBindingRow(page: Page) {
  return page.locator('.bindings-panel .el-table__row').filter({ hasText: 'HEADER' })
}

test.describe('ops-paste-binding-seam (BDD-OPS-PASTE-BINDING-001)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  let cleanFixture: StructuredAuthoringFixture

  test.beforeAll(async ({ request }) => {
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    )
    await assertDemoCatalogSeeded(request)
    cleanFixture = await prepareDraftTemplateWithCleanBinding(request)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  // BDD-OPS-PASTE-BINDING-001 / S1 — object paste BLOCKED; Accept disabled; content not applied
  test('S1a: object paste is blocked and Accept stays disabled', async ({ page, request }) => {
    await openDevBindingEditor(page, request, cleanFixture.templateId, 'HEADER')

    const jsonPreview = page.locator('.json-preview pre')
    const initialJson = await jsonPreview.textContent()
    expect(initialJson).toBeTruthy()

    await pasteHtmlIntoEditor(page, OBJECT_PASTE_HTML)

    const dialog = pasteDialog(page)
    await expect(dialog).toBeVisible({ timeout: 15_000 })
    await expect(dialog.getByText(/paste cleaning summary/i)).toBeVisible()
    await expect(dialog.getByText(/paste blocked/i).first()).toBeVisible()
    await expect(dialog.getByText(/^Blocked$/i).first()).toBeVisible()
    await expect(dialog.getByTestId('paste-summary-accept')).toBeDisabled()

    await captureEvidence(page, 'S1a-object-paste-blocked.png')

    await dialog.getByTestId('paste-summary-cancel').click()
    await expect(dialog).not.toBeVisible()
    await expect(jsonPreview).toHaveText(initialJson ?? '')
  })

  // BDD-OPS-PASTE-BINDING-001 / S1 — absolute positioning paste BLOCKED
  test('S1b: absolute positioning paste is blocked and Accept stays disabled', async ({
    page,
    request,
  }) => {
    await openDevBindingEditor(page, request, cleanFixture.templateId, 'HEADER')

    const jsonPreview = page.locator('.json-preview pre')
    const initialJson = await jsonPreview.textContent()
    expect(initialJson).toBeTruthy()

    await pasteHtmlIntoEditor(page, ABSOLUTE_PASTE_HTML)

    const dialog = pasteDialog(page)
    await expect(dialog).toBeVisible({ timeout: 15_000 })
    await expect(dialog.getByText(/paste blocked/i).first()).toBeVisible()
    await expect(dialog.getByTestId('paste-summary-accept')).toBeDisabled()

    await captureEvidence(page, 'S1b-absolute-paste-blocked.png')

    await dialog.getByTestId('paste-summary-cancel').click()
    await expect(dialog).not.toBeVisible()
    await expect(jsonPreview).toHaveText(initialJson ?? '')
  })

  // BDD-OPS-PASTE-BINDING-001 / S2 — clean paste Accept → residue blockedCount=0
  test('S2: clean paragraph paste Accept saves pasteCleaningEvidence with blockedCount=0', async ({
    page,
    request,
  }) => {
    await openDevBindingEditor(page, request, cleanFixture.templateId, 'HEADER')

    await pasteHtmlIntoEditor(page, CLEAN_PASTE_HTML)

    const dialog = pasteDialog(page)
    await expect(dialog).toBeVisible({ timeout: 15_000 })
    await expect(dialog.getByText(/paste blocked/i)).toHaveCount(0)
    await expect(dialog.getByTestId('paste-summary-accept')).toBeEnabled()

    await dialog.getByTestId('paste-summary-accept').click()
    await expect(dialog).not.toBeVisible()
    await expect(page.locator('.json-preview pre')).toContainText('"type":"paragraph"')
    await expect(page.locator('.json-preview pre')).toContainText('Clean paragraph from E2E paste')

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        /\/templates\/[^/]+\/bindings\/HEADER/.test(response.url()),
      { timeout: 30_000 },
    )

    await page.getByRole('button', { name: /^save$/i }).click()
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()

    const body = (await saveResponse.json()) as {
      result: {
        validationStatus: string
        pasteCleaningEvidence?: {
          blockedCount?: number
          unresolvedPasteBlockers?: boolean
          items?: Array<{ category: string }>
        } | null
        structuredContentJson: string
      }
    }

    const evidence = body.result.pasteCleaningEvidence
    expect(evidence).toBeTruthy()
    expect(evidence?.blockedCount ?? 0).toBe(0)
    expect(evidence?.unresolvedPasteBlockers).toBeFalsy()
    expect(evidence?.items?.some((item) => item.category === 'BLOCKED')).toBeFalsy()
    expect(body.result.structuredContentJson).not.toContain('<object')
    expect(body.result.structuredContentJson).not.toContain(CLEAN_PASTE_HTML)
    expect(body.result.validationStatus).toBe('VALID')

    await expect(page.locator('.el-message').getByText(/binding saved/i)).toBeVisible()
    await captureEvidence(page, 'S2-clean-paste-accepted.png')
  })

  // BDD-OPS-PASTE-BINDING-001 / S3 + S4 — inject unresolved residue → binding + publish gate fail-closed
  test('S3/S4: unresolved paste residue marks binding incompatible and publish gate PASTE_CLEANING_BLOCKERS', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithUnresolvedPasteResidue(request)

    expect(fixture.binding.validationStatus).toBe('INCOMPATIBLE_CONTENT_TYPE')
    expect(fixture.binding.pasteCleaningEvidence?.blockedCount).toBeGreaterThan(0)
    expect(fixture.binding.pasteCleaningEvidence?.unresolvedPasteBlockers).toBe(true)

    const validation = await validateBindingsViaApi(request, fixture.templateId)
    const header = validation.bindings.find((b) => b.anchorId === 'HEADER')
    expect(header?.validationStatus).toBe('INCOMPATIBLE_CONTENT_TYPE')
    expect(validation.summary.blocking).toBe(true)

    const gate = await fetchPublishGateViaApi(request, fixture.templateId)
    const pasteItem = gate.items.find((item) => item.checkCode === 'PASTE_CLEANING_BLOCKERS')
    expect(pasteItem).toBeTruthy()
    expect(pasteItem?.ready).toBe(false)
    expect(pasteItem?.blocker).toBe(true)
    expect(pasteItem?.messageKey).toBe('api.publishGate.pasteCleaningBlockers.blocked')
    expect(gate.ready).toBe(false)

    await openDevBindingEditor(page, request, fixture.templateId, 'HEADER', {
      expectPreviewPane: false,
    })

    // openDevBindingEditor already opens HEADER edit; assert residue UI then return to list for tag
    await expect(page.getByTestId('binding-paste-residue-alert')).toBeVisible()
    await expect(page.getByText(/unresolved paste-cleaning blockers/i)).toBeVisible()

    await page.getByRole('button', { name: /^back$/i }).click()
    const row = headerBindingRow(page)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await expect(row.getByTestId('binding-paste-residue-tag')).toBeVisible()
    await expect(row.getByText(/paste blocked/i)).toBeVisible()
    await expect(row.getByText(/incompatible content type/i)).toBeVisible()

    await captureEvidence(page, 'S3-S4-paste-residue-binding-ui.png')
  })

  // BDD-OPS-PASTE-BINDING-001 / S5 — clear residue / clean rewrite recovers paste gate
  test('S5: clear paste residue recovers binding status and PASTE_CLEANING_BLOCKERS gate', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithUnresolvedPasteResidue(request)

    const blockedGate = await fetchPublishGateViaApi(request, fixture.templateId)
    expect(
      blockedGate.items.find((item) => item.checkCode === 'PASTE_CLEANING_BLOCKERS')?.ready,
    ).toBe(false)

    await openDevBindingEditor(page, request, fixture.templateId, 'HEADER', {
      expectPreviewPane: false,
    })
    await expect(page.getByTestId('binding-paste-residue-alert')).toBeVisible()

    await page.getByTestId('binding-clear-paste-residue').click()

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        /\/templates\/[^/]+\/bindings\/HEADER/.test(response.url()),
      { timeout: 30_000 },
    )
    await page.getByRole('button', { name: /^save$/i }).click()
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()

    const body = (await saveResponse.json()) as {
      result: {
        validationStatus: string
        pasteCleaningEvidence?: { blockedCount?: number; unresolvedPasteBlockers?: boolean } | null
      }
    }
    expect(body.result.pasteCleaningEvidence == null || body.result.pasteCleaningEvidence.blockedCount === 0).toBe(
      true,
    )
    expect(body.result.validationStatus).toBe('VALID')

    await expect(page.locator('.el-message').getByText(/binding saved/i)).toBeVisible()

    // Save closes the editor and returns to the bindings list
    const row = headerBindingRow(page)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await expect(row.getByTestId('binding-paste-residue-tag')).toHaveCount(0)

    const recoveredGate = await fetchPublishGateViaApi(request, fixture.templateId)
    const pasteItem = recoveredGate.items.find((item) => item.checkCode === 'PASTE_CLEANING_BLOCKERS')
    expect(pasteItem?.ready).toBe(true)
    expect(pasteItem?.blocker).toBe(false)
    expect(pasteItem?.messageKey).toBe('api.publishGate.pasteCleaningBlockers.ready')

    await captureEvidence(page, 'S5-paste-residue-cleared.png')
  })

  // Optional clean-rewrite path for S5 adjacency — Accept clean paste over residue
  test('S5b: clean paste Accept rewrite clears unresolved paste residue', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithUnresolvedPasteResidue(request)

    // Ensure content tree is still valid so Accept path can replace residue
    await upsertBindingViaApi(request, fixture.templateId, 'HEADER', CLEAN_STRUCTURED_CONTENT_JSON, {
      pasteCleaningEvidence: {
        transformedCount: 0,
        removedCount: 0,
        warningCount: 0,
        blockedCount: 1,
        unresolvedPasteBlockers: true,
        items: [
          {
            category: 'BLOCKED',
            messageKey: 'paste.summary.blocked',
            detectionSummary: 'Blocked absolute positioning in pasted HTML.',
          },
        ],
      },
    })

    await openDevBindingEditor(page, request, fixture.templateId, 'HEADER', {
      expectPreviewPane: false,
    })
    await expect(page.getByTestId('binding-paste-residue-alert')).toBeVisible()

    await pasteHtmlIntoEditor(page, CLEAN_PASTE_HTML)
    const dialog = pasteDialog(page)
    await expect(dialog).toBeVisible({ timeout: 15_000 })
    await expect(dialog.getByTestId('paste-summary-accept')).toBeEnabled()
    await dialog.getByTestId('paste-summary-accept').click()
    await expect(dialog).not.toBeVisible()

    // Alert should clear once pending Accept evidence overrides residue
    await expect(page.getByTestId('binding-paste-residue-alert')).toHaveCount(0)

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        /\/templates\/[^/]+\/bindings\/HEADER/.test(response.url()),
      { timeout: 30_000 },
    )
    await page.getByRole('button', { name: /^save$/i }).click()
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()

    const body = (await saveResponse.json()) as {
      result: {
        validationStatus: string
        pasteCleaningEvidence?: { blockedCount?: number; unresolvedPasteBlockers?: boolean } | null
      }
    }
    expect(body.result.pasteCleaningEvidence?.blockedCount ?? 0).toBe(0)
    expect(body.result.pasteCleaningEvidence?.unresolvedPasteBlockers).toBeFalsy()
    expect(body.result.validationStatus).toBe('VALID')

    const gate = await fetchPublishGateViaApi(request, fixture.templateId)
    expect(gate.items.find((item) => item.checkCode === 'PASTE_CLEANING_BLOCKERS')?.ready).toBe(true)

    await captureEvidence(page, 'S5b-clean-rewrite-cleared.png')
  })
})
