import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
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

export async function listMasters(): Promise<MasterDocumentSummary[]> {
  const response = await http.get<ApiEnvelope<MasterDocumentSummary[]>>('/masters')
  return unwrapEnvelope(response.data)
}

export async function getMaster(masterId: string): Promise<MasterDocumentDetail> {
  const response = await http.get<ApiEnvelope<MasterDocumentDetail>>(`/masters/${masterId}`)
  return unwrapEnvelope(response.data)
}

export async function createMaster(
  payload: CreateMasterPayload,
  file: File,
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

export async function replaceMasterFile(masterId: string, file: File): Promise<MasterDocumentDetail> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await http.put<ApiEnvelope<MasterDocumentDetail>>(
    `/masters/${masterId}/file`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
  return unwrapEnvelope(response.data)
}
