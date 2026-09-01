import { expect, type Page } from '@playwright/test'

export const DEMO_EMAIL = 'admin@novaflow.ai'
export const DEMO_PASSWORD = 'Admin123!'

export async function login(page: Page) {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(DEMO_EMAIL)
  await page.getByTestId('login-password').fill(DEMO_PASSWORD)
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 })
}

export function uniqueName(prefix: string) {
  return `${prefix}-${Date.now()}`
}

/** Ant Design Popconfirm 在英文 locale 下按钮为 OK */
export async function confirmPopconfirm(page: Page) {
  const pop = page.locator('.ant-popconfirm, .ant-popover')
  await pop.getByRole('button', { name: /^(OK|确定)$/ }).click()
}
