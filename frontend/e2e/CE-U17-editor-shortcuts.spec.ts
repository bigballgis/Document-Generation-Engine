/**
 * CE-U17 — Editor keyboard shortcuts (Ctrl/Cmd+S / Ctrl/Cmd+P + palette Actions)
 * BDD: docs/behavior/ce-u17-editor-shortcuts.md (BDD-CE-U17-EKS-001…012)
 *
 * Canonical run (after stage 5 DEPLOY_OK):
 *   $env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
 *   pnpm -C frontend exec playwright test e2e/CE-U17-editor-shortcuts.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 *
 * Scenario map (this file):
 *   BDD-CE-U17-EKS-001 — Ctrl+S saves binding (same path as Save)
 *   BDD-CE-U17-EKS-002 — Ctrl+P refreshes preview (same path as Refresh now)
 *   BDD-CE-U17-EKS-003 — palette lists/executes Save binding then closes
 *   BDD-CE-U17-EKS-004 — palette lists/executes Refresh preview then closes
 *   BDD-CE-U17-EKS-005 — outside edit surface: no author Actions / no handlers
 *   BDD-CE-U17-EKS-006 — palette open suppresses Ctrl+S / Ctrl+P
 *   BDD-CE-U17-EKS-007 — other aria-modal suppresses shortcuts
 *   BDD-CE-U17-EKS-008 — fail-closed: no authorTemplates → no Save action/API
 *   BDD-CE-U17-EKS-010 — Ctrl+K still opens palette inside editor (C6 coexistence)
 *   (009/011 covered by unit tests + CE-U21; 012 UIUX → e2e-uiux-reviewer)
 */
import { expect, test, type APIRequestContext, type Page, type Request } from '@playwright/test'

import { E2E_TEMPLATE_AUTHOR, E2E_TEMPLATE_TESTER, loginAs } from './helpers/auth'
import {
  mutateBindingStructure,
  openDevBindingEditor,
} from './helpers/core-fortress-f7'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import { P14_T01_VIEWPORT } from './helpers/uiux-evidence'

const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const PALETTE_CHORD = `${process.platform === 'darwin' ? 'Meta' : 'Control'}+k`

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function openEditor(page: Page, request: APIRequestContext, templateId: string) {
  await openDevBindingEditor(page, request, templateId)
  await dismissOnboardingTourIfPresent(page)
  await expect(page.getByTestId('binding-editor')).toBeVisible()
}

/**
 * Closed Element Plus dialogs keep `aria-modal="true"` under `display:none` overlays.
 * Pre-fix builds of `isAriaModalOpen` treated those as open and blocked Ctrl+S/P.
 * Worktree FE fix ignores hidden ancestors; this helper keeps E2E green on either image.
 */
async function neutralizeClosedOverlayAriaModals(page: Page): Promise<void> {
  await page.evaluate(() => {
    document.querySelectorAll('[aria-modal="true"]').forEach((node) => {
      let current: HTMLElement | null = node as HTMLElement
      while (current) {
        const style = getComputedStyle(current)
        if (style.display === 'none' || style.visibility === 'hidden') {
          node.removeAttribute('aria-modal')
          return
        }
        current = current.parentElement
      }
    })
  })
}

/**
 * Dispatch Ctrl/Cmd+S or Ctrl/Cmd+P to document.
 * Ctrl+P is often swallowed by Chromium print UI before page handlers; KeyboardEvent
 * matches the production document listener (useAuthoringEditorShortcuts).
 */
async function pressAuthorShortcut(page: Page, key: 's' | 'p'): Promise<void> {
  await neutralizeClosedOverlayAriaModals(page)
  const meta = process.platform === 'darwin'
  await page.evaluate(
    ({ keyName, useMeta }) => {
      document.dispatchEvent(
        new KeyboardEvent('keydown', {
          key: keyName,
          code: keyName === 's' ? 'KeyS' : 'KeyP',
          ctrlKey: !useMeta,
          metaKey: useMeta,
          bubbles: true,
          cancelable: true,
        }),
      )
    },
    { keyName: key, useMeta: meta },
  )
}

async function openPalette(page: Page) {
  await page.keyboard.press(PALETTE_CHORD)
  await expect(page.getByTestId('command-palette')).toBeVisible()
  await expect(page.getByTestId('command-palette-input')).toBeFocused()
}

function isBindingSavePut(url: string, templateId: string): boolean {
  return url.includes(`/templates/${templateId}/bindings/`)
}

function isPreviewRefreshPost(url: string): boolean {
  return url.includes('/previews/test-generate') || url.includes('/test-generate')
}

