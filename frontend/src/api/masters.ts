import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  CreateMasterPayload,
  DecideMasterReviewPayload,
  MasterDocumentDetail,
  MasterDocumentSummary,
  MasterImpactAnalysis,
  SubmitMasterReviewPayload,
} from '@/types/master'

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function listMasters(): Promise<MasterDocumentSummary[]> {
  const response = await http.get<ApiEnvelope<MasterDocumentSummary[]>>('/masters')
  return unwrap(response.data)
}

export async function getMaster(masterId: string): Promise<MasterDocumentDetail> {
  const response = await http.get<ApiEnvelope<MasterDocumentDetail>>(`/masters/${masterId}`)
  return unwrap(response.data)
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
  return unwrap(response.data)
}

export async function submitMasterReview(
  masterId: string,
  payload: SubmitMasterReviewPayload,
): Promise<MasterDocumentDetail> {
  const response = await http.post<ApiEnvelope<MasterDocumentDetail>>(
    `/masters/${masterId}/submit-review`,
    payload,
  )
  return unwrap(response.data)
}

export async function decideMasterReview(
  masterId: string,
  payload: DecideMasterReviewPayload,
): Promise<MasterDocumentDetail> {
  const response = await http.post<ApiEnvelope<MasterDocumentDetail>>(
    `/masters/${masterId}/review`,
    payload,
  )
  return unwrap(response.data)
}

export async function getMasterImpactAnalysis(masterId: string): Promise<MasterImpactAnalysis> {
  const response = await http.get<ApiEnvelope<MasterImpactAnalysis>>(
    `/masters/${masterId}/impact-analysis`,
  )
  return unwrap(response.data)
}
