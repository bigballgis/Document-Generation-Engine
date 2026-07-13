import { describe, expect, it } from 'vitest'
import {
  buildApiPolicyImpactFindings,
  hasHardBlockFindings,
} from '@/utils/apiPolicyImpactFindings'

describe('buildApiPolicyImpactFindings', () => {
  it('maps DEFAULT_ROUTE_TARGET non-callable hard-block to reason/impact/advice', () => {
    const findings = buildApiPolicyImpactFindings({
      blocking: true,
      warnings: [
        'api.apimgmt.policyImpact.defaultRouteChanged',
        'api.apimgmt.policyImpact.defaultRouteNotCallable',
      ],
    })

    expect(findings).toHaveLength(1)
    expect(findings[0]).toMatchObject({
      code: 'DEFAULT_ROUTE_NOT_CALLABLE',
      reasonKey: 'apiPolicy.detail.impact.findings.defaultRouteNotCallable.reason',
      impactKey: 'apiPolicy.detail.impact.findings.defaultRouteNotCallable.impact',
      adviceKey: 'apiPolicy.detail.impact.findings.defaultRouteNotCallable.advice',
      expectedErrorCode: 'DEFAULT_ROUTE_TARGET_UNAVAILABLE',
    })
    expect(hasHardBlockFindings({
      blocking: true,
      warnings: ['api.apimgmt.policyImpact.defaultRouteNotCallable'],
    })).toBe(true)
  })

  it('returns no findings for non-blocking warning-only previews', () => {
    expect(
      buildApiPolicyImpactFindings({
        blocking: false,
        warnings: ['api.apimgmt.policyImpact.defaultRouteChanged'],
      }),
    ).toEqual([])
    expect(
      hasHardBlockFindings({
        blocking: false,
        warnings: ['api.apimgmt.policyImpact.defaultRouteChanged'],
      }),
    ).toBe(false)
  })
})
