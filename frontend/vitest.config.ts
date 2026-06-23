import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts', 'tests/**/*.test.ts'],
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
      // OPT-B / B3 ratchet floors (non-regression, set just below the 2026-06-23
      // baseline of lines 23.6% / functions 34.7% / branches 64.5%). Raise these
      // toward 80% as OPT-C adds tests for the large untested views.
      thresholds: {
        lines: 22,
        functions: 32,
        branches: 55,
        statements: 22,
      },
    },
  },
})
