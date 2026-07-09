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
  expectNoForbiddenL1OnPrimarySurface,
  managementNav,
  myTodosNavSection,
} from './helpers/nav'
import { E2E_API_BASE_URL } from './helpers/masters-api'

const dockerTarget =
  process.env.E2E_TARGET === 'docker' || process.env.FRONTEND_PORT === '4173'
const defaultPort = dockerTarget ? 4173 : 5173
const FRONTEND_BASE_URL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${defaultPort}`

test.describe('P21-T01 behavior nav + L1 terminology (§12.2)', () => {
  test.beforeAll(async ({ request }) => {
    await requireDockerStack(request, { frontendBaseUrl: FRONTEND_BASE_URL, skipMessage: `Stack required (${FRONTEND_BASE_URL} + ${E2E_API_BASE_URL}). Start with .\\scripts\\docker-deploy.ps1 or pnpm dev + backend on :8080` })
  })

  test('TEMPLATE_TESTER sees My to-dos with testing entry only; deep-link to TEST queue', async ({
    page,
  }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)

    await expectBehaviorNavItems(page, [BEHAVIOR_NAV_LABELS.testing])

    await myTodosNavSection(page).getByRole('button', { name: BEHAVIOR_NAV_LABELS.testing }).click()
    await expect(page).toHaveURL(/\/dashboard\?queue=TEST/)
    expect(new URL(page.url()).hash).toBe('#tasks-section')
    await expect(page.getByRole('heading', { level: 1, name: /waiting on my testing/i })).toBeVisible()
    await expect(page.locator('#tasks-section')).toBeVisible()
  })

  test('GROUP_ADMIN sees all six behavior entries under My to-dos', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)

    await expectBehaviorNavItems(page, ALL_BEHAVIOR_LABELS)
  })

  test('GLOBAL_ADMIN sees all six behavior entries under My to-dos', async ({ page }) => {
    await loginAs(page, E2E_ADMIN)

    await expectBehaviorNavItems(page, ALL_BEHAVIOR_LABELS)
  })

  test('AUDIT_ADMIN has no My to-dos behavior group (hidden, not disabled)', async ({ page }) => {
    await loginAs(page, E2E_AUDIT_ADMIN)

    await expectMyTodosGroupAbsent(page)
    await expect(managementNav(page).getByRole('button', { name: BEHAVIOR_NAV_LABELS.approval })).toHaveCount(
      0,
    )
    await expect(page.getByRole('button', { name: BEHAVIOR_NAV_LABELS.approval, disabled: true })).toHaveCount(
      0,
    )

    await expect(managementNav(page).getByRole('button', { name: /^activity log$/i })).toBeVisible()
  })

  test('L1 terminology: nav and dashboard primary surfaces avoid IT jargon', async ({ page }) => {
    await loginAs(page, E2E_GROUP_ADMIN)
    await page.goto('/dashboard#tasks-section')

    await expect(page.getByRole('heading', { level: 1, name: /my tasks/i })).toBeVisible()
    await expect(page.getByText(/workflow to-dos for in-flight letter templates/i)).toBeVisible()
    await expect(page.locator('#tasks-section').getByRole('heading', { name: /^my to-dos$/i })).toBeVisible()

    await expectNoForbiddenL1OnPrimarySurface(page)

    await expect(managementNav(page).getByRole('button', { name: /letterhead templates/i })).toBeVisible()
    await expect(managementNav(page).getByRole('button', { name: /api management/i })).toBeVisible()
    await expect(managementNav(page).getByRole('button', { name: /activity log/i })).toBeVisible()
    await expect(managementNav(page).getByRole('button', { name: /\bapi policies\b/i })).toHaveCount(0)
    await expect(managementNav(page).getByRole('button', { name: /audit console/i })).toHaveCount(0)
  })

  test('resource-typed Templates nav remains accessible for tester', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)

    await managementNav(page).getByRole('button', { name: /^templates$/i }).click()
    await expect(page).toHaveURL(/\/templates/)
    await expect(page.getByRole('heading', { name: /^templates$/i })).toBeVisible()
    await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
  })

  test('overview dashboard item navigates without queue or filter params', async ({ page }) => {
    await loginAs(page, E2E_TEMPLATE_TESTER)
    await page.goto('/dashboard?queue=TEST#tasks-section')

    await managementNav(page).getByRole('button', { name: /^my tasks$/i }).click()
    await expect(page).toHaveURL(/\/dashboard(?:\/)?(?:\?[^#]*)?(?:#.*)?$/)
    expect(page.url()).not.toMatch(/queue=/)
    expect(page.url()).not.toMatch(/filter=/)
  })
})
