export type LifecycleDecisionDialogMode =
  | 'test-fail'
  | 'test-pass'
  | 'approval-reject'
  | 'approval-approve'

export interface LifecycleDecisionSubmitPayload {
  reasonCategory?: string
  impactSummary?: string
  commentSummary?: string
  fidelityViewedConfirmed?: boolean
  coverageViewedConfirmed?: boolean
  previewViewedConfirmed?: boolean
  keyEvidenceConfirmed?: boolean
  remediationTestRecordId?: string
  remediationChangeDiffRef?: string
  remediationChecklistCode?: string
  exceptionIntervention?: boolean
  exceptionReason?: string
  secondaryConfirmed?: boolean
}

export type LifecycleDecisionDialogProps = {
  modelValue: boolean
  mode: LifecycleDecisionDialogMode
  templateId?: string
  loading?: boolean
  initialComment?: string
}

export type LifecycleDecisionDialogEmit = {
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: LifecycleDecisionSubmitPayload): void
}

export type LifecycleDecisionFormState = {
  reasonCategory: string
  impactSummary: string
  commentSummary: string
  fidelityViewedConfirmed: boolean
  coverageViewedConfirmed: boolean
  previewViewedConfirmed: boolean
  keyEvidenceConfirmed: boolean
  remediationTestRecordId: string
  remediationChangeDiffRef: string
  remediationChecklistCode: string
  exceptionIntervention: boolean
  exceptionReason: string
  secondaryConfirmed: boolean
}

export function buildLifecycleDecisionSubmitPayload(
  mode: LifecycleDecisionDialogMode,
  form: LifecycleDecisionFormState,
): LifecycleDecisionSubmitPayload {
  if (mode === 'test-pass') {
    return {
      fidelityViewedConfirmed: form.fidelityViewedConfirmed,
      coverageViewedConfirmed: form.coverageViewedConfirmed,
      previewViewedConfirmed: form.previewViewedConfirmed,
      commentSummary: form.commentSummary.trim() || undefined,
      exceptionIntervention: form.exceptionIntervention || undefined,
      exceptionReason: form.exceptionIntervention ? form.exceptionReason.trim() : undefined,
      secondaryConfirmed: form.exceptionIntervention ? form.secondaryConfirmed : undefined,
    }
  }
  if (mode === 'approval-approve') {
    return {
      commentSummary: form.commentSummary.trim(),
      fidelityViewedConfirmed: form.fidelityViewedConfirmed,
      keyEvidenceConfirmed: form.keyEvidenceConfirmed,
      exceptionIntervention: form.exceptionIntervention || undefined,
      exceptionReason: form.exceptionIntervention ? form.exceptionReason.trim() : undefined,
      secondaryConfirmed: form.exceptionIntervention ? form.secondaryConfirmed : undefined,
    }
  }
  return {
    reasonCategory: form.reasonCategory.trim(),
    impactSummary: form.impactSummary.trim(),
    commentSummary: form.commentSummary.trim() || undefined,
    remediationTestRecordId: form.remediationTestRecordId.trim() || undefined,
    remediationChangeDiffRef: form.remediationChangeDiffRef.trim() || undefined,
    remediationChecklistCode: form.remediationChecklistCode.trim() || undefined,
  }
}

export function resetLifecycleDecisionForm(
  form: LifecycleDecisionFormState,
  initialComment?: string,
) {
  form.reasonCategory = ''
  form.impactSummary = ''
  form.commentSummary = initialComment ?? ''
  form.fidelityViewedConfirmed = false
  form.coverageViewedConfirmed = false
  form.previewViewedConfirmed = false
  form.keyEvidenceConfirmed = false
  form.remediationTestRecordId = ''
  form.remediationChangeDiffRef = ''
  form.remediationChecklistCode = ''
  form.exceptionIntervention = false
  form.exceptionReason = ''
  form.secondaryConfirmed = false
}
