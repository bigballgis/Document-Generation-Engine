import { describe, expect, it } from 'vitest'

import { resolveManualChunk } from '../../build/manualChunks'

describe('bundleStrategy (SOR-P06)', () => {
  it('splits element-plus into its own chunk', () => {
    expect(resolveManualChunk('/workspace/node_modules/element-plus/es/index.mjs')).toBe('element-plus')
  })

  it('splits vue ecosystem into vue-vendor chunk', () => {
    expect(resolveManualChunk('/workspace/node_modules/vue/dist/vue.runtime.esm-bundler.js')).toBe(
      'vue-vendor',
    )
    expect(resolveManualChunk('/workspace/node_modules/vue-router/dist/vue-router.mjs')).toBe('vue-vendor')
    expect(resolveManualChunk('/workspace/node_modules/pinia/dist/pinia.mjs')).toBe('vue-vendor')
  })

  it('splits icons into element-icons chunk', () => {
    expect(resolveManualChunk('/workspace/node_modules/@element-plus/icons-vue/dist/index.js')).toBe(
      'element-icons',
    )
  })

  it('returns undefined for application source', () => {
    expect(resolveManualChunk('/workspace/frontend/src/main.ts')).toBeUndefined()
  })

  it('splits pdfjs into pdfjs-vendor chunk', () => {
    expect(resolveManualChunk('/workspace/node_modules/pdfjs-dist/build/pdf.mjs')).toBe('pdfjs-vendor')
  })
})
