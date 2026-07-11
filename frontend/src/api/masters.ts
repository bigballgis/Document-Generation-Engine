import type { AxiosProgressEvent } from 'axios'
import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import {
  collectAllPageContent,
  type CollectedCatalogPage,
} from '@/api/catalogPageCollect'
import type { ApiEnvelope } from '@/types/session'
import type { PageView } from '@/types/identity'
import type {
  CreateMasterPayload,
  DecideMasterReviewPayload,
  MasterDocumentDetail,
  MasterDocumentSummary,
  MasterImpactAnalysis,
  MasterRevisionLineDetail,
  MasterRevisionLinePage,
  SubmitMasterReviewPayload,
  UpdateMasterMetadataPayload,
} from '@/types/master'

/** Optional UX callbacks for multipart master DOCX transfers (endpoints/payloads unchanged). */
export type MasterFileUploadOptions = {
  onUploadProgress?: (percent: number | null) => void
}

export type MasterListQueryOptions = {
  signal?: AbortSignal
  search?: string
  groupCode?: string
  status?: string
  sort?: string
}

function normalizeGroupCode(groupCode: string | undefined): string | undefined {
  const trimmed = groupCode?.trim()
  return trimmed ? trimmed.toUpperCase() : undefined
}

function toUploadPercent(event: AxiosProgressEvent): number | null {
  if (!event.total || event.total <= 0) {
    return null
  }
  return Math.min(100, Math.round((event.loaded / event.total) * 100))
}

function attachUploadProgress(
  options: MasterFileUploadOptions | undefined,
): ((event: AxiosProgressEvent) => void) | undefined {
  if (!options?.onUploadProgress) {
    return undefined
  }
  return (event) => {
    options.onUploadProgress!(toUploadPercent(event))
  }
}

export async function listMasters(
  page = 0,
  size = 20,
  options: MasterListQueryOptions = {},
): Promise<PageView<MasterDocumentSummary>> {
  const params: Record<string, string | number> = { page, size }
  if (options.search) {
    params.search = options.search
  }
  const groupCode = normalizeGroupCode(options.groupCode)
  if (groupCode) {
    params.groupCode = groupCode
  }
  if (options.status) {
    params.status = options.status
  }
  if (options.sort) {
    params.sort = options.sort
  }
  const response = await http.get<ApiEnvelope<PageView<MasterDocumentSummary>>>('/masters', {
    params,
    signal: options.signal,
  })
  return unwrapEnvelope(response.data)
}

/** Multi-page merge for dashboard / picker consumers (LR-C5 PageView). */
export async function listAllMasters(
  options: MasterListQueryOptions = {},
): Promise<CollectedCatalogPage<MasterDocumentSummary>> {
  return collectAllPageContent((page, size) => listMasters(page, size, options))
}

export async function getMaster(masterId: string): Promise<MasterDocumentDetail> {
  const response = await http.get<ApiEnvelope<MasterDocumentDetail>>(`/masters/${masterId}`)
  return unwrapEnvelope(response.data)
}

export async function createMaster(
  payload: CreateMasterPayload,
  file: File,
  options?: MasterFileUploadOptions,
): Promise<MasterDocumentDetail> {
  const formData = new FormData()
  formData.append('groupCode', payload.groupCode)
  formData.append('name', payload.name)
  if (payload.description) {
    formData.append('description', payload.description)
  }
  formData.append('file', file)

  const response = await http.post<ApiEnvelope<MasterDocumentDetail>>('/masters', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: attachUploadProgress(options),
  })
  return unwrapEnvelope(response.data)
}

export async function submitMasterReview(
  masterId: string,
  payload: SubmitMasterReviewPayload,
): Promise<MasterDocumentDetail> {
  const response = await http.post<ApiEnvelope<MasterDocumentDetail>>(
    `/masters/${masterId}/submit-review`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function decideMasterReview(
  masterId: string,
  payload: DecideMasterReviewPayload,
): Promise<MasterDocumentDetail> {
  const response = await http.post<ApiEnvelope<MasterDocumentDetail>>(
    `/masters/${masterId}/review`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function getMasterImpactAnalysis(masterId: string): Promise<MasterImpactAnalysis> {
  const response = await http.get<ApiEnvelope<MasterImpactAnalysis>>(
    `/masters/${masterId}/impact-analysis`,
  )
  return unwrapEnvelope(response.data)
}

export async function updateMasterMetadata(
  masterId: string,
  payload: UpdateMasterMetadataPayload,
): Promise<MasterDocumentDetail> {
  const response = await http.patch<ApiEnvelope<MasterDocumentDetail>>(
    `/masters/${masterId}`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function downloadMasterFile(masterId: string): Promise<{ blob: Blob; filename: string }> {
  const response = await http.get<Blob>(`/masters/${masterId}/download`, {
    responseType: 'blob',
  })
  const disposition = response.headers['content-disposition'] ?? ''
  const filenameMatch = /filename="([^"]+)"/i.exec(disposition)
  const filename = filenameMatch?.[1] ?? 'master.docx'
  return { blob: response.data, filename }
}

export async function listMasterRevisionLines(
  masterId: string,
  page = 0,
  size = 20,
): Promise<MasterRevisionLinePage> {
  const response = await http.get<ApiEnvelope<MasterRevisionLinePage>>(
    `/masters/${masterId}/revision-lines`,
    { params: { page, size } },
  )
  return unwrapEnvelope(response.data)
}

export async function getMasterRevisionLine(
  masterId: string,
  revisionLineId: string,
): Promise<MasterRevisionLineDetail> {
  const response = await http.get<ApiEnvelope<MasterRevisionLineDetail>>(
    `/masters/${masterId}/revision-lines/${revisionLineId}`,
  )
  return unwrapEnvelope(response.data)
}

export async function downloadMasterRevisionLineFile(
  masterId: string,
  revisionLineId: string,
): Promise<{ blob: Blob; filename: string }> {
  const response = await http.get<Blob>(
    `/masters/${masterId}/revision-lines/${revisionLineId}/download`,
    { responseType: 'blob' },
  )
  const disposition = response.headers['content-disposition'] ?? ''
  const filenameMatch = /filename="([^"]+)"/i.exec(disposition)
  const filename = filenameMatch?.[1] ?? 'master.docx'
  return { blob: response.data, filename }
}

export async function replaceMasterFile(
  masterId: string,
  file: File,
  options?: MasterFileUploadOptions,
): Promise<MasterDocumentDetail> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await http.put<ApiEnvelope<MasterDocumentDetail>>(
    `/masters/${masterId}/file`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: attachUploadProgress(options),
    },
  )
  return unwrapEnvelope(response.data)
}
