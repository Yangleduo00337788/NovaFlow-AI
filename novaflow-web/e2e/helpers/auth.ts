import { expect, type Page } from '@playwright/test'

export const ADMIN_EMAIL = 'admin@novaflow.ai'
export const ADMIN_PASSWORD = 'Admin123!'

/** 兼容旧引用 */
export const DEMO_EMAIL = ADMIN_EMAIL
export const DEMO_PASSWORD = ADMIN_PASSWORD

export const PLATFORM_EMAIL = 'platform@novaflow.ai'
export const PLATFORM_PASSWORD = 'Platform123!'

export const PORTAL_EMAIL = 'user@novaflow.ai'
export const PORTAL_PASSWORD = 'User123!'

export async function loginAs(page: Page, email: string, password: string, urlPattern: RegExp) {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(email)
  await page.getByTestId('login-password').fill(password)
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(urlPattern, { timeout: 15000 })
}

export async function login(page: Page) {
  await loginAs(page, ADMIN_EMAIL, ADMIN_PASSWORD, /\/dashboard/)
}

export async function loginAsPlatform(page: Page) {
  await loginAs(page, PLATFORM_EMAIL, PLATFORM_PASSWORD, /\/platform/)
}

export async function loginAsPortal(page: Page) {
  await loginAs(page, PORTAL_EMAIL, PORTAL_PASSWORD, /\/portal/)
}

export function uniqueName(prefix: string) {
  return `${prefix}-${Date.now()}`
}

/** Ant Design Popconfirm 在英文 locale 下按钮为 OK */
export async function confirmPopconfirm(page: Page) {
  const pop = page.locator('.ant-popconfirm, .ant-popover')
  await pop.getByRole('button', { name: /^(OK|确定)$/ }).click()
}
