/**
 * CE-U14 UIUX evidence — dashboard lifecycle todos deep links
 * Dual-brand REDBC/GREENBC @1920 (Stage 7).
 * BDD: docs/behavior/ce-u14-dashboard-lifecycle-todos.md (DLT-001…004 surfaces)
 */
import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

import {
  E2E_TEMPLATE_APPROVER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  prepareRetailTemplateInTesting,
  requireOpenTestWorkItemForTemplate,
} from './helpers/collaboration-api'
import { E2E_API_BASE_URL, assertDemoCatalogSeeded } from './helpers/masters-api'
import { requireDockerStack } from './helpers/stack-readiness'
import { prepareTemplatePendingApprovalDecision } from './helpers/submit-approval-gate-api'
import { listTemplateVersionLines } from './helpers/template-version-lines-api'
import {
  dashboardTaskRow,
  expectDashboardPartitionHeading,
  filterDashboardTasksByItem,
} from './helpers/ui'
import {
  captureCeU14LocatorScreenshot,
  captureCeU14Screenshot,
  CE_U14_VIEWPORT,
  ensureCeU14EvidenceDirs,
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

async function openDashboardQueueTasks(page: Page, queue: string) {
  await page.goto(`/dashboard?queue=${queue}#tasks-section`)
  await dismissOnboardingTourIfPresent(page)
  const tasks = page.locator('#tasks-section')
  await expect(tasks).toBeVisible({ timeout: 30_000 })
  await expect(tasks.locator('.el-skeleton')).toHaveCount(0, { timeout: 60_000 })
  await expect(page.getByText(/unable to load collaboration to-do items/i)).not.toBeVisible()
  return tasks
}

function workspaceActions(page: Page) {
  return page.locator('.workspace-tab-shell__actions')
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

/** CE-U15 Stepper must not appear on CE-U14 dashboard / workspace surfaces. */
async function assertNoCeU15StepperDom(page: Page): Promise<void> {
  await expect(page.locator('.el-steps')).toHaveCount(0)
  await expect(page.locator('[class*="lifecycle-stepper"]')).toHaveCount(0)
  await expect(page.locator('[data-testid*="stepper"]')).toHaveCount(0)
  await expect(page.locator('[data-ce-u15-stepper]')).toHaveCount(0)
}

async function expectNoCriticalAxeViolations(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const critical = results.violations.filter((violation) => violation.impact === 'critical')
  expect(critical, `${label} critical axe violations`).toEqual([])
}

test.describe('CE-U14 dashboard lifecycle todos UIUX evidence @1920 dual-brand', () => {
  test.describe.configure({ mode: 'serial', timeout: 420_000 })

  test.beforeAll(async ({ request }) => {
    ensureCeU14EvidenceDirs()
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}).`,
    })
    await assertDemoCatalogSeeded(request)
  })

  test('01–03 dual-brand: TEST queue + testing action rail', async ({ page, request }) => {
    await page.setViewportSize(CE_U14_VIEWPORT)

    const fixture = await prepareRetailTemplateInTesting(request, {
      externalId: `E2E-CE-U14-UIUX-T-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U14 UIUX Test ${Date.now().toString(36).toUpperCase()}`,
    })
    await requireOpenTestWorkItemForTemplate(request, fixture)
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()

    await loginAs(page, E2E_TEMPLATE_TESTER)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openDashboardQueueTasks(page, 'TEST')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /waiting on my testing/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expectDashboardPartitionHeading(page, /in testing/i)
    await expect(page.locator('[data-partition-id="queue-TEST"]')).toBeVisible()
    await assertNoCeU15StepperDom(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U14 TEST queue REDBC')

    await filterDashboardTasksByItem(page, fixture.name)
    const row = await dashboardTaskRow(page, fixture.name)
    await expect(row).toBeVisible({ timeout: 30_000 })

    await captureCeU14Screenshot(page, '01-test-queue-redbc-1920x1080.png')
    await captureCeU14LocatorScreenshot(
      page.locator('#tasks-section'),
      '01b-test-queue-tasks-crop-redbc-1920x1080.png',
    )
    await captureCeU14LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '01c-brand-header-redbc-crop.png',
    )

    await row.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(
      new RegExp(
        `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}\\?.*workspaceTab=testing`,
      ),
      { timeout: 30_000 },
    )
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByRole('tab', { name: /^template testing$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(
      workspaceActions(page).getByRole('button', { name: /^confirm test pass$/i }),
    ).toBeVisible({ timeout: 15_000 })
    await expect(
      workspaceActions(page).getByRole('button', { name: /^record test failure$/i }),
    ).toBeVisible()
    await assertNoCeU15StepperDom(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U14 testing action rail REDBC')

    await captureCeU14Screenshot(page, '02-testing-action-rail-redbc-1920x1080.png')
    await captureCeU14LocatorScreenshot(
      page.locator('#dev-workspace'),
      '02b-dev-workspace-crop-redbc-1920x1080.png',
    )
    await captureCeU14LocatorScreenshot(
      workspaceActions(page),
      '02c-workspace-actions-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(
      workspaceActions(page).getByRole('button', { name: /^confirm test pass$/i }),
    ).toBeVisible()
    await assertNoCeU15StepperDom(page)
    await assertNoViewportOverflow(page)

    await captureCeU14Screenshot(page, '03-testing-action-rail-greenbc-1920x1080.png')
    await captureCeU14LocatorScreenshot(
      page.locator('#dev-workspace'),
      '03b-dev-workspace-crop-greenbc-1920x1080.png',
    )
    await captureCeU14LocatorScreenshot(
      workspaceActions(page),
      '03c-workspace-actions-crop-greenbc-1920x1080.png',
    )
    await captureCeU14LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '03d-brand-header-greenbc-crop.png',
    )

    // Dual-brand TEST queue (return to dashboard)
    await openDashboardQueueTasks(page, 'TEST')
    await expect(page.locator('[data-partition-id="queue-TEST"]')).toBeVisible()
    await assertNoCeU15StepperDom(page)
    await captureCeU14Screenshot(page, '03e-test-queue-greenbc-1920x1080.png')
  })

  test('04–05 dual-brand: APPROVAL decision rail', async ({ page, request }) => {
    await page.setViewportSize(CE_U14_VIEWPORT)

    const fixture = await prepareTemplatePendingApprovalDecision(request, {
      externalId: `E2E-CE-U14-UIUX-A-${Date.now().toString(36).toUpperCase()}`,
      name: `E2E CE-U14 UIUX Appr ${Date.now().toString(36).toUpperCase()}`,
    })
    const lines = await listTemplateVersionLines(request, fixture.templateId)
    const inFlight = lines.find((line) => line.lineKind === 'IN_FLIGHT')
    expect(inFlight?.devVersionId).toBeTruthy()

    await loginAs(page, E2E_TEMPLATE_APPROVER)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await openDashboardQueueTasks(page, 'APPROVAL')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /waiting on my approval/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await assertNoCeU15StepperDom(page)
    await assertNoViewportOverflow(page)

    await filterDashboardTasksByItem(page, fixture.name)
    const row = await dashboardTaskRow(page, fixture.name)
    await expect(row).toBeVisible({ timeout: 30_000 })
    await captureCeU14Screenshot(page, '04-approval-queue-redbc-1920x1080.png')

    await row.getByRole('button', { name: /^open$/i }).click()
    await expect(page).toHaveURL(
      new RegExp(
        `/templates/${fixture.templateId}/dev/${inFlight!.devVersionId}\\?.*workspaceTab=approval`,
      ),
      { timeout: 30_000 },
    )
    await expect(page).toHaveURL(/approvalTab=submitApproval/)
    await expect(page.locator('#dev-workspace')).toBeVisible({ timeout: 30_000 })
    await expect(page.getByRole('tab', { name: /^template approval$/i })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toBeVisible({
      timeout: 15_000,
    })
    await expect(workspaceActions(page).getByRole('button', { name: /^reject$/i })).toBeVisible()
    await assertNoCeU15StepperDom(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U14 APPROVAL decision rail REDBC')

    await captureCeU14Screenshot(page, '04b-approval-action-rail-redbc-1920x1080.png')
    await captureCeU14LocatorScreenshot(
      page.locator('#dev-workspace'),
      '04c-dev-workspace-approval-crop-redbc-1920x1080.png',
    )
    await captureCeU14LocatorScreenshot(
      workspaceActions(page),
      '04d-workspace-actions-approval-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(workspaceActions(page).getByRole('button', { name: /^approve$/i })).toBeVisible()
    await assertNoCeU15StepperDom(page)
    await assertNoViewportOverflow(page)

    await captureCeU14Screenshot(page, '05-approval-action-rail-greenbc-1920x1080.png')
    await captureCeU14LocatorScreenshot(
      workspaceActions(page),
      '05b-workspace-actions-approval-crop-greenbc-1920x1080.png',
    )
    await captureCeU14LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '05c-brand-header-greenbc-crop.png',
    )
  })

  test('06 dual-brand: author fail-closed (no lifecycle queue partitions)', async ({ page }) => {
    await page.setViewportSize(CE_U14_VIEWPORT)

    await loginAs(page, E2E_TEMPLATE_AUTHOR)
    await dismissOnboardingTourIfPresent(page)
    await switchBrand(page, 'REDBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'REDBC')

    await page.goto('/dashboard#tasks-section')
    await dismissOnboardingTourIfPresent(page)
    const tasks = page.locator('#tasks-section')
    await expect(tasks).toBeVisible({ timeout: 30_000 })
    await expect(tasks.locator('.el-skeleton')).toHaveCount(0)

    await expect(page.locator('[data-partition-id="queue-TEST"]')).toHaveCount(0)
    await expect(page.locator('[data-partition-id="queue-APPROVAL"]')).toHaveCount(0)
    await expect(page.locator('[data-partition-id="queue-PENDING_RELEASE"]')).toHaveCount(0)
    await assertNoCeU15StepperDom(page)
    await assertNoViewportOverflow(page)
    await expectNoCriticalAxeViolations(page, 'CE-U14 author fail-closed REDBC')

    await captureCeU14Screenshot(page, '06-author-fail-closed-redbc-1920x1080.png')
    await captureCeU14LocatorScreenshot(
      tasks,
      '06b-author-tasks-crop-redbc-1920x1080.png',
    )

    await switchBrand(page, 'GREENBC')
    await expect(page.locator('html')).toHaveAttribute('data-brand', 'GREENBC')
    await expect(page.locator('[data-partition-id="queue-TEST"]')).toHaveCount(0)
    await expect(page.locator('[data-partition-id="queue-APPROVAL"]')).toHaveCount(0)
    await expect(page.locator('[data-partition-id="queue-PENDING_RELEASE"]')).toHaveCount(0)
    await assertNoCeU15StepperDom(page)

    await captureCeU14Screenshot(page, '07-author-fail-closed-greenbc-1920x1080.png')
    await captureCeU14LocatorScreenshot(
      page.locator('#tasks-section'),
      '07b-author-tasks-crop-greenbc-1920x1080.png',
    )
    await captureCeU14LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '07c-brand-header-greenbc-crop.png',
    )

    // Direct queue deep-link must not invent a TEST partition for authors.
    await page.goto('/dashboard?queue=TEST#tasks-section')
    await dismissOnboardingTourIfPresent(page)
    await expect(page.locator('#tasks-section')).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('[data-partition-id="queue-TEST"] .el-table__row')).toHaveCount(0)
    await assertNoCeU15StepperDom(page)
    await captureCeU14Screenshot(page, '07d-author-queue-test-deeplink-greenbc-1920x1080.png')
  })
})
