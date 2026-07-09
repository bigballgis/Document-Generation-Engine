import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  assertDockerStackReady,
  mutateBindingStructure,
  openDevBindingEditor,
} from './helpers/core-fortress-f7'
import {
  prepareDraftTemplateWithCleanBinding,
  type StructuredAuthoringFixture,
} from './helpers/structured-authoring-api'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('CORE-FORTRESS F7 side-by-side preview', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let fixture: StructuredAuthoringFixture

  test.beforeAll(async ({ request }) => {
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1`,
    )

    fixture = await prepareDraftTemplateWithCleanBinding(request)
  })

  test.beforeEach(async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
  })

  // BDD-F7-B2-001 — wide layout shows editor and preview together
  test('binding editor shows side-by-side layout with editor and preview panes', async ({
    page,
    request,
  }) => {
    await openDevBindingEditor(page, request, fixture.templateId)

    const layout = page.getByTestId('authoring-side-by-side-layout')
    await expect(layout).toBeVisible()
    await expect(page.getByTestId('authoring-editor-pane')).toBeVisible()
    await expect(page.getByTestId('authoring-preview-pane')).toBeVisible()
    await expect(layout).not.toHaveClass(/authoring-side-by-side--stacked/)
  })

  // BDD-F7-B2-002 — narrow viewport stacks preview below editor
  test.describe('narrow viewport', () => {
    test.use({ viewport: { width: 375, height: 812 } })

    test('stacks preview with collapse toggle', async ({ page, request }) => {
      await openDevBindingEditor(page, request, fixture.templateId, 'HEADER', {
        expectPreviewPane: false,
      })

      const layout = page.getByTestId('authoring-side-by-side-layout')
      await expect(layout).toHaveClass(/authoring-side-by-side--stacked/)
      await expect(page.getByTestId('authoring-preview-toggle')).toBeVisible()

      const boundary = page.getByTestId('authoring-preview-boundary')
      await boundary.scrollIntoViewIfNeeded()
      await expect(boundary).toBeVisible()

      await page.getByTestId('authoring-preview-toggle').click()
      await expect(boundary).not.toBeVisible()

      await page.getByTestId('authoring-preview-toggle').click()
      await expect(boundary).toBeVisible()
    })
  })

  // BDD-F7-B2-007 — empty preview state before first refresh
  test('preview pane shows empty state before first refresh', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)

    await expect(page.getByTestId('authoring-preview-empty')).toBeVisible()
    await expect(page.getByText(/no preview yet/i)).toBeVisible()
    await expect(page.getByTestId('authoring-preview-refresh')).toBeEnabled()
    await expect(page.getByTestId('authoring-preview-stale-badge')).toHaveCount(0)
  })

  // BDD-F7-B2-005 — CD-PIT-08 boundary copy visible
  test('preview pane shows non-authoritative boundary copy', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)

    const boundary = page.getByTestId('authoring-preview-boundary')
    await expect(boundary).toBeVisible()
    await expect(boundary).toContainText(/guidance only/i)
    await expect(boundary).toContainText(/not legal evidence/i)
  })

  // BDD-F7-B2-003 — structure mutation marks preview stale
  test('structure mutation shows stale badge after preview exists', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)

    const refreshButton = page.getByTestId('authoring-preview-refresh')
    const refreshResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-generate') &&
        response.ok(),
      { timeout: 120_000 },
    )

    await refreshButton.click()
    await refreshResponsePromise
    await expect(page.locator('.el-message').getByText(/test generation started|preview/i).first()).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByTestId('authoring-preview-empty')).toHaveCount(0, { timeout: 60_000 })
    await expect(page.getByTestId('authoring-preview-stale-badge')).toHaveCount(0)

    await mutateBindingStructure(page)

    await expect(page.getByTestId('authoring-preview-stale-badge')).toBeVisible()
    await expect(page.getByText(/preview out of date/i)).toBeVisible()
    await expect(refreshButton).toBeEnabled()
  })

  // BDD-F7-B2-004 — explicit refresh clears stale badge
  test('Refresh now clears stale badge and updates preview artifact', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)

    const refreshButton = page.getByTestId('authoring-preview-refresh')

    const initialRefresh = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-generate') &&
        response.ok(),
      { timeout: 120_000 },
    )
    await refreshButton.click()
    await initialRefresh
    await expect(page.getByTestId('authoring-preview-empty')).toHaveCount(0, { timeout: 60_000 })

    await mutateBindingStructure(page)
    await expect(page.getByTestId('authoring-preview-stale-badge')).toBeVisible()

    const staleRefresh = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-generate') &&
        response.ok(),
      { timeout: 120_000 },
    )
    await refreshButton.click()
    await staleRefresh

    await expect(page.getByTestId('authoring-preview-stale-badge')).toHaveCount(0, {
      timeout: 60_000,
    })
    await expect(page.locator('.el-message').getByText(/test generation started|preview/i).first()).toBeVisible({
      timeout: 30_000,
    })
  })

  // BDD-F7-B2-006 — no duplicate in-flight refresh submissions
  test('Refresh button stays disabled while preview generation is in flight', async ({
    page,
    request,
  }) => {
    await openDevBindingEditor(page, request, fixture.templateId)

    const refreshButton = page.getByTestId('authoring-preview-refresh')
    await refreshButton.click()

    await expect(refreshButton).toBeDisabled({ timeout: 5_000 })

    await page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        response.url().includes('/test-generate'),
      { timeout: 120_000 },
    )

    await expect(refreshButton).toBeEnabled({ timeout: 120_000 })
  })
})
