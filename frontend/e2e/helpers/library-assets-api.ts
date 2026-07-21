import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import type { APIRequestContext } from '@playwright/test'

import { DEMO_GROUP_CODE, E2E_ADMIN } from './auth'
import { E2E_API_BASE_URL } from './masters-api'

const FIXTURES_DIR = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'fixtures')
export const E2E_ASSET_PNG_PATH = path.join(FIXTURES_DIR, 'e2e-asset-1x1.png')
export const E2E_ASSET_PNG_FILENAME = 'e2e-asset-1x1.png'

export type LibraryAssetClass = 'IMAGE' | 'SEAL' | 'OTHER'
export type LibraryAssetStatus = 'ACTIVE' | 'DISABLED'

export interface LibraryAssetView {
  groupCode: string
  assetKey: string
  assetClass: LibraryAssetClass
  status: LibraryAssetStatus
  contentType: string
  sizeBytes: number
  originalFileName: string
  uploadedBy: string
  uploadedAt: string
}

interface ApiEnvelope<T> {
  result: T
}

interface PageView<T> {
  content: T[]
  totalElements: number
}

async function apiLogin(
  request: APIRequestContext,
  credentials: { username: string; password: string } = E2E_ADMIN,
): Promise<string> {
  const response = await request.post(`${E2E_API_BASE_URL}/auth/login`, {
    data: credentials,
  })
  if (!response.ok()) {
    throw new Error(`API login failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<{ accessToken: string }>
  return body.result.accessToken
}

export function uniqueE2eAssetKey(prefix: string): string {
  const stamp = Date.now().toString(36)
  const rand = Math.random().toString(36).slice(2, 8)
  return `${prefix}-${stamp}-${rand}`
}

export async function uploadLibraryAssetViaApi(
  request: APIRequestContext,
  options: {
    assetKey: string
    assetClass: LibraryAssetClass
    /** Owning business group (required by ALGI). Defaults to RETAIL demo group. */
    groupCode?: string
    credentials?: { username: string; password: string }
    filePath?: string
  },
): Promise<{ status: number; asset?: LibraryAssetView; bodyText: string }> {
  const token = await apiLogin(request, options.credentials ?? E2E_ADMIN)
  const filePath = options.filePath ?? E2E_ASSET_PNG_PATH
  const groupCode = (options.groupCode ?? DEMO_GROUP_CODE).trim()
  const response = await request.post(`${E2E_API_BASE_URL}/library/assets`, {
    headers: { Authorization: `Bearer ${token}` },
    multipart: {
      groupCode,
      assetKey: options.assetKey,
      assetClass: options.assetClass,
      file: {
        name: E2E_ASSET_PNG_FILENAME,
        mimeType: 'image/png',
        buffer: fs.readFileSync(filePath),
      },
    },
  })
  const bodyText = await response.text()
  if (!response.ok()) {
    return { status: response.status(), bodyText }
  }
  const body = JSON.parse(bodyText) as ApiEnvelope<LibraryAssetView>
  return { status: response.status(), asset: body.result, bodyText }
}

export async function listLibraryAssetsViaApi(
  request: APIRequestContext,
  options: {
    q?: string
    status?: 'ACTIVE' | 'DISABLED' | 'ALL'
    assetClass?: LibraryAssetClass
    groupCode?: string
    credentials?: { username: string; password: string }
  } = {},
): Promise<LibraryAssetView[]> {
  const token = await apiLogin(request, options.credentials ?? E2E_ADMIN)
  const params = new URLSearchParams({ page: '0', size: '50' })
  if (options.q) {
    params.set('q', options.q)
  }
  if (options.status) {
    params.set('status', options.status)
  }
  if (options.assetClass) {
    params.set('assetClass', options.assetClass)
  }
  if (options.groupCode?.trim()) {
    params.set('groupCode', options.groupCode.trim())
  }
  const response = await request.get(`${E2E_API_BASE_URL}/library/assets?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok()) {
    throw new Error(`GET /library/assets failed (${response.status()}): ${await response.text()}`)
  }
  const body = (await response.json()) as ApiEnvelope<PageView<LibraryAssetView>>
  return body.result.content
}

export async function ensureActiveLibraryAsset(
  request: APIRequestContext,
  options: {
    assetKey: string
    assetClass?: LibraryAssetClass
    groupCode?: string
    credentials?: { username: string; password: string }
  },
): Promise<LibraryAssetView> {
  const groupCode = (options.groupCode ?? DEMO_GROUP_CODE).trim()
  const uploaded = await uploadLibraryAssetViaApi(request, {
    assetKey: options.assetKey,
    assetClass: options.assetClass ?? 'IMAGE',
    groupCode,
    credentials: options.credentials,
  })
  if (uploaded.status === 201 && uploaded.asset) {
    return uploaded.asset
  }
  if (uploaded.status === 409) {
    const existing = await listLibraryAssetsViaApi(request, {
      q: options.assetKey,
      status: 'ACTIVE',
      groupCode,
      credentials: options.credentials,
    })
    const hit = existing.find(
      (row) => row.assetKey === options.assetKey && row.groupCode === groupCode,
    )
    if (hit) {
      return hit
    }
  }
  throw new Error(
    `Failed to seed library asset ${groupCode}/${options.assetKey} (${uploaded.status}): ${uploaded.bodyText}`,
  )
}

export async function disableLibraryAssetViaApi(
  request: APIRequestContext,
  assetKey: string,
  options: {
    groupCode?: string
    credentials?: { username: string; password: string }
  } = {},
): Promise<LibraryAssetView> {
  const credentials = options.credentials ?? E2E_ADMIN
  const groupCode = (options.groupCode ?? DEMO_GROUP_CODE).trim()
  const token = await apiLogin(request, credentials)
  const response = await request.post(
    `${E2E_API_BASE_URL}/library/assets/${encodeURIComponent(assetKey)}/disable?groupCode=${encodeURIComponent(groupCode)}`,
    { headers: { Authorization: `Bearer ${token}` } },
  )
  if (!response.ok()) {
    throw new Error(
      `POST /library/assets/${assetKey}/disable?groupCode=${groupCode} failed (${response.status()}): ${await response.text()}`,
    )
  }
  return ((await response.json()) as ApiEnvelope<LibraryAssetView>).result
}

/** Disable every ACTIVE managed asset so Wave 8 honest-empty journeys stay deterministic. */
export async function disableAllActiveLibraryAssetsViaApi(
  request: APIRequestContext,
  credentials: { username: string; password: string } = E2E_ADMIN,
): Promise<number> {
  let disabled = 0
  for (let guard = 0; guard < 40; guard += 1) {
    const rows = await listLibraryAssetsViaApi(request, {
      status: 'ACTIVE',
      credentials,
    })
    if (rows.length === 0) {
      break
    }
    for (const row of rows) {
      await disableLibraryAssetViaApi(request, row.assetKey, {
        groupCode: row.groupCode,
        credentials,
      })
      disabled += 1
    }
  }
  return disabled
}
