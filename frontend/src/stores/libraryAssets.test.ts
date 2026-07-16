import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as libraryAssetsApi from '@/api/libraryAssets'
import { useLibraryAssetsStore } from '@/stores/libraryAssets'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'

vi.mock('@/api/libraryAssets', () => ({
  listLibraryAssets: vi.fn(),
  uploadLibraryAsset: vi.fn(),
  disableLibraryAsset: vi.fn(),
}))

describe('libraryAssets store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockReset()
    vi.mocked(libraryAssetsApi.uploadLibraryAsset).mockReset()
    vi.mocked(libraryAssetsApi.disableLibraryAsset).mockReset()
  })

  it('loads a page of assets into state', async () => {
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockResolvedValue({
      content: [
        {
          assetKey: 'IMG-1',
          assetClass: 'IMAGE',
          status: 'ACTIVE',
          contentType: 'image/png',
          sizeBytes: 10,
          contentSha256: 'a'.repeat(64),
          originalFileName: 'a.png',
          uploadedBy: 'author',
          uploadedAt: '2026-07-16T00:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const store = useLibraryAssetsStore()
    await store.fetchAssets(0, 20, { q: 'IMG', status: 'ACTIVE' })

    expect(libraryAssetsApi.listLibraryAssets).toHaveBeenCalledWith(0, 20, {
      q: 'IMG',
      status: 'ACTIVE',
    })
    expect(store.assets).toHaveLength(1)
    expect(store.assetListTotalElements).toBe(1)
    expect(store.lastErrorMessageKey).toBeNull()
  })

  it('records a list error message key on failure', async () => {
    vi.mocked(libraryAssetsApi.listLibraryAssets).mockRejectedValue(
      axiosEnvelopeError(503, 'api.error.generation.serviceUnavailable', {
        retryable: true,
      }),
    )
    const store = useLibraryAssetsStore()
    await expect(store.fetchAssets()).rejects.toBeTruthy()
    expect(store.lastListErrorRetryable).toBe(true)
    expect(store.lastErrorMessageKey).toBeTruthy()
    expect(store.assets).toEqual([])
  })

  it('uploads and disables assets through the API module', async () => {
    const uploaded = {
      assetKey: 'IMG-2',
      assetClass: 'IMAGE' as const,
      status: 'ACTIVE' as const,
      contentType: 'image/png' as const,
      sizeBytes: 4,
      contentSha256: 'b'.repeat(64),
      originalFileName: 'b.png',
      uploadedBy: 'author',
      uploadedAt: '2026-07-16T01:00:00Z',
    }
    vi.mocked(libraryAssetsApi.uploadLibraryAsset).mockResolvedValue(uploaded)
    vi.mocked(libraryAssetsApi.disableLibraryAsset).mockResolvedValue({
      ...uploaded,
      status: 'DISABLED',
    })

    const store = useLibraryAssetsStore()
    const file = new File([new Uint8Array([1])], 'b.png', { type: 'image/png' })
    await expect(
      store.uploadAsset({ assetKey: 'IMG-2', assetClass: 'IMAGE', file }),
    ).resolves.toEqual(uploaded)
    await expect(store.disableAsset('IMG-2')).resolves.toMatchObject({ status: 'DISABLED' })
  })
})
