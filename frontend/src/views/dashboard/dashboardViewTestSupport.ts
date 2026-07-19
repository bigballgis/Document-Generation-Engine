import { expect, vi } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { ref } from 'vue'
import DashboardView from '@/views/dashboard/DashboardView.vue'
import { useTemplatesStore } from '@/stores/templates'
import { useCollaborationStore } from '@/stores/collaboration'
import { useSessionStore } from '@/stores/session'
import { useMastersStore } from '@/stores/masters'
import { DASHBOARD_ZERO_SUMMARY, useDashboardStore } from '@/stores/dashboard'
import type { DashboardSummaryView } from '@/api/dashboard'
import type { ManagementCapabilities } from '@/types/session'

const BASE_CAPABILITIES: ManagementCapabilities = {
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
  readAudit: false,
  manageAssetLibrary: false,
  manageLegalHold: false,
}

export function caps(overrides: Partial<ManagementCapabilities> = {}): ManagementCapabilities {
  return { ...BASE_CAPABILITIES, ...overrides }
}

export const routeRef = ref({
  hash: '',
  path: '/dashboard',
  query: {} as Record<string, string>,
})

const journeyTimelineStub = {
  name: 'RoleJourneyTimeline',
  props: ['steps', 'currentStepIndex', 'guidanceKey', 'titleKey'],
  template:
    '<div class="journey-timeline-stub" :data-step-count="steps.length" :data-current-index="currentStepIndex" :data-guidance-key="guidanceKey">{{ titleKey }}</div>',
}

const defaultStubs = {
  DashboardStatCards: true,
  LoadErrorPanel: true,
  CollaborationTimeoutConfigPanel: true,
  TaskHubPartitionSection: true,
  RoleJourneyTimeline: journeyTimelineStub,
  ElCard: { template: '<div><slot /></div>' },
  ElSkeleton: true,
  ElEmpty: true,
  ElButton: true,
  ElTabs: { template: '<div class="dashboard-tabs-stub"><slot /></div>' },
  ElTabPane: true,
}

export function mountDashboard(extraStubs: Record<string, unknown> = {}) {
  return mount(DashboardView, {
    global: {
      stubs: {
        ...defaultStubs,
        ...extraStubs,
      },
    },
  })
}

function mountDashboardWorkflowTab(extraStubs: Record<string, unknown> = {}) {
  routeRef.value.query = { tab: 'workflow' }
  return mountDashboard(extraStubs)
}

export function mountDashboardQueueTab(
  queue: string,
  extraStubs: Record<string, unknown> = {},
) {
  routeRef.value.query = { queue }
  return mountDashboard(extraStubs)
}

export function resetDashboardRoute() {
  routeRef.value = { hash: '', path: '/dashboard', query: {} }
}

type SessionPreset =
  | 'admin'
  | 'templateAuthor'
  | 'masterDesigner'
  | 'tester'
  | 'approver'
  | 'teamLead'
  | 'globalAdmin'

const SESSION_PRESETS: Record<
  SessionPreset,
  {
    displayName: string
    authorizedGroupCodes: string[]
    visibleRoutes: string[]
    roles: string[]
    capabilities: ManagementCapabilities
    allowAllRoutes?: boolean
  }
