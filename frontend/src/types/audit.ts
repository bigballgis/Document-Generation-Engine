export type AuditReadActorRole = 'GLOBAL_ADMIN' | 'GROUP_ADMIN' | 'AUDIT_ADMIN'

export interface ManagementAuditEvent {
  eventAt: string
  eventType: string
  templateId?: string
  credentialId?: string
  previousPolicyVersion?: number
  policyVersion?: number
  changedAreas: string[]
  rollback: boolean
  rollbackSourcePolicyVersion?: number
  actorSummary?: string
  credentialFingerprint?: string
  statusSummary?: string
  warningCodes: string[]
}

export interface LifecycleAuditEvent {
  eventAt: string
  eventType: string
  templateId?: string
  operation?: string
  fromState?: string
  toState?: string
  actorId?: string
  summary?: string
  warningCodes: string[]
}

export interface ManagementAuditExportResult {
  format: string
  events: Array<
    ManagementAuditEvent & {
      actorSummaryMasked?: string
      credentialFingerprintMasked?: string
    }
  >
}

export interface AuditQueryFilters {
  actorRole: AuditReadActorRole
  templateId?: string
  eventType?: string
  eventAtFrom?: string
  eventAtTo?: string
  groupScope?: string
}
