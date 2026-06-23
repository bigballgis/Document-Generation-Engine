import { describe, expect, it } from 'vitest'
import { pathForRouteKey, ROUTE_KEYS, templateDetailPath } from '@/routing/routeKeys'

describe('routeKeys', () => {
  it('maps product logical routes to frontend paths', () => {
    expect(pathForRouteKey(ROUTE_KEYS.globalGovernanceHome)).toBe('/home/global-governance')
    expect(pathForRouteKey(ROUTE_KEYS.templateAuthoringHome)).toBe('/home/template-authoring')
    expect(pathForRouteKey(ROUTE_KEYS.masterManagement)).toBe('/masters')
    expect(pathForRouteKey(ROUTE_KEYS.templateManagement)).toBe('/templates')
    expect(pathForRouteKey(ROUTE_KEYS.auditConsole)).toBe('/home/audit')
  })

  it('builds template detail paths', () => {
    expect(templateDetailPath('tpl-1')).toBe('/templates/tpl-1')
  })

  it('falls back to forbidden for unknown route keys', () => {
    expect(pathForRouteKey('route.unknown')).toBe('/forbidden')
  })
})
