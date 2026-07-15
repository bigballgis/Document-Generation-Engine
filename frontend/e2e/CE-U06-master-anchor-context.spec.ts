import { expect, test, type Page } from '@playwright/test'

import {
  E2E_GROUP_ADMIN,
  E2E_MASTER_DESIGNER,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import {
  createDraftMasterForHubSubmit,
  createPendingReviewMasterForDecide,
  E2E_API_BASE_URL,
  getMasterDetailViaApi,
  patchMasterAnchorDisplayLabelViaApi,
  prepareDemoMasterWithReplaceHistory,
  resolveFolMasterCurrentRevisionPath,
  restoreDemoMasterToApproved,
} from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openRevisionWorkspace(page: Page, revisionPath: string, masterName: string) {
  await page.goto(revisionPath)
  await dismissOnboardingTourIfPresent(page)
  await expect(page.locator('.page-header')).toContainText(masterName, { timeout: 30_000 })
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
  await expect(page.getByTestId('master-anchor-position-overview')).toBeVisible({
    timeout: 30_000,
  })
}

function sortedByDocumentSequence<T extends { documentSequence?: number; anchorId: string }>(
  anchors: T[],
): T[] {
  return [...anchors].sort((left, right) => {
    const leftSeq = left.documentSequence ?? Number.MAX_SAFE_INTEGER
    const rightSeq = right.documentSequence ?? Number.MAX_SAFE_INTEGER
    if (leftSeq !== rightSeq) {
      return leftSeq - rightSeq
    }
    return left.anchorId.localeCompare(right.anchorId)
  })
}

/**
 * CE-U06 — master anchor visual context (ordered 1-based positions + displayLabel edit).
 * BDD: docs/behavior/ce-u06-master-anchor-context.md (BDD-CE-U06-MAC-001…007)
 */
test.describe('CE-U06 master anchor context (BDD-CE-U06-MAC)', () => {
  test.describe.configure({ mode: 'serial', timeout: 300_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-CE-U06-MAC-001 — ordered 1-based positions; no DOCX canvas', async ({
    page,
    request,
  }) => {
    const fixture = await resolveFolMasterCurrentRevisionPath(request)
    const ordered = sortedByDocumentSequence(fixture.anchors).slice(0, 3)
    expect(ordered.length).toBeGreaterThanOrEqual(2)

    await loginAs(page, E2E_GROUP_ADMIN)
    await openRevisionWorkspace(page, fixture.currentRevisionPath, fixture.name)

    const overview = page.getByTestId('master-anchor-position-overview')
    await expect(overview.getByTestId('master-anchor-position-list')).toBeVisible()

    for (let index = 0; index < ordered.length; index += 1) {
      const anchor = ordered[index]!
      const expectedPosition = (anchor.documentSequence ?? index) + 1
      const row = page.locator('tr.el-table__row').filter({
        has: page.getByTestId(`master-anchor-position-${anchor.anchorId}`),
      })
      await expect(page.getByTestId(`master-anchor-position-${anchor.anchorId}`)).toHaveText(
        String(expectedPosition),
      )
      await expect(page.getByTestId(`master-anchor-label-${anchor.anchorId}`)).toBeVisible()
      await expect(row).toContainText(anchor.anchorId)
    }

    // DOM order of position cells follows documentSequence ascending.
    const positionTexts = await Promise.all(
      ordered.map(async (anchor) =>
        (await page.getByTestId(`master-anchor-position-${anchor.anchorId}`).innerText()).trim(),
      ),
    )
    expect(positionTexts).toEqual(
      ordered.map((anchor, index) => String((anchor.documentSequence ?? index) + 1)),
    )

    const rowOrder = await page
      .locator('tr.el-table__row [data-testid^="master-anchor-position-"]')
      .evaluateAll((nodes) => nodes.slice(0, 3).map((node) => (node.textContent ?? '').trim()))
    expect(rowOrder).toEqual(['1', '2', '3'])

    await expect(page.locator('[data-testid="docx-canvas"]')).toHaveCount(0)
    await expect(page.locator('.docx-canvas, .wysiwyg-editor, iframe[src*="office"]')).toHaveCount(0)
  })

  test('BDD-CE-U06-MAC-002 — row selection highlight is single-select', async ({
    page,
    request,
  }) => {
    const fixture = await resolveFolMasterCurrentRevisionPath(request)
    const ordered = sortedByDocumentSequence(fixture.anchors)
    const first = ordered[0]!
    const second = ordered[1]!

    await loginAs(page, E2E_GROUP_ADMIN)
    await openRevisionWorkspace(page, fixture.currentRevisionPath, fixture.name)

    const firstRow = page.locator('tr.el-table__row').filter({
      has: page.getByTestId(`master-anchor-label-${first.anchorId}`),
    })
    const secondRow = page.locator('tr.el-table__row').filter({
      has: page.getByTestId(`master-anchor-label-${second.anchorId}`),
    })

    await firstRow.click()
    await expect(firstRow).toHaveClass(/master-anchor-row--selected/)
    await expect(page.locator('tr.master-anchor-row--selected')).toHaveCount(1)

    await secondRow.click()
    await expect(secondRow).toHaveClass(/master-anchor-row--selected/)
    await expect(firstRow).not.toHaveClass(/master-anchor-row--selected/)
    await expect(page.locator('tr.master-anchor-row--selected')).toHaveCount(1)
  })

  test('BDD-CE-U06-MAC-003 — writable session can edit displayLabel', async ({
    page,
    request,
  }) => {
    const fixture = await createDraftMasterForHubSubmit(request, {
      name: `E2E-CE-U06-MAC-003 ${Date.now()}`,
    })
    const detail = await getMasterDetailViaApi(request, fixture.masterId)
    const anchor = detail.anchors[0]
    expect(anchor).toBeTruthy()
    const anchorId = anchor!.anchorId
    const newLabel = `CE-U06 Label ${Date.now()}`

    await loginAs(page, E2E_MASTER_DESIGNER)
    await openRevisionWorkspace(page, fixture.currentRevisionPath, fixture.name)

    await expect(page.getByTestId(`master-anchor-edit-label-${anchorId}`)).toBeVisible()
    const positionBefore = await page.getByTestId(`master-anchor-position-${anchorId}`).innerText()

    await page.getByTestId(`master-anchor-edit-label-${anchorId}`).click()
    const dialog = page.getByTestId('master-anchor-display-label-dialog')
    await expect(dialog).toBeVisible()
    await dialog.getByTestId('master-anchor-display-label-input').fill(newLabel)
    await dialog.getByTestId('master-anchor-display-label-save').click()

    await expect(page.getByTestId(`master-anchor-label-${anchorId}`)).toHaveText(newLabel, {
      timeout: 30_000,
    })
    await expect(page.getByTestId(`master-anchor-position-${anchorId}`)).toHaveText(positionBefore)
    await expect(
      page.locator('tr.el-table__row').filter({
        has: page.getByTestId(`master-anchor-position-${anchorId}`),
      }),
    ).toContainText(anchorId)

    await page.reload()
    await dismissOnboardingTourIfPresent(page)
    await expect(page.getByTestId(`master-anchor-label-${anchorId}`)).toHaveText(newLabel, {
      timeout: 30_000,
    })

    const refreshed = await getMasterDetailViaApi(request, fixture.masterId)
    const refreshedAnchor = refreshed.anchors.find((row) => row.anchorId === anchorId)
    expect(refreshedAnchor?.displayLabel).toBe(newLabel)
  })

  test('BDD-CE-U06-MAC-005 — no manageMasters fail-closed (route + API)', async ({
    page,
    request,
  }) => {
    const fixture = await createDraftMasterForHubSubmit(request, {
      name: `E2E-CE-U06-MAC-005 ${Date.now()}`,
    })

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(fixture.currentRevisionPath)
    await dismissOnboardingTourIfPresent(page)
    await expect(page).toHaveURL(/\/forbidden/, { timeout: 15_000 })
    await expect(page.getByTestId('master-anchor-position-overview')).toHaveCount(0)
    await expect(page.locator('[data-testid^="master-anchor-edit-label-"]')).toHaveCount(0)

    const patch = await patchMasterAnchorDisplayLabelViaApi(
      request,
      E2E_TEMPLATE_AUTHOR,
      fixture.masterId,
      fixture.currentRevisionLineId,
      'HEADER',
      'Should be denied',
    )
    expect(patch.status).toBe(403)
    const errorCode =
      typeof patch.body === 'object' &&
      patch.body !== null &&
      'error' in patch.body &&
      typeof (patch.body as { error?: { code?: string } }).error?.code === 'string'
        ? (patch.body as { error: { code: string } }).error.code
        : null
    expect(errorCode).toMatch(/ACCESS_DENIED|FORBIDDEN/i)
  })

  test('BDD-CE-U06-MAC-006 — historical revision line hides edit', async ({ page, request }) => {
    const history = await prepareDemoMasterWithReplaceHistory(request)

    try {
      await loginAs(page, E2E_MASTER_DESIGNER)
      await page.goto(history.historicalRevisionPath)
      await dismissOnboardingTourIfPresent(page)
      await expect(page.getByTestId('master-anchor-position-overview')).toBeVisible({
        timeout: 30_000,
      })
      await expect(page.getByTestId('master-anchor-position-list')).toBeVisible()
      await expect(page.locator('[data-testid^="master-anchor-edit-label-"]')).toHaveCount(0)
    } finally {
      await restoreDemoMasterToApproved(request, { force: true })
    }
  })

  test('BDD-CE-U06-MAC-007 — PENDING_REVIEW hides displayLabel edit', async ({
    page,
    request,
  }) => {
    const fixture = await createPendingReviewMasterForDecide(request, {
      name: `E2E-CE-U06-MAC-007 ${Date.now()}`,
    })

    await loginAs(page, E2E_MASTER_DESIGNER)
    await openRevisionWorkspace(page, fixture.currentRevisionPath, fixture.name)

    await expect(page.getByTestId('master-anchor-position-list')).toBeVisible()
    await expect(page.locator('[data-testid^="master-anchor-edit-label-"]')).toHaveCount(0)
  })
})
