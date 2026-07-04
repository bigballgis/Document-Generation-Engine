import type { Schema } from '@/types/openapi'

export type AuditReadActorRole = Schema<'AuditReadActorRole'>

export type ManagementAuditEvent = Schema<'ManagementAuditEvent'>

export type LifecycleAuditEvent = Schema<'LifecycleAuditEvent'>

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
