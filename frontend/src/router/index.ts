import { createRouter, createWebHistory, type RouteLocationGeneric } from 'vue-router'
import { reportRouteAccessDenied } from '@/api/securityAudit'
import { i18n } from '@/i18n'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

function redirectLegacyApiPolicyDetail(to: RouteLocationGeneric) {
  const templateId = String(to.params.templateId ?? '')
  const domain = to.query.domain
  const location: { path: string; query: { tab: string }; hash?: string } = {
    path: `/templates/${templateId}`,
    query: { tab: 'apiAccess' },
  }
  if (typeof domain === 'string' && domain.length > 0) {
    location.hash = `#domain=${encodeURIComponent(domain)}`
  }
  return location
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: () => {
        const sessionStore = useSessionStore()
        return sessionStore.authenticated ? sessionStore.defaultHomePath() : '/login'
      },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/forbidden',
      name: 'forbidden',
      component: () => import('@/views/ForbiddenView.vue'),
      meta: { public: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/dashboard/DashboardView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.dashboardHome },
    },
    {
      path: '/entitlement/users',
      name: 'entitlement-users',
      component: () => import('@/views/identity/UserManagementView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.identityAdministration },
    },
    {
      path: '/entitlement/groups',
      name: 'entitlement-groups',
      component: () => import('@/views/identity/GroupManagementView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.identityAdministration },
    },
    {
      path: '/api/policies',
      name: 'api-policy-management',
      component: () => import('@/views/api/ApiPolicyHomeView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.apiPolicyManagement },
    },
    {
      path: '/api/policies/:templateId',
      redirect: redirectLegacyApiPolicyDetail,
    },
    {
      path: '/audit',
      name: 'audit-console',
      component: () => import('@/views/audit/AuditConsoleView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.auditConsole },
    },
    {
      path: '/governance/legal-holds',
      name: 'legal-hold-administration',
      component: () => import('@/views/legalHold/LegalHoldListView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.legalHoldAdministration },
    },
    {
      path: '/masters',
      name: 'master-list',
      component: () => import('@/views/masters/MasterListView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.masterManagement },
    },
    {
      path: '/masters/:masterId',
      name: 'master-package-hub',
      component: () => import('@/views/masters/MasterPackageHubView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.masterManagement },
    },
    {
      path: '/masters/:masterId/revisions/:revisionLineId',
      name: 'master-revision-detail',
      component: () => import('@/views/masters/MasterRevisionDetailView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.masterManagement },
    },
    {
      path: '/templates',
      name: 'template-list',
      component: () => import('@/views/templates/TemplateListView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.templateManagement },
    },
    {
      path: '/templates/:templateId',
      name: 'template-package-hub',
      component: () => import('@/views/templates/TemplatePackageHubView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.templateManagement },
    },
    {
      path: '/templates/:templateId/dev/:devVersionId',
      name: 'template-dev-version',
      component: () => import('@/views/templates/TemplateDevVersionEditorView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.templateManagement },
    },
    {
      path: '/templates/:templateId/releases/:releaseVersion',
      name: 'template-release-detail',
      component: () => import('@/views/templates/TemplateReleaseDetailView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.templateManagement },
    },
    {
      path: '/content-modules',
      name: 'content-module-list',
      component: () => import('@/views/contentModules/ContentModuleListView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.contentModuleManagement },
    },
    {
      path: '/content-modules/:moduleId',
      name: 'content-module-detail',
      component: () => import('@/views/contentModules/ContentModuleDetailView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.contentModuleManagement },
    },
    {
      path: '/library/assets',
      name: 'asset-library-list',
      component: () => import('@/views/library/AssetLibraryListView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.assetLibraryManagement },
    },
    { path: '/workbench/tester', redirect: '/dashboard#tasks-section' },
    { path: '/workbench/approver', redirect: '/dashboard#tasks-section' },
    { path: '/workbench/escalation', redirect: '/dashboard#tasks-section' },
    { path: '/home/global-governance', redirect: '/dashboard' },
    { path: '/home/group-governance', redirect: '/dashboard' },
    { path: '/home/template-authoring', redirect: '/dashboard' },
    { path: '/home/tester-workbench', redirect: '/dashboard#tasks-section' },
    { path: '/home/approver-workbench', redirect: '/dashboard#tasks-section' },
    { path: '/home/escalation-workbench', redirect: '/dashboard#tasks-section' },
    { path: '/home/identity', redirect: '/entitlement/users' },
    { path: '/home/api-policy', redirect: '/api/policies' },
    { path: '/home/audit', redirect: '/audit' },
  ],
})

let sessionRestorePromise: Promise<boolean> | null = null

router.beforeEach(async (to) => {
  const sessionStore = useSessionStore()

  if (!sessionStore.session && sessionStore.accessToken) {
    sessionRestorePromise ??= sessionStore.restoreSession()
    await sessionRestorePromise
  }

  if (to.meta.public) {
    if (to.name === 'login' && sessionStore.authenticated) {
      return sessionStore.defaultHomePath()
    }
    return true
  }

  if (!sessionStore.authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const logicalRoute = to.meta.logicalRoute
  if (typeof logicalRoute === 'string') {
    if (!sessionStore.canAccessRoute(logicalRoute)) {
      const traceId = crypto.randomUUID()
      sessionStore.recordRouteDeny(traceId)
      // Fire-and-forget: durable audit must not block Forbidden navigation (D7-C11).
      void reportRouteAccessDenied({ routeKey: logicalRoute, traceId }).catch(() => {
        // Fail-safe: reporting errors are intentionally ignored.
      })
      return {
        name: 'forbidden',
        query: { traceId },
      }
    }
  }

  return true
})

const ROUTE_TITLE_KEYS: Record<string, string> = {
  dashboard: 'dashboard.title',
  'template-list': 'templates.list.title',
  'template-package-hub': 'templates.packageHub.breadcrumbLabel',
  'master-list': 'masters.list.title',
  'master-package-hub': 'masters.hub.breadcrumbLabel',
  'audit-console': 'audit.title',
  'legal-hold-administration': 'legalHold.list.title',
  'api-policy-management': 'apiPolicy.home.title',
  'entitlement-users': 'identity.usersPageTitle',
  'entitlement-groups': 'identity.groupsPageTitle',
  'content-module-list': 'contentModules.list.title',
  'asset-library-list': 'assetLibrary.list.title',
  login: 'login.title',
  forbidden: 'forbidden.title',
}

router.afterEach((to) => {
  const appTitle = i18n.global.t('app.title')
  const name = typeof to.name === 'string' ? to.name : ''
  const pageKey = ROUTE_TITLE_KEYS[name]
  if (pageKey) {
    document.title = `${i18n.global.t(pageKey)} | ${appTitle}`
  } else {
    document.title = appTitle
  }
})

export default router
