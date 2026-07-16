/**
 * CE-U15 UIUX evidence — Lifecycle Stepper + publish-gate Go fix
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/ce-u15-lifecycle-stepper.md (LSS-001 / LSS-004 visual surfaces)
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type APIRequestContext, type Page, type Route } from '@playwright/test'

import {
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { prepareTemplatePendingRelease } from './helpers/submit-approval-gate-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  captureCeU15LocatorScreenshot,
  captureCeU15Screenshot,
  CE_U15_VIEWPORT,
  ensureCeU15EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

type PublishGateItem = {
  checkCode: string
  ready: boolean
  blocker: boolean
  messageKey?: string
  summary?: string
}

type PublishGateEnvelope = {
  metadata?: unknown
  result: {
    ready: boolean
    items: PublishGateItem[]
  }
  error?: unknown
}

async function dismissOnboardingTourIfPresent(page: Page): Promise<void> {
  const skipTour = page.getByTestId('onboarding-tour-skip')
  if (await skipTour.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await skipTour.click()
    await expect(skipTour).toHaveCount(0)
  }
}

async function resolveInFlightDevVersionId(
  request: APIRequestContext,
  templateId: string,
): Promise<string> {
  const lines = await listTemplateVersionLines(request, templateId)
  const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
  expect(inFlight?.devVersionId, `IN_FLIGHT devVersion for ${templateId}`).toBeTruthy()
  return inFlight!.devVersionId
}

async function openDevWorkspace(
  page: Page,
  templateId: string,
  devVersionId: string,
  query = '',
): Promise<void> {
  const suffix = query.startsWith('?') || query.length === 0 ? query : `?${query}`
  await page.goto(`/templates/${templateId}/dev/${devVersionId}${suffix}`)
  await dismissOnboardingTourIfPresent(page)
  await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
  await expect(page.getByTestId('lifecycle-stepper')).toBeVisible({ timeout: 30_000 })
}

function stepper(page: Page) {
  return page.getByTestId('lifecycle-stepper')
}

function workspaceActions(page: Page) {
  return page.locator('.workspace-tab-shell__actions')
}

async function forcePublishGatePending(page: Page, pendingCodes: string[]): Promise<void> {
  await page.route('**/api/management/v1/templates/*/publish-gate**', async (route: Route) => {
    const response = await route.fetch()
    const body = (await response.json()) as PublishGateEnvelope
    if (!body?.result?.items) {
      await route.fulfill({ response })
      return
    }
    const pending = new Set(pendingCodes)
    const items = body.result.items.map((item) =>
      pending.has(item.checkCode)
        ? {
            ...item,
            ready: false,
            blocker: item.blocker ?? true,
            messageKey: item.messageKey ?? `api.publishGate.${item.checkCode}.pending`,
            summary: item.summary ?? `${item.checkCode} pending (E2E CE-U15 UIUX)`,
          }
        : item,
    )
    for (const code of pendingCodes) {
      if (!items.some((item) => item.checkCode === code)) {
        items.push({
          checkCode: code,
          ready: false,
          blocker: true,
          messageKey: `api.publishGate.${code}.pending`,
          summary: `${code} pending (E2E CE-U15 UIUX)`,
        })
      }
    }
    await route.fulfill({
      status: response.status(),
      contentType: 'application/json',
      body: JSON.stringify({
        ...body,
        result: {
          ...body.result,
          ready: false,
          items,
        },
      }),
    })
  })
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

async function expectNoCriticalAxeViolations(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

/** Stepper must sit above WorkspaceTabShell and not host workflow CTAs. */
async function assertStepperOrientationOnly(page: Page): Promise<void> {
  const stepperBox = await stepper(page).boundingBox()
  const shellBox = await page.locator('.workspace-tab-shell').boundingBox()
  expect(stepperBox, 'lifecycle-stepper bounding box').toBeTruthy()
  expect(shellBox, 'workspace-tab-shell bounding box').toBeTruthy()
  expect(stepperBox!.y + stepperBox!.height).toBeLessThanOrEqual(shellBox!.y + 2)

  await expect(stepper(page).getByRole('button', { name: /^submit for testing$/i })).toHaveCount(0)
  await expect(stepper(page).getByRole('button', { name: /^approve$/i })).toHaveCount(0)
  await expect(stepper(page).getByRole('button', { name: /^confirm go-live$/i })).toHaveCount(0)
  await expect(stepper(page).getByRole('button', { name: /^publish$/i })).toHaveCount(0)
}

