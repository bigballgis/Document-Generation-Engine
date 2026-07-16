import { describe, expect, it } from 'vitest'
import { canAccessRouteWithCapability } from '@/auth/routeCapabilities'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import type { ManagementSession } from '@/types/session'

function session(partial: Partial<ManagementSession> = {}): ManagementSession {
  return {
    username: '10000001',
    displayName: 'Tester',
    email: 'tester@example.com',
    authSource: 'LOCAL',
    roles: ['TEMPLATE_AUTHOR'],
    authorizedGroupCodes: ['RETAIL'],
    defaultRoute: ROUTE_KEYS.dashboardHome,
    visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement],
    expiresAt: '2099-01-01T00:00:00Z',
    ...partial,
  }
}

describe('routeCapabilities', () => {
  it('denies template route when capabilities explicitly false despite visibleRoutes', () => {
    const denied = canAccessRouteWithCapability(
      ROUTE_KEYS.templateManagement,
      session({
        capabilities: {
          manageMasters: false,
          reviewMasters: false,
          authorTemplates: false,
          decideTests: false,
          decideApprovals: false,
          publishTemplates: false,
          stopTemplates: false,
          restoreOrDeprecateTemplates: false,
          deleteTemplates: false,
          exportTemplates: false,
          viewCollaborationWorkItems: false,
          maintainCollaborationTimeoutConfig: false,
          authorContentModules: false,
          decideContentModuleReviews: false,
          manageContentModuleLifecycle: false,
          manageApiPolicy: false,
          readAudit: false,
          manageAssetLibrary: false,
        },
      }),
    )

    expect(denied).toBe(false)
  })

  it('allows route when capability guard passes and route is visible', () => {
    const allowed = canAccessRouteWithCapability(
      ROUTE_KEYS.templateManagement,
      session({
        capabilities: {
          manageMasters: false,
          reviewMasters: false,
          authorTemplates: true,
          decideTests: false,
          decideApprovals: false,
          publishTemplates: false,
          stopTemplates: false,
          restoreOrDeprecateTemplates: false,
          deleteTemplates: false,
          exportTemplates: false,
          viewCollaborationWorkItems: false,
          maintainCollaborationTimeoutConfig: false,
          authorContentModules: false,
          decideContentModuleReviews: false,
          manageContentModuleLifecycle: false,
          manageApiPolicy: false,
          readAudit: false,
          manageAssetLibrary: false,
        },
      }),
    )

    expect(allowed).toBe(true)
  })

  it('allows template route for tester with decideTests despite authorTemplates false', () => {
    const allowed = canAccessRouteWithCapability(
      ROUTE_KEYS.templateManagement,
      session({
        roles: ['TEMPLATE_TESTER'],
        capabilities: {
          manageMasters: false,
          reviewMasters: false,
          authorTemplates: false,
          decideTests: true,
          decideApprovals: false,
          publishTemplates: false,
          stopTemplates: false,
          restoreOrDeprecateTemplates: false,
          deleteTemplates: false,
          exportTemplates: false,
          viewCollaborationWorkItems: true,
          maintainCollaborationTimeoutConfig: false,
          authorContentModules: false,
          decideContentModuleReviews: false,
          manageContentModuleLifecycle: false,
          manageApiPolicy: false,
          readAudit: false,
          manageAssetLibrary: false,
        },
      }),
    )

    expect(allowed).toBe(true)
  })

  it('allows template route for approver with decideApprovals', () => {
    const allowed = canAccessRouteWithCapability(
      ROUTE_KEYS.templateManagement,
      session({
        roles: ['TEMPLATE_APPROVER'],
        capabilities: {
          manageMasters: false,
          reviewMasters: false,
          authorTemplates: false,
          decideTests: false,
          decideApprovals: true,
          publishTemplates: false,
          stopTemplates: false,
          restoreOrDeprecateTemplates: false,
          deleteTemplates: false,
          exportTemplates: false,
          viewCollaborationWorkItems: true,
          maintainCollaborationTimeoutConfig: false,
          authorContentModules: false,
          decideContentModuleReviews: false,
          manageContentModuleLifecycle: false,
          manageApiPolicy: false,
          readAudit: false,
          manageAssetLibrary: false,
        },
      }),
    )

    expect(allowed).toBe(true)
  })

  it('allows template route for publisher with publishTemplates despite authorTemplates false', () => {
    const allowed = canAccessRouteWithCapability(
      ROUTE_KEYS.templateManagement,
      session({
        roles: ['TEMPLATE_PUBLISHER'],
        capabilities: {
          manageMasters: false,
          reviewMasters: false,
          authorTemplates: false,
          decideTests: false,
          decideApprovals: false,
          publishTemplates: true,
          stopTemplates: false,
          restoreOrDeprecateTemplates: false,
          deleteTemplates: false,
          exportTemplates: false,
          viewCollaborationWorkItems: false,
          maintainCollaborationTimeoutConfig: false,
          authorContentModules: false,
          decideContentModuleReviews: false,
          manageContentModuleLifecycle: false,
          manageApiPolicy: false,
          readAudit: false,
          manageAssetLibrary: false,
        },
      }),
    )

    expect(allowed).toBe(true)
  })

  it('allows template route for tester via role fallback when capabilities absent', () => {
    const allowed = canAccessRouteWithCapability(
      ROUTE_KEYS.templateManagement,
      session({
        roles: ['TEMPLATE_TESTER'],
        capabilities: undefined,
      }),
    )

    expect(allowed).toBe(true)
  })

  it('allows template route for approver via role fallback when capabilities absent', () => {
    const allowed = canAccessRouteWithCapability(
      ROUTE_KEYS.templateManagement,
      session({
        roles: ['TEMPLATE_APPROVER'],
        capabilities: undefined,
      }),
    )

    expect(allowed).toBe(true)
  })

  it('fails closed on unknown route keys even when listed in visibleRoutes', () => {
    const denied = canAccessRouteWithCapability(
      'route.unknown-future-surface',
      session({
        roles: ['GLOBAL_ADMIN'],
        visibleRoutes: ['route.unknown-future-surface', ROUTE_KEYS.dashboardHome],
      }),
    )

    expect(denied).toBe(false)
  })

  it('allows asset library route when manageAssetLibrary is true', () => {
    const allowed = canAccessRouteWithCapability(
      ROUTE_KEYS.assetLibraryManagement,
      session({
        roles: ['TEMPLATE_AUTHOR'],
        visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.assetLibraryManagement],
        capabilities: {
          manageMasters: false,
          reviewMasters: false,
          authorTemplates: true,
          decideTests: false,
          decideApprovals: false,
          publishTemplates: false,
          stopTemplates: false,
          restoreOrDeprecateTemplates: false,
          deleteTemplates: false,
          exportTemplates: false,
          viewCollaborationWorkItems: false,
          maintainCollaborationTimeoutConfig: false,
          authorContentModules: false,
          decideContentModuleReviews: false,
          manageContentModuleLifecycle: false,
          manageApiPolicy: false,
          readAudit: false,
          manageAssetLibrary: true,
        },
      }),
    )
    expect(allowed).toBe(true)
  })

  it('denies asset library for AUDIT_ADMIN even when listed in visibleRoutes', () => {
    const denied = canAccessRouteWithCapability(
      ROUTE_KEYS.assetLibraryManagement,
      session({
        roles: ['AUDIT_ADMIN'],
        visibleRoutes: [ROUTE_KEYS.auditConsole, ROUTE_KEYS.assetLibraryManagement],
        capabilities: undefined,
      }),
    )
    expect(denied).toBe(false)
  })

  it('denies dashboard when role matrix would not grant it despite visibleRoutes', () => {
    const denied = canAccessRouteWithCapability(
      ROUTE_KEYS.dashboardHome,
      session({
        roles: ['AUDIT_ADMIN'],
        visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.auditConsole],
        capabilities: undefined,
      }),
    )

    expect(denied).toBe(false)
  })

  it('denies api-policy when role cannot manage policy despite visibleRoutes', () => {
    const denied = canAccessRouteWithCapability(
      ROUTE_KEYS.apiPolicyManagement,
      session({
        roles: ['TEMPLATE_AUTHOR'],
        visibleRoutes: [
          ROUTE_KEYS.dashboardHome,
          ROUTE_KEYS.templateManagement,
          ROUTE_KEYS.apiPolicyManagement,
        ],
        capabilities: undefined,
      }),
    )

    expect(denied).toBe(false)
  })

  it('allows dashboard for template author when visible and role-aligned', () => {
    expect(canAccessRouteWithCapability(ROUTE_KEYS.dashboardHome, session())).toBe(true)
  })
})
