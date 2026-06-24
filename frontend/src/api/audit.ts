import { http } from '@/api/http'
import type {
  AuditPagedResult,
  AuditQueryFilters,
  LifecycleAuditEvent,
  ManagementAuditEvent,
  LifecycleAuditExportResult,
  ManagementAuditExportResult,
} from '@/types/audit'
import type { ApiEnvelope } from '@/types/session'

const DEFAULT_PAGE_SIZE = 20

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

function buildAuditParams(filters: AuditQueryFilters): Record<string, string | number> {
  const params: Record<string, string | number> = {
    actorRole: filters.actorRole,
    page: filters.page ?? 0,
    size: filters.size ?? DEFAULT_PAGE_SIZE,
  }
  if (filters.templateId) {
    params.templateId = filters.templateId
  }
  if (filters.eventType) {
    params.eventType = filters.eventType
  }
  if (filters.eventAtFrom) {
    params.eventAtFrom = filters.eventAtFrom
  }
  if (filters.eventAtTo) {
    params.eventAtTo = filters.eventAtTo
  }
  if (filters.groupScope) {
    params.groupScope = filters.groupScope
  }
  return params
}

export async function listManagementEvents(
  filters: AuditQueryFilters,
): Promise<AuditPagedResult<ManagementAuditEvent>> {
  const response = await http.get<ApiEnvelope<AuditPagedResult<ManagementAuditEvent>>>(
    '/admin/audit/management-events',
    { params: buildAuditParams(filters) },
  )
  return unwrap(response.data)
}

export async function exportManagementEvents(
  filters: AuditQueryFilters,
): Promise<ManagementAuditExportResult> {
  const response = await http.get<ApiEnvelope<ManagementAuditExportResult>>(
    '/admin/audit/management-events/export',
    { params: buildExportParams(filters) },
  )
  return unwrap(response.data)
}

export async function listLifecycleEvents(
  filters: AuditQueryFilters,
): Promise<AuditPagedResult<LifecycleAuditEvent>> {
  const response = await http.get<ApiEnvelope<AuditPagedResult<LifecycleAuditEvent>>>(
    '/admin/audit/lifecycle-events',
    { params: buildAuditParams(filters) },
  )
  return unwrap(response.data)
}

export async function exportLifecycleEvents(
  filters: AuditQueryFilters,
): Promise<LifecycleAuditExportResult> {
  const response = await http.get<ApiEnvelope<LifecycleAuditExportResult>>(
    '/admin/audit/lifecycle-events/export',
    { params: buildExportParams(filters) },
  )
  return unwrap(response.data)
}

function buildExportParams(filters: AuditQueryFilters): Record<string, string> {
  const params: Record<string, string> = {
    actorRole: filters.actorRole,
  }
  if (filters.templateId) {
    params.templateId = filters.templateId
  }
  if (filters.eventType) {
    params.eventType = filters.eventType
  }
  if (filters.eventAtFrom) {
    params.eventAtFrom = filters.eventAtFrom
  }
  if (filters.eventAtTo) {
    params.eventAtTo = filters.eventAtTo
  }
  if (filters.groupScope) {
    params.groupScope = filters.groupScope
  }
  return params
}
