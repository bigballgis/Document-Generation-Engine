import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { assertDockerStackReady, openDevBindingEditor } from './helpers/core-fortress-f7'
import { ensureDemoRetailMasterApproved } from './helpers/masters-api'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'

/**
 * CE-U01 — Nested structured editor (BDD-CE-U01-NESTED-EDITOR-001).
 *
 *   pnpm -C frontend exec playwright test e2e/ce-u01-nested-editor.spec.ts \
 *     --config playwright.docker.config.ts --workers=1
 */

const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'CE-U01-nested-editor',
)

async function editorJsonPreview(page: Page): Promise<string> {
  const details = page.locator('details.json-preview')
  await expect(details).toBeAttached()
  await details.evaluate((el) => {
    ;(el as HTMLDetailsElement).open = true
  })
  const preview = details.locator('pre')
  await expect(preview).toBeVisible()
  return (await preview.textContent()) ?? ''
}

test.describe('CE-U01 nested structured editor', () => {
  test.beforeAll(async ({ request }) => {
    const ready = await assertDockerStackReady(request)
    test.skip(!ready, 'Docker acceptance stack not ready at :4173/:8080')
    await ensureDemoRetailMasterApproved(request)
  })

  test('NE-02/NE-05: add paragraph inside condition and preview pane stays visible', async ({
    page,
    request,
  }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await loginAs(page, E2E_ADMIN)
    await openDevBindingEditor(page, request, fixture.templateId)

    await page.getByTestId('insert-block-node').filter({ hasText: /^condition$/i }).click()
    const conditionNested = page.locator('[data-testid^="nested-block-children-"]').last()
    await expect(conditionNested).toBeVisible()

    await conditionNested
      .getByTestId('insert-nested-block-node')
      .filter({ hasText: /^paragraph$/i })
      .click()

    await conditionNested
      .locator('[data-testid^="structured-block-card-"]')
      .getByTestId('paragraph-input')
      .fill('Nested clause body')

    await expect
      .poll(async () => {
        const json = await editorJsonPreview(page)
        return json.includes('Nested clause body') && json.includes('"type":"conditionBlock"')
      })
      .toBe(true)

    await expect(page.getByTestId('authoring-preview-pane')).toBeVisible()
    await page.screenshot({ path: path.join(EVIDENCE_DIR, 'NE-02-nested-paragraph.png'), fullPage: true })
  })

  test('NE-04: undo removes nested paragraph inside condition', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await loginAs(page, E2E_ADMIN)
    await openDevBindingEditor(page, request, fixture.templateId)

    await page.getByTestId('insert-block-node').filter({ hasText: /^condition$/i }).click()
    const conditionNested = page.locator('[data-testid^="nested-block-children-"]').last()
    await conditionNested
      .getByTestId('insert-nested-block-node')
      .filter({ hasText: /^paragraph$/i })
      .click()

    await expect
      .poll(async () => {
        const parsed = JSON.parse(await editorJsonPreview(page)) as {
          nodes: Array<{ type: string; children?: Array<{ type: string }> }>
        }
        const condition = parsed.nodes.find((node) => node.type === 'conditionBlock')
        return condition?.children?.some((child) => child.type === 'paragraph') ?? false
      })
      .toBe(true)

    await page.getByTestId('structured-editor-undo').click()

    await expect
      .poll(async () => {
        const parsed = JSON.parse(await editorJsonPreview(page)) as {
          nodes: Array<{ type: string; children?: Array<{ type: string }> }>
        }
        const condition = parsed.nodes.find((node) => node.type === 'conditionBlock')
        return (condition?.children?.length ?? 0) === 0
      })
      .toBe(true)

    await page.screenshot({ path: path.join(EVIDENCE_DIR, 'NE-04-undo-nested.png'), fullPage: true })
  })
})