> = {
  admin: {
    displayName: 'Admin',
    authorizedGroupCodes: ['RETAIL'],
    visibleRoutes: ['route.template-management'],
    roles: [],
    capabilities: caps(),
  },
  templateAuthor: {
    displayName: 'Author',
    authorizedGroupCodes: ['RETAIL'],
    visibleRoutes: ['route.template-management'],
    roles: ['TEMPLATE_AUTHOR'],
    capabilities: caps({
      authorTemplates: true,
      viewCollaborationWorkItems: true,
      exportTemplates: true,
      authorContentModules: true,
    }),
  },
  masterDesigner: {
    displayName: 'Designer',
    authorizedGroupCodes: ['RETAIL'],
    visibleRoutes: ['route.master-management', 'route.template-management'],
    roles: ['MASTER_DESIGNER'],
    capabilities: caps({ manageMasters: true, authorTemplates: true, authorContentModules: true }),
    allowAllRoutes: true,
  },
  tester: {
    displayName: 'Tester',
    authorizedGroupCodes: ['RETAIL'],
    visibleRoutes: ['route.template-management'],
    roles: ['TEMPLATE_TESTER'],
    capabilities: caps({ decideTests: true, viewCollaborationWorkItems: true }),
  },
  approver: {
    displayName: 'Approver',
    authorizedGroupCodes: ['RETAIL'],
    visibleRoutes: ['route.template-management'],
    roles: ['TEMPLATE_APPROVER'],
    capabilities: caps({
      decideApprovals: true,
      viewCollaborationWorkItems: true,
      decideContentModuleReviews: true,
    }),
  },
  teamLead: {
    displayName: 'Group Admin',
    authorizedGroupCodes: ['RETAIL'],
    visibleRoutes: ['route.template-management', 'route.master-management'],
    roles: ['GROUP_ADMIN'],
    capabilities: caps({
      publishTemplates: true,
      reviewMasters: true,
      decideApprovals: false,
      viewCollaborationWorkItems: true,
      maintainCollaborationTimeoutConfig: true,
    }),
    allowAllRoutes: true,
  },
  globalAdmin: {
    displayName: 'Global Admin',
    authorizedGroupCodes: ['*'],
    visibleRoutes: [
      'route.template-management',
      'route.master-management',
      'route.identity-administration',
    ],
    roles: ['GLOBAL_ADMIN'],
    capabilities: caps({
      publishTemplates: true,
      reviewMasters: true,
      deleteTemplates: true,
      decideApprovals: true,
      viewCollaborationWorkItems: true,
      maintainCollaborationTimeoutConfig: true,
    }),
    allowAllRoutes: true,
  },
}

export function stubSession(
  preset: SessionPreset,
  overrides: {
    visibleRoutes?: string[]
    capabilities?: ManagementCapabilities
    allowAllRoutes?: boolean
  } = {},
) {
  const base = SESSION_PRESETS[preset]
  const sessionStore = useSessionStore()
  const visibleRoutes = overrides.visibleRoutes ?? base.visibleRoutes
  const allowAllRoutes = overrides.allowAllRoutes ?? base.allowAllRoutes ?? false
  sessionStore.session = {
    displayName: base.displayName,
    authorizedGroupCodes: base.authorizedGroupCodes,
    visibleRoutes,
    roles: base.roles,
    capabilities: overrides.capabilities ?? base.capabilities,
  } as never
  vi.spyOn(sessionStore, 'canAccessRoute').mockImplementation((routeKey: string) =>
    allowAllRoutes ? true : visibleRoutes.includes(routeKey),
  )
  return sessionStore
}

export function stubDashboardSummary(
  summary: Partial<DashboardSummaryView> | 'reject' | 'resolved' = 'resolved',
) {
  const dashboardStore = useDashboardStore()
  if (summary === 'reject') {
    vi.spyOn(dashboardStore, 'fetchSummary').mockRejectedValue(new Error('network'))
  } else if (summary === 'resolved') {
    vi.spyOn(dashboardStore, 'fetchSummary').mockImplementation(async () => {
      dashboardStore.summary = { ...DASHBOARD_ZERO_SUMMARY }
    })
  } else {
    vi.spyOn(dashboardStore, 'fetchSummary').mockImplementation(async () => {
      dashboardStore.summary = { ...DASHBOARD_ZERO_SUMMARY, ...summary }
    })
  }
  return dashboardStore
}

