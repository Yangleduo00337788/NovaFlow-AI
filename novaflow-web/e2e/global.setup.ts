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
import { cleanupE2ETestResources } from './helpers/api'

const adminAuthFile = 'e2e/.auth/admin.json'
const platformAuthFile = 'e2e/.auth/platform.json'
const portalAuthFile = 'e2e/.auth/portal.json'

function ensureAuthDir(file: string) {
  mkdirSync(dirname(file), { recursive: true })
}

const apiBase = (process.env.NOVAFLOW_API_URL ?? 'http://localhost:8088').replace(/\/$/, '')

async function isHealthy(response: import('@playwright/test').APIResponse) {
  if (!response.ok()) {
    return false
  }
  try {
    const body = await response.json()
    return body.code === 0 || body.status === 'UP' || body.data?.status === 'UP'
  } catch {
    return true
  }
}

async function waitForBackend(request: import('@playwright/test').APIRequestContext) {
  for (let attempt = 0; attempt < 60; attempt++) {
    try {
      const direct = await request.get(`${apiBase}/api/v1/health`)
      if (await isHealthy(direct)) {
        return
      }
    } catch {
      // retry until backend is ready; Vite proxy is required only after webServer is up
    }
    await new Promise((resolve) => setTimeout(resolve, 2000))
  }
  throw new Error(`Backend API is not ready for E2E setup (${apiBase}/api/v1/health)`)
}

setup('wait for backend API', async ({ request }) => {
  await waitForBackend(request)
})

setup('cleanup stale E2E resources', async ({ request }) => {
  setup.setTimeout(180_000)
  await cleanupE2ETestResources(request)
})

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
