import { describe, expect, it } from 'vitest'
import { blobToArrayBuffer } from '@/utils/blobToArrayBuffer'

describe('blobToArrayBuffer', () => {
  it('reads bytes from a Blob via arrayBuffer', async () => {
    const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    const buffer = await blobToArrayBuffer(blob)
    expect(buffer.byteLength).toBeGreaterThan(0)
  })

  it('falls back to Response when arrayBuffer is unavailable', async () => {
    const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' })
    Reflect.deleteProperty(blob, 'arrayBuffer')
    const buffer = await blobToArrayBuffer(blob)
    expect(buffer.byteLength).toBeGreaterThan(0)
  })
})
