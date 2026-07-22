/**
 * CE-U21 UIUX evidence — per-anchor draft recovery banner + binding 409 conflict MessageBox
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/ce-u21-draft-anchor-concurrency.md (DAC-002 / DAC-007 visual surfaces)
 *
 * Canonical run (after stage 5 DEPLOY_OK):
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test `
 *     e2e/a11y-smoke.spec.ts e2e/CE-U21-draft-anchor-concurrency-uiux-evidence.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { E2E_ADMIN, loginAs } from './helpers/auth'
import {
  assertDockerStackReady,
  dirtyGuardDialog,
  mutateBindingStructure,
  openDevBindingEditor,
} from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import {
  getBindingUpdatedAtViaApi,
  prepareDraftTemplateWithCleanBinding,
  prepareDualAnchorFolDraftTemplate,
  upsertBindingViaApi,
} from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  captureCeU21LocatorScreenshot,
  captureCeU21Screenshot,
  CE_U21_VIEWPORT,
  ensureCeU21EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const DRAFT_KEY_PREFIX = 'docgen.structuredDraft.v1:'

/** Concurrent server payload — mirrors functional CE-U21 spec. */
const CONCURRENT_JSON = JSON.stringify({
  schemaVersion: '1.0',
  nodes: [
    {
      type: 'paragraph',
      children: [{ type: 'textRun', value: 'Concurrent session saved content' }],
    },
  ],
})

function recoveryBanner(page: Page) {
  return page.getByTestId('structured-draft-recovery-banner')
}

function bindingVersionConflictDialog(page: Page) {
  return page.locator('.el-message-box').filter({ hasText: /binding updated elsewhere/i })
}

async function resolveDevVersionId(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  if (!inFlight) {
    throw new Error(`No in-flight dev version for template ${templateId}`)
  }
  return inFlight.devVersionId
}

function draftStorageKey(
  userId: string,
  templateId: string,
  devVersionId: string,
  anchorId?: string,
): string {
  const base = `${DRAFT_KEY_PREFIX}${userId}:${templateId}:${devVersionId}`
  return anchorId ? `${base}:${anchorId}` : base
}

async function waitForLocalDraft(page: Page, key: string): Promise<void> {
  await expect
    .poll(
      async () =>
        page.evaluate((storageKey) => {
          const raw = localStorage.getItem(storageKey)
          if (!raw) {
            return null
          }
          try {
            const parsed = JSON.parse(raw) as {
              structureJson?: string
              draftUpdatedAt?: string
            }
            if (
              typeof parsed.structureJson !== 'string' ||
              typeof parsed.draftUpdatedAt !== 'string'
            ) {
              return null
            }
            return parsed.draftUpdatedAt
          } catch {
            return null
          }
        }, key),
      { timeout: 15_000, intervals: [200, 400, 500] },
    )
    .not.toBeNull()
}

async function clearDraftKeys(page: Page, keys: string[]): Promise<void> {
  await page.evaluate((storageKeys) => {
    for (const key of storageKeys) {
      localStorage.removeItem(key)
    }
  }, keys)
}

async function leaveEditorKeepingLocalDraft(page: Page): Promise<void> {
  await page.getByRole('button', { name: /^back$/i }).click()
  const dialog = dirtyGuardDialog(page)
  await expect(dialog).toBeVisible({ timeout: 15_000 })
  await dialog.getByTestId('dirty-guard-discard').click()
  await expect(dialog).not.toBeVisible({ timeout: 15_000 })
  await expect(page.getByTestId('controlled-structured-content-editor')).toHaveCount(0)
}

async function reopenBindingEditor(page: Page, anchorId: string): Promise<void> {
  const row = page.locator('.bindings-panel .el-table__row').filter({ hasText: anchorId })
  await expect(row).toBeVisible({ timeout: 30_000 })
  await row.getByRole('button', { name: /^edit$/i }).click()
  await expect(page.getByTestId('controlled-structured-content-editor')).toBeVisible()
}

