export type LegalHoldScopeType = 'TEMPLATE_WINDOW' | 'INVOCATION_SET'

export type LegalHoldStatus = 'ACTIVE' | 'RELEASED'

export interface LegalHoldView {
  id: string
  holdExternalId: string
  scopeType: LegalHoldScopeType
  status: LegalHoldStatus
  reason: string | null
  templateId: string | null
  templateExternalId: string | null
  effectiveFrom: string | null
  effectiveTo: string | null
  invocationExternalIds: string[]
  invocationCount: number
  createdAt: string
  createdByUsername: string
  /** Optional additive display name (N18); FE falls back to username when absent. */
  createdByDisplayName?: string | null
  releasedAt: string | null
  releasedByUsername: string | null
}

export interface CreateLegalHoldPayload {
  scopeType: LegalHoldScopeType
  reason?: string | null
  templateId?: string | null
  templateExternalId?: string | null
  effectiveFrom?: string | null
  effectiveTo?: string | null
  invocationExternalIds?: string[] | null
}

export type LegalHoldStatusFilter = LegalHoldStatus | ''