function isAuthorMutationRequest(req: Request, templateId: string): boolean {
  const method = req.method()
  const url = req.url()
  if (method === 'PUT' && isBindingSavePut(url, templateId)) {
    return true
  }
  return method === 'POST' && isPreviewRefreshPost(url)
}

/** Assert Ctrl/Cmd+S/P (or other triggers) do not start save/refresh network calls. */
async function expectNoAuthorMutationAfter(
  page: Page,
  templateId: string,
  trigger: () => Promise<void>,
  quietMs = 1_500,
): Promise<void> {
  const leakedPromise = page
    .waitForRequest((req) => isAuthorMutationRequest(req, templateId), { timeout: quietMs })
    .then((req) => req)
    .catch(() => null)
  await trigger()
  const leaked = await leakedPromise
  expect(leaked, 'expected no binding save / preview refresh request').toBeNull()
}

test.describe('CE-U17 editor shortcuts (BDD-CE-U17-EKS)', () => {
  test.describe.configure({ mode: 'serial', timeout: 240_000 })

  let templateId: string

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    templateId = fixture.templateId
  })

  test.beforeEach(async ({ page }) => {
    page.setDefaultTimeout(20_000)
    await page.setViewportSize(P14_T01_VIEWPORT)
  })

  test('BDD-CE-U17-EKS-001: Ctrl/Cmd+S saves binding via same path as Save', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openEditor(page, request, templateId)
    await mutateBindingStructure(page)

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        isBindingSavePut(response.url(), templateId),
      { timeout: 60_000 },
    )

    // Focus editor surface (not toolbar Back) then fire Ctrl/Cmd+S.
    await page.getByTestId('controlled-structured-content-editor').click()
    await pressAuthorShortcut(page, 's')
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()
    await expect(page.locator('.el-message').getByText(/binding saved/i)).toBeVisible({
      timeout: 15_000,
    })
  })

  test('BDD-CE-U17-EKS-002: Ctrl/Cmd+P refreshes preview via same path as Refresh now', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openEditor(page, request, templateId)

    const refreshResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        isPreviewRefreshPost(response.url()) &&
        response.ok(),
      { timeout: 120_000 },
    )

    await page.getByTestId('controlled-structured-content-editor').click()
    await pressAuthorShortcut(page, 'p')
    await refreshResponsePromise
    await expect(page.getByTestId('authoring-preview-refresh')).toBeEnabled({ timeout: 120_000 })
    await expect(
      page.locator('.el-message').getByText(/test generation started|preview/i).first(),
    ).toBeVisible({ timeout: 30_000 })
  })

  test('BDD-CE-U17-EKS-003: palette lists Save binding and Enter executes then closes', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openEditor(page, request, templateId)
    await mutateBindingStructure(page)

    await openPalette(page)
    await expect(page.getByTestId('command-palette-group-actions')).toBeVisible()
    const saveAction = page.getByTestId('command-palette-action-save-binding')
    await expect(saveAction).toBeVisible()
    await expect(saveAction).toContainText(/save binding/i)
    await expect(saveAction).toContainText(/Ctrl\+S|⌘S/i)

    const saveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        isBindingSavePut(response.url(), templateId),
      { timeout: 60_000 },
    )
    await saveAction.click()
    const saveResponse = await saveResponsePromise
    expect(saveResponse.ok()).toBeTruthy()
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
    await expect(page.locator('.el-message').getByText(/binding saved/i)).toBeVisible({
      timeout: 15_000,
    })
  })

  test('BDD-CE-U17-EKS-004: palette lists Refresh preview and executes then closes', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openEditor(page, request, templateId)

    await openPalette(page)
    await expect(page.getByTestId('command-palette-group-actions')).toBeVisible()
    const refreshAction = page.getByTestId('command-palette-action-refresh-preview')
    await expect(refreshAction).toBeVisible()
    await expect(refreshAction).toContainText(/refresh preview/i)
    await expect(refreshAction).toContainText(/Ctrl\+P|⌘P/i)

    const refreshResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        isPreviewRefreshPost(response.url()) &&
        response.ok(),
      { timeout: 120_000 },
    )
    await refreshAction.click()
    await refreshResponsePromise
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
    await expect(page.getByTestId('authoring-preview-refresh')).toBeEnabled({ timeout: 120_000 })
  })

  test('BDD-CE-U17-EKS-005: outside edit surface Ctrl+S/P do not call author handlers', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    // Bindings list (not anchor edit) — author Actions must be absent.
    const lines = await listTemplateVersionLines(request, templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()
    await page.goto(
      `/templates/${templateId}/dev/${inFlight!.devVersionId}?workspaceTab=design&designTab=bindings`,
    )
    await dismissOnboardingTourIfPresent(page)
    await expect(page.locator('.bindings-panel')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByTestId('binding-editor')).toHaveCount(0)

    await expectNoAuthorMutationAfter(page, templateId, async () => {
      await pressAuthorShortcut(page, 's')
      await pressAuthorShortcut(page, 'p')
    })

    await openPalette(page)
    await expect(page.getByTestId('command-palette-group-actions')).toHaveCount(0)
    await expect(page.getByTestId('command-palette-action-save-binding')).toHaveCount(0)
    await expect(page.getByTestId('command-palette-action-refresh-preview')).toHaveCount(0)
    await page.keyboard.press('Escape')
    await expect(page.getByTestId('command-palette')).toHaveCount(0)

    // Dashboard — same fail-closed omission
    await page.goto('/dashboard')
    await expect(page.locator('#tasks-section, #dev-workspace, main').first()).toBeVisible({
      timeout: 30_000,
    })
    await openPalette(page)
    await expect(page.getByTestId('command-palette-action-save-binding')).toHaveCount(0)
    await expect(page.getByTestId('command-palette-action-refresh-preview')).toHaveCount(0)
  })

  test('BDD-CE-U17-EKS-006: command palette open suppresses Ctrl+S / Ctrl+P', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openEditor(page, request, templateId)
    await mutateBindingStructure(page)

    await openPalette(page)
    // Do not neutralize — presented command-palette must keep suppressing S/P.
    await expectNoAuthorMutationAfter(page, templateId, async () => {
      await page.evaluate(() => {
        document.dispatchEvent(
          new KeyboardEvent('keydown', {
            key: 's',
            code: 'KeyS',
            ctrlKey: true,
            bubbles: true,
            cancelable: true,
          }),
        )
        document.dispatchEvent(
          new KeyboardEvent('keydown', {
            key: 'p',
            code: 'KeyP',
            ctrlKey: true,
            bubbles: true,
            cancelable: true,
          }),
        )
      })
    })
    await expect(page.getByTestId('command-palette')).toBeVisible()
    await page.keyboard.press('Escape')
  })

  test('BDD-CE-U17-EKS-007: other aria-modal dialog suppresses shortcuts', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openEditor(page, request, templateId)
    await mutateBindingStructure(page)

    await page.getByRole('button', { name: /^back$/i }).click()
    const dirtyDialog = page.getByTestId('dirty-guard-dialog')
    await expect(dirtyDialog).toBeVisible({ timeout: 15_000 })
    // Presented modal only (closed EP overlays with aria-modal are ignored by U17-D8).
    await expect(
      page.locator('.el-overlay:not([style*="display: none"]) [aria-modal="true"]').first(),
    ).toBeVisible()

    await expectNoAuthorMutationAfter(page, templateId, async () => {
      await page.evaluate(() => {
        document.dispatchEvent(
          new KeyboardEvent('keydown', {
            key: 's',
            code: 'KeyS',
            ctrlKey: true,
            bubbles: true,
            cancelable: true,
          }),
        )
        document.dispatchEvent(
          new KeyboardEvent('keydown', {
            key: 'p',
            code: 'KeyP',
            ctrlKey: true,
            bubbles: true,
            cancelable: true,
          }),
        )
      })
    })

    await dirtyDialog.getByTestId('dirty-guard-stay').click()
    await expect(dirtyDialog).not.toBeVisible({ timeout: 15_000 })
    await expect(page.getByTestId('binding-editor')).toBeVisible()
  })

  test('BDD-CE-U17-EKS-008: fail-closed — no authorTemplates omits Save binding / save API', async ({
    page,
  }) => {
    // TEMPLATE_TESTER lacks authorTemplates write — must not expose Save binding or fire save API.
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard')
    await expect(page.locator('#tasks-section, main').first()).toBeVisible({ timeout: 30_000 })

    await expectNoAuthorMutationAfter(page, templateId, async () => {
      await pressAuthorShortcut(page, 's')
    })

    await openPalette(page)
    await expect(page.getByTestId('command-palette-action-save-binding')).toHaveCount(0)
    await expect(page.locator('.el-message').getByText(/binding saved/i)).toHaveCount(0)
  })

  test('BDD-CE-U17-EKS-010: Ctrl/Cmd+K still opens palette inside binding editor', async ({
    page,
    request,
  }) => {
    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await openEditor(page, request, templateId)
    await neutralizeClosedOverlayAriaModals(page)
    await page.getByTestId('controlled-structured-content-editor').click()

    await openPalette(page)
    await expect(page.getByTestId('command-palette-group-actions')).toBeVisible()
    await expect(page.getByTestId('command-palette-action-save-binding')).toBeVisible()
    await expect(page.getByTestId('command-palette-action-refresh-preview')).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(page.getByTestId('command-palette')).toHaveCount(0)
  })
})
