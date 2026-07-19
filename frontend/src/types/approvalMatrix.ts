/** IBL-E3 / ADR-0064 — package-level approval matrix mode. */
export type ApprovalMatrixMode = 'SINGLE_TRACK' | 'LEGAL_THEN_COMPLIANCE'

/** IBL-E3 / ADR-0064 — multi-stage approval stage. */
export type ApprovalStage = 'LEGAL' | 'COMPLIANCE'

/** Template approval sub-state including IBL-E3 multi-stage values. */
export type ApprovalSubState =
  | 'PENDING_SUBMIT'
  | 'PENDING_DECISION'
  | 'PENDING_LEGAL_DECISION'
  | 'PENDING_COMPLIANCE_DECISION'

export const APPROVAL_MATRIX_MODE_VALUES = [
  'SINGLE_TRACK',
  'LEGAL_THEN_COMPLIANCE',
] as const satisfies readonly ApprovalMatrixMode[]
