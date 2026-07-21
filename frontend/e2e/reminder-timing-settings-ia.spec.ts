/**
 * Reminder timing settings IA / Task Master #153
 *
 * BDD SoT: docs/behavior/reminder-timing-settings-ia.md
 *   BDD-RT-IA-001…007, 009, 011, 013…015 (UI journeys)
 *   BDD-RT-IA-008/012/016 — API/escalation contract covered elsewhere
 *
 * Run against Docker acceptance stack:
 *   pnpm -C frontend exec playwright test e2e/reminder-timing-settings-ia.spec.ts `
 *     --config playwright.docker.config.ts --workers=1
 */
import { expect, test, type Page, type Request } from '@playwright/test'

import {
  E2E_ADMIN,
  E2E_GROUP_ADMIN,
  E2E_LEGAL_REVIEWER,
  E2E_TEMPLATE_AUTHOR,
  E2E_TEMPLATE_TESTER,
  loginAs,
} from './helpers/auth'
import {
  getCollaborationTimeoutConfig,
} from './helpers/collaboration-api'
import { E2E_API_BASE_URL } from './helpers/masters-api'
import { managementNav } from './helpers/nav'
import { requireDockerStack } from './helpers/stack-readiness'
import { reLoginAs } from './helpers/ui'
import { dismissOnboardingTourIfPresent } from './helpers/uiux-evidence'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

const REMINDER_TIMING_PATH = '/system/settings/reminder-timing'
const SYSTEM_SETTINGS_NAV = /^system settings$/i
const TEAM_SETTINGS = /^team settings$/i
const REMINDER_TIMING = /reminder timing/i

function timeoutPanel(page: Page) {
  return page.locator('.timeout-config-card')
}

function isTimeoutConfigPut(request: Request): boolean {
  return (
    request.method() === 'PUT' &&
    request.url().includes('/api/management/v1/collaboration-timeout-config')
  )
}

async function expectDashboardOverviewWithoutTimeoutPanel(page: Page) {
  await page.goto('/dashboard')
  await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
  await expect(page.locator('.timeout-config-card')).toHaveCount(0)
  await expect(page.getByRole('heading', { name: REMINDER_TIMING })).toHaveCount(0)
  await expect(page.getByRole('button', { name: TEAM_SETTINGS })).toHaveCount(0)
}

