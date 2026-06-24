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
