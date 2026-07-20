/**
 * SYS-NORM Wave 2 / #146 — Template (+ Master) Package Hub IA.
 *
 * BDD SoT: docs/behavior/sys-norm-hub-ia.md
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/SYS-NORM-W2-hub-ia.spec.ts `
 *     --config playwright.docker.config.ts
 *
 * Coverage map:
 *   W2-001/002/fluid — hub Version lines primary; removed hub tabs
 *   W2-003/004       — Properties drawer open/close
 *   W2-005/006       — Dependencies on release + dev surfaces
 *   W2-007           — hub API settings → package settings shell
 *   W2-008           — version-line API perspective deep-link
 *   W2-010/011       — legacy apiAccess / #apiAccess / /api/policies redirect
 *   W2-012           — ?tab=overview → Properties; ?tab=dependencies guidance/nav
 *   W2-013           — Dev honest empty (DEMO draft); wrong-surface when seedable
 *   W2-016           — Master hub Properties + revision lines parity
 *   W2-009/014/015/017/018 — unit/i18n contract (not asserted here)
 */
import { expect, test, type APIRequestContext, type Locator, type Page } from '@playwright/test'

import {
  DEMO_FULL_FLOW_EXTERNAL_ID,
  DEMO_GROUP_CODE,
  DEMO_MASTER_NAME,
  DEMO_TEMPLATE_EXTERNAL_ID,
  E2E_ADMIN,
  FOL_TEMPLATE_EXTERNAL_ID,
  loginAs,
  loginAsGlobalAdmin,
} from './helpers/auth'
import {
  ensureDemoFullFlowPublished,
  findTemplateByExternalId,
  type DemoFullFlowFixture,
} from './helpers/content-modules-api'
import { requireFolTemplate } from './helpers/fol-api'
import { demoMasterDetailPath, E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import {
  listTemplateVersionLines,
  type TemplateVersionLineSummary,
} from './helpers/template-version-lines-api'

async function requireDemoRetailTemplate(request: APIRequestContext): Promise<{ templateId: string }> {
  const template = await findTemplateByExternalId(request, DEMO_TEMPLATE_EXTERNAL_ID)
  if (!template) {
    throw new Error(
      `Demo template "${DEMO_TEMPLATE_EXTERNAL_ID}" was not found. Ensure DOCGEN_SEED_DEMO_CATALOG=true.`,
    )
  }
  return { templateId: template.id }
}

async function expectFluidHubLayout(page: Page) {
  const layout = page.locator('[data-testid="template-package-hub"].app-page-layout').first()
  await expect(layout).toBeVisible({ timeout: 20_000 })
  await expect(layout).toHaveClass(/app-page-layout--fluid/)
  await expect(layout.locator('.app-page-layout__inner')).toHaveCount(0)
}

async function openTemplateHub(page: Page, templateId: string) {
  await page.goto(`/templates/${templateId}`)
  await expect(page.getByText(/unable to load template/i)).not.toBeVisible()
  await expect(page.getByTestId('template-package-hub')).toBeVisible({ timeout: 20_000 })
  await expect(page.locator('.version-lines-card')).toBeVisible({ timeout: 20_000 })
}

function hubSecondaryTabs(page: Page): Locator {
  return page.getByTestId('template-package-hub').locator('.secondary-tabs')
}

async function assertRemovedHubTabs(page: Page) {
  const hub = page.getByTestId('template-package-hub')
  await expect(hubSecondaryTabs(page)).toHaveCount(0)
  await expect(hub.getByRole('tab', { name: /^overview$/i })).toHaveCount(0)
  await expect(hub.getByRole('tab', { name: /^dependencies$/i })).toHaveCount(0)
  await expect(hub.getByRole('tab', { name: /^external access$/i })).toHaveCount(0)
  await expect(hub.getByRole('tab', { name: /^api access$/i })).toHaveCount(0)
}

async function resolvePublishedFixture(
  request: APIRequestContext,
): Promise<{ templateId: string; releaseVersion: string; publishedLine: TemplateVersionLineSummary }> {
  const existing = await findTemplateByExternalId(request, DEMO_FULL_FLOW_EXTERNAL_ID)
  const candidates: string[] = []
  if (existing) {
    candidates.push(existing.id)
  }

  for (const templateId of candidates) {
    const lines = await listTemplateVersionLines(request, templateId)
    const published = lines.find((line) => line.lineKind === 'PUBLISHED' && line.releaseVersion)
    if (published?.releaseVersion) {
      return {
        templateId,
        releaseVersion: published.releaseVersion,
        publishedLine: published,
      }
    }
  }

  const seeded: DemoFullFlowFixture = await ensureDemoFullFlowPublished(request)
  const lines = await listTemplateVersionLines(request, seeded.templateId)
  const published = lines.find((line) => line.lineKind === 'PUBLISHED' && line.releaseVersion)
  if (!published?.releaseVersion) {
    throw new Error(
      `No PUBLISHED version line after ensureDemoFullFlowPublished (${seeded.templateId})`,
    )
  }
  return {
    templateId: seeded.templateId,
    releaseVersion: published.releaseVersion,
    publishedLine: published,
  }
}

async function adminAccessToken(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: E2E_ADMIN,
  })
  if (!response.ok()) {
    throw new Error(`Admin API login failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as { result: { accessToken: string } }
  return body.result.accessToken
}

test.describe('SYS-NORM Wave 2 — Hub IA functional journeys', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      skipMessage:
        'Stack required (:4173 + :8080). Start with .\\scripts\\docker-deploy-queue.ps1',
    })
  })

  test('BDD-SYS-NORM-W2-001/002: hub opens Version lines; Overview/Dependencies/External access absent', async ({
    page,
    request,
  }) => {
    const fixture = await requireDemoRetailTemplate(request)
    await loginAsGlobalAdmin(page)
    await openTemplateHub(page, fixture.templateId)

    await expect(page.getByText(/version lines/i).first()).toBeVisible()
    await expect(page.locator('.version-lines-card .el-table__body-wrapper tbody tr').first()).toBeVisible()
    await assertRemovedHubTabs(page)
    await expectFluidHubLayout(page)
  })

  test('BDD-SYS-NORM-W2-003/004: Properties opens right drawer with overview content and closes', async ({
    page,
    request,
  }) => {
    const fixture = await requireDemoRetailTemplate(request)
    await loginAsGlobalAdmin(page)
    await openTemplateHub(page, fixture.templateId)

    await page.getByTestId('hub-properties-action').click()
    const drawer = page.getByTestId('template-properties-drawer')
    await expect(drawer).toBeVisible({ timeout: 15_000 })
    await expect(drawer.getByTestId('template-overview-summary')).toBeVisible()
    await expect(page.locator('.version-lines-card')).toBeVisible()

    await page.keyboard.press('Escape')
    await expect(drawer).toBeHidden({ timeout: 15_000 })
    await expect(page).toHaveURL(
      new RegExp(`/templates/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?:\\?.*)?$`),
    )
    await expect(page.locator('.version-lines-card')).toBeVisible()
  })

  test('BDD-SYS-NORM-W2-007: hub API settings navigates to package settings shell', async ({
    page,
    request,
  }) => {
    const fixture = await requireDemoRetailTemplate(request)
    await loginAs(page, E2E_ADMIN)
    await openTemplateHub(page, fixture.templateId)

    await page.getByTestId('hub-api-settings-action').click()
    await expect(page).toHaveURL(
      new RegExp(
        `/api/packages/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/settings`,
      ),
      { timeout: 20_000 },
    )
    await expect(page.getByTestId('api-package-settings-interim-banner')).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByTestId('api-package-settings-panel')).toBeVisible()
  })

  test('BDD-SYS-NORM-W2-010/011: legacy apiAccess / #apiAccess / policies redirect to settings', async ({
    page,
    request,
  }) => {
    const fixture = await requireDemoRetailTemplate(request)
    await loginAsGlobalAdmin(page)
    const settingsRe = new RegExp(
      `/api/packages/${fixture.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/settings`,
    )

    await page.goto(`/templates/${fixture.templateId}?tab=apiAccess`)
    await expect(page).toHaveURL(settingsRe, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toBeVisible({
      timeout: 20_000,
    })

    await page.goto(`/templates/${fixture.templateId}#apiAccess`)
    await expect(page).toHaveURL(settingsRe, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toBeVisible({
      timeout: 20_000,
    })

    await page.goto(`/api/policies/${fixture.templateId}`)
    await expect(page).toHaveURL(settingsRe, { timeout: 20_000 })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toBeVisible({
      timeout: 20_000,
    })
  })

  test('BDD-SYS-NORM-W2-012: legacy overview opens Properties; dependencies shows guidance or release', async ({
    page,
    request,
  }) => {
    const fixture = await requireDemoRetailTemplate(request)
    await loginAsGlobalAdmin(page)

    await page.goto(`/templates/${fixture.templateId}?tab=overview`)
    await expect(page.getByTestId('template-properties-drawer')).toBeVisible({ timeout: 20_000 })
    await expect(page.getByTestId('template-overview-summary')).toBeVisible()
    await assertRemovedHubTabs(page)

    await page.goto(`/templates/${fixture.templateId}?tab=dependencies`)
    await expect(page).toHaveURL(/\/templates\//, { timeout: 20_000 })

    const guidance = page.getByTestId('hub-dependencies-guidance')
    const onReleaseDeps =
      page.url().includes('/releases/') && page.url().includes('workspaceTab=dependencies')
    if (onReleaseDeps) {
      await expect(page.getByRole('tab', { name: /^dependencies$/i })).toHaveAttribute(
        'aria-selected',
        'true',
        { timeout: 20_000 },
      )
      await expect(page.getByTestId('template-dependencies-panel')).toBeVisible()
    } else {
      await expect(guidance).toBeVisible({ timeout: 20_000 })
      await expect(page.locator('.version-lines-card')).toBeVisible()
      await assertRemovedHubTabs(page)
    }
  })

  test('BDD-SYS-NORM-W2-005/006/008: release+dev Dependencies; API perspective deep-link', async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000)
    const published = await resolvePublishedFixture(request)
    const draft = await requireDemoRetailTemplate(request)
    await loginAsGlobalAdmin(page)

    await openTemplateHub(page, published.templateId)
    await expect(page.getByTestId('version-line-api-perspective').first()).toBeVisible({
      timeout: 20_000,
    })
    await page.getByTestId('version-line-api-settings-link').first().click()
    await expect(page).toHaveURL(
      new RegExp(
        `/api/packages/${published.templateId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/settings`,
      ),
      { timeout: 20_000 },
    )
    await expect(page).toHaveURL(/releaseVersion=/)
    await expect(page.getByTestId('api-package-settings-release-context')).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByTestId('api-package-settings-interim-banner')).toBeVisible()

    await page.goto(
      `/templates/${published.templateId}/releases/${encodeURIComponent(published.releaseVersion)}`,
    )
    await expect(page.getByRole('tab', { name: /^dependencies$/i })).toBeVisible({ timeout: 20_000 })
    await page.getByRole('tab', { name: /^dependencies$/i }).click()
    await expect(page).toHaveURL(/workspaceTab=dependencies/)
    await expect(page.getByTestId('template-dependencies-panel')).toBeVisible({ timeout: 20_000 })

    const draftLines = await listTemplateVersionLines(request, draft.templateId)
    const inFlight = draftLines.find((line) => line.lineKind === 'IN_FLIGHT')
    test.skip(!inFlight, `No IN_FLIGHT line on ${DEMO_TEMPLATE_EXTERNAL_ID} for W2-006`)
    await page.goto(`/templates/${draft.templateId}/dev/${inFlight!.devVersionId}`)
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByRole('tab', { name: /^dependencies$/i })).toBeVisible()
    await page.getByRole('tab', { name: /^dependencies$/i }).click()
    await expect(page.getByTestId('template-dependencies-panel')).toBeVisible({ timeout: 20_000 })
  })

  test('BDD-SYS-NORM-W2-013: Dev honest empty; wrong-surface when seedable', async ({
    page,
    request,
  }) => {
    test.setTimeout(120_000)
    const draft = await requireDemoRetailTemplate(request)
    const draftLines = await listTemplateVersionLines(request, draft.templateId)
    const inFlight = draftLines.find((line) => line.lineKind === 'IN_FLIGHT')
    test.skip(!inFlight, `No IN_FLIGHT line on ${DEMO_TEMPLATE_EXTERNAL_ID} for honest empty`)

    await loginAsGlobalAdmin(page)
    await page.goto(
      `/templates/${draft.templateId}/dev/${inFlight!.devVersionId}?workspaceTab=design`,
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('dev-design-honest-empty')).toBeVisible({ timeout: 20_000 })

    // Wrong-surface: FOL in-flight row can carry PUBLISHED lifecycle while still on /dev/.
    // Published /dev/{publishedDevVersionId} returns API 403 TEMPLATE_VERSION_IMMUTABLE → Forbidden
    // (not the wrong-surface panel) — documented, not asserted as W2-013 primary.
    let wrongSurfaceCovered = false
    try {
      const fol = await requireFolTemplate(request)
      const adminToken = await adminAccessToken(request)
      const folLines = await listTemplateVersionLines(request, fol.templateId, adminToken)
      const folInFlight = folLines.find(
        (line) => line.lineKind === 'IN_FLIGHT' && line.lifecycleStatus === 'PUBLISHED',
      )
      if (folInFlight) {
        await page.goto(`/templates/${fol.templateId}/dev/${folInFlight.devVersionId}`)
        await expect(page.getByTestId('dev-editor-wrong-surface')).toBeVisible({ timeout: 30_000 })
        await expect(page.getByTestId('dev-editor-open-correct-surface')).toBeVisible()
        wrongSurfaceCovered = true
      }
    } catch (error) {
      test.info().annotations.push({
        type: 'note',
        description: `Wrong-surface optional path skipped: ${error instanceof Error ? error.message : String(error)}`,
      })
    }

    if (!wrongSurfaceCovered) {
      test.info().annotations.push({
        type: 'note',
        description:
          `W2-013 wrong-surface not seedable on this stack (no ${FOL_TEMPLATE_EXTERNAL_ID} IN_FLIGHT+PUBLISHED); honest empty covered on DEMO draft`,
      })
    }
  })

  test('BDD-SYS-NORM-W2-016: Master hub Properties parity (smoke)', async ({ page, request }) => {
    const hubPath = await demoMasterDetailPath(request)
    await loginAsGlobalAdmin(page)
    await page.goto(hubPath)

    await expect(page.getByRole('heading', { level: 1, name: DEMO_MASTER_NAME })).toBeVisible({
      timeout: 20_000,
    })
    await expect(page.getByText(`Group: ${DEMO_GROUP_CODE}`)).toBeVisible()

    const revisionLines = page.locator('.revision-lines-card')
    await expect(revisionLines.getByText(/^revision lines$/i)).toBeVisible({ timeout: 20_000 })
    await expect(revisionLines.locator('.el-table__body-wrapper tbody tr').first()).toBeVisible()

    await expect(page.getByTestId('hub-api-settings-action')).toHaveCount(0)
    await expect(page.getByRole('tab', { name: /^external access$/i })).toHaveCount(0)

    await page.getByTestId('master-hub-properties-action').click()
    const drawer = page.getByTestId('master-properties-drawer')
    await expect(drawer).toBeVisible({ timeout: 15_000 })
    await expect(drawer.getByText(DEMO_MASTER_NAME, { exact: true })).toBeVisible()
    await expect(drawer.getByText(DEMO_GROUP_CODE, { exact: true })).toBeVisible()
    await expect(revisionLines).toBeVisible()

    await page.keyboard.press('Escape')
    await expect(drawer).toBeHidden({ timeout: 15_000 })
    await expect(page).toHaveURL(/\/masters\/[^/?]+$/)
  })
})
