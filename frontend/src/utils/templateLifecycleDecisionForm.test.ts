import { describe, expect, it } from 'vitest'
import {
  hasRejectRemediationLink,
  invalidLifecycleDecisionFields,
  isApprovalPassDecisionValid,
  isLifecycleDecisionFormValid,
  isPublishGateReady,
  isPublishSummaryConfirmReady,
  isSubmitGateReady,
  isRejectDecisionValid,
  isTestPassDecisionValid,
  mapPublishGateChecklistItems,
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

  it('requires rationale, fidelity viewed, and key evidence for approval pass (BDD-CDP-FID-002)', () => {
    expect(
      isApprovalPassDecisionValid({
        commentSummary: 'Ready',
        keyEvidenceConfirmed: true,
        fidelityViewedConfirmed: true,
      }),
    ).toBe(true)
    expect(
      isApprovalPassDecisionValid({
        commentSummary: 'Ready',
        keyEvidenceConfirmed: true,
        fidelityViewedConfirmed: false,
      }),
    ).toBe(false)
    expect(
      isApprovalPassDecisionValid({
        commentSummary: 'Ready',
        keyEvidenceConfirmed: true,
      }),
    ).toBe(false)
    expect(
      isApprovalPassDecisionValid({
        commentSummary: '',
        keyEvidenceConfirmed: true,
        fidelityViewedConfirmed: true,
      }),
    ).toBe(false)
  })

  it('requires fidelity viewed confirmation before publish summary confirm (BDD-CDP-FID-003)', () => {
    expect(
      isPublishSummaryConfirmReady({
        hasBlockers: false,
        fidelityViewedConfirmed: true,
      }),
    ).toBe(true)
    expect(
      isPublishSummaryConfirmReady({
        hasBlockers: false,
        fidelityViewedConfirmed: false,
      }),
    ).toBe(false)
    expect(
      isPublishSummaryConfirmReady({
        hasBlockers: true,
        fidelityViewedConfirmed: true,
      }),
    ).toBe(false)
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

  it('SCEN-AOD-13: maps API_POLICY summary adGroupsConfigured into display items', () => {
    const items = mapPublishGateChecklistItems(
      [
        {
          checkCode: 'API_POLICY',
          ready: true,
          blocker: false,
          messageKey: 'api.publishGate.apiPolicy.ready',
          summary: 'skeletonPresent=true,adGroupsConfigured=false',
        },
        {
          checkCode: 'ANCHOR_INTEGRITY',
          ready: true,
          blocker: true,
          messageKey: 'api.publishGate.anchorIntegrity.ready',
          summary: '',
        },
      ],
      (item) => item.checkCode,
    )

    expect(items[0]?.adGroupsConfigured).toBe(false)
    expect(items[1]?.adGroupsConfigured).toBeUndefined()
  })
})
