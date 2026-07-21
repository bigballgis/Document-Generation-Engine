export type LibraryAssetClass = 'IMAGE' | 'SEAL' | 'OTHER'
export type LibraryAssetStatus = 'ACTIVE' | 'DISABLED'
export type LibraryAssetListStatusFilter = 'ACTIVE' | 'DISABLED' | 'ALL'

/** Group-scoped catalog metadata row (CE-E02 + ALGI). Binary bytes are never returned. */
export interface LibraryAssetView {
  groupCode: string
  assetKey: string
  assetClass: LibraryAssetClass
  status: LibraryAssetStatus
  contentType: 'image/png' | 'image/jpeg'
  sizeBytes: number
  contentSha256: string
  originalFileName: string
  uploadedBy: string
  uploadedAt: string
}

export interface LibraryAssetListQueryOptions {
  signal?: AbortSignal
  groupCode?: string
  assetClass?: LibraryAssetClass
  status?: LibraryAssetListStatusFilter
  q?: string
}

export const LIBRARY_ASSET_KEY_PATTERN = /^[A-Za-z][A-Za-z0-9._-]{0,127}$/
export const LIBRARY_ASSET_MAX_BYTES = 5 * 1024 * 1024
export const LIBRARY_ASSET_ACCEPTED_MIME = ['image/png', 'image/jpeg'] as const
