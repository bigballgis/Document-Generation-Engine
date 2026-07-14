import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { expect, test } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import { assertDockerStackReady, openDevBindingEditor } from './helpers/core-fortress-f7'
import { ensureDemoRetailMasterApproved } from './helpers/masters-api'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { switchBrand } from './helpers/uiux-evidence'

/**
 * CE-U01 UIUX evidence — nested editor @1920 dual-brand.
 */

const EVIDENCE_DIR = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'evidence',
  'CE-U01-nested-editor-uiux',
)

const VIEWPORTS = [
  { brand: 'REDBC', width: 1920, height: 1080 },
  { brand: 'GREENBC', width: 1920, height: 1080 },
] as const

test.describe('CE-U01 nested editor UIUX evidence', () => {
  test.beforeAll(async ({ request }) => {
    const ready = await assertDockerStackReady(request)
    test.skip(!ready, 'Docker acceptance stack not ready')
    await ensureDemoRetailMasterApproved(request)
  })

  for (const viewport of VIEWPORTS) {
    test(`nested condition panel @${viewport.brand} ${viewport.width}px`, async ({
      page,
      request,
    }) => {
      await page.setViewportSize({ width: viewport.width, height: viewport.height })
      const fixture = await prepareDraftTemplateWithCleanBinding(request)
      await loginAs(page, E2E_ADMIN)
      await openDevBindingEditor(page, request, fixture.templateId)

      if (viewport.brand === 'GREENBC') {
        await switchBrand(page, 'GREENBC')
      }

      await page.getByTestId('insert-block-node').filter({ hasText: /^condition$/i }).click()
      const conditionNested = page.locator('[data-testid^="nested-block-children-"]').last()
      await conditionNested
        .getByTestId('insert-nested-block-node')
        .filter({ hasText: /^paragraph$/i })
        .click()

      await expect(conditionNested).toBeVisible()
      await page.screenshot({
        path: path.join(EVIDENCE_DIR, `nested-editor-${viewport.brand}-1920.png`),
        fullPage: true,
      })
    })
  }
})
