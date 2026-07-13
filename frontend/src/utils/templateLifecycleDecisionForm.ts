import { parseAdGroupsConfiguredFromSummary } from '@/utils/apiAccessDiagnostics'

export const TEMPLATE_DECISION_REASON_CATEGORIES = [
  'BINDING_ISSUE',
  'VARIABLE_SCHEMA_ISSUE',
  'RULE_VALIDATION_ISSUE',
  'FIDELITY_WARNING',
  'COVERAGE_BELOW_THRESHOLD',
  'PREVIEW_COMPARISON_DIFF',
  'CONTRACT_SCOPE_CHANGE',
  'OTHER',
] as const

export type TemplateDecisionReasonCategory =
  (typeof TEMPLATE_DECISION_REASON_CATEGORIES)[number]

export interface LifecycleDecisionFormFields {
  reasonCategory: string
  impactSummary: string
  commentSummary?: string
}

export interface LifecyclePositiveDecisionFields {
  fidelityViewedConfirmed?: boolean
  coverageViewedConfirmed?: boolean
  previewViewedConfirmed?: boolean
  keyEvidenceConfirmed?: boolean
  commentSummary?: string
  exceptionIntervention?: boolean
  exceptionReason?: string
  secondaryConfirmed?: boolean
}

export interface LifecycleRejectRemediationFields {
  remediationTestRecordId?: string
  remediationChangeDiffRef?: string
  remediationChecklistCode?: string
}

export type LifecycleDecisionFormField = 'reasonCategory' | 'impactSummary'

export function invalidLifecycleDecisionFields(
  fields: LifecycleDecisionFormFields,
): LifecycleDecisionFormField[] {
  const invalid: LifecycleDecisionFormField[] = []
  if (!fields.reasonCategory.trim()) {
    invalid.push('reasonCategory')
  }
  if (!fields.impactSummary.trim()) {
    invalid.push('impactSummary')
  }
  return invalid
}

export function isLifecycleDecisionFormValid(fields: LifecycleDecisionFormFields): boolean {
  return invalidLifecycleDecisionFields(fields).length === 0
}

export function isTestPassDecisionValid(fields: LifecyclePositiveDecisionFields): boolean {
  if (fields.exceptionIntervention) {
    return (
      Boolean(fields.exceptionReason?.trim()) &&
      Boolean(fields.secondaryConfirmed) &&
      Boolean(fields.fidelityViewedConfirmed) &&
      Boolean(fields.coverageViewedConfirmed) &&
      Boolean(fields.previewViewedConfirmed)
    )
  }
  return (
    Boolean(fields.fidelityViewedConfirmed) &&
    Boolean(fields.coverageViewedConfirmed) &&
    Boolean(fields.previewViewedConfirmed)
  )
}

export function isApprovalPassDecisionValid(fields: LifecyclePositiveDecisionFields): boolean {
  if (!fields.commentSummary?.trim()) {
    return false
  }
  if (fields.exceptionIntervention) {
    return (
      Boolean(fields.fidelityViewedConfirmed) &&
      Boolean(fields.keyEvidenceConfirmed) &&
      Boolean(fields.exceptionReason?.trim()) &&
      Boolean(fields.secondaryConfirmed)
    )
  }
  return Boolean(fields.fidelityViewedConfirmed) && Boolean(fields.keyEvidenceConfirmed)
}

export function hasRejectRemediationLink(fields: LifecycleRejectRemediationFields): boolean {
  return Boolean(
    fields.remediationTestRecordId?.trim() ||
      fields.remediationChangeDiffRef?.trim() ||
      fields.remediationChecklistCode?.trim(),
  )
}

export function isRejectDecisionValid(
  fields: LifecycleDecisionFormFields & LifecycleRejectRemediationFields,
): boolean {
  return isLifecycleDecisionFormValid(fields) && hasRejectRemediationLink(fields)
}

export interface PublishGateDisplayItem {
  key: string
  label: string
  ready: boolean
  informational?: boolean
  blocker?: boolean
  /** Parsed from API_POLICY summary (`adGroupsConfigured=`); null when absent. */
  adGroupsConfigured?: boolean | null
}

export function mapPublishGateChecklistItems(
  items: Array<{ checkCode: string; ready: boolean; blocker: boolean; messageKey: string; summary: string }>,
  resolveLabel: (item: { checkCode: string; messageKey: string; summary: string }) => string,
): PublishGateDisplayItem[] {
  return items.map((item) => ({
    key: item.checkCode,
    label: resolveLabel(item),
    ready: item.ready,
    informational: !item.blocker,
    blocker: item.blocker,
    adGroupsConfigured:
      item.checkCode === 'API_POLICY'
        ? parseAdGroupsConfiguredFromSummary(item.summary)
        : undefined,
  }))
}

export function isPublishGateReady(params: {
  checklistReady: boolean
  releaseVersion: string
  versionConflict: boolean
}): boolean {
  return (
    params.checklistReady &&
    Boolean(params.releaseVersion.trim()) &&
    !params.versionConflict
  )
}

/** Confirm go-live in the publish summary dialog (BDD-CDP-FID-003). */
export function isPublishSummaryConfirmReady(params: {
  hasBlockers: boolean
  fidelityViewedConfirmed: boolean
}): boolean {
  return !params.hasBlockers && Boolean(params.fidelityViewedConfirmed)
}

export function isSubmitGateReady(params: { checklistReady: boolean }): boolean {
  return params.checklistReady
}
