import { expect, test } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  assertDockerStackReady,
  dirtyGuardDialog,
  mutateBindingStructure,
  openDevBindingEditor,
  triggerRouteLeaveViaNav,
} from './helpers/core-fortress-f7'
import {
  prepareDraftTemplateWithCleanBinding,
  type StructuredAuthoringFixture,
} from './helpers/structured-authoring-api'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

test.describe('CORE-FORTRESS F7 dirty guard', () => {
  test.describe.configure({ mode: 'serial', timeout: 180_000 })

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

  // BDD-F7-B1-001 — unsaved route intercept
  test('dirty binding editor blocks route leave with confirmation dialog', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)
    await mutateBindingStructure(page)

    await triggerRouteLeaveViaNav(page)

    const dialog = dirtyGuardDialog(page)
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('heading', { name: /unsaved changes/i })).toBeVisible()
    await expect(page).toHaveURL(/\/dev\//)
  })

  // BDD-F7-B1-002 — Stay preserves edit state
  test('Stay keeps current route and dirty editor state', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)
    await mutateBindingStructure(page)

    await page.getByRole('button', { name: /^back$/i }).click()

    const dialog = dirtyGuardDialog(page)
    await expect(dialog).toBeVisible()
    await dialog.getByTestId('dirty-guard-stay').click()

    await expect(dialog).not.toBeVisible()
    await expect(page).toHaveURL(/\/dev\//)
    await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
    await expect(page.getByTestId('insert-block-node').filter({ hasText: /^paragraph$/i })).toBeVisible()
  })

  // BDD-F7-B1-003 — Discard abandons and returns to bindings list
  test('Discard abandons unsaved binding edits and exits editor', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)
    await mutateBindingStructure(page)

    await page.getByRole('button', { name: /^back$/i }).click()

    const dialog = dirtyGuardDialog(page)
    await expect(dialog).toBeVisible()
    await dialog.getByTestId('dirty-guard-discard').click()

    await expect(dialog).not.toBeVisible({ timeout: 15_000 })
    await expect(page.locator('.bindings-panel .el-table')).toBeVisible()
    await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0)
  })

  // BDD-F7-B1-004 — Save clears dirty and allows subsequent navigation
  test('Save persists binding then allows route leave', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)
    await mutateBindingStructure(page)

    await triggerRouteLeaveViaNav(page)

    const dialog = dirtyGuardDialog(page)
    await expect(dialog).toBeVisible()

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${fixture.templateId}/bindings/HEADER`),
      { timeout: 60_000 },
    )

    await dialog.getByTestId('dirty-guard-save').click()

    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })

    await triggerRouteLeaveViaNav(page)
    await expect(dirtyGuardDialog(page)).toHaveCount(0)
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 })
  })

  // BDD-F7-B1-005 — pristine navigation has no friction
  test('pristine binding editor navigates away without confirmation', async ({ page, request }) => {
    await openDevBindingEditor(page, request, fixture.templateId)

    await triggerRouteLeaveViaNav(page)

    await expect(dirtyGuardDialog(page)).toHaveCount(0)
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 })
  })
})
