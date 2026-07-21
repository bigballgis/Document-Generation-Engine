import { describe, expect, it } from 'vitest'
import { collectLeafKeys, resolveLeafValue } from '@/i18n/collectLeafKeys'
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
import { globalAdminJourneySteps } from '@/constants/roleJourneyDefinitions'
import type { ManagementCapabilities } from '@/types/session'

const dashboardRoute = ROUTE_KEYS.dashboardHome

const globalAdminCapabilities: ManagementCapabilities = {
  manageMasters: true,
  reviewMasters: true,
  authorTemplates: true,
  decideTests: true,
  decideApprovals: true,
  decideLegalApprovals: false,
  publishTemplates: true,
  stopTemplates: true,
  restoreOrDeprecateTemplates: true,
  deleteTemplates: true,
  exportTemplates: true,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: true,
  authorContentModules: true,
  decideContentModuleReviews: true,
  manageContentModuleLifecycle: true,
  manageApiPolicy: true,
  readAudit: true,
  manageAssetLibrary: true,
  manageLegalHold: true,
}

const testerCapabilities: ManagementCapabilities = {
  ...globalAdminCapabilities,
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: false,
  decideTests: true,
  decideApprovals: false,
  decideLegalApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: false,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: false,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
  manageAssetLibrary: true,
  manageLegalHold: false,
}

const approverCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  decideTests: false,
  decideApprovals: true,
  decideLegalApprovals: false,
  decideContentModuleReviews: true,
}

const authorCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  decideTests: false,
  authorTemplates: true,
  exportTemplates: true,
  authorContentModules: true,
}

const masterDesignerCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  decideTests: false,
  authorTemplates: true,
  authorContentModules: true,
  manageMasters: true,
}

