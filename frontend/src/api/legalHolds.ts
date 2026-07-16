import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { PageView } from '@/types/identity'
import type {
  CreateLegalHoldPayload,
  LegalHoldStatus,
  LegalHoldView,
} from '@/types/legalHold'
import type { ApiEnvelope } from '@/types/session'

export async function listLegalHolds(
  page = 0,
  size = 20,
  options: {
    status?: LegalHoldStatus
    signal?: AbortSignal
  } = {},
): Promise<PageView<LegalHoldView>> {
  const params: Record<string, string | number> = { page, size }
  if (options.status) {
    params.status = options.status
  }
  const response = await http.get<ApiEnvelope<PageView<LegalHoldView>>>('/legal-holds', {
    params,
    signal: options.signal,
  })
  return unwrapEnvelope(response.data)
}

export async function getLegalHold(id: string): Promise<LegalHoldView> {
  const response = await http.get<ApiEnvelope<LegalHoldView>>(
    `/legal-holds/${encodeURIComponent(id)}`,
  )
  return unwrapEnvelope(response.data)
}

export async function createLegalHold(payload: CreateLegalHoldPayload): Promise<LegalHoldView> {
  const response = await http.post<ApiEnvelope<LegalHoldView>>('/legal-holds', payload)
  return unwrapEnvelope(response.data)
}

export async function releaseLegalHold(id: string): Promise<LegalHoldView> {
  const response = await http.post<ApiEnvelope<LegalHoldView>>(
    `/legal-holds/${encodeURIComponent(id)}/release`,
  )
  return unwrapEnvelope(response.data)
}
