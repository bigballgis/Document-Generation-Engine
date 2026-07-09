import { expect, type APIRequestContext, type Page } from '@playwright/test'

import { isDockerStackReady } from './helpers/stack-readiness'

import { listTemplateVersionLines } from './template-version-lines-api'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'

export async function assertDockerStackReady(request: APIRequestContext): Promise<boolean> {
  return isDockerStackReady(request, { frontendBaseUrl: FRONTEND_BASE_URL })
}

export function dirtyGuardDialog(page: Page) {
  return page.getByTestId('dirty-guard-dialog')
}

export async function openTemplateBindingEditor(
  page: Page,
  templateId: string,
  anchorId = 'HEADER',
): Promise<void> {
  await page.goto(`/templates/${templateId}?tab=authoring&authoringTab=bindings`)
  await expect(page.getByRole('heading', { name: /template authoring/i })).toBeVisible({
    timeout: 30_000,
  })

  const row = page.locator('.bindings-panel .el-table__row').filter({ hasText: anchorId })
  await expect(row).toBeVisible({ timeout: 30_000 })
  await row.getByRole('button', { name: /^edit$/i }).click()

  await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
  await expect(page.getByTestId('authoring-side-by-side-layout')).toBeVisible()
}

export async function openDevBindingEditor(
  page: Page,
  request: APIRequestContext,
  templateId: string,
  anchorId = 'HEADER',
  options?: { expectPreviewPane?: boolean },
): Promise<void> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight) {
    throw new Error(`No in-flight dev version for template ${templateId}`)
  }

  await page.goto(
    `/templates/${templateId}/dev/${inFlight.devVersionId}?workspaceTab=design&designTab=bindings`,
  )
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })

  const row = page.locator('.bindings-panel .el-table__row').filter({ hasText: anchorId })
  await expect(row).toBeVisible({ timeout: 30_000 })
  await row.getByRole('button', { name: /^edit$/i }).click()

  await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
  await expect(page.getByTestId('authoring-side-by-side-layout')).toBeVisible()
  if (options?.expectPreviewPane !== false) {
    await expect(page.getByTestId('authoring-preview-pane')).toBeVisible()
  }
}

export async function mutateBindingStructure(page: Page): Promise<void> {
  await page
    .getByTestId('insert-block-node')
    .filter({ hasText: /^paragraph$/i })
    .click()
}

export async function triggerRouteLeaveViaNav(page: Page): Promise<void> {
  await page
    .getByRole('navigation', { name: /management navigation/i })
    .getByRole('button', { name: /^my tasks$/i })
    .click()
}