function behaviorItemIds(
  roles: string[],
  capabilities: ManagementCapabilities,
  visibleRoutes: string[] = [dashboardRoute, ROUTE_KEYS.templateManagement],
): string[] {
  return buildVisibleBehaviorNavItems(
    { roles, capabilities },
    visibleRoutes,
  ).map((item) => item.id)
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

  it('includes asset library under document content when route is visible', () => {
    const groups = buildVisibleNavGroups(
      [
        dashboardRoute,
        ROUTE_KEYS.templateManagement,
        ROUTE_KEYS.assetLibraryManagement,
      ],
      ['DOCUMENT_AUTHOR'],
      authorCapabilities,
    )
    const contentGroup = groups.find((group) => group.id === 'documentContent')
    expect(contentGroup?.items.some((item) => item.id === 'asset-library')).toBe(true)
    expect(contentGroup?.items.find((item) => item.id === 'asset-library')?.path).toBe(
      '/library/assets',
    )
  })

  it('hides asset library when route is not visible', () => {
    const groups = buildVisibleNavGroups(
      [dashboardRoute, ROUTE_KEYS.templateManagement],
      ['DOCUMENT_AUTHOR'],
      authorCapabilities,
    )
    const contentGroup = groups.find((group) => group.id === 'documentContent')
    expect(contentGroup?.items.some((item) => item.id === 'asset-library')).toBe(false)
  })

  describe('behavior nav visibility (Spec C)', () => {
    it('shows only testing entry for TEMPLATE_TESTER with decideTests', () => {
      expect(
        behaviorItemIds(['TEMPLATE_TESTER'], testerCapabilities),
      ).toEqual(['behavior-testing'])
    })

    it('shows approval (+ escalation) for GROUP_ADMIN with decideApprovals (ex-approver)', () => {
      expect(
        behaviorItemIds(['GROUP_ADMIN'], approverCapabilities),
      ).toEqual(['behavior-approval', 'behavior-escalation'])
    })

    it('shows only remediation entry for TEMPLATE_AUTHOR with authorTemplates', () => {
      expect(
        behaviorItemIds(['DOCUMENT_AUTHOR'], authorCapabilities),
      ).toEqual(['behavior-remediation'])
    })

    it('shows all six behavior entries for GROUP_ADMIN', () => {
      expect(
        behaviorItemIds(
          ['GROUP_ADMIN'],
          globalAdminCapabilities,
          [
            dashboardRoute,
            ROUTE_KEYS.masterManagement,
            ROUTE_KEYS.templateManagement,
            ROUTE_KEYS.apiPolicyManagement,
            ROUTE_KEYS.identityAdministration,
          ],
        ),
      ).toEqual([
        'behavior-testing',
        'behavior-approval',
        'behavior-remediation',
        'behavior-pending-release',
        'behavior-escalation',
        'behavior-master-review',
      ])
    })

    it('shows remediation + master-review for DOCUMENT_AUTHOR with master-management route', () => {
      expect(
        behaviorItemIds(
          ['DOCUMENT_AUTHOR'],
          masterDesignerCapabilities,
          [dashboardRoute, ROUTE_KEYS.masterManagement, ROUTE_KEYS.templateManagement],
        ),
      ).toEqual(['behavior-remediation', 'behavior-master-review'])
    })

    it('returns no behavior items for AUDIT_ADMIN without dashboard-home', () => {
      expect(
        buildVisibleBehaviorNavItems(
          { roles: ['AUDIT_ADMIN'], capabilities: { ...globalAdminCapabilities, readAudit: true, decideTests: false } },
          [ROUTE_KEYS.auditConsole],
        ),
      ).toEqual([])
    })

    it('hides behavior entries when dashboard-home is missing (fail-closed)', () => {
      const items = buildVisibleBehaviorNavItems(
        { roles: ['TEMPLATE_TESTER'], capabilities: testerCapabilities },
        [ROUTE_KEYS.templateManagement],
      )

      expect(items).toEqual([])
    })

    it('hides entry when capability is explicitly false even if role would grant', () => {
      expect(
        behaviorItemIds(['TEMPLATE_TESTER'], { ...testerCapabilities, decideTests: false }),
      ).toEqual([])
    })
  })

  describe('sidebar nav groups', () => {
    it('does not inject myTodos group into visible nav groups', () => {
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
        'entitlement',
        'documentContent',
      ])
    })

    it('omits myTodos group when no behavior entries are visible', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.auditConsole],
        ['AUDIT_ADMIN'],
        {
          ...globalAdminCapabilities,
          manageMasters: false,
          reviewMasters: false,
          authorTemplates: false,
          decideTests: false,
          decideApprovals: false,
  decideLegalApprovals: false,
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
          readAudit: true,
          manageAssetLibrary: true,
          manageLegalHold: false,
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
        'behavior-legal',
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
        {
          id: 'asset-library',
          routeKey: ROUTE_KEYS.assetLibraryManagement,
          path: '/library/assets',
        },
        { id: 'api-policies', routeKey: ROUTE_KEYS.apiPolicyManagement, path: '/api/policies' },
        {
          id: 'api-invocations',
          routeKey: ROUTE_KEYS.apiPolicyManagement,
          path: '/api/invocations',
        },
        { id: 'audit', routeKey: ROUTE_KEYS.auditConsole, path: '/audit' },
        {
          id: 'legal-holds',
          routeKey: ROUTE_KEYS.legalHoldAdministration,
          path: '/governance/legal-holds',
        },
      ])
    })

    it('hides Document brands and Legal entities from Security nav (BDD-SYS-NORM-W1-003)', () => {
      const security = NAV_GROUPS.find((group) => group.id === 'security')
      expect(security?.items.map((item) => item.id)).toEqual(['audit', 'legal-holds'])
      expect(security?.items.some((item) => item.id === 'document-brands')).toBe(false)
      expect(security?.items.some((item) => item.id === 'legal-entities')).toBe(false)
    })

    it('shows legal holds only when route is visible', () => {
      const withHold = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.legalHoldAdministration],
        ['GLOBAL_ADMIN'],
        globalAdminCapabilities,
      )
      const security = withHold.find((group) => group.id === 'security')
      expect(security?.items.map((item) => item.id)).toEqual(['legal-holds'])

      const withoutHold = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.auditConsole],
        ['GROUP_ADMIN'],
        { ...globalAdminCapabilities, manageLegalHold: false },
      )
      const securityWithout = withoutHold.find((group) => group.id === 'security')
      expect(securityWithout?.items.map((item) => item.id)).toEqual(['audit'])
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

    it('BDD-SYS-NORM-W3-012: External services nav includes dashboard + invocations', () => {
      const groups = buildVisibleNavGroups(
        [dashboardRoute, ROUTE_KEYS.apiPolicyManagement],
        ['GLOBAL_ADMIN'],
        globalAdminCapabilities,
      )
      const apiGroup = groups.find((group) => group.id === 'api')
      expect(apiGroup?.items.map((item) => item.id)).toEqual(['api-policies', 'api-invocations'])
      expect(apiGroup?.items.some((item) => item.path.includes('/settings'))).toBe(false)
      expect(en.nav.items.apiInvocations).toBe('Invocation records')
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
      en.audit.title,
      en.audit.description,
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

    it('passes forbidden L1 token grep on API management copy (P21-T09a)', () => {
      const apiManagementL1Values = [
        en.apiPolicy.home.title,
        en.apiPolicy.home.description,
        en.apiPolicy.detail.title,
        en.apiPolicy.detail.openConsole,
        en.apiPolicy.detail.templateTabHint,
        en.apiPolicy.detail.saveSuccess,
        en.apiPolicy.detail.domains.OUTPUT_POLICY,
        en.apiPolicy.detail.impact.confirmTitle,
        en.templates.policy.title,
        en.templates.policy.credentialsTitle,
        en.templates.policy.createCredential,
        en.templates.policy.notConfiguredTitle,
        en.templates.policy.impact.title,
        en.templates.error.loadPolicy,
        en.templates.error.loadCredentials,
        en.templates.publishGate.checkCodes.API_POLICY,
        en.api.apimgmt.policyImpact.blocking,
        en.api.apimgmt.policyImpact.safe,
        en.dashboard.quickLinks.apiPolicies,
      ]

      for (const value of apiManagementL1Values) {
        expect(value).not.toMatch(forbiddenPattern)
      }

      expect(en.apiPolicy.home.title).toBe('External services overview')
      expect(zhCN.apiPolicy.home.title).toBe('对外服务概览')
      expect(zhCN.templates.policy.credentialsTitle).toBe('接入账号')
    })

    it('passes forbidden L1 token grep on reminder timing + confirm-on-behalf copy (P21-T09b)', () => {
      const reminderExceptionL1Values = [
        en.collaboration.timeoutConfig.title,
        en.collaboration.timeoutConfig.description,
        en.collaboration.timeoutConfig.save,
        en.collaboration.timeoutConfig.saveSuccess,
        en.collaboration.timeoutConfig.testThresholdHours,
        en.collaboration.timeoutConfig.approvalThresholdHours,
        en.collaboration.timeoutConfig.pendingReleaseThresholdHours,
        en.collaboration.timeoutConfig.remediationThresholdHours,
        en.collaboration.timeoutConfig.error.load,
        en.collaboration.timeoutConfig.error.save,
        en.collaboration.workItem.trigger.TIMEOUT_ESCALATION.description,
        en.templates.lifecycle.decisionForm.exceptionIntro,
        en.templates.lifecycle.decisionForm.exceptionIntervention,
        en.templates.lifecycle.decisionForm.exceptionReason,
        en.templates.lifecycle.decisionForm.exceptionSecondaryConfirm,
      ]

      for (const value of reminderExceptionL1Values) {
        expect(value).not.toMatch(forbiddenPattern)
        expect(value.toLowerCase()).not.toMatch(/\bescalation\b/)
      }

      expect(en.collaboration.timeoutConfig.saveSuccess).toBe('Reminder timing saved.')
      expect(en.templates.lifecycle.decisionForm.exceptionIntervention).toBe('Confirm on behalf')
      expect(zhCN.templates.lifecycle.decisionForm.exceptionIntervention).toBe('代为确认')
    })

    it('passes forbidden L1 token grep on global admin journey copy (P21-T10)', () => {
      const globalAdminL1Values = [
        en.journey.roles.GLOBAL_ADMIN.title,
        en.journey.roles.GLOBAL_ADMIN.empty.guidance,
        ...globalAdminJourneySteps.flatMap((step) => {
          const stepKey = step.id as keyof typeof en.journey.roles.GLOBAL_ADMIN.steps
          const stepCopy = en.journey.roles.GLOBAL_ADMIN.steps[stepKey]
          return [stepCopy.label, stepCopy.guidance, stepCopy.cta]
        }),
        en.identity.title,
        en.identity.description,
      ]

      for (const value of globalAdminL1Values) {
        expect(value).not.toMatch(forbiddenPattern)
        expect(value.toLowerCase()).not.toMatch(/\b(governance|entitlement|console|escalation)\b/)
      }

      expect(en.journey.roles.GLOBAL_ADMIN.title).toBe('Bank-wide administration workflow')
    })

    it('passes forbidden L1 token grep on residual template/audit/contentModule copy (P21-X01)', () => {
      const p21ForbiddenPattern =
        /\b(policy|credential|lifecycle|semver|gate|governance|console|orchestrate|entitlement|anchor integrity|\banchor\b)\b/i

      function collectStringLeaves(
        root: Record<string, unknown>,
        excludedPrefixes: string[] = [],
      ): string[] {
        return collectLeafKeys(root)
          .filter((key) => !excludedPrefixes.some((prefix) => key.startsWith(prefix)))
          .map((key) => resolveLeafValue(root, key))
          .filter((value): value is string => typeof value === 'string')
      }

      const templateValues = collectStringLeaves(en.templates as Record<string, unknown>, [
        'contract.',
        'bindingGate.invalidBindingLine',
      ])
      const auditValues = collectStringLeaves(en.audit as Record<string, unknown>, [
        'eventTypes.',
        'export.managementFilename',
        'export.lifecycleFilename',
      ])
      const contentModuleValues = collectStringLeaves(en.contentModules as Record<string, unknown>)

      const residualL1Values = [...templateValues, ...auditValues, ...contentModuleValues]

      for (const value of residualL1Values) {
        expect(value).not.toMatch(p21ForbiddenPattern)
        expect(value.toLowerCase()).not.toMatch(/\bescalation\b/)
      }

      expect(en.templates.lifecycle.title).toBe('Workflow actions')
      expect(en.templates.publishGate.title).toBe('Pre-release checks')
      expect(en.templates.governance.title).toBe('Post-publish controls')
      expect(en.contentModules.list.title).toBe('Standard clauses')
      expect(en.nav.routes.globalGovernance).toBe('Bank-wide administration')
    })
  })
})
