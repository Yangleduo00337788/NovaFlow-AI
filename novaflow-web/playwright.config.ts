import { defineConfig, devices } from '@playwright/test'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const adminAuthFile = 'e2e/.auth/admin.json'
const platformAuthFile = 'e2e/.auth/platform.json'
const portalAuthFile = 'e2e/.auth/portal.json'

const repoRoot = path.join(path.dirname(fileURLToPath(import.meta.url)), '..')
const apiBase = (process.env.NOVAFLOW_API_URL ?? 'http://localhost:8088').replace(/\/$/, '')

const desktopChrome = {
  ...devices['Desktop Chrome'],
  ...(process.env.CI ? {} : { channel: 'chrome' as const }),
}

function inheritedEnv(): Record<string, string> {
  const env: Record<string, string> = {}
  for (const [key, value] of Object.entries(process.env)) {
    if (typeof value === 'string') {
      env[key] = value
    }
  }
  env.NOVAFLOW_API_URL = apiBase
  return env
}

const frontendWebServer = {
  command: 'npm run dev -- --host 127.0.0.1 --port 3000 --strictPort',
  url: 'http://127.0.0.1:3000',
  reuseExistingServer: !process.env.CI,
  timeout: 180000,
  env: inheritedEnv(),
}

const backendWebServer = {
  command: process.platform === 'win32'
    ? 'mvn.cmd -q -pl novaflow-server spring-boot:run -DskipTests'
    : 'mvn -q -pl novaflow-server spring-boot:run -DskipTests',
  cwd: repoRoot,
  url: `${apiBase}/api/v1/health`,
  reuseExistingServer: true,
  timeout: 240000,
}

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : 1,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://127.0.0.1:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /global\.setup\.ts/,
      use: {
        ...desktopChrome,
      },
    },
    {
      name: 'chromium-auth',
      use: {
        ...desktopChrome,
      },
      testMatch: /auth\.spec\.ts|auth-expiry\.spec\.ts|double-submit\.spec\.ts|error-states\.spec\.ts/,
    },
    {
      name: 'chromium',
      use: {
        ...desktopChrome,
        storageState: adminAuthFile,
      },
      dependencies: ['setup'],
      testIgnore: [
        /auth\.spec\.ts/,
        /auth-expiry\.spec\.ts/,
        /double-submit\.spec\.ts/,
        /error-states\.spec\.ts/,
        /global\.setup\.ts/,
        /portal\.spec\.ts/,
        /embed\.spec\.ts/,
        /xss\.spec\.ts/,
        /roles\.spec\.ts/,
        /custom-roles\.spec\.ts/,
        /route-guard\.spec\.ts/,
        /platform-ops\.spec\.ts/,
        /developer-center\.spec\.ts/,
      ],
      grepInvert: /平台超管页面可加载/,
    },
    {
      name: 'chromium-platform',
      use: {
        ...desktopChrome,
        storageState: platformAuthFile,
      },
      dependencies: ['setup'],
      testMatch: /smoke-pages\.spec\.ts/,
      grep: /平台超管页面可加载/,
    },
    {
      name: 'chromium-platform-ops',
      use: {
        ...desktopChrome,
        storageState: platformAuthFile,
      },
      dependencies: ['setup'],
      testMatch: /platform-ops\.spec\.ts/,
    },
    {
      name: 'chromium-portal',
      use: {
        ...desktopChrome,
        storageState: portalAuthFile,
      },
      dependencies: ['setup'],
      testMatch: /portal\.spec\.ts/,
    },
    {
      name: 'chromium-embed',
      use: {
        ...desktopChrome,
      },
      testMatch: /embed\.spec\.ts|xss\.spec\.ts/,
    },
    {
      name: 'chromium-developer-center',
      use: {
        ...desktopChrome,
        storageState: adminAuthFile,
      },
      dependencies: ['setup'],
      testMatch: /developer-center\.spec\.ts/,
    },
    {
      name: 'chromium-roles',
      use: {
        ...desktopChrome,
        storageState: { cookies: [], origins: [] },
      },
      testMatch: /roles\.spec\.ts|custom-roles\.spec\.ts/,
    },
    {
      name: 'chromium-route-guard',
      use: {
        ...desktopChrome,
        storageState: { cookies: [], origins: [] },
      },
      testMatch: /route-guard\.spec\.ts/,
    },
  ],
  webServer: process.env.CI ? [frontendWebServer] : [backendWebServer, frontendWebServer],
})
