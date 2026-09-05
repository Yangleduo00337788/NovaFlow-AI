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

export const DEVELOPER_EMAIL = 'developer@novaflow.ai'
export const DEVELOPER_PASSWORD = 'Developer123!'

export const OPERATOR_EMAIL = 'operator@novaflow.ai'
export const OPERATOR_PASSWORD = 'Operator123!'

export const VIEWER_EMAIL = 'viewer@novaflow.ai'
export const VIEWER_PASSWORD = 'Viewer123!'

/** 六角色演示账号（与 README / DataInitializer 一致） */
export const ROLE_ACCOUNTS = [
  { label: '企业所有者', email: ADMIN_EMAIL, password: ADMIN_PASSWORD, home: /\/dashboard/ },
  { label: '开发者', email: DEVELOPER_EMAIL, password: DEVELOPER_PASSWORD, home: /\/dashboard/ },
  { label: '运维人员', email: OPERATOR_EMAIL, password: OPERATOR_PASSWORD, home: /\/dashboard/ },
  { label: '企业成员', email: PORTAL_EMAIL, password: PORTAL_PASSWORD, home: /\/portal/ },
  { label: '只读用户', email: VIEWER_EMAIL, password: VIEWER_PASSWORD, home: /\/dashboard/ },
  { label: '平台超管', email: PLATFORM_EMAIL, password: PLATFORM_PASSWORD, home: /\/platform/ },
] as const

export async function loginAs(page: Page, email: string, password: string, urlPattern: RegExp) {
  const loginPath = urlPattern.test('/platform') ? '/platform/login' : '/login'
  await page.goto(loginPath)
  await page.getByTestId('login-email').fill(email)
  await page.getByTestId('login-password').fill(password)
  await page.getByTestId('login-submit').click()
  await expect(page).toHaveURL(
    (url) => {
      const path = typeof url === 'string' ? new URL(url).pathname : url.pathname
      return !path.includes('/login') && urlPattern.test(path)
    },
    { timeout: 15000 },
  )
}

export async function loginAsPlatform(page: Page) {
  await loginAs(page, PLATFORM_EMAIL, PLATFORM_PASSWORD, /\/platform/)
}

export async function login(page: Page) {
  await loginAs(page, ADMIN_EMAIL, ADMIN_PASSWORD, /\/dashboard/)
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
