import { defineConfig } from 'vitest/config'

export default defineConfig({
  // Stands in for the webgazer-assets Vite plugin, which only runs for the app itself.
  define: { __WEBGAZER_ASSET_DIR__: JSON.stringify('webgazer/0.0.0-test/') },
  test: {
    environment: 'node',
    include: [
      'tests/eyetracking/**/*.test.ts',
      'tests/m4/**/*.test.ts',
      'tests/m4/**/*.test.tsx',
      'tests/m5/**/*.test.ts',
      'tests/m5/**/*.test.tsx',
    ],
    clearMocks: true,
    restoreMocks: true,
  },
})
