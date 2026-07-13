import { expect, type Page } from '@playwright/test'

export async function confirmMessageBox(page: Page) {
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await box.getByRole('button', { name: /^(ok|confirm)$/i }).click()
}

export async function promptMessageBox(page: Page, value: string) {
  const box = page.locator('.el-message-box')
  await expect(box).toBeVisible()
  await box.locator('textarea, input').first().fill(value)
  await box.getByRole('button', { name: /^(ok|confirm)$/i }).click()
}

export async function reLoginAs(
  page: Page,
  login: (page: Page, credentials: { username: string; password: string }) => Promise<void>,
  credentials: { username: string; password: string },
) {
  // Sign out is inside the header user dropdown (ManagementShell), not a top-level button.
  await page.locator('header .user-menu-trigger').click()
  await page.getByRole('menuitem', { name: /sign out|退出登录/i }).click()
  await expect(page).toHaveURL(/\/login/)
  await login(page, credentials)
}

export async function openContentModulesList(page: Page) {
  await page.goto('/content-modules')
  await expect(page.getByRole('heading', { name: /^standard clauses$/i })).toBeVisible()
  await expect(page.getByText(/unable to load content modules/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
}

export async function filterContentModulesTableColumn(
  page: Page,
  columnLabel: string | RegExp,
  value: string,
) {
  const header = page
    .locator('.table-column-header')
    .filter({ has: page.getByText(columnLabel, { exact: typeof columnLabel === 'string' }) })
    .first()
  await header.locator('input').first().fill(value)
}

export async function openDemoTemplateAuthoringTab(page: Page, externalId: string) {
  await page.goto('/templates')
  await expect(page.getByText(/unable to load templates/i)).not.toBeVisible()
  await page.getByRole('row', { name: new RegExp(externalId) }).click()
  await expect(page).toHaveURL(/\/templates\/[^/?]+/)
  await page.getByRole('tab', { name: /authoring/i }).click()
}

export async function selectElementPlusOption(page: Page, optionText: string | RegExp) {
  await page.locator('.el-select-dropdown__item').filter({ hasText: optionText }).click()
}

export async function filterDashboardTasksByItem(page: Page, itemName: string) {
  const tasksSection = page.locator('#tasks-section')
  await expect(tasksSection).toBeVisible({ timeout: 30_000 })
  await expect(tasksSection.locator('.el-skeleton')).toHaveCount(0)

  const partition = tasksSection.locator('.task-partition').first()
  await expect(partition).toBeVisible()
  // Column filters are popover triggers (TableColumnHeader); EP keeps closed popovers in DOM.
  await partition.getByRole('button', { name: /^Filter Item$/i }).click()
  const filterInput = page.locator('.table-column-filter-popover input:visible')
  await expect(filterInput).toBeVisible()
  await filterInput.fill(itemName)
}

export async function expectDashboardPartitionHeading(page: Page, heading: string | RegExp) {
  await expect(page.locator('#tasks-section').getByRole('heading', { level: 3, name: heading })).toBeVisible()
}

export async function dashboardTaskRow(page: Page, itemName: string) {
  return page
    .locator('#tasks-section .el-table__row.app-data-table__activatable-row')
    .filter({ hasText: itemName })
    .first()
}
