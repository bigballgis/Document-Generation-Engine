import { describe, expect, it } from 'vitest'
import { resolveHubLegacyDeepLink } from '@/views/templates/hub/resolveHubLegacyDeepLink'

describe('resolveHubLegacyDeepLink (BDD-SYS-NORM-W2-010…012)', () => {
  it('redirects apiAccess tab to package settings shell', () => {
    expect(
      resolveHubLegacyDeepLink({
        templateId: 'tpl-1',
        query: { tab: 'apiAccess' },
      }),
    ).toEqual({
      kind: 'apiSettings',
      path: '/api/packages/tpl-1/settings',
    })
  })

  it('redirects #apiAccess / #domain hash to package settings', () => {
    expect(
      resolveHubLegacyDeepLink({
        templateId: 'tpl-1',
        query: {},
        hash: '#apiAccess',
      }),
    ).toEqual({
      kind: 'apiSettings',
      path: '/api/packages/tpl-1/settings',
    })

    expect(
      resolveHubLegacyDeepLink({
        templateId: 'tpl-1',
        query: {},
        hash: '#domain=OUTPUT_POLICY',
      }),
    ).toEqual({
      kind: 'apiSettings',
      path: '/api/packages/tpl-1/settings?panel=domain&domain=OUTPUT_POLICY',
    })
  })

  it('opens Properties for overview query without restoring a hub tab', () => {
    expect(
      resolveHubLegacyDeepLink({
        templateId: 'tpl-1',
        query: { tab: 'overview' },
      }),
    ).toEqual({ kind: 'properties' })
  })

  it('routes dependencies to release surface when version context exists', () => {
    expect(
      resolveHubLegacyDeepLink({
        templateId: 'tpl-1',
        query: { tab: 'dependencies' },
        preferredReleaseVersion: '1.2.0',
      }),
    ).toEqual({
      kind: 'dependencies',
      path: '/templates/tpl-1/releases/1.2.0?workspaceTab=dependencies',
      guidance: false,
    })
  })

  it('lands on version lines with guidance when dependencies has no version context', () => {
    expect(
      resolveHubLegacyDeepLink({
        templateId: 'tpl-1',
        query: { tab: 'dependencies' },
      }),
    ).toEqual({ kind: 'dependencies', guidance: true })
  })
})
