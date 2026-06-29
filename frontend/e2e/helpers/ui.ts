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
  await page.getByRole('button', { name: /sign out/i }).click()
  await expect(page).toHaveURL(/\/login/)
  await login(page, credentials)
}

export async function openContentModulesList(page: Page) {
  await page.goto('/content-modules')
  await expect(page.getByRole('heading', { name: /^content modules$/i })).toBeVisible()
  await expect(page.getByText(/unable to load content modules/i)).not.toBeVisible()
  await expect(page.locator('.el-skeleton')).toHaveCount(0)
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
