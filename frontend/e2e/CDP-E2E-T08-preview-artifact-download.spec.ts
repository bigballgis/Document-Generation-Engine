import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  prepareCdpMvpGoldenDraft,
  type CdpMvpGoldenFixture,
} from './helpers/cdp-mvp-golden-api'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  listTestDataSets,
  openFolDevEditorTestingTab,
  previewProgressDialog,
  runPreviewFromFirstDataSetRow,
  waitForPreviewConcurrencySlot,
  waitForPreviewDialogSuccess,
} from './helpers/template-testing-api'

/** Docker acceptance UI (override with E2E_BASE_URL / FRONTEND_PORT). */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

/**
 * Assert artifact GET succeeds with session cookies (not 401/403/410) and non-empty body.
 * Preview download buttons are `<a href target="_blank">` with Content-Disposition attachment.
 */
async function assertArtifactDownloadViaHref(
  page: import('@playwright/test').Page,
  linkName: RegExp,
  expectedContentHint: RegExp,
): Promise<void> {
  const dialog = previewProgressDialog(page)
  const link = dialog.getByRole('link', { name: linkName })
  await expect(link).toBeVisible()

  const href = await link.getAttribute('href')
  expect(href, `Download link ${linkName} must expose artifact href`).toBeTruthy()
  expect(href!).toMatch(/\/previews\/[^/]+\/artifacts\/(docx|pdf)/i)

  // JWT lives in localStorage (not cookies) — page.request must forward the Bearer token.
  const token = await page.evaluate(() => localStorage.getItem('docgen.accessToken'))
  expect(token, 'Expected management access token in localStorage').toBeTruthy()

  const response = await page.request.get(href!, {
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(
    [401, 403, 410].includes(response.status()),
    `Artifact GET ${href} must not be 401/403/410 (got ${response.status()})`,
  ).toBe(false)
  expect(response.ok(), `Artifact GET ${href} failed: ${response.status()}`).toBeTruthy()

  const contentType = response.headers()['content-type'] ?? ''
  expect(contentType).toMatch(expectedContentHint)

  const body = await response.body()
  expect(body.byteLength).toBeGreaterThan(0)
}

test.describe('CDP-E2E-T08 Preview success + artifact download (BDD-CDP-PREV-001/002)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: CdpMvpGoldenFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    // CDP golden DRAFT + publishable bindings + CDP-MVP-DATASET-01 — known successful render path.
    fixture = await prepareCdpMvpGoldenDraft(request)
    expect(fixture.lifecycleStatus).toBe('DRAFT')
  })

  test('BDD-CDP-PREV-001 — Run preview reaches success dialog with Download DOCX/PDF + expires copy', async ({
    page,
    request,
  }) => {
    const dataSets = await listTestDataSets(request, fixture.templateId)
    expect(dataSets.length).toBeGreaterThan(0)
    await waitForPreviewConcurrencySlot(request, fixture.templateId, dataSets[0]!.testDataSetId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)

    const previewStart = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/previews/async-preview'),
      { timeout: 30_000 },
    )

    await runPreviewFromFirstDataSetRow(page)
    const startResponse = await previewStart
    expect([200, 202]).toContain(startResponse.status())

    const dialog = previewProgressDialog(page)
    await expect(
      dialog.getByText(/queued|generating docx|converting to pdf|uploading/i).first(),
    ).toBeVisible({ timeout: 30_000 })

    // Must reach success — do not accept error/retry as pass (closes P12 T13 success-frame gap).
    await waitForPreviewDialogSuccess(page)

    await expect(dialog.locator('.preview-progress__success')).toBeVisible()
    await expect(dialog.locator('.preview-progress__error')).toHaveCount(0)
    await expect(dialog.getByRole('link', { name: /^download docx$/i })).toBeVisible()
    await expect(dialog.getByRole('link', { name: /^download pdf$/i })).toBeVisible()
    await expect(dialog.getByText(/expires in/i)).toBeVisible()
  })

  test('BDD-CDP-PREV-002 — Download DOCX and PDF artifact URLs succeed (not 401/403/410)', async ({
    page,
    request,
  }) => {
    const dataSets = await listTestDataSets(request, fixture.templateId)
    expect(dataSets.length).toBeGreaterThan(0)
    await waitForPreviewConcurrencySlot(request, fixture.templateId, dataSets[0]!.testDataSetId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openFolDevEditorTestingTab(page, fixture.templateId, request)
    await runPreviewFromFirstDataSetRow(page)
    await waitForPreviewDialogSuccess(page)

    await assertArtifactDownloadViaHref(
      page,
      /^download docx$/i,
      /wordprocessingml|officedocument|octet-stream|msword/i,
    )
    await assertArtifactDownloadViaHref(page, /^download pdf$/i, /pdf|octet-stream/i)
  })
})
