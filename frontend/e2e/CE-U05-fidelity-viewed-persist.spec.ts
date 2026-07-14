import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'
import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import {
  fetchPublishGateViaApi,
  type PublishGateChecklistResult,
} from './helpers/structured-authoring-api'
import {
  getPreviewViaApi,
  openSucceededPreviewDetails,
  openTestingPreviewRunsTab,
  prepareSucceededPreviewWithComparison,
  type PreviewComparisonFixture,
} from './helpers/preview-comparison-api'

/** Docker acceptance UI (override with E2E_BASE_URL / FRONTEND_PORT). */
const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

function gateItem(checklist: PublishGateChecklistResult, checkCode: string) {
  return checklist.items.find((item) => item.checkCode === checkCode)
}

async function markWarningViewedViaApi(
  request: import('@playwright/test').APIRequestContext,
  templateId: string,
  previewId: string,
  warningIndex: number,
) {
  const login = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: E2E_TEMPLATE_AUTHOR,
  })
  expect(login.ok()).toBeTruthy()
  const token = ((await login.json()) as { result: { accessToken: string } }).result.accessToken
  const response = await request.put(
    `${E2E_API_BASE_URL}/templates/${templateId}/previews/${previewId}/fidelity-warnings/viewed`,
    {
      headers: { Authorization: `Bearer ${token}` },
      data: { warningIndex },
    },
  )
  expect(response.ok()).toBeTruthy()
  return ((await response.json()) as { result: PreviewComparisonFixture['preview'] }).result
}

/**
 * CE-U05 — fidelity viewed persistence + publish gate.
 * BDD: docs/behavior/ce-u05-fidelity-viewed-persist.md (BDD-CE-U05-FVP-001…004)
 */
test.describe('CE-U05 fidelity viewed persistence (BDD-CE-U05-FVP)', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  let fixture: PreviewComparisonFixture

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
    fixture = await prepareSucceededPreviewWithComparison(request)
    expect(fixture.preview.fidelityWarnings.some((w) => !w.viewed)).toBe(true)
  })

  test('BDD-CE-U05-FVP-002 — publish gate blocks while warnings unviewed', async ({
    request,
  }) => {
    const gate = await fetchPublishGateViaApi(request, fixture.templateId)
    const fidelityItem = gateItem(gate, 'FIDELITY_WARNINGS_VIEWED')
    expect(fidelityItem).toBeDefined()
    expect(fidelityItem?.ready).toBe(false)
    expect(fidelityItem?.blocker).toBe(true)
  })

  test('BDD-CE-U05-FVP-001 — mark viewed persists across refresh', async ({ request }) => {
    const unviewedIndex = fixture.preview.fidelityWarnings.findIndex((w) => !w.viewed)
    expect(unviewedIndex).toBeGreaterThanOrEqual(0)

    const updated = await markWarningViewedViaApi(
      request,
      fixture.templateId,
      fixture.previewId,
      unviewedIndex,
    )
    expect(updated.fidelityWarnings[unviewedIndex]?.viewed).toBe(true)

    const reloaded = await getPreviewViaApi(request, fixture.templateId, fixture.previewId)
    expect(reloaded.fidelityWarnings[unviewedIndex]?.viewed).toBe(true)
  })

  test('BDD-CE-U05-FVP-003 — publish gate ready when all warnings viewed', async ({
    request,
  }) => {
    for (let index = 0; index < fixture.preview.fidelityWarnings.length; index += 1) {
      const preview = await getPreviewViaApi(request, fixture.templateId, fixture.previewId)
      if (!preview.fidelityWarnings[index]?.viewed) {
        await markWarningViewedViaApi(request, fixture.templateId, fixture.previewId, index)
      }
    }

    const gate = await fetchPublishGateViaApi(request, fixture.templateId)
    const fidelityItem = gateItem(gate, 'FIDELITY_WARNINGS_VIEWED')
    expect(fidelityItem).toBeDefined()
    expect(fidelityItem?.ready).toBe(true)
    expect(fidelityItem?.blocker).toBe(false)
  })

  test('BDD-CE-U05-FVP-004 — human-readable warning + edit binding link in UI', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openTestingPreviewRunsTab(page, fixture.templateId, request)
    await openSucceededPreviewDetails(page)

    const warningList = page.getByTestId('fidelity-warning-list')
    await expect(warningList).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('fidelity-warning-human-message').first()).toBeVisible()
    await expect(page.getByTestId('fidelity-warning-technical-toggle').first()).toBeVisible()
    await expect(page.getByTestId('fidelity-warning-edit-binding').first()).toBeVisible()
  })
})
