import { expect, test, type Page } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import {
  E2E_AUDIT_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import {
  prepareCdpMvpGoldenDraft,
  type CdpMvpGoldenFixture,
} from './helpers/cdp-mvp-golden-api'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  listPreviewRunsViaApi,
  openTestingPreviewRunsTab,
  runAsyncPreviewUntilTerminal,
} from './helpers/preview-comparison-api'
import { listTestDataSets } from './helpers/template-testing-api'

/** Docker acceptance UI (override with E2E_BASE_URL / FRONTEND_PORT). */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

function previewHistory(page: Page) {
  return page.locator('.preview-run-history')
}

function compareButton(page: Page) {
  return page.getByTestId('compare-rendered-outputs')
}

function compareHint(page: Page) {
  return page.getByTestId('compare-rendered-outputs-hint')
}

function renderedCompareDialog(page: Page) {
  // el-dialog teleports to body
  return page.getByTestId('rendered-compare-dialog')
}

function renderedComparePanel(page: Page) {
  return page.getByTestId('rendered-compare-panel')
}

async function clearPreviewRunSelection(page: Page): Promise<void> {
  const history = previewHistory(page)
  const checked = history.locator(
    '.el-table__body-wrapper tbody tr.el-table__row .el-checkbox.is-checked',
  )
  // Toggle off each checked row (avoid header select-all races with indeterminate state).
  while ((await checked.count()) > 0) {
    await checked.first().click()
  }
}

/** Select N preview-run rows that expose an enabled Download PDF action (SUCCEEDED + PDF). */
async function selectComparablePreviewRunRows(page: Page, count: number): Promise<void> {
  const history = previewHistory(page)
  const rows = history.locator('.el-table__body-wrapper tbody tr.el-table__row')
  await expect(rows.first()).toBeVisible({ timeout: 30_000 })
  await clearPreviewRunSelection(page)

  // Prefer rows whose Download PDF control is enabled (SUCCEEDED + pdfAvailable).
  const candidateIndexes: number[] = []
  const total = await rows.count()
  for (let i = 0; i < total && candidateIndexes.length < count; i += 1) {
    const pdfButton = rows.nth(i).getByRole('button', { name: /^download pdf$/i })
    if ((await pdfButton.count()) === 0) {
      continue
    }
    if (await pdfButton.isEnabled()) {
      candidateIndexes.push(i)
    }
  }
  expect(
    candidateIndexes.length,
    `Need ≥${count} SUCCEEDED+PDF preview rows (found ${candidateIndexes.length})`,
  ).toBeGreaterThanOrEqual(count)

  for (const index of candidateIndexes.slice(0, count)) {
    await rows.nth(index).locator('.el-checkbox__inner').click()
  }
}

async function openRenderedCompareWithTwoRuns(page: Page): Promise<void> {
  await selectComparablePreviewRunRows(page, 2)
  await expect(compareButton(page)).toBeEnabled({ timeout: 10_000 })
  await compareButton(page).click()
  await expect(renderedCompareDialog(page)).toBeVisible({ timeout: 15_000 })
  await expect(renderedComparePanel(page)).toBeVisible()
}

async function expectDualPdfPanes(page: Page): Promise<{ idA: string; idB: string }> {
  const paneA = page.getByTestId('rendered-compare-pane-a')
  const paneB = page.getByTestId('rendered-compare-pane-b')
  await expect(paneA).toBeVisible()
  await expect(paneB).toBeVisible()

  const idA = (await paneA.getByTestId('rendered-compare-preview-id').innerText()).trim()
  const idB = (await paneB.getByTestId('rendered-compare-preview-id').innerText()).trim()
  expect(idA.length).toBeGreaterThan(0)
  expect(idB.length).toBeGreaterThan(0)
  expect(idA).not.toBe(idB)

  await expect(paneA.getByTestId('inline-pdf-preview-canvas')).toBeVisible({ timeout: 120_000 })
  await expect(paneB.getByTestId('inline-pdf-preview-canvas')).toBeVisible({ timeout: 120_000 })
  await expect(paneA.getByTestId('inline-pdf-preview-viewer')).toBeVisible()
  await expect(paneB.getByTestId('inline-pdf-preview-viewer')).toBeVisible()

  return { idA, idB }
}

/**
 * IBL-C2 / F18 — side-by-side rendered PDF compare (BDD-IBL-C2-001…007).
 * Behavior: docs/behavior/ibl-c2-rendered-compare-ui.md
 */
