import { createRouter, createWebHistory } from 'vue-router'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { canAccessLogicalRoute, sessionContext } from '@/auth/roles'
import { useSessionStore } from '@/stores/session'

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
      name: 'api-policy-detail',
      component: () => import('@/views/api/ApiPolicyDetailView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.apiPolicyManagement },
    },
    {
      path: '/audit',
      name: 'audit-console',
      component: () => import('@/views/audit/AuditConsoleView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.auditConsole },
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
      name: 'template-detail',
      component: () => import('@/views/templates/TemplateDetailView.vue'),
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
    const context = sessionContext(sessionStore.session)
    const allowed = canAccessLogicalRoute(
      logicalRoute,
      context,
      sessionStore.session?.visibleRoutes ?? [],
    )
    if (!allowed) {
      const traceId = crypto.randomUUID()
      sessionStore.recordRouteDeny(traceId)
      return {
        name: 'forbidden',
        query: { traceId },
      }
    }
  }

  return true
})

export default router
