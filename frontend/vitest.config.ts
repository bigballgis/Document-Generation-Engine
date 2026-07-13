import { defineConfig } from 'vitest/config'
import { fileURLToPath, URL } from 'node:url'

import { createAppPlugins } from './vite.shared'

export default defineConfig({
  plugins: createAppPlugins({ elementPlusStyle: false }),
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts', 'tests/**/*.test.ts'],
    testTimeout: 20_000,
    coverage: {
      provider: 'v8',
      reporter: ['text-summary', 'json-summary'],
      include: ['src/**/*.{ts,vue}'],
      exclude: [
        'src/**/*.test.ts',
        'src/main.ts',
        'src/**/*.d.ts',
        'src/i18n/locales/**',
      ],
      // LR-C13 / OPT-B ratchet floors — set just below the 2026-07-11 measured
      // baseline of lines 81.46% / functions 56.58% / branches 82.41% / statements 81.46%.
      // Previous floors (pre-LR-C13): lines 22 / functions 32 / branches 55 / statements 22.
      thresholds: {
        lines: 80,
        functions: 55,
        branches: 80,
        statements: 80,
      },
    },
  },
})