async function assertNoViewportOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement
    return {
      scrollWidth: doc.scrollWidth,
      clientWidth: doc.clientWidth,
    }
  })
  expect(
    overflow.scrollWidth,
    `horizontal overflow: scrollWidth=${overflow.scrollWidth} clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

async function expectNoCriticalAxeViolations(
  page: Page,
  label: string,
  includeSelector?: string,
): Promise<void> {
  let builder = new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
  if (includeSelector) {
    builder = builder.include(includeSelector)
    // Shared toolbar style picker — pre-existing; exclude for CE-U21 visual gate (same as CE-U20).
    builder = builder.exclude('[data-testid=style-picker]')
  }
  const results = await builder.analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

test.describe('CE-U21 draft-anchor concurrency UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ timeout: 240_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeU21EvidenceDirs()
    const stackReady = await assertDockerStackReady(request)
    test.skip(
      !stackReady,
      `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    )
  })

  test('01–02 dual-brand: per-anchor recovery banner in binding editor (DAC-002)', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U21_VIEWPORT)

    const fixture = await prepareDualAnchorFolDraftTemplate(request)
    const devVersionId = await resolveDevVersionId(request, fixture.templateId)
    const keyA = draftStorageKey(
      E2E_ADMIN.username,
      fixture.templateId,
      devVersionId,
      fixture.anchorA,
    )

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await clearDraftKeys(page, [keyA])
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openDevBindingEditor(page, request, fixture.templateId, fixture.anchorA, {
      expectPreviewPane: false,
    })
    await mutateBindingStructure(page)
    await waitForLocalDraft(page, keyA)
    await leaveEditorKeepingLocalDraft(page)
    await reopenBindingEditor(page, fixture.anchorA)

    const banner = recoveryBanner(page)
    await expect(banner).toBeVisible({ timeout: 15_000 })
    await banner.scrollIntoViewIfNeeded()
    await expect(banner.getByText(/unsaved local draft found/i)).toBeVisible()
    await expect(banner.getByTestId('structured-draft-recovery-banner-restore')).toBeVisible()
    await expect(banner.getByTestId('structured-draft-recovery-banner-discard')).toBeVisible()
    await assertNoViewportOverflow(page)

    await captureCeU21Screenshot(page, '01-recovery-banner-editor-redbc-1920x1080.png')
    await captureCeU21LocatorScreenshot(banner, '01b-recovery-banner-crop-redbc-1920x1080.png')
    await captureCeU21LocatorScreenshot(
      page.getByTestId('controlled-structured-content-editor'),
      '01c-binding-editor-context-redbc-1920x1080.png',
    )
    await captureCeU21LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01d-brand-header-redbc-crop.png',
    )

    await expectNoCriticalAxeViolations(
      page,
      'CE-U21 recovery banner REDBC',
      '[data-testid=structured-draft-recovery-banner]',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(banner).toBeVisible()
    await banner.scrollIntoViewIfNeeded()
    await assertNoViewportOverflow(page)

    await captureCeU21Screenshot(page, '02-recovery-banner-editor-greenbc-1920x1080.png')
    await captureCeU21LocatorScreenshot(banner, '02b-recovery-banner-crop-greenbc-1920x1080.png')
    await captureCeU21LocatorScreenshot(
      page.getByTestId('controlled-structured-content-editor'),
      '02c-binding-editor-context-greenbc-1920x1080.png',
    )
    await captureCeU21LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02d-brand-header-greenbc-crop.png',
    )

    const restore = banner.getByTestId('structured-draft-recovery-banner-restore')
    await expect(restore).toBeVisible()
    // Primary Restore should pick up GREENBC brand token
    await expect(restore).toHaveCSS('background-color', 'rgb(0, 132, 127)')
  })

  test('03–04 dual-brand: binding version conflict MessageBox (DAC-007)', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U21_VIEWPORT)

    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveDevVersionId(request, fixture.templateId)
    const keyHeader = draftStorageKey(
      E2E_ADMIN.username,
      fixture.templateId,
      devVersionId,
      'HEADER',
    )

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await clearDraftKeys(page, [keyHeader])
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openDevBindingEditor(page, request, fixture.templateId, 'HEADER')
    await mutateBindingStructure(page)
    await waitForLocalDraft(page, keyHeader)

    const serverToken = await getBindingUpdatedAtViaApi(request, fixture.templateId, 'HEADER')
    await upsertBindingViaApi(request, fixture.templateId, 'HEADER', CONCURRENT_JSON, {
      expectedUpdatedAt: serverToken,
      credentials: E2E_ADMIN,
    })

    const conflictResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${fixture.templateId}/bindings/HEADER`) &&
        response.status() === 409,
      { timeout: 60_000 },
    )
    await page.getByTestId('binding-editor-save').click()
    await conflictResponsePromise

    const dialog = bindingVersionConflictDialog(page)
    await expect(dialog).toBeVisible({ timeout: 15_000 })
    await expect(dialog.getByText(/binding updated elsewhere/i)).toBeVisible()
    await expect(
      dialog.getByText(/this binding was updated elsewhere\. reload the binding/i),
    ).toBeVisible()
    await expect(dialog.getByRole('button', { name: /^reload$/i })).toBeVisible()
    await expect(dialog.getByRole('button', { name: /^keep editing$/i })).toBeVisible()
    // Must not reuse publish version-conflict copy
    await expect(page.getByText(/publish version conflict/i)).toHaveCount(0)
    await assertNoViewportOverflow(page)

    await captureCeU21Screenshot(page, '03-conflict-dialog-redbc-1920x1080.png')
    await captureCeU21LocatorScreenshot(dialog, '03b-conflict-dialog-crop-redbc-1920x1080.png')
    await captureCeU21LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03c-brand-header-redbc-crop.png',
    )

    await expectNoCriticalAxeViolations(page, 'CE-U21 conflict MessageBox REDBC', '.el-message-box')

    await dialog.getByRole('button', { name: /^keep editing$/i }).click()
    await expect(dialog).toHaveCount(0)

    // Re-trigger conflict for GREENBC capture (server still ahead; local still stale)
    await mutateBindingStructure(page)
    await waitForLocalDraft(page, keyHeader)

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')

    const conflict2Promise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        response.url().includes(`/templates/${fixture.templateId}/bindings/HEADER`) &&
        response.status() === 409,
      { timeout: 60_000 },
    )
    await page.getByTestId('binding-editor-save').click()
    await conflict2Promise

    const dialogGreen = bindingVersionConflictDialog(page)
    await expect(dialogGreen).toBeVisible({ timeout: 15_000 })
    await expect(dialogGreen.getByRole('button', { name: /^reload$/i })).toBeVisible()
    await expect(dialogGreen.getByRole('button', { name: /^keep editing$/i })).toBeVisible()
    await expect(page.getByText(/publish version conflict/i)).toHaveCount(0)
    await assertNoViewportOverflow(page)

    await captureCeU21Screenshot(page, '04-conflict-dialog-greenbc-1920x1080.png')
    await captureCeU21LocatorScreenshot(
      dialogGreen,
      '04b-conflict-dialog-crop-greenbc-1920x1080.png',
    )
    await captureCeU21LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04c-brand-header-greenbc-crop.png',
    )

    // Warning MessageBox — confirm primary Reload uses brand green on GREENBC
    const reloadBtn = dialogGreen.getByRole('button', { name: /^reload$/i })
    await expect(reloadBtn).toHaveCSS('background-color', 'rgb(0, 132, 127)')

    await dialogGreen.getByRole('button', { name: /^keep editing$/i }).click()
    await expect(dialogGreen).toHaveCount(0)
  })
})
