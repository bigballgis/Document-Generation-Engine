import type { ApiPolicyImpactPreview } from '@/types/template'

/** Structured hard-block / warning finding (permission-matrix fixed shape). */
export interface ApiPolicyImpactFindingView {
  code: string
  reasonKey: string
  impactKey: string
  adviceKey: string
  expectedErrorCode: string
}

const DEFAULT_ROUTE_NOT_CALLABLE = 'api.apimgmt.policyImpact.defaultRouteNotCallable'

/**
 * Maps impact-preview warning keys into the fixed reason / impact / advice structure
 * required by API policy governance (permission matrix + BDD-CDP-APIPOL-002).
 */
export function buildApiPolicyImpactFindings(
  preview: Pick<ApiPolicyImpactPreview, 'blocking' | 'warnings'>,
): ApiPolicyImpactFindingView[] {
  if (!preview.blocking) {
    return []
  }

  const warnings = preview.warnings ?? []
  if (warnings.includes(DEFAULT_ROUTE_NOT_CALLABLE)) {
    return [
      {
        code: 'DEFAULT_ROUTE_NOT_CALLABLE',
        reasonKey: 'apiPolicy.detail.impact.findings.defaultRouteNotCallable.reason',
        impactKey: 'apiPolicy.detail.impact.findings.defaultRouteNotCallable.impact',
        adviceKey: 'apiPolicy.detail.impact.findings.defaultRouteNotCallable.advice',
        expectedErrorCode: 'DEFAULT_ROUTE_TARGET_UNAVAILABLE',
      },
    ]
  }

  return [
    {
      code: 'GENERIC_HARD_BLOCK',
      reasonKey: 'apiPolicy.detail.impact.findings.generic.reason',
      impactKey: 'apiPolicy.detail.impact.findings.generic.impact',
      adviceKey: 'apiPolicy.detail.impact.findings.generic.advice',
      expectedErrorCode: 'TEMPLATE_VALIDATION_FAILED',
    },
  ]
}

export function hasHardBlockFindings(
  preview: Pick<ApiPolicyImpactPreview, 'blocking' | 'warnings'>,
): boolean {
  return preview.blocking === true && buildApiPolicyImpactFindings(preview).length > 0
}
