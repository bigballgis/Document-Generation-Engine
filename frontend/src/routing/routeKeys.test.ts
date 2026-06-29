import { describe, expect, it } from 'vitest'
import { apiPolicyDetailPath, pathForRouteKey, ROUTE_KEYS, templateDetailPath } from '@/routing/routeKeys'

describe('routeKeys', () => {
  it('maps product logical routes to frontend paths', () => {
    expect(pathForRouteKey(ROUTE_KEYS.dashboardHome)).toBe('/dashboard')
    expect(pathForRouteKey(ROUTE_KEYS.globalGovernanceHome)).toBe('/dashboard')
    expect(pathForRouteKey(ROUTE_KEYS.masterManagement)).toBe('/masters')
    expect(pathForRouteKey(ROUTE_KEYS.templateManagement)).toBe('/templates')
    expect(pathForRouteKey(ROUTE_KEYS.auditConsole)).toBe('/audit')
    expect(pathForRouteKey(ROUTE_KEYS.apiPolicyManagement)).toBe('/api/policies')
    expect(apiPolicyDetailPath('tpl-1', 'OUTPUT_POLICY')).toBe(
      '/api/policies/tpl-1?domain=OUTPUT_POLICY',
    )
    expect(pathForRouteKey(ROUTE_KEYS.identityAdministration)).toBe('/entitlement/users')
    expect(pathForRouteKey(ROUTE_KEYS.testerWorkbench)).toBe('/workbench/tester')
    expect(pathForRouteKey(ROUTE_KEYS.approverWorkbench)).toBe('/workbench/approver')
    expect(pathForRouteKey(ROUTE_KEYS.escalationWorkbench)).toBe('/workbench/escalation')
  })

  it('builds template detail paths', () => {
    expect(templateDetailPath('tpl-1')).toBe('/templates/tpl-1')
    expect(templateDetailPath('tpl-1', 'overview')).toBe('/templates/tpl-1?tab=overview')
  })

  it('falls back to forbidden for unknown route keys', () => {
    expect(pathForRouteKey('route.unknown')).toBe('/forbidden')
  })
})
