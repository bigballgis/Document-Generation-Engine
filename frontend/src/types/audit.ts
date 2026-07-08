import type { Schema } from '@/types/openapi'

export type AuditReadActorRole = Schema<'AuditReadActorRole'>

/** Optional display fields may arrive before OpenAPI schema refresh. */
export type AuditEventDisplayExtensions = {
  templateDisplayName?: string
  templateExternalId?: string
  actorDisplayName?: string
}

export type ManagementAuditEvent = Schema<'ManagementAuditEvent'> & AuditEventDisplayExtensions

export type LifecycleAuditEvent = Schema<'LifecycleAuditEvent'> &
  AuditEventDisplayExtensions & {
    actorSummary?: string
  }

export type ManagementAuditExportResult = Schema<'ManagementAuditExportResponse'>

export interface LifecycleAuditExportResult {
  format: string
  events: LifecycleAuditEvent[]
}

/** Not yet modeled in `openapi-v1.yaml` (management audit query params). */
export interface AuditQueryFilters {
  actorRole: AuditReadActorRole
  templateId?: string
  eventType?: string
  requestId?: string
  eventAtFrom?: string
  eventAtTo?: string
  groupScope?: string
  page?: number
  size?: number
}

export interface AuditPagedResult<T> {
  events: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
