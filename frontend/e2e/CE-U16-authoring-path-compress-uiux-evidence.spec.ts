/**
 * CE-U16 UIUX evidence — Authoring path compression
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/ce-u16-authoring-path-compress.md (APC-001 / 003–007 visual surfaces)
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

import { DEMO_MASTER_NAME, E2E_TEMPLATE_AUTHOR, loginAs } from './helpers/auth'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareDraftTemplateWithCleanBinding } from './helpers/structured-authoring-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  captureCeU16LocatorScreenshot,
  captureCeU16Screenshot,
  CE_U16_VIEWPORT,
  ensureCeU16EvidenceDirs,
  switchBrand,
} from './helpers/uiux-evidence'

const FRONTEND_BASE_URL =
  process.env.E2E_BASE_URL ?? `http://127.0.0.1:${process.env.FRONTEND_PORT ?? '4173'}`

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
}

function authoringGuide(page: Page) {
  return page.getByTestId('authoring-path-guide')
}

function guideStep(page: Page, stepId: string) {
  return page.getByTestId(`authoring-path-guide-step-${stepId}`)
}

function lifecycleStepper(page: Page) {
  return page.getByTestId('lifecycle-stepper')
}

async function openPostCreateAuthoringPath(
  page: Page,
  templateId: string,
  devVersionId: string,
): Promise<void> {
  await openDevWorkspace(
    page,
    templateId,
    devVersionId,
    'workspaceTab=design&authoringGuide=1&authoringGuideStep=master',
  )
}

async function expectBindingsPanelActive(page: Page): Promise<void> {
  await expect(page).toHaveURL(/designTab=bindings|workspaceTab=design/)
  const bindingsTab = page.getByRole('tab', { name: /^bindings$/i })
  await expect(bindingsTab).toBeVisible({ timeout: 30_000 })
  await expect(bindingsTab).toHaveAttribute('aria-selected', 'true')
  await expect(page.locator('.bindings-panel')).toBeVisible()
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

/** Authoring path must coexist with lifecycle stepper and host no lifecycle CTAs. */
async function assertAuthoringPathOrientationOnly(page: Page): Promise<void> {
  await expect(lifecycleStepper(page)).toBeVisible()
  await expect(authoringGuide(page)).toBeVisible()

  const guideBox = await authoringGuide(page).boundingBox()
  const stepperBox = await lifecycleStepper(page).boundingBox()
  expect(guideBox, 'authoring-path-guide bounding box').toBeTruthy()
  expect(stepperBox, 'lifecycle-stepper bounding box').toBeTruthy()

  await expect(authoringGuide(page).getByRole('button', { name: /^submit for testing$/i })).toHaveCount(
    0,
  )
  await expect(authoringGuide(page).getByRole('button', { name: /^approve$/i })).toHaveCount(0)
  await expect(authoringGuide(page).getByRole('button', { name: /^confirm go-live$/i })).toHaveCount(
    0,
  )
  await expect(authoringGuide(page).getByRole('button', { name: /^publish$/i })).toHaveCount(0)
}

