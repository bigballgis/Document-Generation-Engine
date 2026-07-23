import fs from 'node:fs'
import path from 'node:path'
import { defineConfig } from 'vitest/config'
import { fileURLToPath, URL } from 'node:url'

import { createAppPlugins } from './vite.shared'

const configDir = path.dirname(fileURLToPath(import.meta.url))

// Windows: ensure coverage/.tmp exists before v8 workers write shard JSON.
fs.mkdirSync(path.join(configDir, 'coverage', '.tmp'), { recursive: true })

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
    // Element Plus + jsdom mounts regularly exceed 20s under coverage on this host.
    testTimeout: 40_000,
    // Windows: forks + capped workers reduce coverage/.tmp shard ENOENT races.
    pool: 'forks',
    maxWorkers: 2,
    coverage: {
      provider: 'v8',
      reporter: ['text-summary', 'json-summary'],
      // Avoid rimraf of coverage/.tmp while workers still flush shards (Windows ENOENT).
      clean: false,
      processingConcurrency: 1,
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
