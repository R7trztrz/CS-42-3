import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/browser',
  testMatch: '**/*.spec.ts',
  fullyParallel: false,
  workers: 1,
  use: {
    baseURL: 'http://127.0.0.1:4186',
    headless: true,
    ...(process.env.FR49_BROWSER_CHANNEL ? { channel: process.env.FR49_BROWSER_CHANNEL } : {}),
    // Points at a Chromium already on the machine when the bundled build is not downloaded.
    ...(process.env.EYETRACKING_BROWSER_EXECUTABLE
      ? { launchOptions: { executablePath: process.env.EYETRACKING_BROWSER_EXECUTABLE } }
      : {}),
  },
  webServer: {
    command: 'node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 4186 --strictPort',
    url: 'http://127.0.0.1:4186',
    reuseExistingServer: false,
  },
})
