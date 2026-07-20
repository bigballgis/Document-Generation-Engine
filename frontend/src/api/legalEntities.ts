import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { PageView } from '@/types/identity'
import type {
  CreateLegalEntityPayload,
  DocumentBrandStatus,
  GroupDefaultLegalEntityView,
  LegalEntityView,
  PutGroupDefaultLegalEntityPayload,
  UpdateLegalEntityPayload,
} from '@/types/documentBrand'
import type { ApiEnvelope } from '@/types/session'

export async function listLegalEntities(
  groupCode: string,
  options: {
    status?: DocumentBrandStatus | ''
    signal?: AbortSignal
  } = {},
): Promise<PageView<LegalEntityView>> {
  const params: Record<string, string> = { groupCode }
  if (options.status) {
    params.status = options.status
  }
  const response = await http.get<ApiEnvelope<PageView<LegalEntityView>>>('/legal-entities', {
    params,
    signal: options.signal,
  })
  return unwrapEnvelope(response.data)
}

export async function getLegalEntity(
  legalEntityCode: string,
  groupCode: string,
  signal?: AbortSignal,
): Promise<LegalEntityView> {
  const response = await http.get<ApiEnvelope<LegalEntityView>>(
    `/legal-entities/${encodeURIComponent(legalEntityCode)}`,
    { params: { groupCode }, signal },
  )
  return unwrapEnvelope(response.data)
}

export async function createLegalEntity(payload: CreateLegalEntityPayload): Promise<LegalEntityView> {
  const response = await http.post<ApiEnvelope<LegalEntityView>>('/legal-entities', payload)
  return unwrapEnvelope(response.data)
}

export async function updateLegalEntity(
  legalEntityCode: string,
  payload: UpdateLegalEntityPayload,
): Promise<LegalEntityView> {
  const response = await http.put<ApiEnvelope<LegalEntityView>>(
    `/legal-entities/${encodeURIComponent(legalEntityCode)}`,
    payload,
  )
  return unwrapEnvelope(response.data)
}

export async function getGroupDefaultLegalEntity(
  groupCode: string,
  signal?: AbortSignal,
): Promise<GroupDefaultLegalEntityView> {
  const response = await http.get<ApiEnvelope<GroupDefaultLegalEntityView>>(
    `/groups/${encodeURIComponent(groupCode)}/default-legal-entity`,
    { signal },
  )
  return unwrapEnvelope(response.data)
}

export async function putGroupDefaultLegalEntity(
  groupCode: string,
  payload: PutGroupDefaultLegalEntityPayload,
): Promise<GroupDefaultLegalEntityView> {
  const response = await http.put<ApiEnvelope<GroupDefaultLegalEntityView>>(
    `/groups/${encodeURIComponent(groupCode)}/default-legal-entity`,
    payload,
  )
  return unwrapEnvelope(response.data)
}
