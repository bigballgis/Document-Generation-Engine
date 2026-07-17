/**
 * CE-U19 — Dependency read-only view (Package Hub Dependencies tab)
 * BDD: docs/behavior/ce-u19-dependency-readonly-view.md (BDD-CE-U19-DRV-001…012)
 *
 * Functional journeys only — UIUX evidence is stage 7 (e2e-uiux-reviewer).
 */
import { expect, test, type Page } from '@playwright/test'

import {
  E2E_CORP_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_AUTHOR,
  loginAs,
} from './helpers/auth'
import { preparePublishedTemplateWithLockedReference } from './helpers/content-modules-api'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  prepareDraftTemplateWithCleanBinding,
  prepareEmptyDraftTemplate,
} from './helpers/structured-authoring-api'
import {
  assertCrossGroupVersionLineAccessDenied,
  cloneReleaseVersion as cloneReleaseVersionApi,
  listTemplateVersionLines,
} from './helpers/template-version-lines-api'

/** Docker acceptance UI on :4173 (override with E2E_BASE_URL). */
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

async function openDependenciesTab(page: Page, templateId: string): Promise<void> {
  await page.goto(`/templates/${templateId}?tab=dependencies`)
  await expect(page).toHaveURL(new RegExp(`tab=dependencies`))
  await expect(page.getByTestId('template-dependencies-panel')).toBeVisible({ timeout: 30_000 })
}

function dependenciesPanel(page: Page) {
  return page.getByTestId('template-dependencies-panel')
}

function hubSecondaryTabs(page: Page) {
  return page.locator('.workspace-tab-shell, .secondary-tabs').first()
}

/**
 * CE-U19 functional journeys (DRV-001…011; DRV-012 functional half via this suite).
 */
