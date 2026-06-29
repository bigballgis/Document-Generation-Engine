import { describe, expect, it } from 'vitest'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'
import {
  BEHAVIOR_NAV_ITEM_SPECS,
  NAV_GROUPS,
  buildVisibleBehaviorNavItems,
  buildVisibleNavGroups,
  resolveNavItemTarget,
} from '@/navigation/navStructure'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import type { ManagementCapabilities } from '@/types/session'

const dashboardRoute = ROUTE_KEYS.dashboardHome

const globalAdminCapabilities: ManagementCapabilities = {
  manageMasters: true,
  reviewMasters: true,
  authorTemplates: true,
  decideTests: true,
  decideApprovals: true,
  publishTemplates: true,
  stopTemplates: true,
  restoreOrDeprecateTemplates: true,
  deleteTemplates: true,
  manageApiPolicy: true,
  readAudit: true,
}

const testerCapabilities: ManagementCapabilities = {
  ...globalAdminCapabilities,
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: false,
  decideTests: true,
  decideApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  manageApiPolicy: false,
  readAudit: false,
}

const approverCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  decideTests: false,
  decideApprovals: true,
}

const authorCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  decideTests: false,
  authorTemplates: true,
}

const masterDesignerCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  decideTests: false,
  authorTemplates: true,
  manageMasters: true,
}

function behaviorIds(groups: ReturnType<typeof buildVisibleNavGroups>): string[] {
  return groups.find((group) => group.id === 'myTodos')?.items.map((item) => item.id) ?? []
}