test.describe('CE-U16 authoring path compress UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeU16EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('01–02 dual-brand: Authoring path Master + lifecycle-stepper coexistence', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U16_VIEWPORT)

    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openPostCreateAuthoringPath(page, fixture.templateId, devVersionId)

    await expect(page).toHaveURL(/authoringGuide=1/)
    await expect(page).toHaveURL(/authoringGuideStep=master/)
    await expect(guideStep(page, 'master')).toHaveAttribute('aria-current', 'step')
    await expect(page.getByTestId('authoring-path-master-panel')).toBeVisible()
    await expect(page.getByTestId('authoring-path-master-identity')).toContainText(DEMO_MASTER_NAME)
    await expect(page.getByTestId('authoring-path-master-anchors')).toBeVisible()
    await assertAuthoringPathOrientationOnly(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U16 authoring path Master REDBC')

    await captureCeU16Screenshot(page, '01-authoring-path-master-redbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      authoringGuide(page),
      '01b-authoring-path-guide-crop-redbc-1920x1080.png',
    )
    await captureCeU16LocatorScreenshot(
      lifecycleStepper(page),
      '01c-lifecycle-stepper-coexist-crop-redbc-1920x1080.png',
    )
    await captureCeU16LocatorScreenshot(
      page.getByTestId('authoring-path-master-panel'),
      '01d-master-panel-crop-redbc-1920x1080.png',
    )
    await captureCeU16LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01e-brand-header-redbc-crop.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(authoringGuide(page)).toBeVisible()
    await expect(lifecycleStepper(page)).toBeVisible()
    await assertAuthoringPathOrientationOnly(page)
    await assertNoViewportOverflow(page)

    await captureCeU16Screenshot(page, '02-authoring-path-master-greenbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      authoringGuide(page),
      '02b-authoring-path-guide-crop-greenbc-1920x1080.png',
    )
    await captureCeU16LocatorScreenshot(
      lifecycleStepper(page),
      '02c-lifecycle-stepper-coexist-crop-greenbc-1920x1080.png',
    )
    await captureCeU16LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '02d-brand-header-greenbc-crop.png',
    )
  })

  test('03–04 dual-brand: default Bindings (no guide) + Authoring path Bindings step', async ({
    page,
    request,
  }) => {
    await page.setViewportSize(CE_U16_VIEWPORT)

    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    // APC-001 — daily Design default Bindings (no authoring guide)
    await openDevWorkspace(page, fixture.templateId, devVersionId, 'workspaceTab=design')
    await expect(authoringGuide(page)).toHaveCount(0)
    await expect(lifecycleStepper(page)).toBeVisible({ timeout: 30_000 })
    await expectBindingsPanelActive(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U16 default Bindings REDBC')

    await captureCeU16Screenshot(page, '03-default-bindings-redbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      page.locator('#dev-workspace'),
      '03b-dev-workspace-bindings-crop-redbc-1920x1080.png',
    )
    await captureCeU16LocatorScreenshot(
      page.locator('.bindings-panel'),
      '03c-bindings-panel-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expectBindingsPanelActive(page)
    await assertNoViewportOverflow(page)

    await captureCeU16Screenshot(page, '04-default-bindings-greenbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '04b-brand-header-greenbc-crop.png',
    )

    // Authoring path Bindings step (same fixture, guide session)
    await switchBrand(page, 'REDBC')
    await openPostCreateAuthoringPath(page, fixture.templateId, devVersionId)
    await guideStep(page, 'bindings').click()
    await expect(page).toHaveURL(/authoringGuideStep=bindings/)
    await expect(page).toHaveURL(/designTab=bindings/)
    await expect(guideStep(page, 'bindings')).toHaveAttribute('aria-current', 'step')
    await expectBindingsPanelActive(page)
    await assertAuthoringPathOrientationOnly(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U16 authoring path Bindings REDBC')

    await captureCeU16Screenshot(page, '05-authoring-path-bindings-redbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      authoringGuide(page),
      '05b-authoring-path-bindings-guide-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(guideStep(page, 'bindings')).toHaveAttribute('aria-current', 'step')
    await assertNoViewportOverflow(page)

    await captureCeU16Screenshot(page, '06-authoring-path-bindings-greenbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '06b-brand-header-greenbc-crop.png',
    )
  })

  test('07–08 dual-brand: Preview step + Skip/Dismiss guide', async ({ page, request }) => {
    await page.setViewportSize(CE_U16_VIEWPORT)

    const fixture = await prepareDraftTemplateWithCleanBinding(request)
    const devVersionId = await resolveInFlightDevVersionId(request, fixture.templateId)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openPostCreateAuthoringPath(page, fixture.templateId, devVersionId)
    await guideStep(page, 'preview').click()
    await expect(page).toHaveURL(/authoringGuideStep=preview/)
    await expect(page).toHaveURL(/workspaceTab=testing/)
    await expect(page).toHaveURL(/testingTab=previewRuns/)
    await expect(guideStep(page, 'preview')).toHaveAttribute('aria-current', 'step')
    await expect(page.getByRole('tab', { name: /^preview runs$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(lifecycleStepper(page)).toBeVisible()
    await expect(authoringGuide(page)).toBeVisible()
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U16 authoring path Preview REDBC')

    await captureCeU16Screenshot(page, '07-authoring-path-preview-redbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      authoringGuide(page),
      '07b-authoring-path-preview-guide-crop-redbc-1920x1080.png',
    )
    await captureCeU16LocatorScreenshot(
      page.locator('#dev-workspace'),
      '07c-dev-workspace-preview-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(guideStep(page, 'preview')).toHaveAttribute('aria-current', 'step')
    await assertNoViewportOverflow(page)

    await captureCeU16Screenshot(page, '08-authoring-path-preview-greenbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '08b-brand-header-greenbc-crop.png',
    )

    // Skip / dismiss — guide gone; workspace + stepper remain
    await switchBrand(page, 'REDBC')
    await openPostCreateAuthoringPath(page, fixture.templateId, devVersionId)
    await expect(authoringGuide(page)).toBeVisible()
    await page.getByTestId('authoring-path-guide-dismiss').click()
    await expect(authoringGuide(page)).toHaveCount(0)
    await expect(page).not.toHaveURL(/authoringGuide=1/)
    await expect(page.getByRole('tab', { name: /^template design$/i })).toBeVisible()
    await expectBindingsPanelActive(page)
    await expect(lifecycleStepper(page)).toBeVisible()
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U16 after Skip guide REDBC')

    await captureCeU16Screenshot(page, '09-skip-guide-workspace-redbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      page.locator('#dev-workspace'),
      '09b-dev-workspace-after-skip-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(authoringGuide(page)).toHaveCount(0)
    await expect(lifecycleStepper(page)).toBeVisible()
    await assertNoViewportOverflow(page)

    await captureCeU16Screenshot(page, '10-skip-guide-workspace-greenbc-1920x1080.png')
    await captureCeU16LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '10b-brand-header-greenbc-crop.png',
    )
  })
})