test.describe('Reminder timing settings IA (BDD-RT-IA)', () => {
  test.describe.configure({ mode: 'serial' })

  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, {
      frontendBaseUrl: FRONTEND_BASE_URL,
      skipMessage: `Docker stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy-queue.ps1`,
    })
  })

  test('BDD-RT-IA-001/014: GLOBAL_ADMIN opens System settings Reminder timing full page', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    const nav = managementNav(page)
    await expect(nav.getByRole('button', { name: SYSTEM_SETTINGS_NAV })).toBeVisible()
    await nav.getByRole('button', { name: SYSTEM_SETTINGS_NAV }).click()

    await expect(page).toHaveURL(new RegExp(`${REMINDER_TIMING_PATH}$`))
    await expect(page.getByRole('heading', { level: 1, name: REMINDER_TIMING })).toBeVisible()
    await expect(page.locator('.app-page-layout')).toHaveClass(/app-page-layout--fluid/)
    await expect(page.getByRole('dialog')).toHaveCount(0)

    const panel = timeoutPanel(page)
    await expect(panel).toBeVisible()
    await expect(panel.getByRole('heading', { name: REMINDER_TIMING })).toBeVisible()
    await expect(panel.getByText(/notifications only/i)).toBeVisible()
    await expect(panel.getByText(/configuration scope/i)).toHaveCount(0)
    await expect(panel.getByText(/global default/i)).toHaveCount(0)
    await expect(panel.getByText(/group override/i)).toHaveCount(0)
    await expect(panel.getByLabel(/group code/i)).toHaveCount(0)
  })
  test('BDD-RT-IA-002/009/011: GLOBAL_ADMIN saves Global default only', async ({ page, request }) => {
    const uniqueHours = 51 + (Date.now() % 10)

    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    await page.goto(REMINDER_TIMING_PATH)
    const panel = timeoutPanel(page)
    await expect(panel).toBeVisible()

    const putPromise = page.waitForRequest(isTimeoutConfigPut)
    const testThresholdInput = panel
      .locator('.el-form-item')
      .filter({ hasText: /testing reminder after/i })
      .locator('.el-input-number input')
    await testThresholdInput.fill(String(uniqueHours))
    await panel.getByRole('button', { name: /save reminder timing/i }).click()

    const putRequest = await putPromise
    const body = putRequest.postDataJSON() as {
      scopeType: string
      groupCode: string | null
      testThresholdHours: number
    }
    expect(body.scopeType).toBe('GLOBAL')
    expect(body.groupCode).toBeNull()
    expect(body.testThresholdHours).toBe(uniqueHours)

    await expect(page.locator('.el-message').getByText(/reminder timing saved/i)).toBeVisible()

    const saved = await getCollaborationTimeoutConfig(request, E2E_ADMIN)
    expect(saved.testThresholdHours).toBe(uniqueHours)
  })

  test('BDD-RT-IA-005/015: Dashboard Overview has no Reminder timing / Team settings', async ({
    page,
  }) => {
    await loginAs(page, E2E_ADMIN)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    await expectDashboardOverviewWithoutTimeoutPanel(page)

    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
    await reLoginAs(page, loginAs, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    await expectDashboardOverviewWithoutTimeoutPanel(page)
  })

  test('BDD-RT-IA-003/004/009: GROUP_ADMIN Team settings dialog saves group override', async ({
    page,
    request,
  }) => {
    const uniqueHours = 61 + (Date.now() % 10)

    await loginAs(page, E2E_GROUP_ADMIN)
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 3_000 })
    await expect(managementNav(page).getByRole('button', { name: SYSTEM_SETTINGS_NAV })).toHaveCount(
      0,
    )

    await page.goto('/entitlement/groups')
    await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
    await expect(page.getByRole('heading', { level: 1, name: /group management/i })).toBeVisible()
    await expect(page.getByTestId('team-settings-button')).toBeVisible()
    await page.getByTestId('team-settings-button').click()
    const dialog = page.getByRole('dialog', { name: TEAM_SETTINGS })
    await expect(dialog).toBeVisible()
    const panel = dialog.locator('.timeout-config-card')
    await expect(panel.getByRole('heading', { name: REMINDER_TIMING })).toBeVisible()
    await expect(panel.getByText(/notifications only/i)).toBeVisible()
    await expect(panel.getByText(/configuration scope/i)).toHaveCount(0)
    await expect(panel.getByText(/global default/i)).toHaveCount(0)
    const groupCodeInput = panel
      .locator('.el-form-item')
      .filter({ hasText: /group code/i })
      .locator('input')
    await expect(groupCodeInput).toBeVisible()
    await expect(groupCodeInput).toHaveAttribute('readonly', '')
    const authorizedGroupCode = (await groupCodeInput.inputValue()).trim()
    expect(authorizedGroupCode.length).toBeGreaterThan(0)

    const putPromise = page.waitForRequest(isTimeoutConfigPut)
    const testThresholdInput = panel
      .locator('.el-form-item')
      .filter({ hasText: /testing reminder after/i })
      .locator('.el-input-number input')
    await testThresholdInput.fill(String(uniqueHours))
    await panel.getByRole('button', { name: /save reminder timing/i }).click()

    const putRequest = await putPromise
    const body = putRequest.postDataJSON() as {
      scopeType: string
      groupCode: string | null
      testThresholdHours: number
    }
    expect(body.scopeType).toBe('GROUP')
    expect(body.groupCode).toBe(authorizedGroupCode)
    expect(body.testThresholdHours).toBe(uniqueHours)

    await expect(page.locator('.el-message').getByText(/reminder timing saved/i)).toBeVisible()

    const saved = await getCollaborationTimeoutConfig(
      request,
      E2E_GROUP_ADMIN,
      authorizedGroupCode,
    )
    expect(saved.testThresholdHours).toBe(uniqueHours)

    await dialog.getByRole('button', { name: /close/i }).click()
    await expect(dialog).toBeHidden()
    await expect(page.getByRole('heading', { level: 1, name: /group management/i })).toBeVisible()
  })
  test('BDD-RT-IA-007: GROUP_ADMIN deep-link to System settings is fail-closed', async ({
    page,
  }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto(REMINDER_TIMING_PATH)
    await expect(page).toHaveURL(/\/forbidden/)
    await expect(page.getByText(/access denied/i)).toBeVisible()
    await expect(page.locator('.timeout-config-card')).toHaveCount(0)
  })

  for (const actor of [
    { label: 'DOCUMENT_AUTHOR', credentials: E2E_TEMPLATE_AUTHOR },
    { label: 'TEMPLATE_TESTER', credentials: E2E_TEMPLATE_TESTER },
    { label: 'LEGAL_REVIEWER', credentials: E2E_LEGAL_REVIEWER },
  ]) {
    test(`BDD-RT-IA-006: ${actor.label} cannot reach Reminder timing edit surfaces`, async ({
      page,
    }) => {
      await loginAs(page, actor.credentials)
      await dismissOnboardingTourIfPresent(page, { appearTimeoutMs: 2_000 })
      await expect(
        managementNav(page).getByRole('button', { name: SYSTEM_SETTINGS_NAV }),
      ).toHaveCount(0)

      await page.goto('/entitlement/groups')
      // Authors may be forbidden on groups; admins without maintain still hide Team settings.
      if (!/\/forbidden/.test(page.url())) {
        await expect(page.getByTestId('team-settings-button')).toHaveCount(0)
      }

      await page.goto(REMINDER_TIMING_PATH)
      await expect(page).toHaveURL(/\/forbidden/)
      await expect(page.getByText(/access denied/i)).toBeVisible()
      await expect(page.locator('.timeout-config-card')).toHaveCount(0)
    })
  }
})
