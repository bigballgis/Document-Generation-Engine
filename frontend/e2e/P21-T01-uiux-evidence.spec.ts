import { expect, test } from '@playwright/test'

import { requireDockerStack } from './helpers/stack-readiness'

import {
  E2E_ADMIN,
  E2E_AUDIT_ADMIN,
  E2E_GROUP_ADMIN,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  ALL_BEHAVIOR_LABELS,
  BEHAVIOR_NAV_LABELS,
  expectBehaviorNavItems,
  expectMyTodosGroupAbsent,
  managementNav,
  myTodosNavSection,
} from './helpers/nav'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { reLoginAs } from './helpers/ui'
import {
  captureP21T01LocatorScreenshot,
  captureP21T01Screenshot,
  ensureP21T01EvidenceDirs,
  P21_T01_VIEWPORT,
  switchBrand,
} from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T01 UIUX evidence', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeAll(async ({ request }) => {
    ensureP21T01EvidenceDirs()

    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1 or pnpm dev + backend on :8080` })
  })

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(P21_T01_VIEWPORT)
  })

  test('capture behavior nav, dashboard queue landing, and dual-brand header evidence', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await expectBehaviorNavItems(page, [BEHAVIOR_NAV_LABELS.testing])
    await captureP21T01LocatorScreenshot(
      page.locator('.shell-nav'),
      '01-tester-sidebar-my-todos-redbc-1440x900.png',
    )

    await myTodosNavSection(page).getByRole('button', { name: BEHAVIOR_NAV_LABELS.testing }).click()
    await expect(page).toHaveURL(/\/dashboard\?queue=TEST/)
    expect(new URL(page.url()).hash).toBe('#tasks-section')
    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.locator('#tasks-section')).toBeVisible()
    await captureP21T01Screenshot(page, '02-dashboard-queue-test-landing-redbc-1440x900.png')

    await captureP21T01LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '07-brand-header-redbc-1440x900.png',
    )
    await switchBrand(page, 'GREENBC')
    await captureP21T01LocatorScreenshot(
      page.locator('.shell-header .header-brand'),
      '08-brand-header-greenbc-1440x900.png',
    )
    await captureP21T01Screenshot(page, '03-dashboard-queue-test-landing-greenbc-1440x900.png')

    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await expectBehaviorNavItems(page, ALL_BEHAVIOR_LABELS)
    await captureP21T01LocatorScreenshot(
      page.locator('.shell-nav'),
      '04-group-admin-sidebar-six-entries-redbc-1440x900.png',
    )

    await reLoginAs(page, loginAs, E2E_ADMIN)
    await expectBehaviorNavItems(page, ALL_BEHAVIOR_LABELS)
    await captureP21T01LocatorScreenshot(
      page.locator('.shell-nav'),
      '05-global-admin-sidebar-six-entries-redbc-1440x900.png',
    )

    await reLoginAs(page, loginAs, E2E_AUDIT_ADMIN)
    await expectMyTodosGroupAbsent(page)
    await expect(managementNav(page).getByRole('button', { name: /^activity log$/i })).toBeVisible()
    await captureP21T01LocatorScreenshot(
      page.locator('.shell-nav'),
      '06-audit-admin-no-my-todos-group-redbc-1440x900.png',
    )
  })
})
