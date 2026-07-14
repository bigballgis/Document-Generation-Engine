import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { assertDockerStackReady, openDevBindingEditor } from './helpers/core-fortress-f7'
import { ensureDemoRetailMasterApproved } from './helpers/masters-api'
import { prepareDraftTemplateWithCleanBinding, upsertBindingViaApi } from './helpers/structured-authoring-api'

/**
 * CE-U02 — Block sort / copy / validate scroll (BDD-CE-U02-BLOCK-SORT-COPY-SCROLL-001).
 */

const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'CE-U02-block-sort-copy-scroll',
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

test.describe('CE-U02 block sort / copy / validate scroll', () => {
  test.beforeAll(async ({ request }) => {
    const ready = await assertDockerStackReady(request)
    test.skip(!ready, 'Docker acceptance stack not ready at :4173/:8080')
    await ensureDemoRetailMasterApproved(request)
  })

  test('BS-02: copy block duplicates paragraph text', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await loginAs(page, E2E_ADMIN)
    await openDevBindingEditor(page, request, fixture.templateId)

    await page.getByTestId('paragraph-input').first().fill('Copy me')
    await page.getByTestId('structured-block-copy').first().click()

    await expect
      .poll(async () => {
        const json = await editorJsonPreview(page)
        const parsed = JSON.parse(json) as { nodes: unknown[] }
        return parsed.nodes.length
      })
      .toBe(2)

    const json = await editorJsonPreview(page)
    expect(json.match(/Copy me/g)?.length).toBe(2)
    await page.screenshot({ path: path.join(EVIDENCE_DIR, 'BS-02-copy-block.png'), fullPage: true })
  })

  test('BS-03: validation issue scrolls block into view', async ({ page, request }) => {
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    await upsertBindingViaApi(
      request,
      fixture.templateId,
      'HEADER',
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"variable","key":"ghostVar"}]}]}',
    )
    await loginAs(page, E2E_ADMIN)
    await openDevBindingEditor(page, request, fixture.templateId)

    await page.getByTestId('structured-editor-validate-structure').click()

    const issues = page.getByTestId('structured-editor-validation-issue')
    await expect(issues.first()).toBeVisible()

    await issues.first().click()
    await expect(page.getByTestId('structured-block-card-0')).toBeInViewport({ timeout: 10_000 })
    await page.screenshot({
      path: path.join(EVIDENCE_DIR, 'BS-03-validate-scroll.png'),
      fullPage: true,
    })
  })
})
