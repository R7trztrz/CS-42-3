import { defineConfig } from 'vitest/config'

export default defineConfig({
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