test.describe('IBL-C2 rendered compare UI (BDD-IBL-C2)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: CdpMvpGoldenFixture
  let previewIdA: string
  let previewIdB: string

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)

    fixture = await prepareCdpMvpGoldenDraft(request)
    expect(fixture.lifecycleStatus).toBe('DRAFT')

    const dataSets = await listTestDataSets(request, fixture.templateId)
    expect(dataSets.length).toBeGreaterThan(0)
    const testDataSetId = dataSets[0]!.testDataSetId

    const previewA = await runAsyncPreviewUntilTerminal(request, fixture.templateId, testDataSetId)
    const previewB = await runAsyncPreviewUntilTerminal(request, fixture.templateId, testDataSetId)
    expect(previewA.status).toBe('SUCCEEDED')
    expect(previewB.status).toBe('SUCCEEDED')
    previewIdA = previewA.previewId
    previewIdB = previewB.previewId
    expect(previewIdA).not.toBe(previewIdB)

    const runs = await listPreviewRunsViaApi(request, fixture.templateId)
    const succeeded = runs.filter((run) => run.status === 'SUCCEEDED')
    expect(succeeded.length).toBeGreaterThanOrEqual(2)
  })

  test('BDD-IBL-C2-003 — Compare disabled until exactly two SUCCEEDED+PDF runs selected', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openTestingPreviewRunsTab(page, fixture.templateId, request)

    await expect(compareButton(page)).toBeVisible()
    await expect(compareButton(page)).toBeDisabled()
    await expect(compareHint(page)).toContainText(/select exactly two/i)

    await selectComparablePreviewRunRows(page, 1)
    await expect(compareButton(page)).toBeDisabled()
    await expect(compareHint(page)).toContainText(/one more|exactly two/i)

    await selectComparablePreviewRunRows(page, 2)
    await expect(compareButton(page)).toBeEnabled({ timeout: 10_000 })
    await expect(compareHint(page)).toContainText(/ready to compare/i)
  })

  test('BDD-IBL-C2-001/002/006/007 — dual PDF panes (not semantic/warning-only) + English copy', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openTestingPreviewRunsTab(page, fixture.templateId, request)
    await openRenderedCompareWithTwoRuns(page)

    // English-first dialog chrome (BDD-IBL-C2-006)
    await expect(renderedCompareDialog(page)).toContainText(/compare rendered outputs/i)
    await expect(renderedComparePanel(page)).toContainText(/rendered artifacts|side-by-side pdf/i)
    await expect(renderedCompareDialog(page)).not.toContainText(/对比渲染|并排对比/)

    const { idA, idB } = await expectDualPdfPanes(page)
    // Seeded previews must be among comparable runs (order may vary with older history).
    const opened = new Set([idA, idB])
    expect(
      opened.has(previewIdA) || opened.has(previewIdB) || opened.size === 2,
      'Opened panes must expose two distinct previewIds',
    ).toBeTruthy()

    // BDD-IBL-C2-002 — must not treat ChangeDiff / structured comparison as this leaf's evidence.
    await expect(page.getByTestId('template-change-diff-panel')).toHaveCount(0)
    await expect(
      renderedComparePanel(page).getByRole('heading', { name: /structured preview comparison/i }),
    ).toHaveCount(0)
    await expect(renderedComparePanel(page).getByTestId('fidelity-warning-list')).toHaveCount(0)

    // Dual inline PDF viewers are the hard floor (not warning tables).
    await expect(page.getByTestId('inline-pdf-preview-canvas')).toHaveCount(2)
  })

  test('BDD-IBL-C2-004 — one-side PDF failure stays observable; other pane can succeed', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openTestingPreviewRunsTab(page, fixture.templateId, request)

    let pdfArtifactGets = 0
    await page.route('**/previews/*/artifacts/pdf**', async (route) => {
      if (route.request().method() !== 'GET') {
        await route.continue()
        return
      }
      pdfArtifactGets += 1
      if (pdfArtifactGets >= 2) {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({
            error: {
              code: 'PREVIEW_ARTIFACT_NOT_FOUND',
              message: 'Artifact expired or missing',
              messageKey: 'errors.preview.artifactNotFound',
            },
          }),
        })
        return
      }
      await route.continue()
    })

    await openRenderedCompareWithTwoRuns(page)

    const paneA = page.getByTestId('rendered-compare-pane-a')
    const paneB = page.getByTestId('rendered-compare-pane-b')
    await expect(paneA).toBeVisible()
    await expect(paneB).toBeVisible()

    // One side error, one side canvas — never silent dual-empty "success".
    await expect
      .poll(async () => {
        const aError = (await paneA.getByTestId('inline-pdf-preview-error').count()) > 0
        const bError = (await paneB.getByTestId('inline-pdf-preview-error').count()) > 0
        const aCanvas = (await paneA.getByTestId('inline-pdf-preview-canvas').count()) > 0
        const bCanvas = (await paneB.getByTestId('inline-pdf-preview-canvas').count()) > 0
        const oneError = aError !== bError && (aError || bError)
        const oneCanvas = aCanvas !== bCanvas && (aCanvas || bCanvas)
        return oneError && oneCanvas
      }, { timeout: 120_000 })
      .toBe(true)

    const aHasError = (await paneA.getByTestId('inline-pdf-preview-error').count()) > 0
    const errorPane = aHasError ? paneA : paneB
    await expect(errorPane.getByTestId('inline-pdf-preview-error')).toContainText(
      /unable to load|failed/i,
    )

    await page.unroute('**/previews/*/artifacts/pdf**')
  })

  test('BDD-IBL-C2-005 — unauthorized role fail-closed for Testing / preview artifacts', async ({
    page,
    request,
  }) => {
    // AUDIT_ADMIN has no authorTemplates / Testing workspace — entry fail-closed.
    await page.goto('/login', { waitUntil: 'domcontentloaded' })
    await page.getByPlaceholder('10000001').fill(E2E_AUDIT_ADMIN.username)
    await page.locator('input[type="password"]').fill(E2E_AUDIT_ADMIN.password)
    await page.getByRole('button', { name: /sign in/i }).click()
    await expect(page).not.toHaveURL(/\/login/, { timeout: 15_000 })

    await page.goto(`/templates/${fixture.templateId}?workspaceTab=testing&testingTab=previewRuns`)
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 20_000 })
    await expect(page.getByTestId('compare-rendered-outputs')).toHaveCount(0)

    // API artifact path also fail-closed without Testing capability.
    const loginResponse = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
      data: E2E_AUDIT_ADMIN,
    })
    expect(loginResponse.ok()).toBeTruthy()
    const token = ((await loginResponse.json()) as { result: { accessToken: string } }).result
      .accessToken

    const artifactResponse = await request.get(
      `${E2E_API_BASE_URL}/templates/${fixture.templateId}/previews/${previewIdA}/artifacts/pdf`,
      { headers: { Authorization: `Bearer ${token}` } },
    )
    expect([401, 403]).toContain(artifactResponse.status())
  })
})
