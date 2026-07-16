import { defineComponent, ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { templateAuthorJourneySteps } from '@/constants/roleJourneyDefinitions'
import { useDashboardJourney } from '@/composables/useDashboardJourney'
import { useCollaborationStore } from '@/stores/collaboration'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { ManagementCapabilities } from '@/types/session'

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

const capabilityRefs = {
  context: ref({ roles: ['TEMPLATE_AUTHOR'] }),
  decideApprovals: ref(false),
  publishTemplates: ref(false),
  reviewMasters: ref(false),
  deleteTemplates: ref(false),
}

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => capabilityRefs,
}))

const BASE_CAPABILITIES: ManagementCapabilities = {
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: true,
  decideTests: false,
  decideApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: true,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: true,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
  manageAssetLibrary: false,
}

function mountJourneyHarness() {
  let journey!: ReturnType<typeof useDashboardJourney>
  const Harness = defineComponent({
    setup() {
      journey = useDashboardJourney()
      return { journey }
    },
    template: '<div />',
  })
  mount(Harness)
  return journey
}

describe('useDashboardJourney', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockClear()
    capabilityRefs.context.value = { roles: ['TEMPLATE_AUTHOR'] }
    capabilityRefs.decideApprovals.value = false
    capabilityRefs.publishTemplates.value = false
    capabilityRefs.reviewMasters.value = false
    capabilityRefs.deleteTemplates.value = false

    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      capabilities: BASE_CAPABILITIES,
    } as never
  })

  it('shows journey section for template author with six steps', () => {
    const journey = mountJourneyHarness()

    expect(journey.showJourneySection.value).toBe(true)
    expect(journey.journeySteps.value.length).toBe(templateAuthorJourneySteps.length)
    expect(journey.journeyTitleKey.value).toBe('journey.roles.TEMPLATE_AUTHOR.title')
  })

  it('sets template author journey index 3 for draft ready to submit', async () => {
    const templatesStore = useTemplatesStore()
    templatesStore.templates = [
      {
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
        bindingsCount: 2,
        hasSuccessfulTrialOutput: true,
      },
    ] as never

    const journey = mountJourneyHarness()

    expect(journey.journeyCurrentStepIndex.value).toBe(3)
  })

  it('sets template author journey waiting guidance when only testing templates exist', () => {
    const templatesStore = useTemplatesStore()
    templatesStore.templates = [
      {
        id: 'tpl-testing',
        externalId: 'TPL-TEST',
        groupCode: 'RETAIL',
        name: 'Testing template',
        lifecycleStatus: 'TESTING',
        releaseVersion: null,
        releaseVersionCount: 0,
        masterId: 'master-1',
        updatedBy: '10000005',
        updatedAt: '2026-06-26T10:00:00Z',
      },
    ] as never

    const journey = mountJourneyHarness()

    expect(journey.journeyCurrentStepIndex.value).toBeNull()
    expect(journey.journeyGuidanceKey.value).toBe('journey.roles.TEMPLATE_AUTHOR.waitingTesting.guidance')
  })

  it('hides journey section when no cluster role matches', () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: [],
      authorizedGroupCodes: ['RETAIL'],
      capabilities: BASE_CAPABILITIES,
    } as never

    const journey = mountJourneyHarness()

    expect(journey.showJourneySection.value).toBe(false)
    expect(journey.journeySteps.value).toEqual([])
  })

  it('navigates to dashboard journey path when openDashboardJourney is called', async () => {
    const templatesStore = useTemplatesStore()
    templatesStore.templates = [
      {
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
        bindingsCount: 2,
        hasSuccessfulTrialOutput: true,
      },
    ] as never

    const journey = mountJourneyHarness()

    expect(journey.dashboardJourneyPath.value).toBeTruthy()
    journey.openDashboardJourney()
    expect(routerPush).toHaveBeenCalledWith(journey.dashboardJourneyPath.value)
  })

  it('sets master designer journey index 2 for ready draft master', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['MASTER_DESIGNER'],
      authorizedGroupCodes: ['RETAIL'],
      capabilities: {
        ...BASE_CAPABILITIES,
        manageMasters: true,
      },
    } as never

    const mastersStore = useMastersStore()
    mastersStore.masters = [
      {
        id: 'm1',
        groupCode: 'RETAIL',
        name: 'Letterhead',
        status: 'DRAFT',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
        updatedBy: '10000005',
        updatedAt: '2026-06-26T10:00:00Z',
      },
    ]
    vi.spyOn(mastersStore, 'getDraftReviewHistory').mockReturnValue(undefined)

    const journey = mountJourneyHarness()

    expect(journey.primaryClusterOneRole.value).toBe('MASTER_DESIGNER')
    expect(journey.journeyCurrentStepIndex.value).toBe(2)
  })

  it('prefers global admin journey over team-lead when both roles present', () => {
    capabilityRefs.context.value = { roles: ['GLOBAL_ADMIN'] }
    capabilityRefs.publishTemplates.value = true
    capabilityRefs.reviewMasters.value = true

    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      capabilities: {
        ...BASE_CAPABILITIES,
        publishTemplates: true,
        reviewMasters: true,
        maintainCollaborationTimeoutConfig: true,
      },
    } as never

    const journey = mountJourneyHarness()

    expect(journey.journeyTitleKey.value).toBe('journey.roles.GLOBAL_ADMIN.title')
    expect(journey.journeyTitleKey.value).not.toBe('journey.roles.GROUP_ADMIN.title')
  })

  it('sets global admin journey index 4 when escalation work items exist', () => {
    capabilityRefs.context.value = { roles: ['GLOBAL_ADMIN'] }

    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      capabilities: {
        ...BASE_CAPABILITIES,
        publishTemplates: true,
        reviewMasters: true,
        deleteTemplates: true,
        maintainCollaborationTimeoutConfig: true,
      },
    } as never

    const collaborationStore = useCollaborationStore()
    collaborationStore.workItems = [
      {
        workItemId: 'wi-escalation',
        templateId: 'tpl-escalation',
        templateName: 'Overdue template',
        queue: 'ESCALATION',
        triggerType: 'TIMEOUT_ESCALATION',
        groupCode: 'RETAIL',
        submitterUserId: '10000003',
        summaryText: 'Overdue reminder',
        ageSeconds: 7200,
        createdAt: '2026-06-26T10:00:00Z',
      },
    ]

    const journey = mountJourneyHarness()

    expect(journey.journeyCurrentStepIndex.value).toBe(4)
  })
})
