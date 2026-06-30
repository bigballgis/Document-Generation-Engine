import { describe, expect, it } from 'vitest'
import {
  hasRejectRemediationLink,
  invalidLifecycleDecisionFields,
  isApprovalPassDecisionValid,
  isLifecycleDecisionFormValid,
  isPublishGateReady,
  isSubmitGateReady,
  isRejectDecisionValid,
  isTestPassDecisionValid,
} from '@/utils/templateLifecycleDecisionForm'

describe('templateLifecycleDecisionForm', () => {
  it('requires reason category and impact summary for negative decisions', () => {
    expect(isLifecycleDecisionFormValid({ reasonCategory: '', impactSummary: '' })).toBe(false)
    expect(invalidLifecycleDecisionFields({ reasonCategory: '', impactSummary: '' })).toEqual([
      'reasonCategory',
      'impactSummary',
    ])
  })

  it('accepts trimmed structured opinion fields', () => {
    expect(
      isLifecycleDecisionFormValid({
        reasonCategory: 'BINDING_ISSUE',
        impactSummary: 'Header binding invalid',
        commentSummary: 'Optional note',
      }),
    ).toBe(true)
  })

  it('rejects whitespace-only values', () => {
    expect(
      isLifecycleDecisionFormValid({
        reasonCategory: '   ',
        impactSummary: '   ',
      }),
    ).toBe(false)
  })

  it('requires all pass confirmations for test pass decisions', () => {
    expect(isTestPassDecisionValid({})).toBe(false)
    expect(
      isTestPassDecisionValid({
        fidelityViewedConfirmed: true,
        coverageViewedConfirmed: true,
        previewViewedConfirmed: true,
      }),
    ).toBe(true)
  })

  it('requires rationale and key evidence for approval pass', () => {
    expect(isApprovalPassDecisionValid({ commentSummary: 'Ready', keyEvidenceConfirmed: true })).toBe(
      true,
    )
    expect(isApprovalPassDecisionValid({ commentSummary: '', keyEvidenceConfirmed: true })).toBe(
      false,
    )
  })

  it('requires remediation link for reject decisions', () => {
    expect(
      isRejectDecisionValid({
        reasonCategory: 'BINDING_ISSUE',
        impactSummary: 'Broken header binding',
      }),
    ).toBe(false)
    expect(
      hasRejectRemediationLink({
        remediationChecklistCode: 'ANCHOR_INTEGRITY',
      }),
    ).toBe(true)
    expect(
      isRejectDecisionValid({
        reasonCategory: 'BINDING_ISSUE',
        impactSummary: 'Broken header binding',
        remediationChecklistCode: 'ANCHOR_INTEGRITY',
      }),
    ).toBe(true)
  })

  it('blocks publish when checklist is not ready, version is empty, or version conflicts', () => {
    expect(
      isPublishGateReady({
        checklistReady: true,
        releaseVersion: '1.0.1',
        versionConflict: false,
      }),
    ).toBe(true)
    expect(
      isPublishGateReady({
        checklistReady: false,
        releaseVersion: '1.0.1',
        versionConflict: false,
      }),
    ).toBe(false)
    expect(
      isPublishGateReady({
        checklistReady: true,
        releaseVersion: '   ',
        versionConflict: false,
      }),
    ).toBe(false)
    expect(
      isPublishGateReady({
        checklistReady: true,
        releaseVersion: '1.0.1',
        versionConflict: true,
      }),
    ).toBe(false)
  })

  it('blocks submit-for-approval when submit-phase checklist is not ready', () => {
    expect(isSubmitGateReady({ checklistReady: true })).toBe(true)
    expect(isSubmitGateReady({ checklistReady: false })).toBe(false)
  })
})
