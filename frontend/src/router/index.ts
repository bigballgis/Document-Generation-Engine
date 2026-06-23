import { createRouter, createWebHistory } from 'vue-router'
import { ROUTE_KEYS } from '@/routing/routeKeys'
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
      path: '/home/global-governance',
      name: 'global-governance-home',
      component: () => import('@/views/home/RoleHomeView.vue'),
      props: {
        routeKey: ROUTE_KEYS.globalGovernanceHome,
        titleKey: 'home.globalGovernance.title',
        descriptionKey: 'home.globalGovernance.description',
      },
      meta: { logicalRoute: ROUTE_KEYS.globalGovernanceHome },
    },
    {
      path: '/home/group-governance',
      name: 'group-governance-home',
      component: () => import('@/views/home/RoleHomeView.vue'),
      props: {
        routeKey: ROUTE_KEYS.groupGovernanceHome,
        titleKey: 'home.groupGovernance.title',
        descriptionKey: 'home.groupGovernance.description',
      },
      meta: { logicalRoute: ROUTE_KEYS.groupGovernanceHome },
    },
    {
      path: '/home/template-authoring',
      name: 'template-authoring-home',
      component: () => import('@/views/home/RoleHomeView.vue'),
      props: {
        routeKey: ROUTE_KEYS.templateAuthoringHome,
        titleKey: 'home.templateAuthoring.title',
        descriptionKey: 'home.templateAuthoring.description',
      },
      meta: { logicalRoute: ROUTE_KEYS.templateAuthoringHome },
    },
    {
      path: '/home/tester-workbench',
      name: 'tester-workbench',
      component: () => import('@/views/tester/TesterWorkbenchView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.testerWorkbench },
    },
    {
      path: '/home/approver-workbench',
      name: 'approver-workbench',
      component: () => import('@/views/approver/ApproverWorkbenchView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.approverWorkbench },
    },
    {
      path: '/home/identity',
      name: 'identity-administration',
      component: () => import('@/views/identity/IdentityAdministrationView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.identityAdministration },
    },
    {
      path: '/home/api-policy',
      name: 'api-policy-management',
      component: () => import('@/views/api/ApiPolicyHomeView.vue'),
      meta: { logicalRoute: ROUTE_KEYS.apiPolicyManagement },
    },
    {
      path: '/home/audit',
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
      name: 'master-detail',
      component: () => import('@/views/masters/MasterDetailView.vue'),
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
  if (typeof logicalRoute === 'string' && !sessionStore.canAccessRoute(logicalRoute)) {
    const traceId = crypto.randomUUID()
    sessionStore.recordRouteDeny(traceId)
    return {
      name: 'forbidden',
      query: { traceId },
    }
  }

  return true
})

export default router