test.describe('CE-U19 dependency read-only view (BDD-CE-U19-DRV)', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('BDD-CE-U19-DRV-001 — Hub exposes Dependencies tab and deep link', async ({
    page,
    request,
  }) => {
    const fixture = await prepareEmptyDraftTemplate(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(`/templates/${fixture.templateId}`)
    await expect(page.getByText(/unable to load template/i)).not.toBeVisible()

    const depsTab = hubSecondaryTabs(page).getByRole('tab', { name: /^dependencies$/i })
    await expect(depsTab).toBeVisible({ timeout: 15_000 })

    await depsTab.click()
    await expect(page).toHaveURL(/tab=dependencies/)
    await expect(dependenciesPanel(page)).toBeVisible({ timeout: 30_000 })

    await openDependenciesTab(page, fixture.templateId)
    await expect(depsTab).toHaveAttribute('aria-selected', 'true')
  })

  test('BDD-CE-U19-DRV-002 — Published master pin visible', async ({ page, request }) => {
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDependenciesTab(page, fixture.templateId)

    await expect(page.getByTestId('template-dependencies-master-section')).toBeVisible()
    await expect(page.getByTestId('template-dependencies-master-link')).toBeVisible()
    await expect(page.getByTestId('template-dependencies-pinned')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('template-dependencies-pin-revision-id')).not.toBeEmpty()
    await expect(page.getByTestId('template-dependencies-not-pinned')).toHaveCount(0)
    await expect(dependenciesPanel(page).getByRole('button', { name: /save|publish|clone/i })).toHaveCount(
      0,
    )
  })

  test('BDD-CE-U19-DRV-003 — In-flight not pinned until publish', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDependenciesTab(page, fixture.templateId)

    await expect(page.getByTestId('template-dependencies-not-pinned')).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByTestId('template-dependencies-not-pinned')).toContainText(
      /not pinned until publish/i,
    )
    await expect(page.getByTestId('template-dependencies-pinned')).toHaveCount(0)

    const working = page.getByTestId('template-dependencies-working-revision')
    if (await working.isVisible().catch(() => false)) {
      await expect(working).toContainText(/not pinned/i)
      await expect(working).not.toContainText(/^Pinned/i)
    }
  })

  test('BDD-CE-U19-DRV-004 — Anchors list from bindings (read-only)', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDependenciesTab(page, fixture.templateId)

    const table = page.getByTestId('template-dependencies-anchors-table')
    await expect(table).toBeVisible({ timeout: 30_000 })
    await expect(table.getByText('HEADER', { exact: true })).toBeVisible()
    await expect(table.getByText('TEXT', { exact: true })).toBeVisible()
    await expect(dependenciesPanel(page).getByRole('button', { name: /save|delete|upsert/i })).toHaveCount(
      0,
    )
  })

  test('BDD-CE-U19-DRV-005 — Anchors honest empty state', async ({ page, request }) => {
    const fixture = await prepareEmptyDraftTemplate(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDependenciesTab(page, fixture.templateId)

    await expect(page.getByTestId('template-dependencies-anchors-empty')).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByTestId('template-dependencies-anchors-table')).toHaveCount(0)
    await expect(page.getByTestId('template-dependencies-anchors-section')).not.toContainText(
      /failed to load|unable to load|retry/i,
    )
  })

  test('BDD-CE-U19-DRV-006 — Clause versions read-only list', async ({ page, request }) => {
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDependenciesTab(page, fixture.templateId)

    const table = page.getByTestId('template-dependencies-clauses-table')
    await expect(table).toBeVisible({ timeout: 30_000 })
    await expect(table.getByText(fixture.referenceKey, { exact: true })).toBeVisible()
    await expect(table.getByText(fixture.semanticVersion, { exact: true })).toBeVisible()
    await expect(dependenciesPanel(page).getByRole('button', { name: /upsert|bump|save/i })).toHaveCount(
      0,
    )
  })

  test('BDD-CE-U19-DRV-007 — Release lines summary + published navigation', async ({
    page,
    request,
  }) => {
    const fixture = await preparePublishedTemplateWithLockedReference(request)
    await cloneReleaseVersionApi(request, fixture.templateId, '1.0.0', 201)

    const lines = await listTemplateVersionLines(request, fixture.templateId)
    expect(lines.some((l) => l.lineKind === 'IN_FLIGHT')).toBeTruthy()
    expect(lines.some((l) => l.lineKind === 'PUBLISHED')).toBeTruthy()

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDependenciesTab(page, fixture.templateId)

    const releaseSection = page.getByTestId('template-dependencies-release-lines-section')
    await expect(page.getByTestId('template-dependencies-release-lines-table')).toBeVisible({
      timeout: 30_000,
    })
    await expect(releaseSection.getByText('IN_FLIGHT', { exact: true }).first()).toBeVisible()
    await expect(releaseSection.getByText('PUBLISHED', { exact: true }).first()).toBeVisible()
    await expect(releaseSection.getByRole('button', { name: /clone|abandon/i })).toHaveCount(0)

    await releaseSection
      .getByTestId('template-dependencies-release-line-link')
      .filter({ hasText: /release\s*1\.0\.0|1\.0\.0/i })
      .first()
      .click()

    await expect(page).toHaveURL(
      new RegExp(
        `/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/releases/1\\.0\\.0`,
      ),
      { timeout: 15_000 },
    )
  })

  test('BDD-CE-U19-DRV-008 — Fail-closed cross-group', async ({ page, request }) => {
    const fixture = await preparePublishedTemplateWithLockedReference(request)
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const published = lines.find((l) => l.lineKind === 'PUBLISHED' && l.releaseVersion === '1.0.0')
    expect(published?.devVersionId).toBeTruthy()

    await assertCrossGroupVersionLineAccessDenied(
      request,
      fixture.templateId,
      published!.devVersionId,
      '1.0.0',
    )

    const corpLogin = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
      data: E2E_CORP_TEMPLATE_AUTHOR,
    })
    expect(corpLogin.ok()).toBeTruthy()
    const corpToken = ((await corpLogin.json()) as { result: { accessToken: string } }).result
      .accessToken
    const refs = await request.get(
      `${E2E_API_BASE_URL}/templates/${fixture.templateId}/content-module-references`,
      { headers: { Authorization: `Bearer ${corpToken}` } },
    )
    expect(refs.status()).toBe(403)
    const refsBody = (await refs.json()) as { error?: { code?: string }; result?: unknown }
    expect(refsBody.error?.code).toBe('ACCESS_DENIED')
    expect(refsBody.result).toBeUndefined()

    await loginAs(page, E2E_CORP_TEMPLATE_AUTHOR)
    await page.goto(`/templates/${fixture.templateId}?tab=dependencies`)
    await expect(page.getByText(/unable to load template|access denied|forbidden/i)).toBeVisible({
      timeout: 15_000,
    })
    await expect(dependenciesPanel(page)).toHaveCount(0)
  })

  test('BDD-CE-U19-DRV-009 — Load failure is not fake empty', async ({ page, request }) => {
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.route(`**/templates/${fixture.templateId}/content-module-references**`, (route) =>
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          metadata: { traceId: 'e2e-ce-u19-clause-fail' },
          error: {
            code: 'INTERNAL_ERROR',
            category: 'SYSTEM',
            messageKey: 'api.error.internal',
            retryable: true,
          },
        }),
      }),
    )

    await openDependenciesTab(page, fixture.templateId)

    await expect(page.getByTestId('template-dependencies-clauses-error')).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByTestId('template-dependencies-clauses-empty')).toHaveCount(0)
    await expect(
      page.getByTestId('template-dependencies-clauses-section').getByRole('button', {
        name: /^retry$/i,
      }),
    ).toBeVisible()
  })

  test('BDD-CE-U19-DRV-010 — No write CTAs on Dependencies tab', async ({ page, request }) => {
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openDependenciesTab(page, fixture.templateId)

    const panel = dependenciesPanel(page)
    await expect(panel).toBeVisible()
    // Write CTAs must not appear inside Dependencies (U19-D8/D13 — hub Version lines may still clone).
    await expect(panel.getByRole('button', { name: /save|publish|clone|abandon|upsert|bump/i })).toHaveCount(
      0,
    )
    await expect(panel.locator('[data-version-line-clone]')).toHaveCount(0)
  })

  test('BDD-CE-U19-DRV-011 — Overview and Version lines coexist', async ({ page, request }) => {
    const fixture = await preparePublishedTemplateWithLockedReference(request)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await page.goto(`/templates/${fixture.templateId}?tab=overview`)
    await expect(hubSecondaryTabs(page).getByRole('tab', { name: /^overview$/i })).toHaveAttribute(
      'aria-selected',
      'true',
      { timeout: 15_000 },
    )
    await expect(page.locator('.version-lines-card')).toBeVisible()

    await hubSecondaryTabs(page).getByRole('tab', { name: /^dependencies$/i }).click()
    await expect(dependenciesPanel(page)).toBeVisible({ timeout: 30_000 })

    await hubSecondaryTabs(page).getByRole('tab', { name: /^overview$/i }).click()
    await expect(page.locator('.version-lines-card')).toBeVisible()
    await expect(dependenciesPanel(page)).toHaveCount(0)
  })
})