describe('navStructure', () => {
  it('does not include standalone workbench navigation', () => {
    const groups = buildVisibleNavGroups(
      [dashboardRoute, ROUTE_KEYS.templateManagement],
      ['TEMPLATE_TESTER'],
      testerCapabilities,
    )

    expect(groups.some((group) => group.id === 'workbench')).toBe(false)
  })

  it('keeps dashboard as the sole overview entry for collaboration roles', () => {
    const groups = buildVisibleNavGroups(
      [dashboardRoute, ROUTE_KEYS.templateManagement],
      ['TEMPLATE_TESTER'],
      testerCapabilities,
    )

    const overviewGroup = groups.find((group) => group.id === 'overview')
    expect(overviewGroup?.items.map((item) => item.id)).toEqual(['dashboard'])
  })

  describe('behavior nav visibility (Spec C)', () => {
    it('shows only testing entry for TEMPLATE_TESTER with decideTests', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.templateManagement],
        ['TEMPLATE_TESTER'],
        testerCapabilities,
      )

      expect(behaviorIds(groups)).toEqual(['behavior-testing'])
    })

    it('shows only approval entry for TEMPLATE_APPROVER with decideApprovals', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.templateManagement],
        ['TEMPLATE_APPROVER'],
        approverCapabilities,
      )

      expect(behaviorIds(groups)).toEqual(['behavior-approval'])
    })

    it('shows only remediation entry for TEMPLATE_AUTHOR with authorTemplates', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.templateManagement],
        ['TEMPLATE_AUTHOR'],
        authorCapabilities,
      )

      expect(behaviorIds(groups)).toEqual(['behavior-remediation'])
    })

    it('shows all six behavior entries for GROUP_ADMIN', () => {
      const groups = buildVisibleNavGroups(
        [
          dashboardRoute,
          ROUTE_KEYS.masterManagement,
          ROUTE_KEYS.templateManagement,
          ROUTE_KEYS.apiPolicyManagement,
          ROUTE_KEYS.identityAdministration,
        ],
        ['GROUP_ADMIN'],
        globalAdminCapabilities,
      )

      expect(behaviorIds(groups)).toEqual([
        'behavior-testing',
        'behavior-approval',
        'behavior-remediation',
        'behavior-pending-release',
        'behavior-escalation',
        'behavior-master-review',
      ])
    })

    it('shows only master-review for MASTER_DESIGNER with master-management route', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.masterManagement, ROUTE_KEYS.templateManagement],
        ['MASTER_DESIGNER'],
        masterDesignerCapabilities,
      )

      expect(behaviorIds(groups)).toEqual(['behavior-master-review'])
    })

    it('omits myTodos group entirely for AUDIT_ADMIN without dashboard-home', () => {
      const groups = buildVisibleNavGroups(
        [ROUTE_KEYS.auditConsole],
        ['AUDIT_ADMIN'],
        { ...globalAdminCapabilities, readAudit: true, decideTests: false },
      )

      expect(groups.some((group) => group.id === 'myTodos')).toBe(false)
    })

    it('hides behavior entries when dashboard-home is missing (fail-closed)', () => {
      const items = buildVisibleBehaviorNavItems(
        { roles: ['TEMPLATE_TESTER'], capabilities: testerCapabilities },
        [ROUTE_KEYS.templateManagement],
      )

      expect(items).toEqual([])
    })

    it('hides entry when capability is explicitly false even if role would grant', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.templateManagement],
        ['TEMPLATE_TESTER'],
        { ...testerCapabilities, decideTests: false },
      )

      expect(groups.some((group) => group.id === 'myTodos')).toBe(false)
    })
  })

  describe('behavior group placement', () => {
    it('inserts myTodos after overview and before entitlement', () => {
      const groups = buildVisibleNavGroups(
        [
          dashboardRoute,
          ROUTE_KEYS.identityAdministration,
          ROUTE_KEYS.templateManagement,
        ],
        ['TEMPLATE_TESTER'],
        testerCapabilities,
      )

      expect(groups.map((group) => group.id)).toEqual([
        'overview',
        'myTodos',
        'entitlement',
        'documentContent',
      ])
    })

    it('omits myTodos group when no behavior entries are visible', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.auditConsole],
        ['AUDIT_ADMIN'],
        {
          manageMasters: false,
          reviewMasters: false,
          authorTemplates: false,
          decideTests: false,
          decideApprovals: false,
          publishTemplates: false,
          stopTemplates: false,
          restoreOrDeprecateTemplates: false,
          deleteTemplates: false,
          manageApiPolicy: false,
          readAudit: true,
        },
      )

      expect(groups.map((group) => group.id)).not.toContain('myTodos')
    })
  })

  describe('deep-link contract', () => {
    it('resolves testing deep link with queue and hash', () => {
      const items = buildVisibleBehaviorNavItems(
        { roles: ['TEMPLATE_TESTER'], capabilities: testerCapabilities },
        [dashboardRoute],
      )
      const testing = items.find((item) => item.id === 'behavior-testing')
      expect(testing).toBeDefined()
      expect(resolveNavItemTarget(testing!)).toEqual({
        path: '/dashboard',
        query: { queue: 'TEST' },
        hash: '#tasks-section',
      })
    })

    it('resolves master-review deep link with filter and hash', () => {
      const items = buildVisibleBehaviorNavItems(
        { roles: ['GROUP_ADMIN'], capabilities: globalAdminCapabilities },
        [dashboardRoute, ROUTE_KEYS.masterManagement],
      )
      const masterReview = items.find((item) => item.id === 'behavior-master-review')
      expect(masterReview).toBeDefined()
      expect(resolveNavItemTarget(masterReview!)).toEqual({
        path: '/dashboard',
        query: { filter: 'master-review' },
        hash: '#tasks-section',
      })
    })

    it('keeps overview dashboard path unfiltered', () => {
      const groups = buildVisibleNavGroups([dashboardRoute], ['TEMPLATE_TESTER'], testerCapabilities)
      const dashboard = groups.find((group) => group.id === 'overview')?.items[0]
      expect(dashboard).toBeDefined()
      expect(resolveNavItemTarget(dashboard!)).toEqual({ path: '/dashboard' })
    })

    it('defines stable behavior item ids matching spec catalog', () => {
      expect(BEHAVIOR_NAV_ITEM_SPECS.map((spec) => spec.id)).toEqual([
        'behavior-testing',
        'behavior-approval',
        'behavior-remediation',
        'behavior-pending-release',
        'behavior-escalation',
        'behavior-master-review',
      ])
    })
  })

  describe('resource nav structure regression', () => {
    it('preserves resource group ids and item ids/routeKeys/paths', () => {
      expect(NAV_GROUPS.map((group) => group.id)).toEqual([
        'overview',
        'entitlement',
        'documentContent',
        'api',
        'security',
      ])

      const resourceItems = NAV_GROUPS.flatMap((group) =>
        group.items.map((item) => ({
          id: item.id,
          routeKey: item.routeKey,
          path: item.path,
        })),
      )

      expect(resourceItems).toEqual([
        { id: 'dashboard', routeKey: dashboardRoute, path: '/dashboard' },
        { id: 'users', routeKey: ROUTE_KEYS.identityAdministration, path: '/entitlement/users' },
        { id: 'groups', routeKey: ROUTE_KEYS.identityAdministration, path: '/entitlement/groups' },
        { id: 'masters', routeKey: ROUTE_KEYS.masterManagement, path: '/masters' },
        { id: 'templates', routeKey: ROUTE_KEYS.templateManagement, path: '/templates' },
        {
          id: 'content-modules',
          routeKey: ROUTE_KEYS.contentModuleManagement,
          path: '/content-modules',
        },
        { id: 'api-policies', routeKey: ROUTE_KEYS.apiPolicyManagement, path: '/api/policies' },
        { id: 'audit', routeKey: ROUTE_KEYS.auditConsole, path: '/audit' },
      ])
    })

    it('keeps API management hidden for tester without api-policy route', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.templateManagement],
        ['TEMPLATE_TESTER'],
        testerCapabilities,
      )

      const apiGroup = groups.find((group) => group.id === 'api')
      expect(apiGroup).toBeUndefined()
    })
  })

  describe('L1 terminology / i18n (Spec B)', () => {
    const inScopeEnValues = [
      en.nav.groups.entitlement,
      en.nav.groups.apiAccess,
      en.nav.groups.security,
      en.nav.items.masters,
      en.nav.items.contentModules,
      en.nav.items.apiPolicies,
      en.nav.items.audit,
      en.nav.routes.identityAdministration,
      en.nav.routes.audit,
      en.nav.routes.apiPolicy,
      en.nav.routes.masters,
      en.nav.routes.templateAuthoring,
      en.dashboard.title,
      en.dashboard.description,
      en.dashboard.stats.sectionDescription,
      en.dashboard.stats.pendingActions.description,
      en.dashboard.tasks.title,
      en.dashboard.tasks.description,
      en.dashboard.tasks.masterReview.title,
      en.dashboard.tasks.templateTest.title,
      en.dashboard.tasks.templateApproval.title,
      en.dashboard.tasks.templatePublish.title,
      en.dashboard.tasks.templateDraft.title,
      en.collaboration.workItems.empty,
      en.collaboration.timeoutConfig.title,
      en.collaboration.workItem.queue.TEST.label,
      en.collaboration.workItem.queue.APPROVAL.label,
      en.collaboration.workItem.queue.REMEDIATION.label,
      en.collaboration.workItem.queue.PENDING_RELEASE.label,
      en.collaboration.workItem.queue.ESCALATION.label,
    ]

    const forbiddenPattern =
      /\b(policy|credential|lifecycle|semver|gate|governance overview|audit console|anchor integrity)\b/i

    it('includes new behavior nav keys in en and zh-CN', () => {
      expect(en.nav.groups.myTodos).toBe('My to-dos')
      expect(en.nav.behaviorItems.testing).toBe('Waiting on my testing')
      expect(zhCN.nav.groups.myTodos).toBe('我的待办')
      expect(zhCN.nav.behaviorItems.testing).toBe('待我测试')
    })

    it('passes forbidden L1 token grep on in-scope en values', () => {
      for (const value of inScopeEnValues) {
        expect(value).not.toMatch(forbiddenPattern)
      }
    })

    it('aligns zh-CN nav L1 with business semantics', () => {
      expect(zhCN.nav.groups.apiAccess).toBe('对外服务')
      expect(zhCN.nav.items.audit).toBe('操作记录')
      expect(zhCN.nav.routes.audit).not.toContain('审计控制台')
    })
  })
})
