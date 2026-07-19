import { describe, expect, it } from 'vitest'
import {
  canShowApprovalDecisionActions,
  deriveApprovalStage,
  isApprovalMatrixModeWritable,
  isAwaitingAnyApprovalDecision,
  isPendingApproverDecision,
  normalizeApprovalMatrixMode,
} from '@/utils/approvalMatrix'

describe('approvalMatrix (BDD-IBL-E3)', () => {
  it('defaults missing mode to SINGLE_TRACK', () => {
    expect(normalizeApprovalMatrixMode(undefined)).toBe('SINGLE_TRACK')
    expect(normalizeApprovalMatrixMode(null)).toBe('SINGLE_TRACK')
    expect(normalizeApprovalMatrixMode('LEGAL_THEN_COMPLIANCE')).toBe('LEGAL_THEN_COMPLIANCE')
  })

  it('E3-C4: mode writable only in DRAFT or APPROVAL+PENDING_SUBMIT', () => {
    expect(isApprovalMatrixModeWritable({ lifecycleStatus: 'DRAFT' })).toBe(true)
    expect(
      isApprovalMatrixModeWritable({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_SUBMIT',
      }),
    ).toBe(true)
    expect(
      isApprovalMatrixModeWritable({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_LEGAL_DECISION',
      }),
    ).toBe(false)
    expect(
      isApprovalMatrixModeWritable({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
      }),
    ).toBe(false)
    expect(isApprovalMatrixModeWritable({ lifecycleStatus: 'PUBLISHED' })).toBe(false)
  })

  it('derives approvalStage from multi-stage sub-states', () => {
    expect(deriveApprovalStage('PENDING_LEGAL_DECISION')).toBe('LEGAL')
    expect(deriveApprovalStage('PENDING_COMPLIANCE_DECISION')).toBe('COMPLIANCE')
    expect(deriveApprovalStage('PENDING_DECISION')).toBeNull()
    expect(deriveApprovalStage('PENDING_SUBMIT')).toBeNull()
  })

  it('E3-C15/C17: decision actions are role- and stage-aware', () => {
    expect(
      canShowApprovalDecisionActions({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_LEGAL_DECISION',
        decideApprovals: true,
        decideLegalApprovals: false,
      }),
    ).toBe(false)
    expect(
      canShowApprovalDecisionActions({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_LEGAL_DECISION',
        decideApprovals: false,
        decideLegalApprovals: true,
      }),
    ).toBe(true)
    expect(
      canShowApprovalDecisionActions({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_COMPLIANCE_DECISION',
        decideApprovals: true,
        decideLegalApprovals: false,
      }),
    ).toBe(true)
    expect(
      canShowApprovalDecisionActions({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_COMPLIANCE_DECISION',
        decideApprovals: false,
        decideLegalApprovals: true,
      }),
    ).toBe(false)
    expect(
      canShowApprovalDecisionActions({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
        decideApprovals: true,
        decideLegalApprovals: false,
      }),
    ).toBe(true)
  })

  it('classifies pending decision sub-states', () => {
    expect(isPendingApproverDecision('PENDING_DECISION')).toBe(true)
    expect(isPendingApproverDecision('PENDING_COMPLIANCE_DECISION')).toBe(true)
    expect(isPendingApproverDecision('PENDING_LEGAL_DECISION')).toBe(false)
    expect(isAwaitingAnyApprovalDecision('PENDING_LEGAL_DECISION')).toBe(true)
  })
})
