import { ROUTE_KEYS } from '@/routing/routeKeys'
import type { NavGroupDefinition } from '@/navigation/navCatalog'

/** User-facing navigation catalog. Order and grouping are fixed in the UI. */
export const NAV_GROUPS: NavGroupDefinition[] = [
  {
    id: 'overview',
    labelKey: 'nav.groups.overview',
    items: [
      {
        id: 'dashboard',
        routeKey: ROUTE_KEYS.dashboardHome,
        path: '/dashboard',
        labelKey: 'nav.items.dashboard',
      },
    ],
  },
  {
    id: 'entitlement',
    labelKey: 'nav.groups.entitlement',
    items: [
      {
        id: 'users',
        routeKey: ROUTE_KEYS.identityAdministration,
        path: '/entitlement/users',
        labelKey: 'nav.items.users',
      },
      {
        id: 'groups',
        routeKey: ROUTE_KEYS.identityAdministration,
        path: '/entitlement/groups',
        labelKey: 'nav.items.groups',
      },
    ],
  },
  {
    id: 'documentContent',
    labelKey: 'nav.groups.content',
    items: [
      {
        id: 'masters',
        routeKey: ROUTE_KEYS.masterManagement,
        path: '/masters',
        labelKey: 'nav.items.masters',
      },
      {
        id: 'templates',
        routeKey: ROUTE_KEYS.templateManagement,
        path: '/templates',
        labelKey: 'nav.items.templates',
      },
      {
        id: 'content-modules',
        routeKey: ROUTE_KEYS.contentModuleManagement,
        path: '/content-modules',
        labelKey: 'nav.items.contentModules',
      },
      {
        id: 'asset-library',
        routeKey: ROUTE_KEYS.assetLibraryManagement,
        path: '/library/assets',
        labelKey: 'nav.items.assetLibrary',
      },
    ],
  },
  {
    id: 'api',
    labelKey: 'nav.groups.apiAccess',
    items: [
      {
        id: 'api-policies',
        routeKey: ROUTE_KEYS.apiPolicyManagement,
        path: '/api/policies',
        labelKey: 'nav.items.apiPolicies',
      },
    ],
  },
  {
    id: 'security',
    labelKey: 'nav.groups.security',
    items: [
      {
        id: 'audit',
        routeKey: ROUTE_KEYS.auditConsole,
        path: '/audit',
        labelKey: 'nav.items.audit',
      },
      {
        id: 'legal-holds',
        routeKey: ROUTE_KEYS.legalHoldAdministration,
        path: '/governance/legal-holds',
        labelKey: 'nav.items.legalHolds',
      },
      // ADR-0071: Document brands + Legal entities retired from product nav (Wave 1 hide;
      // Wave 6 hard runtime retire). Security = audit + legal holds only.
    ],
  },
]
