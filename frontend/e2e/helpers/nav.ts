import { expect, type Locator, type Page } from '@playwright/test'

/** P21 §12.2 behavior nav item labels (en baseline). */
export const BEHAVIOR_NAV_LABELS = {
  testing: /waiting on my testing/i,
  approval: /waiting on my approval/i,
  remediation: /waiting on my fixes/i,
  pendingRelease: /waiting to confirm go-live/i,
  escalation: /overdue to follow up/i,
  masterReview: /masters to review/i,
} as const

export const ALL_BEHAVIOR_LABELS = Object.values(BEHAVIOR_NAV_LABELS)

/** Forbidden L1 nouns on nav + dashboard primary surfaces (P21 §12.2 Spec B). */
export const FORBIDDEN_L1_PATTERN =
  /\b(policy|credential|lifecycle|semver|gate|anchor integrity|governance overview|audit console)\b/i

export function managementNav(page: Page): Locator {
  return page.getByRole('navigation', { name: /management navigation/i })
}

export function myTodosNavSection(page: Page): Locator {
  return managementNav(page).locator('section').filter({
    has: page.getByRole('heading', { name: /^my to-dos$/i }),
  })
}

export async function expectMyTodosGroupVisible(page: Page) {
  await expect(managementNav(page).getByRole('heading', { name: /^my to-dos$/i })).toBeVisible()
}

export async function expectMyTodosGroupAbsent(page: Page) {
  await expect(managementNav(page).getByRole('heading', { name: /^my to-dos$/i })).toHaveCount(0)
  for (const label of ALL_BEHAVIOR_LABELS) {
    await expect(managementNav(page).getByRole('button', { name: label })).toHaveCount(0)
  }
}

export async function expectBehaviorNavItems(page: Page, labels: RegExp[]) {
  await expectMyTodosGroupVisible(page)
  const section = myTodosNavSection(page)
  await expect(section.getByRole('button')).toHaveCount(labels.length)
  for (const label of labels) {
    await expect(section.getByRole('button', { name: label })).toBeVisible()
  }
  for (const hidden of ALL_BEHAVIOR_LABELS.filter(
    (candidate) => !labels.some((visible) => visible.source === candidate.source),
  )) {
    await expect(managementNav(page).getByRole('button', { name: hidden })).toHaveCount(0)
  }
}

export async function expectNoForbiddenL1OnPrimarySurface(page: Page) {
  const nav = managementNav(page)
  await expect(nav).toBeVisible()
  const navText = (await nav.innerText()).toLowerCase()
  expect(navText).not.toMatch(FORBIDDEN_L1_PATTERN)

  const main = page.locator('main.shell-content')
  await expect(main).toBeVisible()
  const headerArea = main.locator('.page-header, .section-header').first()
  if (await headerArea.count()) {
    const headerText = (await headerArea.innerText()).toLowerCase()
    expect(headerText).not.toMatch(FORBIDDEN_L1_PATTERN)
  }
}
