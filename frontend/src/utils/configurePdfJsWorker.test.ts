import { describe, expect, it, vi, beforeEach } from 'vitest'

vi.mock('pdfjs-dist/build/pdf.worker.min.mjs?url', () => ({
  default: 'mock-pdf-worker.js',
}))

describe('configurePdfJsWorker', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  it('sets workerSrc once across repeated calls', async () => {
    const first = await import('@/utils/configurePdfJsWorker')
    first.configurePdfJsWorker()
    const firstSrc = first.pdfjs.GlobalWorkerOptions.workerSrc

    first.configurePdfJsWorker()
    expect(first.pdfjs.GlobalWorkerOptions.workerSrc).toBe(firstSrc)
    expect(firstSrc).toBe('mock-pdf-worker.js')
  })
})