export function stubTemplates(templates: unknown[] | 'empty' | 'resolved' = 'resolved') {
  stubDashboardSummary()
  const templatesStore = useTemplatesStore()
  if (templates === 'resolved') {
    // Overview stats come from summary; leave templates empty unless a test seeds rows.
  } else if (templates === 'empty') {
    templatesStore.templates = []
  } else {
    templatesStore.templates = templates as never
  }
  vi.spyOn(templatesStore, 'enrichDevVersionIdsForWorkflow').mockResolvedValue(undefined)
  return templatesStore
}

export function stubMasters(
  masters: unknown[] | 'resolved' = 'resolved',
  options: { enrich?: boolean; reviewHistory?: (masterId: string) => unknown } = {},
) {
  stubDashboardSummary()
  const mastersStore = useMastersStore()
  if (masters === 'resolved') {
    vi.spyOn(mastersStore, 'fetchDashboardWorkflowMasters').mockResolvedValue(undefined)
  } else {
    mastersStore.masters = masters as never
    vi.spyOn(mastersStore, 'fetchDashboardWorkflowMasters').mockImplementation(async () => {
      mastersStore.masters = masters as never
    })
  }
  if (options.enrich !== false) {
    vi.spyOn(mastersStore, 'enrichDraftMasterReviewHistory').mockResolvedValue(undefined)
  }
  vi.spyOn(mastersStore, 'enrichCurrentRevisionLineIdsForWorkflow').mockResolvedValue(undefined)
  if (options.reviewHistory) {
    vi.spyOn(mastersStore, 'getDraftReviewHistory').mockImplementation(
      options.reviewHistory as never,
    )
  }
  return mastersStore
}

export function setWorkItems(items: unknown[]) {
  const collaborationStore = useCollaborationStore()
  collaborationStore.workItems = items as never
  return collaborationStore
}

export function templateFixture(overrides: Record<string, unknown> = {}) {
  return {
    id: 'tpl-1',
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Retail letter',
    lifecycleStatus: 'DRAFT',
    releaseVersion: null,
    releaseVersionCount: 0,
    masterId: 'master-1',
    updatedBy: '10000005',
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

export function masterFixture(overrides: Record<string, unknown> = {}) {
  return {
    id: 'm1',
    groupCode: 'RETAIL',
    name: 'Letterhead',
    status: 'DRAFT',
    originalFilename: 'letterhead.docx',
    anchorCount: 2,
    updatedBy: '10000005',
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

export function workItemFixture(overrides: Record<string, unknown> = {}) {
  return {
    workItemId: 'wi-1',
    templateId: 'tpl-1',
    templateName: 'Retail letter',
    queue: 'TEST',
    triggerType: 'SUBMIT_FOR_TEST',
    groupCode: 'RETAIL',
    submitterUserId: '10000003',
    summaryText: 'Ready for test',
    ageSeconds: 3600,
    createdAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

function journeyStub(wrapper: VueWrapper) {
  return wrapper.find('.journey-timeline-stub')
}

export function expectJourney(
  wrapper: VueWrapper,
  opts: {
    stepCount?: number
    index?: string | undefined
    guidanceKey?: string
    titleKey?: string
  },
) {
  const stub = journeyStub(wrapper)
  if (opts.stepCount !== undefined) {
    expect(Number(stub.attributes('data-step-count'))).toBe(opts.stepCount)
  }
  if ('index' in opts) {
    if (opts.index === undefined) {
      expect(stub.attributes('data-current-index')).toBeUndefined()
    } else {
      expect(stub.attributes('data-current-index')).toBe(opts.index)
    }
  }
  if (opts.guidanceKey !== undefined) {
    expect(stub.attributes('data-guidance-key')).toBe(opts.guidanceKey)
  }
  if (opts.titleKey !== undefined) {
    expect(stub.text()).toBe(opts.titleKey)
  }
}

export async function mountWorkflowAndFlush(extraStubs: Record<string, unknown> = {}) {
  const wrapper = mountDashboardWorkflowTab(extraStubs)
  await flushPromises()
  return wrapper
}
