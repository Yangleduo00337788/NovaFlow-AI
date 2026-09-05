import { defineConfig, devices } from '@playwright/test'

const adminAuthFile = 'e2e/.auth/admin.json'
const platformAuthFile = 'e2e/.auth/platform.json'
const portalAuthFile = 'e2e/.auth/portal.json'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : 1,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /global\.setup\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
      },
    },
    {
      name: 'chromium-auth',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
      },
      testMatch: /auth\.spec\.ts|auth-expiry\.spec\.ts|double-submit\.spec\.ts|error-states\.spec\.ts/,
    },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
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
      ],
      grepInvert: /平台超管页面可加载/,
    },
    {
      name: 'chromium-platform',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        storageState: platformAuthFile,
      },
      dependencies: ['setup'],
      testMatch: /smoke-pages\.spec\.ts/,
      grep: /平台超管页面可加载/,
    },
    {
      name: 'chromium-portal',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        storageState: portalAuthFile,
      },
      dependencies: ['setup'],
      testMatch: /portal\.spec\.ts/,
    },
    {
      name: 'chromium-embed',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
      },
      testMatch: /embed\.spec\.ts|xss\.spec\.ts/,
    },
    {
      name: 'chromium-roles',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        storageState: { cookies: [], origins: [] },
      },
      testMatch: /roles\.spec\.ts|custom-roles\.spec\.ts/,
    },
    {
      name: 'chromium-route-guard',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        storageState: { cookies: [], origins: [] },
      },
      testMatch: /route-guard\.spec\.ts/,
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
})
