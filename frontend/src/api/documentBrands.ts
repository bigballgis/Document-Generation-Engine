import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { PageView } from '@/types/identity'
import type {
  CreateDocumentBrandPayload,
  DocumentBrandStatus,
  DocumentBrandView,
  UpdateDocumentBrandPayload,
} from '@/types/documentBrand'
import type { ApiEnvelope } from '@/types/session'

export async function listDocumentBrands(
  groupCode: string,
  options: {
    status?: DocumentBrandStatus | ''
    signal?: AbortSignal
  } = {},
): Promise<PageView<DocumentBrandView>> {
  const params: Record<string, string> = { groupCode }
  if (options.status) {
    params.status = options.status
  }
  const response = await http.get<ApiEnvelope<PageView<DocumentBrandView>>>('/document-brands', {
    params,
    signal: options.signal,
  })
  return unwrapEnvelope(response.data)
}

export async function getDocumentBrand(
  documentBrandCode: string,
  groupCode: string,
  signal?: AbortSignal,
): Promise<DocumentBrandView> {
  const response = await http.get<ApiEnvelope<DocumentBrandView>>(
    `/document-brands/${encodeURIComponent(documentBrandCode)}`,
    { params: { groupCode }, signal },
  )
  return unwrapEnvelope(response.data)
}

export async function createDocumentBrand(
  payload: CreateDocumentBrandPayload,
): Promise<DocumentBrandView> {
  const response = await http.post<ApiEnvelope<DocumentBrandView>>('/document-brands', payload)
  return unwrapEnvelope(response.data)
}

export async function updateDocumentBrand(
  documentBrandCode: string,
  payload: UpdateDocumentBrandPayload,
): Promise<DocumentBrandView> {
  const response = await http.put<ApiEnvelope<DocumentBrandView>>(
    `/document-brands/${encodeURIComponent(documentBrandCode)}`,
    payload,
  )
  return unwrapEnvelope(response.data)
}
