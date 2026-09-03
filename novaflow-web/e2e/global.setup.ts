import { test as setup } from '@playwright/test'
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  PLATFORM_EMAIL,
  PLATFORM_PASSWORD,
  PORTAL_EMAIL,
  PORTAL_PASSWORD,
} from './helpers/auth'

const adminAuthFile = 'e2e/.auth/admin.json'
const platformAuthFile = 'e2e/.auth/platform.json'
const portalAuthFile = 'e2e/.auth/portal.json'

setup('authenticate as tenant admin', async ({ page }) => {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(ADMIN_EMAIL)
  await page.getByTestId('login-password').fill(ADMIN_PASSWORD)
  await page.getByTestId('login-submit').click()
  await page.waitForURL(/\/dashboard/, { timeout: 15000 })
  await page.context().storageState({ path: adminAuthFile })
})

setup('authenticate as platform super admin', async ({ page }) => {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(PLATFORM_EMAIL)
  await page.getByTestId('login-password').fill(PLATFORM_PASSWORD)
  await page.getByTestId('login-submit').click()
  await page.waitForURL(/\/platform/, { timeout: 15000 })
  await page.context().storageState({ path: platformAuthFile })
})

setup('authenticate as portal user', async ({ page }) => {
  await page.goto('/login')
  await page.getByTestId('login-email').fill(PORTAL_EMAIL)
  await page.getByTestId('login-password').fill(PORTAL_PASSWORD)
  await page.getByTestId('login-submit').click()
  await page.waitForURL(/\/portal/, { timeout: 15000 })
  await page.context().storageState({ path: portalAuthFile })
})
