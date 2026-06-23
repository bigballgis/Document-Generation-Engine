import { describe, expect, it, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { useDashboardStats } from '@/composables/useDashboardStats'
import { ROUTE_KEYS } from '@/routing/routeKeys'

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
})
