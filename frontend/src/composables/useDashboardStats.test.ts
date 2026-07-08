import { describe, expect, it, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { useDashboardStats } from '@/composables/useDashboardStats'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useApiPolicyStore } from '@/stores/apiPolicy'

describe('useDashboardStats', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('filters master and template stat cards by visible routes', () => {
    const visibleRoutes = ref([ROUTE_KEYS.templateManagement])
    const { stats } = useDashboardStats(visibleRoutes)

    expect(stats.value.some((stat) => stat.key === 'pendingActions')).toBe(true)
    expect(stats.value.some((stat) => stat.key === 'catalogTemplates')).toBe(true)
    expect(stats.value.some((stat) => stat.key === 'catalogMasters')).toBe(false)
  })

  it('routes pending actions card to the tasks section anchor', () => {
    const { stats } = useDashboardStats([])
    const pending = stats.value.find((stat) => stat.key === 'pendingActions')

    expect(pending?.path).toBe('/dashboard#tasks-section')
  })

  it('shows external services alert card when api policy route is visible', () => {
    const apiPolicyStore = useApiPolicyStore()
    apiPolicyStore.alerts = [
      {
        alertKind: 'MISSING_AD_GROUP',
        templateId: 'tpl-1',
        templateName: 'Demo',
        templateExternalId: 'DEMO-1',
      },
    ]

    const visibleRoutes = ref([ROUTE_KEYS.apiPolicyManagement])
    const { stats } = useDashboardStats(visibleRoutes)
    const externalServices = stats.value.find((stat) => stat.key === 'externalServicesAlerts')

    expect(externalServices).toBeDefined()
    expect(externalServices?.count).toBe(1)
    expect(externalServices?.path).toBe('/api/policies')
  })

  it('hides external services alert card without api policy route', () => {
    const apiPolicyStore = useApiPolicyStore()
    apiPolicyStore.alerts = [
      {
        alertKind: 'MISSING_AD_GROUP',
        templateId: 'tpl-1',
        templateName: 'Demo',
        templateExternalId: 'DEMO-1',
      },
    ]

    const visibleRoutes = ref([ROUTE_KEYS.templateManagement])
    const { stats } = useDashboardStats(visibleRoutes)

    expect(stats.value.some((stat) => stat.key === 'externalServicesAlerts')).toBe(false)
  })
})
