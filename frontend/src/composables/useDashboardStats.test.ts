import { describe, expect, it, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { useDashboardStats } from '@/composables/useDashboardStats'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useMastersStore } from '@/stores/masters'
import { useTemplatesStore } from '@/stores/templates'

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

  it('uses totalElements for catalog counts when page content is a partial slice', () => {
    const mastersStore = useMastersStore()
    mastersStore.$patch({
      masters: [
        {
          id: 'master-1',
          groupCode: 'RETAIL',
          name: 'Retail letterhead',
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      masterListTotalElements: 42,
    })
    const templatesStore = useTemplatesStore()
    templatesStore.$patch({
      templates: [
        {
          id: 'tpl-1',
          externalId: 'TPL-1',
          name: 'Demo',
          groupCode: 'RETAIL',
          lifecycleStatus: 'DRAFT',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: '10000001',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      templateListTotalElements: 87,
    })

    const visibleRoutes = ref([ROUTE_KEYS.masterManagement, ROUTE_KEYS.templateManagement])
    const { stats } = useDashboardStats(visibleRoutes)

    expect(stats.value.find((stat) => stat.key === 'catalogMasters')?.count).toBe(42)
    expect(stats.value.find((stat) => stat.key === 'catalogTemplates')?.count).toBe(87)
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
