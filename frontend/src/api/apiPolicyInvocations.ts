import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { PageView } from '@/types/identity'
import type {
  ManagementInvocationDetail,
  ManagementInvocationFilters,
  ManagementInvocationSummary,
} from '@/types/template'

function buildInvocationListParams(
  page: number,
  size: number,
  filters: ManagementInvocationFilters = {},
): Record<string, string | number> {
  const params: Record<string, string | number> = { page, size }
  if (filters.status?.trim()) {
    params.status = filters.status.trim()
  }
  if (filters.invocationKind?.trim()) {
    params.invocationKind = filters.invocationKind.trim()
  }
  if (filters.requestId?.trim()) {
    params.requestId = filters.requestId.trim()
  }
  if (filters.createdAfter?.trim()) {
    params.createdAfter = filters.createdAfter.trim()
  }
  if (filters.createdBefore?.trim()) {
    params.createdBefore = filters.createdBefore.trim()
  }
  if (filters.credentialId?.trim()) {
    params.credentialId = filters.credentialId.trim()
  }
  if (filters.resolvedReleaseVersion?.trim()) {
    params.resolvedReleaseVersion = filters.resolvedReleaseVersion.trim()
  }
  return params
}

function buildInvocationFilterParams(
  filters: ManagementInvocationFilters = {},
): Record<string, string> {
  const params: Record<string, string> = {}
  if (filters.status?.trim()) {
    params.status = filters.status.trim()
  }
  if (filters.invocationKind?.trim()) {
    params.invocationKind = filters.invocationKind.trim()
  }
  if (filters.requestId?.trim()) {
    params.requestId = filters.requestId.trim()
  }
  if (filters.createdAfter?.trim()) {
    params.createdAfter = filters.createdAfter.trim()
  }
  if (filters.createdBefore?.trim()) {
    params.createdBefore = filters.createdBefore.trim()
  }
  if (filters.credentialId?.trim()) {
    params.credentialId = filters.credentialId.trim()
  }
  if (filters.resolvedReleaseVersion?.trim()) {
    params.resolvedReleaseVersion = filters.resolvedReleaseVersion.trim()
  }
  return params
}

export async function listInvocations(
  templateId: string,
  page: number,
  size: number,
  filters: ManagementInvocationFilters = {},
): Promise<PageView<ManagementInvocationSummary>> {
  const response = await http.get<ApiEnvelope<PageView<ManagementInvocationSummary>>>(
    `/templates/${templateId}/api/invocations`,
    { params: buildInvocationListParams(page, size, filters) },
  )
  return unwrapEnvelope(response.data)
}

export async function getInvocationDetail(
  templateId: string,
  invocationId: string,
): Promise<ManagementInvocationDetail> {
  const response = await http.get<ApiEnvelope<ManagementInvocationDetail>>(
    `/templates/${templateId}/api/invocations/${invocationId}`,
  )
  return unwrapEnvelope(response.data)
}

export async function exportInvocationsCsv(
  templateId: string,
  filters: ManagementInvocationFilters = {},
): Promise<{ blob: Blob; filename: string; truncated: boolean }> {
  const response = await http.get<Blob>(`/templates/${templateId}/api/invocations/export`, {
    params: buildInvocationFilterParams(filters),
    responseType: 'blob',
  })
  const disposition = response.headers['content-disposition'] ?? ''
  const filenameMatch = /filename="([^"]+)"/i.exec(disposition)
  const filename = filenameMatch?.[1] ?? `invocations-${templateId}.csv`
  const truncatedHeader = response.headers['x-export-truncated']
  const truncated =
    typeof truncatedHeader === 'string'
      ? truncatedHeader.toLowerCase() === 'true'
      : Boolean(truncatedHeader)
  return { blob: response.data, filename, truncated }
}
