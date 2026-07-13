import { defineComponent } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useOnboardingTour } from '@/composables/useOnboardingTour'
import { onboardingTourDismissStorageKey } from '@/utils/onboardingTourStorage'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'
import { templateAuthorJourneySteps } from '@/constants/roleJourneyDefinitions'

const routerPush = vi.fn().mockResolvedValue(undefined)
const routeState = {
  path: '/dashboard',
  fullPath: '/dashboard',
}

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ push: routerPush }),
}))

const capabilityRefs = {
  decideApprovals: { value: false },
  publishTemplates: { value: false },
  reviewMasters: { value: false },
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
}

function patchAuthorSession(username = 'author.user') {
  const sessionStore = useSessionStore()
  sessionStore.session = {
    username,
    displayName: 'Author',
    email: 'author@example.com',
    authSource: 'LOCAL',
    roles: ['TEMPLATE_AUTHOR'],
    authorizedGroupCodes: ['RETAIL'],
    defaultRoute: 'route.dashboard-home',
    visibleRoutes: ['route.dashboard-home'],
    capabilities: BASE_CAPABILITIES,
    expiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
  } as never
}

function mountTourHarness(options: { autoOpen?: boolean } = {}) {
  let tour!: ReturnType<typeof useOnboardingTour>
  const Harness = defineComponent({
    setup() {
      tour = useOnboardingTour({ autoOpenOnMount: options.autoOpen ?? false })
      return () => null
    },
  })
  mount(Harness)
  return tour
}

describe('useOnboardingTour', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    routerPush.mockClear()
    routeState.path = '/dashboard'
    routeState.fullPath = '/dashboard'
    capabilityRefs.decideApprovals.value = false
    capabilityRefs.publishTemplates.value = false
    capabilityRefs.reviewMasters.value = false
    document.body.innerHTML = ''
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('builds author tour steps from definitions (BDD-LRP-C8-006)', () => {
    patchAuthorSession()
    const tour = mountTourHarness()
    expect(tour.tourRole.value).toBe('TEMPLATE_AUTHOR')
    expect(tour.tourSteps.value).toBe(templateAuthorJourneySteps)
    expect(tour.canReplay.value).toBe(true)
  })

  it('opens at step 0 when not dismissed (BDD-LRP-C8-001)', async () => {
    patchAuthorSession()
    document.body.innerHTML =
      '<div data-journey-timeline><button data-journey-step></button></div>'
    const tour = mountTourHarness()
    const opened = await tour.openTour({ force: false })
    expect(opened).toBe(true)
    expect(tour.open.value).toBe(true)
    expect(tour.current.value).toBe(0)
  })

  it('does not auto-open when dismiss marker exists (BDD-LRP-C8-002)', async () => {
    patchAuthorSession()
    localStorage.setItem(onboardingTourDismissStorageKey('author.user'), '1')
    const tour = mountTourHarness()
    const opened = await tour.openTour({ force: false })
    expect(opened).toBe(false)
    expect(tour.open.value).toBe(false)
  })

  it('skip persists dismiss and closes (BDD-LRP-C8-002)', async () => {
    patchAuthorSession()
    document.body.innerHTML =
      '<div data-journey-timeline><button data-journey-step></button></div>'
    const tour = mountTourHarness()
    await tour.openTour({ force: true })
    tour.dismiss()
    expect(tour.open.value).toBe(false)
    expect(localStorage.getItem(onboardingTourDismissStorageKey('author.user'))).toBe('1')
  })

  it('replay opens from step 1 ignoring dismiss (BDD-LRP-C8-003 / 014)', async () => {
    patchAuthorSession()
    localStorage.setItem(onboardingTourDismissStorageKey('author.user'), '1')
    document.body.innerHTML =
      '<div data-journey-timeline><button data-journey-step></button></div>'
    const tour = mountTourHarness()
    await tour.replay()
    expect(tour.open.value).toBe(true)
    expect(tour.current.value).toBe(0)
    expect(localStorage.getItem(onboardingTourDismissStorageKey('author.user'))).toBe('1')
  })

  it('does not open when no tour role (BDD-LRP-C8-009)', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      username: 'nobody',
      displayName: 'Nobody',
      email: 'n@example.com',
      authSource: 'LOCAL',
      roles: [],
      authorizedGroupCodes: [],
      defaultRoute: 'route.dashboard-home',
      visibleRoutes: ['route.dashboard-home'],
      capabilities: BASE_CAPABILITIES,
      expiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
    } as never
    const tour = mountTourHarness()
    expect(tour.canReplay.value).toBe(false)
    const opened = await tour.openTour({ force: true })
    expect(opened).toBe(false)
    expect(tour.open.value).toBe(false)
  })

  it('navigates to dashboard when timeline anchors are missing (C8-C11)', async () => {
    patchAuthorSession()
    routeState.path = '/templates'
    routeState.fullPath = '/templates'
    routerPush.mockImplementation(async () => {
      routeState.path = '/dashboard'
      routeState.fullPath = '/dashboard'
      document.body.innerHTML =
        '<div data-journey-timeline><button data-journey-step></button></div>'
    })
    const tour = mountTourHarness()
    await tour.openTour({ force: true })
    expect(routerPush).toHaveBeenCalledWith('/dashboard')
    expect(tour.open.value).toBe(true)
  })

  it('auto-opens on mount when not dismissed', async () => {
    patchAuthorSession()
    document.body.innerHTML =
      '<div data-journey-timeline><button data-journey-step></button></div>'
    const tour = mountTourHarness({ autoOpen: true })
    await vi.waitFor(() => {
      expect(tour.open.value).toBe(true)
    })
  })
})
