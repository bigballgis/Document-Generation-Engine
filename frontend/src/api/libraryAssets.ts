import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { PageView } from '@/types/identity'
import type {
  LibraryAssetClass,
  LibraryAssetListQueryOptions,
  LibraryAssetView,
} from '@/types/libraryAsset'

export async function listLibraryAssets(
  page = 0,
  size = 20,
  options: LibraryAssetListQueryOptions = {},
): Promise<PageView<LibraryAssetView>> {
  const params: Record<string, string | number> = { page, size }
  const groupCode = options.groupCode?.trim()
  if (groupCode) {
    params.groupCode = groupCode
  }
  if (options.assetClass) {
    params.assetClass = options.assetClass
  }
  if (options.status) {
    params.status = options.status
  }
  if (options.q) {
    params.q = options.q
  }
  const response = await http.get<ApiEnvelope<PageView<LibraryAssetView>>>('/library/assets', {
    params,
    signal: options.signal,
  })
  return unwrapEnvelope(response.data)
}

export async function uploadLibraryAsset(payload: {
  groupCode: string
  assetKey: string
  assetClass: LibraryAssetClass
  file: File
}): Promise<LibraryAssetView> {
  const formData = new FormData()
  formData.append('groupCode', payload.groupCode.trim())
  formData.append('assetKey', payload.assetKey)
  formData.append('assetClass', payload.assetClass)
  formData.append('file', payload.file)

  const response = await http.post<ApiEnvelope<LibraryAssetView>>('/library/assets', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrapEnvelope(response.data)
}

export async function disableLibraryAsset(
  assetKey: string,
  groupCode: string,
): Promise<LibraryAssetView> {
  const response = await http.post<ApiEnvelope<LibraryAssetView>>(
    `/library/assets/${encodeURIComponent(assetKey)}/disable`,
    null,
    { params: { groupCode: groupCode.trim() } },
  )
  return unwrapEnvelope(response.data)
}
