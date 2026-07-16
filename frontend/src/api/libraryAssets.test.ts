import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from '@/api/http'
import * as libraryAssetsApi from '@/api/libraryAssets'

vi.mock('@/api/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('libraryAssets API', () => {
  beforeEach(() => {
    vi.mocked(http.get).mockReset()
    vi.mocked(http.post).mockReset()
  })

  it('lists assets with page/size and optional filters', async () => {
    vi.mocked(http.get).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          content: [
            {
              assetKey: 'IMG-LOGO',
              assetClass: 'IMAGE',
              status: 'ACTIVE',
              contentType: 'image/png',
              sizeBytes: 1200,
              contentSha256: 'a'.repeat(64),
              originalFileName: 'logo.png',
              uploadedBy: 'author',
              uploadedAt: '2026-07-16T00:00:00Z',
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        },
      },
    })

    const pageView = await libraryAssetsApi.listLibraryAssets(0, 20, {
      assetClass: 'IMAGE',
      status: 'ACTIVE',
      q: 'IMG',
    })

    expect(http.get).toHaveBeenCalledWith('/library/assets', {
      params: { page: 0, size: 20, assetClass: 'IMAGE', status: 'ACTIVE', q: 'IMG' },
      signal: undefined,
    })
    expect(pageView.content).toHaveLength(1)
    expect(pageView.content[0]?.assetKey).toBe('IMG-LOGO')
  })

  it('uploads multipart asset payload', async () => {
    const file = new File([new Uint8Array([1, 2, 3])], 'logo.png', { type: 'image/png' })
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          assetKey: 'IMG-LOGO',
          assetClass: 'IMAGE',
          status: 'ACTIVE',
          contentType: 'image/png',
          sizeBytes: 3,
          contentSha256: 'b'.repeat(64),
          originalFileName: 'logo.png',
          uploadedBy: 'author',
          uploadedAt: '2026-07-16T00:00:00Z',
        },
      },
    })

    const result = await libraryAssetsApi.uploadLibraryAsset({
      assetKey: 'IMG-LOGO',
      assetClass: 'IMAGE',
      file,
    })

    expect(http.post).toHaveBeenCalledWith(
      '/library/assets',
      expect.any(FormData),
      expect.objectContaining({
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    )
    const formData = vi.mocked(http.post).mock.calls[0]?.[1] as FormData
    expect(formData.get('assetKey')).toBe('IMG-LOGO')
    expect(formData.get('assetClass')).toBe('IMAGE')
    expect(formData.get('file')).toBe(file)
    expect(result.assetKey).toBe('IMG-LOGO')
  })

  it('disables an asset by key', async () => {
    vi.mocked(http.post).mockResolvedValue({
      data: {
        metadata: {},
        result: {
          assetKey: 'IMG-LOGO',
          assetClass: 'IMAGE',
          status: 'DISABLED',
          contentType: 'image/png',
          sizeBytes: 3,
          contentSha256: 'b'.repeat(64),
          originalFileName: 'logo.png',
          uploadedBy: 'admin',
          uploadedAt: '2026-07-16T00:00:00Z',
        },
      },
    })

    const result = await libraryAssetsApi.disableLibraryAsset('IMG-LOGO')

    expect(http.post).toHaveBeenCalledWith('/library/assets/IMG-LOGO/disable')
    expect(result.status).toBe('DISABLED')
  })
})
