import { describe, expect, it, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  buildTaskPartitions,
  dashboardQuickLinks,
  getVisibleCollaborationQueues,
  parseDashboardTaskScope,
  useWorkflowTasks,
} from '@/composables/useWorkflowTasks'
import { useCollaborationStore } from '@/stores/collaboration'
import { useAuthorWorkflowStore } from '@/stores/authorWorkflow'
import { useContentModulesStore } from '@/stores/contentModules'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'

const testerCapabilities: ManagementCapabilities = {
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
}

const reviewerCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  reviewMasters: true,
}

const managerCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  manageMasters: true,
}

const authorCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  authorTemplates: true,
  viewCollaborationWorkItems: false,
  decideTests: false,
}

describe('useWorkflowTasks', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['TEMPLATE_TESTER'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management', 'route.master-management'],
      capabilities: testerCapabilities,
    } as never
  })

  it('builds collaboration test tasks from backend work items', () => {
    const collaborationStore = useCollaborationStore()
    collaborationStore.workItems = [
      {
        workItemId: 'wi-1',
        templateId: 't1',
        templateName: 'Letter',
        groupCode: 'RETAIL',
        queue: 'TEST',
        triggerType: 'SUBMIT_FOR_TEST',
        submitterUserId: '10000003',
        summaryText: 'Template submitted for testing',
        createdAt: '2026-06-26T10:00:00Z',
        ageSeconds: 120,
      },
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'template-test' && task.source === 'collaboration')).toBe(true)
  })

  it('builds master review tasks for pending review masters', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      roles: ['GLOBAL_ADMIN'],
      capabilities: reviewerCapabilities,
    } as never

    const mastersStore = useMastersStore()
    mastersStore.masters = [
      {
        id: 'm1',
        name: 'Master A',
        groupCode: 'RETAIL',
        status: 'PENDING_REVIEW',
      } as never,
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'master-review')).toBe(true)
  })

  it('builds clause outdated bump tasks for template authors', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      roles: ['TEMPLATE_AUTHOR'],
      capabilities: authorCapabilities,
    } as never

    const authorWorkflowStore = useAuthorWorkflowStore()
    authorWorkflowStore.outdatedClauseTasks = [
      {
        templateId: 'tpl-1',
        externalId: 'TPL-1',
        groupCode: 'RETAIL',
        name: 'Loan Notice',
        inFlightDevVersionId: 'dev-1',
        outdatedReferenceCount: 2,
        updatedAt: '2026-07-14T10:00:00Z',
      },
    ]

    const { tasks } = useWorkflowTasks()
    const clauseTask = tasks.value.find((task) => task.kind === 'clause-outdated-bump')
    expect(clauseTask).toBeDefined()
    expect(clauseTask?.path).toContain('/templates/tpl-1/dev/dev-1')
    expect(clauseTask?.path).toContain('designTab=contentModules')
    expect(clauseTask?.templateId).toBe('tpl-1')

    const scope = parseDashboardTaskScope({}, { reviewMasters: false, manageMasters: false })
    const partitions = buildTaskPartitions(scope, tasks.value, {
      roles: ['TEMPLATE_AUTHOR'],
      capabilities: authorCapabilities,
    })
    const clausePartition = partitions.find((partition) => partition.kind === 'clause-outdated-bump')
    expect(clausePartition?.tasks).toHaveLength(1)
    expect(clausePartition?.headingKey).toBe('dashboard.tasks.clauseOutdatedBump.title')
  })

  it('builds master rework tasks for rejected masters when upload is allowed', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      roles: ['GLOBAL_ADMIN'],
      capabilities: managerCapabilities,
    } as never

    const mastersStore = useMastersStore()
    mastersStore.masters = [
      {
        id: 'm2',
        name: 'Rejected Master',
        groupCode: 'RETAIL',
        status: 'REJECTED',
      } as never,
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'master-rework')).toBe(true)
    const reworkTask = tasks.value.find((task) => task.kind === 'master-rework')
    expect(reworkTask?.titleKey).toBe('dashboard.tasks.masterRework.itemTitle')
  })

  it('builds master rework tasks for DRAFT masters with rejected review history', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      roles: ['MASTER_DESIGNER'],
      capabilities: managerCapabilities,
    } as never

    const mastersStore = useMastersStore()
    mastersStore.masters = [
      {
        id: 'm3',
        name: 'Draft rework',
        groupCode: 'RETAIL',
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
        updatedBy: '10000005',
        updatedAt: '2026-06-26T10:00:00Z',
      } as never,
    ]
    mastersStore.draftReviewHistoryByMasterId = {
      m3: [
        {
          action: 'REJECTED',
          decision: 'REJECTED',
          changeSummary: null,
          commentSummary: 'Fix layout',
          actorUsername: '10000001',
          createdAt: '2026-06-25T10:00:00Z',
        },
      ],
    }

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'master-rework' && task.id === 'master-rework-m3')).toBe(
      true,
    )
  })

  it('builds content-module review and rework tasks with lifecycle deep links', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      roles: ['TEMPLATE_APPROVER', 'TEMPLATE_AUTHOR'],
      capabilities: {
        ...testerCapabilities,
        decideContentModuleReviews: true,
        authorContentModules: true,
        decideTests: false,
        viewCollaborationWorkItems: false,
      },
    } as never

    const contentModulesStore = useContentModulesStore()
    contentModulesStore.workflowTasks = [
      {
        moduleId: 'MOD-A',
        moduleCode: 'MOD-A',
        name: 'Disclosure A',
        groupCode: 'RETAIL',
        kind: 'PENDING_REVIEW',
        semanticVersion: '1.0.0',
        updatedAt: '2026-07-15T10:00:00Z',
      },
      {
        moduleId: 'MOD-B',
        moduleCode: 'MOD-B',
        name: 'Disclosure B',
        groupCode: 'RETAIL',
        kind: 'REWORK',
        semanticVersion: '1.1.0',
        rejectionReason: 'Wording not acceptable',
        updatedAt: '2026-07-15T09:00:00Z',
      },
    ]

    const { tasks } = useWorkflowTasks()
    const reviewTask = tasks.value.find((task) => task.kind === 'content-module-review')
    const reworkTask = tasks.value.find((task) => task.kind === 'content-module-rework')
    expect(reviewTask?.entityName).toBe('Disclosure A')
    expect(reviewTask?.path).toBe('/content-modules/MOD-A?workspaceTab=lifecycle')
    expect(reworkTask?.entityName).toBe('Disclosure B')
    expect(reworkTask?.path).toBe('/content-modules/MOD-B?workspaceTab=lifecycle')
    expect(reworkTask?.summaryText).toBe('Wording not acceptable')

    const scope = parseDashboardTaskScope({}, { reviewMasters: false, manageMasters: false })
    const partitions = buildTaskPartitions(scope, tasks.value, {
      roles: ['TEMPLATE_APPROVER', 'TEMPLATE_AUTHOR'],
      capabilities: {
        ...testerCapabilities,
        decideContentModuleReviews: true,
        authorContentModules: true,
      },
    })
    expect(partitions.find((partition) => partition.kind === 'content-module-review')?.tasks).toHaveLength(1)
    expect(partitions.find((partition) => partition.kind === 'content-module-rework')?.tasks).toHaveLength(1)
  })

  it('hides content-module review/rework tasks without matching capabilities', () => {
    const contentModulesStore = useContentModulesStore()
    contentModulesStore.workflowTasks = [
      {
        moduleId: 'MOD-A',
        moduleCode: 'MOD-A',
        name: 'Disclosure A',
        groupCode: 'RETAIL',
        kind: 'PENDING_REVIEW',
        updatedAt: '2026-07-15T10:00:00Z',
      },
      {
        moduleId: 'MOD-B',
        moduleCode: 'MOD-B',
        name: 'Disclosure B',
        groupCode: 'RETAIL',
        kind: 'REWORK',
        rejectionReason: 'Fix wording',
        updatedAt: '2026-07-15T09:00:00Z',
      },
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'content-module-review')).toBe(false)
    expect(tasks.value.some((task) => task.kind === 'content-module-rework')).toBe(false)
  })

  it('filters quick links by visible routes without legacy workbench shortcuts', () => {
    const links = dashboardQuickLinks(['route.template-management'])
    expect(links.some((link) => link.labelKey === 'dashboard.quickLinks.templates')).toBe(true)
    expect(links).toHaveLength(1)
  })

  it('includes ESCALATION queue for group admin escalation visibility', () => {
    const adminCapabilities: ManagementCapabilities = {
      ...testerCapabilities,
      decideApprovals: true,
      publishTemplates: true,
    }
    const queues = getVisibleCollaborationQueues({
      roles: ['GROUP_ADMIN'],
      capabilities: adminCapabilities,
    })
    expect(queues).toContain('ESCALATION')
  })

  it('excludes ESCALATION queue for tester without admin role', () => {
    const queues = getVisibleCollaborationQueues({
      roles: ['TEMPLATE_TESTER'],
      capabilities: testerCapabilities,
    })
    expect(queues).not.toContain('ESCALATION')
  })

  it('sorts collaboration tasks newest-first by createdAt', () => {
    const collaborationStore = useCollaborationStore()
    collaborationStore.workItems = [
      {
        workItemId: 'wi-old',
        templateId: 't-old',
        templateName: 'Older',
        groupCode: 'RETAIL',
        queue: 'TEST',
        triggerType: 'SUBMIT_FOR_TEST',
        submitterUserId: '10000003',
        summaryText: 'Older item',
        createdAt: '2026-06-20T10:00:00Z',
        ageSeconds: 120,
      },
      {
        workItemId: 'wi-new',
        templateId: 't-new',
        templateName: 'Newer',
        groupCode: 'RETAIL',
        queue: 'TEST',
        triggerType: 'SUBMIT_FOR_TEST',
        submitterUserId: '10000003',
        summaryText: 'Newer item',
        createdAt: '2026-06-26T10:00:00Z',
        ageSeconds: 60,
      },
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value[0]?.workItemId).toBe('wi-new')
    expect(tasks.value[1]?.workItemId).toBe('wi-old')
  })

  it('sorts master review tasks by updatedAt surrogate newest-first', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      roles: ['GLOBAL_ADMIN'],
      capabilities: reviewerCapabilities,
    } as never

    const mastersStore = useMastersStore()
    mastersStore.masters = [
      {
        id: 'm-old',
        name: 'Older master',
        groupCode: 'RETAIL',
        status: 'PENDING_REVIEW',
        updatedAt: '2026-06-01T10:00:00Z',
      } as never,
      {
        id: 'm-new',
        name: 'Newer master',
        groupCode: 'RETAIL',
        status: 'PENDING_REVIEW',
        updatedAt: '2026-06-20T10:00:00Z',
      } as never,
    ]

    const scope = parseDashboardTaskScope(
      { filter: 'master-review' },
      { reviewMasters: true, manageMasters: false },
    )
    const { tasks } = useWorkflowTasks()
    const partitions = buildTaskPartitions(scope, tasks.value, {
      roles: ['GLOBAL_ADMIN'],
      capabilities: reviewerCapabilities,
    })

    expect(partitions).toHaveLength(1)
    expect(partitions[0]?.tasks[0]?.id).toBe('master-review-m-new')
  })

  it('parseDashboardTaskScope maps queue deep link to queue filter', () => {
    const scope = parseDashboardTaskScope(
      { queue: 'TEST' },
      { reviewMasters: false, manageMasters: false },
    )
    expect(scope.pageTitleKey).toBe('collaboration.workItem.queue.TEST.title')
    expect(scope.mode).toBe('queue')
    expect(scope.queueFilter).toBe('TEST')
    expect(scope.fetchCollaboration).toBe(true)
  })

  it('parseDashboardTaskScope ignores invalid queue and falls back to unfiltered hub', () => {
    const scope = parseDashboardTaskScope(
      { queue: 'NOT_A_QUEUE' },
      { reviewMasters: true, manageMasters: false },
    )
    expect(scope.pageTitleKey).toBe('dashboard.title')
    expect(scope.mode).toBe('unfiltered')
    expect(scope.queueFilter).toBeNull()
  })

  it('parseDashboardTaskScope maps master-review filter without collaboration fetch', () => {
    const scope = parseDashboardTaskScope(
      { filter: 'master-review' },
      { reviewMasters: true, manageMasters: true },
    )
    expect(scope.pageTitleKey).toBe('nav.behaviorItems.masterReview')
    expect(scope.fetchCollaboration).toBe(false)
    expect(scope.mode).toBe('master-review')
  })

  it('buildTaskPartitions keeps TEST and ESCALATION rows in separate sections', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      capabilities: {
        ...testerCapabilities,
        decideApprovals: true,
        publishTemplates: true,
      },
    } as never

    const collaborationStore = useCollaborationStore()
    collaborationStore.workItems = [
      {
        workItemId: 'wi-test',
        templateId: 'tpl-x',
        templateName: 'Template X',
        groupCode: 'RETAIL',
        queue: 'TEST',
        triggerType: 'SUBMIT_FOR_TEST',
        submitterUserId: '10000003',
        summaryText: 'Test item',
        createdAt: '2026-06-26T10:00:00Z',
        ageSeconds: 120,
      },
      {
        workItemId: 'wi-esc',
        templateId: 'tpl-x',
        templateName: 'Template X',
        groupCode: 'RETAIL',
        queue: 'ESCALATION',
        triggerType: 'TIMEOUT_ESCALATION',
        submitterUserId: '10000003',
        summaryText: 'Escalation item',
        createdAt: '2026-06-26T11:00:00Z',
        ageSeconds: 3600,
      },
    ]

    const scope = parseDashboardTaskScope({}, { reviewMasters: false, manageMasters: false })
    const { tasks } = useWorkflowTasks()
    const adminCapabilities: ManagementCapabilities = {
      ...testerCapabilities,
      decideApprovals: true,
      publishTemplates: true,
    }
    const partitions = buildTaskPartitions(scope, tasks.value, {
      roles: ['GLOBAL_ADMIN'],
      capabilities: adminCapabilities,
    })

    const testPartition = partitions.find((partition) => partition.queue === 'TEST')
    const escalationPartition = partitions.find((partition) => partition.queue === 'ESCALATION')
    expect(testPartition?.tasks).toHaveLength(1)
    expect(escalationPartition?.tasks).toHaveLength(1)
    expect(testPartition?.tasks[0]?.kind).toBe('template-test')
    expect(escalationPartition?.tasks[0]?.kind).toBe('template-escalation')
  })
})