test.describe('CE-U15 lifecycle stepper UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeU15EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('01–03 dual-brand: DRAFT Workflow progress Stepper (LSS-001)', async ({ page, request }) => {
    await page.setViewportSize(CE_U15_VIEWPORT)

    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openDevWorkspace(page, fixture.templateId, devVersionId)

    await expect(stepper(page)).toContainText(/workflow progress/i)
    await expect(page.getByTestId('lifecycle-stepper-step-draft')).toHaveAttribute(
      'aria-current',
      'step',
    )
    await expect(page.getByTestId('lifecycle-stepper-step-draft')).toContainText(/draft/i)
    await assertStepperOrientationOnly(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U15 DRAFT stepper REDBC')

    await captureCeU15Screenshot(page, '01-draft-stepper-redbc-1920x1080.png')
    await captureCeU15LocatorScreenshot(
      stepper(page),
      '01b-lifecycle-stepper-crop-redbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      page.locator('#dev-workspace'),
      '01c-dev-workspace-crop-redbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01d-brand-header-redbc-crop.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(stepper(page)).toBeVisible()
    await expect(stepper(page)).toContainText(/workflow progress/i)
    await assertStepperOrientationOnly(page)
    await assertNoViewportOverflow(page)

    await captureCeU15Screenshot(page, '02-draft-stepper-greenbc-1920x1080.png')
    await captureCeU15LocatorScreenshot(
      stepper(page),
      '02b-lifecycle-stepper-crop-greenbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      page.locator('#dev-workspace'),
      '02c-dev-workspace-crop-greenbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02d-brand-header-greenbc-crop.png',
    )
  })

  test('04–07 dual-brand: publish readiness Go fix + post-nav bindings (LSS-004)', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U15_VIEWPORT)

    const fixture = await prepareTemplatePendingRelease(request, {
      externalId: `E2E-CE-U15-UIUX-GF-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U15 UIUX GoFix ${Date.now().toString(36).toUpperCase()}`,
    })
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await forcePublishGatePending(page, ['ANCHOR_INTEGRITY'])

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openDevWorkspace(
      page,
      fixture.templateId,
      devVersionId,
      'workspaceTab=approval&approvalTab=publishReadiness',
    )

    await expect(page.getByTestId('lifecycle-stepper-step-pendingRelease')).toHaveAttribute(
      'aria-current',
      'step',
    )
    await expect(stepper(page)).toContainText(/workflow progress/i)
    await assertStepperOrientationOnly(page)
    await expect(
      workspaceActions(page).getByRole('button', { name: /^confirm go-live$/i }),
    ).toBeVisible({ timeout: 60_000 })

    const goFix = page.getByTestId('publish-gate-go-fix-ANCHOR_INTEGRITY')
    await expect(goFix).toBeVisible({ timeout: 30_000 })
    await expect(goFix).toHaveText(/^Go fix$/i)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U15 publish readiness + Go fix REDBC')

    await captureCeU15Screenshot(page, '03-publish-readiness-gofix-redbc-1920x1080.png')
    await captureCeU15LocatorScreenshot(
      stepper(page),
      '03b-stepper-pending-release-crop-redbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      page.locator('#dev-workspace'),
      '03c-dev-workspace-publish-crop-redbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      goFix,
      '03d-go-fix-anchor-crop-redbc.png',
    )
    await captureCeU15LocatorScreenshot(
      workspaceActions(page),
      '03e-workspace-actions-crop-redbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03f-brand-header-redbc-crop.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.getByTestId('publish-gate-go-fix-ANCHOR_INTEGRITY')).toBeVisible()
    await expect(page.getByTestId('publish-gate-go-fix-ANCHOR_INTEGRITY')).toHaveText(/^Go fix$/i)
    await assertStepperOrientationOnly(page)
    await assertNoViewportOverflow(page)

    await captureCeU15Screenshot(page, '04-publish-readiness-gofix-greenbc-1920x1080.png')
    await captureCeU15LocatorScreenshot(
      stepper(page),
      '04b-stepper-pending-release-crop-greenbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      page.getByTestId('publish-gate-go-fix-ANCHOR_INTEGRITY'),
      '04c-go-fix-anchor-crop-greenbc.png',
    )
    await captureCeU15LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04d-brand-header-greenbc-crop.png',
    )

    // Post Go-fix navigation — design/bindings (REDBC for consistency of journey crop)
    await switchBrand(page, 'REDBC')
    await page.getByTestId('publish-gate-go-fix-ANCHOR_INTEGRITY').click()
    await expect(page).toHaveURL(/workspaceTab=design/, { timeout: 15_000 })
    await expect(page).toHaveURL(/designTab=bindings/)
    await expect(page.getByRole('tab', { name: /^template design$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(stepper(page)).toBeVisible()
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U15 post Go-fix bindings REDBC')

    await captureCeU15Screenshot(page, '05-post-gofix-bindings-redbc-1920x1080.png')
    await captureCeU15LocatorScreenshot(
      page.locator('#dev-workspace'),
      '05b-dev-workspace-bindings-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(stepper(page)).toBeVisible()
    await assertNoViewportOverflow(page)
    await captureCeU15Screenshot(page, '06-post-gofix-bindings-greenbc-1920x1080.png')
    await captureCeU15LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '06b-brand-header-greenbc-crop.png',
    )
  })

  test('07 dual-brand: dashboard Tasks remain stepper-free (LSS-010)', async ({ page }) => {
    await page.setViewportSize(CE_U15_VIEWPORT)

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await page.goto('/dashboard?queue=TEST#tasks-section')
    await dismissOnboardingTourIfPresent(page)
    const tasks = page.locator('#tasks-section')
    await expect(tasks).toBeVisible({ timeout: 30_000 })
    await expect(tasks.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })

    await expect(page.getByTestId('lifecycle-stepper')).toHaveCount(0)
    await expect(page.locator('[data-ce-u15-stepper]')).toHaveCount(0)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U15 dashboard Tasks REDBC')

    await captureCeU15Screenshot(page, '07-dashboard-tasks-no-stepper-redbc-1920x1080.png')
    await captureCeU15LocatorScreenshot(
      tasks,
      '07b-tasks-section-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.getByTestId('lifecycle-stepper')).toHaveCount(0)
    await expect(page.locator('[data-ce-u15-stepper]')).toHaveCount(0)
    await assertNoViewportOverflow(page)

    await captureCeU15Screenshot(page, '08-dashboard-tasks-no-stepper-greenbc-1920x1080.png')
    await captureCeU15LocatorScreenshot(
      page.locator('#tasks-section'),
      '08b-tasks-section-crop-greenbc-1920x1080.png',
    )
    await captureCeU15LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '08c-brand-header-greenbc-crop.png',
    )
  })
})
