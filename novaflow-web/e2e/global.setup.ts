import { mkdirSync } from 'fs'
import { dirname } from 'path'
import { test as setup } from '@playwright/test'
import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  PLATFORM_EMAIL,
  PLATFORM_PASSWORD,
  PORTAL_EMAIL,
  PORTAL_PASSWORD,
  loginAs,
} from './helpers/auth'

const adminAuthFile = 'e2e/.auth/admin.json'
const platformAuthFile = 'e2e/.auth/platform.json'
const portalAuthFile = 'e2e/.auth/portal.json'

function ensureAuthDir(file: string) {
  mkdirSync(dirname(file), { recursive: true })
}

setup('authenticate as tenant admin', async ({ page }) => {
  await loginAs(page, ADMIN_EMAIL, ADMIN_PASSWORD, /\/dashboard/)
  ensureAuthDir(adminAuthFile)
  await page.context().storageState({ path: adminAuthFile })
})

setup('authenticate as platform super admin', async ({ page }) => {
  await loginAs(page, PLATFORM_EMAIL, PLATFORM_PASSWORD, /\/platform/)
  ensureAuthDir(platformAuthFile)
  await page.context().storageState({ path: platformAuthFile })
})

setup('authenticate as portal user', async ({ page }) => {
  await loginAs(page, PORTAL_EMAIL, PORTAL_PASSWORD, /\/portal/)
  await page.locator('.portal-client').waitFor({ state: 'visible', timeout: 15000 })
  ensureAuthDir(portalAuthFile)
  await page.context().storageState({ path: portalAuthFile })
})
