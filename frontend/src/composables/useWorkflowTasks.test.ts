import { describe, expect, it, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { dashboardQuickLinks, useWorkflowTasks } from '@/composables/useWorkflowTasks'
import { useTemplatesStore } from '@/stores/templates'
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

const authorCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  authorTemplates: true,
}

const approverCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  decideApprovals: true,
}

const adminCapabilities: ManagementCapabilities = {
  ...testerCapabilities,
  publishTemplates: true,
}

describe('useWorkflowTasks', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management', 'route.master-management'],
      capabilities: testerCapabilities,
    } as never
  })

  it('builds template test tasks for testing lifecycle status', () => {
    const templatesStore = useTemplatesStore()
    templatesStore.templates = [
      {
        id: 't1',
        name: 'Letter',
        externalId: 'TPL-1',
        groupCode: 'RETAIL',
        lifecycleStatus: 'TESTING',
      } as never,
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'template-test')).toBe(true)
  })

  it('builds master review tasks for pending review masters', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
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

  it('builds template approval tasks when approval capability is granted', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      capabilities: approverCapabilities,
    } as never

    const templatesStore = useTemplatesStore()
    templatesStore.templates = [
      {
        id: 't2',
        name: 'Approval Letter',
        externalId: 'TPL-2',
        groupCode: 'RETAIL',
        lifecycleStatus: 'APPROVAL',
      } as never,
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'template-approval')).toBe(true)
  })

  it('builds template publish tasks for admins on pending release', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      capabilities: adminCapabilities,
    } as never

    const templatesStore = useTemplatesStore()
    templatesStore.templates = [
      {
        id: 't3',
        name: 'Release Letter',
        externalId: 'TPL-3',
        groupCode: 'RETAIL',
        lifecycleStatus: 'PENDING_RELEASE',
      } as never,
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'template-publish')).toBe(true)
  })

  it('builds draft tasks for authors on draft templates', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      ...sessionStore.session,
      capabilities: authorCapabilities,
    } as never

    const templatesStore = useTemplatesStore()
    templatesStore.templates = [
      {
        id: 't4',
        name: 'Draft Letter',
        externalId: 'TPL-4',
        groupCode: 'RETAIL',
        lifecycleStatus: 'DRAFT',
      } as never,
    ]

    const { tasks } = useWorkflowTasks()
    expect(tasks.value.some((task) => task.kind === 'template-author-draft')).toBe(true)
  })

  it('filters quick links by visible routes', () => {
    const links = dashboardQuickLinks(['route.template-management'])
    expect(links).toHaveLength(1)
    expect(links[0]?.labelKey).toBe('dashboard.quickLinks.templates')
  })
})
