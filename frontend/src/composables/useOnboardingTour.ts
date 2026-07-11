import { computed, nextTick, onMounted, ref, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCapabilities } from '@/composables/useCapabilities'
import { useSessionStore } from '@/stores/session'
import {
  isOnboardingTourDismissed,
  markOnboardingTourDismissed,
} from '@/utils/onboardingTourStorage'
import {
  resolvePrimaryTourRole,
  resolveTourStepsForRole,
  type TourRole,
} from '@/utils/resolvePrimaryTourRole'
import type { RoleJourneyStep } from '@/constants/roleJourneyDefinitions'

const DASHBOARD_PATH = '/dashboard'

export interface UseOnboardingTourOptions {
  /** When true, attempt first-login auto-open after mount (C8-C7). Default true. */
  autoOpenOnMount?: boolean
  storage?: Storage
}

function waitAnimationFrame(): Promise<void> {
  return new Promise((resolve) => {
    if (typeof requestAnimationFrame === 'function') {
      requestAnimationFrame(() => resolve())
    } else {
      resolve()
    }
  })
}

function queryJourneyStep(index: number): HTMLElement | null {
  const steps = document.querySelectorAll<HTMLElement>(
    '[data-journey-timeline] [data-journey-step]',
  )
  return steps[index] ?? null
}

function hasJourneyTimeline(): boolean {
  return Boolean(document.querySelector('[data-journey-timeline]'))
}

export function useOnboardingTour(options: UseOnboardingTourOptions = {}) {
  const autoOpenOnMount = options.autoOpenOnMount ?? true
  const storage = options.storage ?? (typeof localStorage !== 'undefined' ? localStorage : null)

  const route = useRoute()
  const router = useRouter()
  const sessionStore = useSessionStore()
  const { decideApprovals, publishTemplates, reviewMasters } = useCapabilities()

  const open: Ref<boolean> = ref(false)
  const current: Ref<number> = ref(0)

  const tourRole = computed((): TourRole | null => {
    const roles = sessionStore.session?.roles ?? []
    return resolvePrimaryTourRole({
      roles,
      decideApprovals: decideApprovals.value,
      publishTemplates: publishTemplates.value,
      reviewMasters: reviewMasters.value,
    })
  })

  const tourSteps = computed((): RoleJourneyStep[] => {
    const role = tourRole.value
    return role ? resolveTourStepsForRole(role) : []
  })

  const canReplay = computed(() => tourSteps.value.length > 0)

  async function ensureDashboardAnchors(): Promise<void> {
    if (hasJourneyTimeline()) {
      return
    }
    if (route.path !== DASHBOARD_PATH) {
      await router.push(DASHBOARD_PATH)
      await nextTick()
      await waitAnimationFrame()
    }
  }

  function resolveStepTarget(index: number): HTMLElement | undefined {
    const stepEl = queryJourneyStep(index)
    if (stepEl) {
      return stepEl
    }
    const timeline = document.querySelector<HTMLElement>('[data-journey-timeline]')
    return timeline ?? undefined
  }

  function targetSelectorFor(index: number): string | (() => HTMLElement | null) {
    return () => resolveStepTarget(index) ?? null
  }

  async function openTour(opts: { force?: boolean } = {}): Promise<boolean> {
    if (tourSteps.value.length === 0) {
      return false
    }
    const username = sessionStore.session?.username
    if (!opts.force) {
      if (!username || !storage || isOnboardingTourDismissed(storage, username)) {
        return false
      }
    }

    await ensureDashboardAnchors()
    await nextTick()
    await waitAnimationFrame()

    current.value = 0
    open.value = true
    return true
  }

  function dismiss(): void {
    open.value = false
    const username = sessionStore.session?.username
    if (username && storage) {
      markOnboardingTourDismissed(storage, username)
    }
  }

  async function replay(): Promise<boolean> {
    if (!canReplay.value) {
      return false
    }
    return openTour({ force: true })
  }

  onMounted(() => {
    if (!autoOpenOnMount) {
      return
    }
    void nextTick(async () => {
      await waitAnimationFrame()
      await openTour({ force: false })
    })
  })

  return {
    open,
    current,
    tourRole,
    tourSteps,
    canReplay,
    openTour,
    dismiss,
    replay,
    targetSelectorFor,
    resolveStepTarget,
  }
}
