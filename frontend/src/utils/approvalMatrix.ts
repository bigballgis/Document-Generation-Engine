import type {
  ApprovalMatrixMode,
  ApprovalStage,
  ApprovalSubState,
} from '@/types/approvalMatrix'
import type { TemplateLifecycleStatus } from '@/types/template'

export function normalizeApprovalMatrixMode(
  mode: ApprovalMatrixMode | null | undefined,
): ApprovalMatrixMode {
  return mode === 'LEGAL_THEN_COMPLIANCE' ? 'LEGAL_THEN_COMPLIANCE' : 'SINGLE_TRACK'
}

/** E3-C4 — mode writable only in DRAFT or APPROVAL+PENDING_SUBMIT. */
export function isApprovalMatrixModeWritable(options: {
  lifecycleStatus?: TemplateLifecycleStatus | null
  approvalSubState?: ApprovalSubState | null
}): boolean {
  const status = options.lifecycleStatus
  if (status === 'DRAFT') {
    return true
  }
  return status === 'APPROVAL' && (options.approvalSubState ?? 'PENDING_SUBMIT') === 'PENDING_SUBMIT'
}

export function deriveApprovalStage(
  approvalSubState?: ApprovalSubState | null,
): ApprovalStage | null {
  if (approvalSubState === 'PENDING_LEGAL_DECISION') {
    return 'LEGAL'
  }
  if (approvalSubState === 'PENDING_COMPLIANCE_DECISION') {
    return 'COMPLIANCE'
  }
  return null
}

export function isPendingLegalDecision(
  approvalSubState?: ApprovalSubState | null,
): boolean {
  return approvalSubState === 'PENDING_LEGAL_DECISION'
}

export function isPendingComplianceDecision(
  approvalSubState?: ApprovalSubState | null,
): boolean {
  return approvalSubState === 'PENDING_COMPLIANCE_DECISION'
}

/** Single-track or compliance-stage awaiting decideApprovals. */
export function isPendingApproverDecision(
  approvalSubState?: ApprovalSubState | null,
): boolean {
  return (
    approvalSubState === 'PENDING_DECISION' ||
    approvalSubState === 'PENDING_COMPLIANCE_DECISION'
  )
}

/** Any in-flight approval decision sub-state (not ready-to-submit). */
export function isAwaitingAnyApprovalDecision(
  approvalSubState?: ApprovalSubState | null,
): boolean {
  return (
    approvalSubState === 'PENDING_DECISION' ||
    approvalSubState === 'PENDING_LEGAL_DECISION' ||
    approvalSubState === 'PENDING_COMPLIANCE_DECISION'
  )
}

export function canShowApprovalDecisionActions(options: {
  lifecycleStatus?: TemplateLifecycleStatus | null
  approvalSubState?: ApprovalSubState | null
  decideApprovals: boolean
  decideLegalApprovals: boolean
}): boolean {
  if (options.lifecycleStatus !== 'APPROVAL') {
    return false
  }
  if (options.approvalSubState === 'PENDING_SUBMIT' || !options.approvalSubState) {
    return false
  }
  if (isPendingLegalDecision(options.approvalSubState)) {
    return options.decideLegalApprovals
  }
  if (isPendingApproverDecision(options.approvalSubState)) {
    return options.decideApprovals
  }
  return false
}

export function approvalMatrixModeLabelKey(mode: ApprovalMatrixMode): string {
  return `templates.approvalMatrix.mode.${mode}`
}

export function approvalStageLabelKey(stage: ApprovalStage): string {
  return `templates.approvalMatrix.stage.${stage}`
}
