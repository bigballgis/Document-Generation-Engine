import { describe, expect, it } from 'vitest'
import {
  apiPolicyDetailPath,
  pathForRouteKey,
  ROUTE_KEYS,
  templateDetailPath,
  templateDevVersionPath,
  templateLifecyclePanelPath,
  templatePackageHubPath,
  templateReleaseDetailPath,
} from '@/routing/routeKeys'

describe('routeKeys', () => {
  it('maps product logical routes to frontend paths', () => {
    expect(pathForRouteKey(ROUTE_KEYS.dashboardHome)).toBe('/dashboard')
    expect(pathForRouteKey(ROUTE_KEYS.globalGovernanceHome)).toBe('/dashboard')
    expect(pathForRouteKey(ROUTE_KEYS.masterManagement)).toBe('/masters')
    expect(pathForRouteKey(ROUTE_KEYS.templateManagement)).toBe('/templates')
    expect(pathForRouteKey(ROUTE_KEYS.auditConsole)).toBe('/audit')
    expect(pathForRouteKey(ROUTE_KEYS.apiPolicyManagement)).toBe('/api/policies')
    expect(apiPolicyDetailPath('tpl-1', 'OUTPUT_POLICY')).toBe(
      '/templates/tpl-1?tab=apiAccess#domain=OUTPUT_POLICY',
    )
    expect(apiPolicyDetailPath('tpl-1')).toBe('/templates/tpl-1?tab=apiAccess')
    expect(pathForRouteKey(ROUTE_KEYS.identityAdministration)).toBe('/entitlement/users')
  })

  it('redirects legacy workbench route keys to dashboard tasks section', () => {
    expect(pathForRouteKey('route.tester-workbench')).toBe('/dashboard#tasks-section')
    expect(pathForRouteKey('route.approver-workbench')).toBe('/dashboard#tasks-section')
    expect(pathForRouteKey('route.escalation-workbench')).toBe('/dashboard#tasks-section')
  })

  it('builds template package navigation paths', () => {
    expect(templatePackageHubPath('tpl-1')).toBe('/templates/tpl-1')
    expect(templatePackageHubPath('tpl-1', 'overview')).toBe('/templates/tpl-1?tab=overview')
    expect(templateDetailPath('tpl-1')).toBe('/templates/tpl-1')
    expect(templateDevVersionPath('tpl-1', 'dev-2')).toBe('/templates/tpl-1/dev/dev-2')
    expect(templateDevVersionPath('tpl-1', 'dev-2', 'lifecycle')).toBe(
      '/templates/tpl-1/dev/dev-2?tab=lifecycle',
    )
    expect(templateReleaseDetailPath('tpl-1', '1.0.0')).toBe('/templates/tpl-1/releases/1.0.0')
    expect(templateLifecyclePanelPath('tpl-1')).toBe('/templates/tpl-1?tab=lifecycle')
  })

  it('falls back to forbidden for unknown route keys', () => {
    expect(pathForRouteKey('route.unknown')).toBe('/forbidden')
  })
})
