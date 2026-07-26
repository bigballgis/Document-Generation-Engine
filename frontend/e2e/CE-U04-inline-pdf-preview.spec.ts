import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, ensureDemoRetailMasterApproved } from './helpers/masters-api'
import { openDevBindingEditor } from './helpers/core-fortress-f7'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'

/** Docker acceptance UI (override with E2E_BASE_URL / FRONTEND_PORT). */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

function authoringInlinePdf(page: import('@playwright/test').Page) {
  // CRCH-W1-1: AuthoringPreviewPane no longer hosts a duplicate viewer; TemplatePreviewPanel owns it.
  return page.getByTestId('preview-inline-pdf-section')
}

/**
 * CE-U04 — in-app PDF preview via pdf.js.
 * BDD: docs/behavior/ce-u04-inline-pdf-preview.md (BDD-CE-U04-IPP-001…003)
 */
test.describe('CE-U04 inline PDF preview (BDD-CE-U04-IPP)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await ensureDemoRetailMasterApproved(request)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  test('BDD-CE-U04-IPP-001 — refresh shows page 1 without download', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await openDevBindingEditor(page, request, fixture.templateId)

    const refreshButton = page.getByTestId('authoring-preview-refresh')
    const previewStart = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-generate') &&
        response.ok(),
      { timeout: 120_000 },
    )
    await refreshButton.click()
    await previewStart

    const pdfArtifact = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        /\/previews\/[^/]+\/artifacts\/pdf$/.test(response.url()) &&
        response.ok(),
      { timeout: 120_000 },
    )
    await pdfArtifact

    const inlinePdf = authoringInlinePdf(page)
    const viewer = inlinePdf.getByTestId('inline-pdf-preview-viewer')
    await expect(viewer).toBeVisible({ timeout: 60_000 })
    await expect(inlinePdf.getByTestId('inline-pdf-preview-page-label')).toContainText(/page 1 of/i, {
      timeout: 120_000,
    })
    await expect(inlinePdf.getByTestId('inline-pdf-preview-canvas')).toBeVisible({ timeout: 120_000 })
    await expect(inlinePdf).toBeVisible()
    await expect(page.getByTestId('inline-pdf-preview-viewer')).toHaveCount(1)
    await expect(page.getByTestId('authoring-inline-pdf-section')).toHaveCount(0)
  })

  test('BDD-CE-U04-IPP-002 — next page control advances page label', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await openDevBindingEditor(page, request, fixture.templateId)

    const refreshButton = page.getByTestId('authoring-preview-refresh')
    await refreshButton.click()
    await page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        /\/previews\/[^/]+\/artifacts\/pdf$/.test(response.url()) &&
        response.ok(),
      { timeout: 120_000 },
    )

    const inlinePdf = authoringInlinePdf(page)
    const pageLabel = inlinePdf.getByTestId('inline-pdf-preview-page-label')
    await expect(pageLabel).toContainText(/page 1 of/i, { timeout: 60_000 })

    const nextButton = inlinePdf.getByTestId('inline-pdf-preview-next')
    if (await nextButton.isEnabled()) {
      await nextButton.click()
      await expect(pageLabel).toContainText(/page 2 of/i)
      await inlinePdf.getByTestId('inline-pdf-preview-prev').click()
      await expect(pageLabel).toContainText(/page 1 of/i)
    } else {
      await expect(pageLabel).toContainText(/page 1 of 1/i)
    }
  })
})
