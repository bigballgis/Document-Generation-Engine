import { describe, expect, it, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { dashboardQuickLinks, useWorkflowTasks } from '@/composables/useWorkflowTasks'
import { useCollaborationStore } from '@/stores/collaboration'
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
  })

  it('filters quick links by visible routes without workbench shortcuts', () => {
    const links = dashboardQuickLinks(['route.template-management'])
    expect(links.some((link) => link.labelKey === 'dashboard.quickLinks.templates')).toBe(true)
    expect(links.some((link) => link.labelKey === 'dashboard.quickLinks.testerWorkbench')).toBe(false)
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
})
